// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

/**
 * @title WarehouseReceiptCancelV2
 * @dev 电子仓单作废合约 V2
 * 支持作废原因存证、操作人记录及与后端状态机同步
 */
contract WarehouseReceiptCancelV2 {

    // 仓单状态枚举（需与Java端 ReceiptStatus 对应）
    enum ReceiptStatus { 
        DRAFT,              // 0
        PENDING_ONCHAIN,    // 1
        NORMAL,             // 2
        ONCHAIN_FAILED,     // 3
        PLEDGED,            // 4
        TRANSFERRED,        // 5
        FROZEN,             // 6
        SPLITTING,          // 7
        SPLIT,              // 8
        MERGING,            // 9
        MERGED,             // 10
        CANCELLING,         // 11
        CANCELLED,          // 12
        EXPIRED,            // 13
        DELIVERED           // 14
    }

    // 仓单作废信息结构体
    struct CancelInfo {
        string receiptId;       // 对应后端 UUID
        string cancelReason;    // 作废原因
        string cancelType;      // 作废类型 (如: QUALITY_ISSUE)
        string cancelledBy;     // 操作人ID
        uint256 cancelTime;     // 链上作废时间戳
        bool isExist;
    }

    // 仓单当前状态映射：receiptId => Status
    mapping(string => ReceiptStatus) public receiptStatuses;
    // 仓单作废详情映射：receiptId => CancelInfo
    mapping(string => CancelInfo) public cancelRecords;

    // 事件：用于后端通过 Web3SDK 监听并更新数据库
    event ReceiptCancelled(
        string indexed receiptId, 
        string cancelType, 
        string cancelledBy, 
        uint256 timestamp
    );

    /**
     * @dev 执行仓单作废
     * @param _receiptId 仓单ID
     * @param _reason 作废原因
     * @param _cancelType 作废类型
     * @param _operatorId 操作人ID (对应后端 userId)
     */
    function cancelReceipt(
        string memory _receiptId,
        string memory _reason,
        string memory _cancelType,
        string memory _operatorId
    ) public {
        // 1. 权限与状态校验
        ReceiptStatus currentStatus = receiptStatuses[_receiptId];
        
        // 只有 NORMAL (2) 或 ONCHAIN_FAILED (3) 状态允许作废
        require(
            currentStatus == ReceiptStatus.NORMAL || currentStatus == ReceiptStatus.ONCHAIN_FAILED,
            "Contract Error: Only NORMAL or FAILED status can be cancelled"
        );

        // 2. 更新状态
        receiptStatuses[_receiptId] = ReceiptStatus.CANCELLED;

        // 3. 记录作废元数据（对应后端新增的 5 个作废相关字段）
        cancelRecords[_receiptId] = CancelInfo({
            receiptId: _receiptId,
            cancelReason: _reason,
            cancelType: _cancelType,
            cancelledBy: _operatorId,
            cancelTime: block.timestamp,
            isExist: true
        });

        // 4. 抛出事件
        emit ReceiptCancelled(_receiptId, _cancelType, _operatorId, block.timestamp);
    }

    /**
     * @dev 模拟初始化仓单状态（实际应由创建合约调用或在此合约维护）
     */
    function initReceiptStatus(string memory _receiptId, uint8 _status) public {
        receiptStatuses[_receiptId] = ReceiptStatus(_status);
    }

    /**
     * @dev 查询作废详情（供后端对账使用）
     */
    function getCancelDetail(string memory _receiptId) public view returns (
        string memory reason,
        string memory cType,
        string memory operator,
        uint256 time
    ) {
        require(cancelRecords[_receiptId].isExist, "Record not found");
        CancelInfo storage info = cancelRecords[_receiptId];
        return (info.cancelReason, info.cancelType, info.cancelledBy, info.cancelTime);
    }
}