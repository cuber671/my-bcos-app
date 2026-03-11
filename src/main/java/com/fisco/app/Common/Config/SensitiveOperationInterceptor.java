package com.fisco.app.Common.Config;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fisco.app.Common.Annotation.SensitiveOperation;
import com.fisco.app.Common.Service.VerificationCodeService;
import com.fisco.app.Common.Utils.JwtUtil;

import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;

/**
 * 敏感操作二次校验拦截器
 * 基于方法上的 @SensitiveOperation 注解进行二次验证码校验
 *
 * 工作流程：
 * 1. 拦截请求，检查目标方法是否有 @SensitiveOperation 注解
 * 2. 获取当前用户的企业ID（entId）
 * 3. 从请求头或参数中获取验证码
 * 4. 校验验证码是否正确
 * 5. 系统管理员可选择是否绕过
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Slf4j
public class SensitiveOperationInterceptor implements HandlerInterceptor {

    private static final String HEADER_VERIFICATION_CODE = "X-Verification-Code";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private VerificationCodeService verificationCodeService;

    /**
     * 预请求处理 - 进行敏感操作二次校验
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

        // 2. 检查方法是否有 @SensitiveOperation 注解
        SensitiveOperation sensitiveOperation = handlerMethod.getMethodAnnotation(SensitiveOperation.class);
        if (sensitiveOperation == null) {
            // 没有注解，放行
            return true;
        }

        // 3. 获取用户权限范围
        Claims claims = (Claims) request.getAttribute(JwtAuthenticationFilter.ATTR_CLAIMS);
        if (claims == null) {
            log.warn("JWT Claims为空，无法进行敏感操作校验: {}", request.getRequestURI());
            sendForbiddenResponse(response, "Authentication required");
            return false;
        }

        Integer scope = JwtUtil.getScope(claims);

        // 4. 系统管理员可选择是否绕过二次校验
        if (sensitiveOperation.adminBypass() && Objects.equals(1, scope)) {
            log.debug("系统管理员绕过敏感操作二次校验，URI: {}", request.getRequestURI());
            return true;
        }

        // 5. 获取企业ID
        Long entId = JwtUtil.getEntId(claims);
        if (entId == null) {
            log.warn("用户无企业ID，无法进行敏感操作校验: {}", request.getRequestURI());
            sendForbiddenResponse(response, "Enterprise verification required for sensitive operation");
            return false;
        }

        // 6. 获取验证码
        String code = getVerificationCode(request);
        if (code == null || code.isEmpty()) {
            log.warn("敏感操作缺少验证码: {}", request.getRequestURI());
            sendForbiddenResponse(response, "Verification code required for sensitive operation");
            return false;
        }

        // 7. 校验验证码
        String codeType = sensitiveOperation.codeType();
        boolean verified = verificationCodeService.verifyCode(entId.toString(), codeType, code);

        if (!verified) {
            log.warn("敏感操作验证码校验失败: {}", request.getRequestURI());
            sendForbiddenResponse(response, "Invalid or expired verification code");
            return false;
        }

        log.info("敏感操作二次校验通过: 操作={}, 企业ID={}, URI: {}",
                sensitiveOperation.value(), entId, request.getRequestURI());
        return true;
    }

    /**
     * 从请求中获取验证码
     * 优先从请求头获取，其次从请求参数获取
     *
     * @param request HTTP请求
     * @return 验证码，如果不存在返回null
     */
    private String getVerificationCode(HttpServletRequest request) {
        // 优先从请求头获取
        String code = request.getHeader(HEADER_VERIFICATION_CODE);
        if (code != null && !code.isEmpty()) {
            return code;
        }

        // 从请求参数获取
        code = request.getParameter("verificationCode");
        if (code != null && !code.isEmpty()) {
            return code;
        }

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
