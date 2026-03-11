package com.fisco.app.Common.Aspect;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import com.fisco.app.Common.Annotation.CircuitProtection;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import lombok.extern.slf4j.Slf4j;

/**
 * 熔断切面
 * 基于Resilience4j实现方法级别的熔断保护
 *
 * 熔断规则：
 * - 失败率超过阈值时触发熔断
 * - 熔断期间直接返回兜底响应
 * - 熔断持续时间后进入半开状态
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Slf4j
@Aspect
@Component
public class CircuitBreakerAspect {

    /**
     * Resilience4j CircuitBreaker 缓存
     */
    private final ConcurrentHashMap<String, CircuitBreaker> cbCache = new ConcurrentHashMap<>();

    /**
     * 拦截带有 @CircuitProtection 注解的方法
     */
    @Around("@annotation(com.fisco.app.Common.Annotation.CircuitProtection)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        CircuitProtection annotation = method.getAnnotation(CircuitProtection.class);
        if (annotation == null) {
            return joinPoint.proceed();
        }

        String circuitBreakerName = annotation.name();
        CircuitBreaker circuitBreaker = getOrCreateCircuitBreaker(circuitBreakerName, annotation);

        // 执行熔断保护
        return executeWithCircuitBreaker(joinPoint, circuitBreaker, annotation);
    }

    /**
     * 获取或创建熔断器
     */
    private CircuitBreaker getOrCreateCircuitBreaker(String name, CircuitProtection annotation) {
        String key = name + "_" + annotation.failureRateThreshold() + "_" + annotation.waitDurationInOpenState();

        return cbCache.computeIfAbsent(key, k -> {
            // 创建自定义配置
            CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                    .failureRateThreshold(annotation.failureRateThreshold())
                    .waitDurationInOpenState(java.time.Duration.ofMillis(annotation.waitDurationInOpenState()))
                    .slidingWindowSize(annotation.slidingWindowSize())
                    .minimumNumberOfCalls(annotation.minimumNumberOfCalls())
                    .permittedNumberOfCallsInHalfOpenState(3)
                    .automaticTransitionFromOpenToHalfOpenEnabled(true)
                    .build();

            CircuitBreaker cb = CircuitBreaker.of(name, config);

            // 注册状态转换监听器
            cb.getEventPublisher()
                    .onStateTransition(event -> log.warn("熔断器状态转换: {} -> {}",
                            event.getStateTransition().getFromState(),
                            event.getStateTransition().getToState()));

            cb.getEventPublisher()
                    .onFailureRateExceeded(event -> log.error("熔断器失败率超过阈值: {}%, 触发熔断",
                            event.getFailureRate()));

            return cb;
        });
    }

    /**
     * 使用熔断器执行方法
     */
    private Object executeWithCircuitBreaker(ProceedingJoinPoint joinPoint,
                                             CircuitBreaker circuitBreaker,
                                             CircuitProtection annotation) throws Throwable {

        String methodName = joinPoint.getSignature().getName();

        // 检查熔断器状态
        CircuitBreaker.State state = circuitBreaker.getState();
        if (state == CircuitBreaker.State.OPEN) {
            log.warn("熔断器开启 - 方法: {}, 熔断器: {}", methodName, circuitBreaker.getName());
            return handleFallback(joinPoint, annotation, "Circuit breaker is OPEN");
        }

        // 执行并捕获结果
        try {
            return circuitBreaker.executeSupplier(() -> {
                try {
                    return joinPoint.proceed();
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            });
        } catch (io.github.resilience4j.circuitbreaker.CallNotPermittedException e) {
            // 熔断开启时捕获异常
            log.error("熔断器拒绝调用 - 方法: {}, 熔断器: {}", methodName, circuitBreaker.getName());
            return handleFallback(joinPoint, annotation, "Call not permitted due to open circuit");
        } catch (Exception e) {
            // 记录失败
            log.error("熔断器执行异常 - 方法: {}, 错误: {}", methodName, e.getMessage());
            throw e;
        }
    }

    /**
     * 处理兜底响应
     */
    private Object handleFallback(ProceedingJoinPoint joinPoint,
                                  CircuitProtection annotation,
                                  String reason) {
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

        // 返回默认兜底响应
        return com.fisco.app.Common.Utils.Result.error(40004, annotation.message());
    }

    /**
     * 查找兜底方法
     * 支持多种匹配策略：精确匹配、无参匹配、参数兼容匹配
     */
    private Method findFallbackMethod(Object target, Method originalMethod, String fallbackMethodName) {
        Class<?>[] paramTypes = originalMethod.getParameterTypes();

        // 1. 尝试精确匹配
        try {
            return target.getClass().getMethod(fallbackMethodName, paramTypes);
        } catch (NoSuchMethodException ignored) {
        }

        // 2. 尝试无参方法
        try {
            return target.getClass().getMethod(fallbackMethodName);
        } catch (NoSuchMethodException ignored) {
        }

        // 3. 遍历所有方法，尝试参数兼容匹配
        for (Method m : target.getClass().getMethods()) {
            if (m.getName().equals(fallbackMethodName)) {
                if (isParamsCompatible(m.getParameterTypes(), paramTypes)) {
                    return m;
                }
            }
        }

        log.warn("未找到兜底方法: {}", fallbackMethodName);
        return null;
    }

    /**
     * 检查参数类型是否兼容
     * 兜底方法参数可以是原方法参数的父类型或相同类型
     */
    private boolean isParamsCompatible(Class<?>[] fallbackParams, Class<?>[] originalParams) {
        if (fallbackParams.length != originalParams.length) {
            return false;
        }
        for (int i = 0; i < fallbackParams.length; i++) {
            // 兜底方法参数必须是原方法参数的父类型或相同类型
            if (!fallbackParams[i].isAssignableFrom(originalParams[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * 清理指定熔断器缓存
     * 用于测试隔离或手动重置熔断器状态
     *
     * @param name 熔断器名称
     */
    public void clearCircuitBreaker(String name) {
        cbCache.entrySet().removeIf(entry -> entry.getKey().startsWith(name + "_"));
        log.info("已清理熔断器缓存: {}", name);
    }

    /**
     * 清理所有熔断器缓存
     * 用于测试隔离或完全重置熔断器状态
     */
    public void clearAllCircuitBreakers() {
        cbCache.clear();
        log.info("已清理所有熔断器缓存");
    }

    /**
     * 获取当前熔断器缓存状态（用于监控）
     *
     * @return 熔断器缓存信息
     */
    public java.util.Map<String, Object> getCircuitBreakerStatus() {
        java.util.Map<String, Object> status = new java.util.HashMap<>();
        status.put("size", cbCache.size());
        java.util.Map<String, String> details = new java.util.HashMap<>();
        cbCache.forEach((key, cb) -> {
            details.put(key, cb.getState().toString());
        });
        status.put("details", details);
        return status;
    }
}
