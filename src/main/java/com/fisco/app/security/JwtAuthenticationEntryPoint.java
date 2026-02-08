package com.fisco.app.security;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * JWT认证入口点
 * 处理未认证的请求，返回401错误
 */
@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                        HttpServletResponse response,
                        AuthenticationException authException) throws IOException {

        log.warn("Unauthorized access attempt: {} {}", request.getMethod(), request.getRequestURI());

        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        // 构建错误响应
        ObjectMapper mapper = new ObjectMapper();
        ErrorResponse errorResponse = new ErrorResponse(
            401,
            "未授权访问",
            "请提供有效的JWT令牌",
            System.currentTimeMillis()
        );

        response.getWriter().write(mapper.writeValueAsString(errorResponse));
    }

    /**
     * 错误响应DTO
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class ErrorResponse {
        private Integer code;
        private String error;
        private String message;
        private Long timestamp;
    }
}
