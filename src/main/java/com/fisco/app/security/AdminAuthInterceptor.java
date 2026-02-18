package com.fisco.app.security;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fisco.app.entity.user.Admin;
import com.fisco.app.service.user.AdminService;
import com.fisco.app.vo.Result;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 管理员权限拦截器
 * 拦截带有@RequireAdmin注解的请求，验证管理员权限
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final AdminService adminService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        // 只处理带有@RequireAdmin注解的方法
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RequireAdmin requireAdmin = handlerMethod.getMethodAnnotation(RequireAdmin.class);

        if (requireAdmin == null) {
            // 没有注解，直接放行
            return true;
        }

        // 检查Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendErrorResponse(response, 401, "未提供认证令牌");
            return false;
        }

        // 提取并验证token
        String token = jwtTokenProvider.extractTokenFromHeader(authHeader);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            sendErrorResponse(response, 401, "令牌无效或已过期");
            return false;
        }

        // 获取管理员用户名
        String username = jwtTokenProvider.getUserAddressFromToken(token);
        if (username == null) {
            sendErrorResponse(response, 401, "令牌无效或已过期");
            return false;
        }

        try {
            // 获取管理员信息
            Admin admin = adminService.getAdminByUsername(username);

            // 检查管理员状态
            if (!admin.isAvailable()) {
                sendErrorResponse(response, 403, "管理员账户已被禁用或锁定");
                return false;
            }

            // 检查角色权限
            RequireAdmin.AdminRole requiredRole = requireAdmin.value();
            if (!hasRequiredRole(admin.getRole(), requiredRole)) {
                sendErrorResponse(response, 403, "权限不足");
                return false;
            }

            // 将管理员信息存入request attribute，供Controller使用
            request.setAttribute("currentAdmin", admin);

            return true;

        } catch (Exception e) {
            log.error("管理员权限验证失败: username={}, error={}", username, e.getMessage());
            sendErrorResponse(response, 403, "权限验证失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 检查管理员角色是否满足所需角色
     */
    private boolean hasRequiredRole(Admin.AdminRole adminRole, RequireAdmin.AdminRole requiredRole) {
        if (requiredRole == RequireAdmin.AdminRole.AUDITOR) {
            // AUDITOR及以上权限
            return adminRole == Admin.AdminRole.AUDITOR ||
                   adminRole == Admin.AdminRole.ADMIN ||
                   adminRole == Admin.AdminRole.SUPER_ADMIN;
        } else if (requiredRole == RequireAdmin.AdminRole.ADMIN) {
            // ADMIN及以上权限
            return adminRole == Admin.AdminRole.ADMIN ||
                   adminRole == Admin.AdminRole.SUPER_ADMIN;
        } else if (requiredRole == RequireAdmin.AdminRole.SUPER_ADMIN) {
            // 仅SUPER_ADMIN权限
            return adminRole == Admin.AdminRole.SUPER_ADMIN;
        }
        return false;
    }

    /**
     * 发送错误响应
     */
    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        Result<?> result = Result.error(message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
