package com.fisco.app.Modules.Test.Controller;

import com.fisco.app.Common.Utils.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * HealthController 单元测试
 *
 * 测试覆盖：
 * - UT001: health() 返回 UP 状态（数据库正常）
 * - UT002: health() 返回 DEGRADED 状态（数据库异常）
 * - UT003: 验证响应包含 status, database, timestamp 字段
 * - UT004: 验证数据库连接检查逻辑
 * - UT005: 验证异常时 status 降级为 DEGRADED
 * - UT006: 匿名访问（无需认证）
 * - UT007: 验证 Result 统一响应格式
 * - UT008: 验证 timestamp 为当前时间
 * - UT009: 验证数据库连接超时处理
 * - UT010: 验证多次调用状态一致
 */
@ExtendWith(MockitoExtension.class)
class HealthControllerTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @InjectMocks
    private HealthController healthController;

    /**
     * UT001: 测试health()返回UP状态（数据库正常）
     */
    @Test
    void testHealth_UpStatus() throws SQLException {
        // Mock数据库连接正常
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(5)).thenReturn(true);

        // 执行测试
        Result<HealthController.HealthStatus> result = healthController.health();

        // 验证结果
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertEquals("操作成功", result.getMsg());
        assertNotNull(result.getData());
        assertEquals("UP", result.getData().getStatus());
        assertEquals("UP", result.getData().getDatabase());
        assertNotNull(result.getData().getTimestamp());
    }

    /**
     * UT002: 测试health()返回DEGRADED状态（数据库异常）
     */
    @Test
    void testHealth_DegradedStatus() throws SQLException {
        // Mock数据库连接失败
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection refused"));

        // 执行测试
        Result<HealthController.HealthStatus> result = healthController.health();

        // 验证结果
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals("DEGRADED", result.getData().getStatus());
        assertEquals("DOWN", result.getData().getDatabase());
    }

    /**
     * UT003: 验证响应包含status, database, timestamp字段
     */
    @Test
    void testHealth_ResponseFields() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(5)).thenReturn(true);

        Result<HealthController.HealthStatus> result = healthController.health();

        assertNotNull(result.getData());
        assertNotNull(result.getData().getStatus());
        assertNotNull(result.getData().getDatabase());
        assertNotNull(result.getData().getTimestamp());
    }

    /**
     * UT004: 验证数据库连接检查逻辑
     */
    @Test
    void testHealth_DatabaseCheckLogic() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(5)).thenReturn(true);

        healthController.health();

        verify(dataSource, times(1)).getConnection();
        verify(connection, times(1)).isValid(5);
    }

    /**
     * UT005: 验证异常时status降级为DEGRADED
     */
    @Test
    void testHealth_DegradedOnException() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new RuntimeException("DB Error"));

        Result<HealthController.HealthStatus> result = healthController.health();

        assertEquals("DEGRADED", result.getData().getStatus());
        assertEquals("DOWN", result.getData().getDatabase());
    }

    /**
     * UT006: 验证匿名访问（无需认证）
     * 注：此测试验证Controller本身不检查认证，具体认证由拦截器控制
     */
    @Test
    void testHealth_AnonymousAccess() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(5)).thenReturn(true);

        // Controller不进行认证检查
        Result<HealthController.HealthStatus> result = healthController.health();

        assertNotNull(result);
        assertEquals(0, result.getCode());
    }

    /**
     * UT007: 验证Result统一响应格式
     */
    @Test
    void testHealth_ResultFormat() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(5)).thenReturn(true);

        Result<HealthController.HealthStatus> result = healthController.health();

        // 验证统一响应格式
        assertNotNull(result.getCode());
        assertNotNull(result.getMsg());
        assertNotNull(result.getData());
        assertNotNull(result.getTimestamp());
        assertNull(result.getTxHash());
    }

    /**
     * UT008: 验证timestamp为当前时间
     */
    @Test
    void testHealth_TimestampCurrent() throws SQLException {
        long before = System.currentTimeMillis();

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(5)).thenReturn(true);

        Result<HealthController.HealthStatus> result = healthController.health();

        long after = System.currentTimeMillis();

        assertTrue(result.getData().getTimestamp() >= before);
        assertTrue(result.getData().getTimestamp() <= after);
    }

    /**
     * UT009: 验证数据库连接超时处理
     */
    @Test
    void testHealth_ConnectionTimeout() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("Timeout"));

        Result<HealthController.HealthStatus> result = healthController.health();

        assertEquals("DOWN", result.getData().getDatabase());
        assertEquals("DEGRADED", result.getData().getStatus());
        verify(dataSource).getConnection();
    }

    /**
     * UT010: 验证多次调用状态一致
     */
    @Test
    void testHealth_MultipleCallsConsistent() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(5)).thenReturn(true);

        Result<HealthController.HealthStatus> result1 = healthController.health();
        Result<HealthController.HealthStatus> result2 = healthController.health();

        assertEquals(result1.getData().getStatus(), result2.getData().getStatus());
        assertEquals(result1.getData().getDatabase(), result2.getData().getDatabase());
    }
}
