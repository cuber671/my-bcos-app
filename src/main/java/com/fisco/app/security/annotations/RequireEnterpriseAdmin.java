package com.fisco.app.security.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 要求企业管理员权限注解
 * 用于方法级别，确保只有企业管理员或系统管理员才能访问
 *
 * 使用示例：
 * @RequireEnterpriseAdmin
 * public void approveUser(String userId) {
 *     // 审核用户
 * }
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireEnterpriseAdmin {

    /**
     * 权限检查失败时的错误消息
     */
    String message() default "需要企业管理员权限";
}
