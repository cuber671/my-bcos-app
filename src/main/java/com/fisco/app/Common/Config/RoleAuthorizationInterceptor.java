package com.fisco.app.Common.Config;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.NonNull;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fisco.app.Common.Annotation.RequirePermission;
import com.fisco.app.Common.Annotation.RequireRole;
import com.fisco.app.Common.Utils.JwtUtil;

import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;

/**
 * 角色权限校验拦截器
 * 基于方法上的 @RequireRole 和 @RequirePermission 注解进行权限校验
 *
 * 工作流程：
 * 1. 拦截请求，检查目标方法是否有权限注解
 * 2. 从 JWT 获取用户角色和权限范围
 * 3. 校验用户是否有权限访问
 * 4. 允许系统管理员(scope=1)或具有匹配角色的用户访问
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Slf4j
public class RoleAuthorizationInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 预请求处理 - 进行角色权限校验
     *
     * @param request  HTTP请求
     * @param response HTTP响应
     * @param handler  处理器
     * @return true=继续执行，false=拒绝访问
     */
    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler)
            throws Exception {

        // 1. 只处理 Controller 方法
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;

        // 2. 检查方法是否有权限注解
        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        RequirePermission requirePermission = handlerMethod.getMethodAnnotation(RequirePermission.class);

        // 如果没有任何权限注解，放行
        if (requireRole == null && requirePermission == null) {
            return true;
        }

        // 3. 从请求属性中获取 JWT Claims（由 JwtAuthenticationFilter 设置）
        Claims claims = (Claims) request.getAttribute(JwtAuthenticationFilter.ATTR_CLAIMS);
        if (claims == null) {
            log.warn("JWT Claims为空，无法进行角色校验: {}", request.getRequestURI());
            sendForbiddenResponse(response, "Authentication required");
            return false;
        }

        // 4. 获取用户权限信息
        Integer scope = JwtUtil.getScope(claims);
        String role = JwtUtil.getRole(claims);

        // 5. 校验 @RequirePermission 权限等级
        if (requirePermission != null) {
            // 系统管理员(scope=1)可绕过权限等级校验
            if (requirePermission.adminBypass() && Objects.equals(1, scope)) {
                log.debug("系统管理员跳过权限等级校验，URI: {}", request.getRequestURI());
                return true;
            }

            // 校验权限等级
            int requiredLevel = requirePermission.level();
            if (!JwtUtil.hasPermissionLevel(claims, requiredLevel)) {
                int userLevel = JwtUtil.getPermissionLevel(claims);
                log.warn("权限等级不足，用户等级: {}, 所需等级: {}, URI: {}",
                        userLevel, requiredLevel, request.getRequestURI());
                sendForbiddenResponse(response, "Access denied: insufficient permission level");
                return false;
            }
            log.debug("权限等级校验通过，用户等级: {}, 所需等级: {}, URI: {}",
                    JwtUtil.getPermissionLevel(claims), requiredLevel, request.getRequestURI());
        }

        // 6. 校验 @RequireRole 角色
        if (requireRole != null) {
            // 系统管理员(scope=1)可绕过角色校验
            if (requireRole.adminBypass() && Objects.equals(1, scope)) {
                log.debug("系统管理员跳过角色校验，URI: {}", request.getRequestURI());
                return true;
            }

            // 校验用户角色是否在允许列表中
            String[] allowedRoles = requireRole.value();
            if (allowedRoles == null || allowedRoles.length == 0) {
                // 没有配置允许角色，默认只允许系统管理员
                if (Objects.equals(1, scope)) {
                    return true;
                }
                log.warn("接口未配置允许角色，访问被拒绝: {}", request.getRequestURI());
                sendForbiddenResponse(response, "Access denied: no roles allowed");
                return false;
            }

            // 检查用户角色是否匹配
            if (role != null && Arrays.asList(allowedRoles).contains(role)) {
                log.debug("角色校验通过，用户角色: {}, 允许角色: {}, URI: {}",
                        role, Arrays.toString(allowedRoles), request.getRequestURI());
            } else {
                // 角色不匹配，拒绝访问
                log.warn("角色权限不足，用户角色: {}, 允许角色: {}, URI: {}",
                        role, Arrays.toString(allowedRoles), request.getRequestURI());
                sendForbiddenResponse(response, "Access denied: insufficient permissions");
                return false;
            }
        }

        return true;
    }

    /**
     * 发送禁止访问响应
     *
     * @param response HTTP响应
     * @param message  错误消息
     */
    private void sendForbiddenResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> error = new HashMap<>();
        error.put("code", 403);
        error.put("message", message);

        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
