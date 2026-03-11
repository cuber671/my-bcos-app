package com.fisco.app.Common.Annotation;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CircuitProtection 注解单元测试
 * 测试注解的默认参数和自定义参数
 *
 * 测试覆盖：
 * - UT009: 注解默认参数验证
 * - UT010: 自定义注解参数验证
 */
class CircuitProtectionAnnotationTest {

    /**
     * UT009: 测试注解默认参数
     * 预期：failureRateThreshold=50, waitDurationInOpenState=30000
     */
    @Test
    void testDefaultAnnotationParameters() {
        // 使用反射读取注解的默认值
        CircuitProtection annotation = getAnnotationWithDefaults();

        // 验证默认值
        assertEquals("default", annotation.name());
        assertEquals(50, annotation.failureRateThreshold());
        assertEquals(30000, annotation.waitDurationInOpenState());
        assertEquals(100, annotation.slidingWindowSize());
        assertEquals(10, annotation.minimumNumberOfCalls());
        assertEquals("", annotation.fallbackMethod());
        assertEquals("Service temporarily unavailable, please try again later", annotation.message());
    }

    /**
     * UT010: 测试自定义注解参数
     * 预期：自定义参数正确读取
     */
    @Test
    void testCustomAnnotationParameters() {
        // 使用反射读取自定义配置的注解
        CircuitProtection annotation = getCustomAnnotation();

        // 验证自定义参数
        assertEquals("blockchain", annotation.name());
        assertEquals(60, annotation.failureRateThreshold());
        assertEquals(60000, annotation.waitDurationInOpenState());
        assertEquals(50, annotation.slidingWindowSize());
        assertEquals(5, annotation.minimumNumberOfCalls());
        assertEquals("blockchainFallback", annotation.fallbackMethod());
        assertEquals("Blockchain service is temporarily unavailable", annotation.message());
    }

    /**
     * UT011: 测试注解的Target和Retention
     * 预期：注解可以用于方法和类级别，运行时保留
     */
    @Test
    void testAnnotationRetentionAndTarget() {
        CircuitProtection annotation = getAnnotationWithDefaults();

        // 验证@Target
        assertTrue(annotation.annotationType().isAnnotationPresent(
            java.lang.annotation.Target.class));

        // 验证@Retention
        Retention retention = annotation.annotationType().getAnnotation(Retention.class);
        assertNotNull(retention);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取带默认值的注解
     * 由于无法直接创建注解实例，我们通过反射验证注解定义
     */
    private CircuitProtection getAnnotationWithDefaults() {
        try {
            // 创建一个测试类来获取注解
            TestClassWithDefaultAnnotation testClass = new TestClassWithDefaultAnnotation();
            return testClass.getClass().getMethod("testMethod").getAnnotation(CircuitProtection.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取自定义配置的注解
     */
    private CircuitProtection getCustomAnnotation() {
        try {
            TestClassWithCustomAnnotation testClass = new TestClassWithCustomAnnotation();
            return testClass.getClass().getMethod("testMethod").getAnnotation(CircuitProtection.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    // ==================== 测试用类 ====================

    static class TestClassWithDefaultAnnotation {
        @CircuitProtection
        public String testMethod() {
            return "test";
        }
    }

    static class TestClassWithCustomAnnotation {
        @CircuitProtection(
            name = "blockchain",
            failureRateThreshold = 60,
            waitDurationInOpenState = 60000,
            slidingWindowSize = 50,
            minimumNumberOfCalls = 5,
            fallbackMethod = "blockchainFallback",
            message = "Blockchain service is temporarily unavailable"
        )
        public String testMethod() {
            return "test";
        }
    }
}
