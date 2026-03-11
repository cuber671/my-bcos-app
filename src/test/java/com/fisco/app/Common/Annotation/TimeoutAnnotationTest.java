package com.fisco.app.Common.Annotation;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Timeout 注解单元测试
 *
 * 测试覆盖：
 * - UT001: 注解默认参数
 * - UT002: 自定义注解参数
 * - UT003: @Target和@Retention验证
 */
class TimeoutAnnotationTest {

    /**
     * UT001: 测试注解默认参数
     * 预期：value=5000, fallbackMethod="", message="请求超时，请稍后重试"
     */
    @Test
    void testDefaultAnnotationParameters() {
        Timeout annotation = getDefaultAnnotation();

        assertEquals(5000, annotation.value());
        assertEquals("", annotation.fallbackMethod());
        assertEquals("请求超时，请稍后重试", annotation.message());
    }

    /**
     * UT002: 测试自定义注解参数
     * 预期：自定义参数正确读取
     */
    @Test
    void testCustomAnnotationParameters() {
        Timeout annotation = getCustomAnnotation();

        assertEquals(3000, annotation.value());
        assertEquals("customFallback", annotation.fallbackMethod());
        assertEquals("自定义超时提示", annotation.message());
    }

    /**
     * UT003: 测试注解的@Target和@Retention
     * 预期：可用于方法和类级别，运行时保留
     */
    @Test
    void testAnnotationRetentionAndTarget() {
        Timeout annotation = getDefaultAnnotation();

        // 验证@Target - 方法和类级别
        java.lang.annotation.Target target = annotation.annotationType().getAnnotation(java.lang.annotation.Target.class);
        assertNotNull(target);
        assertArrayEquals(
            new java.lang.annotation.ElementType[]{
                java.lang.annotation.ElementType.METHOD,
                java.lang.annotation.ElementType.TYPE
            },
            target.value()
        );

        // 验证@Retention - 运行时保留
        Retention retention = annotation.annotationType().getAnnotation(Retention.class);
        assertNotNull(retention);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
    }

    // ==================== 辅助方法 ====================

    private Timeout getDefaultAnnotation() {
        try {
            TestClassWithDefaultTimeout testClass = new TestClassWithDefaultTimeout();
            return testClass.getClass().getMethod("testMethod").getAnnotation(Timeout.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private Timeout getCustomAnnotation() {
        try {
            TestClassWithCustomTimeout testClass = new TestClassWithCustomTimeout();
            return testClass.getClass().getMethod("testMethod").getAnnotation(Timeout.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    // ==================== 测试用类 ====================

    static class TestClassWithDefaultTimeout {
        @Timeout
        public String testMethod() {
            return "test";
        }
    }

    static class TestClassWithCustomTimeout {
        @Timeout(
            value = 3000,
            fallbackMethod = "customFallback",
            message = "自定义超时提示"
        )
        public String testMethod() {
            return "test";
        }

        public String customFallback() {
            return "fallback";
        }
    }
}
