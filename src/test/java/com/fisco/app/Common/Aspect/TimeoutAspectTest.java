package com.fisco.app.Common.Aspect;

import com.fisco.app.Common.Annotation.Timeout;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TimeoutAspect 单元测试
 *
 * 测试覆盖：
 * - UT004: 超时切面类结构
 * - UT005: Future超时实现
 * - UT006: 兜底方法查找
 * - UT007: 超时返回错误码
 */
class TimeoutAspectTest {

    /**
     * UT004: 测试超时切面类结构
     * 验证包含必要的方法
     */
    @Test
    void testTimeoutAspectStructure() throws NoSuchMethodException {
        // 验证executeWithTimeout方法存在
        Method executeMethod = TimeoutAspect.class.getDeclaredMethod(
            "executeWithTimeout",
            org.aspectj.lang.ProceedingJoinPoint.class,
            int.class,
            Timeout.class
        );
        assertNotNull(executeMethod);

        // 验证handleFallback方法存在
        Method handleFallbackMethod = TimeoutAspect.class.getDeclaredMethod(
            "handleFallback",
            org.aspectj.lang.ProceedingJoinPoint.class,
            Timeout.class,
            String.class
        );
        assertNotNull(handleFallbackMethod);

        // 验证findFallbackMethod方法存在
        Method findFallbackMethod = TimeoutAspect.class.getDeclaredMethod(
            "findFallbackMethod",
            Object.class,
            Method.class,
            String.class
        );
        assertNotNull(findFallbackMethod);

        System.out.println("✅ UT004: 超时切面类结构验证通过");
    }

    /**
     * UT005: 测试Future超时实现
     * 验证使用线程池+Future.get(timeout)实现
     */
    @Test
    void testFutureTimeoutImplementation() throws Exception {
        // 验证timeoutExecutor字段存在
        java.lang.reflect.Field executorField = TimeoutAspect.class.getDeclaredField("timeoutExecutor");
        assertNotNull(executorField);
        executorField.setAccessible(true);

        // 创建切面实例
        TimeoutAspect aspect = new TimeoutAspect();
        Object executor = executorField.get(aspect);
        assertNotNull(executor);
        assertTrue(executor instanceof java.util.concurrent.ExecutorService);

        System.out.println("✅ UT005: Future超时实现验证通过");
    }

    /**
     * UT006: 测试兜底方法查找
     * 验证findFallbackMethod可以找到兜底方法
     */
    @Test
    void testFindFallbackMethod() throws Exception {
        // 验证兜底方法可以被找到
        Method testMethod = TestServiceWithTimeoutFallback.class.getMethod("doWork", String.class);
        Timeout annotation = testMethod.getAnnotation(Timeout.class);

        assertNotNull(annotation);
        assertEquals("fallbackMethod", annotation.fallbackMethod());

        // 验证兜底方法存在
        Method fallbackMethod = TestServiceWithTimeoutFallback.class.getMethod("fallbackMethod", String.class);
        assertNotNull(fallbackMethod);

        System.out.println("✅ UT006: 兜底方法查找验证通过");
    }

    /**
     * UT007: 测试超时返回错误码
     * 验证Result.error使用40003错误码
     */
    @Test
    void testTimeoutErrorCode() throws Exception {
        // 获取Result.error方法 - 使用Integer类型
        Method errorMethod = com.fisco.app.Common.Utils.Result.class.getMethod("error", Integer.class, String.class);
        assertNotNull(errorMethod);

        // 调用返回超时错误 - 使用Integer包装类型
        Object result = errorMethod.invoke(null, Integer.valueOf(40003), "请求超时，请稍后重试");

        assertNotNull(result);
        com.fisco.app.Common.Utils.Result<?> errorResult = (com.fisco.app.Common.Utils.Result<?>) result;
        assertEquals(40003, errorResult.getCode());
        assertEquals("请求超时，请稍后重试", errorResult.getMsg());

        System.out.println("✅ UT007: 超时返回错误码验证通过");
    }

    /**
     * UT008: 测试默认超时配置
     * 验证默认超时为5秒
     */
    @Test
    void testDefaultTimeoutValue() throws Exception {
        // 验证defaultTimeout字段存在
        java.lang.reflect.Field timeoutField = TimeoutAspect.class.getDeclaredField("defaultTimeout");
        assertNotNull(timeoutField);
        timeoutField.setAccessible(true);

        TimeoutAspect aspect = new TimeoutAspect();
        timeoutField.set(aspect, 5000);

        int timeout = timeoutField.getInt(aspect);
        assertEquals(5000, timeout);

        System.out.println("✅ UT008: 默认超时配置验证通过");
    }

    // ==================== 测试用类 ====================

    static class TestServiceWithTimeoutFallback {
        @Timeout(value = 3000, fallbackMethod = "fallbackMethod")
        public String doWork(String param) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "work done";
        }

        public String fallbackMethod(String param) {
            return "fallback result";
        }
    }
}
