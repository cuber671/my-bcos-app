// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

/**
 * @title WarehouseReceiptChangeHistoryV2
 * @dev 电子仓单变更历史溯源合约
 * 记录仓单全生命周期的属性变更，提供不可篡改的审计追踪
 */
contract WarehouseReceiptChangeHistoryV2 {

    // ==================== 1. 权限控制变量 ====================
    address public admin;
    mapping(address => bool) public authorizedCallers;

    // ==================== 2. 枚举与结构体 ====================
    enum ChangeType {
        STATUS_CHANGE, Holder_CHANGE, QUANTITY_CHANGE, VALUE_CHANGE,
        EXPIRY_CHANGE, LOCATION_CHANGE, FROZEN_CHANGE, INFO_UPDATE
    }

    struct ChangeRecord {
        string receiptId;
        ChangeType changeType;
        string oldValue;
        string newValue;
        string operatorId;
        string operatorName;
        string reason;
        uint256 timestamp;
        address caller; // 记录发起此记录的链上地址
    }

    // ==================== 3. 存储与事件 ====================
    mapping(string => ChangeRecord[]) private receiptHistories;

    event ReceiptHistoryRecorded(
        string indexed receiptId,
        ChangeType indexed changeType,
        string operatorId,
        uint256 timestamp
    );
    
    event CallerAuthorized(address indexed caller, bool status);

    // ==================== 4. 权限修饰器 ====================
    modifier onlyAdmin() {
        require(msg.sender == admin, "History: Only admin");
        _;
    }

    modifier onlyAuthorized() {
        require(msg.sender == admin || authorizedCallers[msg.sender], "History: Not authorized");
        _;
    }

    // ==================== 5. 构造函数 (完善部分) ====================
    /**
     * @dev 构造函数：初始化管理员并授权初始后端地址
     * @param _admin 管理员地址（通常是合约部署者或多签钱包）
     */
    constructor(address _admin) {
        require(_admin != address(0), "History: Admin cannot be zero address");
        admin = _admin;
        
        // 默认将 Admin 加入授权名单，方便初始调试
        authorizedCallers[_admin] = true;
    }

    // ==================== 6. 权限管理功能 ====================
    /**
     * @dev 设置授权调用者（如 Java 后端的多个服务地址）
     */
    function setAuthorizedCaller(address _caller, bool _status) external onlyAdmin {
        require(_caller != address(0), "History: Invalid caller address");
        authorizedCallers[_caller] = _status;
        emit CallerAuthorized(_caller, _status);
    }

    // ==================== 7. 核心业务功能 ====================
    /**
     * @dev 记录一次变更操作
     * 权限改为 onlyAuthorized：只有白名单地址能写入
     */
    function recordChange(
        string memory _receiptId,
        uint8 _typeIndex,
        string memory _oldValue,
        string memory _newValue,
        string memory _operatorId,
        string memory _operatorName,
        string memory _reason
    ) public onlyAuthorized {
        require(_typeIndex <= uint8(ChangeType.INFO_UPDATE), "History: Invalid ChangeType");

        receiptHistories[_receiptId].push(ChangeRecord({
            receiptId: _receiptId,
            changeType: ChangeType(_typeIndex),
            oldValue: _oldValue,
            newValue: _newValue,
            operatorId: _operatorId,
            operatorName: _operatorName,
            reason: _reason,
            timestamp: block.timestamp,
            caller: msg.sender // 存证当前调用的后端地址
        }));

        emit ReceiptHistoryRecorded(_receiptId, ChangeType(_typeIndex), _operatorId, block.timestamp);
    }

    // ==================== 8. 查询功能 ====================
    function getHistory(string memory _receiptId) public view returns (ChangeRecord[] memory) {
        return receiptHistories[_receiptId];
    }

    function getHistoryCount(string memory _receiptId) public view returns (uint256) {
        return receiptHistories[_receiptId].length;
    }

    function getHistoryByIndex(string memory _receiptId, uint256 _index) public view returns (ChangeRecord memory) {
        require(_index < receiptHistories[_receiptId].length, "History: Index out of bounds");
        return receiptHistories[_receiptId][_index];
    }
}