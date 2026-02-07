// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

/**
 * @title WarehouseReceiptWithFreeze
 * @dev 仓单融资管理智能合约 - 支持冻结/解冻功能
 */
contract WarehouseReceiptWithFreeze {
    // 仓单状态
    enum ReceiptStatus {
        Created,      // 已创建
        Verified,     // 已验证（仓库确认）
        Pledged,      // 已质押
        Financed,     // 已融资
        Released,     // 已释放
        Liquidated,   // 已清算
        Expired,      // 已过期
        Frozen        // 已冻结（新增）
    }

    // 冻结记录
    struct FreezeRecord {
        string receiptId;
        address freezer;           // 冻结操作人
        string freezeReason;       // 冻结原因
        uint256 freezeTime;        // 冻结时间
        string referenceNo;        // 相关文件编号
    }

    // 核心资产数据（链上）
    struct ReceiptCore {
        address owner;                 // 货主地址
        address warehouse;             // 仓库地址
        address financialInstitution;  // 金融机构地址
        uint256 totalPrice;           // 总价（分）
        uint256 storageDate;          // 入库日期
        uint256 expiryDate;           // 过期日期
        ReceiptStatus status;         // 状态
        uint256 pledgeAmount;         // 质押金额
        uint256 financeAmount;        // 融资金额
        bool exists;                  // 是否存在
    }

    // 元数据（链下数据指纹）
    struct ReceiptMeta {
        string receiptId;             // 仓单ID
        uint256 releaseDate;          // 释放日期
        uint256 financeRate;         // 融资利率
        uint256 financeDate;         // 融资日期
        bytes32 dataHash;             // 链下详细数据的哈希值
        uint256 createdAt;            // 创建时间
        uint256 updatedAt;            // 更新时间
        uint256 frozenAt;             // 冻结时间（新增）
    }

    // 质押记录
    struct PledgeRecord {
        string receiptId;
        address owner;
        address financialInstitution;
        uint256 amount;
        uint256 timestamp;
        string recordType;
    }

    // 转让记录
    struct TransferRecord {
        string receiptId;
        address from;
        address to;
        uint256 transferPrice;
        uint256 timestamp;
    }

    // 状态变量
    address public admin;
    uint256 public receiptCount;

    // 双层存储映射
    mapping(string => ReceiptCore) public receiptCores;
    mapping(string => ReceiptMeta) public receiptMetas;

    // 冻结记录映射
    mapping(string => FreezeRecord[]) public freezeHistory;

    // 辅助映射
    mapping(address => string[]) public ownerReceipts;
    mapping(address => string[]) public warehouseReceipts;
    mapping(address => string[]) public financierReceipts;
    mapping(string => PledgeRecord[]) public pledgeHistory;
    mapping(string => TransferRecord[]) public transferHistory;

    // 事件
    event ReceiptCreated(
        string indexed receiptId,
        address indexed owner,
        address indexed warehouse,
        uint256 totalPrice,
        bytes32 dataHash
    );

    event ReceiptVerified(
        string indexed receiptId,
        address indexed warehouse
    );

    event ReceiptPledged(
        string indexed receiptId,
        address indexed owner,
        address indexed financialInstitution,
        uint256 pledgeAmount
    );

    event ReceiptFinanced(
        string indexed receiptId,
        address indexed financialInstitution,
        uint256 financeAmount,
        uint256 financeRate
    );

    event ReceiptReleased(
        string indexed receiptId,
        uint256 releaseDate
    );

    event ReceiptLiquidated(
        string indexed receiptId,
        string reason
    );

    event ReceiptTransferred(
        string indexed receiptId,
        address indexed from,
        address indexed to,
        uint256 transferPrice,
        uint256 timestamp
    );

    // 冻结/解冻事件（新增）
    event ReceiptFrozen(
        string indexed receiptId,
        address indexed freezer,
        string freezeReason,
        string referenceNo,
        uint256 timestamp
    );

    event ReceiptUnfrozen(
        string indexed receiptId,
        address indexed unfreezer,
        ReceiptStatus newStatus,
        uint256 timestamp
    );

    // 修饰器
    modifier onlyAdmin() {
        require(msg.sender == admin, "Only admin can call this function");
        _;
    }

    modifier onlyExistingReceipt(string memory receiptId) {
        require(receiptCores[receiptId].exists, "Receipt does not exist");
        _;
    }

    modifier onlyOwner(string memory receiptId) {
        require(
            receiptCores[receiptId].owner == msg.sender,
            "Only owner can call this function"
        );
        _;
    }

    modifier onlyWarehouse(string memory receiptId) {
        require(
            receiptCores[receiptId].warehouse == msg.sender,
            "Only warehouse can call this function"
        );
        _;
    }

    modifier onlyFinancialInstitution(string memory receiptId) {
        require(
            receiptCores[receiptId].financialInstitution == msg.sender,
            "Only financial institution can call this function"
        );
        _;
    }

    /**
     * @dev 构造函数
     */
    constructor() {
        admin = msg.sender;
        receiptCount = 0;
    }

    /**
     * @dev 创建仓单
     */
    function createReceipt(
        string calldata receiptId,
        address warehouse,
        uint256 totalPrice,
        uint256 storageDate,
        uint256 expiryDate,
        bytes32 dataHash
    ) external returns (bool) {
        require(bytes(receiptId).length > 0, "Receipt ID cannot be empty");
        require(!receiptCores[receiptId].exists, "Receipt already exists");
        require(warehouse != address(0), "Invalid warehouse address");
        require(totalPrice > 0, "Total price must be greater than 0");
        require(expiryDate > storageDate, "Expiry date must be after storage date");

        ReceiptCore storage core = receiptCores[receiptId];
        core.owner = msg.sender;
        core.warehouse = warehouse;
        core.financialInstitution = address(0);
        core.totalPrice = totalPrice;
        core.storageDate = storageDate;
        core.expiryDate = expiryDate;
        core.status = ReceiptStatus.Created;
        core.pledgeAmount = 0;
        core.financeAmount = 0;
        core.exists = true;

        ReceiptMeta storage meta = receiptMetas[receiptId];
        meta.receiptId = receiptId;
        meta.releaseDate = 0;
        meta.financeRate = 0;
        meta.financeDate = 0;
        meta.dataHash = dataHash;
        meta.createdAt = block.timestamp;
        meta.updatedAt = block.timestamp;
        meta.frozenAt = 0;  // 初始化为0，表示未冻结

        ownerReceipts[msg.sender].push(receiptId);
        warehouseReceipts[warehouse].push(receiptId);
        receiptCount++;

        emit ReceiptCreated(receiptId, msg.sender, warehouse, totalPrice, dataHash);
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
        require(core.status == ReceiptStatus.Created, "Invalid status for verification");

        core.status = ReceiptStatus.Verified;
        receiptMetas[receiptId].updatedAt = block.timestamp;

        emit ReceiptVerified(receiptId, msg.sender);
        return true;
    }

    /**
     * @dev 质押仓单
     */
    function pledgeReceipt(
        string memory receiptId,
        address financialInstitution,
        uint256 pledgeAmount
    ) public onlyExistingReceipt(receiptId) returns (bool) {
        ReceiptCore storage core = receiptCores[receiptId];

        require(core.owner == msg.sender, "Not the owner");
        require(core.status == ReceiptStatus.Verified, "Receipt not verified");
        require(financialInstitution != address(0), "Invalid financial institution");
        require(pledgeAmount > 0 && pledgeAmount <= core.totalPrice, "Invalid pledge amount");

        core.status = ReceiptStatus.Pledged;
        core.financialInstitution = financialInstitution;
        core.pledgeAmount = pledgeAmount;
        receiptMetas[receiptId].updatedAt = block.timestamp;

        // 记录质押历史
        pledgeHistory[receiptId].push(PledgeRecord({
            receiptId: receiptId,
            owner: msg.sender,
            financialInstitution: financialInstitution,
            amount: pledgeAmount,
            timestamp: block.timestamp,
            recordType: "pledge"
        }));

        emit ReceiptPledged(receiptId, msg.sender, financialInstitution, pledgeAmount);
        return true;
    }

    /**
     * @dev 融资（基于质押的仓单）
     */
    function financeReceipt(
        string memory receiptId,
        uint256 financeAmount,
        uint256 financeRate
    ) public onlyExistingReceipt(receiptId) returns (bool) {
        ReceiptCore storage core = receiptCores[receiptId];

        require(
            core.financialInstitution == msg.sender,
            "Only financial institution can finance"
        );
        require(core.status == ReceiptStatus.Pledged, "Receipt not pledged");
        require(financeAmount > 0 && financeAmount <= core.pledgeAmount, "Invalid finance amount");
        require(financeRate <= 10000, "Finance rate too high");

        core.status = ReceiptStatus.Financed;
        core.financeAmount = financeAmount;
        receiptMetas[receiptId].financeRate = financeRate;
        receiptMetas[receiptId].financeDate = block.timestamp;
        receiptMetas[receiptId].updatedAt = block.timestamp;

        financierReceipts[msg.sender].push(receiptId);

        emit ReceiptFinanced(receiptId, msg.sender, financeAmount, financeRate);
        return true;
    }

    /**
     * @dev 释放仓单（还款后释放）
     */
    function releaseReceipt(string memory receiptId)
        public
        onlyExistingReceipt(receiptId)
        returns (bool)
    {
        ReceiptCore storage core = receiptCores[receiptId];

        require(
            msg.sender == core.owner || msg.sender == core.financialInstitution,
            "Not authorized to release"
        );
        require(core.status == ReceiptStatus.Financed, "Not in financed status");

        core.status = ReceiptStatus.Released;
        receiptMetas[receiptId].releaseDate = block.timestamp;
        receiptMetas[receiptId].updatedAt = block.timestamp;

        // 记录释放历史
        pledgeHistory[receiptId].push(PledgeRecord({
            receiptId: receiptId,
            owner: core.owner,
            financialInstitution: core.financialInstitution,
            amount: core.financeAmount,
            timestamp: block.timestamp,
            recordType: "release"
        }));

        emit ReceiptReleased(receiptId, block.timestamp);
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
            core.status == ReceiptStatus.Pledged || core.status == ReceiptStatus.Financed,
            "Invalid status for liquidation"
        );
        require(block.timestamp > core.expiryDate, "Not yet expired");

        core.status = ReceiptStatus.Liquidated;
        receiptMetas[receiptId].updatedAt = block.timestamp;

        // 记录清算历史
        pledgeHistory[receiptId].push(PledgeRecord({
            receiptId: receiptId,
            owner: core.owner,
            financialInstitution: core.financialInstitution,
            amount: core.pledgeAmount,
            timestamp: block.timestamp,
            recordType: "liquidate"
        }));

        emit ReceiptLiquidated(receiptId, reason);
        return true;
    }

    /**
     * @dev 转让仓单（背书转让）
     */
    function transferReceipt(
        string memory receiptId,
        address newOwner,
        uint256 transferPrice
    ) public onlyExistingReceipt(receiptId) returns (bool) {
        ReceiptCore storage core = receiptCores[receiptId];

        require(core.owner == msg.sender, "Only owner can transfer receipt");
        require(newOwner != address(0), "New owner cannot be zero address");
        require(newOwner != core.owner, "Cannot transfer to self");
        require(
            core.status == ReceiptStatus.Created || core.status == ReceiptStatus.Verified,
            "Receipt status does not allow transfer"
        );

        address previousOwner = core.owner;
        core.owner = newOwner;
        receiptMetas[receiptId].updatedAt = block.timestamp;

        // 添加到新货主列表
        ownerReceipts[newOwner].push(receiptId);

        // 记录转让历史
        transferHistory[receiptId].push(TransferRecord({
            receiptId: receiptId,
            from: previousOwner,
            to: newOwner,
            transferPrice: transferPrice,
            timestamp: block.timestamp
        }));

        emit ReceiptTransferred(receiptId, previousOwner, newOwner, transferPrice, block.timestamp);
        return true;
    }

    // ==================== 冻结/解冻功能（新增）====================

    /**
     * @dev 冻结仓单（仅管理员可调用）
     * @param receiptId 仓单ID
     * @param freezeReason 冻结原因
     * @param referenceNo 相关文件编号
     */
    function freezeReceipt(
        string memory receiptId,
        string memory freezeReason,
        string memory referenceNo
    ) public onlyAdmin onlyExistingReceipt(receiptId) returns (bool) {
        ReceiptCore storage core = receiptCores[receiptId];

        // 只有Verified、Pledged、Financed、Released状态可以冻结
        require(
            core.status == ReceiptStatus.Verified ||
            core.status == ReceiptStatus.Pledged ||
            core.status == ReceiptStatus.Financed ||
            core.status == ReceiptStatus.Released,
            "Receipt status does not allow freezing"
        );

        // 记录冻结前的状态
        ReceiptStatus previousStatus = core.status;

        // 更新状态为Frozen
        core.status = ReceiptStatus.Frozen;
        receiptMetas[receiptId].frozenAt = block.timestamp;
        receiptMetas[receiptId].updatedAt = block.timestamp;

        // 记录冻结历史
        freezeHistory[receiptId].push(FreezeRecord({
            receiptId: receiptId,
            freezer: msg.sender,
            freezeReason: freezeReason,
            freezeTime: block.timestamp,
            referenceNo: referenceNo
        }));

        emit ReceiptFrozen(receiptId, msg.sender, freezeReason, referenceNo, block.timestamp);
        return true;
    }

    /**
     * @dev 解冻仓单（仅管理员可调用）
     * @param receiptId 仓单ID
     * @param newStatus 解冻后的目标状态（Verified/Pledged/Financed/Released）
     */
    function unfreezeReceipt(
        string memory receiptId,
        ReceiptStatus newStatus
    ) public onlyAdmin onlyExistingReceipt(receiptId) returns (bool) {
        ReceiptCore storage core = receiptCores[receiptId];

        // 只有Frozen状态可以解冻
        require(core.status == ReceiptStatus.Frozen, "Receipt is not frozen");

        // 目标状态必须是有效的非冻结状态
        require(
            newStatus == ReceiptStatus.Verified ||
            newStatus == ReceiptStatus.Pledged ||
            newStatus == ReceiptStatus.Financed ||
            newStatus == ReceiptStatus.Released,
            "Invalid target status for unfreezing"
        );

        // 更新状态
        core.status = newStatus;
        receiptMetas[receiptId].frozenAt = 0;  // 清除冻结时间
        receiptMetas[receiptId].updatedAt = block.timestamp;

        emit ReceiptUnfrozen(receiptId, msg.sender, newStatus, block.timestamp);
        return true;
    }

    /**
     * @dev 获取仓单冻结历史记录数量
     */
    function getFreezeHistoryCount(string memory receiptId) public view returns (uint256) {
        return freezeHistory[receiptId].length;
    }

    /**
     * @dev 获取仓单冻结历史记录
     */
    function getFreezeHistory(string memory receiptId, uint256 index)
        public
        view
        returns (FreezeRecord memory)
    {
        require(index < freezeHistory[receiptId].length, "Index out of bounds");
        return freezeHistory[receiptId][index];
    }

    // ==================== 查询功能 ====================

    /**
     * @dev 获取仓单核心数据
     */
    function getReceiptCore(string memory receiptId)
        public
        view
        onlyExistingReceipt(receiptId)
        returns (ReceiptCore memory)
    {
        return receiptCores[receiptId];
    }

    /**
     * @dev 获取仓单元数据
     */
    function getReceiptMeta(string memory receiptId)
        public
        view
        onlyExistingReceipt(receiptId)
        returns (ReceiptMeta memory)
    {
        return receiptMetas[receiptId];
    }

    /**
     * @dev 获取货主的仓单列表
     */
    function getOwnerReceipts(address owner) public view returns (string[] memory) {
        return ownerReceipts[owner];
    }

    /**
     * @dev 获取仓库的仓单列表
     */
    function getWarehouseReceipts(address warehouse) public view returns (string[] memory) {
        return warehouseReceipts[warehouse];
    }

    /**
     * @dev 获取金融机构的仓单列表
     */
    function getFinancierReceipts(address financier) public view returns (string[] memory) {
        return financierReceipts[financier];
    }

    /**
     * @dev 获取质押历史记录数量
     */
    function getPledgeHistoryCount(string memory receiptId) public view returns (uint256) {
        return pledgeHistory[receiptId].length;
    }

    /**
     * @dev 转移管理员权限
     */
    function transferAdmin(address newAdmin) public onlyAdmin {
        require(newAdmin != address(0), "New admin cannot be zero address");
        admin = newAdmin;
    }
}
