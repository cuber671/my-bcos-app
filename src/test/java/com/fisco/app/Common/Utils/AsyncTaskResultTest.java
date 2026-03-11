package com.fisco.app.Common.Utils;

import com.fisco.app.Common.Enums.AsyncTaskStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AsyncTaskResult 单元测试
 *
 * 测试覆盖：
 * - UT003: 默认构造方法
 * - UT004-UT007: 状态变更方法
 * - UT008: incrementRetry
 * - UT009-UT010: 状态判断方法
 */
class AsyncTaskResultTest {

    /**
     * UT003: 测试默认构造方法
     */
    @Test
    void testDefaultConstructor() {
        AsyncTaskResult<String> result = new AsyncTaskResult<>("task123");

        assertEquals("task123", result.getTaskId());
        assertEquals(AsyncTaskStatus.PENDING, result.getStatus());
        assertEquals(0, result.getRetryCount());
        assertNotNull(result.getCreateTime());
        assertNotNull(result.getUpdateTime());

        System.out.println("✅ UT003: 默认构造方法验证通过");
    }

    /**
     * UT004: 测试 setProcessing()
     */
    @Test
    void testSetProcessing() {
        AsyncTaskResult<String> result = new AsyncTaskResult<>("task123");
        LocalDateTime before = result.getUpdateTime();

        // 等待一小段时间确保时间戳不同
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}

        result.setProcessing();

        assertEquals(AsyncTaskStatus.PROCESSING, result.getStatus());
        assertTrue(result.getUpdateTime().isAfter(before) || result.getUpdateTime().isEqual(before));

        System.out.println("✅ UT004: setProcessing() 验证通过");
    }

    /**
     * UT005: 测试 setSuccess()
     */
    @Test
    void testSetSuccess() {
        AsyncTaskResult<String> result = new AsyncTaskResult<>("task123");

        result.setSuccess("task result data");

        assertEquals(AsyncTaskStatus.SUCCESS, result.getStatus());
        assertEquals("task result data", result.getResult());
        assertNotNull(result.getUpdateTime());

        System.out.println("✅ UT005: setSuccess() 验证通过");
    }

    /**
     * UT006: 测试 setFailed()
     */
    @Test
    void testSetFailed() {
        AsyncTaskResult<String> result = new AsyncTaskResult<>("task123");

        result.setFailed("Error message");

        assertEquals(AsyncTaskStatus.FAILED, result.getStatus());
        assertEquals("Error message", result.getErrorMessage());
        assertNotNull(result.getUpdateTime());

        System.out.println("✅ UT006: setFailed() 验证通过");
    }

    /**
     * UT007: 测试 setTimeout()
     */
    @Test
    void testSetTimeout() {
        AsyncTaskResult<String> result = new AsyncTaskResult<>("task123");

        result.setTimeout();

        assertEquals(AsyncTaskStatus.TIMEOUT, result.getStatus());
        assertEquals("任务执行超时", result.getErrorMessage());
        assertNotNull(result.getUpdateTime());

        System.out.println("✅ UT007: setTimeout() 验证通过");
    }

    /**
     * UT008: 测试 incrementRetry()
     */
    @Test
    void testIncrementRetry() {
        AsyncTaskResult<String> result = new AsyncTaskResult<>("task123");
        assertEquals(0, result.getRetryCount());

        result.incrementRetry();
        assertEquals(1, result.getRetryCount());

        result.incrementRetry();
        assertEquals(2, result.getRetryCount());

        System.out.println("✅ UT008: incrementRetry() 验证通过");
    }

    /**
     * UT009: 测试 isCompleted()
     */
    @Test
    void testIsCompleted() {
        AsyncTaskResult<String> result = new AsyncTaskResult<>("task123");

        // PENDING时不是完成状态
        assertFalse(result.isCompleted());

        // PROCESSING时不是完成状态
        result.setProcessing();
        assertFalse(result.isCompleted());

        // SUCCESS是完成状态
        result.setSuccess("done");
        assertTrue(result.isCompleted());

        // FAILED是完成状态
        result.setFailed("error");
        assertTrue(result.isCompleted());

        // TIMEOUT是完成状态
        result.setTimeout();
        assertTrue(result.isCompleted());

        System.out.println("✅ UT009: isCompleted() 验证通过");
    }

    /**
     * UT010: 测试 isSuccess()
     */
    @Test
    void testIsSuccess() {
        AsyncTaskResult<String> result = new AsyncTaskResult<>("task123");

        // PENDING时不是成功
        assertFalse(result.isSuccess());

        // PROCESSING时不是成功
        result.setProcessing();
        assertFalse(result.isSuccess());

        // SUCCESS时是成功
        result.setSuccess("done");
        assertTrue(result.isSuccess());

        // FAILED时不是成功
        result.setFailed("error");
        assertFalse(result.isSuccess());

        // TIMEOUT时不是成功
        result.setTimeout();
        assertFalse(result.isSuccess());

        System.out.println("✅ UT010: isSuccess() 验证通过");
    }
}
