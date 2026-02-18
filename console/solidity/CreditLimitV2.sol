// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

/**
 * @title CreditLimitV2
 * @dev 信用额度管理智能合约 - V2版本
 *
 * 核心设计原则：
 * 1. 数据精简化与哈希化 - 仅核心数据上链，扩展数据哈希化存储
 * 2. 严格访问控制 - 细粒度权限控制，包括Java后端专用权限
 * 3. 规避栈溢出 - 函数参数<16个，使用struct组织数据
 * 4. 异常处理与状态一致性 - 完整的require验证，遵循Checks-Effects-Interactions模式
 * 5. 充分利用事件 - 所有关键操作触发事件，支持链下监听和同步
 *
 * @notice V2版本新增：Java后端访问控制、完整的事件系统、风险等级动态调整
 */
contract CreditLimitV2 {
    // ==================== 枚举定义 ====================

    /**
     * @dev 额度类型枚举
     */
    enum LimitType {
        Financing,   // 0 - 融资额度
        Guarantee,   // 1 - 担保额度
        Credit       // 2 - 赊账额度
    }

    /**
     * @dev 额度状态枚举
     */
    enum LimitStatus {
        Active,      // 0 - 生效中
        Frozen,      // 1 - 已冻结
        Expired,     // 2 - 已失效
        Cancelled    // 3 - 已取消
    }

    /**
     * @dev 风险等级枚举
     */
    enum RiskLevel {
        Low,         // 0 - 低风险
        Medium,      // 1 - 中风险
        High         // 2 - 高风险
    }

    /**
     * @dev 使用类型枚举
     */
    enum UsageType {
        Use,         // 0 - 使用额度
        Release,     // 1 - 释放额度
        Freeze,      // 2 - 冻结额度
        Unfreeze     // 3 - 解冻额度
    }

    /**
     * @dev 调整类型枚举
     */
    enum AdjustType {
        Increase,    // 0 - 增加额度
        Decrease,    // 1 - 减少额度
        Reset        // 2 - 重置额度
    }

    // ==================== 结构体定义 ====================

    /**
     * @dev 信用额度核心数据（上链存储）
     * @notice 仅核心业务逻辑必需的数据上链存储
     * @notice 时间字段移除，通过事件记录；exists标志移除，通过 enterprise != 0x0 判断
     * @notice 统计字段（overdueCount, badDebtCount）移除，通过事件记录
     */
    struct CreditLimitCore {
        // ========== 谁能做（地址权限） ==========
        address enterprise;             // 企业地址

        // ========== 给多少钱（金额） ==========
        uint256 totalLimit;             // 总额度
        uint256 usedLimit;              // 已使用额度
        uint256 frozenLimit;            // 冻结额度

        // ========== 什么时候做（时间） ==========
        uint256 expiryDate;             // 失效日期（业务逻辑需要用于判断有效期）

        // ========== 业务状态 ==========
        LimitType limitType;            // 额度类型
        LimitStatus status;             // 状态
        RiskLevel riskLevel;            // 风险等级
        uint256 warningThreshold;       // 预警阈值（百分比）
    }

    /**
     * @dev 信用额度元数据（哈希化存储）
     * @notice 扩展数据通过哈希上链，原文存储在链下
     */
    struct CreditLimitMeta {
        string limitId;
        bytes32 coreDataHash;           // 核心数据哈希（企业名称、审批原因等）
        bytes32 extendedDataHash;       // 扩展数据哈希（风险评估报告、信用评级等）
        uint256 createdAt;
        uint256 updatedAt;
    }

    /**
     * @dev 额度使用记录（精简版）
     * @notice 移除冗余字段，通过当前状态和 amount 可推算变更前后状态
     */
    struct UsageRecord {
        string businessId;              // 业务ID（关联业务即可）
        UsageType usageType;            // 使用类型
        uint256 amount;                 // 金额
        address operator;               // 操作人地址
        uint256 timestamp;              // 时间戳
        bytes32 dataHash;               // 数据哈希
    }

    /**
     * @dev 额度调整记录（精简版）
     * @notice 移除冗余字段，通过当前状态和事件可推算调整详情
     */
    struct AdjustRecord {
        AdjustType adjustType;          // 调整类型
        address approver;               // 审批人地址
        uint256 newLimit;              // 调整后额度
        string reason;                  // 原因
        uint256 timestamp;              // 时间戳
        bytes32 dataHash;               // 数据哈希
    }

    /**
     * @dev 信用额度完整信息结构（用于返回，避免栈溢出）
     * @notice 封装所有信用额度相关数据，减少函数返回参数数量
     * @notice 时间字段和统计字段已移除，通过事件记录获取
     */
    struct CreditLimitInfo {
        address enterprise;
        uint256 totalLimit;
        uint256 usedLimit;
        uint256 frozenLimit;
        uint256 availableLimit;
        LimitType limitType;
        LimitStatus status;
        RiskLevel riskLevel;
        uint256 warningThreshold;
        uint256 expiryDate;
    }

    // ==================== 状态变量 ====================

    /**
     * @dev 管理员地址
     */
    address public admin;

    /**
     * @dev Java后端地址
     * @notice Java后端服务地址，用于敏感操作（如创建信用额度）
     */
    address public javaBackend;

    /**
     * @dev 信用额度总数
     */
    uint256 public limitCount;

    /**
     * @dev 信用额度核心数据映射
     */
    mapping(string => CreditLimitCore) public creditLimitCores;

    /**
     * @dev 信用额度元数据映射
     */
    mapping(string => CreditLimitMeta) public creditLimitMetas;

    /**
     * @dev 额度使用记录映射
     */
    mapping(string => UsageRecord[]) public usageRecords;

    /**
     * @dev 额度调整记录映射
     */
    mapping(string => AdjustRecord[]) public adjustRecords;

    /**
     * @dev 企业的额度列表映射
     */
    mapping(address => string[]) public enterpriseLimits;

    // ==================== 事件定义 ====================

    /**
     * @dev 信用额度创建事件
     */
    event CreditLimitCreated(
        string indexed limitId,
        address indexed enterprise,
        LimitType indexed limitType,
        uint256 totalLimit,
        uint256 effectiveDate,
        uint256 expiryDate,
        bytes32 coreDataHash,
        bytes32 extendedDataHash
    );

    /**
     * @dev 信用额度使用事件（增强版）
     * @notice 包含变更前后状态，通过事件记录详细信息
     */
    event CreditLimitUsed(
        string indexed limitId,
        UsageType indexed usageType,
        address indexed operator,
        uint256 amount,
        uint256 beforeUsedLimit,
        uint256 afterUsedLimit,
        uint256 remainingLimit,
        uint256 timestamp
    );

    /**
     * @dev 信用额度调整事件
     */
    event CreditLimitAdjusted(
        string indexed limitId,
        AdjustType indexed adjustType,
        address indexed approver,
        uint256 beforeLimit,
        uint256 afterLimit,
        string reason
    );

    /**
     * @dev 信用额度冻结事件
     */
    event CreditLimitFrozen(
        string indexed limitId,
        address indexed admin,
        string reason
    );

    /**
     * @dev 信用额度解冻事件
     */
    event CreditLimitUnfrozen(
        string indexed limitId,
        address indexed admin,
        string reason
    );

    /**
     * @dev 信用额度失效事件
     */
    event CreditLimitExpired(
        string indexed limitId,
        uint256 timestamp
    );

    /**
     * @dev 风险等级更新事件
     */
    event RiskLevelUpdated(
        string indexed limitId,
        RiskLevel indexed oldLevel,
        RiskLevel indexed newLevel,
        string reason
    );

    /**
     * @dev 预警触发事件
     */
    event WarningTriggered(
        string indexed limitId,
        address indexed enterprise,
        uint256 usageRate,
        uint256 warningThreshold
    );

    /**
     * @dev 逾期次数增加事件
     */
    event OverdueCountIncremented(
        string indexed limitId,
        uint256 newCount
    );

    /**
     * @dev 坏账次数增加事件
     */
    event BadDebtCountIncremented(
        string indexed limitId,
        uint256 newCount
    );

    /**
     * @dev 管理员设置事件
     */
    event AdminSet(
        address indexed oldAdmin,
        address indexed newAdmin,
        uint256 timestamp
    );

    /**
     * @dev Java后端设置事件
     */
    event JavaBackendSet(
        address indexed oldBackend,
        address indexed newBackend,
        uint256 timestamp
    );

    // ==================== 修饰器 ====================

    /**
     * @dev 仅管理员可调用
     */
    modifier onlyAdmin() {
        require(msg.sender == admin, "Only admin can call this function");
        _;
    }

    /**
     * @dev 仅Java后端可调用
     * @notice 保护敏感操作，如创建信用额度
     */
    modifier onlyJavaBackend() {
        require(msg.sender == javaBackend, "Only Java backend can call this function");
        _;
    }

    /**
     * @dev 信用额度必须存在
     * @notice 通过 enterprise != address(0) 判断是否存在
     */
    modifier limitExists(string memory limitId) {
        require(creditLimitCores[limitId].enterprise != address(0), "Credit limit does not exist");
        _;
    }

    /**
     * @dev 信用额度必须处于生效状态
     */
    modifier onlyActiveLimit(string memory limitId) {
        require(creditLimitCores[limitId].status == LimitStatus.Active, "Credit limit is not active");
        _;
    }

    // ==================== 构造函数 ====================

    /**
     * @dev 构造函数
     * @param _admin 管理员地址
     */
    constructor(address _admin) {
        require(_admin != address(0), "Admin cannot be zero address");

        admin = _admin;
        javaBackend = _admin;  // 初始时，Java后端与管理员相同
        limitCount = 0;

        emit AdminSet(address(0), admin, block.timestamp);
        emit JavaBackendSet(address(0), javaBackend, block.timestamp);
    }

    // ==================== 管理员函数 ====================

    /**
     * @dev 设置新管理员
     * @param newAdmin 新管理员地址
     * @return success 操作是否成功
     */
    function setAdmin(address newAdmin)
        external
        onlyAdmin
        returns (bool success)
    {
        require(newAdmin != address(0), "New admin cannot be zero address");
        require(newAdmin != admin, "New admin is same as current");

        address oldAdmin = admin;
        admin = newAdmin;

        emit AdminSet(oldAdmin, newAdmin, block.timestamp);
        return true;
    }

    /**
     * @dev 设置Java后端地址
     * @param newBackend 新的Java后端地址
     * @return success 操作是否成功
     */
    function setJavaBackend(address newBackend)
        external
        onlyAdmin
        returns (bool success)
    {
        require(newBackend != address(0), "New backend cannot be zero address");
        require(newBackend != javaBackend, "New backend is same as current");

        address oldBackend = javaBackend;
        javaBackend = newBackend;

        emit JavaBackendSet(oldBackend, newBackend, block.timestamp);
        return true;
    }

    // ==================== 核心业务函数 ====================

    /**
     * @dev 创建信用额度
     * @notice 仅Java后端可调用
     * @param limitId 额度ID
     * @param enterprise 企业地址
     * @param limitType 额度类型
     * @param totalLimit 总额度
     * @param warningThreshold 预警阈值（百分比）
     * @param effectiveDate 生效日期（仅用于事件记录和验证）
     * @param expiryDate 失效日期
     * @param coreDataHash 核心数据哈希（企业名称、审批原因等）
     * @param extendedDataHash 扩展数据哈希（风险评估报告、信用评级等）
     * @return success 操作是否成功
     */
    function createCreditLimit(
        string memory limitId,
        address enterprise,
        LimitType limitType,
        uint256 totalLimit,
        uint256 warningThreshold,
        uint256 effectiveDate,
        uint256 expiryDate,
        bytes32 coreDataHash,
        bytes32 extendedDataHash
    ) external onlyJavaBackend returns (bool success) {
        // ========== Checks（检查阶段） ==========

        // 1. 验证额度ID
        require(bytes(limitId).length > 0, "Limit ID cannot be empty");
        require(creditLimitCores[limitId].enterprise == address(0), "Credit limit already exists");

        // 2. 验证地址
        require(enterprise != address(0), "Invalid enterprise address");

        // 3. 验证金额
        require(totalLimit > 0, "Total limit must be greater than 0");

        // 4. 验证预警阈值
        require(warningThreshold > 0 && warningThreshold <= 100, "Invalid warning threshold");

        // 5. 验证时间
        require(effectiveDate > 0, "Invalid effective date");
        require(expiryDate == 0 || expiryDate > effectiveDate, "Expiry date must be after effective date");

        // 6. 验证哈希
        require(coreDataHash != bytes32(0), "Core data hash cannot be zero");

        // ========== Effects（生效阶段） ==========

        // 创建核心数据
        CreditLimitCore storage core = creditLimitCores[limitId];
        core.enterprise = enterprise;
        core.totalLimit = totalLimit;
        core.usedLimit = 0;
        core.frozenLimit = 0;
        core.expiryDate = expiryDate;
        core.limitType = limitType;
        core.status = LimitStatus.Active;
        core.riskLevel = RiskLevel.Low;
        core.warningThreshold = warningThreshold;

        // 创建元数据
        creditLimitMetas[limitId] = CreditLimitMeta({
            limitId: limitId,
            coreDataHash: coreDataHash,
            extendedDataHash: extendedDataHash,
            createdAt: block.timestamp,
            updatedAt: block.timestamp
        });

        // 更新索引
        enterpriseLimits[enterprise].push(limitId);
        limitCount++;

        // ========== Events（事件阶段） ==========

        emit CreditLimitCreated(
            limitId,
            enterprise,
            limitType,
            totalLimit,
            effectiveDate,
            expiryDate,
            coreDataHash,
            extendedDataHash
        );

        return true;
    }

    /**
     * @dev 使用信用额度
     * @param limitId 额度ID
     * @param amount 使用金额
     * @param businessId 业务ID
     * @param dataHash 数据哈希
     * @return success 操作是否成功
     */
    function useCredit(
        string memory limitId,
        uint256 amount,
        string memory businessId,
        bytes32 dataHash
    ) external limitExists(limitId) onlyActiveLimit(limitId) returns (bool success) {
        // ========== Checks ==========
        CreditLimitCore storage core = creditLimitCores[limitId];
        uint256 availableLimit = core.totalLimit - core.usedLimit - core.frozenLimit;
        require(amount > 0, "Amount must be greater than 0");
        require(availableLimit >= amount, "Insufficient credit limit");

        // ========== Effects ==========
        uint256 beforeUsed = core.usedLimit;

        core.usedLimit += amount;

        uint256 afterUsed = core.usedLimit;
        uint256 remainingLimit = core.totalLimit - core.usedLimit - core.frozenLimit;

        // 创建使用记录（精简版）
        usageRecords[limitId].push(UsageRecord({
            businessId: businessId,
            usageType: UsageType.Use,
            amount: amount,
            operator: msg.sender,
            timestamp: block.timestamp,
            dataHash: dataHash
        }));

        creditLimitMetas[limitId].updatedAt = block.timestamp;

        // 检查是否触发预警
        uint256 usageRate = (core.usedLimit * 100) / core.totalLimit;
        if (usageRate >= core.warningThreshold) {
            emit WarningTriggered(limitId, core.enterprise, usageRate, core.warningThreshold);
        }

        // ========== Events ==========
        emit CreditLimitUsed(limitId, UsageType.Use, msg.sender, amount, beforeUsed, afterUsed, remainingLimit, block.timestamp);
        return true;
    }

    /**
     * @dev 释放信用额度
     * @param limitId 额度ID
     * @param amount 释放金额
     * @param businessId 业务ID
     * @param dataHash 数据哈希
     * @return success 操作是否成功
     */
    function releaseCredit(
        string memory limitId,
        uint256 amount,
        string memory businessId,
        bytes32 dataHash
    ) external limitExists(limitId) returns (bool success) {
        // ========== Checks ==========
        CreditLimitCore storage core = creditLimitCores[limitId];
        require(amount > 0, "Amount must be greater than 0");
        require(core.usedLimit >= amount, "Cannot release more than used");

        // ========== Effects ==========
        uint256 beforeUsed = core.usedLimit;

        core.usedLimit -= amount;

        uint256 afterUsed = core.usedLimit;
        uint256 remainingLimit = core.totalLimit - core.usedLimit - core.frozenLimit;

        // 创建使用记录（精简版）
        usageRecords[limitId].push(UsageRecord({
            businessId: businessId,
            usageType: UsageType.Release,
            amount: amount,
            operator: msg.sender,
            timestamp: block.timestamp,
            dataHash: dataHash
        }));

        creditLimitMetas[limitId].updatedAt = block.timestamp;

        // ========== Events ==========
        emit CreditLimitUsed(limitId, UsageType.Release, msg.sender, amount, beforeUsed, afterUsed, remainingLimit, block.timestamp);
        return true;
    }

    /**
     * @dev 调整信用额度
     * @param limitId 额度ID
     * @param adjustType 调整类型
     * @param newLimit 新额度
     * @param reason 调整原因
     * @param dataHash 数据哈希
     * @return success 操作是否成功
     */
    function adjustCreditLimit(
        string memory limitId,
        AdjustType adjustType,
        uint256 newLimit,
        string memory reason,
        bytes32 dataHash
    ) external limitExists(limitId) onlyAdmin returns (bool success) {
        // ========== Checks ==========
        CreditLimitCore storage core = creditLimitCores[limitId];
        require(newLimit > 0, "New limit must be greater than 0");

        // ========== Effects ==========
        uint256 beforeLimit = core.totalLimit;

        if (adjustType == AdjustType.Reset) {
            core.totalLimit = newLimit;
        } else if (adjustType == AdjustType.Increase) {
            require(newLimit > beforeLimit, "New limit must be greater than current limit");
            core.totalLimit = newLimit;
        } else if (adjustType == AdjustType.Decrease) {
            require(newLimit >= core.usedLimit, "New limit cannot be less than used limit");
            core.totalLimit = newLimit;
        }

        uint256 afterLimit = core.totalLimit;

        // 创建调整记录（精简版）
        adjustRecords[limitId].push(AdjustRecord({
            adjustType: adjustType,
            approver: msg.sender,
            newLimit: afterLimit,
            reason: reason,
            timestamp: block.timestamp,
            dataHash: dataHash
        }));

        creditLimitMetas[limitId].updatedAt = block.timestamp;

        // ========== Events ==========
        emit CreditLimitAdjusted(limitId, adjustType, msg.sender, beforeLimit, afterLimit, reason);
        return true;
    }

    /**
     * @dev 冻结信用额度
     * @param limitId 额度ID
     * @param amount 冻结金额
     * @param reason 冻结原因
     * @return success 操作是否成功
     */
    function freezeCreditLimit(
        string memory limitId,
        uint256 amount,
        string memory reason
    ) external limitExists(limitId) onlyAdmin returns (bool success) {
        // ========== Checks ==========
        CreditLimitCore storage core = creditLimitCores[limitId];
        require(core.status == LimitStatus.Active, "Credit limit is not active");
        uint256 availableLimit = core.totalLimit - core.usedLimit - core.frozenLimit;
        require(amount > 0 && amount <= availableLimit, "Invalid freeze amount");

        // ========== Effects ==========
        core.frozenLimit += amount;

        creditLimitMetas[limitId].updatedAt = block.timestamp;

        // ========== Events ==========
        emit CreditLimitFrozen(limitId, msg.sender, reason);
        return true;
    }

    /**
     * @dev 解冻信用额度
     * @param limitId 额度ID
     * @param amount 解冻金额
     * @param reason 解冻原因
     * @return success 操作是否成功
     */
    function unfreezeCreditLimit(
        string memory limitId,
        uint256 amount,
        string memory reason
    ) external limitExists(limitId) onlyAdmin returns (bool success) {
        // ========== Checks ==========
        CreditLimitCore storage core = creditLimitCores[limitId];
        require(amount > 0 && amount <= core.frozenLimit, "Invalid unfreeze amount");

        // ========== Effects ==========
        core.frozenLimit -= amount;

        creditLimitMetas[limitId].updatedAt = block.timestamp;

        // ========== Events ==========
        emit CreditLimitUnfrozen(limitId, msg.sender, reason);
        return true;
    }

    /**
     * @dev 冻结整个信用额度
     * @param limitId 额度ID
     * @param reason 冻结原因
     * @return success 操作是否成功
     */
    function freezeEntireLimit(string memory limitId, string memory reason)
        external
        limitExists(limitId)
        onlyAdmin
        returns (bool success)
    {
        // ========== Checks ==========
        CreditLimitCore storage core = creditLimitCores[limitId];
        require(core.status == LimitStatus.Active, "Credit limit is not active");

        // ========== Effects ==========
        core.status = LimitStatus.Frozen;

        creditLimitMetas[limitId].updatedAt = block.timestamp;

        // ========== Events ==========
        emit CreditLimitFrozen(limitId, msg.sender, reason);
        return true;
    }

    /**
     * @dev 解冻整个信用额度
     * @param limitId 额度ID
     * @param reason 解冻原因
     * @return success 操作是否成功
     */
    function unfreezeEntireLimit(string memory limitId, string memory reason)
        external
        limitExists(limitId)
        onlyAdmin
        returns (bool success)
    {
        // ========== Checks ==========
        CreditLimitCore storage core = creditLimitCores[limitId];
        require(core.status == LimitStatus.Frozen, "Credit limit is not frozen");

        // ========== Effects ==========
        // 检查是否到期
        if (core.expiryDate > 0 && block.timestamp >= core.expiryDate) {
            core.status = LimitStatus.Expired;
            emit CreditLimitExpired(limitId, block.timestamp);
        } else {
            core.status = LimitStatus.Active;
        }

        creditLimitMetas[limitId].updatedAt = block.timestamp;

        // ========== Events ==========
        emit CreditLimitUnfrozen(limitId, msg.sender, reason);
        return true;
    }

    /**
     * @dev 更新风险等级
     * @param limitId 额度ID
     * @param newRiskLevel 新风险等级
     * @param reason 更新原因
     * @return success 操作是否成功
     */
    function updateRiskLevel(
        string memory limitId,
        RiskLevel newRiskLevel,
        string memory reason
    ) external limitExists(limitId) onlyAdmin returns (bool success) {
        // ========== Checks ==========
        CreditLimitCore storage core = creditLimitCores[limitId];

        // ========== Effects ==========
        RiskLevel oldLevel = core.riskLevel;
        core.riskLevel = newRiskLevel;

        creditLimitMetas[limitId].updatedAt = block.timestamp;

        // ========== Events ==========
        emit RiskLevelUpdated(limitId, oldLevel, newRiskLevel, reason);
        return true;
    }

    /**
     * @dev 增加逾期次数
     * @notice 已移除存储计数，仅触发事件供后端统计
     * @param limitId 额度ID
     * @return success 操作是否成功
     */
    function incrementOverdueCount(string memory limitId)
        external
        limitExists(limitId)
        onlyAdmin
        returns (bool success)
    {
        creditLimitMetas[limitId].updatedAt = block.timestamp;

        // ========== Events ==========
        emit OverdueCountIncremented(limitId, 1);
        return true;
    }

    /**
     * @dev 增加坏账次数
     * @notice 已移除存储计数，仅触发事件供后端统计
     * @param limitId 额度ID
     * @return success 操作是否成功
     */
    function incrementBadDebtCount(string memory limitId)
        external
        limitExists(limitId)
        onlyAdmin
        returns (bool success)
    {
        creditLimitMetas[limitId].updatedAt = block.timestamp;

        // ========== Events ==========
        emit BadDebtCountIncremented(limitId, 1);
        return true;
    }

    // ==================== 查询函数 ====================

    /**
     * @dev 获取信用额度完整信息（优化版 - 使用结构体避免栈溢出）
     * @param limitId 额度ID
     * @return info 信用额度完整信息结构体
     */
    function getCreditLimit(string memory limitId)
        external
        view
        limitExists(limitId)
        returns (CreditLimitInfo memory info)
    {
        CreditLimitCore memory core = creditLimitCores[limitId];
        return CreditLimitInfo({
            enterprise: core.enterprise,
            totalLimit: core.totalLimit,
            usedLimit: core.usedLimit,
            frozenLimit: core.frozenLimit,
            availableLimit: core.totalLimit - core.usedLimit - core.frozenLimit,
            limitType: core.limitType,
            status: core.status,
            riskLevel: core.riskLevel,
            warningThreshold: core.warningThreshold,
            expiryDate: core.expiryDate
        });
    }

    /**
     * @dev 获取额度元数据
     * @param limitId 额度ID
     * @return coreDataHash 核心数据哈希
     * @return extendedDataHash 扩展数据哈希
     * @return createdAt 创建时间
     * @return updatedAt 更新时间
     */
    function getCreditLimitMeta(string memory limitId)
        external
        view
        limitExists(limitId)
        returns (
            bytes32 coreDataHash,
            bytes32 extendedDataHash,
            uint256 createdAt,
            uint256 updatedAt
        )
    {
        CreditLimitMeta memory meta = creditLimitMetas[limitId];
        return (
            meta.coreDataHash,
            meta.extendedDataHash,
            meta.createdAt,
            meta.updatedAt
        );
    }

    /**
     * @dev 获取企业的所有额度ID
     * @param enterprise 企业地址
     * @return limitIds 额度ID数组
     */
    function getEnterpriseLimits(address enterprise)
        external
        view
        returns (string[] memory limitIds)
    {
        return enterpriseLimits[enterprise];
    }

    /**
     * @dev 获取额度使用记录数量
     * @param limitId 额度ID
     * @return count 记录数量
     */
    function getUsageRecordCount(string memory limitId)
        external
        view
        returns (uint256 count)
    {
        return usageRecords[limitId].length;
    }

    /**
     * @dev 获取额度调整记录数量
     * @param limitId 额度ID
     * @return count 记录数量
     */
    function getAdjustRecordCount(string memory limitId)
        external
        view
        returns (uint256 count)
    {
        return adjustRecords[limitId].length;
    }

    /**
     * @dev 获取信用额度总数
     * @return count 额度总数
     */
    function getCreditLimitCount()
        external
        view
        returns (uint256 count)
    {
        return limitCount;
    }

    /**
     * @dev 检查信用额度是否存在
     * @param limitId 额度ID
     * @return exists 是否存在（通过 enterprise != address(0) 判断）
     */
    function creditLimitExists(string memory limitId)
        external
        view
        returns (bool exists)
    {
        return creditLimitCores[limitId].enterprise != address(0);
    }

    /**
     * @dev 检查信用额度是否生效
     * @param limitId 额度ID
     * @return isActive 是否生效
     */
    function isCreditLimitActive(string memory limitId)
        external
        view
        returns (bool isActive)
    {
        return creditLimitCores[limitId].status == LimitStatus.Active;
    }

    /**
     * @dev 批量查询信用额度状态
     * @param limitIds 额度ID数组
     * @return statuses 状态数组
     */
    function getCreditLimitsStatus(string[] calldata limitIds)
        external
        view
        returns (LimitStatus[] memory statuses)
    {
        statuses = new LimitStatus[](limitIds.length);
        for (uint256 i = 0; i < limitIds.length; i++) {
            if (creditLimitCores[limitIds[i]].enterprise != address(0)) {
                statuses[i] = creditLimitCores[limitIds[i]].status;
            } else {
                statuses[i] = LimitStatus.Cancelled;  // 不存在的视为已取消
            }
        }
        return statuses;
    }
}