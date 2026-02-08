package com.fisco.app.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;


/**
 * HTTP请求日志切面
 * 自动记录所有Controller的HTTP请求、响应和执行时间
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RequestLoggingAspect {

    private final ObjectMapper objectMapper;

    /**
     * 定义切点：拦截所有Controller
     */
    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void controllerPointcut() {
    }

    /**
     * 环绕通知：记录请求和响应日志
     */
    @Around("controllerPointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取请求信息
        ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        // 获取方法信息
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();

        // 获取当前用户
        String username = getCurrentUsername();

        // 记录请求开始
        if (request != null) {
            log.info(">>> 请求开始: method={}, uri={}, class={}, method={}, user={}, ip={}",
                request.getMethod(),
                request.getRequestURI(),
                className,
                methodName,
                username,
                getClientIp(request)
            );

            // 记录请求参数（DEBUG级别）
            if (log.isDebugEnabled()) {
                logRequestParams(joinPoint.getArgs());
            }
        }

        Object result = null;
        Throwable exception = null;

        try {
            // 执行目标方法
            result = joinPoint.proceed();

            // 记录成功响应
            long duration = System.currentTimeMillis() - startTime;
            log.info("<<< 请求成功: method={}, uri={}, duration={}ms",
                request != null ? request.getMethod() : "UNKNOWN",
                request != null ? request.getRequestURI() : "UNKNOWN",
                duration
            );

            // 性能警告
            if (duration > 3000) {
                log.warn("⚠️  慢请求: method={}, uri={}, duration={}ms",
                    request != null ? request.getMethod() : "UNKNOWN",
                    request != null ? request.getRequestURI() : "UNKNOWN",
                    duration
                );
            }

            return result;

        } catch (Throwable e) {
            exception = e;
            long duration = System.currentTimeMillis() - startTime;

            // 记录异常
            log.error("✗ 请求失败: method={}, uri={}, class={}, method={}, user={}, duration={}ms, error={}",
                request != null ? request.getMethod() : "UNKNOWN",
                request != null ? request.getRequestURI() : "UNKNOWN",
                className,
                methodName,
                username,
                duration,
                e.getMessage(),
                e
            );

            throw exception;
        }
    }

    /**
     * 获取当前登录用户名
     */
    private String getCurrentUsername() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                return authentication.getName();
            }
        } catch (Exception e) {
            log.debug("Failed to get current user", e);
        }
        return "anonymous";
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 处理多个IP的情况（取第一个）
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    /**
     * 记录请求参数（DEBUG级别）
     */
    private void logRequestParams(Object[] args) {
        if (args == null || args.length == 0) {
            return;
        }

        try {
            for (int i = 0; i < args.length; i++) {
                if (args[i] != null) {
                    // 跳过这些对象类型
                    if (args[i] instanceof HttpServletRequest ||
                        args[i] instanceof Authentication) {
                        continue;
                    }

                    // 脱敏处理
                    String paramStr = objectMapper.writeValueAsString(args[i]);
                    paramStr = maskSensitiveData(paramStr);

                    log.debug("  参数[{}]: {}", i, paramStr);
                }
            }
        } catch (Exception e) {
            log.debug("Failed to log request params", e);
        }
    }

    /**
     * 脱敏处理
     */
    private String maskSensitiveData(String data) {
        if (data == null) {
            return null;
        }

        // 脱敏密码字段
        return data.replaceAll("\"password\":\"[^\"]*\"", "\"password\":\"******\"")
                   .replaceAll("\"secret\":\"[^\"]*\"", "\"secret\":\"******\"");
    }
}
