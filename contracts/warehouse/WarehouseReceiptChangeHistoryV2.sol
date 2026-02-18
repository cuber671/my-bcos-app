// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

/**
 * @title WarehouseReceiptChangeHistoryV2
 * @dev 电子仓单变更历史溯源合约
 * 记录仓单全生命周期的属性变更，提供不可篡改的审计追踪
 */
contract WarehouseReceiptChangeHistoryV2 {

    // 变更类型枚举 (需与后端 ChangeType 对应)
    enum ChangeType {
        STATUS_CHANGE,      // 状态变更
        HOLDER_CHANGE,      // 持有人/持单地址变更
        QUANTITY_CHANGE,    // 数量变更
        VALUE_CHANGE,       // 金额/价值变更
        EXPIRY_CHANGE,      // 有效期变更
        LOCATION_CHANGE,    // 仓位/存储位置变更
        FROZEN_CHANGE,      // 冻结/解冻相关
        INFO_UPDATE         // 其他基础信息更新
    }

    // 变更记录结构体
    struct ChangeRecord {
        string receiptId;       // 仓单ID (UUID)
        ChangeType changeType;  // 变更类型
        string oldValue;        // 变更前值 (建议存Json或序列化字符串)
        string newValue;        // 变更后值
        string operatorId;      // 操作人ID
        string operatorName;    // 操作人姓名
        string reason;          // 变更原因
        uint256 timestamp;      // 链上存证时间
        bytes32 txHash;         // 对应的交易哈希 (辅助追溯)
    }

    // 存储映射：receiptId => 变更记录数组
    mapping(string => ChangeRecord[]) private receiptHistories;

    // 事件：便于后端订阅并推送给审计系统
    event ReceiptHistoryRecorded(
        string indexed receiptId,
        ChangeType indexed changeType,
        string operatorId,
        uint256 timestamp
    );

    /**
     * @dev 记录一次变更操作
     * 由后端 Service 在完成业务逻辑后调用
     */
    function recordChange(
        string memory _receiptId,
        uint8 _typeIndex,
        string memory _oldValue,
        string memory _newValue,
        string memory _operatorId,
        string memory _operatorName,
        string memory _reason
    ) public {
        require(_typeIndex <= uint8(ChangeType.INFO_UPDATE), "Invalid ChangeType");

        ChangeRecord memory newRecord = ChangeRecord({
            receiptId: _receiptId,
            changeType: ChangeType(_typeIndex),
            oldValue: _oldValue,
            newValue: _newValue,
            operatorId: _operatorId,
            operatorName: _operatorName,
            reason: _reason,
            timestamp: block.timestamp,
            txHash: bytes32(0) // 实际使用中可通过 blockhash 获取或由后端补录
        });

        receiptHistories[_receiptId].push(newRecord);

        emit ReceiptHistoryRecorded(_receiptId, ChangeType(_typeIndex), _operatorId, block.timestamp);
    }

    /**
     * @dev 获取指定仓单的所有历史记录
     */
    function getHistory(string memory _receiptId) public view returns (ChangeRecord[] memory) {
        return receiptHistories[_receiptId];
    }

    /**
     * @dev 获取历史记录总数 (分页辅助)
     */
    function getHistoryCount(string memory _receiptId) public view returns (uint256) {
        return receiptHistories[_receiptId].length;
    }

    /**
     * @dev 按索引获取单条历史记录
     */
    function getHistoryByIndex(string memory _receiptId, uint256 _index) public view returns (ChangeRecord memory) {
        require(_index < receiptHistories[_receiptId].length, "Index out of bounds");
        return receiptHistories[_receiptId][_index];
    }
}