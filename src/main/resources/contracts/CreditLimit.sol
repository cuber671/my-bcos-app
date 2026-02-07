// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

/**
 * @title CreditLimit
 * @dev 信用额度管理智能合约
 */
contract CreditLimit {
    // 额度类型
    enum LimitType {
        Financing,   // 融资额度
        Guarantee,   // 担保额度
        Credit       // 赊账额度
    }

    // 额度状态
    enum LimitStatus {
        Active,      // 生效中
        Frozen,      // 已冻结
        Expired,     // 已失效
        Cancelled    // 已取消
    }

    // 风险等级
    enum RiskLevel {
        Low,         // 低风险
        Medium,      // 中风险
        High         // 高风险
    }

    // 使用类型
    enum UsageType {
        Use,         // 使用
        Release,     // 释放
        Freeze,      // 冻结
        Unfreeze     // 解冻
    }

    // 调整类型
    enum AdjustType {
        Increase,    // 增加
        Decrease,    // 减少
        Reset        // 重置
    }

    // 信用额度核心数据
    struct CreditLimitData {
        address enterprise;              // 企业地址
        string enterpriseName;           // 企业名称
        LimitType limitType;             // 额度类型
        uint256 totalLimit;             // 总额度（分）
        uint256 usedLimit;              // 已使用额度（分）
        uint256 frozenLimit;            // 冻结额度（分）
        uint256 warningThreshold;       // 预警阈值（百分比）
        uint256 effectiveDate;          // 生效日期
        uint256 expiryDate;             // 失效日期
        LimitStatus status;             // 状态
        address approver;               // 审批人地址
        uint256 overdueCount;           // 逾期次数
        uint256 badDebtCount;           // 坏账次数
        RiskLevel riskLevel;            // 风险等级
        bool exists;                    // 是否存在
    }

    // 额度使用记录
    struct UsageRecord {
        string limitId;                 // 额度ID
        UsageType usageType;            // 使用类型
        string businessType;            // 业务类型
        string businessId;              // 业务ID
        uint256 amount;                 // 金额（分）
        uint256 beforeAvailable;        // 使用前可用额度
        uint256 afterAvailable;         // 使用后可用额度
        uint256 beforeUsed;             // 使用前已使用额度
        uint256 afterUsed;              // 使用后已使用额度
        address operator;               // 操作人地址
        uint256 timestamp;              // 时间戳
        bytes32 dataHash;               // 数据哈希
    }

    // 额度调整记录
    struct AdjustRecord {
        string limitId;                 // 额度ID
        AdjustType adjustType;          // 调整类型
        uint256 beforeLimit;            // 调整前额度
        uint256 afterLimit;             // 调整后额度
        uint256 adjustAmount;           // 调整金额
        address requester;              // 申请人地址
        address approver;               // 审批人地址
        uint256 requestDate;            // 申请日期
        uint256 approveDate;            // 审批日期
        string reason;                  // 原因
        bytes32 dataHash;               // 数据哈希
    }

    // 状态变量
    address public admin;
    uint256 public limitCount;

    // 映射
    mapping(string => CreditLimitData) public creditLimits;
    mapping(string => UsageRecord[]) public usageRecords;
    mapping(string => AdjustRecord[]) public adjustRecords;
    mapping(address => string[]) public enterpriseLimits;

    // 事件
    event CreditLimitCreated(
        string indexed limitId,
        address indexed enterprise,
        LimitType limitType,
        uint256 totalLimit,
        bytes32 dataHash
    );

    event CreditLimitUsed(
        string indexed limitId,
        UsageType usageType,
        uint256 amount,
        uint256 remainingLimit,
        bytes32 dataHash
    );

    event CreditLimitAdjusted(
        string indexed limitId,
        AdjustType adjustType,
        uint256 beforeLimit,
        uint256 afterLimit,
        bytes32 dataHash
    );

    event CreditLimitFrozen(
        string indexed limitId,
        address indexed operator,
        string reason
    );

    event CreditLimitUnfrozen(
        string indexed limitId,
        address indexed operator,
        string reason
    );

    event CreditLimitExpired(
        string indexed limitId,
        uint256 timestamp
    );

    event RiskLevelUpdated(
        string indexed limitId,
        RiskLevel oldLevel,
        RiskLevel newLevel,
        string reason
    );

    // 修饰器
    modifier onlyAdmin() {
        require(msg.sender == admin, "Only admin can call this function");
        _;
    }

    modifier onlyExistingLimit(string memory limitId) {
        require(creditLimits[limitId].exists, "Credit limit does not exist");
        _;
    }

    /**
     * @dev 构造函数
     */
    constructor() {
        admin = msg.sender;
        limitCount = 0;
    }

    /**
     * @dev 创建信用额度
     * @param limitId 额度ID
     * @param enterprise 企业地址
     * @param enterpriseName 企业名称
     * @param limitType 额度类型
     * @param totalLimit 总额度（分）
     * @param warningThreshold 预警阈值
     * @param effectiveDate 生效日期
     * @param expiryDate 失效日期
     * @param dataHash 数据哈希
     */
    function createCreditLimit(
        string memory limitId,
        address enterprise,
        string memory enterpriseName,
        LimitType limitType,
        uint256 totalLimit,
        uint256 warningThreshold,
        uint256 effectiveDate,
        uint256 expiryDate,
        bytes32 dataHash
    ) public onlyAdmin {
        require(!creditLimits[limitId].exists, "Credit limit already exists");
        require(totalLimit > 0, "Total limit must be greater than 0");
        require(warningThreshold > 0 && warningThreshold <= 100, "Invalid warning threshold");
        require(effectiveDate > 0, "Effective date must be set");

        // 创建额度数据
        CreditLimitData storage limitData = creditLimits[limitId];
        limitData.enterprise = enterprise;
        limitData.enterpriseName = enterpriseName;
        limitData.limitType = limitType;
        limitData.totalLimit = totalLimit;
        limitData.usedLimit = 0;
        limitData.frozenLimit = 0;
        limitData.warningThreshold = warningThreshold;
        limitData.effectiveDate = effectiveDate;
        limitData.expiryDate = expiryDate;
        limitData.status = LimitStatus.Active;
        limitData.approver = admin;
        limitData.overdueCount = 0;
        limitData.badDebtCount = 0;
        limitData.riskLevel = RiskLevel.Low;
        limitData.exists = true;

        // 添加到企业的额度列表
        enterpriseLimits[enterprise].push(limitId);

        // 更新计数
        limitCount++;

        // 触发事件
        emit CreditLimitCreated(limitId, enterprise, limitType, totalLimit, dataHash);
    }

    /**
     * @dev 使用额度
     * @param limitId 额度ID
     * @param amount 使用金额（分）
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @param dataHash 数据哈希
     */
    function useCredit(
        string memory limitId,
        uint256 amount,
        string memory businessType,
        string memory businessId,
        bytes32 dataHash
    ) public onlyExistingLimit(limitId) {
        CreditLimitData storage limitData = creditLimits[limitId];

        require(limitData.status == LimitStatus.Active, "Credit limit is not active");
        uint256 availableLimit = limitData.totalLimit - limitData.usedLimit - limitData.frozenLimit;
        require(availableLimit >= amount, "Insufficient credit limit");

        // 记录使用前状态
        uint256 beforeAvailable = availableLimit;
        uint256 beforeUsed = limitData.usedLimit;

        // 更新使用额度
        limitData.usedLimit += amount;

        // 记录使用前状态
        uint256 afterAvailable = limitData.totalLimit - limitData.usedLimit - limitData.frozenLimit;
        uint256 afterUsed = limitData.usedLimit;

        // 创建使用记录
        UsageRecord memory record = UsageRecord({
            limitId: limitId,
            usageType: UsageType.Use,
            businessType: businessType,
            businessId: businessId,
            amount: amount,
            beforeAvailable: beforeAvailable,
            afterAvailable: afterAvailable,
            beforeUsed: beforeUsed,
            afterUsed: afterUsed,
            operator: msg.sender,
            timestamp: block.timestamp,
            dataHash: dataHash
        };

        usageRecords[limitId].push(record);

        // 触发事件
        emit CreditLimitUsed(limitId, UsageType.Use, amount, afterAvailable, dataHash);
    }

    /**
     * @dev 释放额度
     * @param limitId 额度ID
     * @param amount 释放金额（分）
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @param dataHash 数据哈希
     */
    function releaseCredit(
        string memory limitId,
        uint256 amount,
        string memory businessType,
        string memory businessId,
        bytes32 dataHash
    ) public onlyExistingLimit(limitId) {
        CreditLimitData storage limitData = creditLimits[limitId];

        require(limitData.usedLimit >= amount, "Cannot release more than used");

        // 记录释放前状态
        uint256 beforeAvailable = limitData.totalLimit - limitData.usedLimit - limitData.frozenLimit;
        uint256 beforeUsed = limitData.usedLimit;

        // 更新使用额度
        limitData.usedLimit -= amount;

        // 记录释放后状态
        uint256 afterAvailable = limitData.totalLimit - limitData.usedLimit - limitData.frozenLimit;
        uint256 afterUsed = limitData.usedLimit;

        // 创建使用记录
        UsageRecord memory record = UsageRecord({
            limitId: limitId,
            usageType: UsageType.Release,
            businessType: businessType,
            businessId: businessId,
            amount: amount,
            beforeAvailable: beforeAvailable,
            afterAvailable: afterAvailable,
            beforeUsed: beforeUsed,
            afterUsed: afterUsed,
            operator: msg.sender,
            timestamp: block.timestamp,
            dataHash: dataHash
        });

        usageRecords[limitId].push(record);

        // 触发事件
        emit CreditLimitUsed(limitId, UsageType.Release, amount, afterAvailable, dataHash);
    }

    /**
     * @dev 调整信用额度
     * @param limitId 额度ID
     * @param adjustType 调整类型
     * @param newLimit 新额度（分）
     * @param reason 原因
     * @param dataHash 数据哈希
     */
    function adjustCreditLimit(
        string memory limitId,
        AdjustType adjustType,
        uint256 newLimit,
        string memory reason,
        bytes32 dataHash
    ) public onlyAdmin onlyExistingLimit(limitId) {
        CreditLimitData storage limitData = creditLimits[limitId];

        require(newLimit > 0, "New limit must be greater than 0");

        // 记录调整前额度
        uint256 beforeLimit = limitData.totalLimit;

        // 根据调整类型更新额度
        if (adjustType == AdjustType.Reset) {
            limitData.totalLimit = newLimit;
        } else if (adjustType == AdjustType.Increase) {
            require(newLimit > beforeLimit, "New limit must be greater than current limit");
            limitData.totalLimit = newLimit;
        } else if (adjustType == AdjustType.Decrease) {
            require(newLimit >= limitData.usedLimit, "New limit cannot be less than used limit");
            limitData.totalLimit = newLimit;
        }

        // 记录调整后额度
        uint256 afterLimit = limitData.totalLimit;
        uint256 adjustAmount = afterLimit > beforeLimit ?
            afterLimit - beforeLimit : beforeLimit - afterLimit;

        // 更新审批人
        limitData.approver = msg.sender;

        // 创建调整记录
        AdjustRecord memory record = AdjustRecord({
            limitId: limitId,
            adjustType: adjustType,
            beforeLimit: beforeLimit,
            afterLimit: afterLimit,
            adjustAmount: adjustAmount,
            requester: msg.sender,
            approver: msg.sender,
            requestDate: block.timestamp,
            approveDate: block.timestamp,
            reason: reason,
            dataHash: dataHash
        };

        adjustRecords[limitId].push(record);

        // 触发事件
        emit CreditLimitAdjusted(limitId, adjustType, beforeLimit, afterLimit, dataHash);
    }

    /**
     * @dev 冻结信用额度
     * @param limitId 额度ID
     * @param reason 冻结原因
     */
    function freezeCreditLimit(string memory limitId, string memory reason)
        public onlyAdmin onlyExistingLimit(limitId) {
        CreditLimitData storage limitData = creditLimits[limitId];

        require(limitData.status == LimitStatus.Active, "Credit limit is not active");

        limitData.status = LimitStatus.Frozen;

        // 触发事件
        emit CreditLimitFrozen(limitId, msg.sender, reason);
    }

    /**
     * @dev 解冻信用额度
     * @param limitId 额度ID
     * @param reason 解冻原因
     */
    function unfreezeCreditLimit(string memory limitId, string memory reason)
        public onlyAdmin onlyExistingLimit(limitId) {
        CreditLimitData storage limitData = creditLimits[limitId];

        require(limitData.status == LimitStatus.Frozen, "Credit limit is not frozen");

        // 检查是否到期
        if (limitData.expiryDate > 0 && block.timestamp >= limitData.expiryDate) {
            limitData.status = LimitStatus.Expired;
            emit CreditLimitExpired(limitId, block.timestamp);
        } else {
            limitData.status = LimitStatus.Active;
        }

        // 触发事件
        emit CreditLimitUnfrozen(limitId, msg.sender, reason);
    }

    /**
     * @dev 更新风险等级
     * @param limitId 额度ID
     * @param newRiskLevel 新风险等级
     * @param reason 原因
     */
    function updateRiskLevel(
        string memory limitId,
        RiskLevel newRiskLevel,
        string memory reason
    ) public onlyAdmin onlyExistingLimit(limitId) {
        CreditLimitData storage limitData = creditLimits[limitId];

        RiskLevel oldLevel = limitData.riskLevel;
        limitData.riskLevel = newRiskLevel;

        // 触发事件
        emit RiskLevelUpdated(limitId, oldLevel, newRiskLevel, reason);
    }

    /**
     * @dev 增加逾期次数
     * @param limitId 额度ID
     */
    function incrementOverdueCount(string memory limitId)
        public onlyAdmin onlyExistingLimit(limitId) {
        creditLimits[limitId].overdueCount++;
    }

    /**
     * @dev 增加坏账次数
     * @param limitId 额度ID
     */
    function incrementBadDebtCount(string memory limitId)
        public onlyAdmin onlyExistingLimit(limitId) {
        creditLimits[limitId].badDebtCount++;
    }

    /**
     * @dev 查询信用额度详情
     * @param limitId 额度ID
     */
    function getCreditLimit(string memory limitId)
        public view onlyExistingLimit(limitId) returns (
            address enterprise,
            string memory enterpriseName,
            LimitType limitType,
            uint256 totalLimit,
            uint256 usedLimit,
            uint256 frozenLimit,
            uint256 availableLimit,
            LimitStatus status,
            RiskLevel riskLevel,
            uint256 overdueCount,
            uint256 badDebtCount
        ) {
        CreditLimitData memory limitData = creditLimits[limitId];
        enterprise = limitData.enterprise;
        enterpriseName = limitData.enterpriseName;
        limitType = limitData.limitType;
        totalLimit = limitData.totalLimit;
        usedLimit = limitData.usedLimit;
        frozenLimit = limitData.frozenLimit;
        availableLimit = totalLimit - usedLimit - frozenLimit;
        status = limitData.status;
        riskLevel = limitData.riskLevel;
        overdueCount = limitData.overdueCount;
        badDebtCount = limitData.badDebtCount;
    }

    /**
     * @dev 查询企业的所有额度ID
     * @param enterprise 企业地址
     */
    function getEnterpriseLimits(address enterprise)
        public view returns (string[] memory) {
        return enterpriseLimits[enterprise];
    }

    /**
     * @dev 查询额度使用记录数量
     * @param limitId 额度ID
     */
    function getUsageRecordCount(string memory limitId)
        public view returns (uint256) {
        return usageRecords[limitId].length;
    }

    /**
     * @dev 查询额度调整记录数量
     * @param limitId 额度ID
     */
    function getAdjustRecordCount(string memory limitId)
        public view returns (uint256) {
        return adjustRecords[limitId].length;
    }

    /**
     * @dev 转移管理员权限
     * @param newAdmin 新管理员地址
     */
    function transferAdmin(address newAdmin) public onlyAdmin {
        require(newAdmin != address(0), "New admin cannot be zero address");
        admin = newAdmin;
    }
}
