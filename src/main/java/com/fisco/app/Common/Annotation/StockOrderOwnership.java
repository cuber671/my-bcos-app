package com.fisco.app.Common.Annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 入库单归属校验注解
 * 用于校验当前操作的入库单是否属于当前登录企业
 *
 * 使用场景：
 * - 防止用户越权操作他人入库单
 * - 确保企业用户只能操作自己企业的入库单
 *
 * 使用示例：
 * <pre>
 * // 校验路径参数中的 stockOrderId 对应的入库单是否属于当前用户
 * @StockOrderOwnership(paramName = "stockOrderId")
 * @GetMapping("/stock-in/{stockOrderId}")
 * public Result<?> getStockOrder(@PathVariable Long stockOrderId) { ... }
 * </pre>
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface StockOrderOwnership {

    /**
     * 要校验的入库单ID参数名
     *
     * @return 参数名（如 stockOrderId）
     */
    String paramName() default "stockOrderId";

    /**
     * 是否允许系统管理员(scope=1)绕过校验
     *
     * @return 是否允许系统管理员绕过
     */
    boolean adminBypass() default true;

    /**
     * 校验失败时的错误消息
     *
     * @return 错误消息
     */
    String errorMessage() default "";
}
