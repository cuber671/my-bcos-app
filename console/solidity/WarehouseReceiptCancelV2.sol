// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

/**
 * @title WarehouseReceiptCancelV2
 * @dev 电子仓单作废合约 V2 - 增加权限管理与构造函数
 */
contract WarehouseReceiptCancelV2 {

    // ==================== 权限管理 ====================
    address public admin;
    mapping(address => bool) public authorizedCallers;

    // ==================== 数据结构 ====================
    enum ReceiptStatus { 
        DRAFT, PENDING_ONCHAIN, NORMAL, ONCHAIN_FAILED, PLEDGED,
        TRANSFERRED, FROZEN, SPLITTING, SPLIT, MERGING, MERGED,
        CANCELLING, CANCELLED, EXPIRED, DELIVERED 
    }

    struct CancelInfo {
        string receiptId;
        string cancelReason;
        string cancelType;
        string cancelledBy;
        uint256 cancelTime;
        bool isExist;
    }

    mapping(string => ReceiptStatus) public receiptStatuses;
    mapping(string => CancelInfo) public cancelRecords;

    // ==================== 事件 ====================
    event ReceiptCancelled(string indexed receiptId, string cancelType, string cancelledBy, uint256 timestamp);
    event CallerAuthorized(address indexed caller, bool status);

    // ==================== 权限修饰器 ====================
    modifier onlyAdmin() {
        require(msg.sender == admin, "Auth: Only admin can perform this action");
        _;
    }

    modifier onlyAuthorized() {
        require(msg.sender == admin || authorizedCallers[msg.sender], "Auth: Caller is not authorized");
        _;
    }

    // ==================== 构造函数 ====================
    /**
     * @dev 构造函数：初始化管理员并授权
     * @param _admin 管理员地址（通常由 Java 后端配置中心传入）
     */
    constructor(address _admin) {
        require(_admin != address(0), "Admin address cannot be zero");
        admin = _admin;
        // 默认将管理员设为授权调用者
        authorizedCallers[_admin] = true;
    }

    // ==================== 管理接口 ====================
    /**
     * @dev 授权特定的 Java 后端节点地址进行操作
     */
    function setAuthorizedCaller(address _caller, bool _status) external onlyAdmin {
        require(_caller != address(0), "Invalid address");
        authorizedCallers[_caller] = _status;
        emit CallerAuthorized(_caller, _status);
    }

    // ==================== 核心业务逻辑 ====================

    /**
     * @dev 执行仓单作废 (增加 onlyAuthorized 权限)
     */
    function cancelReceipt(
        string memory _receiptId,
        string memory _reason,
        string memory _cancelType,
        string memory _operatorId
    ) public onlyAuthorized { // <--- 关键权限限制
        ReceiptStatus currentStatus = receiptStatuses[_receiptId];
        
        require(
            currentStatus == ReceiptStatus.NORMAL || currentStatus == ReceiptStatus.ONCHAIN_FAILED,
            "Status Error: Only NORMAL or FAILED can be cancelled"
        );

        receiptStatuses[_receiptId] = ReceiptStatus.CANCELLED;

        cancelRecords[_receiptId] = CancelInfo({
            receiptId: _receiptId,
            cancelReason: _reason,
            cancelType: _cancelType,
            cancelledBy: _operatorId,
            cancelTime: block.timestamp,
            isExist: true
        });

        emit ReceiptCancelled(_receiptId, _cancelType, _operatorId, block.timestamp);
    }

    /**
     * @dev 模拟初始化状态（建议也增加权限限制）
     */
    function initReceiptStatus(string memory _receiptId, uint8 _status) public onlyAuthorized {
        receiptStatuses[_receiptId] = ReceiptStatus(_status);
    }

    /**
     * @dev 查询作废详情
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