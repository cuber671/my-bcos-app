package com.fisco.app.exception;

import lombok.Getter;

/**
 * 业务异常类
 */
@Getter
public class BusinessException extends RuntimeException {

    private Integer code;
    private String message;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
        this.message = message;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public BusinessException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }

    public static class EnterpriseNotFoundException extends BusinessException {
        public EnterpriseNotFoundException(String address) {
            super(404, "企业不存在: " + address);
        }
    }

    public static class ReceivableNotFoundException extends BusinessException {
        public ReceivableNotFoundException(String receivableId) {
            super(404, "应收账款不存在: " + receivableId);
        }
    }

    public static class BillNotFoundException extends BusinessException {
        public BillNotFoundException(String billId) {
            super(404, "票据不存在: " + billId);
        }
    }

    public static class WarehouseReceiptNotFoundException extends BusinessException {
        public WarehouseReceiptNotFoundException(String receiptId) {
            super(404, "仓单不存在: " + receiptId);
        }
    }

    public static class UserNotFoundException extends BusinessException {
        public UserNotFoundException(String userId) {
            super(404, "用户不存在: " + userId);
        }
    }

    public static class InvalidStatusException extends BusinessException {
        public InvalidStatusException(String message) {
            super(400, message);
        }
    }

    public static class BlockchainException extends BusinessException {
        public BlockchainException(String message) {
            super("区块链操作失败: " + message);
        }
        public BlockchainException(String message, Throwable cause) {
            super(500, "区块链操作失败: " + message, cause);
        }
    }

    public static class AdminNotFoundException extends BusinessException {
        public AdminNotFoundException(String message) {
            super(404, message);
        }
    }

    // ==================== 合约相关异常 ====================

    public static class ContractNotFoundException extends BusinessException {
        public ContractNotFoundException(String address) {
            super(404, "合约不存在: " + address);
        }
    }

    public static class InvalidContractAbiException extends BusinessException {
        public InvalidContractAbiException(String message) {
            super(400, "合约ABI格式错误: " + message);
        }
    }

    public static class ContractDeploymentFailedException extends BusinessException {
        public ContractDeploymentFailedException(String message) {
            super(500, "合约部署失败: " + message);
        }
    }
}
