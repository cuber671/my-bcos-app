package com.fisco.app.config;
import com.fisco.app.vo.Result;

import com.fisco.app.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理应用中的各种异常，记录详细的日志信息
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常处理
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常 [{}]: code={}, message={}, uri={}",
            e.getClass().getSimpleName(),
            e.getCode(),
            e.getMessage(),
            request.getRequestURI()
        );
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常处理
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        log.warn("参数校验失败 [{}]: field={}, message={}, uri={}",
            e.getBindingResult().getObjectName(),
            message,
            request.getRequestURI()
        );

        return Result.paramError(message);
    }

    /**
     * 参数绑定异常处理
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleBindException(BindException e, HttpServletRequest request) {
        String message = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        log.warn("参数绑定失败 [{}]: field={}, message={}, uri={}",
            e.getObjectName(),
            message,
            request.getRequestURI()
        );

        return Result.paramError(message);
    }

    /**
     * 约束违反异常处理
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleConstraintViolationException(ConstraintViolationException e, HttpServletRequest request) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));

        log.warn("约束违反: message={}, uri={}", message, request.getRequestURI());

        return Result.paramError(message);
    }

    /**
     * 请求参数缺失异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleMissingParameterException(MissingServletRequestParameterException e, HttpServletRequest request) {
        log.warn("请求参数缺失: parameter={}, type={}, uri={}",
            e.getParameterName(),
            e.getParameterType(),
            request.getRequestURI()
        );

        return Result.paramError("缺少必需参数: " + e.getParameterName());
    }

    /**
     * 参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleTypeMismatchException(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        Class<?> requiredType = e.getRequiredType();
        String typeName = requiredType != null ? requiredType.getSimpleName() : "unknown";

        log.warn("参数类型不匹配: parameter={}, requiredType={}, value={}, uri={}",
            e.getName(),
            typeName,
            e.getValue(),
            request.getRequestURI()
        );

        return Result.paramError("参数类型错误: " + e.getName());
    }

    /**
     * 请求体读取失败异常
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleMessageNotReadableException(HttpMessageNotReadableException e, HttpServletRequest request) {
        log.warn("请求体读取失败: message={}, uri={}",
            e.getMessage(),
            request.getRequestURI()
        );

        return Result.paramError("请求体格式错误或缺失");
    }

    /**
     * 认证失败异常
     */
    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<?> handleBadCredentialsException(BadCredentialsException e, HttpServletRequest request) {
        log.warn("认证失败: message={}, uri={}, ip={}",
            e.getMessage(),
            request.getRequestURI(),
            getClientIp(request)
        );

        return Result.error(401, "用户名或密码错误");
    }

    /**
     * 访问拒绝异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<?> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        log.warn("访问被拒绝: message={}, uri={}, ip={}",
            e.getMessage(),
            request.getRequestURI(),
            getClientIp(request)
        );

        return Result.error(403, "权限不足，无法访问");
    }

    /**
     * 404异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<?> handleNotFoundException(NoHandlerFoundException e, HttpServletRequest request) {
        log.warn("接口不存在: method={}, uri={}, ip={}",
            e.getHttpMethod(),
            e.getRequestURL(),
            getClientIp(request)
        );

        return Result.error(404, "接口不存在: " + e.getRequestURL());
    }

    /**
     * 系统异常处理
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常: type={}, message={}, uri={}, ip={}",
            e.getClass().getName(),
            e.getMessage(),
            request.getRequestURI(),
            getClientIp(request),
            e
        );

        return Result.error("系统异常，请稍后重试");
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 处理多个IP的情况（取第一个）
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }
}
