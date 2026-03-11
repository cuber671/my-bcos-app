package com.fisco.app.Common.Utils;

import java.io.Serializable;

import com.fisco.app.Common.Enums.ResultCodeEnum;

import lombok.Data;

/**
 * 通用API响应封装类
 */
@Data
public class Result<T> implements Serializable {
    private Integer code;
    private String msg;
    private T data;
    private Long timestamp;
    private String txHash;
    /**
     * 错误堆栈信息（仅测试/开发环境返回）
     */
    private String errorStack;  

    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    // --- 1. 成功响应静态方法 (Success) ---

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(ResultCodeEnum.SUCCESS.getCode());
        result.setMsg(ResultCodeEnum.SUCCESS.getMessage());
        result.setData(data);
        return result;
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 区块链业务专用的成功响应（带交易哈希）
     */
    public static <T> Result<T> success(T data, String txHash) {
        Result<T> result = success(data);
        result.setTxHash(txHash);
        return result;
    }

    // --- 1.1 异步处理响应 (Accepted 202) ---

    /**
     * 202 - 请求已接收（异步任务）
     */
    public static <T> Result<T> accepted(T data) {
        Result<T> result = new Result<>();
        result.setCode(ResultCodeEnum.ACCEPTED.getCode());
        result.setMsg(ResultCodeEnum.ACCEPTED.getMessage());
        result.setData(data);
        return result;
    }

    /**
     * 202 - 请求已接收（异步任务，带任务ID）
     */
    public static <T> Result<T> accepted(T data, String taskId) {
        Result<T> result = accepted(data);
        result.setMsg("任务已提交，taskId: " + taskId);
        return result;
    }

    // --- 2. 基础错误响应静态方法 (Base Errors) ---

    /**
     * 400 - 参数校验失败
     */
    public static <T> Result<T> paramError(String customMessage) {
        return error(ResultCodeEnum.PARAM_ERROR, customMessage);
    }

    /**
     * 401 - 尚未登录或登录超时 (JWT失效/未传)
     */
    public static <T> Result<T> unauthorized() {
        return error(ResultCodeEnum.UNAUTHORIZED);
    }

    /**
     * 403 - 权限不足 (角色不匹配或越权操作)
     */
    public static <T> Result<T> forbidden() {
        return error(ResultCodeEnum.FORBIDDEN);
    }

    /**
     * 404 - 资源不存在
     */
    public static <T> Result<T> notFound() {
        return error(ResultCodeEnum.NOT_FOUND);
    }

    /**
     * 500 - 服务器内部异常
     */
    public static <T> Result<T> systemError() {
        return error(ResultCodeEnum.SYSTEM_ERROR);
    }

    // --- 3. 通用构造方法流 ---

    /**
     * 通过枚举直接构造错误响应
     */
    public static <T> Result<T> error(ResultCodeEnum codeEnum) {
        return error(codeEnum.getCode(), codeEnum.getMessage());
    }

    /**
     * 通过枚举构造，自定义详细消息（推荐：如具体哪个参数错了）
     */
    public static <T> Result<T> error(ResultCodeEnum codeEnum, String customMessage) {
        return error(codeEnum.getCode(), customMessage);
    }

    /**
     * 最底层的错误构造方法
     */
    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMsg(message);
        return result;
    }

    /**
     * 设置错误堆栈信息（用于全局异常处理）
     */
    public static <T> Result<T> error(Integer code, String message, String errorStack) {
        Result<T> result = error(code, message);
        result.setErrorStack(errorStack);
        return result;
    }
}