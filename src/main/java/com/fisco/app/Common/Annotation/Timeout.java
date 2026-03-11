package com.fisco.app.Common.Annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 超时控制注解
 * 用于标记需要超时控制的方法
 *
 * 使用示例：
 * <pre>
 * // 默认超时 5 秒
 * @Timeout
 * @PostMapping("/api/create")
 * public Result&lt;?&gt; create(...) { ... }
 *
 * // 自定义超时 3 秒
 * @Timeout(value = 3000)
 * @PostMapping("/blockchain/call")
 * public Result&lt;?&gt; callContract(...) { ... }
 *
 * // 自定义超时 + 兜底方法
 * @Timeout(value = 5000, fallbackMethod = "timeoutFallback")
 * public Result&lt;?&gt; timeoutFallback(...) {
 *     return Result.error(40003, "请求超时，请稍后重试");
 * }
 * </pre>
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Timeout {

    /**
     * 超时时间（毫秒）
     * 默认 5 秒
     */
    int value() default 5000;

    /**
     * 超时兜底方法名称
     * 超时触发时调用的备用方法
     */
    String fallbackMethod() default "";

    /**
     * 超时提示消息
     */
    String message() default "请求超时，请稍后重试";
}
