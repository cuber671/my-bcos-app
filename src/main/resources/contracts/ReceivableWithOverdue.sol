// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

/**
 * @title ReceivableWithOverdue
 * @dev 应收账款管理智能合约 - 支持逾期管理功能
 *      在 Receivable.sol 基础上扩展，添加催收、罚息、坏账管理功能
 */
contract ReceivableWithOverdue {
    // ==================== 原合约的数据结构 ====================

    // 应收账款状态
    enum ReceivableStatus {
        Created,      // 已创建
        Confirmed,    // 已确认
        Financed,     // 已融资
        Repaid,       // 已还款
        Defaulted,    // 已违约
        Cancelled     // 已取消
    }

    // 逾期等级
    enum OverdueLevel {
        Mild,        // 轻度逾期 (1-30天)
        Moderate,    // 中度逾期 (31-90天)
        Severe,      // 重度逾期 (91-179天)
        BadDebt      // 坏账 (180天+)
    }

    // 催收类型
    enum RemindType {
        Email,       // 邮件
        Sms,        // 短信
        Phone,      // 电话
        Letter,     // 函件
        Legal       // 法律
    }

    // 罚息类型
    enum PenaltyType {
        Auto,       // 自动计算
        Manual      // 手动计算
    }

    // 坏账类型
    enum BadDebtType {
        Overdue180,  // 逾期180天+
        Bankruptcy,  // 破产
        Dispute,     // 争议
        Other       // 其他
    }

    // 回收状态
    enum RecoveryStatus {
        NotRecovered,       // 未回收
        PartialRecovered,   // 部分回收
        FullRecovered       // 全额回收
    }

    // 核心资产数据
    struct ReceivableCore {
        address supplier;              // 供应商地址
        address coreEnterprise;        // 核心企业地址
        address currentHolder;         // 当前持有人
        address financier;             // 资金方
        uint256 amount;                // 金额（分）
        uint256 issueDate;             // 出票日期
        uint256 dueDate;               // 到期日期
        ReceivableStatus status;       // 状态
        uint256 financeAmount;         // 融资金额
        uint256 financeRate;          // 融资利率（基点）
        uint256 financeDate;          // 融资日期
        bool exists;                  // 是否存在
    }

    // 元数据
    struct ReceivableMeta {
        string receivableId;          // 应收账款ID
        string currency;              // 币种符号
        bytes32 dataHash;             // 链下详细数据的哈希值
        uint256 createdAt;            // 创建时间
        uint256 updatedAt;            // 更新时间
    }

    // 逾期信息
    struct OverdueInfo {
        OverdueLevel level;           // 逾期等级
        uint256 overdueDays;          // 逾期天数
        uint256 penaltyAmount;        // 累计罚息金额（分）
        uint256 lastRemindDate;       // 最后催收日期
        uint256 remindCount;          // 催收次数
        uint256 badDebtDate;          // 坏账认定日期
        string badDebtReason;         // 坏账原因
        bool isOverdue;               // 是否逾期
    }

    // 催收记录
    struct RemindRecord {
        string receivableId;          // 应收账款ID
        RemindType remindType;        // 催收类型
        uint256 remindDate;           // 催收日期
        address operator;             // 操作人地址
        string remindContent;         // 催收内容
        bytes32 dataHash;             // 数据哈希
    }

    // 罚息记录
    struct PenaltyRecord {
        string receivableId;          // 应收账款ID
        PenaltyType penaltyType;      // 罚息类型
        uint256 principalAmount;      // 本金金额（分）
        uint256 overdueDays;          // 逾期天数
        uint256 dailyRate;           // 日利率（×10000，如0.05%存储为5）
        uint256 penaltyAmount;       // 罚息金额（分）
        uint256 totalPenaltyAmount;  // 累计罚息（分）
        uint256 calculateStartDate;  // 计算起始日期
        uint256 calculateEndDate;    // 计算结束日期
        uint256 calculateDate;       // 计算日期
        bytes32 dataHash;            // 数据哈希
    }

    // 坏账记录
    struct BadDebtRecord {
        string receivableId;          // 应收账款ID
        BadDebtType badDebtType;      // 坏账类型
        uint256 principalAmount;      // 本金金额（分）
        uint256 overdueDays;          // 逾期天数
        uint256 totalPenaltyAmount;  // 累计罚息（分）
        uint256 totalLossAmount;     // 总损失金额（分）
        string badDebtReason;        // 坏账原因
        RecoveryStatus recoveryStatus; // 回收状态
        uint256 recoveredAmount;     // 已回收金额（分）
        uint256 recordDate;          // 记录日期
        bytes32 dataHash;            // 数据哈希
    }

    // 状态变量
    address public admin;
    uint256 public receivableCount;
    uint256 public remindRecordCount;
    uint256 public penaltyRecordCount;
    uint256 public badDebtRecordCount;

    // 映射
    mapping(string => ReceivableCore) public receivableCores;
    mapping(string => ReceivableMeta) public receivableMetas;
    mapping(string => OverdueInfo) public overdueInfos;
    mapping(string => RemindRecord[]) public remindRecords;
    mapping(string => PenaltyRecord[]) public penaltyRecords;
    mapping(string => BadDebtRecord) public badDebtRecords;

    // ==================== 事件 ====================

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

    event ReceivableRepaid(
        string indexed receivableId,
        uint256 amount,
        uint256 timestamp
    );

    event OverdueStatusUpdated(
        string indexed receivableId,
        OverdueLevel level,
        uint256 overdueDays
    );

    event RemindRecordCreated(
        string indexed receivableId,
        RemindType remindType,
        address indexed operator,
        uint256 remindDate
    );

    event PenaltyRecordCreated(
        string indexed receivableId,
        PenaltyType penaltyType,
        uint256 penaltyAmount,
        uint256 totalPenaltyAmount
    );

    event BadDebtRecordCreated(
        string indexed receivableId,
        BadDebtType badDebtType,
        uint256 totalLossAmount
    );

    // ==================== 修饰器 ====================

    modifier onlyAdmin() {
        require(msg.sender == admin, "Only admin can call this function");
        _;
    }

    modifier onlyExistingReceivable(string memory receivableId) {
        require(receivableCores[receivableId].exists, "Receivable does not exist");
        _;
    }

    // ==================== 构造函数 ====================

    constructor() {
        admin = msg.sender;
        receivableCount = 0;
        remindRecordCount = 0;
        penaltyRecordCount = 0;
        badDebtRecordCount = 0;
    }

    // ==================== 原有功能 ====================

    /**
     * @dev 创建应收账款
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

        // 保存核心数据
        receivableCores[receivableId] = ReceivableCore({
            supplier: msg.sender,
            coreEnterprise: coreEnterprise,
            currentHolder: msg.sender,
            financier: address(0),
            amount: amount,
            issueDate: issueDate,
            dueDate: dueDate,
            status: ReceivableStatus.Created,
            financeAmount: 0,
            financeRate: 0,
            financeDate: 0,
            exists: true
        });

        // 保存元数据
        receivableMetas[receivableId] = ReceivableMeta({
            receivableId: receivableId,
            currency: "CNY",
            dataHash: dataHash,
            createdAt: block.timestamp,
            updatedAt: block.timestamp
        });

        // 初始化逾期信息（未逾期）
        overdueInfos[receivableId] = OverdueInfo({
            level: OverdueLevel.Mild,
            overdueDays: 0,
            penaltyAmount: 0,
            lastRemindDate: 0,
            remindCount: 0,
            badDebtDate: 0,
            badDebtReason: "",
            isOverdue: false
        });

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
        returns (bool)
    {
        ReceivableCore storage core = receivableCores[receivableId];
        require(core.status == ReceivableStatus.Created, "Invalid status for confirmation");
        require(core.coreEnterprise == msg.sender, "Only core enterprise can confirm");

        core.status = ReceivableStatus.Confirmed;
        receivableMetas[receivableId].updatedAt = block.timestamp;

        emit ReceivableConfirmed(receivableId, msg.sender);
        return true;
    }

    /**
     * @dev 应收账款融资
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

        emit ReceivableFinanced(receivableId, financier, financeAmount, financeRate);
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

        emit ReceivableRepaid(receivableId, amount, block.timestamp);
        return true;
    }

    // ==================== 逾期管理功能 ====================

    /**
     * @dev 更新逾期状态
     * @param receivableId 应收账款ID
     * @param level 逾期等级
     * @param overdueDays 逾期天数
     */
    function updateOverdueStatus(
        string memory receivableId,
        OverdueLevel level,
        uint256 overdueDays
    ) public onlyExistingReceivable(receivableId) returns (bool) {
        // 验证权限：供应商、资金方或管理员可以更新
        ReceivableCore memory core = receivableCores[receivableId];
        require(
            msg.sender == core.supplier ||
            msg.sender == core.financier ||
            msg.sender == admin,
            "Not authorized to update overdue status"
        );

        // 更新逾期信息
        OverdueInfo storage info = overdueInfos[receivableId];
        info.level = level;
        info.overdueDays = overdueDays;

        if (overdueDays > 0) {
            info.isOverdue = true;
        }

        receivableMetas[receivableId].updatedAt = block.timestamp;

        emit OverdueStatusUpdated(receivableId, level, overdueDays);
        return true;
    }

    /**
     * @dev 记录催收
     * @param receivableId 应收账款ID
     * @param remindType 催收类型
     * @param operator 操作人地址
     * @param remindDate 催收日期
     * @param remindContent 催收内容
     * @param dataHash 数据哈希
     */
    function recordRemind(
        string memory receivableId,
        RemindType remindType,
        address operator,
        uint256 remindDate,
        string memory remindContent,
        bytes32 dataHash
    ) public onlyExistingReceivable(receivableId) returns (bool) {
        // 创建催收记录
        remindRecords[receivableId].push(RemindRecord({
            receivableId: receivableId,
            remindType: remindType,
            remindDate: remindDate,
            operator: operator,
            remindContent: remindContent,
            dataHash: dataHash
        }));

        // 更新催收次数
        OverdueInfo storage info = overdueInfos[receivableId];
        info.remindCount++;
        info.lastRemindDate = remindDate;

        remindRecordCount++;

        emit RemindRecordCreated(receivableId, remindType, operator, remindDate);
        return true;
    }

    /**
     * @dev 记录罚息
     * @param receivableId 应收账款ID
     * @param penaltyType 罚息类型
     * @param principalAmount 本金金额（分）
     * @param overdueDays 逾期天数
     * @param dailyRate 日利率（×10000）
     * @param penaltyAmount 本次罚息（分）
     * @param totalPenaltyAmount 累计罚息（分）
     * @param calculateStartDate 计算起始日期
     * @param calculateEndDate 计算结束日期
     * @param dataHash 数据哈希
     */
    function recordPenalty(
        string memory receivableId,
        PenaltyType penaltyType,
        uint256 principalAmount,
        uint256 overdueDays,
        uint256 dailyRate,
        uint256 penaltyAmount,
        uint256 totalPenaltyAmount,
        uint256 calculateStartDate,
        uint256 calculateEndDate,
        bytes32 dataHash
    ) public onlyExistingReceivable(receivableId) returns (bool) {
        // 创建罚息记录
        penaltyRecords[receivableId].push(PenaltyRecord({
            receivableId: receivableId,
            penaltyType: penaltyType,
            principalAmount: principalAmount,
            overdueDays: overdueDays,
            dailyRate: dailyRate,
            penaltyAmount: penaltyAmount,
            totalPenaltyAmount: totalPenaltyAmount,
            calculateStartDate: calculateStartDate,
            calculateEndDate: calculateEndDate,
            calculateDate: block.timestamp,
            dataHash: dataHash
        }));

        // 更新累计罚息
        OverdueInfo storage info = overdueInfos[receivableId];
        info.penaltyAmount = totalPenaltyAmount;

        penaltyRecordCount++;

        emit PenaltyRecordCreated(receivableId, penaltyType, penaltyAmount, totalPenaltyAmount);
        return true;
    }

    /**
     * @dev 记录坏账
     * @param receivableId 应收账款ID
     * @param badDebtType 坏账类型
     * @param principalAmount 本金金额（分）
     * @param overdueDays 逾期天数
     * @param totalPenaltyAmount 累计罚息（分）
     * @param totalLossAmount 总损失（分）
     * @param badDebtReason 坏账原因
     * @param dataHash 数据哈希
     */
    function recordBadDebt(
        string memory receivableId,
        BadDebtType badDebtType,
        uint256 principalAmount,
        uint256 overdueDays,
        uint256 totalPenaltyAmount,
        uint256 totalLossAmount,
        string memory badDebtReason,
        bytes32 dataHash
    ) public onlyExistingReceivable(receivableId) onlyAdmin returns (bool) {
        // 检查是否已经存在坏账记录
        require(badDebtRecords[receivableId].principalAmount == 0, "Bad debt already recorded");

        // 创建坏账记录
        badDebtRecords[receivableId] = BadDebtRecord({
            receivableId: receivableId,
            badDebtType: badDebtType,
            principalAmount: principalAmount,
            overdueDays: overdueDays,
            totalPenaltyAmount: totalPenaltyAmount,
            totalLossAmount: totalLossAmount,
            badDebtReason: badDebtReason,
            recoveryStatus: RecoveryStatus.NotRecovered,
            recoveredAmount: 0,
            recordDate: block.timestamp,
            dataHash: dataHash
        });

        // 更新逾期信息
        OverdueInfo storage info = overdueInfos[receivableId];
        info.level = OverdueLevel.BadDebt;
        info.badDebtDate = block.timestamp;
        info.badDebtReason = badDebtReason;
        info.isOverdue = true;

        badDebtRecordCount++;

        emit BadDebtRecordCreated(receivableId, badDebtType, totalLossAmount);
        return true;
    }

    /**
     * @dev 更新坏账回收状态
     * @param receivableId 应收账款ID
     * @param recoveredAmount 已回收金额（分）
     * @param recoveryStatus 回收状态
     */
    function updateBadDebtRecovery(
        string memory receivableId,
        uint256 recoveredAmount,
        RecoveryStatus recoveryStatus
    ) public onlyExistingReceivable(receivableId) onlyAdmin returns (bool) {
        BadDebtRecord storage record = badDebtRecords[receivableId];
        require(record.principalAmount > 0, "Bad debt record not found");

        record.recoveredAmount = recoveredAmount;
        record.recoveryStatus = recoveryStatus;

        return true;
    }

    // ==================== 查询功能 ====================

    /**
     * @dev 获取应收账款核心数据
     */
    function getReceivableCore(string memory receivableId)
        public
        view
        returns (ReceivableCore memory)
    {
        return receivableCores[receivableId];
    }

    /**
     * @dev 获取逾期信息
     */
    function getOverdueInfo(string memory receivableId)
        public
        view
        returns (OverdueInfo memory)
    {
        return overdueInfos[receivableId];
    }

    /**
     * @dev 获取催收记录数量
     */
    function getRemindRecordCount(string memory receivableId)
        public
        view
        returns (uint256)
    {
        return remindRecords[receivableId].length;
    }

    /**
     * @dev 获取催收记录
     */
    function getRemindRecord(string memory receivableId, uint256 index)
        public
        view
        returns (RemindRecord memory)
    {
        require(index < remindRecords[receivableId].length, "Index out of bounds");
        return remindRecords[receivableId][index];
    }

    /**
     * @dev 获取罚息记录数量
     */
    function getPenaltyRecordCount(string memory receivableId)
        public
        view
        returns (uint256)
    {
        return penaltyRecords[receivableId].length;
    }

    /**
     * @dev 获取罚息记录
     */
    function getPenaltyRecord(string memory receivableId, uint256 index)
        public
        view
        returns (PenaltyRecord memory)
    {
        require(index < penaltyRecords[receivableId].length, "Index out of bounds");
        return penaltyRecords[receivableId][index];
    }

    /**
     * @dev 获取坏账记录
     */
    function getBadDebtRecord(string memory receivableId)
        public
        view
        returns (BadDebtRecord memory)
    {
        return badDebtRecords[receivableId];
    }

    /**
     * @dev 检查是否为坏账
     */
    function isBadDebt(string memory receivableId)
        public
        view
        returns (bool)
    {
        return badDebtRecords[receivableId].principalAmount > 0;
    }

    /**
     * @dev 转移管理员权限
     */
    function transferAdmin(address newAdmin) public onlyAdmin {
        require(newAdmin != address(0), "New admin cannot be zero address");
        admin = newAdmin;
    }
}
