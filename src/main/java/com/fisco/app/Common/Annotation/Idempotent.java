package com.fisco.app.Common.Annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口幂等性注解
 * 用于标记需要幂等性校验的写入接口
 *
 * 使用示例：
 * <pre>
 * // 默认配置：transactionId参数，24小时有效期
 * @Idempotent
 * @PostMapping("/api/create")
 * public Result&lt;?&gt; create(@RequestBody CreateRequest request) {
 *     // request.transactionId 作为幂等Key
 * }
 *
 * // 自定义配置
 * @Idempotent(transactionIdParam = "txId", expireHours = 12, required = false)
 * @PostMapping("/api/update")
 * public Result&lt;?&gt; update(@RequestBody UpdateRequest request) {
 *     // request.txId 作为幂等Key
 * }
 * </pre>
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /**
     * transactionId 参数名
     * 从请求参数或JSON Body中获取
     */
    String transactionIdParam() default "transactionId";

    /**
     * 幂等Key有效期（小时）
     * 超过此时间后相同的transactionId可以再次请求
     */
    int expireHours() default 24;

    /**
     * 幂等性校验失败时的提示消息
     */
    String message() default "重复请求，请勿重复提交";

    /**
     * transactionId是否必须
     * true: 缺少transactionId返回错误
     * false: 缺少transactionId放行（不进行幂等校验）
     */
    boolean required() default true;
}
