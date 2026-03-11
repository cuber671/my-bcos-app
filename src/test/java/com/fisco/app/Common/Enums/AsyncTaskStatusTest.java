package com.fisco.app.Common.Enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AsyncTaskStatus 枚举单元测试
 *
 * 测试覆盖：
 * - UT001: 枚举所有状态
 * - UT002: 枚举属性验证
 */
class AsyncTaskStatusTest {

    /**
     * UT001: 测试枚举所有状态
     */
    @Test
    void testAllStatusValues() {
        // 验证所有状态存在
        AsyncTaskStatus[] statuses = AsyncTaskStatus.values();
        assertEquals(5, statuses.length);

        // 验证状态名称
        assertNotNull(AsyncTaskStatus.PENDING);
        assertNotNull(AsyncTaskStatus.PROCESSING);
        assertNotNull(AsyncTaskStatus.SUCCESS);
        assertNotNull(AsyncTaskStatus.FAILED);
        assertNotNull(AsyncTaskStatus.TIMEOUT);

        System.out.println("✅ UT001: 枚举所有状态验证通过 - " + statuses.length + "个状态");
    }

    /**
     * UT002: 测试枚举属性
     */
    @Test
    void testEnumProperties() {
        // PENDING
        assertEquals("PENDING", AsyncTaskStatus.PENDING.getCode());
        assertEquals("待处理", AsyncTaskStatus.PENDING.getDescription());

        // PROCESSING
        assertEquals("PROCESSING", AsyncTaskStatus.PROCESSING.getCode());
        assertEquals("处理中", AsyncTaskStatus.PROCESSING.getDescription());

        // SUCCESS
        assertEquals("SUCCESS", AsyncTaskStatus.SUCCESS.getCode());
        assertEquals("成功", AsyncTaskStatus.SUCCESS.getDescription());

        // FAILED
        assertEquals("FAILED", AsyncTaskStatus.FAILED.getCode());
        assertEquals("失败", AsyncTaskStatus.FAILED.getDescription());

        // TIMEOUT
        assertEquals("TIMEOUT", AsyncTaskStatus.TIMEOUT.getCode());
        assertEquals("超时", AsyncTaskStatus.TIMEOUT.getDescription());

        System.out.println("✅ UT002: 枚举属性验证通过");
    }
}
