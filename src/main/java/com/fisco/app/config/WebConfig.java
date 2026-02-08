package com.fisco.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fisco.app.security.AdminAuthInterceptor;
import com.fisco.app.security.EnterpriseAuthInterceptor;

import lombok.RequiredArgsConstructor;

/**
 * Web MVC配置类
 * 配置拦截器等
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    @NonNull
    private final AdminAuthInterceptor adminAuthInterceptor;

    @NonNull
    private final EnterpriseAuthInterceptor enterpriseAuthInterceptor;

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        // 注册管理员权限拦截器
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/api/admin/**")      // 拦截所有管理员接口
                .addPathPatterns("/api/audit/**")     // 拦截审计日志接口（需要管理员权限）
                .excludePathPatterns(
                    "/api/admin/auth/login",          // 排除登录接口
                    "/api/admin/auth/validate"        // 排除验证接口
                );

        // 注册企业权限拦截器
        registry.addInterceptor(enterpriseAuthInterceptor)
                .addPathPatterns("/api/enterprise/**")  // 拦截所有企业接口
                .excludePathPatterns(
                    "/api/enterprise/register"          // 排除企业注册接口（公开）
                );
    }
}
