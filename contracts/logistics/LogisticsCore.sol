// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.0;

/**
 * @title LogisticsCore
 * @dev 物流核心合约（极简版）
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
contract LogisticsCore {

    // ==================== 状态变量 ====================

    address public admin;
    uint256 public delegateCount;
    mapping(string => bool) private voucherNoExists;
    mapping(string => uint8) private voucherStatus; // 0=不存在, 1=Pending, 2=Assigned, 3=InTransit, 4=Delivered, 5=Invalid

    // ==================== 事件定义 ====================

    event LogisticsDelegateCreated(string indexed voucherNo, address indexed operator, uint256 timestamp);
    event CarrierAssigned(string indexed voucherNo, bytes32 indexed carrierHash, address indexed operator, uint256 timestamp);
    event StatusUpdated(string indexed voucherNo, uint8 oldStatus, uint8 newStatus, address indexed operator, uint256 timestamp);

    // ==================== 修饰器 ====================

    modifier onlyAdmin() {
        require(msg.sender == admin, "Only admin");
        _;
    }

    // ==================== 构造函数 ====================

    constructor(address _initialAdmin) {
        require(_initialAdmin != address(0), "Admin cannot be zero");
        admin = _initialAdmin;
    }

    // ==================== 核心功能 ====================

    function createLogisticsDelegate(string calldata voucherNo) external onlyAdmin returns (bool success) {
        require(!voucherNoExists[voucherNo], "Voucher exists");
        voucherNoExists[voucherNo] = true;
        voucherStatus[voucherNo] = 1; // Pending
        delegateCount++;
        emit LogisticsDelegateCreated(voucherNo, msg.sender, block.timestamp);
        return true;
    }

    function assignCarrier(string calldata voucherNo, bytes32 carrierHash) external returns (bool success) {
        require(voucherNoExists[voucherNo], "Voucher not found");
        require(voucherStatus[voucherNo] == 1, "Invalid status");
        voucherStatus[voucherNo] = 2; // Assigned
        emit CarrierAssigned(voucherNo, carrierHash, msg.sender, block.timestamp);
        return true;
    }

    function getStatus(string calldata voucherNo) external view returns (uint8) {
        return voucherStatus[voucherNo];
    }

    function exists(string calldata voucherNo) external view returns (bool) {
        return voucherNoExists[voucherNo];
    }

    // ==================== 管理员功能 ====================

    function setAdmin(address newAdmin) external onlyAdmin returns (bool) {
        require(newAdmin != address(0), "Invalid address");
        admin = newAdmin;
        return true;
    }
}
