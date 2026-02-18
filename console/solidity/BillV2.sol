// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

/**
 * @title BillV2
 * @dev 票据管理智能合约 V2.0
 *
 * 核心功能：
 * 1. 票据生命周期管理（签发、承兑、背书、贴现、付款）
 * 2. 质押管理（质押、解质押）
 * 3. 融资管理（申请、批准、偿还）
 * 4. 拆分合并（拆分、合并）
 * 5. 担保管理（添加、移除）
 * 6. 追索管理（发起、结算）
 * 7. 冻结管理（冻结、解冻）
 * @author FISCO BCOS Supply Chain Finance Team
 */
contract BillV2 {

    // ==================== 枚举定义 ====================

    /**
     * @dev 票据状态枚举
     */
    enum BillStatus {
        DRAFT,           // 草稿
        ISSUED,          // 已开票
        ENDORSED,        // 已背书
        PLEDGED,         // 已质押
        DISCOUNTED,      // 已贴现
        FINANCED,        // 已融资
        FROZEN,          // 已冻结
        PAID,            // 已付款
        SETTLED,         // 已结算
        CANCELLED        // 已作废
    }

    /**
     * @dev 质押状态枚举
     */
    enum PledgeStatus {
        ACTIVE,          // 活跃
        RELEASED,        // 已释放
        DEFAULTED        // 已违约
    }

    /**
     * @dev 融资状态枚举
     */
    enum FinancingStatus {
        PENDING,         // 待审批
        APPROVED,        // 已批准
        REJECTED,        // 已拒绝
        ACTIVE,          // 融资中
        COMPLETED,       // 已完成
        DEFAULTED        // 已违约
    }

    /**
     * @dev 追索状态枚举
     */
    enum RecourseStatus {
        PENDING,         // 待处理
        APPROVED,        // 已批准
        REJECTED,        // 已拒绝
        SETTLED,         // 已结算
        CANCELLED        // 已取消
    }

    // ==================== 结构体定义（避免栈溢出）====================

    /**
     * @dev 核心票据数据（仅9个关键字段上链）
     */
    struct BillCore {
        string billId;              // 索引键
        address issuer;             // 出票人（权限判定）
        address acceptor;           // 承兑人（付款责任）
        address currentHolder;      // 当前持票人（所有权）
        uint256 amount;             // 票面金额（金额计算）
        uint256 issueDate;          // 开票日期（时间逻辑）
        uint256 dueDate;            // 到期日期（到期判断）
        BillStatus status;          // 状态（状态流转）
        bool frozen;                // 冻结标志（权限控制）
        bool exists;                 // 存在标志
    }

    /**
     * @dev 票据元数据（哈希化存储）
     */
    struct BillMeta {
        string billId;              // 票据ID
        bytes32 coreDataHash;       // 核心数据哈希（包含billNo、billType等）
        bytes32 extendedDataHash;   // 扩展数据哈希（包含所有详细信息）
        uint256 createdAt;          // 创建时间
        uint256 updatedAt;          // 更新时间
    }

    /**
     * @dev 质押记录结构
     */
    struct Pledge {
        string billId;
        address pledgee;            // 质押权人
        uint256 amount;             // 质押金额
        uint256 period;             // 质押期限
        uint256 pledgeDate;         // 质押日期
        PledgeStatus status;       // 质押状态
        bool active;               // 是否活跃
    }

    /**
     * @dev 融资记录结构
     */
    struct Financing {
        string billId;
        address institution;        // 融资机构
        uint256 amount;             // 融资金额
        uint256 rate;               // 融资利率（基点）
        uint256 financingDate;     // 融资日期
        FinancingStatus status;    // 融资状态
        bool active;               // 是否活跃
    }

    /**
     * @dev 担保记录结构
     */
    struct Guarantee {
        string billId;
        address guarantor;         // 担保人
        uint256 amount;             // 担保金额
        uint256 guaranteeDate;     // 担保日期
        bool active;               // 是否活跃
    }

    /**
     * @dev 追索记录结构
     */
    struct Recourse {
        string billId;
        address claimant;           // 追索人
        uint256 amount;             // 追索金额
        string reason;              // 追索原因
        uint256 recourseDate;      // 追索日期
        RecourseStatus status;     // 追索状态
    }

    /**
     * @dev 背书信息结构
     */
    struct EndorsementInfo {
        address endorser;           // 背书人
        address endorsee;           // 被背书人
        uint256 timestamp;         // 背书时间
    }

    /**
     * @dev 票据完整信息结构（用于返回，避免栈溢出）
     * @notice 封装所有票据相关数据，减少函数返回参数数量
     */
    struct BillInfo {
        string billId;
        address issuer;
        address acceptor;
        address currentHolder;
        uint256 amount;
        uint256 issueDate;
        uint256 dueDate;
        BillStatus status;
        bool frozen;
        bytes32 coreDataHash;
        bytes32 extendedDataHash;
    }

    // ==================== 状态变量 ====================

    // 管理员
    address public admin;
    address public javaBackend;

    // 计数器
    uint256 public billCount;

    // 双层存储映射
    mapping(string => BillCore) public billCores;
    mapping(string => BillMeta) public billMetas;

    // 功能模块映射
    mapping(string => Pledge) public pledges;
    mapping(string => Financing) public financings;
    mapping(string => Guarantee[]) public guarantees;
    mapping(string => Recourse) public recourses;
    mapping(string => EndorsementInfo[]) public endorsementHistory;

    // 辅助映射
    mapping(address => string[]) public issuedBills;        // 出票人票据列表
    mapping(address => string[]) public holdingBills;       // 持票人票据列表
    mapping(address => string[]) public acceptedBills;       // 承兑人票据列表
    mapping(string => bool) public frozenBills;             // 冻结票据列表

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
     * @dev 仅当前持票人可调用
     */
    modifier onlyHolder(string memory billId) {
        require(
            billCores[billId].currentHolder == msg.sender,
            "Only current holder"
        );
        _;
    }

    /**
     * @dev 仅出票人可调用
     */
    modifier onlyIssuer(string memory billId) {
        require(
            billCores[billId].issuer == msg.sender,
            "Only issuer"
        );
        _;
    }

    /**
     * @dev 仅承兑人可调用
     */
    modifier onlyAcceptor(string memory billId) {
        require(
            billCores[billId].acceptor == msg.sender,
            "Only acceptor"
        );
        _;
    }

    /**
     * @dev 票据必须存在且未冻结
     */
    modifier billExists(string memory billId) {
        require(billCores[billId].exists, "Bill not exist");
        require(!billCores[billId].frozen, "Bill frozen");
        _;
    }

    // ==================== 事件定义 ====================

    // 票据生命周期事件
    event BillIssued(
        string indexed billId,
        address indexed issuer,
        address indexed currentHolder,
        uint256 amount,
        bytes32 coreDataHash,
        bytes32 extendedDataHash
    );

    event BillAccepted(
        string indexed billId,
        address indexed acceptor,
        uint256 acceptanceDate
    );

    event BillEndorsed(
        string indexed billId,
        address indexed endorser,
        address indexed endorsee,
        uint256 endorsementCount
    );

    event BillDiscounted(
        string indexed billId,
        address indexed institution,
        uint256 amount,
        uint256 rate
    );

    event BillPaid(
        string indexed billId,
        uint256 amount,
        address payer,
        uint256 paymentDate
    );

    event BillCancelled(
        string indexed billId,
        address indexed admin,
        string reason
    );

    // 质押事件
    event BillPledged(
        string indexed billId,
        address indexed pledgee,
        uint256 amount,
        uint256 period
    );

    event BillUnpledged(
        string indexed billId,
        address indexed pledgee,
        uint256 amount
    );

    // 融资事件
    event BillFinancingApplied(
        string indexed billId,
        address indexed institution,
        uint256 amount,
        uint256 rate
    );

    event BillFinancingApproved(
        string indexed billId,
        address indexed institution,
        uint256 amount
    );

    event BillFinancingRepaid(
        string indexed billId,
        address indexed institution,
        uint256 amount
    );

    // 担保事件
    event BillGuaranteeAdded(
        string indexed billId,
        address indexed guarantor,
        uint256 amount
    );

    event BillGuaranteeRemoved(
        string indexed billId,
        address indexed guarantor,
        uint256 amount
    );

    // 追索事件
    event BillRecourseInitiated(
        string indexed billId,
        address indexed claimant,
        uint256 amount,
        string reason
    );

    event BillRecourseSettled(
        string indexed billId,
        address indexed claimant,
        uint256 amount
    );

    // 冻结事件
    event BillFrozen(
        string indexed billId,
        address indexed admin,
        string reason
    );

    event BillUnfrozen(
        string indexed billId,
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
        billCount = 0;

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

    // ==================== 1. 票据生命周期管理 ====================

    /**
     * @dev 签发票据（发行票据）
     * @param billId 票据ID
     * @param acceptor 承兑人地址
     * @param currentHolder 当前持票人地址
     * @param amount 票面金额
     * @param issueDate 开票日期（时间戳）
     * @param dueDate 到期日期（时间戳）
     * @param coreDataHash 核心数据哈希
     * @param extendedDataHash 扩展数据哈希
     * @return bool 是否成功
     */
    function issueBill(
        string memory billId,
        address acceptor,
        address currentHolder,
        uint256 amount,
        uint256 issueDate,
        uint256 dueDate,
        bytes32 coreDataHash,
        bytes32 extendedDataHash
    )
        external
        onlyJavaBackend
        returns (bool)
    {
        // Checks（检查）
        require(bytes32(coreDataHash) != bytes32(0), "Invalid core hash");
        require(bytes32(extendedDataHash) != bytes32(0), "Invalid extended hash");
        require(amount > 0, "Invalid amount");
        require(issueDate > 0, "Invalid issue date");
        require(dueDate > issueDate, "Invalid due date");
        require(acceptor != address(0), "Invalid acceptor");
        require(currentHolder != address(0), "Invalid holder");
        require(!billCores[billId].exists, "Bill already exists");

        // Effects（生效）
        BillCore storage bill = billCores[billId];
        bill.billId = billId;
        bill.issuer = msg.sender;
        bill.acceptor = acceptor;
        bill.currentHolder = currentHolder;
        bill.amount = amount;
        bill.issueDate = issueDate;
        bill.dueDate = dueDate;
        bill.status = BillStatus.ISSUED;
        bill.frozen = false;
        bill.exists = true;

        // 元数据
        billMetas[billId] = BillMeta({
            billId: billId,
            coreDataHash: coreDataHash,
            extendedDataHash: extendedDataHash,
            createdAt: block.timestamp,
            updatedAt: block.timestamp
        });

        // 更新计数器
        billCount++;

        // 更新索引
        issuedBills[msg.sender].push(billId);
        holdingBills[currentHolder].push(billId);

        // Events（事件）
        emit BillIssued(billId, msg.sender, currentHolder, amount, coreDataHash, extendedDataHash);

        return true;
    }

    /**
     * @dev 承兑票据
     * @param billId 票据ID
     * @return bool 是否成功
     */
    function acceptBill(string memory billId)
        external
        billExists(billId)
        onlyAcceptor(billId)
        returns (bool)
    {
        // Checks
        require(billCores[billId].status == BillStatus.ISSUED, "Invalid status for acceptance");

        // Effects
        billCores[billId].status = BillStatus.ENDORSED;
        billMetas[billId].updatedAt = block.timestamp;

        // Index
        acceptedBills[msg.sender].push(billId);

        // Event
        emit BillAccepted(billId, msg.sender, block.timestamp);

        return true;
    }

    /**
     * @dev 背书票据
     * @param billId 票据ID
     * @param endorsee 被背书人地址
     * @return bool 是否成功
     */
    function endorseBill(string memory billId, address endorsee)
        external
        billExists(billId)
        onlyHolder(billId)
        returns (bool)
    {
        // Checks
        require(endorsee != address(0), "Invalid endorsee");
        require(billCores[billId].status == BillStatus.ISSUED ||
                billCores[billId].status == BillStatus.ENDORSED,
                "Invalid status for endorsement");

        // Effects
        BillCore storage bill = billCores[billId];
        address previousHolder = bill.currentHolder;
        bill.currentHolder = endorsee;
        bill.status = BillStatus.ENDORSED;
        billMetas[billId].updatedAt = block.timestamp;

        // Index
        holdingBills[endorsee].push(billId);

        // History
        endorsementHistory[billId].push(EndorsementInfo({
            endorser: previousHolder,
            endorsee: endorsee,
            timestamp: block.timestamp
        }));

        // Event
        emit BillEndorsed(billId, previousHolder, endorsee, endorsementHistory[billId].length);

        return true;
    }

    /**
     * @dev 贴现票据
     * @param billId 票据ID
     * @param institution 贴现机构地址
     * @param amount 贴现金额
     * @param rate 贴现利率（基点）
     * @return bool 是否成功
     */
    function discountBill(
        string memory billId,
        address institution,
        uint256 amount,
        uint256 rate
    )
        external
        billExists(billId)
        onlyJavaBackend
        returns (bool)
    {
        // Checks
        require(institution != address(0), "Invalid institution");
        require(amount > 0, "Invalid discount amount");
        require(rate > 0, "Invalid discount rate");
        require(billCores[billId].status == BillStatus.ENDORSED, "Invalid status for discounting");
        require(billCores[billId].amount >= amount, "Insufficient amount");

        // Effects
        BillCore storage bill = billCores[billId];
        bill.status = BillStatus.DISCOUNTED;
        billMetas[billId].updatedAt = block.timestamp;

        // Event
        emit BillDiscounted(billId, institution, amount, rate);

        return true;
    }

    /**
     * @dev 付款
     * @param billId 票据ID
     * @return bool 是否成功
     */
    function payBill(string memory billId)
        external
        billExists(billId)
        returns (bool)
    {
        // Checks
        require(billCores[billId].status != BillStatus.PAID, "Already paid");

        // Effects
        BillCore storage bill = billCores[billId];
        bill.status = BillStatus.PAID;
        billMetas[billId].updatedAt = block.timestamp;

        // Event
        emit BillPaid(billId, bill.amount, msg.sender, block.timestamp);

        return true;
    }

    /**
     * @dev 作废票据
     * @param billId 票据ID
     * @param reason 作废原因
     * @return bool 是否成功
     */
    function cancelBill(string memory billId, string memory reason)
        external
        billExists(billId)
        onlyAdmin
        returns (bool)
    {
        // Effects
        billCores[billId].status = BillStatus.CANCELLED;
        billMetas[billId].updatedAt = block.timestamp;

        // Event
        emit BillCancelled(billId, msg.sender, reason);

        return true;
    }

    // ==================== 2. 质押管理 ====================

    /**
     * @dev 质押票据
     * @param billId 票据ID
     * @param pledgee 质押权人
     * @param amount 质押金额
     * @param period 质押期限（天）
     * @return bool 是否成功
     */
    function pledgeBill(
        string memory billId,
        address pledgee,
        uint256 amount,
        uint256 period
    )
        external
        billExists(billId)
        onlyJavaBackend
        returns (bool)
    {
        // Checks
        require(pledgee != address(0), "Invalid pledgee");
        require(amount > 0, "Invalid pledge amount");
        require(period > 0, "Invalid pledge period");
        require(!pledges[billId].active, "Already pledged");

        // Effects
        BillCore storage bill = billCores[billId];
        bill.status = BillStatus.PLEDGED;
        billMetas[billId].updatedAt = block.timestamp;

        pledges[billId] = Pledge({
            billId: billId,
            pledgee: pledgee,
            amount: amount,
            period: period,
            pledgeDate: block.timestamp,
            status: PledgeStatus.ACTIVE,
            active: true
        });

        // Event
        emit BillPledged(billId, pledgee, amount, period);

        return true;
    }

    /**
     * @dev 解除质押
     * @param billId 票据ID
     * @return bool 是否成功
     */
    function unpledgeBill(string memory billId)
        external
        billExists(billId)
        onlyJavaBackend
        returns (bool)
    {
        // Checks
        require(pledges[billId].active, "Not pledged");

        // Effects
        BillCore storage bill = billCores[billId];
        bill.status = BillStatus.ENDORSED;
        billMetas[billId].updatedAt = block.timestamp;

        pledges[billId].active = false;
        pledges[billId].status = PledgeStatus.RELEASED;

        // Event
        emit BillUnpledged(billId, pledges[billId].pledgee, pledges[billId].amount);

        return true;
    }

    // ==================== 3. 融资管理 ====================

    /**
     * @dev 申请融资
     * @param billId 票据ID
     * @param institution 融资机构
     * @param amount 融资金额
     * @param rate 融资利率（基点）
     * @return bool 是否成功
     */
    function applyFinancing(
        string memory billId,
        address institution,
        uint256 amount,
        uint256 rate
    )
        external
        billExists(billId)
        onlyHolder(billId)
        returns (bool)
    {
        // Checks
        require(institution != address(0), "Invalid institution");
        require(amount > 0, "Invalid financing amount");
        require(rate > 0, "Invalid rate");
        require(!financings[billId].active, "Already financed");

        // Effects
        financings[billId] = Financing({
            billId: billId,
            institution: institution,
            amount: amount,
            rate: rate,
            financingDate: block.timestamp,
            status: FinancingStatus.PENDING,
            active: true
        });

        // Event
        emit BillFinancingApplied(billId, institution, amount, rate);

        return true;
    }

    /**
     * @dev 批准融资
     * @param billId 票据ID
     * @return bool 是否成功
     */
    function approveFinancing(string memory billId)
        external
        billExists(billId)
        onlyJavaBackend
        returns (bool)
    {
        // Checks
        require(financings[billId].active, "No financing application");
        require(financings[billId].status == FinancingStatus.PENDING, "Not pending");

        // Effects
        BillCore storage bill = billCores[billId];
        bill.status = BillStatus.FINANCED;
        billMetas[billId].updatedAt = block.timestamp;

        financings[billId].status = FinancingStatus.APPROVED;

        // Event
        emit BillFinancingApproved(billId, financings[billId].institution, financings[billId].amount);

        return true;
    }

    /**
     * @dev 拒绝融资
     * @param billId 票据ID
     * @return bool 是否成功
     */
    function rejectFinancing(string memory billId)
        external
        billExists(billId)
        onlyJavaBackend
        returns (bool)
    {
        // Checks
        require(financings[billId].active, "No financing application");

        // Effects
        financings[billId].status = FinancingStatus.REJECTED;
        financings[billId].active = false;

        return true;
    }

    /**
     * @dev 偿还融资
     * @param billId 票据ID
     * @return bool 是否成功
     */
    function repayFinancing(string memory billId)
        external
        billExists(billId)
        onlyJavaBackend
        returns (bool)
    {
        // Checks
        require(financings[billId].active, "No active financing");

        // Effects
        BillCore storage bill = billCores[billId];
        bill.status = BillStatus.ENDORSED;
        billMetas[billId].updatedAt = block.timestamp;

        financings[billId].active = false;
        financings[billId].status = FinancingStatus.COMPLETED;

        // Event
        emit BillFinancingRepaid(billId, financings[billId].institution, financings[billId].amount);

        return true;
    }

    // ==================== 4. 担保管理 ====================

    /**
     * @dev 添加担保
     * @param billId 票据ID
     * @param guarantor 担保人
     * @param amount 担保金额
     * @return bool 是否成功
     */
    function addGuarantee(
        string memory billId,
        address guarantor,
        uint256 amount
    )
        external
        billExists(billId)
        returns (bool)
    {
        // Checks
        require(guarantor != address(0), "Invalid guarantor");
        require(amount > 0, "Invalid guarantee amount");

        // Effects
        guarantees[billId].push(Guarantee({
            billId: billId,
            guarantor: guarantor,
            amount: amount,
            guaranteeDate: block.timestamp,
            active: true
        }));

        // Event
        emit BillGuaranteeAdded(billId, guarantor, amount);

        return true;
    }

    /**
     * @dev 移除担保
     * @param billId 票据ID
     * @param guaranteeIndex 担保索引
     * @return bool 是否成功
     */
    function removeGuarantee(string memory billId, uint256 guaranteeIndex)
        external
        billExists(billId)
        returns (bool)
    {
        // Checks
        require(guaranteeIndex < guarantees[billId].length, "Invalid guarantee index");
        require(guarantees[billId][guaranteeIndex].active, "Guarantee not active");

        // Effects
        guarantees[billId][guaranteeIndex].active = false;

        // Event
        emit BillGuaranteeRemoved(
            billId,
            guarantees[billId][guaranteeIndex].guarantor,
            guarantees[billId][guaranteeIndex].amount
        );

        return true;
    }

    // ==================== 5. 追索管理 ====================

    /**
     * @dev 发起追索
     * @param billId 票据ID
     * @param amount 追索金额
     * @param reason 追索原因
     * @return bool 是否成功
     */
    function initiateRecourse(
        string memory billId,
        uint256 amount,
        string memory reason
    )
        external
        billExists(billId)
        returns (bool)
    {
        // Checks
        require(amount > 0, "Invalid recourse amount");
        require(bytes(reason).length > 0, "Invalid reason");
        require(recourses[billId].status == RecourseStatus.CANCELLED || bytes(recourses[billId].billId).length == 0, "Recourse already initiated");

        // Effects
        BillCore storage bill = billCores[billId];
        bill.status = BillStatus.FROZEN;
        billMetas[billId].updatedAt = block.timestamp;

        recourses[billId] = Recourse({
            billId: billId,
            claimant: msg.sender,
            amount: amount,
            reason: reason,
            recourseDate: block.timestamp,
            status: RecourseStatus.PENDING
        });

        // Event
        emit BillRecourseInitiated(billId, msg.sender, amount, reason);

        return true;
    }

    /**
     * @dev 结算追索
     * @param billId 票据ID
     * @return bool 是否成功
     */
    function settleRecourse(string memory billId)
        external
        billExists(billId)
        onlyJavaBackend
        returns (bool)
    {
        // Checks
        require(recourses[billId].status == RecourseStatus.APPROVED, "Recourse not approved");

        // Effects
        BillCore storage bill = billCores[billId];
        bill.status = BillStatus.SETTLED;
        billMetas[billId].updatedAt = block.timestamp;

        recourses[billId].status = RecourseStatus.SETTLED;

        // Event
        emit BillRecourseSettled(billId, recourses[billId].claimant, recourses[billId].amount);

        return true;
    }

    // ==================== 6. 冻结管理 ====================

    /**
     * @dev 冻结票据
     * @param billId 票据ID
     * @param reason 冻结原因
     * @return bool 是否成功
     */
    function freezeBill(string memory billId, string memory reason)
        external
        billExists(billId)
        onlyAdmin
        returns (bool)
    {
        // Effects
        billCores[billId].frozen = true;
        billMetas[billId].updatedAt = block.timestamp;
        frozenBills[billId] = true;

        // Event
        emit BillFrozen(billId, msg.sender, reason);

        return true;
    }

    /**
     * @dev 解除冻结
     * @param billId 票据ID
     * @param reason 解冻原因
     * @return bool 是否成功
     */
    function unfreezeBill(string memory billId, string memory reason)
        external
        billExists(billId)
        onlyAdmin
        returns (bool)
    {
        // Effects
        billCores[billId].frozen = false;
        billMetas[billId].updatedAt = block.timestamp;
        frozenBills[billId] = false;

        // Event
        emit BillUnfrozen(billId, msg.sender, reason);

        return true;
    }

    // ==================== 查询功能 ====================

    /**
     * @dev 查询票据完整信息（优化版 - 使用结构体避免栈溢出）
     * @param billId 票据ID
     * @return info 票据完整信息结构体
     */
    function getBill(string memory billId)
        external
        view
        returns (BillInfo memory info)
    {
        BillCore storage bill = billCores[billId];
        BillMeta storage meta = billMetas[billId];

        require(bill.exists, "Bill not found");

        return BillInfo({
            billId: bill.billId,
            issuer: bill.issuer,
            acceptor: bill.acceptor,
            currentHolder: bill.currentHolder,
            amount: bill.amount,
            issueDate: bill.issueDate,
            dueDate: bill.dueDate,
            status: bill.status,
            frozen: bill.frozen,
            coreDataHash: meta.coreDataHash,
            extendedDataHash: meta.extendedDataHash
        });
    }

    /**
     * @dev 查询票据核心数据
     * @param billId 票据ID
     * @return id 票据ID
     * @return issuer 出票人
     * @return acceptor 承兑人
     * @return currentHolder 当前持票人
     * @return amount 票面金额
     * @return status 状态
     * @return frozen 是否冻结
     */
    function getBillCore(string memory billId)
        external
        view
        returns (
            string memory id,
            address issuer,
            address acceptor,
            address currentHolder,
            uint256 amount,
            BillStatus status,
            bool frozen
        )
    {
        BillCore storage bill = billCores[billId];
        require(bill.exists, "Bill not found");

        return (
            bill.billId,
            bill.issuer,
            bill.acceptor,
            bill.currentHolder,
            bill.amount,
            bill.status,
            bill.frozen
        );
    }

    /**
     * @dev 查询票据元数据
     * @param billId 票据ID
     * @return coreDataHash 核心数据哈希
     * @return extendedDataHash 扩展数据哈希
     * @return createdAt 创建时间
     * @return updatedAt 更新时间
     */
    function getBillMeta(string memory billId)
        external
        view
        returns (
            bytes32 coreDataHash,
            bytes32 extendedDataHash,
            uint256 createdAt,
            uint256 updatedAt
        )
    {
        BillMeta storage meta = billMetas[billId];
        require(billCores[billId].exists, "Bill not found");

        return (
            meta.coreDataHash,
            meta.extendedDataHash,
            meta.createdAt,
            meta.updatedAt
        );
    }

    /**
     * @dev 查询质押信息
     * @param billId 票据ID
     * @return pledgee 质押权人
     * @return amount 质押金额
     * @return period 质押期限
     * @return status 质押状态
     * @return active 是否活跃
     */
    function getPledge(string memory billId)
        external
        view
        returns (
            address pledgee,
            uint256 amount,
            uint256 period,
            PledgeStatus status,
            bool active
        )
    {
        Pledge storage pledge = pledges[billId];
        require(billCores[billId].exists, "Bill not found");

        return (
            pledge.pledgee,
            pledge.amount,
            pledge.period,
            pledge.status,
            pledge.active
        );
    }

    /**
     * @dev 查询融资信息
     * @param billId 票据ID
     * @return institution 融资机构
     * @return amount 融资金额
     * @return rate 融资利率
     * @return status 融资状态
     * @return active 是否活跃
     */
    function getFinancing(string memory billId)
        external
        view
        returns (
            address institution,
            uint256 amount,
            uint256 rate,
            FinancingStatus status,
            bool active
        )
    {
        Financing storage financing = financings[billId];
        require(billCores[billId].exists, "Bill not found");

        return (
            financing.institution,
            financing.amount,
            financing.rate,
            financing.status,
            financing.active
        );
    }

    /**
     * @dev 查询担保信息数量
     * @param billId 票据ID
     * @return 担保数量
     */
    function getGuaranteeCount(string memory billId)
        external
        view
        returns (uint256)
    {
        require(billCores[billId].exists, "Bill not found");
        return guarantees[billId].length;
    }

    /**
     * @dev 查询追索信息
     * @param billId 票据ID
     * @return claimant 追索人
     * @return amount 追索金额
     * @return reason 追索原因
     * @return recourseDate 追索日期
     * @return status 追索状态
     */
    function getRecourse(string memory billId)
        external
        view
        returns (
            address claimant,
            uint256 amount,
            string memory reason,
            uint256 recourseDate,
            RecourseStatus status
        )
    {
        Recourse storage recourse = recourses[billId];
        require(billCores[billId].exists, "Bill not found");

        return (
            recourse.claimant,
            recourse.amount,
            recourse.reason,
            recourse.recourseDate,
            recourse.status
        );
    }

    /**
     * @dev 查询背书历史数量
     * @param billId 票据ID
     * @return 背书次数
     */
    function getEndorsementCount(string memory billId)
        external
        view
        returns (uint256)
    {
        require(billCores[billId].exists, "Bill not found");
        return endorsementHistory[billId].length;
    }

    /**
     * @dev 查询用户签发的票据
     * @param user 用户地址
     * @return 票据ID数组
     */
    function getIssuedBills(address user)
        external
        view
        returns (string[] memory)
    {
        return issuedBills[user];
    }

    /**
     * @dev 查询用户持有的票据
     * @param user 用户地址
     * @return 票据ID数组
     */
    function getHoldingBills(address user)
        external
        view
        returns (string[] memory)
    {
        return holdingBills[user];
    }

    /**
     * @dev 查询票据总数
     * @return 票据总数
     */
    function getBillCount()
        external
        view
        returns (uint256)
    {
        return billCount;
    }

    /**
     * @dev 检查票据是否存在
     * @param billId 票据ID
     * @return 是否存在
     */
    function isBillExists(string memory billId)
        external
        view
        returns (bool)
    {
        return billCores[billId].exists;
    }

    /**
     * @dev 检查票据是否冻结
     * @param billId 票据ID
     * @return 是否冻结
     */
    function isBillFrozen(string memory billId)
        external
        view
        returns (bool)
    {
        return billCores[billId].frozen;
    }

    /**
     * @dev 检查票据是否质押
     * @param billId 票据ID
     * @return 是否质押
     */
    function isBillPledged(string memory billId)
        external
        view
        returns (bool)
    {
        return pledges[billId].active;
    }

    /**
     * @dev 检查票据是否在融资
     * @param billId 票据ID
     * @return 是否在融资
     */
    function isBillFinancing(string memory billId)
        external
        view
        returns (bool)
    {
        return financings[billId].active;
    }

    /**
     * @dev 批量查询票据状态
     * @param billIds 票据ID数组
     * @return exists 是否存在数组
     * @return frozen 是否冻结数组
     */
    function getBillsStatus(string[] memory billIds)
        external
        view
        returns (bool[] memory exists, bool[] memory frozen)
    {
        uint256 length = billIds.length;
        bool[] memory existsArray = new bool[](length);
        bool[] memory frozenArray = new bool[](length);

        for (uint256 i = 0; i < length; i++) {
            existsArray[i] = billCores[billIds[i]].exists;
            frozenArray[i] = billCores[billIds[i]].frozen;
        }

        return (existsArray, frozenArray);
    }

    /**
     * @dev 批量查询票据持票人
     * @param billIds 票据ID数组
     * @return currentHolders 当前持票人数组
     */
    function getBillsHolders(string[] memory billIds)
        external
        view
        returns (address[] memory currentHolders)
    {
        uint256 length = billIds.length;
        address[] memory holdersArray = new address[](length);

        for (uint256 i = 0; i < length; i++) {
            holdersArray[i] = billCores[billIds[i]].currentHolder;
        }

        return holdersArray;
    }
}
