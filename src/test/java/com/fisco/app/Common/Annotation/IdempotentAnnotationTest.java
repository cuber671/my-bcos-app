package com.fisco.app.Common.Annotation;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Idempotent 注解单元测试
 * 测试幂等性注解的参数配置
 *
 * 测试覆盖：
 * - UT001: 注解默认参数
 * - UT002: 自定义注解参数
 * - UT003: @Target和@Retention验证
 */
class IdempotentAnnotationTest {

    /**
     * UT001: 测试注解默认参数
     * 预期：transactionIdParam="transactionId", expireHours=24, required=true
     */
    @Test
    void testDefaultAnnotationParameters() {
        // 使用反射读取默认注解参数
        Idempotent annotation = getDefaultAnnotation();

        // 验证默认值
        assertEquals("transactionId", annotation.transactionIdParam());
        assertEquals(24, annotation.expireHours());
        assertEquals("重复请求，请勿重复提交", annotation.message());
        assertTrue(annotation.required());
    }

    /**
     * UT002: 测试自定义注解参数
     * 预期：自定义参数正确读取
     */
    @Test
    void testCustomAnnotationParameters() {
        // 使用反射读取自定义配置的注解
        Idempotent annotation = getCustomAnnotation();

        // 验证自定义参数
        assertEquals("txId", annotation.transactionIdParam());
        assertEquals(12, annotation.expireHours());
        assertEquals("自定义重复提交提示", annotation.message());
        assertFalse(annotation.required());
    }

    /**
     * UT003: 测试注解的@Target和@Retention
     * 预期：可用于方法和类级别，运行时保留
     */
    @Test
    void testAnnotationRetentionAndTarget() {
        Idempotent annotation = getDefaultAnnotation();

        // 验证@Target - 方法和类级别
        java.lang.annotation.Target target = annotation.annotationType().getAnnotation(java.lang.annotation.Target.class);
        assertNotNull(target);
        assertArrayEquals(
            new java.lang.annotation.ElementType[]{java.lang.annotation.ElementType.METHOD, java.lang.annotation.ElementType.TYPE},
            target.value()
        );

        // 验证@Retention - 运行时保留
        Retention retention = annotation.annotationType().getAnnotation(Retention.class);
        assertNotNull(retention);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取带默认值的注解
     */
    private Idempotent getDefaultAnnotation() {
        try {
            TestClassWithDefaultAnnotation testClass = new TestClassWithDefaultAnnotation();
            return testClass.getClass().getMethod("testMethod").getAnnotation(Idempotent.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取自定义配置的注解
     */
    private Idempotent getCustomAnnotation() {
        try {
            TestClassWithCustomAnnotation testClass = new TestClassWithCustomAnnotation();
            return testClass.getClass().getMethod("testMethod").getAnnotation(Idempotent.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    // ==================== 测试用类 ====================

    static class TestClassWithDefaultAnnotation {
        @Idempotent
        public String testMethod() {
            return "test";
        }
    }

    static class TestClassWithCustomAnnotation {
        @Idempotent(
            transactionIdParam = "txId",
            expireHours = 12,
            message = "自定义重复提交提示",
            required = false
        )
        public String testMethod() {
            return "test";
        }
    }
}
