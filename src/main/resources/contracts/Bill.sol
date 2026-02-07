// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

/**
 * @title Bill
 * @dev 票据/信用证管理智能合约 - 双层存储架构优化版
 */
contract Bill {
    // 票据类型
    enum BillType {
        CommercialBill,     // 商业汇票
        BankBill,          // 银行汇票
        LetterOfCredit     // 信用证
    }

    // 票据状态
    enum BillStatus {
        Issued,           // 已开票
        Endorsed,         // 已背书
        Discounted,       // 已贴现
        Accepted,         // 已承兑
        Paid,             // 已付款
        Dishonored,       // 已拒付
        Cancelled         // 已取消
    }

    // 核心资产数据（链上）
    struct BillCore {
        address issuer;           // 出票人
        address acceptor;         // 承兑人
        address currentHolder;    // 当前持票人
        uint256 amount;           // 票面金额（分）
        uint256 issueDate;        // 出票日期
        uint256 dueDate;          // 到期日期
        uint256 paymentDate;      // 付款日期
        BillStatus status;        // 状态
        uint256 endorsementCount; // 背书次数
        bool exists;              // 是否存在
    }

    // 元数据（链下数据指纹）
    struct BillMeta {
        string billId;          // 票据ID
        BillType billType;      // 票据类型
        address beneficiary;    // 受益人
        string currency;        // 币种符号
        bytes32 dataHash;       // 链下详细数据的哈希值
        uint256 createdAt;      // 创建时间
        uint256 updatedAt;      // 更新时间
    }

    // 背书记录
    struct Endorsement {
        string billId;
        address endorser;
        address endorsee;
        uint256 timestamp;
        string endorsementType;
    }

    // 状态变量
    address public admin;
    uint256 public billCount;

    // 双层存储映射
    mapping(string => BillCore) public billCores;
    mapping(string => BillMeta) public billMetas;

    // 辅助映射
    mapping(address => string[]) public issuedBills;
    mapping(address => string[]) public holdingBills;
    mapping(address => string[]) public acceptedBills;
    mapping(string => Endorsement[]) public endorsementHistory;

    // 事件
    event BillIssued(
        string indexed billId,
        address indexed issuer,
        address indexed beneficiary,
        uint256 amount,
        BillType billType,
        bytes32 dataHash
    );

    event BillAccepted(
        string indexed billId,
        address indexed acceptor
    );

    event BillEndorsed(
        string indexed billId,
        address indexed endorser,
        address indexed endorsee
    );

    event BillDiscounted(
        string indexed billId,
        address indexed holder,
        address indexed financialInstitution,
        uint256 discountAmount,
        uint256 discountRate
    );

    event BillPaid(
        string indexed billId,
        uint256 amount,
        uint256 paymentDate
    );

    event BillDishonored(
        string indexed billId,
        string reason
    );

    // 修饰器
    modifier onlyAdmin() {
        require(msg.sender == admin, "Only admin can call this function");
        _;
    }

    modifier onlyExistingBill(string memory billId) {
        require(billCores[billId].exists, "Bill does not exist");
        _;
    }

    modifier onlyCurrentHolder(string memory billId) {
        require(
            billCores[billId].currentHolder == msg.sender,
            "Only current holder can call this function"
        );
        _;
    }

    modifier onlyAcceptor(string memory billId) {
        require(
            billCores[billId].acceptor == msg.sender,
            "Only acceptor can call this function"
        );
        _;
    }

    /**
     * @dev 构造函数
     */
    constructor() {
        admin = msg.sender;
        billCount = 0;
    }

    /**
     * @dev 开票（精简参数版）
     */
    function issueBill(
        string calldata billId,
        BillType billType,
        address acceptor,
        address beneficiary,
        uint256 amount,
        uint256 issueDate,
        uint256 dueDate,
        bytes32 dataHash
    ) external returns (bool) {
        require(bytes(billId).length > 0, "Bill ID cannot be empty");
        require(!billCores[billId].exists, "Bill already exists");
        require(acceptor != address(0), "Invalid acceptor");
        require(beneficiary != address(0), "Invalid beneficiary");
        require(amount > 0, "Amount must be greater than 0");
        require(dueDate > issueDate, "Due date must be after issue date");

        // 使用作用域隔离减少栈深度
        {
            BillCore storage core = billCores[billId];
            core.issuer = msg.sender;
            core.acceptor = acceptor;
            core.currentHolder = beneficiary;
            core.amount = amount;
            core.issueDate = issueDate;
            core.dueDate = dueDate;
            core.paymentDate = 0;
            core.status = BillStatus.Issued;
            core.endorsementCount = 0;
            core.exists = true;
        }

        {
            BillMeta storage meta = billMetas[billId];
            meta.billId = billId;
            meta.billType = billType;
            meta.beneficiary = beneficiary;
            meta.currency = "CNY";
            meta.dataHash = dataHash;
            meta.createdAt = block.timestamp;
            meta.updatedAt = block.timestamp;
        }

        issuedBills[msg.sender].push(billId);
        holdingBills[beneficiary].push(billId);
        billCount++;

        emit BillIssued(billId, msg.sender, beneficiary, amount, billType, dataHash);
        return true;
    }

    /**
     * @dev 承兑
     */
    function acceptBill(string memory billId)
        public
        onlyExistingBill(billId)
        onlyAcceptor(billId)
        returns (bool)
    {
        BillCore storage core = billCores[billId];

        require(core.status == BillStatus.Issued, "Invalid status for acceptance");

        core.status = BillStatus.Accepted;
        billMetas[billId].updatedAt = block.timestamp;

        acceptedBills[msg.sender].push(billId);

        emit BillAccepted(billId, msg.sender);
        return true;
    }

    /**
     * @dev 背书转让
     */
    function endorseBill(
        string memory billId,
        address newHolder,
        string memory endorsementType
    ) public onlyExistingBill(billId) returns (bool) {
        BillCore storage core = billCores[billId];

        require(core.currentHolder == msg.sender, "Not the current holder");
        require(newHolder != address(0), "Invalid new holder");
        require(newHolder != msg.sender, "Cannot endorse to yourself");
        require(
            core.status == BillStatus.Issued ||
            core.status == BillStatus.Accepted ||
            core.status == BillStatus.Endorsed,
            "Invalid status for endorsement"
        );

        address oldHolder = core.currentHolder;
        core.currentHolder = newHolder;
        core.status = BillStatus.Endorsed;
        core.endorsementCount++;
        billMetas[billId].updatedAt = block.timestamp;

        // 记录背书历史
        endorsementHistory[billId].push(Endorsement({
            billId: billId,
            endorser: msg.sender,
            endorsee: newHolder,
            timestamp: block.timestamp,
            endorsementType: endorsementType
        }));

        holdingBills[newHolder].push(billId);

        emit BillEndorsed(billId, msg.sender, newHolder);
        return true;
    }

    /**
     * @dev 贴现
     */
    function discountBill(
        string memory billId,
        address financialInstitution,
        uint256 discountAmount,
        uint256 discountRate
    ) public onlyExistingBill(billId) returns (bool) {
        BillCore storage core = billCores[billId];

        require(core.currentHolder == msg.sender, "Not the current holder");
        require(
            core.status == BillStatus.Accepted || core.status == BillStatus.Endorsed,
            "Invalid status for discounting"
        );
        require(discountAmount > 0 && discountAmount <= core.amount, "Invalid discount amount");
        require(discountRate <= 10000, "Discount rate too high");

        address oldHolder = core.currentHolder;
        core.currentHolder = financialInstitution;
        core.status = BillStatus.Discounted;
        billMetas[billId].updatedAt = block.timestamp;

        // 记录贴现为背书
        endorsementHistory[billId].push(Endorsement({
            billId: billId,
            endorser: msg.sender,
            endorsee: financialInstitution,
            timestamp: block.timestamp,
            endorsementType: "discount"
        }));

        holdingBills[financialInstitution].push(billId);

        emit BillDiscounted(billId, msg.sender, financialInstitution, discountAmount, discountRate);
        return true;
    }

    /**
     * @dev 付款
     */
    function payBill(string memory billId)
        public
        onlyExistingBill(billId)
        onlyAcceptor(billId)
        returns (bool)
    {
        BillCore storage core = billCores[billId];

        require(
            core.status == BillStatus.Accepted ||
            core.status == BillStatus.Endorsed ||
            core.status == BillStatus.Discounted,
            "Invalid status for payment"
        );

        core.status = BillStatus.Paid;
        core.paymentDate = block.timestamp;
        billMetas[billId].updatedAt = block.timestamp;

        emit BillPaid(billId, core.amount, block.timestamp);
        return true;
    }

    /**
     * @dev 拒付
     */
    function dishonorBill(string memory billId, string memory reason)
        public
        onlyExistingBill(billId)
        onlyAcceptor(billId)
        returns (bool)
    {
        BillCore storage core = billCores[billId];

        require(core.status != BillStatus.Paid, "Bill already paid");
        require(core.status != BillStatus.Dishonored, "Bill already dishonored");

        core.status = BillStatus.Dishonored;
        billMetas[billId].updatedAt = block.timestamp;

        emit BillDishonored(billId, reason);
        return true;
    }

    /**
     * @dev 取消票据
     */
    function cancelBill(string memory billId)
        public
        onlyExistingBill(billId)
        returns (bool)
    {
        BillCore storage core = billCores[billId];

        require(
            msg.sender == core.issuer || msg.sender == core.currentHolder,
            "Not authorized to cancel"
        );
        require(core.status == BillStatus.Issued, "Can only cancel issued bills");

        core.status = BillStatus.Cancelled;
        billMetas[billId].updatedAt = block.timestamp;

        return true;
    }

    /**
     * @dev 获取票据核心数据
     */
    function getBillCore(string memory billId)
        public
        view
        onlyExistingBill(billId)
        returns (BillCore memory)
    {
        return billCores[billId];
    }

    /**
     * @dev 获取票据元数据
     */
    function getBillMeta(string memory billId)
        public
        view
        onlyExistingBill(billId)
        returns (BillMeta memory)
    {
        return billMetas[billId];
    }

    /**
     * @dev 获取持票人的票据列表
     */
    function getHoldingBills(address holder) public view returns (string[] memory) {
        return holdingBills[holder];
    }

    /**
     * @dev 获取已承兑票据列表
     */
    function getAcceptedBills(address acceptor) public view returns (string[] memory) {
        return acceptedBills[acceptor];
    }

    /**
     * @dev 获取背书历史记录数量
     */
    function getEndorsementHistoryCount(string memory billId) public view returns (uint256) {
        return endorsementHistory[billId].length;
    }

    /**
     * @dev 转移管理员权限
     */
    function transferAdmin(address newAdmin) public onlyAdmin {
        require(newAdmin != address(0), "New admin cannot be zero address");
        admin = newAdmin;
    }
}
