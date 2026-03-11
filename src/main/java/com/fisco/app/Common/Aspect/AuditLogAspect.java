package com.fisco.app.Common.Aspect;

import java.lang.reflect.Field;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import com.fisco.app.Common.Annotation.AuditLog;
import com.fisco.app.Common.Utils.AuditContext;

import lombok.extern.slf4j.Slf4j;

/**
 * 审计日志切面
 * 自动为标注了@AuditLog注解的方法填充operator_id字段
 *
 * 工作原理：
 * 1. 方法执行后：检查返回值，如果是Entity则填充operator_id
 * 2. 自动从AuditContext获取userId填充到entity的operatorId字段
 *
 * 使用示例：
 * <pre>
 * @AuditLog(module = "ENTERPRISE", operation = "更新企业信息")
 * @PutMapping("/enterprise/{entId}")
 * public Result&lt;Enterprise&gt; updateEnterprise(...) {
 *     // 返回的Enterprise对象会自动填充operatorId字段
 * }
 * </pre>
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Slf4j
@Aspect
@Component
public class AuditLogAspect {

    /**
     * 切点：匹配所有标注了@AuditLog注解的方法
     */
    @Pointcut("@annotation(com.fisco.app.Common.Annotation.AuditLog)")
    public void auditLogPointcut() {
    }

    /**
     * 环绕通知：执行方法并填充operator_id
     */
    @Around("auditLogPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 获取方法上的@AuditLog注解
        AuditLog auditLog = joinPoint.getTarget().getClass()
                .getMethod(joinPoint.getSignature().getName())
                .getAnnotation(AuditLog.class);

        // 如果注解获取失败，尝试从方法获取
        if (auditLog == null) {
            auditLog = joinPoint.getSignature().getName().contains("auditLog")
                    ? null : null;
        }

        // 2. 如果不需要自动填充operator_id，直接执行
        if (auditLog == null || !auditLog.autoFillOperator()) {
            return joinPoint.proceed();
        }

        // 3. 执行方法
        Object result = joinPoint.proceed();

        // 4. 方法执行成功后，尝试填充operator_id
        if (result != null) {
            fillOperatorId(result);
        }

        return result;
    }

    /**
     * 填充operator_id字段
     * 支持单对象和List包装的对象
     *
     * @param result 方法返回值
     */
    private void fillOperatorId(Object result) {
        Long userId = AuditContext.getUserId();
        if (userId == null) {
            log.debug("未获取到用户ID，跳过operator_id填充");
            return;
        }

        // 处理单个实体对象
        if (isEntity(result)) {
            fillEntityOperatorId(result, userId);
            return;
        }

        // 处理List包装的实体对象
        if (result instanceof java.util.List) {
            java.util.List<?> list = (java.util.List<?>) result;
            for (Object item : list) {
                if (isEntity(item)) {
                    fillEntityOperatorId(item, userId);
                }
            }
        }
    }

    /**
     * 判断是否为实体对象（有无operatorId字段）
     *
     * @param obj 对象
     * @return true=是实体
     */
    private boolean isEntity(Object obj) {
        if (obj == null) {
            return false;
        }
        try {
            obj.getClass().getDeclaredField("operatorId");
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    /**
     * 填充实体的operatorId字段
     *
     * @param entity 实体对象
     * @param userId 用户ID
     */
    private void fillEntityOperatorId(Object entity, Long userId) {
        try {
            Field field = entity.getClass().getDeclaredField("operatorId");
            field.setAccessible(true);

            // 检查是否已有值，避免覆盖
            Object existingValue = field.get(entity);
            if (existingValue != null) {
                log.debug("实体{}的operatorId已有值: {}，跳过填充",
                        entity.getClass().getSimpleName(), existingValue);
                return;
            }

            field.set(entity, userId);
            log.debug("成功填充operatorId: {} 到实体{}",
                    userId, entity.getClass().getSimpleName());
        } catch (Exception e) {
            log.warn("填充operatorId失败: {}", e.getMessage());
        }
    }
}
