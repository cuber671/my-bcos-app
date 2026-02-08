package com.fisco.app.aspect;

import com.fisco.app.entity.system.AuditLog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fisco.app.annotation.Audited;
import com.fisco.app.repository.system.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;


/**
 * 审计日志切面
 * 自动记录带有 @Audited 注解的方法调用
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * 定义切点：拦截所有带 @Audited 注解的方法
     */
    @Pointcut("@annotation(com.fisco.app.annotation.Audited)")
    public void auditLogPointcut() {
    }

    /**
     * 环绕通知：记录审计日志
     */
    @Around("auditLogPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 获取 @Audited 注解
        Audited annotation = method.getAnnotation(Audited.class);
        if (annotation == null) {
            return joinPoint.proceed();
        }

        // 创建审计日志对象
        AuditLog auditLog = new AuditLog();
        auditLog.setModule(annotation.module());
        auditLog.setActionType(annotation.actionType());
        auditLog.setActionDesc(annotation.actionDesc());
        auditLog.setEntityType(annotation.entityType());

        try {
            // 获取请求信息
            HttpServletRequest request = getRequest();
            if (request != null) {
                auditLog.setRequestMethod(request.getMethod());
                auditLog.setRequestUrl(request.getRequestURI());
                auditLog.setRequestIp(getClientIp(request));
                auditLog.setUserAgent(request.getHeader("User-Agent"));
            }

            // 获取当前用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                auditLog.setUserAddress(authentication.getName());
            }

            // 记录请求参数
            if (annotation.logRequest()) {
                Object[] args = joinPoint.getArgs();
                try {
                    // 过滤掉敏感参数（如密码）
                    Map<String, Object> params = filterSensitiveData(args);
                    auditLog.setOldValue(objectMapper.writeValueAsString(params));
                } catch (Exception e) {
                    log.warn("Failed to serialize request params", e);
                }
            }

            // 执行目标方法
            Object result = joinPoint.proceed();

            // 记录响应结果
            if (annotation.logResponse() && result != null) {
                try {
                    String resultJson = objectMapper.writeValueAsString(result);
                    // 限制长度
                    if (resultJson.length() > 10000) {
                        resultJson = resultJson.substring(0, 10000) + "...";
                    }
                    auditLog.setNewValue(resultJson);
                } catch (Exception e) {
                    log.warn("Failed to serialize response", e);
                }
            }

            // 提取实体ID（如果返回的是实体对象）
            if (result != null) {
                String entityId = extractEntityId(result);
                if (entityId != null) {
                    auditLog.setEntityId(entityId);
                }
            }

            auditLog.setIsSuccess(true);
            auditLog.setResult("SUCCESS");

            return result;

        } catch (Exception e) {
            // 记录失败信息
            auditLog.setIsSuccess(false);
            auditLog.setResult("FAILURE");
            auditLog.setErrorMessage(e.getMessage());

            throw e;

        } finally {
            // 计算操作时长
            long duration = System.currentTimeMillis() - startTime;
            auditLog.setDuration(duration);

            // 异步保存审计日志（避免影响主业务）
            try {
                auditLogRepository.save(auditLog);
                log.debug("Audit log saved: module={}, action={}, duration={}ms",
                    auditLog.getModule(), auditLog.getActionType(), duration);
            } catch (Exception e) {
                log.error("Failed to save audit log", e);
            }
        }
    }

    /**
     * 获取当前请求
     */
    private HttpServletRequest getRequest() {
        try {
            ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
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
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
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
     * 过滤敏感数据
     */
    private Map<String, Object> filterSensitiveData(Object[] args) {
        Map<String, Object> filtered = new HashMap<>();
        if (args == null || args.length == 0) {
            return filtered;
        }

        for (int i = 0; i < args.length; i++) {
            if (args[i] != null) {
                // 跳过 Authentication 和 HttpServletRequest 对象
                if (args[i] instanceof Authentication || args[i] instanceof HttpServletRequest) {
                    continue;
                }

                String paramName = "arg" + i;
                Object paramValue = args[i];

                // 敏感字段脱敏
                if (paramValue != null) {
                    String paramStr = paramValue.toString();
                    if (paramStr.contains("password") || paramStr.contains("secret")) {
                        paramValue = "******";
                    }
                }

                filtered.put(paramName, paramValue);
            }
        }

        return filtered;
    }

    /**
     * 从返回对象中提取实体ID
     */
    private String extractEntityId(Object result) {
        if (result == null) {
            return null;
        }

        try {
            // 尝试通过反射获取 ID、billId、receiptId 等字段
            String[] idFields = {"id", "billId", "receiptId", "receivableId", "address"};

            for (String fieldName : idFields) {
                try {
                    java.lang.reflect.Field field = result.getClass().getDeclaredField(fieldName);
                    field.setAccessible(true);
                    Object value = field.get(result);
                    if (value != null) {
                        return value.toString();
                    }
                } catch (NoSuchFieldException e) {
                    // 继续尝试下一个字段
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract entity ID", e);
        }

        return null;
    }
}
