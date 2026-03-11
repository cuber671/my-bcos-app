package com.fisco.app.Common.Annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 熔断保护注解
 * 用于标记需要熔断保护的方法
 *
 * 使用示例：
 * <pre>
 * // 区块链调用熔断保护
 * @CircuitProtection(name = "blockchain", fallbackMethod = "fallback")
 * @PostMapping("/blockchain/call")
 * public Result&lt;?&gt; callContract(...) { ... }
 *
 * // 兜底方法
 * public Result&lt;?&gt; fallback(...) {
 *     return Result.error(40004, "Service temporarily unavailable");
 * }
 * </pre>
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CircuitProtection {

    /**
     * 熔断器名称
     * 用于区分不同接口的熔断规则
     */
    String name() default "default";

    /**
     * 失败率阈值
     * 超过此失败率时触发熔断
     * 默认 50%
     */
    int failureRateThreshold() default 50;

    /**
     * 熔断持续时间（毫秒）
     * 熔断开启后多长时间内拒绝请求
     * 默认 30秒
     */
    int waitDurationInOpenState() default 30000;

    /**
     * 滑动窗口大小
     * 用于计算失败率的调用数量
     * 默认 100
     */
    int slidingWindowSize() default 100;

    /**
     * 最小调用次数
     * 达到此次数后才开始计算失败率
     * 默认 10
     */
    int minimumNumberOfCalls() default 10;

    /**
     * 兜底方法名称
     * 熔断触发时调用的备用方法
     */
    String fallbackMethod() default "";

    /**
     * 熔断提示消息
     */
    String message() default "Service temporarily unavailable, please try again later";
}
