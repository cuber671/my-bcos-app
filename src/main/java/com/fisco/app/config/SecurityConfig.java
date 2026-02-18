package com.fisco.app.config;

import com.fisco.app.security.JwtAuthenticationEntryPoint;
import com.fisco.app.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security配置类
 * 配置HTTP安全规则和JWT认证
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF（使用JWT不需要CSRF保护）
            .csrf().disable()

            // 配置会话管理为无状态
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()

            // 配置异常处理
            .exceptionHandling()
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
            .and()

            // 配置授权规则
            .authorizeRequests()
                // 公开端点 - 不需要认证
                .antMatchers(
                    "/",
                    "/api/auth/**",                    // 用户登录、注册等认证接口
                    "/api/admin/auth/login",          // 管理员登录接口
                    "/api/admin/auth/validate",       // 管理员令牌验证接口
                    "/api/enterprise/register",       // 企业注册
                    "/api/blockchain/**",             // 区块链基础交互接口
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/swagger-resources/**",
                    "/webjars/**",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/favicon.ico",
                    "/error"
                ).permitAll()

                // 管理员端点 - 需要系统管理员角色
                .antMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")

                // 所有其他API端点都需要认证
                .antMatchers("/api/**").authenticated()

                // 其他任何请求需要认证
                .anyRequest().authenticated()
            .and()

            // 禁用表单登录和HTTP Basic
            .formLogin().disable()
            .httpBasic().disable()

            // 添加JWT认证过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
