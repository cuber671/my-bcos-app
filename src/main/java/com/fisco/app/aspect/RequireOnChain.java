package com.fisco.app.aspect;

import java.lang.annotation.*;

/**
 * 标记需要区块链上链的操作
 * 使用此注解的方法会在执行前检查仓单的区块链状态
 * 只有区块链状态为 SYNCED 的仓单才能执行这些操作
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireOnChain {

    /**
     * 操作名称，用于异常提示
     * 例如：转让、质押、提货
     */
    String value() default "";

    /**
     * 是否允许 ONCHAIN_FAILED 状态的操作（默认不允许）
     * 如果设置为 true，上链失败的仓单也可以执行该操作
     */
    boolean allowFailed() default false;
}
