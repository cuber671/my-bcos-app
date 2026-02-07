package com.fisco.app.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 审计日志注解
 * 用于标记需要记录审计日志的方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

    /**
     * 操作模块（如 BILL, RECEIVABLE, WAREHOUSE_RECEPT等）
     */
    String module();

    /**
     * 操作类型（如 CREATE, UPDATE, DELETE, QUERY等）
     */
    String actionType();

    /**
     * 操作描述
     */
    String actionDesc() default "";

    /**
     * 实体类型（当操作涉及具体实体时）
     */
    String entityType() default "";

    /**
     * 是否记录请求参数
     */
    boolean logRequest() default true;

    /**
     * 是否记录响应结果
     */
    boolean logResponse() default true;

    /**
     * 是否记录变更详情
     */
    boolean logChanges() default false;
}
