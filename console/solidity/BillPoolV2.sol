// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

/**
 * @title BillPoolV2
 * @dev 票据池管理合约 - 权限优化版
 * 职责：票据资产存证、投资逻辑校验、拆分溯源
 */
contract BillPoolV2 {
    
    // ==================== 1. 数据定义 ====================
    
    // 票据状态枚举（需与 Java 中的 BillStatus 对应）
    enum BillStatus { 
        DRAFT,              // 0: 草稿
        PENDING_ISSUANCE,    // 1: 待签发
        ISSUED,             // 2: 已签发
        NORMAL,             // 3: 正常持有
        ENDORSED,           // 4: 已背书
        PLEDGED,            // 5: 已质押
        DISCOUNTED,         // 6: 已贴现/投资
        FINANCED,           // 7: 已融资
        FROZEN,             // 8: 已冻结
        EXPIRED,            // 9: 已过期
        DISHONORED,         // 10: 已拒付
        CANCELLED,          // 11: 已作废
        PAID,               // 12: 已兑付
        SETTLED             // 13: 已清算
    }

    struct Bill {
        string billId;        // 内部 UUID
        string billNo;        // 票据编号
        uint256 faceValue;    // 票面金额
        uint256 dueDate;      // 到期日
        address currentHolder;// 当前持有人链上地址
        BillStatus status;    // 当前状态
        string parentBillId;  // 拆分溯源：父票 ID
        bool isExists;        // 存在标识
    }

    // ==================== 2. 状态变量 ====================
    
    address public admin;                              // 管理员地址
    mapping(address => bool) public authorizedCallers; // 授权白名单（Java 后端地址池）
    mapping(string => Bill) private _bills;            // 票据存储映射
    uint256 public totalBills;                        // 总票据计数

    // ==================== 3. 事件 ====================
    
    event BillInvested(string indexed billId, address indexed investor, uint256 investAmount, string txHash);
    event BillSplit(string indexed parentId, string[] subBillIds, uint256[] amounts);
    event BillCreated(string indexed billId, string billNo, uint256 amount);
    event CallerAuthorized(address indexed caller, bool status);

    // ==================== 4. 访问控制 ====================
    
    modifier onlyAdmin() {
        require(msg.sender == admin, "Auth: Only admin can perform this");
        _;
    }

    modifier onlyAuthorized() {
        require(msg.sender == admin || authorizedCallers[msg.sender], "Auth: Caller is not authorized");
        _;
    }

    // ==================== 5. 构造函数 ====================
    
    /**
     * @dev 构造函数：初始化合约管理员
     * @param _admin 管理员地址（通常由部署脚本传入 Java 后端管理的 Admin 公钥）
     */
    constructor(address _admin) {
        require(_admin != address(0), "Admin: Zero address detected");
        admin = _admin;
        authorizedCallers[_admin] = true; // 管理员默认授权
    }

    // ==================== 6. 管理接口 ====================

    /**
     * @dev 授权特定的 Java 后端节点地址进行操作
     */
    function setAuthorizedCaller(address _caller, bool _status) external onlyAdmin {
        require(_caller != address(0), "Invalid address");
        authorizedCallers[_caller] = _status;
        emit CallerAuthorized(_caller, _status);
    }

    // ==================== 7. 核心业务逻辑 ====================

    /**
     * @dev 初始化/创建票据上链
     */
    function initBill(
        string memory _billId,
        string memory _billNo,
        uint256 _amount,
        uint256 _dueDate,
        address _holder
    ) external onlyAuthorized {
        require(!_bills[_billId].isExists, "Bill: Already exists");
        
        _bills[_billId] = Bill({
            billId: _billId,
            billNo: _billNo,
            faceValue: _amount,
            dueDate: _dueDate,
            currentHolder: _holder,
            status: BillStatus.NORMAL,
            parentBillId: "",
            isExists: true
        });
        
        totalBills++;
        emit BillCreated(_billId, _billNo, _amount);
    }

    /**
     * @dev 票据投资（权利转移）
     */
    function investBill(
        string memory _billId, 
        address _investor,
        uint256 _investAmount
    ) external onlyAuthorized {
        Bill storage bill = _bills[_billId];
        
        require(bill.isExists, "Bill: Not found");
        require(bill.status == BillStatus.NORMAL, "Status: Not investable");
        require(bill.dueDate > block.timestamp, "Time: Bill expired");
        require(bill.currentHolder != _investor, "Logic: Cannot invest self-owned bill");
        
        // 业务规则校验：投资额度需在面值的 10%-100% 之间
        require(_investAmount <= bill.faceValue, "Amount: Over face value");
        require(_investAmount >= bill.faceValue / 10, "Amount: Under 10% limit");

        bill.currentHolder = _investor;
        bill.status = BillStatus.DISCOUNTED;

        emit BillInvested(_billId, _investor, _investAmount, "INTERNAL_TX");
    }

    /**
     * @dev 票据拆分（作废原票，生成子票）
     */
    function splitBill(
        string memory _parentId,
        string[] memory _subIds,
        string[] memory _subNos,
        uint256[] memory _amounts
    ) external onlyAuthorized {
        Bill storage parent = _bills[_parentId];
        
        require(parent.isExists, "Parent: Not found");
        require(parent.status == BillStatus.NORMAL, "Status: Parent not splittable");
        require(_subIds.length == _amounts.length && _subIds.length == _subNos.length, "Params: Length mismatch");

        uint256 totalAmount = 0;
        for (uint256 i = 0; i < _amounts.length; i++) {
            totalAmount += _amounts[i];
            
            _bills[_subIds[i]] = Bill({
                billId: _subIds[i],
                billNo: _subNos[i],
                faceValue: _amounts[i],
                dueDate: parent.dueDate,
                currentHolder: parent.currentHolder,
                status: BillStatus.NORMAL,
                parentBillId: _parentId,
                isExists: true
            });
            totalBills++;
        }

        require(totalAmount == parent.faceValue, "Logic: Amount sum mismatch");

        // 原票状态变更为已作废/已拆分
        parent.status = BillStatus.CANCELLED;

        emit BillSplit(_parentId, _subIds, _amounts);
    }

    // ==================== 8. 查询接口 ====================

    function getBill(string memory _id) public view returns (
        string memory billNo, 
        uint256 faceValue, 
        address holder, 
        BillStatus status,
        string memory parentId
    ) {
        Bill memory b = _bills[_id];
        require(b.isExists, "Bill: Not found");
        return (b.billNo, b.faceValue, b.currentHolder, b.status, b.parentBillId);
    }
}