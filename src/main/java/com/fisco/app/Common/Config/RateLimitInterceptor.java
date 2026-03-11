package com.fisco.app.Common.Config;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.fisco.app.Common.Annotation.RateLimit;

import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;

/**
 * 限流拦截器
 * 基于Guava RateLimiter实现接口限流
 *
 * 限流规则：
 * - 按IP + 用户ID + 接口维度限流
 * - 支持自定义QPS配置
 * - 超限返回429 Too Many Requests
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    /**
     * RateLimiter缓存
     * key格式：ip_userId_key
     */
    private final ConcurrentHashMap<String, RateLimiter> rateLimiterCache = new ConcurrentHashMap<>();

    @Value("${rate-limit.default-query-qps:10}")
    private int defaultQueryQps;

    @Value("${rate-limit.default-write-qps:2}")
    private int defaultWriteQps;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, Object handler)
            throws Exception {

        // 只对HandlerMethod进行拦截（Controller方法）
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;

        // 检查方法是否有@RateLimit注解
        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (rateLimit == null) {
            rateLimit = handlerMethod.getBeanType().getAnnotation(RateLimit.class);
        }

        // 如果没有限流注解，放行
        if (rateLimit == null) {
            return true;
        }

        // 获取限流key
        String rateLimitKey = getRateLimitKey(request, rateLimit.key());

        // 获取或创建RateLimiter
        int configuredQps = rateLimit.qps();
        final int qps;
        if (configuredQps <= 0) {
            // 根据key类型确定默认QPS
            qps = getDefaultQps(rateLimit.key());
        } else {
            qps = configuredQps;
        }

        final int finalQps = qps;
        RateLimiter rateLimiter = rateLimiterCache.computeIfAbsent(rateLimitKey,
                k -> RateLimiter.create(finalQps));

        // 尝试获取令牌
        double waitTime = rateLimiter.acquire();
        if (waitTime > 0) {
            log.warn("限流触发 - key: {}, waitTime: {}ms", rateLimitKey, waitTime * 1000);
            sendTooManyRequestsResponse(response, rateLimit.message());
            return false;
        }

        log.debug("限流检查通过 - key: {}", rateLimitKey);
        return true;
    }

    /**
     * 构建限流key
     * 格式：ip_userId_key
     */
    private String getRateLimitKey(HttpServletRequest request, String key) {
        String ip = getClientIp(request);
        String userId = getUserId(request);

        StringBuilder sb = new StringBuilder();
        sb.append(ip);
        if (userId != null && !userId.isEmpty()) {
            sb.append("_").append(userId);
        }
        if (key != null && !key.isEmpty()) {
            sb.append("_").append(key);
        }

        return sb.toString();
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 获取用户ID（如果已登录）
     */
    private String getUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("user_id");
        return userId != null ? userId.toString() : null;
    }

    /**
     * 根据key类型获取默认QPS
     */
    private int getDefaultQps(String key) {
        if (key == null || key.isEmpty()) {
            return defaultQueryQps;
        }
        if (key.toLowerCase().contains("write") ||
            key.toLowerCase().contains("post") ||
            key.toLowerCase().contains("put") ||
            key.toLowerCase().contains("delete")) {
            return defaultWriteQps;
        }
        return defaultQueryQps;
    }

    /**
     * 发送429响应
     */
    private void sendTooManyRequestsResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> error = new HashMap<>();
        error.put("code", 429);
        error.put("message", message);

        response.getWriter().write(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(error));
    }
}
