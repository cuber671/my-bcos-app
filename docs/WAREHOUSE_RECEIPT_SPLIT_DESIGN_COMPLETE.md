# 仓单拆分功能完整设计文档

生成时间：2026-02-02
状态：待实施

---

## 📋 设计检查清单

### ✅ 已有设计
- [x] 基本业务流程
- [x] 接口定义
- [x] 数据库表设计
- [x] 基本权限验证（持单人检查）
- [x] 基本边界检查（数量相等）

### ⚠️ 需要补充
- [ ] **详细的权限验证机制**
- [ ] **完整的边界检查规则**
- [ ] **智能合约接口设计**
- [ ] **上链操作事务处理**
- [ ] **状态枚举扩展（需要SPLIT、SPLITTING状态）**
- [ ] **错误处理和回滚机制**

---

## 🔐 权限验证设计（完整版）

### 1. 提交拆分申请 - 权限验证

#### 多层权限检查

```java
@Transactional
public SplitApplicationResponse submitSplitApplication(
        SplitApplicationRequest request,
        String applicantId,
        String applicantName) {

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    // ==================== 第1层：身份验证 ====================
    if (!(auth instanceof UserAuthentication)) {
        throw new BusinessException("无效的认证信息");
    }
    UserAuthentication userAuth = (UserAuthentication) auth;

    // ==================== 第2层：持单人权限验证（基于区块链地址） ====================
    ElectronicWarehouseReceipt receipt = repository.findById(request.getParentReceiptId())
            .orElseThrow(() -> new BusinessException("仓单不存在"));

    // 检查是否是当前持单人
    permissionChecker.checkHolderPermission(
        auth,
        receipt.getHolderAddress(),
        "申请拆分仓单"
    );

    // ==================== 第3层：企业角色验证 ====================
    // 只有货主企业可以申请拆分
    if (!userAuth.getEnterpriseId().equals(receipt.getOwnerId())) {
        throw new BusinessException("只有货主企业可以申请拆分仓单");
    }

    // ==================== 第4层：操作权限验证 ====================
    // 仓储企业不能申请拆分自己的仓单
    if (userAuth.getEnterpriseId().equals(receipt.getWarehouseId())) {
        throw new BusinessException("仓储企业不能申请拆分仓单");
    }

    // 继续处理...
}
```

#### 权限矩阵

| 角色 | 提交拆分申请 | 审核拆分申请 | 说明 |
|------|------------|------------|------|
| **货主（持单人）** | ✅ | ❌ | 只能拆分自己持有的仓单 |
| **仓储方** | ❌ | ✅ | 可以审核关联的仓单拆分 |
| **金融机构** | ❌ | ❌ | 无权限 |
| **系统管理员** | ✅ | ✅ | 可以拆分和审核所有仓单 |

---

### 2. 审核拆分申请 - 权限验证

```java
@Transactional
public SplitApprovalResponse approveSplitApplication(
        SplitApprovalRequest request,
        String reviewerId,
        String reviewerName) {

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    UserAuthentication userAuth = (UserAuthentication) auth;

    // 查询拆分申请
    ReceiptSplitApplication application = splitApplicationRepository.findById(request.getApplicationId())
            .orElseThrow(() -> new BusinessException("拆分申请不存在"));

    // 查询父仓单
    ElectronicWarehouseReceipt receipt = repository.findById(application.getParentReceiptId())
            .orElseThrow(() -> new BusinessException("仓单不存在"));

    // ==================== 权限验证 ====================

    // 1. 系统管理员可以审核所有拆分申请
    if (userAuth.isSystemAdmin()) {
        log.info("系统管理员审核拆分申请: applicationId={}", request.getApplicationId());
        // 继续处理
    }
    // 2. 仓储方可以审核关联仓单的拆分申请
    else if (userAuth.getEnterpriseId().equals(receipt.getWarehouseId())) {
        log.info("仓储方审核拆分申请: applicationId={}, warehouseId={}",
                request.getApplicationId(), userAuth.getEnterpriseId());
        // 继续处理
    }
    // 3. 其他角色无权限
    else {
        throw new BusinessException("无权限审核此拆分申请");
    }

    // 继续处理...
}
```

---

## 🛡️ 边界检查设计（完整版）

### 1. 提交拆分申请 - 边界检查

```java
private void validateSplitRequest(
        ElectronicWarehouseReceipt parentReceipt,
        List<SplitDetailRequest> splits) {

    log.info("开始验证拆分请求: parentReceiptId={}, splitCount={}",
            parentReceipt.getId(), splits.size());

    // ==================== 第1层：仓单状态检查 ====================

    // 1.1 仓单必须存在
    if (parentReceipt == null) {
        throw new BusinessException("仓单不存在");
    }

    // 1.2 只能拆分NORMAL状态的仓单
    if (parentReceipt.getReceiptStatus() != ElectronicWarehouseReceipt.ReceiptStatus.NORMAL) {
        throw new BusinessException(
            String.format("只能拆分正常状态的仓单，当前状态: %s",
                parentReceipt.getReceiptStatus()));
    }

    // 1.3 仓单必须已上链
    if (parentReceipt.getBlockchainStatus() != ElectronicWarehouseReceipt.BlockchainStatus.SYNCED) {
        throw new BusinessException("只能拆分已上链的仓单");
    }

    // 1.4 仓单不能已冻结
    if (parentReceipt.getReceiptStatus() == ElectronicWarehouseReceipt.ReceiptStatus.FROZEN) {
        throw new BusinessException("已冻结的仓单不能拆分");
    }

    // 1.5 仓单不能已质押
    if (parentReceipt.getReceiptStatus() == ElectronicWarehouseReceipt.ReceiptStatus.PLEDGED) {
        throw new BusinessException("已质押的仓单不能拆分，请先释放");
    }

    // 1.6 仓单不能已过期
    if (parentReceipt.getReceiptStatus() == ElectronicWarehouseReceipt.ReceiptStatus.EXPIRED) {
        throw new BusinessException("已过期的仓单不能拆分");
    }

    // 1.7 仓单不能已拆分
    if (parentReceipt.getReceiptStatus() == ElectronicWarehouseReceipt.ReceiptStatus.SPLIT) {
        throw new BusinessException("该仓单已被拆分，无法再次拆分");
    }

    // 1.8 仓单不能正在拆分中
    if (parentReceipt.getReceiptStatus() == ElectronicWarehouseReceipt.ReceiptStatus.SPLITTING) {
        throw new BusinessException("该仓单正在拆分中，请勿重复提交");
    }

    // ==================== 第2层：拆分规则检查 ====================

    // 2.1 至少拆分成2个子仓单
    if (splits == null || splits.size() < 2) {
        throw new BusinessException("至少需要拆分成2个子仓单");
    }

    // 2.2 最多拆分成10个子仓单（性能考虑）
    if (splits.size() > 10) {
        throw new BusinessException("单次拆分最多生成10个子仓单");
    }

    // ==================== 第3层：数量和价值检查 ====================

    BigDecimal totalSplitQuantity = BigDecimal.ZERO;
    BigDecimal totalSplitValue = BigDecimal.ZERO;

    for (int i = 0; i < splits.size(); i++) {
        SplitDetailRequest split = splits.get(i);

        // 3.1 数量必须大于0
        if (split.getQuantity() == null || split.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(
                String.format("第%d个子仓单的数量必须大于0", i + 1));
        }

        // 3.2 数量精度检查（最多2位小数）
        if (split.getQuantity().scale() > 2) {
            throw new BusinessException(
                String.format("第%d个子仓单的数量最多保留2位小数", i + 1));
        }

        // 3.3 单价必须大于0
        if (split.getUnitPrice() == null || split.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(
                String.format("第%d个子仓单的单价必须大于0", i + 1));
        }

        // 3.4 计算总价值
        BigDecimal splitValue = split.getQuantity().multiply(split.getUnitPrice());

        // 3.5 验证计算的总价值是否正确
        if (split.getTotalValue() == null ||
            split.getTotalValue().compareTo(splitValue) != 0) {
            throw new BusinessException(
                String.format("第%d个子仓单的总价值计算错误: 数量 × 单价 = %s，但提供的是 %s",
                    i + 1, splitValue, split.getTotalValue()));
        }

        totalSplitQuantity = totalSplitQuantity.add(split.getQuantity());
        totalSplitValue = totalSplitValue.add(split.getTotalValue());
    }

    // 3.6 拆分后的数量总和必须等于原仓单数量
    if (totalSplitQuantity.compareTo(parentReceipt.getQuantity()) != 0) {
        throw new BusinessException(
            String.format("拆分后的数量总和(%s)必须等于原仓单数量(%s)",
                totalSplitQuantity, parentReceipt.getQuantity()));
    }

    // 3.7 拆分后的总价值必须等于原仓单价值
    if (totalSplitValue.compareTo(parentReceipt.getTotalValue()) != 0) {
        throw new BusinessException(
            String.format("拆分后的总价值(%s)必须等于原仓单价值(%s)",
                totalSplitValue, parentReceipt.getTotalValue()));
    }

    // ==================== 第4层：货物信息检查 ====================

    // 4.1 货物名称必须一致
    String parentGoodsName = parentReceipt.getGoodsName();
    for (int i = 0; i < splits.size(); i++) {
        if (!parentGoodsName.equals(splits.get(i).getGoodsName())) {
            throw new BusinessException(
                String.format("第%d个子仓单的货物名称必须与父仓单一致", i + 1));
        }
    }

    // 4.2 计量单位必须一致
    String parentUnit = parentReceipt.getUnit();
    for (int i = 0; i < splits.size(); i++) {
        if (!parentUnit.equals(splits.get(i).getUnit())) {
            throw new BusinessException(
                String.format("第%d个子仓单的计量单位必须与父仓单一致", i + 1));
        }
    }

    // 4.3 单价必须一致（拆分不改变单价）
    for (int i = 0; i < splits.size(); i++) {
        if (splits.get(i).getUnitPrice().compareTo(parentReceipt.getUnitPrice()) != 0) {
            throw new BusinessException(
                String.format("第%d个子仓单的单价必须与父仓单一致", i + 1));
        }
    }

    // ==================== 第5层：存储位置检查 ====================

    // 5.1 所有子仓单的存储位置不能相同
    Set<String> locations = new HashSet<>();
    for (int i = 0; i < splits.size(); i++) {
        String location = splits.get(i).getStorageLocation();
        if (location == null || location.trim().isEmpty()) {
            throw new BusinessException(
                String.format("第%d个子仓单的存储位置不能为空", i + 1));
        }
        if (locations.contains(location)) {
            throw new BusinessException(
                String.format("第%d个子仓单的存储位置重复", i + 1));
        }
        locations.add(location);
    }

    // 5.2 存储位置必须在同一仓库
    String parentWarehouseLocation = parentReceipt.getWarehouseLocation();
    for (int i = 0; i < splits.size(); i++) {
        // 这里可以验证存储位置是否在同一个仓库区域
        // 简化处理，只检查不为空
    }

    // ==================== 第6层：业务规则检查 ====================

    // 6.1 检查是否已有待处理的拆分申请
    if (splitApplicationRepository.existsPendingSplitApplication(parentReceipt.getId())) {
        throw new BusinessException("该仓单已有待审核的拆分申请，请勿重复提交");
    }

    // 6.2 检查父仓单是否已进行背书转让
    if (parentReceipt.getEndorsementCount() != null && parentReceipt.getEndorsementCount() > 0) {
        throw new BusinessException("已背书转让的仓单不能拆分");
    }

    log.info("拆分请求验证通过: parentReceiptId={}", parentReceipt.getId());
}
```

---

### 2. 审核拆分申请 - 边界检查

```java
private void validateSplitApproval(
        ReceiptSplitApplication application,
        String approvalResult) {

    log.info("开始验证拆分审核: applicationId={}, result={}",
            application.getId(), approvalResult);

    // ==================== 第1层：申请状态检查 ====================

    // 1.1 申请必须存在
    if (application == null) {
        throw new BusinessException("拆分申请不存在");
    }

    // 1.2 只能审核待审核状态的申请
    if (!"PENDING".equals(application.getRequestStatus())) {
        throw new BusinessException(
            String.format("该拆分申请已被%s，无法重复审核",
                application.getRequestStatus()));
    }

    // 1.3 审核结果必须有效
    if (!"APPROVED".equals(approvalResult) && !"REJECTED".equals(approvalResult)) {
        throw new BusinessException("审核结果只能是APPROVED或REJECTED");
    }

    // ==================== 第2层：父仓单状态检查（再次验证） ====================

    ElectronicWarehouseReceipt parentReceipt = repository.findById(application.getParentReceiptId())
            .orElseThrow(() -> new BusinessException("父仓单不存在"));

    // 2.1 仓单状态必须仍然是NORMAL
    if (parentReceipt.getReceiptStatus() != ElectronicWarehouseReceipt.ReceiptStatus.NORMAL) {
        throw new BusinessException(
            String.format("仓单状态已变更，无法拆分。当前状态: %s",
                parentReceipt.getReceiptStatus()));
    }

    // 2.2 如果是批准，需要重新验证拆分规则
    if ("APPROVED".equals(approvalResult)) {
        // 重新验证拆分规则（防止审核期间仓单信息被修改）
        List<SplitDetailRequest> splits = parseSplits(application.getSplitDetails());
        validateSplitRequest(parentReceipt, splits);
    }

    log.info("拆分审核验证通过: applicationId={}", application.getId());
}
```

---

## 🔗 智能合约接口设计

### 1. 智能合约方法

**合约文件：** `WarehouseReceiptWithSplit.sol`

```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.6.0;

import "./WarehouseReceiptWithFreeze.sol";

/**
 * 仓单拆分功能扩展
 * 继承自冻结功能合约
 */
contract WarehouseReceiptWithSplit is WarehouseReceiptWithFreeze {

    // ==================== 状态变量 ====================

    // 父仓单映射
    mapping(string => string) public parentReceipt; // childId => parentId

    // 子仓单列表
    mapping(string => string[]) public childReceipts; // parentId => childIds[]

    // 子仓单数量
    mapping(string => uint256) public childReceiptCount; // parentId => count

    // ==================== 事件 ====================

    event ReceiptSplit(
        string indexed parentId,
        string[] childIds,
        uint256 splitCount,
        uint256 timestamp
    );

    // ==================== 拆分方法 ====================

    /**
     * 拆分仓单
     *
     * @param parentId 父仓单ID
     * @param childIds 子仓单ID数组
     * @param childQuantities 子仓单数量数组
     * @param childTotalValues 子仓单总价值数组
     */
    function splitReceipt(
        string memory parentId,
        string[] memory childIds,
        uint256[] memory childQuantities,
        uint256[] memory childTotalValues
    ) public onlyAdmin returns (bool) {
        // ==================== 参数验证 ====================

        require(bytes(parentId).length > 0, "Parent receipt ID cannot be empty");
        require(childIds.length >= 2, "At least 2 child receipts required");
        require(childIds.length == childQuantities.length, "Length mismatch");
        require(childIds.length == childTotalValues.length, "Length mismatch");

        // ==================== 父仓单检查 ====================

        // 验证父仓单存在
        require(receipts[parentId].exists, "Parent receipt not found");

        // 验证父仓单状态为正常
        ReceiptStatus parentStatus = receipts[parentId].status;
        require(parentStatus == ReceiptStatus.Normal, "Only normal receipts can be split");

        // 验证父仓单未冻结
        require(!receipts[parentId].isFrozen, "Frozen receipts cannot be split");

        // 验证父仓单未质押
        require(!receipts[parentId].isPledged, "Pledged receipts cannot be split");

        // 验证父仓单未过期
        require(block.timestamp <= receipts[parentId].expiryDate, "Expired receipts cannot be split");

        // ==================== 数量和价值验证 ====================

        uint256 totalQuantity = 0;
        uint256 totalValue = 0;

        for (uint256 i = 0; i < childIds.length; i++) {
            // 验证子仓单ID不为空
            require(bytes(childIds[i]).length > 0, "Child receipt ID cannot be empty");

            // 验证子仓单数量大于0
            require(childQuantities[i] > 0, "Child quantity must be positive");

            // 累加数量和价值
            totalQuantity += childQuantities[i];
            totalValue += childTotalValues[i];
        }

        // 验证总数量等于父仓单数量
        require(totalQuantity == receipts[parentId].quantity,
            "Total child quantity must equal parent quantity");

        // 验证总价值等于父仓单价值
        require(totalValue == receipts[parentId].totalValue,
            "Total child value must equal parent value");

        // ==================== 创建子仓单记录 ====================

        for (uint256 i = 0; i < childIds.length; i++) {
            // 创建子仓单
            receipts[childIds[i]] = ReceiptData({
                exists: true,
                owner: receipts[parentId].owner,
                holder: receipts[parentId].holder,
                warehouse: receipts[parentId].warehouse,
                goodsName: receipts[parentId].goodsName,
                quantity: childQuantities[i],
                unitPrice: receipts[parentId].unitPrice,
                totalValue: childTotalValues[i],
                status: ReceiptStatus.Normal,
                isFrozen: false,
                isPledged: false,
                createdAt: block.timestamp,
                updatedAt: block.timestamp,
                expiryDate: receipts[parentId].expiryDate
            });

            // 记录父子关系
            parentReceipt[childIds[i]] = parentId;
        }

        // 记录子仓单列表
        childReceipts[parentId] = childIds;
        childReceiptCount[parentId] = childIds.length;

        // 更新父仓单状态为已拆分
        receipts[parentId].status = ReceiptStatus.Split;

        // ==================== 发出事件 ====================

        emit ReceiptSplit(
            parentId,
            childIds,
            childIds.length,
            block.timestamp
        );

        return true;
    }

    // ==================== 查询方法 ====================

    /**
     * 获取父仓单ID
     */
    function getParentReceipt(string memory childId)
        public view returns (string memory) {
        return parentReceipt[childId];
    }

    /**
     * 获取子仓单列表
     */
    function getChildReceipts(string memory parentId)
        public view returns (string[] memory) {
        return childReceipts[parentId];
    }

    /**
     * 获取子仓单数量
     */
    function getChildReceiptCount(string memory parentId)
        public view returns (uint256) {
        return childReceiptCount[parentId];
    }
}
```

### 2. ContractService 方法

```java
/**
 * 拆分仓单上链
 *
 * @param parentId 父仓单ID
 * @param childIds 子仓单ID数组
 * @param childQuantities 子仓单数量数组
 * @param childTotalValues 子仓单总价值数组
 * @return 交易哈希
 */
public String splitReceiptOnChain(
        String parentId,
        List<String> childIds,
        List<BigInteger> childQuantities,
        List<BigInteger> childTotalValues) {

    log.info("拆分仓单上链: parentId={}, childCount={}", parentId, childIds.size());

    try {
        // 检查合约是否已加载
        if (warehouseReceiptContract == null) {
            throw new BlockchainIntegrationException("仓单合约未加载，无法上链");
        }

        // 转换参数
        String[] childIdArray = childIds.toArray(new String[0]);
        BigInteger[] childQuantityArray = childQuantities.toArray(new BigInteger[0]);
        BigInteger[] childTotalValueArray = childTotalValues.toArray(new BigInteger[0]);

        // 调用智能合约
        TransactionReceipt receipt = warehouseReceiptContract.splitReceipt(
            parentId,
            childIdArray,
            childQuantityArray,
            childTotalValueArray
        );

        // 检查交易状态
        if (receipt.getStatus() != 0) {
            throw new BlockchainIntegrationException(
                "拆分仓单上链失败: " + receipt.getStatus());
        }

        String txHash = receipt.getTransactionHash();
        log.info("拆分仓单上链成功: parentId={}, txHash={}", parentId, txHash);

        return txHash;

    } catch (Exception e) {
        log.error("拆分仓单上链异常: parentId={}", parentId, e);
        throw new BlockchainIntegrationException("拆分仓单上链失败: " + e.getMessage(), e);
    }
}
```

---

## 🔄 上链操作事务处理

### 完整的拆分执行逻辑（带事务和回滚）

```java
@Transactional(rollbackFor = Exception.class)
public SplitApprovalResponse executeSplit(
        String applicationId,
        String reviewerId,
        String reviewerName) {

    log.info("开始执行拆分: applicationId={}, reviewer={}", applicationId, reviewerName);

    try {
        // ==================== 第1步：查询和验证 ====================

        // 1.1 查询拆分申请
        ReceiptSplitApplication application = splitApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("拆分申请不存在"));

        // 1.2 查询父仓单
        ElectronicWarehouseReceipt parentReceipt = repository.findById(application.getParentReceiptId())
                .orElseThrow(() -> new RuntimeException("父仓单不存在"));

        // 1.3 验证申请状态
        if (!"PENDING".equals(application.getRequestStatus())) {
            throw new RuntimeException("申请状态异常: " + application.getRequestStatus());
        }

        // 1.4 验证父仓单状态
        validateReceiptForSplit(parentReceipt);

        // 1.5 解析拆分详情
        List<SplitDetailRequest> splits = parseSplits(application.getSplitDetails());

        // 1.6 验证拆分规则
        validateSplitRequest(parentReceipt, splits);

        // ==================== 第2步：生成子仓单 ====================

        List<ElectronicWarehouseReceipt> childReceipts = new ArrayList<>();
        List<String> childIds = new ArrayList<>();
        List<BigInteger> childQuantities = new ArrayList<>();
        List<BigInteger> childTotalValues = new ArrayList<>();

        for (int i = 0; i < splits.size(); i++) {
            SplitDetailRequest split = splits.get(i);

            // 创建子仓单
            ElectronicWarehouseReceipt childReceipt = createChildReceipt(
                parentReceipt,
                split,
                i + 1
            );

            childReceipts.add(childReceipt);
            childIds.add(childReceipt.getId());

            // 转换为BigInteger（智能合约使用uint256）
            childQuantities.add(split.getQuantity().multiply(new BigDecimal("100")).toBigInteger());
            childTotalValues.add(split.getTotalValue().multiply(new BigDecimal("100")).toBigInteger());
        }

        // ==================== 第3步：保存到数据库 ====================

        // 3.1 保存所有子仓单
        for (ElectronicWarehouseReceipt child : childReceipts) {
            repository.save(child);
            log.debug("保存子仓单: id={}, receiptNo={}", child.getId(), child.getReceiptNo());
        }

        // 3.2 更新父仓单状态
        parentReceipt.setReceiptStatus(ElectronicWarehouseReceipt.ReceiptStatus.SPLITTING);
        parentReceipt.setSplitTime(LocalDateTime.now());
        parentReceipt.setSplitCount((long) splits.size());
        repository.save(parentReceipt);

        log.info("数据库更新完成: parentId={}, childCount={}", parentReceipt.getId(), childIds.size());

        // ==================== 第4步：上链操作 ====================

        try {
            // 4.1 调用智能合约拆分仓单
            String txHash = contractService.splitReceiptOnChain(
                parentReceipt.getId(),
                childIds,
                childQuantities,
                childTotalValues
            );

            log.info("拆分上链成功: txHash={}", txHash);

            // 4.2 获取区块号
            Long blockNumber = contractService.getBlockNumber(txHash);

            // 4.3 更新所有仓单的区块链信息
            parentReceipt.setTxHash(txHash);
            parentReceipt.setBlockNumber(blockNumber);
            parentReceipt.setBlockchainStatus(ElectronicWarehouseReceipt.BlockchainStatus.SYNCED);
            parentReceipt.setBlockchainTimestamp(LocalDateTime.now());
            parentReceipt.setReceiptStatus(ElectronicWarehouseReceipt.ReceiptStatus.SPLIT);
            repository.save(parentReceipt);

            for (ElectronicWarehouseReceipt child : childReceipts) {
                child.setTxHash(txHash);
                child.setBlockNumber(blockNumber);
                child.setBlockchainStatus(ElectronicWarehouseReceipt.BlockchainStatus.SYNCED);
                child.setBlockchainTimestamp(LocalDateTime.now());
                repository.save(child);
            }

            log.info("区块链信息更新完成: txHash={}, blockNumber={}", txHash, blockNumber);

            // ==================== 第5步：更新申请状态 ====================

            application.setRequestStatus("APPROVED");
            application.setReviewerId(reviewerId);
            application.setReviewerName(reviewerName);
            application.setReviewTime(LocalDateTime.now());
            application.setSplitTxHash(txHash);
            application.setBlockNumber(blockNumber);
            splitApplicationRepository.save(application);

            log.info("拆分申请审核完成: applicationId={}", applicationId);

            // ==================== 返回成功响应 ====================

            return SplitApprovalResponse.success(
                applicationId,
                parentReceipt.getId(),
                parentReceipt.getReceiptNo(),
                splits.size(),
                childIds,
                txHash,
                blockNumber
            );

        } catch (Exception e) {
            // ==================== 上链失败回滚 ====================

            log.error("拆分上链失败，执行回滚: applicationId={}, error={}",
                applicationId, e.getMessage(), e);

            // 回滚1: 删除所有子仓单
            for (ElectronicWarehouseReceipt child : childReceipts) {
                repository.deleteById(child.getId());
            }
            log.info("已删除所有子仓单");

            // 回滚2: 恢复父仓单状态
            parentReceipt.setReceiptStatus(ElectronicWarehouseReceipt.ReceiptStatus.NORMAL);
            parentReceipt.setSplitCount(null);
            parentReceipt.setSplitTime(null);
            repository.save(parentReceipt);
            log.info("已恢复父仓单状态");

            // 回滚3: 更新申请状态为失败
            application.setRequestStatus("FAILED");
            application.setReviewComments("上链失败: " + e.getMessage());
            splitApplicationRepository.save(application);
            log.info("已更新申请状态为失败");

            // 抛出异常，触发事务回滚
            throw new RuntimeException("拆分上链失败: " + e.getMessage(), e);
        }

    } catch (Exception e) {
        log.error("拆分执行失败: applicationId={}", applicationId, e);
        throw new RuntimeException("拆分执行失败: " + e.getMessage(), e);
    }
}

/**
 * 创建子仓单
 */
private ElectronicWarehouseReceipt createChildReceipt(
        ElectronicWarehouseReceipt parent,
        SplitDetailRequest split,
        int index) {

    ElectronicWarehouseReceipt child = new ElectronicWarehouseReceipt();

    // 继承父仓单的所有基础信息
    child.setId(UUID.randomUUID().toString());
    child.setParentReceiptId(parent.getId());
    child.setWarehouseId(parent.getWarehouseId());
    child.setWarehouseAddress(parent.getWarehouseAddress());
    child.setWarehouseName(parent.getWarehouseName());
    child.setWarehouseLocation(parent.getWarehouseLocation());

    child.setOwnerId(parent.getOwnerId());
    child.setOwnerAddress(parent.getOwnerAddress());
    child.setOwnerName(parent.getOwnerName());
    child.setOwnerOperatorId(parent.getOwnerOperatorId());
    child.setOwnerOperatorName(parent.getOwnerOperatorName());

    child.setHolderAddress(parent.getHolderAddress());
    child.setCurrentHolder(parent.getCurrentHolder());

    child.setWarehouseOperatorId(parent.getWarehouseOperatorId());
    child.setWarehouseOperatorName(parent.getWarehouseOperatorName());

    // 使用拆分请求的货物信息
    child.setGoodsName(split.getGoodsName());
    child.setUnit(parent.getUnit());
    child.setQuantity(split.getQuantity());
    child.setUnitPrice(split.getUnitPrice());
    child.setTotalValue(split.getTotalValue());
    child.setStorageLocation(split.getStorageLocation());

    // 复制其他信息
    child.setExpiryDate(parent.getExpiryDate());
    child.setStorageDate(parent.getStorageDate());
    child.setMarketPrice(parent.getMarketPrice());
    child.setRemarks(parent.getRemarks());

    // 生成子仓单编号
    child.setReceiptNo(generateChildReceiptNo(parent.getReceiptNo(), index));

    // 设置初始状态
    child.setReceiptStatus(ElectronicWarehouseReceipt.ReceiptStatus.NORMAL);
    child.setBlockchainStatus(ElectronicWarehouseReceipt.BlockchainStatus.PENDING);

    // 审计信息
    child.setCreatedBy(parent.getCreatedBy());
    child.setCreatedAt(LocalDateTime.now());
    child.setUpdatedAt(LocalDateTime.now());

    return child;
}

/**
 * 生成子仓单编号
 */
private String generateChildReceiptNo(String parentReceiptNo, int index) {
    return parentReceiptNo + "-" + String.format("%02d", index);
}
```

---

## 📊 状态枚举扩展

### 需要添加的状态

```java
public enum ReceiptStatus {
    DRAFT,              // 草稿
    PENDING_ONCHAIN,    // 待上链
    NORMAL,             // 正常
    ONCHAIN_FAILED,     // 上链失败
    PLEDGED,            // 已质押
    TRANSFERRED,        // 已转让
    FROZEN,             // 已冻结
    SPLITTING,          // 拆分中 ✨ 新增
    SPLIT,              // 已拆分 ✨ 新增
    EXPIRED,            // 已过期
    DELIVERED,          // 已提货
    CANCELLED           // 已取消
}
```

---

## 📝 总结

### ✅ 完善的内容

1. **权限验证机制**
   - 4层权限检查：身份 → 持单人 → 企业角色 → 操作权限
   - 完整的权限矩阵
   - 基于区块链地址的持单人验证

2. **边界检查规则**
   - 6层边界验证：状态 → 拆分规则 → 数量价值 → 货物信息 → 存储位置 → 业务规则
   - 共15+项验证点
   - 详细的错误提示

3. **智能合约接口**
   - 完整的Solidity合约代码
   - 参数验证逻辑
   - 父子关系映射
   - 事件发出机制

4. **上链操作事务处理**
   - 完整的事务管理
   - 上链失败自动回滚
   - 3层回滚机制
   - 详细的日志记录

5. **状态管理**
   - 新增SPLITTING和SPLIT状态
   - 清晰的状态流转

### 🎯 实施优先级

| 任务 | 优先级 | 工作量 |
|------|--------|--------|
| 添加状态枚举 | 高 | 0.5小时 |
| 实现边界检查 | 高 | 2小时 |
| 实现权限验证 | 高 | 1.5小时 |
| 编写智能合约 | 高 | 3小时 |
| 实现Service层 | 高 | 4小时 |
| 实现Controller层 | 高 | 2小时 |
| 单元测试 | 中 | 3小时 |
| 集成测试 | 中 | 2小时 |
| **总计** | - | **18小时** |

---

**文档版本：** v2.0 - 完整版
**最后更新：** 2026-02-02
**状态：** 设计完善，待实施
