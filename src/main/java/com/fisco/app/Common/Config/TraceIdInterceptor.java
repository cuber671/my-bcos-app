package com.fisco.app.Common.Config;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

/**
 * TraceId拦截器 - 为每个请求生成唯一链路ID
 * 用于全链路追踪，日志中会自动包含traceId
 */
@Component
public class TraceIdInterceptor implements HandlerInterceptor {

    public static final String TRACE_ID = "traceId";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 优先从请求头获取traceId，否则生成新的
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            traceId = generateTraceId();
        }

        // 将traceId放入MDC上下文
        MDC.put(TRACE_ID, traceId);

        // 将traceId设置到响应头，方便前端追踪
        response.setHeader(TRACE_ID_HEADER, traceId);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求完成后清除MDC中的traceId
        MDC.remove(TRACE_ID);
    }

    /**
     * 生成traceId - 使用UUID简化版（去掉横线）
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
