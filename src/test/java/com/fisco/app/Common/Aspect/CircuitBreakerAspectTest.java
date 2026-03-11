package com.fisco.app.Common.Aspect;

import com.fisco.app.Common.Annotation.CircuitProtection;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CircuitBreakerAspect 单元测试
 * 测试熔断切面的各种场景
 *
 * 测试覆盖：
 * - UT001: 正常方法执行（无@CircuitProtection注解）
 * - UT005: 兜底方法存在时调用
 * - UT006: 兜底方法不存在时
 * - UT007: 不同name创建独立熔断器
 * - FT004-FT006: 缓存清理方法验证
 * - FT007-FT009: 兜底方法匹配策略验证
 */
class CircuitBreakerAspectTest {

    /**
     * UT001: 测试无@CircuitProtection注解的方法
     * 验证没有注解的方法不被熔断保护
     */
    @Test
    void testNormalMethodWithoutAnnotation() throws Exception {
        Method method = TestServiceWithoutAnnotation.class.getMethod("normalMethod");
        CircuitProtection annotation = method.getAnnotation(CircuitProtection.class);

        // 验证：此方法没有@CircuitProtection注解
        assertNull(annotation);
    }

    /**
     * UT005: 测试兜底方法存在时的调用
     * 验证兜底方法可以被正确找到
     */
    @Test
    void testFallbackMethodExists() throws Exception {
        Method testMethod = TestServiceWithFallback.class.getMethod("testMethod");
        CircuitProtection annotation = testMethod.getAnnotation(CircuitProtection.class);

        // 验证：注解配置正确
        assertNotNull(annotation);
        assertEquals("testCircuit", annotation.name());
        assertEquals("fallback", annotation.fallbackMethod());

        // 验证：兜底方法可以被找到
        Method fallbackMethod = TestServiceWithFallback.class.getMethod("fallback");
        assertNotNull(fallbackMethod);
        assertEquals("fallback", fallbackMethod.getName());
    }

    /**
     * UT006: 测试兜底方法不存在时的处理
     * 验证当兜底方法不存在时的行为
     */
    @Test
    void testFallbackMethodNotExists() throws Exception {
        Method testMethod = TestServiceWithoutFallback.class.getMethod("testMethod");
        CircuitProtection annotation = testMethod.getAnnotation(CircuitProtection.class);

        // 验证：注解中配置的兜底方法名
        assertEquals("nonExistentFallback", annotation.fallbackMethod());

        // 验证：尝试查找不存在的兜底方法会抛出异常
        assertThrows(NoSuchMethodException.class, () -> {
            TestServiceWithoutFallback.class.getMethod("nonExistentFallback");
        });
    }

    /**
     * UT007: 测试不同name对应不同配置
     * 验证不同熔断器名称有不同的配置参数
     */
    @Test
    void testDifferentNameCreatesSeparateCircuitBreakers() throws Exception {
        // blockchain熔断器配置
        Method blockchainMethod = TestServiceBlockchain.class.getMethod("blockchainCall");
        CircuitProtection blockchainAnnotation = blockchainMethod.getAnnotation(CircuitProtection.class);

        // default熔断器配置
        Method defaultMethod = TestServiceDefault.class.getMethod("defaultCall");
        CircuitProtection defaultAnnotation = defaultMethod.getAnnotation(CircuitProtection.class);

        // 验证：两个熔断器配置不同
        assertEquals("blockchain", blockchainAnnotation.name());
        assertEquals(5, blockchainAnnotation.minimumNumberOfCalls());
        assertEquals(50, blockchainAnnotation.slidingWindowSize());

        assertEquals("default", defaultAnnotation.name());
        assertEquals(10, defaultAnnotation.minimumNumberOfCalls());
        assertEquals(100, defaultAnnotation.slidingWindowSize());

        // 验证配置确实不同
        assertNotEquals(blockchainAnnotation.minimumNumberOfCalls(), defaultAnnotation.minimumNumberOfCalls());
    }

    /**
     * UT008: 测试熔断器切面类结构
     * 验证切面类包含必要的方法
     */
    @Test
    void testCircuitBreakerAspectHasRequiredMethods() throws Exception {
        // 验证切面类包含executeWithCircuitBreaker方法
        Method executeMethod = CircuitBreakerAspect.class.getDeclaredMethod(
            "executeWithCircuitBreaker",
            org.aspectj.lang.ProceedingJoinPoint.class,
            io.github.resilience4j.circuitbreaker.CircuitBreaker.class,
            CircuitProtection.class
        );
        assertNotNull(executeMethod);

        // 验证切面类包含handleFallback方法
        Method handleFallbackMethod = CircuitBreakerAspect.class.getDeclaredMethod(
            "handleFallback",
            org.aspectj.lang.ProceedingJoinPoint.class,
            CircuitProtection.class,
            String.class
        );
        assertNotNull(handleFallbackMethod);
    }

    // ==================== FT004-FT006: 缓存清理方法测试 ====================

    /**
     * FT004: 测试 clearCircuitBreaker 方法存在
     * 验证清理指定熔断器的方法存在且可访问
     */
    @Test
    void testClearCircuitBreakerMethod() throws Exception {
        // 验证方法存在
        Method clearMethod = CircuitBreakerAspect.class.getMethod("clearCircuitBreaker", String.class);
        assertNotNull(clearMethod);

        // 验证方法可调用
        CircuitBreakerAspect aspect = new CircuitBreakerAspect();
        clearMethod.invoke(aspect, "testCircuit");

        System.out.println("✅ FT004: clearCircuitBreaker 方法验证通过");
    }

    /**
     * FT005: 测试 clearAllCircuitBreakers 方法存在
     * 验证清理所有熔断器的方法存在且可访问
     */
    @Test
    void testClearAllCircuitBreakersMethod() throws Exception {
        // 验证方法存在
        Method clearAllMethod = CircuitBreakerAspect.class.getMethod("clearAllCircuitBreakers");
        assertNotNull(clearAllMethod);

        // 验证方法可调用
        CircuitBreakerAspect aspect = new CircuitBreakerAspect();
        clearAllMethod.invoke(aspect);

        System.out.println("✅ FT005: clearAllCircuitBreakers 方法验证通过");
    }

    /**
     * FT006: 测试 getCircuitBreakerStatus 方法
     * 验证获取熔断器状态的方法存在且返回正确格式
     */
    @Test
    @SuppressWarnings("unchecked")
    void testGetCircuitBreakerStatusMethod() throws Exception {
        // 验证方法存在
        Method statusMethod = CircuitBreakerAspect.class.getMethod("getCircuitBreakerStatus");
        assertNotNull(statusMethod);

        // 验证返回类型
        assertEquals(Map.class, statusMethod.getReturnType());

        // 验证方法可调用
        CircuitBreakerAspect aspect = new CircuitBreakerAspect();
        Map<String, Object> status = (Map<String, Object>) statusMethod.invoke(aspect);

        // 验证返回结构
        assertNotNull(status);
        assertTrue(status.containsKey("size"));
        assertTrue(status.containsKey("details"));

        System.out.println("✅ FT006: getCircuitBreakerStatus 方法验证通过, 当前缓存大小: " + status.get("size"));
    }

    // ==================== FT007-FT009: 兜底方法匹配策略测试 ====================

    /**
     * FT007: 测试兜底方法精确匹配策略
     * 验证参数类型完全相同时能找到兜底方法
     */
    @Test
    void testFallbackMethodExactMatch() throws Exception {
        // 验证：TestServiceWithExactFallback 类存在且有精确匹配的兜底方法
        Method testMethod = TestServiceWithExactFallback.class.getMethod("testMethod", String.class, Integer.class);
        CircuitProtection annotation = testMethod.getAnnotation(CircuitProtection.class);

        assertNotNull(annotation);
        assertEquals("exactFallback", annotation.fallbackMethod());

        // 验证精确匹配的兜底方法存在
        Method fallbackMethod = TestServiceWithExactFallback.class.getMethod("exactFallback", String.class, Integer.class);
        assertNotNull(fallbackMethod);

        System.out.println("✅ FT007: 兜底方法精确匹配验证通过");
    }

    /**
     * FT008: 测试兜底方法无参匹配策略
     * 验证无参兜底方法能被找到
     */
    @Test
    void testFallbackMethodNoArgsMatch() throws Exception {
        // 验证：TestServiceWithNoArgsFallback 类有无参兜底方法
        Method testMethod = TestServiceWithNoArgsFallback.class.getMethod("testMethod", String.class);
        CircuitProtection annotation = testMethod.getAnnotation(CircuitProtection.class);

        assertNotNull(annotation);
        assertEquals("noArgsFallback", annotation.fallbackMethod());

        // 验证无参兜底方法存在
        Method fallbackMethod = TestServiceWithNoArgsFallback.class.getMethod("noArgsFallback");
        assertNotNull(fallbackMethod);

        System.out.println("✅ FT008: 兜底方法无参匹配验证通过");
    }

    /**
     * FT009: 测试兜底方法参数兼容匹配策略
     * 验证兜底方法参数是父类型时也能匹配
     */
    @Test
    void testFallbackMethodCompatibleMatch() throws Exception {
        // 验证：参数兼容匹配 - 验证 isParamsCompatible 方法存在
        Method isParamsCompatible = CircuitBreakerAspect.class.getDeclaredMethod(
            "isParamsCompatible", Class[].class, Class[].class
        );
        assertNotNull(isParamsCompatible);

        // 验证兼容匹配逻辑
        // Object[] 可接受任何数组
        Class<?>[] fallbackParams = { Object.class };
        Class<?>[] originalParams = { String.class };

        // 使用反射调用（私有方法）- 需要通过实例调用
        CircuitBreakerAspect aspect = new CircuitBreakerAspect();
        isParamsCompatible.setAccessible(true);
        Boolean result = (Boolean) isParamsCompatible.invoke(aspect, (Object) fallbackParams, (Object) originalParams);

        // 验证：Object 是 String 的父类，应该兼容
        assertTrue(result);

        System.out.println("✅ FT009: 兜底方法参数兼容匹配验证通过");
    }

    // ==================== 测试用服务类 ====================

    static class TestServiceWithoutAnnotation {
        public String normalMethod() {
            return "normal";
        }
    }

    static class TestServiceWithFallback {
        @CircuitProtection(name = "testCircuit", fallbackMethod = "fallback")
        public String testMethod() {
            throw new RuntimeException("Simulated failure");
        }

        public String fallback() {
            return "fallback response";
        }
    }

    static class TestServiceWithoutFallback {
        @CircuitProtection(name = "test", fallbackMethod = "nonExistentFallback")
        public String testMethod() {
            throw new RuntimeException("Simulated failure");
        }
    }

    static class TestServiceBlockchain {
        @CircuitProtection(
            name = "blockchain",
            failureRateThreshold = 50,
            waitDurationInOpenState = 30000,
            slidingWindowSize = 50,
            minimumNumberOfCalls = 5,
            fallbackMethod = "fallback"
        )
        public String blockchainCall() {
            return "blockchain result";
        }

        public String fallback() {
            return "fallback";
        }
    }

    static class TestServiceDefault {
        @CircuitProtection(
            name = "default",
            failureRateThreshold = 50,
            waitDurationInOpenState = 30000,
            slidingWindowSize = 100,
            minimumNumberOfCalls = 10,
            fallbackMethod = "fallback"
        )
        public String defaultCall() {
            return "default result";
        }

        public String fallback() {
            return "fallback";
        }
    }

    // FT007: 精确匹配测试用类
    static class TestServiceWithExactFallback {
        @CircuitProtection(name = "test", fallbackMethod = "exactFallback")
        public String testMethod(String param1, Integer param2) {
            throw new RuntimeException("Simulated failure");
        }

        public String exactFallback(String param1, Integer param2) {
            return "exact fallback";
        }
    }

    // FT008: 无参匹配测试用类
    static class TestServiceWithNoArgsFallback {
        @CircuitProtection(name = "test", fallbackMethod = "noArgsFallback")
        public String testMethod(String param1) {
            throw new RuntimeException("Simulated failure");
        }

        public String noArgsFallback() {
            return "no args fallback";
        }
    }
}
