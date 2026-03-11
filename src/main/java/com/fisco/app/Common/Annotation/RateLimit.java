package com.fisco.app.Common.Annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 限流注解
 * 用于标记需要限流的接口
 *
 * 使用示例：
 * <pre>
 * // 查询接口限流10 QPS
 * @RateLimit(qps = 10, key = "query")
 * @GetMapping("/api/query")
 * public Result&lt;?&gt; query(...) { ... }
 *
 * // 写入接口限流2 QPS
 * @RateLimit(qps = 2, key = "write")
 * @PostMapping("/api/write")
 * public Result&lt;?&gt; write(...) { ... }
 * </pre>
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 每秒允许的请求数（QPS）
     * 默认10 QPS
     */
    int qps() default 10;

    /**
     * 限流维度键
     * 用于区分不同接口的限流规则
     * 如：query, write, api等
     */
    String key() default "";

    /**
     * 限流提示消息
     */
    String message() default "Too many requests, please try again later";

    /**
     * 限流超时时间（毫秒）
     * 获取令牌超时后返回429
     */
    int timeout() default 0;
}
