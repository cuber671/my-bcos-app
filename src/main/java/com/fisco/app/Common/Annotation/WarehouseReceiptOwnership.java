package com.fisco.app.Common.Annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 仓单归属校验注解
 * 用于校验当前操作的仓单是否属于当前登录企业
 *
 * 使用场景：
 * - 防止用户越权操作他人仓单
 * - 确保企业用户只能操作自己企业的仓单
 *
 * 使用示例：
 * <pre>
 * // 校验路径参数中的 receiptId 对应的仓单是否属于当前用户
 * @WarehouseReceiptOwnership(paramName = "receiptId")
 * @PostMapping("/receipt/split")
 * public Result<?> splitReceipt(@PathVariable Long receiptId) { ... }
 *
 * // 校验请求体中的 receiptId
 * @WarehouseReceiptOwnership(paramName = "receiptId", fromBody = true)
 * @PostMapping("/endorsement/launch")
 * public Result<?> launchEndorsement(@RequestBody EndorsementRequest request) { ... }
 * </pre>
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface WarehouseReceiptOwnership {

    /**
     * 要校验的仓单ID参数名
     * 从请求参数或请求体中获取该参数值，与数据库中的owner_ent_id进行比对
     *
     * @return 参数名（如 receiptId, receipt_id）
     */
    String paramName() default "receiptId";

    /**
     * 是否从请求体中获取参数值
     * 当为true时，从JSON请求体中获取参数
     *
     * @return 是否从请求体获取
     */
    boolean fromBody() default false;

    /**
     * 是否允许系统管理员(scope=1)绕过校验
     * 系统管理员可以操作所有仓单
     *
     * @return 是否允许系统管理员绕过
     */
    boolean adminBypass() default true;

    /**
     * 校验失败时的错误消息
     *
     * @return 错误消息
     */
    String errorMessage() default "";
}
