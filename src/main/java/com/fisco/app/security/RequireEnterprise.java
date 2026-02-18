package com.fisco.app.security;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 企业权限注解
 * 标注在Controller方法上，表示该方法需要企业用户认证才能访问
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireEnterprise {
    /**
     * 是否需要企业已激活
     * 默认为true，即只有已激活的企业才能访问
     */
    boolean requireActive() default true;
}
