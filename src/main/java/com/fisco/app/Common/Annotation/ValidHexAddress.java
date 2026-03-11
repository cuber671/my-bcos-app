package com.fisco.app.Common.Annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import com.fisco.app.Common.Annotation.impl.HexAddressValidator;

/**
 * 区块链地址校验注解
 * 校验区块链地址是否符合42位hex格式（0x开头 + 40位十六进制字符）
 *
 * 使用示例：
 * <pre>
 * public class TransactionRequest {
 *     @ValidHexAddress(message = "无效的发起方地址")
 *     private String fromAddress;
 *
 *     @ValidHexAddress(message = "无效的目标地址")
 *     private String toAddress;
 * }
 * </pre>
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = HexAddressValidator.class)
public @interface ValidHexAddress {

    /**
     * 校验失败时的错误消息
     */
    String message() default "Invalid blockchain address format";

    /**
     * 所属分组
     */
    Class<?>[] groups() default {};

    /**
     * 负载
     */
    Class<? extends Payload>[] payload() default {};

    /**
     * 是否允许为空
     * 设为false时，@NotNull会自动生效
     */
    boolean allowNull() default true;
}
