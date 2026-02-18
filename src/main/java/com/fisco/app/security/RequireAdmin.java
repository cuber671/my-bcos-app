package com.fisco.app.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 管理员权限注解
 * 标注在Controller方法上，表示该方法需要管理员权限才能访问
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAdmin {

    /**
     * 所需的最小管理员角色
     * 默认为AUDITOR（审核员）
     */
    AdminRole value() default AdminRole.AUDITOR;

    /**
     * 管理员角色枚举
     */
    enum AdminRole {
        AUDITOR,      // 审核员 - 可以审核企业
        ADMIN,        // 管理员 - 拥有审核员权限 + 企业管理权限
        SUPER_ADMIN   // 超级管理员 - 拥有所有权限
    }
}
