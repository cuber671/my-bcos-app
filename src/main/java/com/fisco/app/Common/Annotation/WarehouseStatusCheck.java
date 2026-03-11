package com.fisco.app.Common.Annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 仓单状态校验注解
 * 用于校验仓单状态是否满足操作条件
 *
 * 状态说明：
 * - 1: 在库 (IN_STOCK)
 * - 2: 待转让 (PENDING_TRANSFER)
 * - 3: 已拆分/合并 (SPLIT_OR_MERGED)
 * - 4: 已核销 (BURNED)
 * - 5: 物流转运中 (IN_TRANSIT)
 *
 * 使用场景：
 * - 拆分/合并操作需要 is_locked=false, status=1
 * - 背书转让需要 is_locked=false, status=1 或 status=2
 * - 核销需要 is_locked=false
 *
 * 使用示例：
 * <pre>
 * // 校验仓单未锁定且状态为在库
 * @WarehouseStatusCheck(paramName = "receiptId", requiredLocked = false, requiredStatus = {1})
 * @PostMapping("/receipt/split")
 * public Result<?> splitReceipt(@PathVariable Long receiptId) { ... }
 *
 * // 只校验未锁定
 * @WarehouseStatusCheck(paramName = "receiptId", requiredLocked = false)
 * @PostMapping("/endorsement/launch")
 * public Result<?> launchEndorsement(@PathVariable Long receiptId) { ... }
 * </pre>
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface WarehouseStatusCheck {

    /**
     * 要校验的仓单ID参数名
     *
     * @return 参数名（如 receiptId）
     */
    String paramName() default "receiptId";

    /**
     * 是否要求仓单处于未锁定状态
     * 质押锁定时 is_locked=true，此时禁止拆分、转让、核销
     *
     * @return 是否要求未锁定，设置为false时不校验锁定状态
     */
    boolean requiredLocked() default false;

    /**
     * 要求仓单状态必须为以下值之一
     * 空数组表示不校验状态
     *
     * @return 允许的状态值数组
     */
    int[] requiredStatus() default {};

    /**
     * 校验失败时的错误消息
     *
     * @return 错误消息
     */
    String errorMessage() default "";
}
