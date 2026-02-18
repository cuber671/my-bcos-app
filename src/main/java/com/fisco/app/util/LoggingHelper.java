package com.fisco.app.util;

import lombok.extern.slf4j.Slf4j;

/**
 * 日志工具类
 * 提供统一的日志格式和常用的日志模板方法
 *
 * 功能：
 * 1. 业务流程日志（带边框和分隔符）
 * 2. 性能监控日志
 * 3. 数据操作日志
 * 4. 区块链操作日志
 *
 * @author FISCO BCOS
 * @since 2025-01-19
 */
@Slf4j
public class LoggingHelper {

    private static final int BORDER_WIDTH = 64;

    /**
     * 记录业务流程开始
     *
     * @param operationName 操作名称
     */
    public static void logOperationStart(String operationName) {
        log.info("╔═══ {} 开始 ═══", operationName);
    }

    /**
     * 记录业务流程结束
     *
     * @param operationName 操作名称
     * @param success 是否成功
     * @param duration 耗时（毫秒）
     */
    public static void logOperationEnd(String operationName, boolean success, long duration) {
        if (success) {
            log.info("✓✓✓ {} 成功完成，耗时: {}ms", operationName, duration);
        } else {
            log.error("✗✗✗ {} 失败，耗时: {}ms", operationName, duration);
        }
        log.info("╚═══ {} 结束 ═══", operationName);
    }

    /**
     * 记录业务流程结束（简化版）
     *
     * @param operationName 操作名称
     */
    public static void logOperationEnd(String operationName) {
        log.info("╚═══ {} 结束 ═══", operationName);
    }

    /**
     * 记录区块链操作开始
     *
     * @param contractName 合约名称
     * @param methodName 方法名称
     * @param params 参数键值对
     */
    public static void logBlockchainOperationStart(String contractName, String methodName, Object... params) {
        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║           区块链操作开始                                     ║");
        log.info("╚════════════════════════════════════════════════════════════╝");
        log.info("合约: {}.{}", contractName, methodName);

        if (params != null && params.length > 0) {
            StringBuilder sb = new StringBuilder("参数: ");
            for (int i = 0; i < params.length; i += 2) {
                if (i + 1 < params.length) {
                    sb.append(params[i]).append("=").append(params[i + 1]);
                    if (i + 2 < params.length) {
                        sb.append(", ");
                    }
                }
            }
            log.info(sb.toString());
        }
    }

    /**
     * 记录区块链操作成功
     *
     * @param methodName 方法名称
     * @param txHash 交易哈希
     * @param duration 耗时
     */
    public static void logBlockchainOperationSuccess(String methodName, String txHash, long duration) {
        log.info("✓✓✓ 区块链操作成功: {}", methodName);
        log.info("  交易哈希: {}", txHash);
        log.info("  执行时间: {}ms", duration);
        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║           区块链操作结束                                     ║");
        log.info("╚════════════════════════════════════════════════════════════╝");
    }

    /**
     * 记录区块链操作失败
     *
     * @param methodName 方法名称
     * @param duration 耗时
     * @param error 错误信息
     */
    public static void logBlockchainOperationFailure(String methodName, long duration, String error) {
        log.error("✗✗✗ 区块链操作失败: {}", methodName);
        log.error("  错误信息: {}", error);
        log.error("  执行时间: {}ms", duration);
        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║       区块链操作失败（结束）                                 ║");
        log.info("╚════════════════════════════════════════════════════════════╝");
    }

    /**
     * 记录数据库操作
     *
     * @param operation 操作类型（INSERT, UPDATE, DELETE, SELECT）
     * @param entityName 实体名称
     * @param id 实体ID
     * @param success 是否成功
     */
    public static void logDatabaseOperation(String operation, String entityName, Object id, boolean success) {
        String symbol = success ? "✓" : "✗";
        log.debug("{} 数据库操作: {} {}, id={}", symbol, operation, entityName, id);
    }

    /**
     * 记录性能指标
     *
     * @param operation 操作名称
     * @param duration 耗时（毫秒）
     * @param threshold 警告阈值（毫秒）
     */
    public static void logPerformance(String operation, long duration, long threshold) {
        if (duration > threshold) {
            log.warn("⚠️  性能警告: {} 耗时 {}ms，超过阈值 {}ms", operation, duration, threshold);
        } else {
            log.debug("性能指标: {} 耗时 {}ms", operation, duration);
        }
    }

    /**
     * 记录数据验证
     *
     * @param fieldName 字段名称
     * @param value 字段值
     * @param valid 是否有效
     */
    public static void logValidation(String fieldName, Object value, boolean valid) {
        String symbol = valid ? "✓" : "✗";
        log.debug("{} 验证: {} = {}", symbol, fieldName, value);
    }

    /**
     * 记录状态变更
     *
     * @param entityName 实体名称
     * @param id 实体ID
     * @param field 字段名称
     * @param oldValue 旧值
     * @param newValue 新值
     */
    public static void logStateChange(String entityName, Object id, String field, Object oldValue, Object newValue) {
        log.info("状态变更: {} [id={}] {} {} -> {}", entityName, id, field, oldValue, newValue);
    }

    /**
     * 记录关键步骤
     *
     * @param step 步骤描述
     */
    public static void logStep(String step) {
        log.debug("→ {}", step);
    }

    /**
     * 记录关键步骤完成
     *
     * @param step 步骤描述
     */
    public static void logStepComplete(String step) {
        log.debug("✓ {}", step);
    }

    /**
     * 记录数据脱敏信息
     *
     * @param fieldName 字段名称
     * @param value 原始值
     * @param maskedValue 脱敏后的值
     */
    public static void logMaskedData(String fieldName, String value, String maskedValue) {
        log.debug("脱敏数据: {} = {}", fieldName, maskedValue);
    }

    /**
     * 格式化分隔线
     *
     * @return 分隔线字符串
     */
    public static String separator() {
        return "═".repeat(BORDER_WIDTH);
    }

    /**
     * 记录请求参数（带脱敏）
     *
     * @param paramName 参数名
     * @param paramValue 参数值
     * @param sensitive 是否敏感信息
     */
    public static void logParameter(String paramName, Object paramValue, boolean sensitive) {
        if (sensitive) {
            log.debug("参数[{}]: ***（敏感信息）", paramName);
        } else {
            log.debug("参数[{}]: {}", paramName, paramValue);
        }
    }

    /**
     * 记录异常堆栈（简化版）
     *
     * @param e 异常对象
     * @param message 附加消息
     */
    public static void logException(Exception e, String message) {
        log.error("异常: {} - {}", message, e.getClass().getSimpleName());
        log.error("错误信息: {}", e.getMessage());
        if (log.isDebugEnabled()) {
            log.debug("详细堆栈:", e);
        }
    }

    /**
     * 记录统计信息
     *
     * @param operation 操作名称
     * @param count 数量
     */
    public static void logStatistics(String operation, long count) {
        log.info("统计: {} 共 {} 条记录", operation, count);
    }

    /**
     * 记录业务规则检查
     *
     * @param ruleName 规则名称
     * @param passed 是否通过
     * @param details 详细信息
     */
    public static void logBusinessRule(String ruleName, boolean passed, String details) {
        String symbol = passed ? "✓" : "✗";
        String status = passed ? "通过" : "失败";
        log.debug("{} 业务规则[{}]: {} - {}", symbol, ruleName, status, details);
    }
}
