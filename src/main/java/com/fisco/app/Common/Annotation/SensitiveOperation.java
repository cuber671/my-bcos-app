package com.fisco.app.Common.Annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 敏感操作校验注解
 * 用于标记需要二次校验的敏感操作
 *
 * 适用场景：
 * - 发起链上交易
 * - 修改资产归属
 * - 大额操作
 * - 企业信息修改
 *
 * 使用示例：
 * <pre>
 * // 链上交易需要二次校验
 * @SensitiveOperation("发起链上交易")
 * @PostMapping("/blockchain/transaction")
 * public Result<?> sendTransaction(...) { ... }
 *
 * // 修改资产归属需要二次校验
 * @SensitiveOperation("修改资产归属")
 * @PostMapping("/asset/transfer")
 * public Result<?> transferAsset(...) { ... }
 * </pre>
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SensitiveOperation {

    /**
     * 操作类型描述
     * 用于日志记录和验证码类型区分
     *
     * @return 操作描述
     */
    String value();

    /**
     * 验证码有效期（秒）
     * 验证码生成后多长时间内有效
     *
     * @return 有效期（默认300秒=5分钟）
     */
    int validitySeconds() default 300;

    /**
     * 验证码类型
     * 用于区分不同类型的验证码
     *
     * @return 验证码类型
     */
    String codeType() default "sensitive";

    /**
     * 是否允许系统管理员(scope=1)绕过二次校验
     * 生产环境建议关闭
     *
     * @return 是否允许绕过
     */
    boolean adminBypass() default false;
}
