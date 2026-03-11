package com.fisco.app.Common.Config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.fisco.app.Common.Enums.ResultCodeEnum;
import com.fisco.app.Common.Utils.LogUtil;
import com.fisco.app.Common.Utils.Result;

import lombok.extern.slf4j.Slf4j;

/**
 * 全局异常处理器
 * 统一处理各类异常，返回标准错误响应格式
 */
@Slf4j
@RestControllerAdvice
@ConditionalOnProperty(prefix = "app.exception-handler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class GlobalExceptionHandler {

    @Autowired
    private Environment environment;

    /**
     * 判断是否为生产环境
     */
    private boolean isProduction() {
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(MethodArgumentNotValidException ex) {
        StringBuilder message = new StringBuilder();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            message.append(error.getField()).append(": ").append(error.getDefaultMessage()).append("; ");
        }

        LogUtil.logValidationFailed("MethodArgumentNotValidException", message.toString());

        if (isProduction()) {
            return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), "参数校验失败");
        } else {
            return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), "参数校验失败: " + message, getStackTrace(ex));
        }
    }

    /**
     * 处理绑定异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException ex) {
        StringBuilder message = new StringBuilder();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            message.append(error.getField()).append(": ").append(error.getDefaultMessage()).append("; ");
        }

        LogUtil.logValidationFailed("BindException", message.toString());

        if (isProduction()) {
            return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), "参数绑定失败");
        } else {
            return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), "参数绑定失败: " + message, getStackTrace(ex));
        }
    }

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBusinessException(BusinessException ex) {
        LogUtil.logBusinessError(String.valueOf(ex.getCode()), ex.getMessage());

        if (isProduction()) {
            return Result.error(ex.getCode(), ex.getMessage());
        } else {
            return Result.error(ex.getCode(), ex.getMessage(), getStackTrace(ex));
        }
    }

    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleRuntimeException(RuntimeException ex) {
        LogUtil.logSystemError("RuntimeException", ex);

        if (isProduction()) {
            return Result.error(ResultCodeEnum.SYSTEM_ERROR.getCode(), "系统繁忙，请稍后重试");
        } else {
            return Result.error(ResultCodeEnum.SYSTEM_ERROR.getCode(), "运行时异常: " + ex.getMessage(), getStackTrace(ex));
        }
    }

    /**
     * 处理其他异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception ex) {
        LogUtil.logSystemError("Exception", ex);

        if (isProduction()) {
            return Result.error(ResultCodeEnum.SYSTEM_ERROR.getCode(), "服务器内部异常");
        } else {
            return Result.error(ResultCodeEnum.SYSTEM_ERROR.getCode(), "服务器异常: " + ex.getMessage(), getStackTrace(ex));
        }
    }

    /**
     * 获取堆栈信息
     */
    private String getStackTrace(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.getClass().getName()).append(": ").append(throwable.getMessage()).append("\n");
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("    at ").append(element.toString()).append("\n");
        }
        return sb.toString();
    }

    /**
     * 业务异常类
     */
    public static class BusinessException extends RuntimeException {
        private final Integer code;

        public BusinessException(Integer code, String message) {
            super(message);
            this.code = code;
        }

        public Integer getCode() {
            return code;
        }
    }
}
