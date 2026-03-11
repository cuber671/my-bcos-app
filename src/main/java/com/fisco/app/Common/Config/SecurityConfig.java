package com.fisco.app.Common.Config;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fisco.app.Modules.Warehouse.Mapper.StockOrderMapper;
import com.fisco.app.Modules.Warehouse.Mapper.WarehouseReceiptMapper;

/**
 * 安全配置 - JWT双令牌策略认证
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig implements WebMvcConfigurer {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitInterceptor rateLimitInterceptor;
    private final IdempotentInterceptor idempotentInterceptor;
    private final TraceIdInterceptor traceIdInterceptor;
    private final WarehouseReceiptMapper warehouseReceiptMapper;
    private final StockOrderMapper stockOrderMapper;

    /**
     * 构造函数注入JwtAuthenticationFilter、RateLimitInterceptor、IdempotentInterceptor和TraceIdInterceptor
     *
     * @param jwtAuthenticationFilter JWT认证过滤器
     * @param rateLimitInterceptor 限流拦截器
     * @param idempotentInterceptor 幂等性拦截器
     * @param traceIdInterceptor 链路追踪ID拦截器
     * @param warehouseReceiptMapper 仓单Mapper，用于ABAC权限校验
     * @param stockOrderMapper 入库单Mapper，用于ABAC权限校验
     */
    @Autowired
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          RateLimitInterceptor rateLimitInterceptor,
                          IdempotentInterceptor idempotentInterceptor,
                          TraceIdInterceptor traceIdInterceptor,
                          WarehouseReceiptMapper warehouseReceiptMapper,
                          StockOrderMapper stockOrderMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitInterceptor = rateLimitInterceptor;
        this.idempotentInterceptor = idempotentInterceptor;
        this.traceIdInterceptor = traceIdInterceptor;
        this.warehouseReceiptMapper = warehouseReceiptMapper;
        this.stockOrderMapper = stockOrderMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF（前后端分离使用JWT）
            .csrf().disable()
            // 不使用Session
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            // 配置拦截规则
            .authorizeRequests()
                // 允许访问认证接口（登录、刷新Token等）
                .antMatchers("/api/auth/**", "/api/v1/auth/**").permitAll()
                .antMatchers("/api/v1/enterprise/register", "/api/v1/enterprise/login", "/api/v1/enterprise/admin/login").permitAll()
                .antMatchers("/api/v1/user/register", "/api/v1/user/login").permitAll()
                // 允许访问健康检查接口
                .antMatchers("/api/v1/health", "/actuator/**").permitAll()
                // 允许访问Swagger
                .antMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .antMatchers("/webjars/**").permitAll()
                // 其他请求需要认证
                .anyRequest().authenticated()
            .and()
            // 添加JWT认证过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            // 禁用HTTP Basic认证（使用JWT）
            .httpBasic().disable();

        return http.build();
    }

    /**
     * 注册拦截器
     * 添加角色权限校验拦截器、数据归属校验拦截器和敏感操作校验拦截器
     *
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        // 注册TraceId拦截器 - 最先执行，生成链路追踪ID
        registry.addInterceptor(traceIdInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/**",
                        "/api/v1/auth/**",
                        "/api/v1/health",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/webjars/**",
                        "/error",
                        "/actuator/**"
                );

        // 注册角色权限校验拦截器
        // 拦截所有API请求（除了认证和Swagger相关）
        registry.addInterceptor(Objects.requireNonNull(roleAuthorizationInterceptor()))
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/**",
                        "/api/v1/auth/**",
                        "/api/v1/health",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/webjars/**",
                        "/error",
                        "/actuator/**"
                );

        // 注册数据归属校验拦截器
        // 在角色权限校验之后执行
        registry.addInterceptor(Objects.requireNonNull(dataOwnershipInterceptor()))
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/**",
                        "/api/v1/auth/**",
                        "/api/v1/health",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/webjars/**",
                        "/error",
                        "/actuator/**"
                );

        // 注册仓单模块ABAC权限校验拦截器
        // 只拦截仓单相关API路径
        registry.addInterceptor(Objects.requireNonNull(warehouseABACInterceptor()))
                .addPathPatterns(
                        "/api/**/warehouse/**",
                        "/api/**/receipt/**",
                        "/api/**/stock-in/**",
                        "/api/**/stock/**",
                        "/api/**/endorsement/**",
                        "/api/**/split/**",
                        "/api/**/merge/**",
                        "/api/**/burn/**"
                )
                .excludePathPatterns(
                        "/api/auth/**",
                        "/api/v1/auth/**",
                        "/api/v1/health",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/webjars/**",
                        "/error",
                        "/actuator/**"
                );

        // 注册敏感操作二次校验拦截器
        // 在数据归属校验之后执行
        registry.addInterceptor(Objects.requireNonNull(sensitiveOperationInterceptor()))
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/**",
                        "/api/v1/auth/**",
                        "/api/v1/health",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/webjars/**",
                        "/error",
                        "/actuator/**"
                );

        // 注册限流拦截器
        // 最后执行限流检查
        registry.addInterceptor(Objects.requireNonNull(rateLimitInterceptor))
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/**",
                        "/api/v1/auth/**",
                        "/api/v1/health",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/webjars/**",
                        "/error",
                        "/actuator/**"
                );

        // 注册幂等性拦截器
        // 在限流之后执行
        registry.addInterceptor(Objects.requireNonNull(idempotentInterceptor))
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/**",
                        "/api/v1/auth/**",
                        "/api/v1/health",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/webjars/**",
                        "/error",
                        "/actuator/**"
                );
    }

    /**
     * 角色权限校验拦截器Bean
     *
     * @return 角色权限校验拦截器
     */
    @Bean
    public RoleAuthorizationInterceptor roleAuthorizationInterceptor() {
        return new RoleAuthorizationInterceptor();
    }

    /**
     * 数据归属校验拦截器Bean
     *
     * @return 数据归属校验拦截器
     */
    @Bean
    public DataOwnershipInterceptor dataOwnershipInterceptor() {
        return new DataOwnershipInterceptor();
    }

    /**
     * 敏感操作二次校验拦截器Bean
     *
     * @return 敏感操作二次校验拦截器
     */
    @Bean
    public SensitiveOperationInterceptor sensitiveOperationInterceptor() {
        return new SensitiveOperationInterceptor();
    }

    /**
     * 仓单模块ABAC权限校验拦截器Bean
     *
     * @return 仓单ABAC权限校验拦截器
     */
    @Bean
    public WarehouseABACInterceptor warehouseABACInterceptor() {
        return new WarehouseABACInterceptor(warehouseReceiptMapper, stockOrderMapper);
    }

    /**
     * 密码编码器 Bean
     * 用于密码加密和验证
     *
     * @return BCrypt密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
