// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import "./DataPackUtils.sol";

/**
 * @title ReceivableOptimized
 * @dev 应收账款管理智能合约 - 数据分类存储优化版
 *
 * 核心原则：
 * ✅ 上链信息：谁能做（地址）+ 什么时候做（时间）+ 给多少钱（金额）
 * 🔒 哈希化信息：发票号、合同号、货物描述、付款条款
 */
contract ReceivableOptimized {
    using DataPackUtils for *;
    using DataValidator for *;

    // ==================== 枚举定义 ====================

    enum ReceivableStatus {
        Created,      // 0 - 已创建
        Confirmed,    // 1 - 已确认（核心企业确认）
        Financed,     // 2 - 已融资
        Repaid,       // 3 - 已还款
        Defaulted,    // 4 - 已违约
        Cancelled     // 5 - 已取消
    }

    // ==================== 上链数据：谁能做 + 什么时候做 + 给多少钱 ====================

    /**
     * @dev 核心上链数据
     */
    struct ReceivableCore {
        // ========== 谁能做（地址） ==========
        address supplier;            // 供应商地址（20字节）
        address coreEnterprise;      // 核心企业地址（20字节）
        address currentHolder;       // 当前持有人（20字节）
        address financier;           // 金融机构地址（20字节）

        // ========== 给多少钱（金额） ==========
        uint96 amount;              // 应收金额（12字节）
        uint96 financedAmount;      // 融资金额（12字节）
        uint96 repaidAmount;        // 已还金额（12字节）

        // ========== 什么时候做（时间） ==========
        uint64 issueDate;           // 出票日期（8字节）
        uint64 dueDate;             // 到期日期（8字节）
        uint64 confirmedAt;         // 确认时间（8字节）
        uint64 financedAt;          // 融资时间（8字节）
        uint64 repaidAt;            // 还款时间（8字节）

        // ========== 业务状态 ==========
        uint8 status;               // 状态枚举（1字节）
        bool exists;                // 是否存在（1字节）
    }

    /**
     * @dev 融资条款（上链）
     */
    struct FinanceTerms {
        uint16 rate;                // 融资利率（基点，2字节）
        uint64 duration;            // 融资期限（8字节）
        uint64 repaymentDate;       // 还款日期（8字节）
    }

    // ==================== 哈希化数据：长文本 + 个人隐私 ====================

    /**
     * @dev 隐私数据哈希指纹
     */
    struct PrivacyHashes {
        bytes32 invoiceNumberHash;      // 发票号码哈希
        bytes32 contractNumberHash;     // 合同编号哈希
        bytes32 goodsDescriptionHash;   // 货物描述哈希（长文本）
        bytes32 paymentTermsHash;       // 付款条款哈希
        bytes32 deliveryInfoHash;       // 交付信息哈希
        bytes32 bankAccountHash;        // 银行账户哈希（隐私）
    }

    // ==================== 历史记录 ====================

    struct TransferRecord {
        address from;               // 转让人（谁能做）
        address to;                 // 受让人（谁能做）
        uint96 amount;             // 金额（给多少钱）
        uint64 timestamp;          // 时间（什么时候做）
        uint8 transferType;        // 类型：0=融资, 1=转让, 2=还款
    }

    // ==================== 状态变量 ====================

    address public admin;
    uint256 public receivableCount;

    mapping(string => ReceivableCore) public receivableCores;
    mapping(string => FinanceTerms) public financeTerms;
    mapping(string => PrivacyHashes) public privacyHashes;

    mapping(address => string[]) public supplierReceivables;
    mapping(address => string[]) public coreEnterpriseReceivables;
    mapping(address => string[]) public financierReceivables;
    mapping(string => TransferRecord[]) public transferHistory;

    // ==================== 事件定义 ====================

    event ReceivableCreated(
        string indexed receivableId,
        address indexed supplier,        // 谁能做
        address indexed coreEnterprise,  // 谁能做
        uint256 amount,                  // 给多少钱
        uint64 issueDate,                // 什么时候做
        bytes32 invoiceNumberHash
    );

    event ReceivableConfirmed(
        string indexed receivableId,
        address indexed coreEnterprise,  // 谁能做
        uint64 confirmedAt               // 什么时候做
    );

    event ReceivableFinanced(
        string indexed receivableId,
        address indexed financier,       // 谁能做
        uint256 financedAmount,          // 给多少钱
        uint256 rate,                    // 多少利率
        uint64 financedAt                // 什么时候做
    );

    event ReceivableTransferred(
        string indexed receivableId,
        address indexed from,            // 谁能做
        address indexed to,              // 谁能做
        uint256 amount,                  // 给多少钱
        uint64 transferredAt             // 什么时候做
    );

    event ReceivableRepaid(
        string indexed receivableId,
        uint256 amount,                  // 给多少钱
        uint64 repaidAt                  // 什么时候做
    );

    event ReceivableDefaulted(
        string indexed receivableId,
        uint256 amount                   // 给多少钱
    );

    // ==================== 修饰器 ====================

    modifier onlyAdmin() {
        require(msg.sender == admin, "Only admin");
        _;
    }

    modifier onlyExistingReceivable(string memory receivableId) {
        require(receivableCores[receivableId].exists, "Not found");
        _;
    }

    modifier onlySupplier(string memory receivableId) {
        require(receivableCores[receivableId].supplier == msg.sender, "Not supplier");
        _;
    }

    modifier onlyCoreEnterprise(string memory receivableId) {
        require(receivableCores[receivableId].coreEnterprise == msg.sender, "Not enterprise");
        _;
    }

    modifier onlyCurrentHolder(string memory receivableId) {
        require(receivableCores[receivableId].currentHolder == msg.sender, "Not holder");
        _;
    }

    // ==================== 构造函数 ====================

    constructor() {
        admin = msg.sender;
        receivableCount = 0;
    }

    // ==================== 核心业务函数 ====================

    /**
     * @dev 创建应收账款
     *
     * 上链参数（谁能做 + 什么时候做 + 给多少钱）：
     * @param receivableId 应收账款ID
     * @param coreEnterprise 核心企业地址（谁能做）
     * @param amount 金额（给多少钱）
     * @param issueDate 出票日期（什么时候做）
     * @param dueDate 到期日期
     *
     * 哈希参数（隐私信息）：
     * @param invoiceNumberHash 发票号码哈希
     * @param contractNumberHash 合同编号哈希
     * @param goodsDescriptionHash 货物描述哈希（长文本）
     * @param paymentTermsHash 付款条款哈希
     * @param deliveryInfoHash 交付信息哈希
     * @param bankAccountHash 银行账户哈希（隐私）
     */
    function createReceivable(
        string calldata receivableId,
        address coreEnterprise,
        uint96 amount,
        uint64 issueDate,
        uint64 dueDate,
        bytes32 invoiceNumberHash,
        bytes32 contractNumberHash,
        bytes32 goodsDescriptionHash,
        bytes32 paymentTermsHash,
        bytes32 deliveryInfoHash,
        bytes32 bankAccountHash
    ) external returns (bool) {
        require(bytes(receivableId).length > 0, "Invalid ID");
        require(!receivableCores[receivableId].exists, "Exists");
        require(coreEnterprise != msg.sender, "Cannot create for self");
        require(DataValidator.validAddress(coreEnterprise), "Invalid enterprise");
        require(DataValidator.validAmount(amount), "Invalid amount");
        require(DataValidator.validTimeline(issueDate, dueDate), "Invalid dates");
        require(DataValidator.validHash(invoiceNumberHash), "Invalid invoice hash");
        require(DataValidator.validHash(contractNumberHash), "Invalid contract hash");
        require(DataValidator.validHash(goodsDescriptionHash), "Invalid goods hash");
        require(DataValidator.validHash(paymentTermsHash), "Invalid payment hash");
        require(DataValidator.validHash(deliveryInfoHash), "Invalid delivery hash");
        require(DataValidator.validHash(bankAccountHash), "Invalid bank hash");

        // 存储核心数据（上链：谁能做 + 什么时候做 + 给多少钱）
        ReceivableCore storage core = receivableCores[receivableId];
        core.supplier = msg.sender;               // 谁能做
        core.coreEnterprise = coreEnterprise;     // 谁能做
        core.currentHolder = msg.sender;          // 谁能做
        core.financier = address(0);
        core.amount = amount;                     // 给多少钱
        core.financedAmount = 0;
        core.repaidAmount = 0;
        core.issueDate = issueDate;               // 什么时候做
        core.dueDate = dueDate;                   // 什么时候做
        core.confirmedAt = 0;
        core.financedAt = 0;
        core.repaidAt = 0;
        core.status = uint8(ReceivableStatus.Created);
        core.exists = true;

        // 存储隐私哈希（不上链原文）
        PrivacyHashes storage hashes = privacyHashes[receivableId];
        hashes.invoiceNumberHash = invoiceNumberHash;
        hashes.contractNumberHash = contractNumberHash;
        hashes.goodsDescriptionHash = goodsDescriptionHash;
        hashes.paymentTermsHash = paymentTermsHash;
        hashes.deliveryInfoHash = deliveryInfoHash;
        hashes.bankAccountHash = bankAccountHash;

        supplierReceivables[msg.sender].push(receivableId);
        coreEnterpriseReceivables[coreEnterprise].push(receivableId);
        receivableCount++;

        emit ReceivableCreated(
            receivableId,
            msg.sender,          // 谁能做
            coreEnterprise,      // 谁能做
            amount,              // 给多少钱
            issueDate,           // 什么时候做
            invoiceNumberHash
        );
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
        require(core.status == uint8(ReceivableStatus.Created), "Invalid status");

        core.status = uint8(ReceivableStatus.Confirmed);
        core.confirmedAt = uint64(block.timestamp);  // 什么时候做

        emit ReceivableConfirmed(receivableId, msg.sender, core.confirmedAt);
        return true;
    }

    /**
     * @dev 融资
     */
    function financeReceivable(
        string memory receivableId,
        address financier,
        uint96 financedAmount,
        uint16 rate,
        uint64 duration
    ) public onlyExistingReceivable(receivableId) returns (bool) {
        ReceivableCore storage core = receivableCores[receivableId];

        require(core.status == uint8(ReceivableStatus.Confirmed), "Not confirmed");
        require(core.currentHolder == msg.sender, "Not holder");
        require(DataValidator.validAddress(financier), "Invalid financier");
        require(DataValidator.validAmount(financedAmount), "Invalid amount");
        require(financedAmount <= core.amount, "Exceeds amount");
        require(DataValidator.validRate(rate), "Invalid rate");

        core.status = uint8(ReceivableStatus.Financed);
        core.currentHolder = financier;              // 谁能做
        core.financier = financier;
        core.financedAmount = financedAmount;        // 给多少钱
        core.financedAt = uint64(block.timestamp);   // 什么时候做

        // 存储融资条款（上链）
        FinanceTerms storage terms = financeTerms[receivableId];
        terms.rate = rate;                           // 多少利率
        terms.duration = duration;
        terms.repaymentDate = core.financedAt + duration;

        // 记录历史
        transferHistory[receivableId].push(TransferRecord({
            from: msg.sender,                        // 谁能做
            to: financier,                           // 谁能做
            amount: financedAmount,                  // 给多少钱
            timestamp: core.financedAt,              // 什么时候做
            transferType: 0  // 融资
        }));

        financierReceivables[financier].push(receivableId);

        emit ReceivableFinanced(
            receivableId,
            financier,           // 谁能做
            financedAmount,      // 给多少钱
            rate,                // 多少利率
            core.financedAt      // 什么时候做
        );
        emit ReceivableTransferred(
            receivableId,
            msg.sender,
            financier,
            financedAmount,
            core.financedAt
        );
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

        require(core.currentHolder == msg.sender, "Not holder");
        require(DataValidator.validAddress(newHolder), "Invalid new holder");
        require(newHolder != msg.sender, "Cannot transfer to self");

        address oldHolder = core.currentHolder;
        core.currentHolder = newHolder;             // 谁能做

        // 记录历史
        transferHistory[receivableId].push(TransferRecord({
            from: oldHolder,                         // 谁能做
            to: newHolder,                          // 谁能做
            amount: core.amount,                    // 给多少钱
            timestamp: uint64(block.timestamp),     // 什么时候做
            transferType: 1  // 转让
        }));

        if (core.status == uint8(ReceivableStatus.Financed)) {
            financierReceivables[newHolder].push(receivableId);
        }

        emit ReceivableTransferred(
            receivableId,
            oldHolder,           // 谁能做
            newHolder,           // 谁能做
            core.amount,         // 给多少钱
            uint64(block.timestamp)  // 什么时候做
        );
        return true;
    }

    /**
     * @dev 还款
     */
    function repayReceivable(string memory receivableId, uint96 amount)
        public
        onlyExistingReceivable(receivableId)
        returns (bool)
    {
        ReceivableCore storage core = receivableCores[receivableId];

        require(
            msg.sender == core.coreEnterprise || msg.sender == core.financier,
            "Not authorized"
        );
        require(core.status == uint8(ReceivableStatus.Financed), "Not financed");
        require(DataValidator.validAmount(amount), "Invalid amount");

        core.status = uint8(ReceivableStatus.Repaid);
        core.repaidAmount = amount;                   // 给多少钱
        core.repaidAt = uint64(block.timestamp);      // 什么时候做

        // 记录历史
        transferHistory[receivableId].push(TransferRecord({
            from: msg.sender,                         // 谁能做
            to: core.financier,                       // 谁能做
            amount: amount,                           // 给多少钱
            timestamp: core.repaidAt,                 // 什么时候做
            transferType: 2  // 还款
        }));

        emit ReceivableRepaid(
            receivableId,
            amount,              // 给多少钱
            core.repaidAt        // 什么时候做
        );
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

        require(core.status == uint8(ReceivableStatus.Financed), "Not financed");
        require(uint64(block.timestamp) > core.dueDate, "Not due");

        core.status = uint8(ReceivableStatus.Defaulted);

        emit ReceivableDefaulted(receivableId, core.amount);  // 给多少钱
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

        require(core.status == uint8(ReceivableStatus.Created), "Cannot cancel");

        core.status = uint8(ReceivableStatus.Cancelled);

        return true;
    }

    // ==================== 查询函数 ====================

    /**
     * @dev 获取应收账款完整信息
     */
    function getReceivable(string memory receivableId)
        external
        view
        onlyExistingReceivable(receivableId)
        returns (
            // 谁能做
            address supplier,
            address coreEnterprise,
            address currentHolder,
            address financier,
            // 给多少钱
            uint256 amount,
            uint256 financedAmount,
            uint256 repaidAmount,
            // 什么时候做
            uint256 issueDate,
            uint256 dueDate,
            uint256 confirmedAt,
            uint256 financedAt,
            uint256 repaidAt,
            // 业务状态
            uint8 status,
            // 隐私哈希
            bytes32 invoiceNumberHash,
            bytes32 contractNumberHash,
            bytes32 goodsDescriptionHash,
            bytes32 paymentTermsHash,
            bytes32 deliveryInfoHash,
            bytes32 bankAccountHash
        )
    {
        ReceivableCore memory core = receivableCores[receivableId];
        PrivacyHashes memory hashes = privacyHashes[receivableId];

        return (
            core.supplier,
            core.coreEnterprise,
            core.currentHolder,
            core.financier,
            core.amount,
            core.financedAmount,
            core.repaidAmount,
            core.issueDate,
            core.dueDate,
            core.confirmedAt,
            core.financedAt,
            core.repaidAt,
            core.status,
            hashes.invoiceNumberHash,
            hashes.contractNumberHash,
            hashes.goodsDescriptionHash,
            hashes.paymentTermsHash,
            hashes.deliveryInfoHash,
            hashes.bankAccountHash
        );
    }

    /**
     * @dev 获取融资条款
     */
    function getFinanceTerms(string memory receivableId)
        external
        view
        returns (
            uint256 rate,
            uint256 duration,
            uint256 repaymentDate
        )
    {
        FinanceTerms memory terms = financeTerms[receivableId];
        return (terms.rate, terms.duration, terms.repaymentDate);
    }

    /**
     * @dev 验证发票号码哈希
     */
    function verifyInvoiceNumber(string memory receivableId, bytes32 providedHash)
        external
        view
        onlyExistingReceivable(receivableId)
        returns (bool)
    {
        return DataValidator.verifyHash(
            privacyHashes[receivableId].invoiceNumberHash,
            providedHash
        );
    }

    /**
     * @dev 验证合同编号哈希
     */
    function verifyContractNumber(string memory receivableId, bytes32 providedHash)
        external
        view
        onlyExistingReceivable(receivableId)
        returns (bool)
    {
        return DataValidator.verifyHash(
            privacyHashes[receivableId].contractNumberHash,
            providedHash
        );
    }

    /**
     * @dev 验证货物描述哈希
     */
    function verifyGoodsDescription(string memory receivableId, bytes32 providedHash)
        external
        view
        onlyExistingReceivable(receivableId)
        returns (bool)
    {
        return DataValidator.verifyHash(
            privacyHashes[receivableId].goodsDescriptionHash,
            providedHash
        );
    }

    /**
     * @dev 验证银行账户哈希（隐私）
     */
    function verifyBankAccount(string memory receivableId, bytes32 providedHash)
        external
        view
        onlyExistingReceivable(receivableId)
        returns (bool)
    {
        return DataValidator.verifyHash(
            privacyHashes[receivableId].bankAccountHash,
            providedHash
        );
    }

    /**
     * @dev 获取转让历史数量
     */
    function getTransferHistoryCount(string memory receivableId)
        external
        view
        returns (uint256)
    {
        return transferHistory[receivableId].length;
    }

    /**
     * @dev 转移管理员权限
     */
    function transferAdmin(address newAdmin) public onlyAdmin {
        require(DataValidator.validAddress(newAdmin), "Invalid address");
        admin = newAdmin;
    }
}
