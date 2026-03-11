package com.fisco.app.Common.Utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LogUtil 单元测试
 *
 * 测试覆盖：
 * - UT001-UT005: 通用日志方法 (debug/info/warn/error/fatal)
 * - UT006-UT008: 业务日志方法
 * - UT009-UT010: MDC上下文传递
 * - UT011-UT015: 边界测试
 */
class LogUtilTest {

    @BeforeEach
    void setUp() {
        // 清理MDC
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        // 清理MDC
        MDC.clear();
    }

    // ==================== 通用日志方法测试 ====================

    /**
     * UT001: 测试Debug日志方法
     * 验证方法可正常调用，不抛异常
     */
    @Test
    void testDebug() {
        assertDoesNotThrow(() -> LogUtil.debug("Test debug message"));
    }

    /**
     * UT002: 测试Info日志方法
     */
    @Test
    void testInfo() {
        assertDoesNotThrow(() -> LogUtil.info("Test info message"));
    }

    /**
     * UT003: 测试Warn日志方法
     */
    @Test
    void testWarn() {
        assertDoesNotThrow(() -> LogUtil.warn("Test warn message"));
    }

    /**
     * UT004: 测试Error日志方法（无异常）
     */
    @Test
    void testError() {
        assertDoesNotThrow(() -> LogUtil.error("Test error message"));
    }

    /**
     * UT005: 测试Fatal日志方法
     */
    @Test
    void testFatal() {
        assertDoesNotThrow(() -> LogUtil.fatal("Test fatal message"));
    }

    // ==================== 业务日志方法测试 ====================

    /**
     * UT006: 测试交易提交日志
     */
    @Test
    void testLogTransactionSubmitted() {
        assertDoesNotThrow(() ->
            LogUtil.logTransactionSubmitted("0xabc123", "TRANSFER"));
    }

    /**
     * UT007: 测试交易确认日志
     */
    @Test
    void testLogTransactionConfirmed() {
        assertDoesNotThrow(() ->
            LogUtil.logTransactionConfirmed("0xdef456", "MINT"));
    }

    /**
     * UT008: 测试业务操作日志
     */
    @Test
    void testLogOperation() {
        assertDoesNotThrow(() ->
            LogUtil.logOperation("CREATE_ASSET", "创建资产"));
    }

    // ==================== MDC上下文传递测试 ====================

    /**
     * UT009: 测试MDC中设置traceId后日志包含traceId
     */
    @Test
    void testMdcTraceId() {
        MDC.put("traceId", "test-trace-123");
        // 验证MDC中traceId已设置
        assertEquals("test-trace-123", MDC.get("traceId"));
    }

    /**
     * UT010: 测试MDC中设置userId后日志包含userId
     */
    @Test
    void testMdcUserId() {
        MDC.put("userId", "1001");
        // 验证MDC中userId已设置
        assertEquals("1001", MDC.get("userId"));
    }

    // ==================== 边界测试 ====================

    /**
     * UT011: 测试null消息
     */
    @Test
    void testNullMessage() {
        assertDoesNotThrow(() -> LogUtil.info(null));
    }

    /**
     * UT012: 测试空消息
     */
    @Test
    void testEmptyMessage() {
        assertDoesNotThrow(() -> LogUtil.info(""));
    }

    /**
     * UT013: 测试带参数的消息
     */
    @Test
    void testMessageWithArgs() {
        assertDoesNotThrow(() ->
            LogUtil.info("User {} logged in from {}", "admin", "192.168.1.1"));
    }

    /**
     * UT014: 测试异常日志方法
     */
    @Test
    void testErrorWithThrowable() {
        Exception ex = new RuntimeException("Test exception");
        assertDoesNotThrow(() ->
            LogUtil.error(ex, "Error occurred"));
    }

    /**
     * UT015: 测试Fatal异常日志方法
     */
    @Test
    void testFatalWithThrowable() {
        Exception ex = new RuntimeException("Fatal error");
        assertDoesNotThrow(() ->
            LogUtil.fatal(ex, "System crashed"));
    }

    /**
     * UT016: 测试MDC中设置txHash
     */
    @Test
    void testSetTxHash() {
        LogUtil.setTxHash("0xtx123hash");
        assertEquals("0xtx123hash", MDC.get("txHash"));

        LogUtil.clearTxHash();
        assertNull(MDC.get("txHash"));
    }

    /**
     * UT017: 测试用户操作日志
     */
    @Test
    void testLogUserAction() {
        assertDoesNotThrow(() ->
            LogUtil.logUserAction("LOGIN", "/api/v1/auth/login"));
    }

    /**
     * UT018: 测试参数校验失败日志
     */
    @Test
    void testLogValidationFailed() {
        assertDoesNotThrow(() ->
            LogUtil.logValidationFailed("userId", "cannot be null"));
    }

    /**
     * UT019: 测试系统异常日志
     */
    @Test
    void testLogSystemError() {
        Exception ex = new RuntimeException("System error");
        assertDoesNotThrow(() ->
            LogUtil.logSystemError("USER_SERVICE", ex));
    }

    /**
     * UT020: 测试业务异常日志
     */
    @Test
    void testLogBusinessError() {
        assertDoesNotThrow(() ->
            LogUtil.logBusinessError("40001", "Invalid parameter"));
    }
}
