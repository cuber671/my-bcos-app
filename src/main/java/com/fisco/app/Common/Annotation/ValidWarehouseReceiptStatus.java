package com.fisco.app.Common.Annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import com.fisco.app.Common.Annotation.impl.WarehouseReceiptStatusValidator;

/**
 * 仓单状态校验注解
 * 校验仓单状态值是否在允许的范围内
 *
 * 状态值说明：
 * - 1: 在库 (IN_STOCK)
 * - 2: 待转让 (PENDING_TRANSFER)
 * - 3: 已拆分/合并 (SPLIT_MERGED)
 * - 4: 已核销 (BURNED)
 * - 5: 物流转运中 (IN_TRANSIT)
 *
 * 使用示例：
 * <pre>
 * public class ReceiptCreateRequest {
 *     @ValidWarehouseReceiptStatus(message = "无效的仓单状态")
 *     private Integer status;
 * }
 * </pre>
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = WarehouseReceiptStatusValidator.class)
public @interface ValidWarehouseReceiptStatus {

    /**
     * 校验失败时的错误消息
     */
    String message() default "Invalid warehouse receipt status, valid values are: 1-IN_STOCK, 2-PENDING_TRANSFER, 3-SPLIT_MERGED, 4-BURNED, 5-IN_TRANSIT";

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
    boolean allowNull() default false;
}
