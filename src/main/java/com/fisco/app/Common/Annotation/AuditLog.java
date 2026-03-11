package com.fisco.app.Common.Annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 审计日志注解
 * 用于标记需要记录操作日志的方法，并自动填充operator_id字段
 *
 * 使用场景：
 * - 所有数据库更新操作（创建、更新、删除）
 * - 敏感业务操作（登录、登出、权限变更）
 * - 链上交易操作
 *
 * 使用示例：
 * <pre>
 * // 记录企业信息更新操作
 * @AuditLog(module = "ENTERPRISE", operation = "更新企业信息")
 * @PutMapping("/enterprise/{entId}")
 * public Result<?> updateEnterprise(@PathVariable Long entId, ...) { ... }
 *
 * // 记录用户删除操作
 * @AuditLog(module = "USER", operation = "删除用户", saveParams = true)
 * @DeleteMapping("/user/{userId}")
 * public Result<?> deleteUser(@PathVariable Long userId) { ... }
 * </pre>
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

    /**
     * 业务模块
     * 如：ENTERPRISE, USER, BILL, WAREHOUSE, BLOCKCHAIN等
     *
     * @return 模块名称
     */
    String module();

    /**
     * 操作类型
     * 如：创建、更新、删除、审核、签发、转让等
     *
     * @return 操作描述
     */
    String operation();

    /**
     * 是否保存请求参数
     * 开启后会序列化方法参数到audit_log表
     *
     * @return 是否保存参数
     */
    boolean saveParams() default false;

    /**
     * 是否保存响应结果
     * 开启后会序列化返回值到audit_log表
     *
     * @return 是否保存结果
     */
    boolean saveResult() default false;

    /**
     * 是否自动填充entity的operator_id字段
     * 开启后会自动从JWT中获取userId填充到entity的operatorId字段
     *
     * @return 是否自动填充operator_id
     */
    boolean autoFillOperator() default true;
}
