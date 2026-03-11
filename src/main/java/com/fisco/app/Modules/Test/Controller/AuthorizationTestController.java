package com.fisco.app.Modules.Test.Controller;

import com.fisco.app.Common.Annotation.AuditLog;
import com.fisco.app.Common.Annotation.CircuitProtection;
import com.fisco.app.Common.Annotation.Idempotent;
import com.fisco.app.Common.Annotation.Timeout;
import com.fisco.app.Common.Annotation.DataOwnership;
import com.fisco.app.Common.Annotation.RateLimit;
import com.fisco.app.Common.Annotation.RequirePermission;
import com.fisco.app.Common.Annotation.RequireRole;
import com.fisco.app.Common.Annotation.ValidHexAddress;
import com.fisco.app.Common.Annotation.impl.HexAddressValidator;
import com.fisco.app.Common.Service.AsyncTaskService;
import com.fisco.app.Common.Service.EncryptionService;
import com.fisco.app.Common.Utils.AsyncTaskResult;
import com.fisco.app.Common.Utils.AuditContext;
import com.fisco.app.Common.Utils.DataMaskingUtil;
import com.fisco.app.Common.Utils.LogUtil;
import com.fisco.app.Common.DTO.TokenResponseDTO;
import com.fisco.app.Common.Utils.PageResult;
import com.fisco.app.Common.Utils.Result;
import com.fisco.app.Modules.Blockchain.Entity.BlockchainTransactionRecord;
import com.fisco.app.Modules.Blockchain.Service.BlockchainAuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

/**
 * 测试Controller - 用于验证Java应用能否正常运行
 */
@RestController
@RequestMapping("/api/test")
public class AuthorizationTestController {

    @Autowired
    private BlockchainAuditService blockchainAuditService;

    @Autowired
    private EncryptionService encryptionService;

    @Autowired
    private AsyncTaskService asyncTaskService;

    @GetMapping("/hello")
    public Result<String> hello() {
        return Result.success("Hello from FISCO BCOS Supply Chain Finance Platform!");
    }

    @GetMapping("/status")
    public Result<String> status() {
        return Result.success("Application is running normally!");
    }

    @GetMapping("/info")
    public Result<String> info() {
        String info = "FISCO BCOS Supply Chain Finance Platform v1.0";
        return Result.success(info);
    }

    // ==================== 角色权限校验测试端点 ====================

    /**
     * 无@RequireRole注解 - 应放行
     */
    @GetMapping("/public")
    public Result<String> publicEndpoint() {
        return Result.success("public: no role required");
    }

    /**
     * @RequireRole({"ADMIN"}) - 仅管理员可访问
     */
    @RequireRole({"ADMIN"})
    @GetMapping("/admin")
    public Result<String> adminEndpoint() {
        return Result.success("admin: admin role only");
    }

    /**
     * @RequireRole({"ADMIN", "FINANCE"}) - 管理员和财务可访问
     */
    @RequireRole({"ADMIN", "FINANCE"})
    @GetMapping("/finance")
    public Result<String> financeEndpoint() {
        return Result.success("finance: admin or finance role");
    }

    /**
     * @RequireRole(value = {"USER"}, adminBypass = false) - 不允许管理员绕过
     */
    @RequireRole(value = {"USER"}, adminBypass = false)
    @GetMapping("/user-only")
    public Result<String> userOnlyEndpoint() {
        return Result.success("user-only: user role only, admin cannot bypass");
    }

    // ==================== 权限等级校验测试端点 ====================

    /**
     * @RequirePermission(level = 10) - 需要管理员权限
     */
    @RequirePermission(level = 10)
    @GetMapping("/perm-admin")
    public Result<String> permAdminEndpoint() {
        return Result.success("perm-admin: requires permission level 10 (ADMIN)");
    }

    /**
     * @RequirePermission(level = 5) - 需要财务权限
     */
    @RequirePermission(level = 5)
    @GetMapping("/perm-finance")
    public Result<String> permFinanceEndpoint() {
        return Result.success("perm-finance: requires permission level 5 (FINANCE)");
    }

    /**
     * @RequirePermission(level = 5, adminBypass = false) - 不允许管理员绕过
     */
    @RequirePermission(level = 5, adminBypass = false)
    @GetMapping("/perm-user-only")
    public Result<String> permUserOnlyEndpoint() {
        return Result.success("perm-user-only: requires permission level 5, admin cannot bypass");
    }

    // ==================== 数据归属校验测试端点 ====================

    /**
     * 无@DataOwnership注解 - 应放行
     */
    @GetMapping("/ownership/public")
    public Result<String> ownershipPublic() {
        return Result.success("ownership-public: no ownership required");
    }

    /**
     * @DataOwnership(paramName = "entId") - entId匹配时放行
     */
    @DataOwnership(paramName = "entId")
    @GetMapping("/ownership/enterprise/{entId}")
    public Result<String> ownershipEnterprise(@PathVariable Long entId) {
        return Result.success("ownership-enterprise: entId=" + entId);
    }

    /**
     * @DataOwnership(paramName = "ownerId") - 自定义参数名
     */
    @DataOwnership(paramName = "ownerId")
    @GetMapping("/ownership/asset/{ownerId}")
    public Result<String> ownershipAsset(@PathVariable Long ownerId) {
        return Result.success("ownership-asset: ownerId=" + ownerId);
    }

    /**
     * @DataOwnership(paramName = "entId", adminBypass = false) - 不允许管理员绕过
     */
    @DataOwnership(paramName = "entId", adminBypass = false)
    @GetMapping("/ownership/strict/{entId}")
    public Result<String> ownershipStrict(@PathVariable Long entId) {
        return Result.success("ownership-strict: entId=" + entId);
    }

    // ==================== 审计日志测试端点 ====================

    /**
     * 无@AuditLog注解 - 应放行
     */
    @GetMapping("/audit/public")
    public Result<String> auditPublic() {
        return Result.success("audit-public: no audit");
    }

    /**
     * @AuditLog - 自动填充operatorId（需要实际调用才生效）
     */
    @AuditLog(module = "TEST", operation = "创建测试实体", autoFillOperator = true)
    @GetMapping("/audit/entity")
    public Result<AuditInfo> createWithAudit() {
        // 返回当前审计上下文信息，验证是否已设置
        AuditInfo info = new AuditInfo();
        info.setUserId(AuditContext.getUserId());
        info.setEntId(AuditContext.getEntId());
        info.setRole(AuditContext.getRole());
        return Result.success(info);
    }

    /**
     * @AuditLog(autoFillOperator = false) - 不填充
     */
    @AuditLog(module = "TEST", operation = "创建测试实体不填充", autoFillOperator = false)
    @GetMapping("/audit/entity/no-fill")
    public Result<AuditInfo> createWithoutAudit() {
        AuditInfo info = new AuditInfo();
        info.setUserId(AuditContext.getUserId());
        info.setEntId(AuditContext.getEntId());
        info.setRole(AuditContext.getRole());
        return Result.success(info);
    }

    /**
     * 审计信息内部类
     */
    public static class AuditInfo {
        private Long userId;
        private Long entId;
        private String role;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public Long getEntId() { return entId; }
        public void setEntId(Long entId) { this.entId = entId; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }

    // ==================== 链上审计测试端点 ====================

    /**
     * 记录链上交易（测试recordTransaction）
     */
    @PostMapping("/blockchain/record")
    public Result<String> recordBlockchainTransaction(@RequestBody Map<String, String> request) {
        String txHash = request.get("txHash");
        String operation = request.get("operation");
        String contractName = request.get("contractName");

        blockchainAuditService.recordTransaction(txHash, operation, contractName);

        return Result.success("Transaction recorded: " + txHash);
    }

    /**
     * 根据txHash查询记录
     */
    @GetMapping("/blockchain/record/{txHash}")
    public Result<BlockchainTransactionRecord> getRecordByTxHash(@PathVariable String txHash) {
        BlockchainTransactionRecord record = blockchainAuditService.getRecordByTxHash(txHash);
        return Result.success(record);
    }

    /**
     * 根据userId查询记录
     */
    @GetMapping("/blockchain/records/user/{userId}")
    public Result<List<BlockchainTransactionRecord>> getRecordsByUserId(@PathVariable Long userId) {
        List<BlockchainTransactionRecord> records = blockchainAuditService.getRecordsByUserId(userId);
        return Result.success(records);
    }

    /**
     * 根据entId查询记录
     */
    @GetMapping("/blockchain/records/ent/{entId}")
    public Result<List<BlockchainTransactionRecord>> getRecordsByEntId(@PathVariable Long entId) {
        List<BlockchainTransactionRecord> records = blockchainAuditService.getRecordsByEntId(entId);
        return Result.success(records);
    }

    // ==================== 加密测试端点 ====================

    /**
     * 获取RSA公钥
     */
    @GetMapping("/encryption/rsa/publickey")
    public Result<String> getRsaPublicKey() {
        String publicKey = encryptionService.getRsaPublicKey();
        return Result.success(publicKey);
    }

    /**
     * AES加密
     */
    @PostMapping("/encryption/aes/encrypt")
    public Result<String> aesEncrypt(@RequestBody Map<String, String> request) {
        String data = request.get("data");
        if (data == null || data.isEmpty()) {
            return Result.error(400, "待加密数据不能为空");
        }
        String encrypted = encryptionService.encryptWithAes(data);
        return Result.success(encrypted);
    }

    /**
     * AES解密
     */
    @PostMapping("/encryption/aes/decrypt")
    public Result<String> aesDecrypt(@RequestBody Map<String, String> request) {
        String encryptedData = request.get("data");
        if (encryptedData == null || encryptedData.isEmpty()) {
            return Result.error(400, "待解密数据不能为空");
        }
        try {
            String decrypted = encryptionService.decryptWithAes(encryptedData);
            return Result.success(decrypted);
        } catch (Exception e) {
            return Result.error(500, "解密失败: " + e.getMessage());
        }
    }

    // ==================== 区块链地址校验测试端点 ====================

    /**
     * 测试有效区块链地址
     */
    @PostMapping("/validation/hex-address")
    public Result<String> validateHexAddress(@RequestBody Map<String, String> request) {
        String address = request.get("address");
        String allowNullStr = request.getOrDefault("allowNull", "true");

        boolean allowNull = Boolean.parseBoolean(allowNullStr);

        ValidHexAddress annotation = new ValidHexAddress() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return ValidHexAddress.class;
            }
            @Override
            public String message() {
                return "Invalid blockchain address format";
            }
            @Override
            public Class<?>[] groups() {
                return new Class<?>[0];
            }
            @Override
            @SuppressWarnings("unchecked")
            public Class<? extends javax.validation.Payload>[] payload() {
                return new Class[0];
            }
            @Override
            public boolean allowNull() {
                return allowNull;
            }
        };

        HexAddressValidator validator = new HexAddressValidator();
        validator.initialize(annotation);
        boolean valid = validator.isValid(address, null);

        return Result.success("Valid: " + valid);
    }

    /**
     * 测试有效区块链地址（简化版）
     */
    @PostMapping("/validation/hex-address/simple")
    public Result<String> validateHexAddressSimple(@RequestBody Map<String, String> request) {
        String address = request.get("address");

        HexAddressValidator validator = new HexAddressValidator();
        validator.initialize(ValidHexAddress.class.getAnnotation(ValidHexAddress.class));
        boolean valid = validator.isValid(address, null);

        return Result.success(valid ? "Valid address" : "Invalid address");
    }

    // ==================== 限流测试端点 ====================

    /**
     * 限流测试 - 查询接口 (10 QPS)
     */
    @RateLimit(qps = 10, key = "query")
    @GetMapping("/rate-limit/query")
    public Result<String> rateLimitQuery() {
        return Result.success("Query success");
    }

    /**
     * 限流测试 - 写入接口 (2 QPS)
     */
    @RateLimit(qps = 2, key = "write")
    @PostMapping("/rate-limit/write")
    public Result<String> rateLimitWrite() {
        return Result.success("Write success");
    }

    /**
     * 限流测试 - 自定义QPS
     */
    @RateLimit(qps = 5, key = "custom")
    @GetMapping("/rate-limit/custom")
    public Result<String> rateLimitCustom() {
        return Result.success("Custom success");
    }

    // ==================== 熔断测试端点 ====================

    /**
     * 熔断测试 - 正常调用
     */
    @CircuitProtection(name = "default", fallbackMethod = "circuitBreakerFallback")
    @GetMapping("/circuit-breaker/normal")
    public Result<String> circuitBreakerNormal() {
        return Result.success("Circuit breaker test: normal call success");
    }

    /**
     * 熔断测试 - 模拟失败调用（用于触发熔断）
     * 连续调用失败率超过50%时触发熔断
     */
    @CircuitProtection(name = "default", fallbackMethod = "circuitBreakerFallback")
    @GetMapping("/circuit-breaker/fail")
    public Result<String> circuitBreakerFail() {
        // 模拟失败，抛出异常触发熔断
        throw new RuntimeException("Simulated failure for circuit breaker test");
    }

    /**
     * 熔断测试 - 区块链调用模拟
     */
    @CircuitProtection(name = "blockchain", fallbackMethod = "circuitBreakerFallback")
    @GetMapping("/circuit-breaker/blockchain")
    public Result<String> circuitBreakerBlockchain() {
        return Result.success("Blockchain call success");
    }

    /**
     * 熔断兜底方法
     */
    public Result<String> circuitBreakerFallback() {
        return Result.error(40004, "Service temporarily unavailable, circuit breaker is OPEN");
    }

    // ==================== 幂等性测试端点 ====================

    /**
     * 幂等性测试 - 创建接口（必需transactionId）
     */
    @Idempotent(transactionIdParam = "transactionId", expireHours = 24)
    @PostMapping("/idempotent/create")
    public Result<String> idempotentCreate(@RequestBody Map<String, String> request) {
        String transactionId = request.get("transactionId");
        return Result.success("Create success, transactionId: " + transactionId);
    }

    /**
     * 幂等性测试 - 创建接口（可选transactionId）
     */
    @Idempotent(transactionIdParam = "transactionId", required = false)
    @PostMapping("/idempotent/create-optional")
    public Result<String> idempotentCreateOptional(@RequestBody Map<String, String> request) {
        String transactionId = request.getOrDefault("transactionId", "none");
        return Result.success("Create success (optional), transactionId: " + transactionId);
    }

    /**
     * 幂等性测试 - 无幂等性校验
     */
    @PostMapping("/idempotent/no-check")
    public Result<String> idempotentNoCheck(@RequestBody Map<String, String> request) {
        return Result.success("No idempotent check success");
    }

    // ==================== 异步处理测试端点 ====================

    /**
     * 异步任务测试 - 立即返回
     * 模拟耗时任务，提交后立即返回202
     */
    @PostMapping("/async/submit")
    public Result<String> asyncSubmit(@RequestBody Map<String, String> request) {
        String data = request.getOrDefault("data", "test");
        boolean failStr = "fail".equalsIgnoreCase(request.getOrDefault("mode", "success"));

        // 提交异步任务
        String taskId = asyncTaskService.submit(() -> {
            // 模拟耗时操作
            Thread.sleep(2000);
            if (failStr) {
                throw new RuntimeException("模拟任务失败");
            }
            return "任务完成: " + data;
        }, "测试异步任务");

        // 立即返回202
        return Result.accepted("异步任务已提交", taskId);
    }

    /**
     * 异步任务测试 - 查询任务状态
     */
    @GetMapping("/async/status/{taskId}")
    public Result<AsyncTaskResult<?>> asyncStatus(@PathVariable String taskId) {
        AsyncTaskResult<?> result = asyncTaskService.getTaskResult(taskId);
        return Result.success(result);
    }

    /**
     * 异步任务测试 - 快速返回成功
     */
    @PostMapping("/async/quick")
    public Result<String> asyncQuick(@RequestBody Map<String, String> request) {
        String taskId = asyncTaskService.submit(() -> "快速任务完成");
        return Result.accepted("快速任务已提交", taskId);
    }

    // ==================== 超时控制测试端点 ====================

    /**
     * 超时测试 - 正常请求（默认5秒超时）
     */
    @Timeout
    @GetMapping("/timeout/normal")
    public Result<String> timeoutNormal() throws InterruptedException {
        Thread.sleep(1000);
        return Result.success("Normal request completed");
    }

    /**
     * 超时测试 - 短超时（1秒）
     */
    @Timeout(value = 1000)
    @GetMapping("/timeout/short")
    public Result<String> timeoutShort() throws InterruptedException {
        Thread.sleep(2000);
        return Result.success("Short timeout test");
    }

    /**
     * 超时测试 - 自定义超时 + 兜底方法
     */
    @Timeout(value = 1500, fallbackMethod = "timeoutFallback")
    @GetMapping("/timeout/fallback")
    public Result<String> timeoutWithFallback() throws InterruptedException {
        Thread.sleep(3000);
        return Result.success("This should not be reached");
    }

    /**
     * 超时兜底方法
     */
    public Result<String> timeoutFallback() {
        return Result.error(40003, "请求超时，触发兜底方法");
    }

    // ==================== 脱敏处理测试端点 ====================

    /**
     * 脱敏测试 - 手机号
     */
    @GetMapping("/mask/phone/{phone}")
    public Result<String> maskPhone(@PathVariable String phone) {
        String masked = DataMaskingUtil.maskPhone(phone);
        return Result.success(masked);
    }

    /**
     * 脱敏测试 - 身份证号
     */
    @GetMapping("/mask/idcard/{idCard}")
    public Result<String> maskIdCard(@PathVariable String idCard) {
        String masked = DataMaskingUtil.maskIdCard(idCard);
        return Result.success(masked);
    }

    /**
     * 脱敏测试 - 钱包地址
     */
    @GetMapping("/mask/address/{address}")
    public Result<String> maskAddress(@PathVariable String address) {
        String masked = DataMaskingUtil.maskWalletAddress(address);
        return Result.success(masked);
    }

    /**
     * 脱敏测试 - 银行卡号
     */
    @GetMapping("/mask/bankcard/{cardNumber}")
    public Result<String> maskBankCard(@PathVariable String cardNumber) {
        String masked = DataMaskingUtil.maskBankCard(cardNumber);
        return Result.success(masked);
    }

    /**
     * 脱敏测试 - 邮箱
     */
    @GetMapping("/mask/email/{email}")
    public Result<String> maskEmail(@PathVariable String email) {
        String masked = DataMaskingUtil.maskEmail(email);
        return Result.success(masked);
    }

    // ==================== 日志分级测试端点 ====================

    /**
     * 日志测试 - Debug级别
     */
    @GetMapping("/log/debug")
    public Result<String> logDebug() {
        LogUtil.debug("这是一条Debug日志");
        return Result.success("Debug日志已记录");
    }

    /**
     * 日志测试 - Info级别
     */
    @GetMapping("/log/info")
    public Result<String> logInfo() {
        LogUtil.info("这是一条Info日志");
        return Result.success("Info日志已记录");
    }

    /**
     * 日志测试 - Warn级别
     */
    @GetMapping("/log/warn")
    public Result<String> logWarn() {
        LogUtil.warn("这是一条Warn日志");
        return Result.success("Warn日志已记录");
    }

    /**
     * 日志测试 - Error级别
     */
    @GetMapping("/log/error")
    public Result<String> logError() {
        LogUtil.error("这是一条Error日志");
        return Result.success("Error日志已记录");
    }

    /**
     * 日志测试 - Fatal级别
     */
    @GetMapping("/log/fatal")
    public Result<String> logFatal() {
        LogUtil.fatal("这是一条Fatal日志");
        return Result.success("Fatal日志已记录");
    }

    /**
     * 日志测试 - 业务操作
     */
    @GetMapping("/log/operation")
    public Result<String> logOperation() {
        LogUtil.logOperation("TEST_OPERATION", "测试业务操作");
        return Result.success("业务操作日志已记录");
    }

    /**
     * 日志测试 - 交易日志
     */
    @GetMapping("/log/transaction/{txHash}")
    public Result<String> logTransaction(@PathVariable String txHash) {
        LogUtil.logTransactionSubmitted(txHash, "TEST_TRANSACTION");
        return Result.success("交易日志已记录, txHash=" + txHash);
    }

    // ==================== 统一响应格式测试端点 ====================

    /**
     * 响应格式测试 - 成功响应（无数据）
     */
    @GetMapping("/response/success/null")
    public Result<String> responseSuccessNull() {
        return Result.success();
    }

    /**
     * 响应格式测试 - 成功响应（有数据）
     */
    @GetMapping("/response/success/data")
    public Result<String> responseSuccessData() {
        return Result.success("测试数据");
    }

    /**
     * 响应格式测试 - 分页响应
     */
    @GetMapping("/response/page")
    public Result<PageResult<String>> responsePage() {
        java.util.List<String> list = java.util.Arrays.asList("item1", "item2", "item3");
        PageResult<String> pageResult = PageResult.of(list, 100L, 1, 10);
        return Result.success(pageResult);
    }

    /**
     * 响应格式测试 - 错误响应
     */
    @GetMapping("/response/error")
    public Result<String> responseError() {
        return Result.paramError("测试参数错误");
    }

    // ==================== 字段命名规范测试端点 ====================

    /**
     * 字段命名测试 - 验证响应使用小驼峰命名
     */
    @GetMapping("/naming/response")
    public Result<TokenResponseDTO> namingResponse() {
        TokenResponseDTO dto = new TokenResponseDTO();
        dto.setAccessToken("testAccessToken");
        dto.setRefreshToken("testRefreshToken");
        dto.setExpiresIn(7200L);
        dto.setTokenType("Bearer");
        dto.setUserId(1L);
        dto.setEntId(100L);
        return Result.success(dto);
    }

    /**
     * 字段命名测试 - 验证请求可接收下划线命名
     */
    @PostMapping("/naming/request")
    public Result<String> namingRequest(@RequestBody TokenResponseDTO dto) {
        return Result.success("接收到的字段: accessToken=" + dto.getAccessToken()
                + ", userId=" + dto.getUserId() + ", entId=" + dto.getEntId());
    }

    // ==================== 统一错误响应测试端点 ====================

    /**
     * 错误响应测试 - 返回错误码和消息
     */
    @GetMapping("/error/business")
    public Result<String> errorBusiness() {
        return Result.error(10001, "业务错误测试");
    }

    /**
     * 错误响应测试 - 返回带 errorStack（开发环境）
     */
    @GetMapping("/error/with-stack")
    public Result<String> errorWithStack() {
        try {
            throw new RuntimeException("测试异常堆栈");
        } catch (RuntimeException e) {
            return Result.error(500, "系统错误测试", "java.lang.RuntimeException: 测试异常堆栈\n    at com.fisco.app.test");
        }
    }

    /**
     * 错误响应测试 - 抛出异常由全局处理器捕获
     */
    @GetMapping("/error/throw")
    public Result<String> errorThrow() {
        throw new com.fisco.app.Common.Config.GlobalExceptionHandler.BusinessException(10002, "测试业务异常");
    }
}
