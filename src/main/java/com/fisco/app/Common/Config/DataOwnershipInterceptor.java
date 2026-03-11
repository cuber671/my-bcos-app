package com.fisco.app.Common.Config;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.NonNull;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fisco.app.Common.Annotation.DataOwnership;
import com.fisco.app.Common.Utils.JwtUtil;

import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;

/**
 * 数据归属校验拦截器
 * 基于方法上的 @DataOwnership 注解进行数据归属校验
 *
 * 工作流程：
 * 1. 拦截请求，检查目标方法是否有 @DataOwnership 注解
 * 2. 从 JWT 获取当前用户的企业ID（entId）
 * 3. 从请求参数获取目标数据的企业ID
 * 4. 比对两者是否匹配
 * 5. 系统管理员(scope=1)可绕过校验
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Slf4j
public class DataOwnershipInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 预请求处理 - 进行数据归属校验
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

        // 2. 检查方法是否有 @DataOwnership 注解
        DataOwnership dataOwnership = handlerMethod.getMethodAnnotation(DataOwnership.class);
        if (dataOwnership == null) {
            // 没有注解，放行
            return true;
        }

        // 3. 从请求属性中获取 JWT Claims（由 JwtAuthenticationFilter 设置）
        Claims claims = (Claims) request.getAttribute(JwtAuthenticationFilter.ATTR_CLAIMS);
        if (claims == null) {
            log.warn("JWT Claims为空，无法进行数据归属校验: {}", request.getRequestURI());
            sendForbiddenResponse(response, "Authentication required");
            return false;
        }

        // 4. 获取用户权限范围
        Integer scope = JwtUtil.getScope(claims);

        // 5. 系统管理员(scope=1)可绕过数据归属校验
        if (dataOwnership.adminBypass() && Objects.equals(1, scope)) {
            log.debug("系统管理员跳过数据归属校验，URI: {}", request.getRequestURI());
            return true;
        }

        // 6. 获取当前用户的企业ID
        Long userEntId = JwtUtil.getEntId(claims);
        if (userEntId == null) {
            // 没有企业ID，可能是个人用户，跳过校验（或者根据业务需求决定是否放行）
            log.debug("用户无企业ID，跳过数据归属校验，URI: {}", request.getRequestURI());
            return true;
        }

        // 7. 从请求参数获取目标企业ID
        String paramName = dataOwnership.paramName();
        Long targetEntId = getParameterAsLong(request, paramName);

        if (targetEntId == null) {
            // 参数不存在，可能不需要校验或者是查询列表
            log.debug("参数 {} 不存在，跳过数据归属校验，URI: {}", paramName, request.getRequestURI());
            return true;
        }

        // 8. 比对数据归属
        if (!Objects.equals(userEntId, targetEntId)) {
            log.warn("数据归属校验失败，用户企业ID: {}, 目标企业ID: {}, URI: {}",
                    userEntId, targetEntId, request.getRequestURI());
            sendForbiddenResponse(response, "Access denied: data ownership verification failed");
            return false;
        }

        log.debug("数据归属校验通过，用户企业ID: {}, 目标企业ID: {}, URI: {}",
                userEntId, targetEntId, request.getRequestURI());
        return true;
    }

    /**
     * 从请求中获取参数值并转换为Long
     *
     * @param request   HTTP请求
     * @param paramName 参数名
     * @return 参数值，如果不存在或无法转换返回null
     */
    private Long getParameterAsLong(HttpServletRequest request, String paramName) {
        if (paramName == null || paramName.isEmpty()) {
            return null;
        }

        // 优先从PathVariable获取（RESTful风格）
        Object uriVariables = request.getAttribute("org.springframework.web.servlet.HandlerMapping.uriTemplateVariables");
        if (uriVariables instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, String> pathVars = (Map<String, String>) uriVariables;
            String value = pathVars.get(paramName);
            if (value != null) {
                try {
                    return Long.parseLong(value);
                } catch (NumberFormatException e) {
                    log.warn("PathVariable {} 无法转换为Long: {}", paramName, value);
                }
            }
        }

        // 从RequestParam获取
        String value = request.getParameter(paramName);
        if (value != null && !value.isEmpty()) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                log.warn("参数 {} 无法转换为Long: {}", paramName, value);
                return null;
            }
        }

        // 从RequestBody获取（需要解析JSON，这里简化处理）
        // 实际项目中可以通过 @RequestBody 注解的参数直接获取

        return null;
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
