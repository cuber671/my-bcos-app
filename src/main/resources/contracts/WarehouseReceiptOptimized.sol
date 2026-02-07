// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import "./DataPackUtils.sol";

/**
 * @title WarehouseReceiptOptimized
 * @dev 仓单融资管理智能合约 - 数据分类存储优化版
 *
 * 核心原则：
 * ✅ 上链信息：谁能做（地址）+ 什么时候做（时间）+ 给多少钱（金额）
 * 🔒 哈希化信息：货物详情、位置信息、个人隐私、长文本描述
 */
contract WarehouseReceiptOptimized {
    using DataPackUtils for *;
    using DataValidator for *;

    // ==================== 枚举定义 ====================

    enum ReceiptStatus {
        Created,      // 0 - 已创建
        Verified,     // 1 - 已验证（仓库确认）
        Pledged,      // 2 - 已质押
        Financed,     // 3 - 已融资
        Released,     // 4 - 已释放
        Liquidated,   // 5 - 已清算
        Expired       // 6 - 已过期
    }

    // ==================== 上链数据：谁能做（地址）+ 什么时候做（时间）+ 给多少钱（金额）====================

    /**
     * @dev 核心上链数据 - 仅包含业务关键信息
     *
     * 上链内容：
     * - 地址（owner, warehouse, financier）：谁能操作
     * - 时间（storageDate, expiryDate, verifiedAt等）：什么时候做
     * - 金额（totalPrice, pledgedAmount等）：给多少钱
     * - 状态（status）：当前业务状态
     */
    struct ReceiptCore {
        // ========== 谁能做（地址） ==========
        address owner;              // 货主地址（20字节）
        address warehouse;          // 仓库地址（20字节）
        address financier;          // 金融机构地址（20字节）

        // ========== 给多少钱（金额） ==========
        uint96 totalPrice;         // 总价（12字节，最大79万亿分）
        uint96 pledgedAmount;      // 质押金额（12字节）
        uint96 financedAmount;     // 融资金额（12字节）
        uint96 releaseAmount;      // 释放金额（12字节）

        // ========== 什么时候做（时间） ==========
        uint64 storageDate;        // 入库日期（8字节）
        uint64 expiryDate;         // 过期日期（8字节）
        uint64 verifiedAt;         // 验证时间（8字节）
        uint64 pledgedAt;          // 质押时间（8字节）
        uint64 financedAt;         // 融资时间（8字节）

        // ========== 业务状态 ==========
        uint8 status;              // 状态枚举（1字节）
        bool exists;               // 是否存在（1字节）
    }

    /**
     * @dev 融资条款（上链）
     */
    struct FinanceTerms {
        uint16 rate;               // 融资利率（基点，2字节）
        uint64 duration;           // 融资期限（8字节）
        uint64 repaymentDate;      // 还款日期（8字节）
    }

    // ==================== 哈希化数据：长文本 + 个人隐私 ====================

    /**
     * @dev 隐私数据哈希指纹
     *
     * 哈希化内容（不上链原文）：
     * - 货物详情：名称、规格、描述（长文本）
     * - 位置信息：仓库名称、详细地址、库区货架号
     * - 个人隐私：收件人姓名、电话、地址
     * - 合同条款：质量标准、验收标准、违约责任
     */
    struct PrivacyHashes {
        bytes32 goodsDetailsHash;      // 货物详情哈希（名称+规格+描述）
        bytes32 locationInfoHash;      // 位置信息哈希（仓库地址+库区+货架）
        bytes32 recipientInfoHash;     // 收件人信息哈希（姓名+电话+地址）
        bytes32 contractTermsHash;     // 合同条款哈希（长文本条款）
    }

    // ==================== 历史记录 ====================

    struct PledgeRecord {
        address owner;              // 质押人（谁能做）
        uint96 amount;             // 金额（给多少钱）
        uint64 timestamp;          // 时间（什么时候做）
        uint8 actionType;          // 操作类型：0=质押, 1=释放, 2=清算
    }

    struct TransferRecord {
        address from;               // 转让人
        address to;                 // 受让人
        uint96 amount;             // 转让金额
        uint64 timestamp;          // 转让时间
    }

    // ==================== 状态变量 ====================

    address public admin;
    uint256 public receiptCount;

    // 核心存储映射
    mapping(string => ReceiptCore) public receiptCores;
    mapping(string => FinanceTerms) public financeTerms;
    mapping(string => PrivacyHashes) public privacyHashes;

    // 辅助映射
    mapping(address => string[]) public ownerReceipts;
    mapping(address => string[]) public warehouseReceipts;
    mapping(address => string[]) public financierReceipts;
    mapping(string => PledgeRecord[]) public pledgeHistory;
    mapping(string => TransferRecord[]) public transferHistory;

    // ==================== 事件定义 ====================

    event ReceiptCreated(
        string indexed receiptId,
        address indexed owner,           // 谁能做
        uint256 totalPrice,              // 给多少钱
        uint64 storageDate,              // 什么时候做
        bytes32 goodsDetailsHash         // 隐私哈希
    );

    event ReceiptVerified(
        string indexed receiptId,
        address indexed warehouse,       // 谁能做
        uint64 verifiedAt                // 什么时候做
    );

    event ReceiptPledged(
        string indexed receiptId,
        address indexed owner,           // 谁能做
        address indexed financier,       // 谁能做
        uint256 pledgedAmount,           // 给多少钱
        uint64 pledgedAt                 // 什么时候做
    );

    event ReceiptFinanced(
        string indexed receiptId,
        address indexed financier,       // 谁能做
        uint256 financedAmount,          // 给多少钱
        uint256 rate,                    // 多少利率
        uint64 financedAt                // 什么时候做
    );

    event ReceiptReleased(
        string indexed receiptId,
        uint256 releaseAmount,           // 给多少钱
        uint64 releasedAt                // 什么时候做
    );

    event ReceiptLiquidated(
        string indexed receiptId,
        string reason
    );

    event ReceiptTransferred(
        string indexed receiptId,
        address indexed from,            // 谁能做
        address indexed to,              // 谁能做
        uint256 amount,                  // 给多少钱
        uint64 transferredAt             // 什么时候做
    );

    // ==================== 修饰器 ====================

    modifier onlyAdmin() {
        require(msg.sender == admin, "Only admin");
        _;
    }

    modifier onlyExistingReceipt(string memory receiptId) {
        require(receiptCores[receiptId].exists, "Receipt not found");
        _;
    }

    modifier onlyOwner(string memory receiptId) {
        require(receiptCores[receiptId].owner == msg.sender, "Not owner");
        _;
    }

    modifier onlyWarehouse(string memory receiptId) {
        require(receiptCores[receiptId].warehouse == msg.sender, "Not warehouse");
        _;
    }

    modifier onlyFinancier(string memory receiptId) {
        require(receiptCores[receiptId].financier == msg.sender, "Not financier");
        _;
    }

    // ==================== 构造函数 ====================

    constructor() {
        admin = msg.sender;
        receiptCount = 0;
    }

    // ==================== 核心业务函数 ====================

    /**
     * @dev 创建仓单
     *
     * 上链参数（谁能做 + 什么时候做 + 给多少钱）：
     * @param receiptId 仓单ID
     * @param warehouse 仓库地址（谁能做）
     * @param totalPrice 总价（给多少钱）
     * @param storageDate 入库日期（什么时候做）
     * @param expiryDate 过期日期
     *
     * 哈希参数（隐私信息）：
     * @param goodsDetailsHash 货物详情哈希（名称、规格、描述等长文本）
     * @param locationInfoHash 位置信息哈希（仓库详细地址、库区、货架）
     * @param recipientInfoHash 收件人信息哈希（姓名、电话、地址等隐私）
     * @param contractTermsHash 合同条款哈希（质量标准、违约条款等长文本）
     */
    function createReceipt(
        string calldata receiptId,
        address warehouse,
        uint96 totalPrice,
        uint64 storageDate,
        uint64 expiryDate,
        bytes32 goodsDetailsHash,
        bytes32 locationInfoHash,
        bytes32 recipientInfoHash,
        bytes32 contractTermsHash
    ) external returns (bool) {
        // 验证参数
        require(bytes(receiptId).length > 0, "Invalid ID");
        require(!receiptCores[receiptId].exists, "Exists");
        require(DataValidator.validAddress(warehouse), "Invalid warehouse");
        require(DataValidator.validAmount(totalPrice), "Invalid amount");
        require(DataValidator.validTimeline(storageDate, expiryDate), "Invalid dates");
        require(DataValidator.validHash(goodsDetailsHash), "Invalid goods hash");
        require(DataValidator.validHash(locationInfoHash), "Invalid location hash");
        require(DataValidator.validHash(recipientInfoHash), "Invalid recipient hash");
        require(DataValidator.validHash(contractTermsHash), "Invalid contract hash");

        // 存储核心数据（上链：谁能做 + 什么时候做 + 给多少钱）
        ReceiptCore storage core = receiptCores[receiptId];
        core.owner = msg.sender;              // 谁能做
        core.warehouse = warehouse;           // 谁能做
        core.financier = address(0);
        core.totalPrice = totalPrice;         // 给多少钱
        core.pledgedAmount = 0;
        core.financedAmount = 0;
        core.releaseAmount = 0;
        core.storageDate = storageDate;       // 什么时候做
        core.expiryDate = expiryDate;         // 什么时候做
        core.verifiedAt = 0;
        core.pledgedAt = 0;
        core.financedAt = 0;
        core.status = uint8(ReceiptStatus.Created);
        core.exists = true;

        // 存储隐私哈希（不上链原文）
        PrivacyHashes storage hashes = privacyHashes[receiptId];
        hashes.goodsDetailsHash = goodsDetailsHash;
        hashes.locationInfoHash = locationInfoHash;
        hashes.recipientInfoHash = recipientInfoHash;
        hashes.contractTermsHash = contractTermsHash;

        ownerReceipts[msg.sender].push(receiptId);
        warehouseReceipts[warehouse].push(receiptId);
        receiptCount++;

        emit ReceiptCreated(
            receiptId,
            msg.sender,          // 谁能做
            totalPrice,          // 给多少钱
            storageDate,         // 什么时候做
            goodsDetailsHash
        );
        return true;
    }

    /**
     * @dev 仓库验证仓单
     */
    function verifyReceipt(string memory receiptId)
        public
        onlyExistingReceipt(receiptId)
        onlyWarehouse(receiptId)
        returns (bool)
    {
        ReceiptCore storage core = receiptCores[receiptId];
        require(core.status == uint8(ReceiptStatus.Created), "Invalid status");

        core.status = uint8(ReceiptStatus.Verified);
        core.verifiedAt = uint64(block.timestamp);  // 什么时候做

        emit ReceiptVerified(receiptId, msg.sender, core.verifiedAt);
        return true;
    }

    /**
     * @dev 质押仓单
     */
    function pledgeReceipt(
        string memory receiptId,
        address financier,
        uint96 pledgedAmount
    ) public onlyExistingReceipt(receiptId) returns (bool) {
        ReceiptCore storage core = receiptCores[receiptId];
        require(core.owner == msg.sender, "Not owner");
        require(core.status == uint8(ReceiptStatus.Verified), "Not verified");
        require(DataValidator.validAddress(financier), "Invalid financier");
        require(DataValidator.validAmount(pledgedAmount), "Invalid amount");
        require(pledgedAmount <= core.totalPrice, "Amount exceeds total");

        core.status = uint8(ReceiptStatus.Pledged);
        core.financier = financier;             // 谁能做
        core.pledgedAmount = pledgedAmount;     // 给多少钱
        core.pledgedAt = uint64(block.timestamp);  // 什么时候做

        // 记录历史
        pledgeHistory[receiptId].push(PledgeRecord({
            owner: msg.sender,
            amount: pledgedAmount,
            timestamp: core.pledgedAt,
            actionType: 0  // 质押
        }));

        emit ReceiptPledged(
            receiptId,
            msg.sender,          // 谁能做
            financier,           // 谁能做
            pledgedAmount,       // 给多少钱
            core.pledgedAt       // 什么时候做
        );
        return true;
    }

    /**
     * @dev 融资
     */
    function financeReceipt(
        string memory receiptId,
        uint96 financedAmount,
        uint16 rate,
        uint64 duration
    ) public onlyExistingReceipt(receiptId) returns (bool) {
        ReceiptCore storage core = receiptCores[receiptId];
        require(core.financier == msg.sender, "Not financier");
        require(core.status == uint8(ReceiptStatus.Pledged), "Not pledged");
        require(DataValidator.validAmount(financedAmount), "Invalid amount");
        require(DataValidator.validRate(rate), "Invalid rate");
        require(financedAmount <= core.pledgedAmount, "Exceeds pledged");

        core.status = uint8(ReceiptStatus.Financed);
        core.financedAmount = financedAmount;   // 给多少钱
        core.financedAt = uint64(block.timestamp);  // 什么时候做

        // 存储融资条款（上链）
        FinanceTerms storage terms = financeTerms[receiptId];
        terms.rate = rate;                      // 多少利率
        terms.duration = duration;              // 多久期限
        terms.repaymentDate = core.financedAt + duration;

        financierReceipts[msg.sender].push(receiptId);

        emit ReceiptFinanced(
            receiptId,
            msg.sender,          // 谁能做
            financedAmount,      // 给多少钱
            rate,                // 多少利率
            core.financedAt      // 什么时候做
        );
        return true;
    }

    /**
     * @dev 释放仓单（还款后）
     */
    function releaseReceipt(string memory receiptId)
        public
        onlyExistingReceipt(receiptId)
        returns (bool)
    {
        ReceiptCore storage core = receiptCores[receiptId];
        require(
            msg.sender == core.owner || msg.sender == core.financier,
            "Not authorized"
        );
        require(core.status == uint8(ReceiptStatus.Financed), "Not financed");

        core.status = uint8(ReceiptStatus.Released);
        core.releaseAmount = core.financedAmount;  // 给多少钱

        // 记录历史
        pledgeHistory[receiptId].push(PledgeRecord({
            owner: core.owner,
            amount: core.financedAmount,
            timestamp: uint64(block.timestamp),
            actionType: 1  // 释放
        }));

        emit ReceiptReleased(
            receiptId,
            core.releaseAmount,   // 给多少钱
            uint64(block.timestamp)  // 什么时候做
        );
        return true;
    }

    /**
     * @dev 清算仓单（违约时）
     */
    function liquidateReceipt(string memory receiptId, string memory reason)
        public
        onlyAdmin
        onlyExistingReceipt(receiptId)
        returns (bool)
    {
        ReceiptCore storage core = receiptCores[receiptId];
        require(
            core.status == uint8(ReceiptStatus.Pledged) || core.status == uint8(ReceiptStatus.Financed),
            "Invalid status"
        );
        require(uint64(block.timestamp) > core.expiryDate, "Not expired");

        core.status = uint8(ReceiptStatus.Liquidated);

        // 记录历史
        pledgeHistory[receiptId].push(PledgeRecord({
            owner: core.owner,
            amount: core.pledgedAmount,
            timestamp: uint64(block.timestamp),
            actionType: 2  // 清算
        }));

        emit ReceiptLiquidated(receiptId, reason);
        return true;
    }

    /**
     * @dev 转让仓单
     */
    function transferReceipt(
        string memory receiptId,
        address newOwner,
        uint96 transferPrice
    ) public onlyExistingReceipt(receiptId) returns (bool) {
        ReceiptCore storage core = receiptCores[receiptId];
        require(core.owner == msg.sender, "Not owner");
        require(DataValidator.validAddress(newOwner), "Invalid new owner");
        require(newOwner != core.owner, "Cannot transfer to self");

        uint8 status = core.status;
        require(
            status == uint8(ReceiptStatus.Created) || status == uint8(ReceiptStatus.Verified),
            "Cannot transfer"
        );

        address previousOwner = core.owner;
        core.owner = newOwner;  // 谁能做

        ownerReceipts[newOwner].push(receiptId);

        // 记录历史
        transferHistory[receiptId].push(TransferRecord({
            from: previousOwner,
            to: newOwner,
            amount: transferPrice,  // 给多少钱
            timestamp: uint64(block.timestamp)  // 什么时候做
        }));

        emit ReceiptTransferred(
            receiptId,
            previousOwner,      // 谁能做
            newOwner,           // 谁能做
            transferPrice,      // 给多少钱
            uint64(block.timestamp)  // 什么时候做
        );
        return true;
    }

    // ==================== 查询函数 ====================

    /**
     * @dev 获取仓单完整信息（仅上链数据）
     */
    function getReceipt(string memory receiptId)
        external
        view
        onlyExistingReceipt(receiptId)
        returns (
            // 谁能做
            address owner,
            address warehouse,
            address financier,
            // 给多少钱
            uint256 totalPrice,
            uint256 pledgedAmount,
            uint256 financedAmount,
            // 什么时候做
            uint256 storageDate,
            uint256 expiryDate,
            uint256 verifiedAt,
            uint256 pledgedAt,
            uint256 financedAt,
            // 业务状态
            uint8 status,
            // 隐私哈希
            bytes32 goodsDetailsHash,
            bytes32 locationInfoHash,
            bytes32 recipientInfoHash,
            bytes32 contractTermsHash
        )
    {
        ReceiptCore memory core = receiptCores[receiptId];
        PrivacyHashes memory hashes = privacyHashes[receiptId];

        return (
            core.owner,
            core.warehouse,
            core.financier,
            core.totalPrice,
            core.pledgedAmount,
            core.financedAmount,
            core.storageDate,
            core.expiryDate,
            core.verifiedAt,
            core.pledgedAt,
            core.financedAt,
            core.status,
            hashes.goodsDetailsHash,
            hashes.locationInfoHash,
            hashes.recipientInfoHash,
            hashes.contractTermsHash
        );
    }

    /**
     * @dev 获取融资条款
     */
    function getFinanceTerms(string memory receiptId)
        external
        view
        returns (
            uint256 rate,
            uint256 duration,
            uint256 repaymentDate
        )
    {
        FinanceTerms memory terms = financeTerms[receiptId];
        return (terms.rate, terms.duration, terms.repaymentDate);
    }

    /**
     * @dev 验证货物详情哈希
     */
    function verifyGoodsDetails(string memory receiptId, bytes32 providedHash)
        external
        view
        onlyExistingReceipt(receiptId)
        returns (bool)
    {
        return DataValidator.verifyHash(
            privacyHashes[receiptId].goodsDetailsHash,
            providedHash
        );
    }

    /**
     * @dev 验证位置信息哈希
     */
    function verifyLocationInfo(string memory receiptId, bytes32 providedHash)
        external
        view
        onlyExistingReceipt(receiptId)
        returns (bool)
    {
        return DataValidator.verifyHash(
            privacyHashes[receiptId].locationInfoHash,
            providedHash
        );
    }

    /**
     * @dev 验证收件人信息哈希（隐私）
     */
    function verifyRecipientInfo(string memory receiptId, bytes32 providedHash)
        external
        view
        onlyExistingReceipt(receiptId)
        returns (bool)
    {
        return DataValidator.verifyHash(
            privacyHashes[receiptId].recipientInfoHash,
            providedHash
        );
    }

    /**
     * @dev 验证合同条款哈希
     */
    function verifyContractTerms(string memory receiptId, bytes32 providedHash)
        external
        view
        onlyExistingReceipt(receiptId)
        returns (bool)
    {
        return DataValidator.verifyHash(
            privacyHashes[receiptId].contractTermsHash,
            providedHash
        );
    }

    /**
     * @dev 获取质押历史数量
     */
    function getPledgeHistoryCount(string memory receiptId)
        external
        view
        returns (uint256)
    {
        return pledgeHistory[receiptId].length;
    }

    /**
     * @dev 转移管理员权限
     */
    function transferAdmin(address newAdmin) public onlyAdmin {
        require(DataValidator.validAddress(newAdmin), "Invalid address");
        admin = newAdmin;
    }
}
