package com.fisco.app.Common.Config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * TraceIdInterceptor 单元测试
 *
 * 测试覆盖：
 * - UT021: 自动生成traceId
 * - UT022: 从请求头获取traceId
 * - UT023: 设置响应头X-Trace-Id
 * - UT024: afterCompletion清理MDC
 * - UT025-UT028: 边界测试
 */
class TraceIdInterceptorTest {

    private TraceIdInterceptor interceptor;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        interceptor = new TraceIdInterceptor();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    /**
     * UT021: 测试自动生成traceId
     * 当请求头没有X-Trace-Id时，应自动生成
     */
    @Test
    void testAutoGenerateTraceId() throws Exception {
        // 设置请求头没有X-Trace-Id
        when(request.getHeader("X-Trace-Id")).thenReturn(null);

        // 执行拦截器
        boolean result = interceptor.preHandle(request, response, null);

        // 验证结果
        assertTrue(result);

        // 验证MDC中设置了traceId
        String traceId = MDC.get("traceId");
        assertNotNull(traceId);
        assertEquals(32, traceId.length()); // UUID去掉横线是32位
    }

    /**
     * UT022: 测试从请求头获取traceId
     * 当请求头有X-Trace-Id时，应使用请求头的值
     */
    @Test
    void testGetTraceIdFromHeader() throws Exception {
        String customTraceId = "custom-trace-id-12345";
        when(request.getHeader("X-Trace-Id")).thenReturn(customTraceId);

        // 执行拦截器
        boolean result = interceptor.preHandle(request, response, null);

        // 验证结果
        assertTrue(result);

        // 验证MDC中使用了请求头的traceId
        assertEquals(customTraceId, MDC.get("traceId"));
    }

    /**
     * UT023: 测试设置响应头X-Trace-Id
     * 拦截器应将traceId设置到响应头
     */
    @Test
    void testSetResponseHeader() throws Exception {
        when(request.getHeader("X-Trace-Id")).thenReturn(null);

        // 执行拦截器
        interceptor.preHandle(request, response, null);

        // 验证响应头被设置
        verify(response).setHeader("X-Trace-Id", MDC.get("traceId"));
    }

    /**
     * UT024: 测试afterCompletion清理MDC
     * 请求完成后应清除MDC中的traceId
     */
    @Test
    void testAfterCompletionCleanup() throws Exception {
        // 先执行preHandle设置traceId
        when(request.getHeader("X-Trace-Id")).thenReturn("test-trace-456");
        interceptor.preHandle(request, response, null);

        // 验证MDC中有traceId
        assertNotNull(MDC.get("traceId"));

        // 执行afterCompletion
        interceptor.afterCompletion(request, response, null, null);

        // 验证MDC中的traceId被清除
        assertNull(MDC.get("traceId"));
    }

    /**
     * UT025: 测试空字符串请求头
     * 应自动生成新的traceId
     */
    @Test
    void testEmptyTraceIdHeader() throws Exception {
        when(request.getHeader("X-Trace-Id")).thenReturn("");

        // 执行拦截器
        boolean result = interceptor.preHandle(request, response, null);

        // 验证结果
        assertTrue(result);

        // 验证生成了新的traceId
        assertNotNull(MDC.get("traceId"));
    }

    /**
     * UT026: 测试afterCompletion带异常
     * 即使有异常也应清理MDC
     */
    @Test
    void testAfterCompletionWithException() throws Exception {
        // 先执行preHandle设置traceId
        when(request.getHeader("X-Trace-Id")).thenReturn("test-trace-789");
        interceptor.preHandle(request, response, null);

        // 执行afterCompletion带异常
        RuntimeException ex = new RuntimeException("Test exception");
        interceptor.afterCompletion(request, response, null, ex);

        // 验证MDC仍被清除
        assertNull(MDC.get("traceId"));
    }

    /**
     * UT027: 测试traceId格式
     * 生成的traceId应该是32位16进制字符串
     */
    @Test
    void testTraceIdFormat() throws Exception {
        when(request.getHeader("X-Trace-Id")).thenReturn(null);

        interceptor.preHandle(request, response, null);

        String traceId = MDC.get("traceId");

        // 验证格式：32位16进制字符
        assertNotNull(traceId);
        assertEquals(32, traceId.length());
        assertTrue(traceId.matches("[0-9a-f]+"));
    }

    /**
     * UT028: 测试多次请求生成不同traceId
     * 每次请求应生成唯一的traceId
     */
    @Test
    void testUniqueTraceId() throws Exception {
        when(request.getHeader("X-Trace-Id")).thenReturn(null);

        // 第一次请求
        interceptor.preHandle(request, response, null);
        String traceId1 = MDC.get("traceId");

        // 清理并第二次请求
        MDC.clear();
        interceptor.preHandle(request, response, null);
        String traceId2 = MDC.get("traceId");

        // 验证两次生成的traceId不同
        assertNotEquals(traceId1, traceId2);
    }
}
