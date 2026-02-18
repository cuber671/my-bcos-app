// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

/**
 * @title WarehouseReceiptV2
 * @dev 仓单融资管理智能合约 V2.0
 *
 * 核心功能：
 * 1. 仓单生命周期管理（创建、验证、质押、融资、释放、清算）
 * 2. 仓单转让管理（转让、转移）
 * 3. 融资管理（申请融资、批准融资、偿还融资）
 * 4. 冻结管理（冻结仓单、解冻仓单）
 *
 * 设计原则：
 * ✅ 数据精简化与哈希化（仅核心字段上链，扩展数据哈希化）
 * ✅ 严格访问控制（onlyAdmin, onlyJavaBackend, onlyOwner, onlyWarehouse）
 * ✅ 规避栈溢出（使用struct封装）
 * ✅ 异常处理与状态一致性（require校验，Checks-Effects-Interactions模式）
 * ✅ 充分利用事件（10个事件支持Java后端监听）
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
contract WarehouseReceiptV2 {

    // ==================== 枚举定义 ====================

    /**
     * @dev 仓单状态枚举
     */
    enum ReceiptStatus {
        Created,       // 已创建
        Verified,      // 已验证（仓库确认）
        Pledged,       // 已质押
        Financed,      // 已融资
        Released,      // 已释放
        Liquidated,    // 已清算
        Expired,       // 已过期
        Frozen         // 已冻结
    }

    /**
     * @dev 质押状态枚举
     */
    enum PledgeStatus {
        Active,        // 活跃
        Released,      // 已释放
        Liquidated     // 已清算
    }

    /**
     * @dev 融资状态枚举
     */
    enum FinanceStatus {
        Pending,       // 待审批
        Approved,      // 已批准
        Rejected,      // 已拒绝
        Active,        // 融资中
        Completed,     // 已完成
        Defaulted      // 已违约
    }

    // ==================== 结构体定义（避免栈溢出）====================

    /**
     * @dev 核心仓单数据（仅上链关键字段）
     */
    struct ReceiptCore {
        string receiptId;              // 仓单ID（索引）
        address owner;                 // 货主地址（权限：所有权）
        address warehouse;             // 仓库地址（权限：验证权）
        address financier;             // 金融机构地址（权限：融资权）
        uint256 totalPrice;            // 总价（金额计算）
        uint256 storageDate;           // 入库日期（时间逻辑）
        uint256 expiryDate;            // 过期日期（到期判断）
        ReceiptStatus status;          // 状态（状态流转）
        bool frozen;                   // 冻结标志（权限控制）
        bool exists;                   // 存在标志
    }

    /**
     * @dev 仓单元数据（哈希化存储）
     */
    struct ReceiptMeta {
        string receiptId;
        bytes32 coreDataHash;          // 核心数据哈希（货物详情）
        bytes32 extendedDataHash;      // 扩展数据哈希（位置、收件人等信息）
        bytes32 financeDataHash;       // 融资数据哈希（融资条款等）
        uint256 createdAt;
        uint256 updatedAt;
    }

    /**
     * @dev 质押记录结构
     */
    struct Pledge {
        string receiptId;
        address financier;             // 质押权人（金融机构）
        uint256 amount;                // 质押金额
        uint256 rate;                  // 质押利率（基点）
        uint256 pledgeDate;            // 质押日期
        PledgeStatus status;           // 质押状态
        bool active;                   // 是否活跃
    }

    /**
     * @dev 融资记录结构
     */
    struct Finance {
        string receiptId;
        address institution;           // 融资机构
        uint256 amount;                // 融资金额
        uint256 rate;                  // 融资利率（基点）
        uint256 duration;              // 融资期限（天）
        uint256 financeDate;           // 融资日期
        FinanceStatus status;          // 融资状态
        bool active;                   // 是否活跃
    }

    /**
     * @dev 转让记录结构
     */
    struct TransferRecord {
        string receiptId;
        address from;                  // 转让人
        address to;                    // 受让人
        uint256 amount;                // 转让金额
        uint256 transferDate;          // 转让日期
    }

    /**
     * @dev 仓单完整信息结构（用于返回，避免栈溢出）
     * @notice 封装所有仓单相关数据，减少函数返回参数数量
     */
    struct ReceiptInfo {
        string receiptId;
        address owner;
        address warehouse;
        address financier;
        uint256 totalPrice;
        uint256 storageDate;
        uint256 expiryDate;
        ReceiptStatus status;
        bool frozen;
        bytes32 coreDataHash;
        bytes32 extendedDataHash;
    }

    // ==================== 状态变量 ====================

    // 管理员
    address public admin;
    address public javaBackend;

    // 计数器
    uint256 public receiptCount;

    // 双层存储映射
    mapping(string => ReceiptCore) public receiptCores;
    mapping(string => ReceiptMeta) public receiptMetas;

    // 功能模块映射
    mapping(string => Pledge) public pledges;
    mapping(string => Finance) public finances;
    mapping(string => TransferRecord[]) public transferHistory;

    // 辅助映射
    mapping(address => string[]) public ownerReceipts;        // 货主仓单列表
    mapping(address => string[]) public warehouseReceipts;    // 仓库仓单列表
    mapping(address => string[]) public financierReceipts;    // 金融机构仓单列表
    mapping(string => bool) public frozenReceipts;            // 冻结仓单列表

    // ==================== 修饰器（访问控制）====================

    /**
     * @dev 仅管理员可调用
     */
    modifier onlyAdmin() {
        require(msg.sender == admin, "Only admin");
        _;
    }

    /**
     * @dev 仅Java后端可调用
     */
    modifier onlyJavaBackend() {
        require(msg.sender == javaBackend, "Only Java backend");
        _;
    }

    /**
     * @dev 仅当前货主可调用
     */
    modifier onlyOwner(string memory receiptId) {
        require(
            receiptCores[receiptId].owner == msg.sender,
            "Only owner"
        );
        _;
    }

    /**
     * @dev 仅仓库可调用
     */
    modifier onlyWarehouse(string memory receiptId) {
        require(
            receiptCores[receiptId].warehouse == msg.sender,
            "Only warehouse"
        );
        _;
    }

    /**
     * @dev 仅金融机构可调用
     */
    modifier onlyFinancier(string memory receiptId) {
        require(
            receiptCores[receiptId].financier == msg.sender,
            "Only financier"
        );
        _;
    }

    /**
     * @dev 仓单必须存在且未冻结
     */
    modifier receiptExists(string memory receiptId) {
        require(receiptCores[receiptId].exists, "Receipt not exist");
        require(!receiptCores[receiptId].frozen, "Receipt frozen");
        _;
    }

    // ==================== 事件定义 ====================

    // 仓单生命周期事件
    event ReceiptCreated(
        string indexed receiptId,
        address indexed owner,
        address indexed warehouse,
        uint256 totalPrice,
        bytes32 coreDataHash,
        bytes32 extendedDataHash
    );

    event ReceiptVerified(
        string indexed receiptId,
        address indexed warehouse,
        uint256 verifiedDate
    );

    event ReceiptPledged(
        string indexed receiptId,
        address indexed owner,
        address indexed financier,
        uint256 amount,
        uint256 rate
    );

    event ReceiptFinanced(
        string indexed receiptId,
        address indexed financier,
        uint256 amount,
        uint256 rate
    );

    event ReceiptReleased(
        string indexed receiptId,
        uint256 amount,
        uint256 releaseDate
    );

    event ReceiptLiquidated(
        string indexed receiptId,
        address indexed admin,
        string reason
    );

    event ReceiptTransferred(
        string indexed receiptId,
        address indexed from,
        address indexed to,
        uint256 amount
    );

    // 冻结事件
    event ReceiptFrozen(
        string indexed receiptId,
        address indexed admin,
        string reason
    );

    event ReceiptUnfrozen(
        string indexed receiptId,
        address indexed admin,
        string reason
    );

    // 管理员事件
    event AdminSet(address indexed oldAdmin, address indexed newAdmin);
    event JavaBackendSet(address indexed oldBackend, address indexed newBackend);

    // ==================== 构造函数 ====================

    /**
     * @dev 构造函数，初始化合约
     */
    constructor() {
        admin = msg.sender;
        javaBackend = msg.sender;
        receiptCount = 0;

        emit AdminSet(address(0), admin);
        emit JavaBackendSet(address(0), javaBackend);
    }

    // ==================== 管理员功能 ====================

    /**
     * @dev 设置管理员
     * @param newAdmin 新管理员地址
     */
    function setAdmin(address newAdmin) external onlyAdmin {
        address oldAdmin = admin;
        admin = newAdmin;
        emit AdminSet(oldAdmin, newAdmin);
    }

    /**
     * @dev 设置Java后端地址
     * @param newBackend 新后端地址
     */
    function setJavaBackend(address newBackend) external onlyAdmin {
        address oldBackend = javaBackend;
        javaBackend = newBackend;
        emit JavaBackendSet(oldBackend, newBackend);
    }

    // ==================== 1. 仓单生命周期管理 ====================

    /**
     * @dev 创建仓单
     * @param receiptId 仓单ID
     * @param warehouse 仓库地址
     * @param totalPrice 总价
     * @param storageDate 入库日期（时间戳）
     * @param expiryDate 过期日期（时间戳）
     * @param coreDataHash 核心数据哈希
     * @param extendedDataHash 扩展数据哈希
     * @return bool 是否成功
     */
    function createReceipt(
        string memory receiptId,
        address warehouse,
        uint256 totalPrice,
        uint256 storageDate,
        uint256 expiryDate,
        bytes32 coreDataHash,
        bytes32 extendedDataHash
    )
        external
        onlyJavaBackend
        returns (bool)
    {
        // Checks（检查）
        require(bytes(receiptId).length > 0, "Invalid receipt ID");
        require(!receiptCores[receiptId].exists, "Receipt already exists");
        require(warehouse != address(0), "Invalid warehouse");
        require(totalPrice > 0, "Invalid total price");
        require(storageDate > 0, "Invalid storage date");
        require(expiryDate > storageDate, "Invalid expiry date");
        require(bytes32(coreDataHash) != bytes32(0), "Invalid core hash");
        require(bytes32(extendedDataHash) != bytes32(0), "Invalid extended hash");

        // Effects（生效）
        ReceiptCore storage receipt = receiptCores[receiptId];
        receipt.receiptId = receiptId;
        receipt.owner = msg.sender;
        receipt.warehouse = warehouse;
        receipt.financier = address(0);
        receipt.totalPrice = totalPrice;
        receipt.storageDate = storageDate;
        receipt.expiryDate = expiryDate;
        receipt.status = ReceiptStatus.Created;
        receipt.frozen = false;
        receipt.exists = true;

        // 元数据
        receiptMetas[receiptId] = ReceiptMeta({
            receiptId: receiptId,
            coreDataHash: coreDataHash,
            extendedDataHash: extendedDataHash,
            financeDataHash: 0x0,
            createdAt: block.timestamp,
            updatedAt: block.timestamp
        });

        // 更新计数器
        receiptCount++;

        // 更新索引
        ownerReceipts[msg.sender].push(receiptId);
        warehouseReceipts[warehouse].push(receiptId);

        // Events（事件）
        emit ReceiptCreated(receiptId, msg.sender, warehouse, totalPrice, coreDataHash, extendedDataHash);

        return true;
    }

    /**
     * @dev 验证仓单
     * @param receiptId 仓单ID
     * @return bool 是否成功
     */
    function verifyReceipt(string memory receiptId)
        external
        receiptExists(receiptId)
        onlyWarehouse(receiptId)
        returns (bool)
    {
        // Checks
        require(receiptCores[receiptId].status == ReceiptStatus.Created, "Invalid status for verification");

        // Effects
        ReceiptCore storage receipt = receiptCores[receiptId];
        receipt.status = ReceiptStatus.Verified;
        receiptMetas[receiptId].updatedAt = block.timestamp;

        // Event
        emit ReceiptVerified(receiptId, msg.sender, block.timestamp);

        return true;
    }

    /**
     * @dev 释放仓单
     * @param receiptId 仓单ID
     * @param amount 释放金额
     * @return bool 是否成功
     */
    function releaseReceipt(string memory receiptId, uint256 amount)
        external
        receiptExists(receiptId)
        onlyFinancier(receiptId)
        returns (bool)
    {
        // Checks
        require(receiptCores[receiptId].status == ReceiptStatus.Financed, "Invalid status for release");
        require(amount > 0, "Invalid release amount");

        // Effects
        ReceiptCore storage receipt = receiptCores[receiptId];
        receipt.status = ReceiptStatus.Released;
        receiptMetas[receiptId].updatedAt = block.timestamp;

        // Event
        emit ReceiptReleased(receiptId, amount, block.timestamp);

        return true;
    }

    /**
     * @dev 清算仓单
     * @param receiptId 仓单ID
     * @param reason 清算原因
     * @return bool 是否成功
     */
    function liquidateReceipt(string memory receiptId, string memory reason)
        external
        receiptExists(receiptId)
        onlyAdmin
        returns (bool)
    {
        // Effects
        ReceiptCore storage receipt = receiptCores[receiptId];
        receipt.status = ReceiptStatus.Liquidated;
        receiptMetas[receiptId].updatedAt = block.timestamp;

        // Event
        emit ReceiptLiquidated(receiptId, msg.sender, reason);

        return true;
    }

    // ==================== 2. 质押管理 ====================

    /**
     * @dev 质押仓单
     * @param receiptId 仓单ID
     * @param financier 金融机构地址
     * @param amount 质押金额
     * @param rate 质押利率（基点）
     * @return bool 是否成功
     */
    function pledgeReceipt(
        string memory receiptId,
        address financier,
        uint256 amount,
        uint256 rate
    )
        external
        receiptExists(receiptId)
        onlyOwner(receiptId)
        returns (bool)
    {
        // Checks
        require(receiptCores[receiptId].status == ReceiptStatus.Verified, "Invalid status for pledge");
        require(financier != address(0), "Invalid financier");
        require(amount > 0, "Invalid pledge amount");
        require(!pledges[receiptId].active, "Already pledged");

        // Effects
        ReceiptCore storage receipt = receiptCores[receiptId];
        receipt.status = ReceiptStatus.Pledged;
        receipt.financier = financier;
        receiptMetas[receiptId].updatedAt = block.timestamp;

        pledges[receiptId] = Pledge({
            receiptId: receiptId,
            financier: financier,
            amount: amount,
            rate: rate,
            pledgeDate: block.timestamp,
            status: PledgeStatus.Active,
            active: true
        });

        // Event
        emit ReceiptPledged(receiptId, msg.sender, financier, amount, rate);

        return true;
    }

    /**
     * @dev 解除质押
     * @param receiptId 仓单ID
     * @return bool 是否成功
     */
    function unpledgeReceipt(string memory receiptId)
        external
        receiptExists(receiptId)
        onlyFinancier(receiptId)
        returns (bool)
    {
        // Checks
        require(pledges[receiptId].active, "Not pledged");

        // Effects
        ReceiptCore storage receipt = receiptCores[receiptId];
        receipt.status = ReceiptStatus.Verified;
        receiptMetas[receiptId].updatedAt = block.timestamp;

        pledges[receiptId].active = false;
        pledges[receiptId].status = PledgeStatus.Released;

        return true;
    }

    // ==================== 3. 融资管理 ====================

    /**
     * @dev 申请融资
     * @param receiptId 仓单ID
     * @param institution 融资机构
     * @param amount 融资金额
     * @param rate 融资利率（基点）
     * @param duration 融资期限（天）
     * @return bool 是否成功
     */
    function applyFinance(
        string memory receiptId,
        address institution,
        uint256 amount,
        uint256 rate,
        uint256 duration
    )
        external
        receiptExists(receiptId)
        onlyOwner(receiptId)
        returns (bool)
    {
        // Checks
        require(receiptCores[receiptId].status == ReceiptStatus.Pledged, "Invalid status for finance");
        require(institution != address(0), "Invalid institution");
        require(amount > 0, "Invalid finance amount");
        require(!finances[receiptId].active, "Already financed");

        // Effects
        finances[receiptId] = Finance({
            receiptId: receiptId,
            institution: institution,
            amount: amount,
            rate: rate,
            duration: duration,
            financeDate: block.timestamp,
            status: FinanceStatus.Pending,
            active: true
        });

        // Event
        emit ReceiptFinanced(receiptId, institution, amount, rate);

        return true;
    }

    /**
     * @dev 批准融资
     * @param receiptId 仓单ID
     * @return bool 是否成功
     */
    function approveFinance(string memory receiptId)
        external
        receiptExists(receiptId)
        onlyJavaBackend
        returns (bool)
    {
        // Checks
        require(finances[receiptId].active, "No finance application");
        require(finances[receiptId].status == FinanceStatus.Pending, "Not pending");

        // Effects
        ReceiptCore storage receipt = receiptCores[receiptId];
        receipt.status = ReceiptStatus.Financed;
        receiptMetas[receiptId].updatedAt = block.timestamp;

        finances[receiptId].status = FinanceStatus.Approved;

        return true;
    }

    // ==================== 4. 转让管理 ====================

    /**
     * @dev 转让仓单
     * @param receiptId 仓单ID
     * @param to 受让人地址
     * @param amount 转让金额
     * @return bool 是否成功
     */
    function transferReceipt(
        string memory receiptId,
        address to,
        uint256 amount
    )
        external
        receiptExists(receiptId)
        onlyOwner(receiptId)
        returns (bool)
    {
        // Checks
        require(to != address(0), "Invalid recipient");
        require(receiptCores[receiptId].status == ReceiptStatus.Verified, "Invalid status for transfer");
        require(amount > 0 && amount <= receiptCores[receiptId].totalPrice, "Invalid amount");

        // Effects
        ReceiptCore storage receipt = receiptCores[receiptId];
        address previousOwner = receipt.owner;
        receipt.owner = to;
        receiptMetas[receiptId].updatedAt = block.timestamp;

        // 索引更新
        ownerReceipts[to].push(receiptId);

        // 历史记录
        transferHistory[receiptId].push(TransferRecord({
            receiptId: receiptId,
            from: previousOwner,
            to: to,
            amount: amount,
            transferDate: block.timestamp
        }));

        // Event
        emit ReceiptTransferred(receiptId, previousOwner, to, amount);

        return true;
    }

    // ==================== 5. 冻结管理 ====================

    /**
     * @dev 冻结仓单
     * @param receiptId 仓单ID
     * @param reason 冻结原因
     * @return bool 是否成功
     */
    function freezeReceipt(string memory receiptId, string memory reason)
        external
        receiptExists(receiptId)
        onlyAdmin
        returns (bool)
    {
        // Effects
        receiptCores[receiptId].frozen = true;
        receiptMetas[receiptId].updatedAt = block.timestamp;
        frozenReceipts[receiptId] = true;

        // Event
        emit ReceiptFrozen(receiptId, msg.sender, reason);

        return true;
    }

    /**
     * @dev 解除冻结
     * @param receiptId 仓单ID
     * @param reason 解冻原因
     * @return bool 是否成功
     */
    function unfreezeReceipt(string memory receiptId, string memory reason)
        external
        receiptExists(receiptId)
        onlyAdmin
        returns (bool)
    {
        // Effects
        receiptCores[receiptId].frozen = false;
        receiptMetas[receiptId].updatedAt = block.timestamp;
        frozenReceipts[receiptId] = false;

        // Event
        emit ReceiptUnfrozen(receiptId, msg.sender, reason);

        return true;
    }

    // ==================== 查询功能 ====================

    /**
     * @dev 查询仓单完整信息（优化版 - 使用结构体避免栈溢出）
     * @param receiptId 仓单ID
     * @return info 仓单完整信息结构体
     */
    function getReceipt(string memory receiptId)
        external
        view
        returns (ReceiptInfo memory info)
    {
        ReceiptCore storage receipt = receiptCores[receiptId];
        ReceiptMeta storage meta = receiptMetas[receiptId];

        require(receipt.exists, "Receipt not found");

        return ReceiptInfo({
            receiptId: receipt.receiptId,
            owner: receipt.owner,
            warehouse: receipt.warehouse,
            financier: receipt.financier,
            totalPrice: receipt.totalPrice,
            storageDate: receipt.storageDate,
            expiryDate: receipt.expiryDate,
            status: receipt.status,
            frozen: receipt.frozen,
            coreDataHash: meta.coreDataHash,
            extendedDataHash: meta.extendedDataHash
        });
    }

    /**
     * @dev 查询质押信息
     * @param receiptId 仓单ID
     * @return financier 金融机构
     * @return amount 质押金额
     * @return rate 质押利率
     * @return status 质押状态
     * @return active 是否活跃
     */
    function getPledge(string memory receiptId)
        external
        view
        returns (
            address financier,
            uint256 amount,
            uint256 rate,
            PledgeStatus status,
            bool active
        )
    {
        Pledge storage pledge = pledges[receiptId];
        require(receiptCores[receiptId].exists, "Receipt not found");

        return (
            pledge.financier,
            pledge.amount,
            pledge.rate,
            pledge.status,
            pledge.active
        );
    }

    /**
     * @dev 查询融资信息
     * @param receiptId 仓单ID
     * @return institution 融资机构
     * @return amount 融资金额
     * @return rate 融资利率
     * @return status 融资状态
     * @return active 是否活跃
     */
    function getFinance(string memory receiptId)
        external
        view
        returns (
            address institution,
            uint256 amount,
            uint256 rate,
            FinanceStatus status,
            bool active
        )
    {
        Finance storage finance = finances[receiptId];
        require(receiptCores[receiptId].exists, "Receipt not found");

        return (
            finance.institution,
            finance.amount,
            finance.rate,
            finance.status,
            finance.active
        );
    }

    /**
     * @dev 查询转让历史数量
     * @param receiptId 仓单ID
     * @return 转让次数
     */
    function getTransferCount(string memory receiptId)
        external
        view
        returns (uint256)
    {
        require(receiptCores[receiptId].exists, "Receipt not found");
        return transferHistory[receiptId].length;
    }

    /**
     * @dev 查询用户仓单
     * @param user 用户地址
     * @return 仓单ID数组
     */
    function getOwnerReceipts(address user)
        external
        view
        returns (string[] memory)
    {
        return ownerReceipts[user];
    }

    /**
     * @dev 查询仓库仓单
     * @param warehouse 仓库地址
     * @return 仓单ID数组
     */
    function getWarehouseReceipts(address warehouse)
        external
        view
        returns (string[] memory)
    {
        return warehouseReceipts[warehouse];
    }

    /**
     * @dev 查询金融机构仓单
     * @param financier 金融机构地址
     * @return 仓单ID数组
     */
    function getFinancierReceipts(address financier)
        external
        view
        returns (string[] memory)
    {
        return financierReceipts[financier];
    }

    /**
     * @dev 查询仓单总数
     * @return 仓单总数
     */
    function getReceiptCount()
        external
        view
        returns (uint256)
    {
        return receiptCount;
    }

    /**
     * @dev 检查仓单是否存在
     * @param receiptId 仓单ID
     * @return 是否存在
     */
    function isReceiptExists(string memory receiptId)
        external
        view
        returns (bool)
    {
        return receiptCores[receiptId].exists;
    }

    /**
     * @dev 检查仓单是否冻结
     * @param receiptId 仓单ID
     * @return 是否冻结
     */
    function isReceiptFrozen(string memory receiptId)
        external
        view
        returns (bool)
    {
        return receiptCores[receiptId].frozen;
    }

    /**
     * @dev 检查仓单是否质押
     * @param receiptId 仓单ID
     * @return 是否质押
     */
    function isReceiptPledged(string memory receiptId)
        external
        view
        returns (bool)
    {
        return pledges[receiptId].active;
    }

    /**
     * @dev 检查仓单是否在融资
     * @param receiptId 仓单ID
     * @return 是否在融资
     */
    function isReceiptFinanced(string memory receiptId)
        external
        view
        returns (bool)
    {
        return finances[receiptId].active;
    }

    /**
     * @dev 批量查询仓单状态
     * @param receiptIds 仓单ID数组
     * @return exists 是否存在数组
     * @return frozen 是否冻结数组
     */
    function getReceiptsStatus(string[] memory receiptIds)
        external
        view
        returns (bool[] memory exists, bool[] memory frozen)
    {
        uint256 length = receiptIds.length;
        bool[] memory existsArray = new bool[](length);
        bool[] memory frozenArray = new bool[](length);

        for (uint256 i = 0; i < length; i++) {
            existsArray[i] = receiptCores[receiptIds[i]].exists;
            frozenArray[i] = receiptCores[receiptIds[i]].frozen;
        }

        return (existsArray, frozenArray);
    }

    /**
     * @dev 批量查询仓单货主
     * @param receiptIds 仓单ID数组
     * @return owners 货主地址数组
     */
    function getReceiptsOwners(string[] memory receiptIds)
        external
        view
        returns (address[] memory owners)
    {
        uint256 length = receiptIds.length;
        address[] memory ownersArray = new address[](length);

        for (uint256 i = 0; i < length; i++) {
            ownersArray[i] = receiptCores[receiptIds[i]].owner;
        }

        return ownersArray;
    }
}
