// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

/**
 * @title Receivable
 * @dev 应收账款管理智能合约 - 双层存储架构优化版
 */
contract Receivable {
    // 应收账款状态
    enum ReceivableStatus {
        Created,      // 已创建
        Confirmed,    // 已确认（核心企业确认）
        Financed,     // 已融资
        Repaid,       // 已还款
        Defaulted,    // 已违约
        Cancelled     // 已取消
    }

    // 核心资产数据（链上）
    struct ReceivableCore {
        address supplier;              // 供应商地址
        address coreEnterprise;        // 核心企业地址
        address currentHolder;         // 当前持有人
        address financier;             // 资金方（融资机构）
        uint256 amount;                // 金额（分）
        uint256 issueDate;             // 出票日期
        uint256 dueDate;               // 到期日期
        ReceivableStatus status;       // 状态
        uint256 financeAmount;         // 融资金额
        uint256 financeRate;          // 融资利率（基点）
        uint256 financeDate;          // 融资日期
        bool exists;                  // 是否存在
    }

    // 元数据（链下数据指纹）
    struct ReceivableMeta {
        string receivableId;          // 应收账款ID
        string currency;              // 币种符号
        bytes32 dataHash;             // 链下详细数据的哈希值
        uint256 createdAt;            // 创建时间
        uint256 updatedAt;            // 更新时间
    }

    // 转让记录
    struct TransferRecord {
        string receivableId;
        address from;
        address to;
        uint256 amount;
        uint256 timestamp;
        string transferType;
    }

    // 状态变量
    address public admin;
    uint256 public receivableCount;

    // 双层存储映射
    mapping(string => ReceivableCore) public receivableCores;
    mapping(string => ReceivableMeta) public receivableMetas;

    // 辅助映射
    mapping(address => string[]) public supplierReceivables;
    mapping(address => string[]) public coreEnterpriseReceivables;
    mapping(address => string[]) public financierReceivables;
    mapping(string => TransferRecord[]) public transferHistory;

    // 事件
    event ReceivableCreated(
        string indexed receivableId,
        address indexed supplier,
        address indexed coreEnterprise,
        uint256 amount,
        bytes32 dataHash
    );

    event ReceivableConfirmed(
        string indexed receivableId,
        address indexed coreEnterprise
    );

    event ReceivableFinanced(
        string indexed receivableId,
        address indexed financier,
        uint256 financeAmount,
        uint256 financeRate
    );

    event ReceivableTransferred(
        string indexed receivableId,
        address indexed from,
        address indexed to,
        uint256 amount
    );

    event ReceivableRepaid(
        string indexed receivableId,
        uint256 amount,
        uint256 timestamp
    );

    event ReceivableDefaulted(
        string indexed receivableId,
        uint256 amount
    );

    // 修饰器
    modifier onlyAdmin() {
        require(msg.sender == admin, "Only admin can call this function");
        _;
    }

    modifier onlyExistingReceivable(string memory receivableId) {
        require(receivableCores[receivableId].exists, "Receivable does not exist");
        _;
    }

    modifier onlySupplier(string memory receivableId) {
        require(
            receivableCores[receivableId].supplier == msg.sender,
            "Only supplier can call this function"
        );
        _;
    }

    modifier onlyCoreEnterprise(string memory receivableId) {
        require(
            receivableCores[receivableId].coreEnterprise == msg.sender,
            "Only core enterprise can call this function"
        );
        _;
    }

    modifier onlyCurrentHolder(string memory receivableId) {
        require(
            receivableCores[receivableId].currentHolder == msg.sender,
            "Only current holder can call this function"
        );
        _;
    }

    /**
     * @dev 构造函数
     */
    constructor() {
        admin = msg.sender;
        receivableCount = 0;
    }

    /**
     * @dev 创建应收账款（精简参数版）
     */
    function createReceivable(
        string calldata receivableId,
        address coreEnterprise,
        uint256 amount,
        uint256 issueDate,
        uint256 dueDate,
        bytes32 dataHash
    ) external returns (bool) {
        require(bytes(receivableId).length > 0, "Receivable ID cannot be empty");
        require(!receivableCores[receivableId].exists, "Receivable already exists");
        require(coreEnterprise != msg.sender, "Cannot create receivable for yourself");
        require(amount > 0, "Amount must be greater than 0");
        require(dueDate > issueDate, "Due date must be after issue date");

        // 使用作用域隔离减少栈深度
        {
            ReceivableCore storage core = receivableCores[receivableId];
            core.supplier = msg.sender;
            core.coreEnterprise = coreEnterprise;
            core.currentHolder = msg.sender;
            core.financier = address(0);
            core.amount = amount;
            core.issueDate = issueDate;
            core.dueDate = dueDate;
            core.status = ReceivableStatus.Created;
            core.financeAmount = 0;
            core.financeRate = 0;
            core.financeDate = 0;
            core.exists = true;
        }

        {
            ReceivableMeta storage meta = receivableMetas[receivableId];
            meta.receivableId = receivableId;
            meta.currency = "CNY";
            meta.dataHash = dataHash;
            meta.createdAt = block.timestamp;
            meta.updatedAt = block.timestamp;
        }

        supplierReceivables[msg.sender].push(receivableId);
        coreEnterpriseReceivables[coreEnterprise].push(receivableId);
        receivableCount++;

        emit ReceivableCreated(receivableId, msg.sender, coreEnterprise, amount, dataHash);
        return true;
    }

    /**
     * @dev 核心企业确认应收账款
     */
    function confirmReceivable(string memory receivableId)
        public
        onlyExistingReceivable(receivableId)
        onlyCoreEnterprise(receivableId)
        returns (bool)
    {
        ReceivableCore storage core = receivableCores[receivableId];

        require(core.status == ReceivableStatus.Created, "Invalid status for confirmation");

        core.status = ReceivableStatus.Confirmed;
        receivableMetas[receivableId].updatedAt = block.timestamp;

        emit ReceivableConfirmed(receivableId, msg.sender);
        return true;
    }

    /**
     * @dev 融资（供应商转让应收账款给资金方）
     */
    function financeReceivable(
        string memory receivableId,
        address financier,
        uint256 financeAmount,
        uint256 financeRate
    ) public onlyExistingReceivable(receivableId) returns (bool) {
        ReceivableCore storage core = receivableCores[receivableId];

        require(core.status == ReceivableStatus.Confirmed, "Receivable not confirmed");
        require(core.currentHolder == msg.sender, "Not the current holder");
        require(financeAmount > 0 && financeAmount <= core.amount, "Invalid finance amount");
        require(financeRate <= 10000, "Finance rate too high");

        core.status = ReceivableStatus.Financed;
        core.currentHolder = financier;
        core.financier = financier;
        core.financeAmount = financeAmount;
        core.financeRate = financeRate;
        core.financeDate = block.timestamp;
        receivableMetas[receivableId].updatedAt = block.timestamp;

        // 记录转让历史
        transferHistory[receivableId].push(TransferRecord({
            receivableId: receivableId,
            from: msg.sender,
            to: financier,
            amount: financeAmount,
            timestamp: block.timestamp,
            transferType: "financing"
        }));

        financierReceivables[financier].push(receivableId);

        emit ReceivableFinanced(receivableId, financier, financeAmount, financeRate);
        emit ReceivableTransferred(receivableId, msg.sender, financier, financeAmount);
        return true;
    }

    /**
     * @dev 转让应收账款
     */
    function transferReceivable(
        string memory receivableId,
        address newHolder
    ) public onlyExistingReceivable(receivableId) returns (bool) {
        ReceivableCore storage core = receivableCores[receivableId];

        require(core.currentHolder == msg.sender, "Not the current holder");
        require(newHolder != address(0), "Invalid new holder");
        require(newHolder != msg.sender, "Cannot transfer to yourself");

        address oldHolder = core.currentHolder;
        core.currentHolder = newHolder;
        receivableMetas[receivableId].updatedAt = block.timestamp;

        // 记录转让历史
        transferHistory[receivableId].push(TransferRecord({
            receivableId: receivableId,
            from: oldHolder,
            to: newHolder,
            amount: core.amount,
            timestamp: block.timestamp,
            transferType: "transfer"
        }));

        if (core.status == ReceivableStatus.Financed) {
            financierReceivables[newHolder].push(receivableId);
        }

        emit ReceivableTransferred(receivableId, oldHolder, newHolder, core.amount);
        return true;
    }

    /**
     * @dev 还款
     */
    function repayReceivable(string memory receivableId, uint256 amount)
        public
        onlyExistingReceivable(receivableId)
        returns (bool)
    {
        ReceivableCore storage core = receivableCores[receivableId];

        require(
            msg.sender == core.coreEnterprise || msg.sender == core.financier,
            "Not authorized to repay"
        );
        require(core.status == ReceivableStatus.Financed, "Not in financed status");
        require(amount > 0, "Amount must be greater than 0");

        core.status = ReceivableStatus.Repaid;
        receivableMetas[receivableId].updatedAt = block.timestamp;

        // 记录还款历史
        transferHistory[receivableId].push(TransferRecord({
            receivableId: receivableId,
            from: msg.sender,
            to: core.financier,
            amount: amount,
            timestamp: block.timestamp,
            transferType: "repayment"
        }));

        emit ReceivableRepaid(receivableId, amount, block.timestamp);
        return true;
    }

    /**
     * @dev 标记违约
     */
    function markAsDefaulted(string memory receivableId)
        public
        onlyAdmin
        onlyExistingReceivable(receivableId)
        returns (bool)
    {
        ReceivableCore storage core = receivableCores[receivableId];

        require(core.status == ReceivableStatus.Financed, "Not in financed status");
        require(block.timestamp > core.dueDate, "Not yet due");

        core.status = ReceivableStatus.Defaulted;
        receivableMetas[receivableId].updatedAt = block.timestamp;

        emit ReceivableDefaulted(receivableId, core.amount);
        return true;
    }

    /**
     * @dev 取消应收账款
     */
    function cancelReceivable(string memory receivableId)
        public
        onlyExistingReceivable(receivableId)
        onlySupplier(receivableId)
        returns (bool)
    {
        ReceivableCore storage core = receivableCores[receivableId];

        require(
            core.status == ReceivableStatus.Created,
            "Cannot cancel confirmed or financed receivable"
        );

        core.status = ReceivableStatus.Cancelled;
        receivableMetas[receivableId].updatedAt = block.timestamp;

        return true;
    }

    /**
     * @dev 获取应收账款核心数据
     */
    function getReceivableCore(string memory receivableId)
        public
        view
        onlyExistingReceivable(receivableId)
        returns (ReceivableCore memory)
    {
        return receivableCores[receivableId];
    }

    /**
     * @dev 获取应收账款元数据
     */
    function getReceivableMeta(string memory receivableId)
        public
        view
        onlyExistingReceivable(receivableId)
        returns (ReceivableMeta memory)
    {
        return receivableMetas[receivableId];
    }

    /**
     * @dev 获取供应商的应收账款列表
     */
    function getSupplierReceivables(address supplier)
        public
        view
        returns (string[] memory)
    {
        return supplierReceivables[supplier];
    }

    /**
     * @dev 获取核心企业的应付账款列表
     */
    function getCoreEnterpriseReceivables(address coreEnterprise)
        public
        view
        returns (string[] memory)
    {
        return coreEnterpriseReceivables[coreEnterprise];
    }

    /**
     * @dev 获取资金方的融资账款列表
     */
    function getFinancierReceivables(address financier)
        public
        view
        returns (string[] memory)
    {
        return financierReceivables[financier];
    }

    /**
     * @dev 获取转让历史记录数量
     */
    function getTransferHistoryCount(string memory receivableId)
        public
        view
        returns (uint256)
    {
        return transferHistory[receivableId].length;
    }

    /**
     * @dev 转移管理员权限
     */
    function transferAdmin(address newAdmin) public onlyAdmin {
        require(newAdmin != address(0), "New admin cannot be zero address");
        admin = newAdmin;
    }
}
