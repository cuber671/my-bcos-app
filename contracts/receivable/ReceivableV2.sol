// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

/**
 * @title ReceivableV2
 * @dev 应收账款管理智能合约 - V2版本
 *
 * 核心设计原则：
 * 1. 数据精简化与哈希化 - 仅核心数据上链，扩展数据哈希化存储
 * 2. 严格访问控制 - 细粒度权限控制，包括Java后端专用权限
 * 3. 规避栈溢出 - 函数参数<16个，使用struct组织数据
 * 4. 异常处理与状态一致性 - 完整的require验证，遵循Checks-Effects-Interactions模式
 * 5. 充分利用事件 - 所有关键操作触发事件，支持链下监听和同步
 *
 * @notice V2版本新增：Java后端访问控制、完整的事件系统、冻结管理功能
 */
contract ReceivableV2 {
    // ==================== 枚举定义 ====================

    /**
     * @dev 应收账款状态枚举
     */
    enum ReceivableStatus {
        Created,      // 0 - 已创建（待确认）
        Confirmed,    // 1 - 已确认（核心企业确认）
        Financed,     // 2 - 已融资
        Repaid,       // 3 - 已还款
        Defaulted,    // 4 - 已违约
        Cancelled     // 5 - 已取消
    }

    /**
     * @dev 逾期等级枚举
     */
    enum OverdueLevel {
        None,        // 0 - 无逾期
        Mild,        // 1 - 轻度逾期 (1-30天)
        Moderate,    // 2 - 中度逾期 (31-90天)
        Severe,      // 3 - 重度逾期 (91-179天)
        BadDebt      // 4 - 坏账 (180天+)
    }

    // ==================== 结构体定义 ====================

    /**
     * @dev 应收账款核心数据（上链存储）
     * @notice 仅核心业务逻辑必需的数据上链存储
     * @notice 时间字段移除，通过事件记录；exists标志移除，通过amount > 0或supplier != 0x0判断
     */
    struct ReceivableCore {
        // ========== 谁能做（地址权限） ==========
        address supplier;              // 供应商地址（创建者）
        address coreEnterprise;        // 核心企业地址（付款方）
        address currentHolder;         // 当前持有人（所有权）
        address financier;             // 金融机构地址（融资方）

        // ========== 给多少钱（金额） ==========
        uint256 amount;                // 应收金额
        uint256 financedAmount;        // 已融资金额

        // ========== 什么时候做（时间） ==========
        uint256 dueDate;               // 到期日期（业务逻辑需要用于判断逾期）

        // ========== 业务状态 ==========
        ReceivableStatus status;       // 状态
        bool frozen;                   // 冻结标志
    }

    /**
     * @dev 应收账款元数据（哈希化存储）
     * @notice 扩展数据通过哈希上链，原文存储在链下
     */
    struct ReceivableMeta {
        string receivableId;
        bytes32 coreDataHash;           // 核心数据哈希（发票号、合同号等）
        bytes32 extendedDataHash;       // 扩展数据哈希（货物描述、付款条款等）
        bytes32 overdueDataHash;        // 逾期数据哈希（催收记录、罚息记录等）
        uint256 createdAt;
        uint256 updatedAt;
    }

    /**
     * @dev 逾期信息
     */
    struct OverdueInfo {
        OverdueLevel level;             // 逾期等级
        uint256 overdueDays;            // 逾期天数
        uint256 penaltyAmount;          // 累计罚息金额
        uint256 lastRemindDate;         // 最后催收日期
        uint256 remindCount;            // 催收次数
        bool isOverdue;                 // 是否逾期
    }

    /**
     * @dev 转让记录
     */
    struct TransferRecord {
        string receivableId;
        address from;                   // 转让人
        address to;                     // 受让人
        uint256 amount;                 // 转让金额
        uint256 timestamp;              // 转让时间
        string transferType;            // 转让类型（融资/转让/还款）
        bytes32 dataHash;               // 转让数据哈希
    }

    /**
     * @dev 应收账款完整信息结构（用于返回，避免栈溢出）
     * @notice 封装所有应收账款相关数据，减少函数返回参数数量
     * @notice 时间字段移除，通过事件记录
     */
    struct ReceivableInfo {
        address supplier;
        address coreEnterprise;
        address currentHolder;
        address financier;
        uint256 amount;
        uint256 financedAmount;
        uint256 dueDate;
        ReceivableStatus status;
        bool frozen;
        bytes32 coreDataHash;
        bytes32 extendedDataHash;
        bytes32 overdueDataHash;
    }

    // ==================== 状态变量 ====================

    /**
     * @dev 管理员地址
     * @notice 合约部署者，拥有最高权限
     */
    address public admin;

    /**
     * @dev Java后端地址
     * @notice Java后端服务地址，用于敏感操作（如创建应收账款）
     */
    address public javaBackend;

    /**
     * @dev 应收账款总数
     */
    uint256 public receivableCount;

    /**
     * @dev 应收账款创建输入结构体（简化版）
     * @notice 封装创建应收账款所需的核心参数，减少栈使用
     * @notice issueDate保留在输入中用于事件记录，但不存储到结构体
     */
    struct ReceivableCreationInput {
        string receivableId;
        address supplier;
        address coreEnterprise;
        uint256 amount;
        uint256 issueDate;
        uint256 dueDate;
        bytes32 metadataHash;  // 包含所有扩展信息（发票号、合同号等）
    }

    /**
     * @dev 应收账款融资输入结构体（简化版）
     * @notice 封装融资所需的核心参数，减少栈使用
     */
    struct ReceivableFinanceInput {
        string receivableId;
        address financier;
        uint256 financedAmount;
        uint256 financeRate;
        bytes32 overdueDataHash;
    }

    /**
     * @dev 逾期状态更新输入结构体（简化版）
     * @notice 封装逾期状态更新所需的参数，减少栈使用
     */
    struct OverdueStatusUpdateInput {
        string receivableId;
        OverdueLevel level;
        uint256 overdueDays;
        bytes32 overdueDataHash;
    }

    /**
     * @dev 应收账款核心数据映射
     */
    mapping(string => ReceivableCore) public receivableCores;

    /**
     * @dev 应收账款元数据映射
     */
    mapping(string => ReceivableMeta) public receivableMetas;

    /**
     * @dev 逾期信息映射
     */
    mapping(string => OverdueInfo) public overdueInfos;

    /**
     * @dev 供应商应收账款列表
     */
    mapping(address => string[]) public supplierReceivables;

    /**
     * @dev 核心企业应付账款列表
     */
    mapping(address => string[]) public coreEnterpriseReceivables;

    /**
     * @dev 金融机构融资账款列表
     */
    mapping(address => string[]) public financierReceivables;

    /**
     * @dev 转让历史记录
     */
    mapping(string => TransferRecord[]) public transferHistory;

    // ==================== 事件定义 ====================

    /**
     * @dev 应收账款创建事件
     */
    event ReceivableCreated(
        string indexed receivableId,
        address indexed supplier,
        address indexed coreEnterprise,
        uint256 amount,
        uint256 issueDate,
        uint256 dueDate,
        bytes32 coreDataHash,
        bytes32 extendedDataHash
    );

    /**
     * @dev 应收账款确认事件
     */
    event ReceivableConfirmed(
        string indexed receivableId,
        address indexed coreEnterprise,
        uint256 confirmedAt
    );

    /**
     * @dev 应收账款融资事件
     */
    event ReceivableFinanced(
        string indexed receivableId,
        address indexed financier,
        uint256 financedAmount,
        uint256 financeRate,
        uint256 financedAt
    );

    /**
     * @dev 应收账款转让事件
     */
    event ReceivableTransferred(
        string indexed receivableId,
        address indexed from,
        address indexed to,
        uint256 amount,
        uint256 timestamp
    );

    /**
     * @dev 应收账款还款事件
     */
    event ReceivableRepaid(
        string indexed receivableId,
        uint256 amount,
        uint256 repaidAt
    );

    /**
     * @dev 应收账款违约事件
     */
    event ReceivableDefaulted(
        string indexed receivableId,
        uint256 amount,
        uint256 defaultedAt
    );

    /**
     * @dev 应收账款取消事件
     */
    event ReceivableCancelled(
        string indexed receivableId,
        address indexed supplier,
        uint256 cancelledAt
    );

    /**
     * @dev 应收账款冻结事件
     */
    event ReceivableFrozen(
        string indexed receivableId,
        address indexed admin,
        string reason
    );

    /**
     * @dev 应收账款解冻事件
     */
    event ReceivableUnfrozen(
        string indexed receivableId,
        address indexed admin,
        string reason
    );

    /**
     * @dev 逾期状态更新事件
     */
    event OverdueStatusUpdated(
        string indexed receivableId,
        OverdueLevel level,
        uint256 overdueDays
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
     * @notice 保护敏感操作，如创建应收账款
     */
    modifier onlyJavaBackend() {
        require(msg.sender == javaBackend, "Only Java backend can call this function");
        _;
    }

    /**
     * @dev 应收账款必须存在
     * @notice 通过 supplier != address(0) 或 amount > 0 判断是否存在
     */
    modifier receivableExists(string memory receivableId) {
        require(receivableCores[receivableId].supplier != address(0), "Receivable does not exist");
        _;
    }

    /**
     * @dev 仅供应商可调用
     */
    modifier onlySupplier(string memory receivableId) {
        require(receivableCores[receivableId].supplier == msg.sender, "Only supplier can call this function");
        _;
    }

    /**
     * @dev 仅核心企业可调用
     */
    modifier onlyCoreEnterprise(string memory receivableId) {
        require(receivableCores[receivableId].coreEnterprise == msg.sender, "Only core enterprise can call this function");
        _;
    }

    /**
     * @dev 仅当前持有人可调用
     */
    modifier onlyCurrentHolder(string memory receivableId) {
        require(receivableCores[receivableId].currentHolder == msg.sender, "Only current holder can call this function");
        _;
    }

    /**
     * @dev 仅金融机构可调用
     */
    modifier onlyFinancier(string memory receivableId) {
        require(receivableCores[receivableId].financier == msg.sender, "Only financier can call this function");
        _;
    }

    /**
     * @dev 应收账款未冻结
     */
    modifier notFrozen(string memory receivableId) {
        require(!receivableCores[receivableId].frozen, "Receivable is frozen");
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
        receivableCount = 0;

        emit AdminSet(address(0), admin, block.timestamp);
        emit JavaBackendSet(address(0), javaBackend, block.timestamp);
    }

    // ==================== 管理员函数 ====================

    /**
     * @dev 设置新管理员
     * @notice 仅当前管理员可调用
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
     * @notice 仅管理员可调用，用于指定Java后端服务地址
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
    * @dev 创建应收账款（优化版：使用 calldata 规避栈溢出）
    */
    function createReceivable(ReceivableCreationInput calldata input)
        external
        onlyJavaBackend
        returns (bool success)
    {
        // 1. 基础验证
        require(bytes(input.receivableId).length > 0, "ID empty");
        require(receivableCores[input.receivableId].supplier == address(0), "Exists");
        require(input.supplier != address(0) && input.coreEnterprise != address(0), "Addr zero");
        require(input.amount > 0, "Amt zero");
        require(input.dueDate > input.issueDate, "Date error");

        // 2. 写入核心数据 (利用 storage 指针减少中间变量)
        {
            ReceivableCore storage core = receivableCores[input.receivableId];
            core.supplier = input.supplier;
            core.coreEnterprise = input.coreEnterprise;
            core.currentHolder = input.supplier;
            core.amount = input.amount;
            core.dueDate = input.dueDate;
            core.status = ReceivableStatus.Created;
            core.financier = address(0);
            core.financedAmount = 0;
        }

        // 3. 处理元数据
        {
            ReceivableMeta storage meta = receivableMetas[input.receivableId];
            meta.receivableId = input.receivableId;
            meta.coreDataHash = input.metadataHash;
            meta.createdAt = block.timestamp;
            meta.updatedAt = block.timestamp;

            supplierReceivables[input.supplier].push(input.receivableId);
            coreEnterpriseReceivables[input.coreEnterprise].push(input.receivableId);
            receivableCount++;
        }

        // 4. 触发事件（issueDate在事件中记录，不存储到结构体）
        emit ReceivableCreated(
            input.receivableId,
            input.supplier,
            input.coreEnterprise,
            input.amount,
            input.issueDate,
            input.dueDate,
            input.metadataHash,
            bytes32(0)
        );

        return true;
    }

    /**
     * @dev 核心企业确认应收账款
     * @param receivableId 应收账款ID
     * @return success 操作是否成功
     */
    function confirmReceivable(string memory receivableId)
        external
        receivableExists(receivableId)
        onlyCoreEnterprise(receivableId)
        notFrozen(receivableId)
        returns (bool success)
    {
        // ========== Checks ==========
        ReceivableCore storage core = receivableCores[receivableId];
        require(core.status == ReceivableStatus.Created, "Invalid status for confirmation");

        // ========== Effects ==========
        core.status = ReceivableStatus.Confirmed;
        receivableMetas[receivableId].updatedAt = block.timestamp;

        // ========== Events ==========
        emit ReceivableConfirmed(receivableId, msg.sender, block.timestamp);
        return true;
    }

    /**
    * @dev 应收账款融资（优化版：calldata 传参）
    * @param input 融资输入参数结构体（存储在 calldata 中，减少栈负载）
    */
    function financeReceivable(ReceivableFinanceInput calldata input)
        external
        receivableExists(input.receivableId)
        onlyCurrentHolder(input.receivableId)
        notFrozen(input.receivableId)
        returns (bool success)
    {
        ReceivableCore storage core = receivableCores[input.receivableId];

        // ========== Checks ==========
        require(core.status == ReceivableStatus.Confirmed, "Receivable not confirmed");
        require(input.financier != address(0), "Invalid financier address");
        require(input.financedAmount > 0 && input.financedAmount <= core.amount, "Invalid finance amount");

        // ========== Effects ==========
        // 使用作用域隔离局部变量，进一步优化栈空间
        {
            address oldHolder = core.currentHolder;
            core.status = ReceivableStatus.Financed;
            core.currentHolder = input.financier;
            core.financier = input.financier;
            core.financedAmount = input.financedAmount;

            // 记录转让历史
            transferHistory[input.receivableId].push(TransferRecord({
                receivableId: input.receivableId,
                from: oldHolder,
                to: input.financier,
                amount: input.financedAmount,
                timestamp: block.timestamp,
                transferType: "financing",
                dataHash: input.overdueDataHash
            }));

            // 触发转让事件
            emit ReceivableTransferred(input.receivableId, oldHolder, input.financier, input.financedAmount, block.timestamp);
        }

        // 更新元数据
        ReceivableMeta storage meta = receivableMetas[input.receivableId];
        meta.updatedAt = block.timestamp;
        meta.overdueDataHash = input.overdueDataHash;

        financierReceivables[input.financier].push(input.receivableId);

        // 触发融资事件（financedAt通过事件记录，不存储到结构体）
        emit ReceivableFinanced(
            input.receivableId,
            input.financier,
            input.financedAmount,
            input.financeRate,
            block.timestamp
        );

        return true;
    }

    /**
     * @dev 还款
     * @param receivableId 应收账款ID
     * @param amount 还款金额（用于事件记录，不存储到结构体）
     * @return success 操作是否成功
     */
    function repayReceivable(string memory receivableId, uint256 amount)
        external
        receivableExists(receivableId)
        notFrozen(receivableId)
        returns (bool success)
    {
        // ========== Checks ==========
        ReceivableCore storage core = receivableCores[receivableId];
        require(
            msg.sender == core.coreEnterprise || msg.sender == core.financier,
            "Not authorized to repay"
        );
        require(core.status == ReceivableStatus.Financed, "Not in financed status");
        require(amount > 0, "Amount must be greater than 0");

        // ========== Effects ==========
        core.status = ReceivableStatus.Repaid;
        receivableMetas[receivableId].updatedAt = block.timestamp;

        // 记录转让历史
        transferHistory[receivableId].push(TransferRecord({
            receivableId: receivableId,
            from: msg.sender,
            to: core.financier,
            amount: amount,
            timestamp: block.timestamp,
            transferType: "repayment",
            dataHash: bytes32(0)
        }));

        // ========== Events ==========
        emit ReceivableRepaid(receivableId, amount, block.timestamp);
        return true;
    }

    /**
     * @dev 标记违约
     * @param receivableId 应收账款ID
     * @return success 操作是否成功
     */
    function markAsDefaulted(string memory receivableId)
        external
        receivableExists(receivableId)
        onlyAdmin
        returns (bool success)
    {
        // ========== Checks ==========
        ReceivableCore storage core = receivableCores[receivableId];
        require(core.status == ReceivableStatus.Financed, "Not in financed status");
        require(block.timestamp > core.dueDate, "Not yet due");

        // ========== Effects ==========
        core.status = ReceivableStatus.Defaulted;
        receivableMetas[receivableId].updatedAt = block.timestamp;

        // ========== Events ==========
        emit ReceivableDefaulted(receivableId, core.amount, block.timestamp);
        return true;
    }

    /**
     * @dev 取消应收账款
     * @param receivableId 应收账款ID
     * @return success 操作是否成功
     */
    function cancelReceivable(string memory receivableId)
        external
        receivableExists(receivableId)
        onlySupplier(receivableId)
        notFrozen(receivableId)
        returns (bool success)
    {
        // ========== Checks ==========
        ReceivableCore storage core = receivableCores[receivableId];
        require(core.status == ReceivableStatus.Created, "Cannot cancel confirmed or financed receivable");

        // ========== Effects ==========
        core.status = ReceivableStatus.Cancelled;
        receivableMetas[receivableId].updatedAt = block.timestamp;

        // ========== Events ==========
        emit ReceivableCancelled(receivableId, msg.sender, block.timestamp);
        return true;
    }

    // ==================== 冻结管理函数 ====================

    /**
     * @dev 冻结应收账款
     * @param receivableId 应收账款ID
     * @param reason 冻结原因
     * @return success 操作是否成功
     */
    function freezeReceivable(string memory receivableId, string memory reason)
        external
        receivableExists(receivableId)
        onlyAdmin
        returns (bool success)
    {
        // ========== Checks ==========
        ReceivableCore storage core = receivableCores[receivableId];
        require(!core.frozen, "Receivable already frozen");

        // ========== Effects ==========
        core.frozen = true;
        receivableMetas[receivableId].updatedAt = block.timestamp;

        // ========== Events ==========
        emit ReceivableFrozen(receivableId, msg.sender, reason);
        return true;
    }

    /**
     * @dev 解除冻结应收账款
     * @param receivableId 应收账款ID
     * @param reason 解冻原因
     * @return success 操作是否成功
     */
    function unfreezeReceivable(string memory receivableId, string memory reason)
        external
        receivableExists(receivableId)
        onlyAdmin
        returns (bool success)
    {
        // ========== Checks ==========
        ReceivableCore storage core = receivableCores[receivableId];
        require(core.frozen, "Receivable not frozen");

        // ========== Effects ==========
        core.frozen = false;
        receivableMetas[receivableId].updatedAt = block.timestamp;

        // ========== Events ==========
        emit ReceivableUnfrozen(receivableId, msg.sender, reason);
        return true;
    }

    // ==================== 逾期管理函数 ====================

    /**
    * @dev 更新逾期状态（优化版：calldata 传参）
    */
    function updateOverdueStatus(OverdueStatusUpdateInput calldata input)
        external 
        receivableExists(input.receivableId) 
        returns (bool success) 
    {
        // ========== Checks ==========
        // 使用 memory 拷贝以减少对 storage 的重复访问（对 view 逻辑优化）
        ReceivableCore memory core = receivableCores[input.receivableId];
        require(
            msg.sender == core.supplier || msg.sender == core.financier || msg.sender == admin,
            "Not authorized"
        );

        // ========== Effects ==========
        OverdueInfo storage info = overdueInfos[input.receivableId];
        info.level = input.level;
        info.overdueDays = input.overdueDays;

        if (input.overdueDays > 0) {
            info.isOverdue = true;
        }

        // 更新元数据时间戳与哈希
        ReceivableMeta storage meta = receivableMetas[input.receivableId];
        meta.updatedAt = block.timestamp;
        meta.overdueDataHash = input.overdueDataHash;

        // ========== Events ==========
        emit OverdueStatusUpdated(input.receivableId, input.level, input.overdueDays);
        return true;
    }

    // ==================== 查询函数 ====================

/**
     * @dev 获取应收账款核心业务信息 (Part 1)
     * @notice 拆分以规避 Stack too deep 错误
     */
    function getReceivableCore(string memory receivableId)
        external
        view
        receivableExists(receivableId)
        returns (
            address supplier,
            address coreEnterprise,
            address currentHolder,
            address financier,
            uint256 amount,
            ReceivableStatus status,
            bool frozen
        )
    {
        ReceivableCore storage core = receivableCores[receivableId];
        return (
            core.supplier,
            core.coreEnterprise,
            core.currentHolder,
            core.financier,
            core.amount,
            core.status,
            core.frozen
        );
    }

    /**
     * @dev 获取应收账款哈希数据 (Part 2)
     * @notice 时间字段已移除，通过事件查询获取
     */
    function getReceivableExtra(string memory receivableId)
        external
        view
        receivableExists(receivableId)
        returns (
            uint256 dueDate,
            bytes32 coreDataHash,
            bytes32 extendedDataHash,
            bytes32 overdueDataHash
        )
    {
        ReceivableCore storage core = receivableCores[receivableId];
        ReceivableMeta storage meta = receivableMetas[receivableId];
        return (
            core.dueDate,
            meta.coreDataHash,
            meta.extendedDataHash,
            meta.overdueDataHash
        );
    }

    /**
     * @dev 获取逾期信息
     * @param receivableId 应收账款ID
     * @return level 逾期等级
     * @return overdueDays 逾期天数
     * @return penaltyAmount 累计罚息金额
     * @return lastRemindDate 最后催收日期
     * @return remindCount 催收次数
     * @return isOverdue 是否逾期
     */
    function getOverdueInfo(string memory receivableId)
        external
        view
        returns (
            OverdueLevel level,
            uint256 overdueDays,
            uint256 penaltyAmount,
            uint256 lastRemindDate,
            uint256 remindCount,
            bool isOverdue
        )
    {
        OverdueInfo memory info = overdueInfos[receivableId];
        return (
            info.level,
            info.overdueDays,
            info.penaltyAmount,
            info.lastRemindDate,
            info.remindCount,
            info.isOverdue
        );
    }

    /**
     * @dev 获取转让历史数量
     * @param receivableId 应收账款ID
     * @return count 转让历史数量
     */
    function getTransferHistoryCount(string memory receivableId)
        external
        view
        returns (uint256 count)
    {
        return transferHistory[receivableId].length;
    }

    /**
     * @dev 获取供应商的应收账款列表
     * @param supplier 供应商地址
     * @return receivables 应收账款ID数组
     */
    function getSupplierReceivables(address supplier)
        external
        view
        returns (string[] memory receivables)
    {
        return supplierReceivables[supplier];
    }

    /**
     * @dev 获取核心企业的应付账款列表
     * @param coreEnterprise 核心企业地址
     * @return receivables 应收账款ID数组
     */
    function getCoreEnterpriseReceivables(address coreEnterprise)
        external
        view
        returns (string[] memory receivables)
    {
        return coreEnterpriseReceivables[coreEnterprise];
    }

    /**
     * @dev 获取金融机构的融资账款列表
     * @param financier 金融机构地址
     * @return receivables 应收账款ID数组
     */
    function getFinancierReceivables(address financier)
        external
        view
        returns (string[] memory receivables)
    {
        return financierReceivables[financier];
    }

    /**
     * @dev 获取应收账款总数
     * @return count 应收账款总数
     */
    function getReceivableCount()
        external
        view
        returns (uint256 count)
    {
        return receivableCount;
    }

    /**
     * @dev 检查应收账款是否存在
     * @param receivableId 应收账款ID
     * @return exists 是否存在（通过 supplier != address(0) 判断）
     */
    function isReceivableExists(string memory receivableId)
        external
        view
        returns (bool exists)
    {
        return receivableCores[receivableId].supplier != address(0);
    }

    /**
     * @dev 检查应收账款是否冻结
     * @param receivableId 应收账款ID
     * @return frozen 是否冻结
     */
    function isReceivableFrozen(string memory receivableId)
        external
        view
        returns (bool frozen)
    {
        return receivableCores[receivableId].frozen;
    }

    /**
     * @dev 检查应收账款是否已融资
     * @param receivableId 应收账款ID
     * @return financed 是否已融资
     */
    function isReceivableFinanced(string memory receivableId)
        external
        view
        returns (bool financed)
    {
        return receivableCores[receivableId].financedAmount > 0;
    }

    /**
     * @dev 检查应收账款是否逾期
     * @param receivableId 应收账款ID
     * @return isOverdue 是否逾期
     */
    function isReceivableOverdue(string memory receivableId)
        external
        view
        returns (bool isOverdue)
    {
        return overdueInfos[receivableId].isOverdue;
    }

    /**
     * @dev 批量查询应收账款状态
     * @param receivableIds 应收账款ID数组
     * @return statuses 状态数组
     */
    function getReceivablesStatus(string[] calldata receivableIds)
        external
        view
        returns (ReceivableStatus[] memory statuses)
    {
        statuses = new ReceivableStatus[](receivableIds.length);
        for (uint256 i = 0; i < receivableIds.length; i++) {
            if (receivableCores[receivableIds[i]].supplier != address(0)) {
                statuses[i] = receivableCores[receivableIds[i]].status;
            } else {
                statuses[i] = ReceivableStatus.Cancelled;  // 不存在的视为已取消
            }
        }
        return statuses;
    }

    /**
     * @dev 批量查询应收账款持有人
     * @param receivableIds 应收账款ID数组
     * @return holders 持有人地址数组
     */
    function getReceivablesHolders(string[] calldata receivableIds)
        external
        view
        returns (address[] memory holders)
    {
        holders = new address[](receivableIds.length);
        for (uint256 i = 0; i < receivableIds.length; i++) {
            if (receivableCores[receivableIds[i]].supplier != address(0)) {
                holders[i] = receivableCores[receivableIds[i]].currentHolder;
            } else {
                holders[i] = address(0);
            }
        }
        return holders;
    }
}