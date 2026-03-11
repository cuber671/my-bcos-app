package com.fisco.app.Common.Utils;

import com.fisco.app.Common.Enums.ResultCodeEnum;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Result 统一响应类单元测试
 *
 * 测试覆盖：
 * - UT001-UT003: success() / success(data) / success(data, txHash)
 * - UT004-UT005: accepted() / accepted(data, taskId)
 * - UT006-UT010: paramError, unauthorized, forbidden, notFound, systemError
 * - UT011-UT015: 边界测试
 */
class ResultTest {

    // ==================== 成功响应测试 ====================

    /**
     * UT001: 测试success()无参数 - 返回code=0, data=null
     */
    @Test
    void testSuccess_NoData() {
        Result<String> result = Result.success();

        assertEquals(0, result.getCode());
        assertEquals("操作成功", result.getMsg());
        assertNull(result.getData());
        assertNotNull(result.getTimestamp());
    }

    /**
     * UT002: 测试success(data) - 返回带数据的成功响应
     */
    @Test
    void testSuccess_WithData() {
        String data = "测试数据";
        Result<String> result = Result.success(data);

        assertEquals(0, result.getCode());
        assertEquals("操作成功", result.getMsg());
        assertEquals(data, result.getData());
        assertNotNull(result.getTimestamp());
    }

    /**
     * UT003: 测试success(data, txHash) - 返回带交易哈希的成功响应
     */
    @Test
    void testSuccess_WithTxHash() {
        String data = "测试数据";
        String txHash = "0xabc123";
        Result<String> result = Result.success(data, txHash);

        assertEquals(0, result.getCode());
        assertEquals("操作成功", result.getMsg());
        assertEquals(data, result.getData());
        assertEquals(txHash, result.getTxHash());
    }

    // ==================== 异步处理响应测试 ====================

    /**
     * UT004: 测试accepted() - 返回202
     */
    @Test
    void testAccepted() {
        Result<String> result = Result.accepted("task");

        assertEquals(202, result.getCode());
        assertEquals("请求已接收", result.getMsg());
        assertEquals("task", result.getData());
    }

    /**
     * UT005: 测试accepted(data, taskId) - 返回带任务ID的202响应
     */
    @Test
    void testAccepted_WithTaskId() {
        Result<String> result = Result.accepted("data", "task123");

        assertEquals(202, result.getCode());
        assertEquals("任务已提交，taskId: task123", result.getMsg());
    }

    // ==================== 错误响应测试 ====================

    /**
     * UT006: 测试paramError - 参数校验失败
     */
    @Test
    void testParamError() {
        Result<String> result = Result.paramError("用户名不能为空");

        assertEquals(400, result.getCode());
        assertEquals("用户名不能为空", result.getMsg());
    }

    /**
     * UT007: 测试unauthorized - 401未授权
     */
    @Test
    void testUnauthorized() {
        Result<String> result = Result.unauthorized();

        assertEquals(401, result.getCode());
        assertEquals("尚未登录或登录超时", result.getMsg());
    }

    /**
     * UT008: 测试forbidden - 403禁止访问
     */
    @Test
    void testForbidden() {
        Result<String> result = Result.forbidden();

        assertEquals(403, result.getCode());
        assertEquals("权限不足，拒绝访问", result.getMsg());
    }

    /**
     * UT009: 测试notFound - 404未找到
     */
    @Test
    void testNotFound() {
        Result<String> result = Result.notFound();

        assertEquals(404, result.getCode());
        assertEquals("资源不存在", result.getMsg());
    }

    /**
     * UT010: 测试systemError - 500系统错误
     */
    @Test
    void testSystemError() {
        Result<String> result = Result.systemError();

        assertEquals(500, result.getCode());
        assertEquals("服务器内部异常", result.getMsg());
    }

    // ==================== 通用错误构造方法测试 ====================

    /**
     * UT011: 测试error(ResultCodeEnum) - 通过枚举构造错误
     */
    @Test
    void testError_WithEnum() {
        Result<String> result = Result.error(ResultCodeEnum.PARAM_ERROR);

        assertEquals(400, result.getCode());
        assertEquals("参数校验失败", result.getMsg());
    }

    /**
     * UT012: 测试error(ResultCodeEnum, customMessage) - 自定义消息
     */
    @Test
    void testError_WithEnumAndMessage() {
        Result<String> result = Result.error(ResultCodeEnum.PARAM_ERROR, "自定义错误消息");

        assertEquals(400, result.getCode());
        assertEquals("自定义错误消息", result.getMsg());
    }

    /**
     * UT013: 测试error(code, message) - 最底层构造方法
     */
    @Test
    void testError_WithCodeAndMessage() {
        Result<String> result = Result.error(999, "自定义错误");

        assertEquals(999, result.getCode());
        assertEquals("自定义错误", result.getMsg());
    }

    // ==================== 边界测试 ====================

    /**
     * UT014: 测试success带null数据
     */
    @Test
    void testSuccess_WithNullData() {
        Result<String> result = Result.success(null);

        assertEquals(0, result.getCode());
        assertNull(result.getData());
    }

    /**
     * UT015: 测试accepted带null数据
     */
    @Test
    void testAccepted_WithNullData() {
        Result<String> result = Result.accepted(null);

        assertEquals(202, result.getCode());
        assertNull(result.getData());
    }

    /**
     * UT016: 测试txHash为null
     */
    @Test
    void testSuccess_TxHashNull() {
        Result<String> result = Result.success("data", null);

        assertNull(result.getTxHash());
    }

    /**
     * UT017: 测试不同数据类型
     */
    @Test
    void testSuccess_DifferentDataTypes() {
        // Integer
        Result<Integer> intResult = Result.success(100);
        assertEquals(100, intResult.getData());

        // List
        List<String> list = List.of("a", "b", "c");
        Result<List<String>> listResult = Result.success(list);
        assertEquals(list, listResult.getData());

        // Object
        Result<Object> objResult = Result.success(new Object());
        assertNotNull(objResult.getData());
    }

    /**
     * UT018: 测试timestamp字段
     */
    @Test
    void testTimestamp() {
        long before = System.currentTimeMillis();
        Result<String> result = Result.success();
        long after = System.currentTimeMillis();

        assertNotNull(result.getTimestamp());
        assertTrue(result.getTimestamp() >= before);
        assertTrue(result.getTimestamp() <= after);
    }

    // ==================== errorStack 测试 ====================

    /**
     * UT019: 测试errorStack字段存在且默认为null
     */
    @Test
    void testErrorStack_DefaultNull() {
        Result<String> result = Result.success();
        assertNull(result.getErrorStack());
    }

    /**
     * UT020: 测试error方法不带errorStack - errorStack为null
     */
    @Test
    void testError_WithoutErrorStack() {
        Result<String> result = Result.error(500, "系统错误");

        assertEquals(500, result.getCode());
        assertEquals("系统错误", result.getMsg());
        assertNull(result.getErrorStack());
    }

    /**
     * UT021: 测试error方法带errorStack - errorStack正确设置
     */
    @Test
    void testError_WithErrorStack() {
        String stackTrace = "java.lang.RuntimeException: 测试异常\n    at com.fisco.app.test";
        Result<String> result = Result.error(500, "系统错误", stackTrace);

        assertEquals(500, result.getCode());
        assertEquals("系统错误", result.getMsg());
        assertEquals(stackTrace, result.getErrorStack());
    }

    /**
     * UT022: 测试业务错误响应格式化 - 带errorStack
     */
    @Test
    void testBusinessError_WithErrorStack() {
        String stack = "BusinessException: 业务异常";
        Result<String> result = Result.error(10001, "业务错误", stack);

        assertEquals(10001, result.getCode());
        assertEquals("业务错误", result.getMsg());
        assertEquals(stack, result.getErrorStack());
    }

    /**
     * UT023: 测试参数校验错误响应格式化 - 不带errorStack
     */
    @Test
    void testParamError_WithoutErrorStack() {
        Result<String> result = Result.paramError("参数不能为空");

        assertEquals(400, result.getCode());
        // paramError使用自定义消息
        assertTrue(result.getMsg().contains("参数不能为空"));
        assertNull(result.getErrorStack());
    }
}
