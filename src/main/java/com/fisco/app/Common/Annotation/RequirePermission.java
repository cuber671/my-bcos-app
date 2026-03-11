package com.fisco.app.Common.Annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限等级校验注解
 * 用于标记需要特定权限等级才能访问的接口
 *
 * 权限等级说明：
 * - 10: 管理员 (ADMIN)
 * - 5:  财务   (FINANCE)
 * - 1:  普通用户 (USER)
 *
 * 使用示例：
 * <pre>
 * @RequirePermission(level = 10)
 * @PostMapping("/admin/delete")
 * public Result<?> delete(...) { ... }
 * </pre>
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    /**
     * 所需权限等级
     * - 10: 管理员
     * - 5:  财务
     * - 1:  普通用户
     *
     * @return 所需权限等级
     */
    int level();

    /**
     * 是否允许系统管理员(scope=1)绕过权限等级校验
     *
     * @return 是否允许系统管理员绕过
     */
    boolean adminBypass() default true;
}
