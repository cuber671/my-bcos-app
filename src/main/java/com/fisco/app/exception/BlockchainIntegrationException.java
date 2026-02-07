package com.fisco.app.exception;

import lombok.Getter;

/**
 * 区块链集成异常类
 * 用于封装智能合约调用失败异常，触发事务回滚
 *
 * 此异常与 BusinessException.BlockchainException 的区别：
 * - BlockchainException: 业务层面的区块链错误（如状态不符、权限不足）
 * - BlockchainIntegrationException: 技术层面的集成错误（如网络超时、合约执行失败、交易回滚）
 *
 * 使用场景：
 * - 合约调用失败（TransactionReceipt.status != 0）
 * - 网络超时或连接失败
 * - 合约地址无效
 * - 参数转换错误
 * - 交易回滚（revert）
 */
@Getter
public class BlockchainIntegrationException extends RuntimeException {

    protected String contractAddress;
    protected String contractMethod;
    protected String transactionHash;
    protected String errorMessage;
    protected Integer errorCode;

    /**
     * 基础构造函数
     */
    public BlockchainIntegrationException(String message) {
        super(message);
        this.errorMessage = message;
        this.errorCode = 500;
    }

    /**
     * 带错误代码的构造函数
     */
    public BlockchainIntegrationException(Integer errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.errorMessage = message;
    }

    /**
     * 带原始异常的构造函数
     */
    public BlockchainIntegrationException(String message, Throwable cause) {
        super(message, cause);
        this.errorMessage = message;
        this.errorCode = 500;
    }

    /**
     * 完整构造函数（包含合约信息）
     */
    public BlockchainIntegrationException(
            String message,
            String contractAddress,
            String contractMethod,
            String transactionHash,
            Throwable cause) {
        super(message, cause);
        this.errorMessage = message;
        this.contractAddress = contractAddress;
        this.contractMethod = contractMethod;
        this.transactionHash = transactionHash;
        this.errorCode = 500;
    }

    /**
     * 合约调用失败异常
     */
    public static class ContractCallException extends BlockchainIntegrationException {
        public ContractCallException(String contractAddress, String method, String message, Throwable cause) {
            super(String.format("合约调用失败 [%s.%s]: %s", contractAddress, method, message),
                  contractAddress, method, null, cause);
            this.contractAddress = contractAddress;
            this.contractMethod = method;
        }
    }

    /**
     * 交易回滚异常
     */
    public static class TransactionRevertException extends BlockchainIntegrationException {
        public TransactionRevertException(String contractAddress, String method, String revertMessage) {
            super(String.format("交易回滚 [%s.%s]: %s", contractAddress, method, revertMessage),
                  contractAddress, method, null, null);
            this.contractAddress = contractAddress;
            this.contractMethod = method;
        }
    }

    /**
     * 合约未找到异常
     */
    public static class ContractNotFoundException extends BlockchainIntegrationException {
        public ContractNotFoundException(String contractAddress) {
            super(String.format("合约不存在或未部署: %s", contractAddress));
        }
    }

    /**
     * 网络超时异常
     */
    public static class NetworkTimeoutException extends BlockchainIntegrationException {
        public NetworkTimeoutException(String contractAddress, String method, long timeoutMs) {
            super(String.format("区块链网络超时 [%s.%s]: 超时时间 %dms", contractAddress, method, timeoutMs));
        }
    }

    /**
     * 参数转换异常
     */
    public static class ParameterConversionException extends BlockchainIntegrationException {
        public ParameterConversionException(String paramName, Object value, String targetType) {
            super(String.format("参数转换失败 [%s]: 无法将 %s 转换为 %s",
                              paramName, value.getClass().getSimpleName(), targetType));
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("BlockchainIntegrationException{");
        sb.append("errorCode=").append(errorCode);
        sb.append(", errorMessage='").append(errorMessage).append("'");

        if (contractAddress != null) {
            sb.append(", contractAddress='").append(contractAddress).append("'");
        }
        if (contractMethod != null) {
            sb.append(", contractMethod='").append(contractMethod).append("'");
        }
        if (transactionHash != null) {
            sb.append(", transactionHash='").append(transactionHash).append("'");
        }

        sb.append("}");
        return sb.toString();
    }
}
