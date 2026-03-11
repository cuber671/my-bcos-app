package com.fisco.app.Common.Annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据归属校验注解
 * 用于校验当前操作的数据是否属于当前登录实体
 *
 * 使用场景：
 * - 防止用户越权查询/修改他人数据
 * - 确保企业用户只能操作自己企业的数据
 *
 * 使用示例：
 * <pre>
 * // 校验请求参数中的 entId 是否属于当前用户
 * @DataOwnership(paramName = "entId")
 * @GetMapping("/enterprise/{entId}")
 * public Result<?> getEnterprise(@PathVariable Long entId) { ... }
 *
 * // 校验请求参数中的 ownerId
 * @DataOwnership(paramName = "ownerId")
 * @GetMapping("/asset/{ownerId}")
 * public Result<?> getAsset(@PathVariable Long ownerId) { ... }
 * </pre>
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataOwnership {

    /**
     * 要校验的请求参数名
     * 从请求参数中获取该参数值，与JWT中的entId进行比对
     *
     * @return 参数名（如 entId, ownerId, enterpriseId）
     */
    String paramName() default "entId";

    /**
     * 是否允许系统管理员(scope=1)绕过校验
     * 系统管理员可以操作所有数据
     *
     * @return 是否允许系统管理员绕过
     */
    boolean adminBypass() default true;
}
