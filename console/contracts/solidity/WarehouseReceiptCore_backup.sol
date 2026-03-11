// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.0;

import "./IWarehouseReceiptCore.sol";

/**
 * @title WarehouseReceiptCore
 * @dev 仓单核心合约
 *
 * 管理仓单的全生命周期，包括创建、转移、锁定、解锁等核心操作
 *
 * 遵循数据上链规范：
 * - 明文上链：receiptId, weight, unit, quantity, status, storageDate, expiryDate, frozen
 * - 哈希化上链：ownerHash, warehouseHash
 * - 全摘要存证：goodsDetailHash, locationPhotoHash, contractHash
 *
 * 栈溢出防护：
 * - 使用 Struct 封装参数
 * - 使用 calldata 传参
 * - 批量操作限制
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
contract WarehouseReceiptCore is IWarehouseReceiptCore {

    // ==================== 常量定义 ====================

    uint256 constant MAX_BATCH_SIZE = 50;
    uint256 constant MAX_WEIGHT = 1000000000; // 最大重量 10亿吨
    uint256 constant STORAGE_DURATION_MIN = 1 days; // 最小存储期1天
    uint256 constant STORAGE_DURATION_MAX = 730 days; // 最大存储期2年

    // ==================== 状态变量 ====================

    address public admin;
    address public javaBackend;
    uint256 public receiptCount;
    uint256 public constant VERSION = 2;

    // ==================== 数据结构 ====================

    /**
     * @dev 仓单状态枚举
     * 遵循文档：1-在库, 2-待转让, 3-已拆分/合并, 4-已核销, 5-物流转运中
     */
    enum ReceiptStatus {
        None,           // 0-不存在
        InStorage,      // 1-在库
        PendingTransfer, // 2-待转让
        SplitMerged,    // 3-已拆分/合并
        Burned,         // 4-已核销
        InTransit       // 5-物流转运中
    }

    /**
     * @dev 仓单核心信息（明文上链字段）- 精简版
     */
    struct ReceiptCore {
        string receiptId;           // 仓单ID（公开检索）
        uint256 weight;            // 重量（业务对账）
        string unit;               // 单位
        uint256 quantity;          // 数量
        ReceiptStatus status;       // 状态
        uint256 storageDate;        // 入库日期
        uint256 expiryDate;         // 过期日期
        bool frozen;               // 冻结标志
    }

    /**
     * @dev 仓单哈希数据（哈希化上链字段）
     */
    struct ReceiptHashData {
        bytes32 ownerHash;         // 货主企业哈希
        bytes32 warehouseHash;     // 仓储企业哈希
    }

    /**
     * @dev 仓单全摘要数据（全摘要存证）
     */
    struct ReceiptEvidenceData {
        bytes32 goodsDetailHash;   // 货物详情哈希
        bytes32 locationPhotoHash;// 现场照片哈希
        bytes32 contractHash;      // 合同文件哈希
    }

    /**
     * @dev 仓单完整数据（使用子作用域避免栈溢出）
     */
    struct Receipt {
        ReceiptCore core;
        ReceiptHashData hashData;
        ReceiptEvidenceData evidence;
    }

    /**
     * @dev 仓单创建输入参数
     */
    struct ReceiptInput {
        string receiptId;
        bytes32 ownerHash;
        bytes32 warehouseHash;
        bytes32 goodsDetailHash;
        bytes32 locationPhotoHash;
        bytes32 contractHash;
        uint256 weight;
        string unit;
        uint256 quantity;
        uint256 storageDate;
        uint256 expiryDate;
    }

    // ==================== 存储层 ====================

    mapping(string => Receipt) private receipts;
    mapping(string => address) private receiptIdToOwner;
    mapping(address => string[]) private ownerToReceiptIds;
    mapping(string => bool) private receiptIdExists;

    // 元数据映射（用于避免栈溢出）
    mapping(string => string) private receiptParentId;
    mapping(string => string) private receiptRootId;
    mapping(string => uint256) private receiptCreatedAt;
    mapping(string => uint256) private receiptUpdatedAt;

    // ==================== 事件定义 ====================

    event ReceiptIssued(
        string indexed receiptId,
        bytes32 indexed ownerHash,
        bytes32 warehouseHash,
        uint256 weight,
        string unit,
        uint256 quantity,
        uint256 timestamp
    );

    event ReceiptTransferred(
        string indexed receiptId,
        address indexed from,
        address indexed to,
        bytes32 newOwnerHash,
        uint256 timestamp
    );

    event ReceiptLocked(
        string indexed receiptId,
        address indexed locker,
        uint256 timestamp
    );

    event ReceiptUnlocked(
        string indexed receiptId,
        address indexed unlocker,
        uint256 timestamp
    );

    event ReceiptDelivered(
        string indexed receiptId,
        address indexed deliveryRecipient,
        uint256 timestamp
    );

    event ReceiptCancelled(
        string indexed receiptId,
        address indexed canceller,
        string reason,
        uint256 timestamp
    );

    event ReceiptFrozen(
        string indexed receiptId,
        bool frozen,
        uint256 timestamp
    );

    // ==================== 修饰器 ====================

    modifier onlyAdmin() {
        require(msg.sender == admin, "Only admin");
        _;
    }

    modifier onlyJavaBackend() {
        require(msg.sender == javaBackend, "Only Java backend");
        _;
    }

    modifier onlyValidReceipt(string memory receiptId) {
        require(receiptIdExists[receiptId], "Receipt not found");
        _;
    }

    modifier onlyActiveReceipt(string memory receiptId) {
        Receipt storage r = receipts[receiptId];
        require(r.core.status == ReceiptStatus.InStorage || r.core.status == ReceiptStatus.InStorage, "Receipt not active");
        require(!r.core.frozen, "Receipt is frozen");
        _;
    }

    modifier onlyReceiptOwner(string memory receiptId) {
        require(
            receipts[receiptId].hashData.ownerHash == bytes32(uint256(uint160(msg.sender))),
            "Not owner"
        );
        _;
    }

    modifier onlyUnlocked(string memory receiptId) {
        require(!receipts[receiptId].core.frozen, "Receipt is locked");
        _;
    }

    // ==================== 构造函数 ====================

    constructor(address _initialAdmin) {
        require(_initialAdmin != address(0), "Admin cannot be zero");
        admin = _initialAdmin;
        javaBackend = _initialAdmin;
    }

    // ==================== 核心功能 ====================

    /**
     * @dev 开立仓单
     * @param input 仓单创建参数
     * @return success 是否成功
     */
    function issueReceipt(ReceiptInput calldata input)
        external onlyJavaBackend returns (bool success)
    {
        // 参数验证
        require(!receiptIdExists[input.receiptId], "Receipt ID exists");
        require(input.weight > 0 && input.weight <= MAX_WEIGHT, "Invalid weight");
        require(input.quantity > 0, "Invalid quantity");
        require(input.storageDate > 0, "Invalid storage date");
        require(input.expiryDate > input.storageDate, "Invalid expiry date");
        require(input.expiryDate <= input.storageDate + STORAGE_DURATION_MAX, "Expiry too far");
        require(input.ownerHash != bytes32(0), "Invalid owner hash");
        require(input.warehouseHash != bytes32(0), "Invalid warehouse hash");

        // 存储核心数据
        receipts[input.receiptId] = Receipt({
            core: ReceiptCore({
                receiptId: input.receiptId,
                weight: input.weight,
                unit: input.unit,
                quantity: input.quantity,
                status: ReceiptStatus.InStorage,
                storageDate: input.storageDate,
                expiryDate: input.expiryDate,
                frozen: false,
                parentId: "",
                rootId: "",
                createdAt: block.timestamp,
                updatedAt: block.timestamp
            }),
            hashData: ReceiptHashData({
                ownerHash: input.ownerHash,
                warehouseHash: input.warehouseHash
            }),
            evidence: ReceiptEvidenceData({
                goodsDetailHash: input.goodsDetailHash,
                locationPhotoHash: input.locationPhotoHash,
                contractHash: input.contractHash
            })
        });

        receiptIdToOwner[input.receiptId] = msg.sender;
        ownerToReceiptIds[msg.sender].push(input.receiptId);
        receiptIdExists[input.receiptId] = true;
        receiptCount++;

        emit ReceiptIssued(
            input.receiptId,
            input.ownerHash,
            input.warehouseHash,
            input.weight,
            input.unit,
            input.quantity,
            block.timestamp
        );

        return true;
    }

    /**
     * @dev 转移仓单所有权
     * @param receiptId 仓单ID
     * @param newOwnerHash 新货主哈希
     * @return success 是否成功
     */
    function transferReceipt(string calldata receiptId, bytes32 newOwnerHash)
        external onlyValidReceipt(receiptId) onlyActiveReceipt(receiptId) returns (bool success)
    {
        Receipt storage r = receipts[receiptId];
        address currentOwner = receiptIdToOwner[receiptId];

        require(msg.sender == currentOwner || msg.sender == admin, "Not authorized");
        require(newOwnerHash != bytes32(0), "Invalid owner hash");

        // 更新所有者
        r.hashData.ownerHash = newOwnerHash;
        r.core.updatedAt = block.timestamp;

        emit ReceiptTransferred(receiptId, currentOwner, address(0), newOwnerHash, block.timestamp);

        return true;
    }

    /**
     * @dev 锁定仓单（金融机构操作）
     * @param receiptId 仓单ID
     * @return success 是否成功
     */
    function lockReceipt(string calldata receiptId)
        external onlyValidReceipt(receiptId) onlyActiveReceipt(receiptId) returns (bool success)
    {
        Receipt storage r = receipts[receiptId];
        require(r.core.status != ReceiptStatus.InStorage, "Already locked");

        r.core.status = ReceiptStatus.InStorage;
        r.core.updatedAt = block.timestamp;

        emit ReceiptLocked(receiptId, msg.sender, block.timestamp);

        return true;
    }

    /**
     * @dev 解锁仓单
     * @param receiptId 仓单ID
     * @return success 是否成功
     */
    function unlockReceipt(string calldata receiptId)
        external onlyValidReceipt(receiptId) onlyActiveReceipt(receiptId) returns (bool success)
    {
        Receipt storage r = receipts[receiptId];
        require(r.core.status == ReceiptStatus.InStorage, "Not locked");

        r.core.status = ReceiptStatus.InStorage;
        r.core.updatedAt = block.timestamp;

        emit ReceiptUnlocked(receiptId, msg.sender, block.timestamp);

        return true;
    }

    /**
     * @dev 冻结/解冻仓单
     * @param receiptId 仓单ID
     * @param frozen 是否冻结
     * @return success 是否成功
     */
    function setFrozen(string calldata receiptId, bool frozen)
        external onlyAdmin onlyValidReceipt(receiptId) returns (bool success)
    {
        Receipt storage r = receipts[receiptId];
        r.core.frozen = frozen;
        r.core.updatedAt = block.timestamp;

        emit ReceiptFrozen(receiptId, frozen, block.timestamp);

        return true;
    }

    /**
     * @dev 注销仓单
     * @param receiptId 仓单ID
     * @param reason 注销原因
     * @return success 是否成功
     */
    function cancelReceipt(string calldata receiptId, string calldata reason)
        external onlyValidReceipt(receiptId) returns (bool success)
    {
        Receipt storage r = receipts[receiptId];
        require(r.core.status != ReceiptStatus.Burned, "Already cancelled");
        require(r.core.status != ReceiptStatus.InStorage, "Already delivered");

        // 只有所有者、管理员或仓库可注销
        address owner = receiptIdToOwner[receiptId];
        require(msg.sender == owner || msg.sender == admin || msg.sender == javaBackend, "Not authorized");

        r.core.status = ReceiptStatus.Burned;
        r.core.updatedAt = block.timestamp;

        emit ReceiptCancelled(receiptId, msg.sender, reason, block.timestamp);

        return true;
    }

    /**
     * @dev 核销仓单（提货出库）
     * @param receiptId 仓单ID
     * @param signatureHash 提货签名哈希
     * @return success 是否成功
     */
    function burnReceipt(string calldata receiptId, bytes32 signatureHash)
        external onlyValidReceipt(receiptId) returns (bool success)
    {
        Receipt storage r = receipts[receiptId];
        require(r.core.status != ReceiptStatus.Burned, "Already burned");
        require(r.core.status != ReceiptStatus.InTransit, "In transit");

        // 只有所有者、管理员或仓库可核销
        address owner = receiptIdToOwner[receiptId];
        require(msg.sender == owner || msg.sender == admin || msg.sender == javaBackend, "Not authorized");

        // 更新状态为已核销
        r.core.status = ReceiptStatus.Burned;
        r.core.updatedAt = block.timestamp;

        emit ReceiptCancelled(receiptId, msg.sender, "Burned - goods delivered", block.timestamp);

        return true;
    }

    // ==================== 查询功能 ====================

    /**
     * @dev 获取仓单完整信息
     * @param receiptId 仓单ID
     * @return _receiptId 仓单ID
     * @return ownerHash 货主哈希
     * @return warehouseHash 仓库哈希
     */
    function getReceipt(string calldata receiptId)
        external view onlyValidReceipt(receiptId) returns (
            string memory _receiptId,
            bytes32 ownerHash,
            bytes32 warehouseHash,
            bytes32 goodsDetailHash,
            bytes32 locationPhotoHash,
            bytes32 contractHash,
            uint256 weight,
            string memory unit,
            uint256 quantity,
            uint8 status,
            uint256 storageDate,
            uint256 expiryDate,
            bool frozen,
            string memory parentId,
            string memory rootId,
            uint256 createdAt,
            uint256 updatedAt
        )
    {
        Receipt storage r = receipts[receiptId];
        return (
            r.core.receiptId,
            r.hashData.ownerHash,
            r.hashData.warehouseHash,
            r.evidence.goodsDetailHash,
            r.evidence.locationPhotoHash,
            r.evidence.contractHash,
            r.core.weight,
            r.core.unit,
            r.core.quantity,
            uint8(r.core.status),
            r.core.storageDate,
            r.core.expiryDate,
            r.core.frozen,
            r.core.parentId,
            r.core.rootId,
            r.core.createdAt,
            r.core.updatedAt
        );
    }

    /**
     * @dev 获取仓单核心信息
     * @param receiptId 仓单ID
     * @return _receiptId 仓单ID
     * @return weight 重量
     * @return unit 单位
     * @return quantity 数量
     * @return status 状态
     * @return storageDate 入库日期
     * @return expiryDate 过期日期
     * @return frozen 冻结状态
     */
    function getReceiptCore(string calldata receiptId)
        external view onlyValidReceipt(receiptId) returns (
            string memory _receiptId,
            uint256 weight,
            string memory unit,
            uint256 quantity,
            uint8 status,
            uint256 storageDate,
            uint256 expiryDate,
            bool frozen
        )
    {
        Receipt storage r = receipts[receiptId];
        return (
            r.core.receiptId,
            r.core.weight,
            r.core.unit,
            r.core.quantity,
            uint8(r.core.status),
            r.core.storageDate,
            r.core.expiryDate,
            r.core.frozen
        );
    }

    /**
     * @dev 批量获取仓单ID列表
     * @param owner 所有者地址
     * @param offset 起始索引
     * @param limit 数量限制
     * @return 仓单ID列表
     */
    function getReceiptIdsByOwner(address owner, uint256 offset, uint256 limit)
        external view returns (string[] memory)
    {
        require(limit <= MAX_BATCH_SIZE, "Limit too large");
        string[] storage allIds = ownerToReceiptIds[owner];

        if (offset >= allIds.length) {
            return new string[](0);
        }

        uint256 resultLength = allIds.length - offset;
        if (resultLength > limit) {
            resultLength = limit;
        }

        string[] memory result = new string[](resultLength);
        for (uint256 i = 0; i < resultLength; i++) {
            result[i] = allIds[offset + i];
        }

        return result;
    }

    /**
     * @dev 检查仓单是否存在
     * @param receiptId 仓单ID
     * @return 是否存在
     */
    function exists(string calldata receiptId) external view returns (bool) {
        return receiptIdExists[receiptId];
    }

    /**
     * @dev 检查仓单是否有效（在库且未冻结）
     * @param receiptId 仓单ID
     * @return 是否有效
     */
    function isValid(string calldata receiptId) external view returns (bool) {
        if (!receiptIdExists[receiptId]) return false;
        Receipt storage r = receipts[receiptId];
        return (r.core.status == ReceiptStatus.InStorage || r.core.status == ReceiptStatus.InStorage) && !r.core.frozen;
    }

    // ==================== 拆分合并操作 ====================

    /**
     * @dev 拆分仓单
     * @param input 拆分参数
     * @return success 是否成功
     */
    function splitReceipt(IWarehouseReceiptCore.SplitInput calldata input)
        external onlyValidReceipt(input.originalReceiptId) onlyActiveReceipt(input.originalReceiptId) onlyReceiptOwner(input.originalReceiptId) onlyUnlocked(input.originalReceiptId) returns (bool success)
    {
        // 验证原仓单
        Receipt storage original = receipts[input.originalReceiptId];

        // 验证操作权限：必须在库且未冻结
        require(original.core.status == ReceiptStatus.InStorage, "Receipt not in storage");

        // 验证拆分参数
        require(input.newReceiptIds.length > 0, "Invalid split count");
        require(input.newReceiptIds.length <= 10, "Split count too large");
        require(input.newReceiptIds.length == input.weights.length, "Length mismatch");
        require(input.newReceiptIds.length == input.ownerHashes.length, "Length mismatch");

        // 验证重量守恒 - 使用子作用域减少栈深度
        uint256 totalWeight;
        {
            uint256[] memory weights = input.weights;
            for (uint256 i = 0; i < weights.length; i++) {
                require(weights[i] > 0, "Invalid weight");
                require(input.ownerHashes[i] != bytes32(0), "Invalid owner hash");
                require(!receiptIdExists[input.newReceiptIds[i]], "New receipt ID exists");
                totalWeight += weights[i];
            }
        }
        require(totalWeight == original.core.weight, "Weight not conserved");

        // 预提取共享数据到局部变量 - 使用子作用域
        string memory rootId;
        bytes32 warehouseHash;
        bytes32 goodsDetailHash;
        bytes32 locationPhotoHash;
        bytes32 contractHash;
        uint256 expiryDate;
        uint256 quantity;
        {
            rootId = original.core.rootId;
            if (bytes(rootId).length == 0) {
                rootId = input.originalReceiptId;
            }
            warehouseHash = original.hashData.warehouseHash;
            goodsDetailHash = original.evidence.goodsDetailHash;
            locationPhotoHash = original.evidence.locationPhotoHash;
            contractHash = original.evidence.contractHash;
            expiryDate = original.core.expiryDate;
            quantity = original.core.quantity;
        }

        // 创建新仓单 - 使用子作用域隔离
        uint256 timestamp = block.timestamp;
        uint256 originalWeight = original.core.weight;
        string memory unit = input.unit;
        {
            for (uint256 i = 0; i < input.newReceiptIds.length; i++) {
                receipts[input.newReceiptIds[i]] = Receipt({
                    core: ReceiptCore({
                        receiptId: input.newReceiptIds[i],
                        weight: input.weights[i],
                        unit: unit,
                        quantity: quantity * input.weights[i] / originalWeight,
                        status: ReceiptStatus.InStorage,
                        storageDate: timestamp,
                        expiryDate: expiryDate,
                        frozen: false,
                        parentId: input.originalReceiptId,
                        rootId: rootId,
                        createdAt: timestamp,
                        updatedAt: timestamp
                    }),
                    hashData: ReceiptHashData({
                        ownerHash: input.ownerHashes[i],
                        warehouseHash: warehouseHash
                    }),
                    evidence: ReceiptEvidenceData({
                        goodsDetailHash: goodsDetailHash,
                        locationPhotoHash: locationPhotoHash,
                        contractHash: contractHash
                    })
                });

                receiptIdExists[input.newReceiptIds[i]] = true;
                receiptIdToOwner[input.newReceiptIds[i]] = msg.sender;
                ownerToReceiptIds[msg.sender].push(input.newReceiptIds[i]);
                receiptCount++;
            }
        }

        // 更新原仓单状态为已拆分
        original.core.status = ReceiptStatus.SplitMerged;
        original.core.updatedAt = timestamp;

        emit ReceiptSplit(
            input.originalReceiptId,
            input.newReceiptIds,
            input.weights,
            timestamp
        );

        return true;
    }

    /**
     * @dev 合并仓单
     * @param input 合并参数
     * @return success 是否成功
     */
    function mergeReceipts(IWarehouseReceiptCore.MergeInput calldata input)
        external onlyJavaBackend returns (bool success)
    {
        // 验证合并参数
        require(input.sourceReceiptIds.length > 1, "Invalid merge count");
        require(input.sourceReceiptIds.length <= 10, "Merge count too large");
        require(!receiptIdExists[input.targetReceiptId], "Target receipt ID exists");
        require(input.targetOwnerHash != bytes32(0), "Invalid owner hash");

        // 验证所有源仓单
        uint256 totalWeight = 0;
        bytes32 firstOwnerHash;
        for (uint256 i = 0; i < input.sourceReceiptIds.length; i++) {
            string memory sourceId = input.sourceReceiptIds[i];
            require(receiptIdExists[sourceId], "Source receipt not found");

            Receipt storage source = receipts[sourceId];
            require(source.core.status == ReceiptStatus.InStorage, "Source not in storage");
            require(!source.core.frozen, "Source is frozen");

            // 验证所有源仓单属于同一持有人
            if (i == 0) {
                firstOwnerHash = source.hashData.ownerHash;
            } else {
                require(source.hashData.ownerHash == firstOwnerHash, "Owner mismatch");
            }

            // 验证调用者为持有人
            require(source.hashData.ownerHash == bytes32(uint256(uint160(msg.sender))), "Not owner");

            totalWeight += source.core.weight;
        }

        // 创建合并后的新仓单
        Receipt storage firstSource = receipts[input.sourceReceiptIds[0]];
        uint256 timestamp = block.timestamp;

        receipts[input.targetReceiptId] = Receipt({
            core: ReceiptCore({
                receiptId: input.targetReceiptId,
                weight: totalWeight,
                unit: input.unit,
                quantity: firstSource.core.quantity,
                status: ReceiptStatus.InStorage,
                storageDate: timestamp,
                expiryDate: firstSource.core.expiryDate,
                frozen: false,
                parentId: "",
                rootId: "",
                createdAt: timestamp,
                updatedAt: timestamp
            }),
            hashData: ReceiptHashData({
                ownerHash: input.targetOwnerHash,
                warehouseHash: firstSource.hashData.warehouseHash
            }),
            evidence: ReceiptEvidenceData({
                goodsDetailHash: firstSource.evidence.goodsDetailHash,
                locationPhotoHash: firstSource.evidence.locationPhotoHash,
                contractHash: firstSource.evidence.contractHash
            })
        });

        receiptIdExists[input.targetReceiptId] = true;
        receiptIdToOwner[input.targetReceiptId] = msg.sender;
        ownerToReceiptIds[msg.sender].push(input.targetReceiptId);
        receiptCount++;

        // 更新源仓单状态为已合并
        for (uint256 i = 0; i < input.sourceReceiptIds.length; i++) {
            receipts[input.sourceReceiptIds[i]].core.status = ReceiptStatus.SplitMerged;
            receipts[input.sourceReceiptIds[i]].core.updatedAt = timestamp;
        }

        emit ReceiptMerged(
            input.sourceReceiptIds,
            input.targetReceiptId,
            totalWeight,
            timestamp
        );

        return true;
    }

    /**
     * @dev 更新仓单所有者
     * @param receiptId 仓单ID
     * @param newOwnerHash 新所有者哈希
     * @return success 是否成功
     */
    function updateOwner(string calldata receiptId, bytes32 newOwnerHash)
        external onlyValidReceipt(receiptId) onlyReceiptOwner(receiptId) onlyUnlocked(receiptId) returns (bool success)
    {
        require(newOwnerHash != bytes32(0), "Invalid owner hash");

        Receipt storage r = receipts[receiptId];
        require(r.core.status == ReceiptStatus.InStorage, "Receipt not in storage");
        require(!r.core.frozen, "Receipt is frozen");

        r.hashData.ownerHash = newOwnerHash;
        r.core.updatedAt = block.timestamp;

        emit ReceiptTransferred(
            receiptId,
            receiptIdToOwner[receiptId],
            address(0),
            newOwnerHash,
            block.timestamp
        );

        return true;
    }

    /**
     * @dev 更新仓单状态
     * @param receiptId 仓单ID
     * @param newStatus 新状态
     * @return success 是否成功
     */
    function updateReceiptStatus(string calldata receiptId, uint8 newStatus)
        external onlyValidReceipt(receiptId) onlyReceiptOwner(receiptId) returns (bool success)
    {
        Receipt storage r = receipts[receiptId];
        uint8 oldStatus = uint8(r.core.status);
        r.core.status = ReceiptStatus(newStatus);
        r.core.updatedAt = block.timestamp;

        emit ReceiptStatusChanged(receiptId, oldStatus, newStatus, msg.sender, block.timestamp);

        return true;
    }

    /**
     * @dev 获取仓单重量
     * @param receiptId 仓单ID
     * @return weight 仓单重量
     */
    function getReceiptWeight(string calldata receiptId)
        external view onlyValidReceipt(receiptId) returns (uint256 weight)
    {
        return receipts[receiptId].core.weight;
    }

    /**
     * @dev 检查仓单是否可以操作
     * @param receiptId 仓单ID
     * @return canOperate 是否可以操作
     */
    function canOperate(string calldata receiptId)
        external view returns (bool canOperate)
    {
        if (!receiptIdExists[receiptId]) return false;
        Receipt storage r = receipts[receiptId];
        return r.core.status == ReceiptStatus.InStorage && !r.core.frozen;
    }

    /**
     * @dev 获取仓单所有者哈希
     * @param receiptId 仓单ID
     * @return ownerHash 仓单所有者哈希
     */
    function getOwnerHash(string calldata receiptId)
        external view onlyValidReceipt(receiptId) returns (bytes32 ownerHash)
    {
        return receipts[receiptId].hashData.ownerHash;
    }

    // ==================== 事件定义（重复部分） ====================

    event ReceiptSplit(
        string indexed originalReceiptId,
        string[] newReceiptIds,
        uint256[] weights,
        uint256 timestamp
    );

    event ReceiptMerged(
        string[] indexed sourceReceiptIds,
        string indexed targetReceiptId,
        uint256 totalWeight,
        uint256 timestamp
    );

    event ReceiptStatusChanged(
        string indexed receiptId,
        uint8 oldStatus,
        uint8 newStatus,
        address indexed operator,
        uint256 timestamp
    );

    // ==================== 管理员功能 ====================

    function setAdmin(address newAdmin) external onlyAdmin returns (bool) {
        require(newAdmin != address(0), "Invalid address");
        admin = newAdmin;
        return true;
    }

    function setJavaBackend(address newBackend) external onlyAdmin returns (bool) {
        require(newBackend != address(0), "Invalid address");
        javaBackend = newBackend;
        return true;
    }
}
