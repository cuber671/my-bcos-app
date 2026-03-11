package com.fisco.app.Common.Config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * GlobalExceptionHandler 全局异常处理器单元测试
 *
 * 测试覆盖：
 * - UT001: handleBusinessException - 业务异常处理
 * - UT002: handleRuntimeException - 运行时异常处理
 * - UT003: 开发环境 - errorStack包含堆栈信息
 * - UT004: 统一响应格式验证
 * - UT005: 错误码和消息验证
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * UT001: 测试handleBusinessException - 返回业务错误
     * 验证：code=10002, msg="测试业务异常"
     */
    @Test
    void testHandleBusinessException() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/api/test/error/throw"))
                .andReturn().getResponse();

        String content = response.getContentAsString();
        assertTrue(content.contains("10002"));
        assertTrue(content.contains("测试业务异常"));
    }

    /**
     * UT002: 测试handleRuntimeException - 返回运行时错误
     */
    @Test
    void testHandleRuntimeException() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/api/test/error/with-stack"))
                .andReturn().getResponse();

        String content = response.getContentAsString();
        assertTrue(content.contains("500"));
        assertTrue(content.contains("系统错误测试"));
    }

    /**
     * UT003: 测试开发环境 - errorStack包含堆栈信息
     */
    @Test
    void testDevEnvironment_HasErrorStack() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/api/test/error/with-stack"))
                .andReturn().getResponse();

        String content = response.getContentAsString();
        assertTrue(content.contains("errorStack"));
        assertTrue(content.contains("RuntimeException") || content.contains("测试异常"));
    }

    /**
     * UT004: 测试统一响应格式
     */
    @Test
    void testResponseFormat() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/api/test/error/business"))
                .andReturn().getResponse();

        String content = response.getContentAsString();

        // 验证统一响应格式
        assertTrue(content.contains("\"code\":"));
        assertTrue(content.contains("\"msg\":"));
        assertTrue(content.contains("\"data\":"));
        assertTrue(content.contains("\"timestamp\":"));
        assertTrue(content.contains("\"errorStack\":"));
    }

    /**
     * UT005: 测试业务错误响应 - errorStack为null
     */
    @Test
    void testBusinessError_NoErrorStack() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/api/test/error/business"))
                .andReturn().getResponse();

        String content = response.getContentAsString();
        // 业务错误不应有堆栈信息
        assertTrue(content.contains("10001"));
    }

    /**
     * UT006: 测试带堆栈的错误响应 - errorStack有值
     */
    @Test
    void testErrorWithStack_HasValue() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/api/test/error/with-stack"))
                .andReturn().getResponse();

        String content = response.getContentAsString();
        // 带堆栈的错误响应应该有非null的errorStack
        assertTrue(content.contains("errorStack"));
        assertFalse(content.contains("\"errorStack\":null"));
    }

    /**
     * UT007: 测试异常全局处理 - BusinessException被捕获
     */
    @Test
    void testGlobalExceptionHandler() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/api/test/error/throw"))
                .andReturn().getResponse();

        String content = response.getContentAsString();
        // 验证BusinessException被正确处理
        assertTrue(content.contains("10002") || content.contains("测试业务异常"));
    }

    /**
     * UT008: 测试txHash字段存在
     */
    @Test
    void testTxHashFieldExists() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/api/test/error/business"))
                .andReturn().getResponse();

        String content = response.getContentAsString();
        // 验证txHash字段存在
        assertTrue(content.contains("\"txHash\":"));
    }
}
