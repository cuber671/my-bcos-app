// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import "./DataPackUtils.sol";

/**
 * @title BillOptimized
 * @dev 票据/信用证管理智能合约 - 数据分类存储优化版
 *
 * 核心原则：
 * ✅ 上链信息：谁能做（地址）+ 什么时候做（时间）+ 给多少钱（金额）
 * 🔒 哈希化信息：票据号码、当事人姓名、交易描述、合同条款
 */
contract BillOptimized {
    using DataPackUtils for *;
    using DataValidator for *;

    // ==================== 枚举定义 ====================

    enum BillType {
        CommercialBill,     // 0 - 商业汇票
        BankBill,          // 1 - 银行汇票
        LetterOfCredit     // 2 - 信用证
    }

    enum BillStatus {
        Issued,           // 0 - 已开票
        Accepted,         // 1 - 已承兑
        Endorsed,         // 2 - 已背书
        Discounted,       // 3 - 已贴现
        Paid,             // 4 - 已付款
        Dishonored,       // 5 - 已拒付
        Cancelled         // 6 - 已取消
    }

    // ==================== 上链数据：谁能做 + 什么时候做 + 给多少钱 ====================

    /**
     * @dev 核心上链数据
     */
    struct BillCore {
        // ========== 谁能做（地址） ==========
        address issuer;              // 出票人（20字节）
        address acceptor;            // 承兑人（20字节）
        address currentHolder;       // 当前持票人（20字节）
        address beneficiary;         // 受益人（20字节）

        // ========== 给多少钱（金额） ==========
        uint96 amount;              // 票面金额（12字节）
        uint96 discountedAmount;    // 贴现金额（12字节）
        uint96 paidAmount;          // 已付金额（12字节）

        // ========== 什么时候做（时间） ==========
        uint64 issueDate;           // 出票日期（8字节）
        uint64 dueDate;             // 到期日期（8字节）
        uint64 acceptanceDate;      // 承兑日期（8字节）
        uint64 paymentDate;         // 付款日期（8字节）

        // ========== 业务状态 ==========
        uint8 billType;             // 票据类型（1字节）
        uint8 status;               // 状态枚举（1字节）
        uint16 endorsementCount;    // 背书次数（2字节）
        bool exists;                // 是否存在（1字节）
    }

    /**
     * @dev 贴现条款（上链）
     */
    struct DiscountTerms {
        uint16 rate;                // 贴现率（基点）
        uint64 discountDate;        // 贴现日期
        uint96 discountAmount;      // 贴现金额
    }

    // ==================== 哈希化数据：长文本 + 个人隐私 ====================

    /**
     * @dev 隐私数据哈希指纹
     */
    struct PrivacyHashes {
        bytes32 billNumberHash;         // 票据号码哈希
        bytes32 issuerInfoHash;         // 出票人信息哈希（名称、税号等）
        bytes32 beneficiaryInfoHash;    // 受益人信息哈希
        bytes32 transactionDescHash;    // 交易描述哈希（长文本）
        bytes32 contractTermsHash;      // 合同条款哈希
    }

    // ==================== 历史记录 ====================

    struct EndorsementRecord {
        address endorser;           // 背书人（谁能做）
        address endorsee;           // 被背书人
        uint96 amount;             // 金额（给多少钱）
        uint64 timestamp;          // 时间（什么时候做）
        uint8 endorsementType;     // 类型：0=转让, 1=贴现, 2=质押
    }

    // ==================== 状态变量 ====================

    address public admin;
    uint256 public billCount;

    mapping(string => BillCore) public billCores;
    mapping(string => DiscountTerms) public discountTerms;
    mapping(string => PrivacyHashes) public privacyHashes;

    mapping(address => string[]) public issuedBills;
    mapping(address => string[]) public holdingBills;
    mapping(address => string[]) public acceptedBills;
    mapping(string => EndorsementRecord[]) public endorsementHistory;

    // ==================== 事件定义 ====================

    event BillIssued(
        string indexed billId,
        address indexed issuer,          // 谁能做
        address indexed beneficiary,     // 谁能做
        uint256 amount,                  // 给多少钱
        uint64 issueDate,                // 什么时候做
        uint8 billType,
        bytes32 billNumberHash
    );

    event BillAccepted(
        string indexed billId,
        address indexed acceptor,        // 谁能做
        uint64 acceptanceDate            // 什么时候做
    );

    event BillEndorsed(
        string indexed billId,
        address indexed endorser,        // 谁能做
        address indexed endorsee,        // 谁能做
        uint256 amount,                  // 给多少钱
        uint64 endorsedAt                // 什么时候做
    );

    event BillDiscounted(
        string indexed billId,
        address indexed holder,          // 谁能做
        address indexed financialInstitution,  // 谁能做
        uint256 discountAmount,          // 给多少钱
        uint256 rate,                    // 多少利率
        uint64 discountedAt              // 什么时候做
    );

    event BillPaid(
        string indexed billId,
        uint256 amount,                  // 给多少钱
        uint64 paymentDate               // 什么时候做
    );

    event BillDishonored(
        string indexed billId,
        string reason
    );

    // ==================== 修饰器 ====================

    modifier onlyAdmin() {
        require(msg.sender == admin, "Only admin");
        _;
    }

    modifier onlyExistingBill(string memory billId) {
        require(billCores[billId].exists, "Bill not found");
        _;
    }

    modifier onlyCurrentHolder(string memory billId) {
        require(billCores[billId].currentHolder == msg.sender, "Not holder");
        _;
    }

    modifier onlyAcceptor(string memory billId) {
        require(billCores[billId].acceptor == msg.sender, "Not acceptor");
        _;
    }

    modifier onlyIssuer(string memory billId) {
        require(billCores[billId].issuer == msg.sender, "Not issuer");
        _;
    }

    // ==================== 构造函数 ====================

    constructor() {
        admin = msg.sender;
        billCount = 0;
    }

    // ==================== 核心业务函数 ====================

    /**
     * @dev 开票
     *
     * 上链参数（谁能做 + 什么时候做 + 给多少钱）：
     * @param billId 票据ID
     * @param billType 票据类型
     * @param acceptor 承兑人地址（谁能做）
     * @param beneficiary 受益人地址（谁能做）
     * @param amount 票面金额（给多少钱）
     * @param issueDate 出票日期（什么时候做）
     * @param dueDate 到期日期
     *
     * 哈希参数（隐私信息）：
     * @param billNumberHash 票据号码哈希
     * @param issuerInfoHash 出票人信息哈希（名称、税号、地址等）
     * @param beneficiaryInfoHash 受益人信息哈希
     * @param transactionDescHash 交易描述哈希（长文本）
     * @param contractTermsHash 合同条款哈希
     */
    function issueBill(
        string calldata billId,
        uint8 billType,
        address acceptor,
        address beneficiary,
        uint96 amount,
        uint64 issueDate,
        uint64 dueDate,
        bytes32 billNumberHash,
        bytes32 issuerInfoHash,
        bytes32 beneficiaryInfoHash,
        bytes32 transactionDescHash,
        bytes32 contractTermsHash
    ) external returns (bool) {
        require(bytes(billId).length > 0, "Invalid ID");
        require(!billCores[billId].exists, "Exists");
        require(DataValidator.validAddress(acceptor), "Invalid acceptor");
        require(DataValidator.validAddress(beneficiary), "Invalid beneficiary");
        require(DataValidator.validAmount(amount), "Invalid amount");
        require(DataValidator.validTimeline(issueDate, dueDate), "Invalid dates");
        require(billType <= uint8(BillType.LetterOfCredit), "Invalid type");
        require(DataValidator.validHash(billNumberHash), "Invalid bill number hash");
        require(DataValidator.validHash(issuerInfoHash), "Invalid issuer info hash");
        require(DataValidator.validHash(beneficiaryInfoHash), "Invalid beneficiary info hash");
        require(DataValidator.validHash(transactionDescHash), "Invalid desc hash");
        require(DataValidator.validHash(contractTermsHash), "Invalid contract hash");

        // 存储核心数据（上链：谁能做 + 什么时候做 + 给多少钱）
        BillCore storage core = billCores[billId];
        core.issuer = msg.sender;               // 谁能做
        core.acceptor = acceptor;               // 谁能做
        core.currentHolder = beneficiary;       // 谁能做
        core.beneficiary = beneficiary;         // 谁能做
        core.amount = amount;                   // 给多少钱
        core.discountedAmount = 0;
        core.paidAmount = 0;
        core.issueDate = issueDate;             // 什么时候做
        core.dueDate = dueDate;                 // 什么时候做
        core.acceptanceDate = 0;
        core.paymentDate = 0;
        core.billType = billType;
        core.status = uint8(BillStatus.Issued);
        core.endorsementCount = 0;
        core.exists = true;

        // 存储隐私哈希（不上链原文）
        PrivacyHashes storage hashes = privacyHashes[billId];
        hashes.billNumberHash = billNumberHash;
        hashes.issuerInfoHash = issuerInfoHash;
        hashes.beneficiaryInfoHash = beneficiaryInfoHash;
        hashes.transactionDescHash = transactionDescHash;
        hashes.contractTermsHash = contractTermsHash;

        issuedBills[msg.sender].push(billId);
        holdingBills[beneficiary].push(billId);
        billCount++;

        emit BillIssued(
            billId,
            msg.sender,          // 谁能做
            beneficiary,         // 谁能做
            amount,              // 给多少钱
            issueDate,           // 什么时候做
            billType,
            billNumberHash
        );
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
        require(core.status == uint8(BillStatus.Issued), "Invalid status");

        core.status = uint8(BillStatus.Accepted);
        core.acceptanceDate = uint64(block.timestamp);  // 什么时候做

        acceptedBills[msg.sender].push(billId);

        emit BillAccepted(billId, msg.sender, core.acceptanceDate);
        return true;
    }

    /**
     * @dev 背书转让
     */
    function endorseBill(
        string memory billId,
        address newHolder,
        uint96 amount
    ) public onlyExistingBill(billId) returns (bool) {
        BillCore storage core = billCores[billId];
        require(core.currentHolder == msg.sender, "Not holder");
        require(DataValidator.validAddress(newHolder), "Invalid new holder");
        require(newHolder != msg.sender, "Cannot endorse to self");

        uint8 status = core.status;
        require(
            status == uint8(BillStatus.Issued) ||
            status == uint8(BillStatus.Accepted) ||
            status == uint8(BillStatus.Endorsed),
            "Invalid status"
        );

        address oldHolder = core.currentHolder;
        core.currentHolder = newHolder;          // 谁能做
        core.status = uint8(BillStatus.Endorsed);
        core.endorsementCount++;

        // 记录历史
        endorsementHistory[billId].push(EndorsementRecord({
            endorser: msg.sender,               // 谁能做
            endorsee: newHolder,                // 谁能做
            amount: amount,                     // 给多少钱
            timestamp: uint64(block.timestamp), // 什么时候做
            endorsementType: 0  // 转让
        }));

        holdingBills[newHolder].push(billId);

        emit BillEndorsed(
            billId,
            msg.sender,          // 谁能做
            newHolder,           // 谁能做
            amount,              // 给多少钱
            uint64(block.timestamp)  // 什么时候做
        );
        return true;
    }

    /**
     * @dev 贴现
     */
    function discountBill(
        string memory billId,
        address financialInstitution,
        uint96 discountAmount,
        uint16 rate
    ) public onlyExistingBill(billId) returns (bool) {
        BillCore storage core = billCores[billId];
        require(core.currentHolder == msg.sender, "Not holder");

        uint8 status = core.status;
        require(
            status == uint8(BillStatus.Accepted) || status == uint8(BillStatus.Endorsed),
            "Invalid status"
        );
        require(DataValidator.validAddress(financialInstitution), "Invalid institution");
        require(DataValidator.validAmount(discountAmount), "Invalid amount");
        require(discountAmount <= core.amount, "Exceeds amount");
        require(DataValidator.validRate(rate), "Invalid rate");

        address oldHolder = core.currentHolder;
        core.currentHolder = financialInstitution;  // 谁能做
        core.status = uint8(BillStatus.Discounted);
        core.discountedAmount = discountAmount;      // 给多少钱

        // 存储贴现条款（上链）
        DiscountTerms storage terms = discountTerms[billId];
        terms.rate = rate;                           // 多少利率
        terms.discountDate = uint64(block.timestamp);
        terms.discountAmount = discountAmount;

        // 记录背书历史
        endorsementHistory[billId].push(EndorsementRecord({
            endorser: msg.sender,
            endorsee: financialInstitution,
            amount: discountAmount,
            timestamp: terms.discountDate,
            endorsementType: 1  // 贴现
        }));

        holdingBills[financialInstitution].push(billId);

        emit BillDiscounted(
            billId,
            msg.sender,              // 谁能做
            financialInstitution,    // 谁能做
            discountAmount,          // 给多少钱
            rate,                    // 多少利率
            terms.discountDate       // 什么时候做
        );
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

        uint8 status = core.status;
        require(
            status == uint8(BillStatus.Accepted) ||
            status == uint8(BillStatus.Endorsed) ||
            status == uint8(BillStatus.Discounted),
            "Invalid status"
        );

        core.status = uint8(BillStatus.Paid);
        core.paymentDate = uint64(block.timestamp);  // 什么时候做
        core.paidAmount = core.amount;               // 给多少钱

        emit BillPaid(
            billId,
            core.paidAmount,      // 给多少钱
            core.paymentDate      // 什么时候做
        );
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

        uint8 status = core.status;
        require(status != uint8(BillStatus.Paid), "Already paid");
        require(status != uint8(BillStatus.Dishonored), "Already dishonored");

        core.status = uint8(BillStatus.Dishonored);

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
            "Not authorized"
        );
        require(core.status == uint8(BillStatus.Issued), "Can only cancel issued bills");

        core.status = uint8(BillStatus.Cancelled);

        return true;
    }

    // ==================== 查询函数 ====================

    /**
     * @dev 获取票据完整信息
     */
    function getBill(string memory billId)
        external
        view
        onlyExistingBill(billId)
        returns (
            // 谁能做
            address issuer,
            address acceptor,
            address currentHolder,
            address beneficiary,
            // 给多少钱
            uint256 amount,
            uint256 discountedAmount,
            uint256 paidAmount,
            // 什么时候做
            uint256 issueDate,
            uint256 dueDate,
            uint256 acceptanceDate,
            uint256 paymentDate,
            // 业务状态
            uint8 billType,
            uint8 status,
            uint16 endorsementCount,
            // 隐私哈希
            bytes32 billNumberHash,
            bytes32 issuerInfoHash,
            bytes32 beneficiaryInfoHash,
            bytes32 transactionDescHash,
            bytes32 contractTermsHash
        )
    {
        BillCore memory core = billCores[billId];
        PrivacyHashes memory hashes = privacyHashes[billId];

        return (
            core.issuer,
            core.acceptor,
            core.currentHolder,
            core.beneficiary,
            core.amount,
            core.discountedAmount,
            core.paidAmount,
            core.issueDate,
            core.dueDate,
            core.acceptanceDate,
            core.paymentDate,
            core.billType,
            core.status,
            core.endorsementCount,
            hashes.billNumberHash,
            hashes.issuerInfoHash,
            hashes.beneficiaryInfoHash,
            hashes.transactionDescHash,
            hashes.contractTermsHash
        );
    }

    /**
     * @dev 获取贴现条款
     */
    function getDiscountTerms(string memory billId)
        external
        view
        returns (
            uint256 rate,
            uint256 discountDate,
            uint256 discountAmount
        )
    {
        DiscountTerms memory terms = discountTerms[billId];
        return (terms.rate, terms.discountDate, terms.discountAmount);
    }

    /**
     * @dev 验证票据号码哈希
     */
    function verifyBillNumber(string memory billId, bytes32 providedHash)
        external
        view
        onlyExistingBill(billId)
        returns (bool)
    {
        return DataValidator.verifyHash(
            privacyHashes[billId].billNumberHash,
            providedHash
        );
    }

    /**
     * @dev 验证出票人信息哈希
     */
    function verifyIssuerInfo(string memory billId, bytes32 providedHash)
        external
        view
        onlyExistingBill(billId)
        returns (bool)
    {
        return DataValidator.verifyHash(
            privacyHashes[billId].issuerInfoHash,
            providedHash
        );
    }

    /**
     * @dev 验证交易描述哈希
     */
    function verifyTransactionDesc(string memory billId, bytes32 providedHash)
        external
        view
        onlyExistingBill(billId)
        returns (bool)
    {
        return DataValidator.verifyHash(
            privacyHashes[billId].transactionDescHash,
            providedHash
        );
    }

    /**
     * @dev 获取背书历史数量
     */
    function getEndorsementHistoryCount(string memory billId)
        external
        view
        returns (uint256)
    {
        return endorsementHistory[billId].length;
    }

    /**
     * @dev 转移管理员权限
     */
    function transferAdmin(address newAdmin) public onlyAdmin {
        require(DataValidator.validAddress(newAdmin), "Invalid address");
        admin = newAdmin;
    }
}
