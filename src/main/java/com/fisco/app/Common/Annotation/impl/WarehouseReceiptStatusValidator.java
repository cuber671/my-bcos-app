package com.fisco.app.Common.Annotation.impl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.fisco.app.Common.Annotation.ValidWarehouseReceiptStatus;
import com.fisco.app.Modules.Warehouse.Entity.WarehouseReceipt;

/**
 * 仓单状态校验器实现
 * 校验仓单状态值是否在允许的范围内
 *
 * 允许的状态值：
 * - 1: 在库 (IN_STOCK)
 * - 2: 待转让 (PENDING_TRANSFER)
 * - 3: 已拆分/合并 (SPLIT_MERGED)
 * - 4: 已核销 (BURNED)
 * - 5: 物流转运中 (IN_TRANSIT)
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
public class WarehouseReceiptStatusValidator implements ConstraintValidator<ValidWarehouseReceiptStatus, Integer> {

    /**
     * 有效的仓单状态值集合
     */
    private static final Set<Integer> VALID_STATUSES = new HashSet<>(Arrays.asList(
            WarehouseReceipt.STATUS_IN_STOCK,
            WarehouseReceipt.STATUS_PENDING_TRANSFER,
            WarehouseReceipt.STATUS_SPLIT_MERGED,
            WarehouseReceipt.STATUS_BURNED,
            WarehouseReceipt.STATUS_IN_TRANSIT
    ));

    /**
     * 是否允许为空
     */
    private boolean allowNull;

    @Override
    public void initialize(ValidWarehouseReceiptStatus constraintAnnotation) {
        this.allowNull = constraintAnnotation.allowNull();
    }

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        // 如果允许为空，且值为空，则校验通过
        if (allowNull && value == null) {
            return true;
        }

        // 如果不允许为空，且值为空，则校验失败
        if (!allowNull && value == null) {
            return false;
        }

        // 校验状态值是否在有效范围内
        boolean valid = VALID_STATUSES.contains(value);

        // 如果校验失败，定制错误消息
        if (!valid && context != null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Invalid warehouse receipt status: " + value + ", valid values are: " +
                    "1-IN_STOCK, 2-PENDING_TRANSFER, 3-SPLIT_MERGED, 4-BURNED, 5-IN_TRANSIT"
            ).addConstraintViolation();
        }

        return valid;
    }
}
