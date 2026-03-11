package com.fisco.app.Common.Utils;

import org.slf4j.MDC;

import lombok.extern.slf4j.Slf4j;

/**
 * 统一日志工具类
 * 自动从MDC获取traceId、userId等上下文信息
 * 支持敏感信息脱敏
 */
@Slf4j
public class LogUtil {

    public static final String TRACE_ID = "traceId";
    public static final String USER_ID = "userId";
    public static final String TX_HASH = "txHash";

    // ========== 通用日志方法 ==========

    /**
     * Debug级别日志
     */
    public static void debug(String message, Object... args) {
        log.debug(buildMessage(message), args);
    }

    /**
     * Info级别日志
     */
    public static void info(String message, Object... args) {
        log.info(buildMessage(message), args);
    }

    /**
     * Warn级别日志
     */
    public static void warn(String message, Object... args) {
        log.warn(buildMessage(message), args);
    }

    /**
     * Error级别日志
     */
    public static void error(String message, Object... args) {
        log.error(buildMessage(message), args);
    }

    /**
     * Error级别日志（带异常）
     */
    public static void error(Throwable throwable, String message, Object... args) {
        log.error(buildMessage(message), throwable, args);
    }

    /**
     * Fatal级别日志（系统严重错误）
     */
    public static void fatal(String message, Object... args) {
        log.error(buildMessage("[FATAL] " + message), args);
    }

    /**
     * Fatal级别日志（带异常）
     */
    public static void fatal(Throwable throwable, String message, Object... args) {
        log.error(buildMessage("[FATAL] " + message), throwable, args);
    }

    // ========== 业务日志方法 ==========

    /**
     * 记录交易提交成功
     */
    public static void logTransactionSubmitted(String txHash, String operation) {
        MDC.put(TX_HASH, txHash);
        log.info("交易已提交: operation={}, txHash={}", operation, txHash);
        MDC.remove(TX_HASH);
    }

    /**
     * 记录交易确认成功
     */
    public static void logTransactionConfirmed(String txHash, String operation) {
        MDC.put(TX_HASH, txHash);
        log.info("交易已确认: operation={}, txHash={}", operation, txHash);
        MDC.remove(TX_HASH);
    }

    /**
     * 记录业务操作
     */
    public static void logOperation(String operation, String detail) {
        log.info("业务操作: operation={}, detail={}", operation, detail);
    }

    /**
     * 记录用户操作
     */
    public static void logUserAction(String action, String target) {
        log.info("用户操作: action={}, target={}", action, target);
    }

    /**
     * 记录参数校验失败
     */
    public static void logValidationFailed(String field, String reason) {
        log.warn("参数校验失败: field={}, reason={}", field, reason);
    }

    /**
     * 记录系统异常
     */
    public static void logSystemError(String module, Throwable throwable) {
        log.error("系统异常: module={}", module, throwable);
    }

    /**
     * 记录业务异常
     */
    public static void logBusinessError(String code, String message) {
        log.warn("业务异常: code={}, message={}", code, message);
    }

    // ========== 私有方法 ==========

    /**
     * 构建带有上下文信息的日志消息
     */
    private static String buildMessage(String message) {
        String traceId = MDC.get(TRACE_ID);
        String userId = MDC.get(USER_ID);
        String txHash = MDC.get(TX_HASH);

        StringBuilder sb = new StringBuilder();
        if (traceId != null) {
            sb.append("[traceId:").append(traceId).append("] ");
        }
        if (userId != null) {
            sb.append("[userId:").append(userId).append("] ");
        }
        if (txHash != null) {
            sb.append("[txHash:").append(txHash).append("] ");
        }
        sb.append(message);

        return sb.toString();
    }

    /**
     * 设置交易哈希到MDC
     */
    public static void setTxHash(String txHash) {
        if (txHash != null) {
            MDC.put(TX_HASH, txHash);
        }
    }

    /**
     * 清除交易哈希
     */
    public static void clearTxHash() {
        MDC.remove(TX_HASH);
    }
}
