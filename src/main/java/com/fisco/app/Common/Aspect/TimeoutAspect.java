package com.fisco.app.Common.Aspect;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fisco.app.Common.Annotation.Timeout;
import com.fisco.app.Common.Utils.Result;

import lombok.extern.slf4j.Slf4j;

/**
 * 超时控制切面
 * 基于线程池 Future 实现方法级超时控制
 *
 * 超时规则：
 * - 默认超时 5 秒
 * - 支持自定义超时时间
 * - 超时后调用兜底方法或返回错误响应
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Slf4j
@Aspect
@Component
public class TimeoutAspect {

    /**
     * 超时执行线程池
     */
    private final ExecutorService timeoutExecutor = Executors.newCachedThreadPool();

    @Value("${timeout.default:5000}")
    private int defaultTimeout;

    /**
     * 拦截带有 @Timeout 注解的方法
     */
    @Around("@annotation(com.fisco.app.Common.Annotation.Timeout)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        Timeout annotation = method.getAnnotation(Timeout.class);
        if (annotation == null) {
            return joinPoint.proceed();
        }

        // 获取超时时间
        int timeoutMs = annotation.value();
        if (timeoutMs <= 0) {
            timeoutMs = defaultTimeout;
        }

        log.debug("超时控制 - 方法: {}, 超时时间: {}ms", method.getName(), timeoutMs);

        // 使用 Future 实现超时控制
        return executeWithTimeout(joinPoint, timeoutMs, annotation);
    }

    /**
     * 使用 Future 实现超时控制
     */
    private Object executeWithTimeout(ProceedingJoinPoint joinPoint, int timeoutMs, Timeout annotation)
            throws Throwable {

        Future<Object> future = timeoutExecutor.submit(() -> {
            try {
                return joinPoint.proceed();
            } catch (Throwable t) {
                throw new Exception(t);
            }
        });

        try {
            // 等待结果，带超时
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            // 超时，取消任务
            future.cancel(true);
            log.warn("方法执行超时: {}, 超时时间: {}ms", joinPoint.getSignature().getName(), timeoutMs);

            // 处理兜底方法
            return handleFallback(joinPoint, annotation, "Method execution timeout");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("方法执行被中断: {}", joinPoint.getSignature().getName());
            throw e;
        } catch (java.util.concurrent.ExecutionException e) {
            // 任务执行异常
            log.error("方法执行异常: {}, error: {}", joinPoint.getSignature().getName(), e.getMessage());
            throw e.getCause();
        }
    }

    /**
     * 处理兜底响应
     */
    private Object handleFallback(ProceedingJoinPoint joinPoint, Timeout annotation, String reason) {
        String fallbackMethodName = annotation.fallbackMethod();

        if (fallbackMethodName != null && !fallbackMethodName.isEmpty()) {
            // 尝试调用兜底方法
            try {
                MethodSignature signature = (MethodSignature) joinPoint.getSignature();
                Method method = signature.getMethod();
                Object target = joinPoint.getTarget();

                // 查找兜底方法
                Method fallbackMethod = findFallbackMethod(target, method, fallbackMethodName);
                if (fallbackMethod != null) {
                    return fallbackMethod.invoke(target, joinPoint.getArgs());
                }
            } catch (Exception e) {
                log.error("兜底方法执行失败: {}", e.getMessage());
            }
        }

        // 返回默认超时响应
        return Result.error(40003, annotation.message());
    }

    /**
     * 查找兜底方法
     */
    private Method findFallbackMethod(Object target, Method originalMethod, String fallbackMethodName) {
        Class<?>[] paramTypes = originalMethod.getParameterTypes();
        try {
            return target.getClass().getMethod(fallbackMethodName, paramTypes);
        } catch (NoSuchMethodException e) {
            log.warn("未找到兜底方法: {}", fallbackMethodName);
            return null;
        }
    }
}
