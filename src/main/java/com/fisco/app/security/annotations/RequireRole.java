package com.fisco.app.security.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 要求特定角色注解
 * 用于方法级别，限制只有特定角色的用户才能访问
 *
 * 使用示例：
 * @RequireRole("ADMIN")
 * public void adminOnlyMethod() {
 *     // 方法实现
 * }
 *
 * 支持多个角色：
 * @RequireRole({"ADMIN", "ENTERPRISE_ADMIN"})
 * public void managerMethod() {
 *     // 方法实现
 * }
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    /**
     * 允许的角色列表
     */
    String[] value();

    /**
     * 权限检查失败时的错误消息
     */
    String message() default "需要特定角色权限才能访问";
}
