package com.fisco.app.security.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * 要求企业访问权限注解
 * 用于方法级别，确保用户只能访问自己企业的数据
 *
 * 使用示例：
 * @RequireEnterpriseAccess
 * public List<User> getUsersByEnterprise(String enterpriseId) {
 *     // 方法实现
 * }
 *
 * 支持SpEL表达式：
 * @RequireEnterpriseAccess(param = "userId")
 * public User getUserById(String userId) {
 *     // 方法实现
 * }
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireEnterpriseAccess {

    /**
     * 指定要检查的企业ID参数名
     * 如果为空，则自动从方法参数中查找名为enterpriseId、enterpriseId等的参数
     */
    String param() default "";

    /**
     * 是否允许系统管理员访问所有企业数据
     * 默认为true
     */
    boolean allowSystemAdmin() default true;

    /**
     * 权限检查失败时的错误消息
     */
    String message() default "无权限访问该企业的数据";
}
