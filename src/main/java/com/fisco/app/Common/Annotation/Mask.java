package com.fisco.app.Common.Annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.fisco.app.Common.Utils.DataMaskingUtil.MaskType;

/**
 * 数据脱敏注解
 * 用于标记需要脱敏的字段
 *
 * 使用示例：
 * <pre>
 * // 实体类字段
 * @Mask(MaskType.PHONE)
 * private String phoneNumber;
 *
 * @Mask(MaskType.ID_CARD)
 * private String idCard;
 *
 * @Mask(MaskType.WALLET_ADDRESS)
 * private String walletAddress;
 * </pre>
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Mask {

    /**
     * 脱敏类型
     */
    MaskType value() default MaskType.PHONE;
}
