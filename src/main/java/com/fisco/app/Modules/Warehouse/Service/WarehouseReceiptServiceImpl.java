package com.fisco.app.Modules.Warehouse.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fisco.app.Common.Constant.EntRoleConstant;
import com.fisco.app.Common.Utils.CurrentUser;
import com.fisco.app.Modules.Warehouse.Entity.ReceiptEndorsement;
import com.fisco.app.Modules.Warehouse.Entity.ReceiptOperationLog;
import com.fisco.app.Modules.Warehouse.Entity.StockOrder;
import com.fisco.app.Modules.Warehouse.Entity.Warehouse;
import com.fisco.app.Modules.Warehouse.Entity.WarehouseReceipt;
import com.fisco.app.Modules.Warehouse.Mapper.ReceiptEndorsementMapper;
import com.fisco.app.Modules.Warehouse.Mapper.ReceiptOperationLogMapper;
import com.fisco.app.Modules.Warehouse.Mapper.StockOrderMapper;
import com.fisco.app.Modules.Warehouse.Mapper.WarehouseMapper;
import com.fisco.app.Modules.Warehouse.Mapper.WarehouseReceiptMapper;

/**
 * 仓单业务服务实现类
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Service
public class WarehouseReceiptServiceImpl implements WarehouseReceiptService {

    @Autowired
    private WarehouseReceiptMapper warehouseReceiptMapper;

    @Autowired
    private ReceiptEndorsementMapper receiptEndorsementMapper;

    @Autowired
    private ReceiptOperationLogMapper receiptOperationLogMapper;

    @Autowired
    private WarehouseMapper warehouseMapper;

    @Autowired
    private StockOrderMapper stockOrderMapper;

    @Autowired
    private WarehouseReceiptContractService warehouseContractService;

    // ==================== 权限校验 ====================

    @Override
    public void checkWarehousePermission(Integer entRole) {
        // 管理员可绕过
        if (CurrentUser.isAdmin()) {
            return;
        }
        if (!EntRoleConstant.isWarehouse(entRole)) {
            throw new RuntimeException("无权限操作：仅仓储方可执行此操作");
        }
    }

    @Override
    public void checkFinancialPermission(Integer entRole) {
        // 管理员可绕过
        if (CurrentUser.isAdmin()) {
            return;
        }
        if (!EntRoleConstant.isFinancialInstitution(entRole)) {
            throw new RuntimeException("无权限操作：仅金融机构可执行此操作");
        }
    }

    @Override
    public void checkReceiptOwnerPermission(Long receiptId) {
        Long currentEntId = CurrentUser.getEntId();
        if (currentEntId == null) {
            throw new RuntimeException("未获取到当前用户企业信息");
        }

        WarehouseReceipt receipt = warehouseReceiptMapper.selectById(receiptId);
        if (receipt == null) {
            throw new RuntimeException("仓单不存在");
        }

        // 管理员可绕过
        if (!CurrentUser.isAdmin() && !receipt.getOwnerEntId().equals(currentEntId)) {
            throw new RuntimeException("无权限操作：非仓单持有人");
        }
    }

    @Override
    public void checkEndorsementTargetPermission(Long endorsementId) {
        Long currentEntId = CurrentUser.getEntId();
        if (currentEntId == null) {
            throw new RuntimeException("未获取到当前用户企业信息");
        }

        ReceiptEndorsement endorsement = receiptEndorsementMapper.selectById(endorsementId);
        if (endorsement == null) {
            throw new RuntimeException("背书记录不存在");
        }

        // 管理员可绕过
        if (!CurrentUser.isAdmin() && !endorsement.getTransfereeEntId().equals(currentEntId)) {
            throw new RuntimeException("无权限操作：仅被背书目标企业可确认");
        }
    }

    @Override
    public void checkEndorsementInitiatorPermission(Long endorsementId) {
        Long currentEntId = CurrentUser.getEntId();
        if (currentEntId == null) {
            throw new RuntimeException("未获取到当前用户企业信息");
        }

        ReceiptEndorsement endorsement = receiptEndorsementMapper.selectById(endorsementId);
        if (endorsement == null) {
            throw new RuntimeException("背书记录不存在");
        }

        // 管理员可绕过
        if (!CurrentUser.isAdmin() && !endorsement.getTransferorEntId().equals(currentEntId)) {
            throw new RuntimeException("无权限操作：仅背书发起方可撤回");
        }
    }

    @Override
    public void validateReceiptStatus(Integer status) {
        // 使用 ValidWarehouseReceiptStatus 注解的校验逻辑
        // 允许的状态值：1-在库, 2-待转让, 3-已拆分/合并, 4-已核销, 5-物流转运中
        if (status == null) {
            throw new IllegalArgumentException("仓单状态不能为空");
        }

        boolean valid = false;
        switch (status) {
            case WarehouseReceipt.STATUS_IN_STOCK:
            case WarehouseReceipt.STATUS_PENDING_TRANSFER:
            case WarehouseReceipt.STATUS_SPLIT_MERGED:
            case WarehouseReceipt.STATUS_BURNED:
            case WarehouseReceipt.STATUS_IN_TRANSIT:
                valid = true;
                break;
        }

        if (!valid) {
            throw new IllegalArgumentException(
                "无效的仓单状态: " + status + ", 有效值为: 1-在库, 2-待转让, 3-已拆分/合并, 4-已核销, 5-物流转运中"
            );
        }
    }

    // ==================== 入库单上链存证 ====================

    @Override
    public String calculateStockOrderHash(StockOrder stockOrder) {
        if (stockOrder == null) {
            throw new IllegalArgumentException("入库单不能为空");
        }

        try {
            // 构建待哈希数据字符串
            StringBuilder data = new StringBuilder();
            data.append(stockOrder.getWarehouseId() != null ? stockOrder.getWarehouseId().toString() : "");
            data.append(stockOrder.getEntId() != null ? stockOrder.getEntId().toString() : "");
            data.append(stockOrder.getUserId() != null ? stockOrder.getUserId().toString() : "");
            data.append(stockOrder.getGoodsName() != null ? stockOrder.getGoodsName() : "");
            data.append(stockOrder.getWeight() != null ? stockOrder.getWeight().toPlainString() : "");
            data.append(stockOrder.getUnit() != null ? stockOrder.getUnit() : "");
            data.append(stockOrder.getAttachmentUrl() != null ? stockOrder.getAttachmentUrl() : "");
            data.append(stockOrder.getCreateTime() != null ? stockOrder.getCreateTime().toString() : "");

            // 计算SHA-256哈希
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.toString().getBytes(StandardCharsets.UTF_8));

            // 转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256算法不可用", e);
        }
    }

    @Override
    public boolean saveStockOrderChainTxHash(Long stockOrderId, String chainTxHash) {
        if (stockOrderId == null) {
            throw new IllegalArgumentException("入库单ID不能为空");
        }

        StockOrder order = stockOrderMapper.selectById(stockOrderId);
        if (order == null) {
            throw new IllegalArgumentException("入库单不存在: " + stockOrderId);
        }

        order.setChainTxHash(chainTxHash);
        return stockOrderMapper.updateById(order) > 0;
    }

    // ==================== 入库单管理 ====================

    @Override
    @Transactional
    public Long applyStockIn(Long warehouseId, Long entId, Long userId, String goodsName,
            BigDecimal weight, String unit, String attachmentUrl) {
        StockOrder order = new StockOrder();
        order.setWarehouseId(warehouseId);
        order.setEntId(entId);
        order.setUserId(userId);
        order.setGoodsName(goodsName);
        order.setWeight(weight);
        order.setUnit(unit != null ? unit : "吨");
        order.setAttachmentUrl(attachmentUrl);
        order.setStatus(StockOrder.STATUS_PENDING);

        // 先插入以获取创建时间
        stockOrderMapper.insert(order);

        // 生成入库单编号（格式：STOCK + 日期 + 序号）
        String stockNo = "STOCK" + java.time.LocalDate.now().toString().replace("-", "")
                        + String.format("%03d", order.getId() % 1000);
        order.setStockNo(stockNo);
        stockOrderMapper.updateById(order);

        // 计算数据哈希（上链存证）
        String dataHash = calculateStockOrderHash(order);
        order.setDataHash(dataHash);
        stockOrderMapper.updateById(order);

        return order.getId();
    }

    @Override
    @Transactional
    public boolean confirmStockOrder(Long stockOrderId) {
        StockOrder order = stockOrderMapper.selectById(stockOrderId);
        if (order == null || order.getStatus() != StockOrder.STATUS_PENDING) {
            return false;
        }
        order.setStatus(StockOrder.STATUS_CONFIRMED);
        return stockOrderMapper.updateById(order) > 0;
    }

    @Override
    @Transactional
    public boolean cancelStockOrder(Long stockOrderId) {
        StockOrder order = stockOrderMapper.selectById(stockOrderId);
        if (order == null || order.getStatus() != StockOrder.STATUS_PENDING) {
            return false;
        }
        order.setStatus(StockOrder.STATUS_CANCELLED);
        return stockOrderMapper.updateById(order) > 0;
    }

    @Override
    public StockOrder getStockOrderById(Long stockOrderId) {
        return stockOrderMapper.selectById(stockOrderId);
    }

    @Override
    public StockOrder getStockOrderByStockNo(String stockNo) {
        if (stockNo == null || stockNo.isEmpty()) {
            return null;
        }
        return stockOrderMapper.selectOne(
            new LambdaQueryWrapper<StockOrder>()
                .eq(StockOrder::getStockNo, stockNo)
        );
    }

    @Override
    public List<StockOrder> getStockOrdersByEntId(Long entId) {
        return stockOrderMapper.selectList(
            new LambdaQueryWrapper<StockOrder>()
                .eq(StockOrder::getEntId, entId)
                .orderByDesc(StockOrder::getCreateTime)
        );
    }

    // ==================== 仓单签发 ====================

    @Override
    @Transactional
    public Long mintReceipt(Long stockOrderId, Long warehouseUserId, String onChainId) {
        // 1. 验证入库单状态
        StockOrder stockOrder = stockOrderMapper.selectById(stockOrderId);
        if (stockOrder == null || stockOrder.getStatus() != StockOrder.STATUS_CONFIRMED) {
            throw new RuntimeException("入库单不存在或未确认");
        }

        // 如果未提供onChainId，自动生成
        if (onChainId == null || onChainId.isBlank()) {
            onChainId = "WR" + System.currentTimeMillis();
        }

        // 2. 调用区块链签发仓单
        try {
            // 生成各字段哈希
            byte[] ownerHash = generateHash(stockOrder.getEntId().toString());
            byte[] warehouseHash = generateHash(stockOrder.getWarehouseId().toString());
            String goodsDetail = stockOrder.getGoodsName() + "|" + stockOrder.getWeight() + "|" + stockOrder.getUnit();
            byte[] goodsDetailHash = generateHash(goodsDetail);
            byte[] locationPhotoHash = stockOrder.getAttachmentUrl() != null
                ? generateHash(stockOrder.getAttachmentUrl())
                : new byte[32];
            byte[] contractHash = generateHash(stockOrderId.toString());

            // 转换重量和日期
            BigDecimal weight = stockOrder.getWeight();
            BigInteger weightBI = weight != null ? weight.toBigInteger() : BigInteger.ZERO;
            BigInteger quantity = BigInteger.ONE;
            BigInteger storageDate = BigInteger.valueOf(Instant.now().getEpochSecond());
            BigInteger expiryDate = BigInteger.valueOf(
                LocalDate.now().plusYears(1).atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
            );

            // 调用区块链签发
            TransactionReceipt txReceipt = warehouseContractService.issueReceipt(
                onChainId,
                ownerHash,
                warehouseHash,
                goodsDetailHash,
                locationPhotoHash,
                contractHash,
                weightBI,
                stockOrder.getUnit() != null ? stockOrder.getUnit() : "吨",
                quantity,
                storageDate,
                expiryDate
            );

            if (txReceipt == null) {
                throw new RuntimeException("区块链签发仓单失败");
            }
        } catch (Exception e) {
            throw new RuntimeException("调用区块链签发仓单异常: " + e.getMessage());
        }

        // 3. 创建仓单记录
        WarehouseReceipt receipt = new WarehouseReceipt();
        receipt.setWarehouseId(stockOrder.getWarehouseId());
        receipt.setOnChainId(onChainId);
        receipt.setOwnerEntId(stockOrder.getEntId());
        receipt.setOwnerUserId(stockOrder.getUserId());
        receipt.setWarehouseEntId(stockOrder.getEntId()); // 仓储方为申请企业
        receipt.setWarehouseUserId(warehouseUserId);
        receipt.setGoodsName(stockOrder.getGoodsName());
        receipt.setWeight(stockOrder.getWeight());
        receipt.setUnit(stockOrder.getUnit());
        receipt.setParentId(0L);
        receipt.setRootId(0L);
        receipt.setIsLocked(false);
        receipt.setStatus(WarehouseReceipt.STATUS_IN_STOCK);

        warehouseReceiptMapper.insert(receipt);
        return receipt.getId();
    }

    @Override
    public WarehouseReceipt getReceiptById(Long receiptId) {
        return warehouseReceiptMapper.selectById(receiptId);
    }

    @Override
    public WarehouseReceipt getReceiptByOnChainId(String onChainId) {
        return warehouseReceiptMapper.selectOne(
            new LambdaQueryWrapper<WarehouseReceipt>()
                .eq(WarehouseReceipt::getOnChainId, onChainId)
        );
    }

    @Override
    public List<WarehouseReceipt> getReceiptsByEntId(Long entId) {
        return warehouseReceiptMapper.selectList(
            new LambdaQueryWrapper<WarehouseReceipt>()
                .eq(WarehouseReceipt::getOwnerEntId, entId)
                .orderByDesc(WarehouseReceipt::getCreateTime)
        );
    }

    @Override
    public List<WarehouseReceipt> getInStockReceipts(Long entId) {
        return warehouseReceiptMapper.selectList(
            new LambdaQueryWrapper<WarehouseReceipt>()
                .eq(WarehouseReceipt::getOwnerEntId, entId)
                .eq(WarehouseReceipt::getStatus, WarehouseReceipt.STATUS_IN_STOCK)
                .orderByDesc(WarehouseReceipt::getCreateTime)
        );
    }

    // ==================== 背书转让 ====================

    @Override
    @Transactional
    public Long launchEndorsement(Long receiptId, Long transferorUserId, Long transfereeEntId,
            String signatureHash) {
        // 1. 验证仓单状态
        if (transfereeEntId == null) {
            throw new RuntimeException("受让方企业ID不能为空");
        }

        WarehouseReceipt receipt = warehouseReceiptMapper.selectById(receiptId);
        if (receipt == null) {
            throw new RuntimeException("仓单不存在");
        }
        if (receipt.getIsLocked()) {
            throw new RuntimeException("仓单已锁定，无法转让");
        }
        if (receipt.getStatus() != WarehouseReceipt.STATUS_IN_STOCK) {
            throw new RuntimeException("仓单状态不允许转让");
        }

        // 2. 创建背书记录
        ReceiptEndorsement endorsement = new ReceiptEndorsement();
        endorsement.setReceiptId(receiptId);
        endorsement.setTransferorEntId(receipt.getOwnerEntId());
        endorsement.setTransferorUserId(transferorUserId);
        endorsement.setTransfereeEntId(transfereeEntId);
        endorsement.setSignatureHash(signatureHash);
        endorsement.setStatus(ReceiptEndorsement.STATUS_PENDING);

        receiptEndorsementMapper.insert(endorsement);

        // 3. 更新仓单状态为待转让
        receipt.setStatus(WarehouseReceipt.STATUS_PENDING_TRANSFER);
        warehouseReceiptMapper.updateById(receipt);

        return endorsement.getId();
    }

    @Override
    @Transactional
    public boolean confirmEndorsement(Long endorsementId, Long transfereeUserId, boolean accept) {
        ReceiptEndorsement endorsement = receiptEndorsementMapper.selectById(endorsementId);
        if (endorsement == null || endorsement.getStatus() != ReceiptEndorsement.STATUS_PENDING) {
            return false;
        }

        if (accept) {
            // 1. 校验仓单存在性
            WarehouseReceipt receipt = warehouseReceiptMapper.selectById(endorsement.getReceiptId());
            if (receipt == null) {
                throw new RuntimeException("仓单不存在");
            }
            // 校验仓单已上链
            if (receipt.getOnChainId() == null || receipt.getOnChainId().isEmpty()) {
                throw new RuntimeException("仓单未上链，无法进行背书确认");
            }

            // 2. 调用区块链背书确认 (使用 Ops 合约)
            try {
                byte[] fromHash = generateHash(endorsement.getTransferorEntId().toString());
                byte[] toHash = generateHash(endorsement.getTransfereeEntId().toString());
                TransactionReceipt txReceipt = warehouseContractService.confirmEndorsement(
                    receipt.getOnChainId(),
                    fromHash,
                    toHash
                );
                if (txReceipt == null) {
                    throw new RuntimeException("区块链背书确认失败");
                }
            } catch (Exception e) {
                throw new RuntimeException("调用区块链背书确认异常: " + e.getMessage());
            }

            // 3. 更新背书记录
            endorsement.setTransfereeUserId(transfereeUserId);
            endorsement.setStatus(ReceiptEndorsement.STATUS_CONFIRMED);
            receiptEndorsementMapper.updateById(endorsement);

            // 4. 更新仓单所有者
            receipt.setOwnerEntId(endorsement.getTransfereeEntId());
            receipt.setOwnerUserId(transfereeUserId);
            receipt.setStatus(WarehouseReceipt.STATUS_IN_STOCK);
            warehouseReceiptMapper.updateById(receipt);
        } else {
            endorsement.setStatus(ReceiptEndorsement.STATUS_REJECTED);
            receiptEndorsementMapper.updateById(endorsement);

            // 恢复仓单状态
            WarehouseReceipt receipt = warehouseReceiptMapper.selectById(endorsement.getReceiptId());
            receipt.setStatus(WarehouseReceipt.STATUS_IN_STOCK);
            warehouseReceiptMapper.updateById(receipt);
        }

        return true;
    }

    @Override
    @Transactional
    public boolean revokeEndorsement(Long endorsementId) {
        ReceiptEndorsement endorsement = receiptEndorsementMapper.selectById(endorsementId);
        if (endorsement == null || endorsement.getStatus() != ReceiptEndorsement.STATUS_PENDING) {
            return false;
        }

        endorsement.setStatus(ReceiptEndorsement.STATUS_REVOKED);
        receiptEndorsementMapper.updateById(endorsement);

        // 恢复仓单状态
        WarehouseReceipt receipt = warehouseReceiptMapper.selectById(endorsement.getReceiptId());
        if (receipt != null) {
            receipt.setStatus(WarehouseReceipt.STATUS_IN_STOCK);
            warehouseReceiptMapper.updateById(receipt);
        }

        return true;
    }

    @Override
    public List<ReceiptEndorsement> getEndorsementsByReceiptId(Long receiptId) {
        return receiptEndorsementMapper.selectList(
            new LambdaQueryWrapper<ReceiptEndorsement>()
                .eq(ReceiptEndorsement::getReceiptId, receiptId)
                .orderByDesc(ReceiptEndorsement::getCreateTime)
        );
    }

    // ==================== 拆分/合并 ====================

    @Override
    @Transactional
    public Long applySplit(Long receiptId, Long applyUserId, BigDecimal[] targetWeights) {
        WarehouseReceipt receipt = warehouseReceiptMapper.selectById(receiptId);
        if (receipt == null) {
            throw new RuntimeException("仓单不存在");
        }
        if (receipt.getIsLocked()) {
            throw new RuntimeException("仓单已锁定，无法拆分");
        }

        // 验证总重量一致
        BigDecimal total = Arrays.stream(targetWeights).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(receipt.getWeight()) != 0) {
            throw new RuntimeException("拆分后总重量与原仓单不一致");
        }

        // 创建拆分记录
        ReceiptOperationLog opLog = new ReceiptOperationLog();
        opLog.setOpType(ReceiptOperationLog.OP_TYPE_SPLIT);
        opLog.setSourceReceiptIds(receiptId.toString());
        opLog.setTotalWeight(receipt.getWeight());
        opLog.setApplyEntId(receipt.getOwnerEntId());
        opLog.setApplyUserId(applyUserId);
        opLog.setStatus(ReceiptOperationLog.STATUS_PENDING);

        receiptOperationLogMapper.insert(opLog);

        // 更新原仓单状态
        receipt.setStatus(WarehouseReceipt.STATUS_SPLIT_MERGED);
        warehouseReceiptMapper.updateById(receipt);

        return opLog.getId();
    }

    @Override
    @Transactional
    public Long applyMerge(List<Long> receiptIds, Long applyUserId) {
        if (receiptIds == null || receiptIds.size() < 2) {
            throw new RuntimeException("合并需要至少2个仓单");
        }

        List<WarehouseReceipt> receipts = warehouseReceiptMapper.selectBatchIds(receiptIds);
        if (receipts.size() != receiptIds.size()) {
            throw new RuntimeException("部分仓单不存在");
        }

        // 验证所有仓单可合并
        BigDecimal totalWeight = BigDecimal.ZERO;
        Long ownerEntId = receipts.get(0).getOwnerEntId();
        for (WarehouseReceipt r : receipts) {
            if (!r.getOwnerEntId().equals(ownerEntId)) {
                throw new RuntimeException("仓单不属于同一企业");
            }
            if (r.getIsLocked()) {
                throw new RuntimeException("仓单" + r.getId() + "已锁定");
            }
            totalWeight = totalWeight.add(r.getWeight());
        }

        // 创建合并记录
        ReceiptOperationLog opLog = new ReceiptOperationLog();
        opLog.setOpType(ReceiptOperationLog.OP_TYPE_MERGE);
        opLog.setSourceReceiptIds(receiptIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        opLog.setTargetReceiptIds("");
        opLog.setTotalWeight(totalWeight);
        opLog.setApplyEntId(ownerEntId);
        opLog.setApplyUserId(applyUserId);
        opLog.setStatus(ReceiptOperationLog.STATUS_PENDING);

        receiptOperationLogMapper.insert(opLog);

        // 更新原仓单状态
        for (WarehouseReceipt r : receipts) {
            r.setStatus(WarehouseReceipt.STATUS_SPLIT_MERGED);
            warehouseReceiptMapper.updateById(r);
        }

        return opLog.getId();
    }

    @Override
    @Transactional
    public boolean executeSplitMerge(Long opLogId, Long executeUserId, boolean execute) {
        ReceiptOperationLog opLog = receiptOperationLogMapper.selectById(opLogId);
        if (opLog == null || opLog.getStatus() != ReceiptOperationLog.STATUS_PENDING) {
            return false;
        }

        if (execute) {
            // 调用区块链记录拆分/合并事件（链上存证）
            // 注意：实际拆分/合并由应用层处理，链上仅记录事件存证
            opLog.setExecuteUserId(executeUserId);
            opLog.setStatus(ReceiptOperationLog.STATUS_COMPLETED);
        } else {
            // 驳回：恢复原仓单状态
            List<Long> sourceIds = Arrays.stream(opLog.getSourceReceiptIds().split(","))
                .map(Long::parseLong).collect(Collectors.toList());
            for (Long id : sourceIds) {
                WarehouseReceipt r = warehouseReceiptMapper.selectById(id);
                if (r != null) {
                    r.setStatus(WarehouseReceipt.STATUS_IN_STOCK);
                    warehouseReceiptMapper.updateById(r);
                }
            }
            opLog.setStatus(ReceiptOperationLog.STATUS_REJECTED);
        }

        receiptOperationLogMapper.updateById(opLog);
        return true;
    }

    @Override
    public ReceiptOperationLog getOperationLogById(Long opLogId) {
        return receiptOperationLogMapper.selectById(opLogId);
    }

    // ==================== 质押/解押 ====================

    @Override
    @Transactional
    public boolean lockReceipt(Long receiptId, String loanId) {
        WarehouseReceipt receipt = warehouseReceiptMapper.selectById(receiptId);
        if (receipt == null) {
            throw new IllegalArgumentException("仓单不存在");
        }
        if (receipt.getIsLocked()) {
            throw new RuntimeException("仓单已锁定，无法重复质押");
        }
        if (receipt.getOnChainId() == null || receipt.getOnChainId().isEmpty()) {
            throw new RuntimeException("仓单未上链，无法进行质押操作");
        }

        // 调用区块链锁定
        warehouseContractService.lockReceipt(receipt.getOnChainId());

        // 更新仓单状态并保存贷款ID
        receipt.setIsLocked(true);
        receipt.setLoanId(loanId);
        return warehouseReceiptMapper.updateById(receipt) > 0;
    }

    @Override
    @Transactional
    public boolean unlockReceipt(Long receiptId) {
        WarehouseReceipt receipt = warehouseReceiptMapper.selectById(receiptId);
        if (receipt == null) {
            throw new IllegalArgumentException("仓单不存在");
        }
        if (!receipt.getIsLocked()) {
            throw new RuntimeException("仓单未锁定，无需解押");
        }
        if (receipt.getOnChainId() == null || receipt.getOnChainId().isEmpty()) {
            throw new RuntimeException("仓单未上链，无法进行解押操作");
        }

        // 调用区块链解锁
        warehouseContractService.unlockReceipt(receipt.getOnChainId());

        receipt.setIsLocked(false);
        return warehouseReceiptMapper.updateById(receipt) > 0;
    }

    // ==================== 核销出库 ====================

    @Override
    @Transactional
    public Long applyBurn(Long receiptId, Long applyUserId, String signatureHash) {
        WarehouseReceipt receipt = warehouseReceiptMapper.selectById(receiptId);
        if (receipt == null) {
            throw new RuntimeException("仓单不存在");
        }
        if (receipt.getIsLocked()) {
            throw new RuntimeException("仓单已锁定，无法申请出库");
        }

        // 创建出库单
        StockOrder order = new StockOrder();
        order.setWarehouseId(receipt.getWarehouseId());
        order.setEntId(receipt.getOwnerEntId());
        order.setUserId(applyUserId);
        order.setGoodsName(receipt.getGoodsName());
        order.setWeight(receipt.getWeight());
        order.setUnit(receipt.getUnit());
        order.setAttachmentUrl(signatureHash); // 暂用signatureHash作为附件
        order.setStatus(StockOrder.STATUS_PENDING);
        order.setRemark("核销出库申请");

        stockOrderMapper.insert(order);

        // 更新仓单状态
        receipt.setStatus(WarehouseReceipt.STATUS_IN_TRANSIT);
        warehouseReceiptMapper.updateById(receipt);

        return order.getId();
    }

    @Override
    @Transactional
    public boolean confirmBurn(Long stockOrderId, Long warehouseUserId) {
        StockOrder order = stockOrderMapper.selectById(stockOrderId);
        if (order == null || order.getStatus() != StockOrder.STATUS_PENDING) {
            return false;
        }

        // 查询对应仓单
        WarehouseReceipt receipt = warehouseReceiptMapper.selectOne(
            new LambdaQueryWrapper<WarehouseReceipt>()
                .eq(WarehouseReceipt::getGoodsName, order.getGoodsName())
                .eq(WarehouseReceipt::getOwnerEntId, order.getEntId())
                .eq(WarehouseReceipt::getStatus, WarehouseReceipt.STATUS_IN_TRANSIT)
        );

        // 必须找到对应的在途仓单才能确认核销
        if (receipt == null) {
            throw new RuntimeException("未找到对应的在途仓单，无法确认核销");
        }

        // 校验仓单已上链
        if (receipt.getOnChainId() == null || receipt.getOnChainId().isEmpty()) {
            throw new RuntimeException("仓单未上链，无法进行核销");
        }

        // 调用区块链核销
        try {
            byte[] signatureHash = order.getAttachmentUrl() != null
                ? generateHash(order.getAttachmentUrl())
                : new byte[32];
            TransactionReceipt txReceipt = warehouseContractService.burnReceipt(
                receipt.getOnChainId(),
                signatureHash
            );
            if (txReceipt == null) {
                throw new RuntimeException("区块链核销仓单失败");
            }
        } catch (Exception e) {
            throw new RuntimeException("调用区块链核销仓单异常: " + e.getMessage());
        }

        // 更新仓单状态为已核销
        receipt.setStatus(WarehouseReceipt.STATUS_BURNED);
        warehouseReceiptMapper.updateById(receipt);

        // 更新出库单状态为已确认
        order.setStatus(StockOrder.STATUS_CONFIRMED);
        return stockOrderMapper.updateById(order) > 0;
    }

    // ==================== 仓库管理 ====================

    @Override
    @Transactional
    public Long createWarehouse(Long entId, String name, String address, String contactUser, String contactPhone) {
        Warehouse warehouse = new Warehouse();
        warehouse.setEntId(entId);
        warehouse.setName(name);
        warehouse.setAddress(address);
        warehouse.setContactUser(contactUser);
        warehouse.setContactPhone(contactPhone);
        warehouse.setStatus(Warehouse.STATUS_NORMAL);

        warehouseMapper.insert(warehouse);
        return warehouse.getId();
    }

    @Override
    public List<Warehouse> getWarehousesByEntId(Long entId) {
        return warehouseMapper.selectList(
            new LambdaQueryWrapper<Warehouse>()
                .eq(Warehouse::getEntId, entId)
                .orderByDesc(Warehouse::getCreateTime)
        );
    }

    // ==================== 溯源查询 ====================

    @Override
    public TraceInfo traceReceipt(Long receiptId) {
        TraceInfo traceInfo = new TraceInfo();

        // 当前仓单
        WarehouseReceipt current = warehouseReceiptMapper.selectById(receiptId);
        traceInfo.setCurrentReceipt(current);

        // 历史仓单（通过parent_id追溯）
        List<WarehouseReceipt> history = new ArrayList<>();
        if (current != null && current.getParentId() != null && current.getParentId() > 0) {
            WarehouseReceipt parent = warehouseReceiptMapper.selectById(current.getParentId());
            while (parent != null) {
                history.add(parent);
                if (parent.getParentId() == null || parent.getParentId() == 0) {
                    break;
                }
                parent = warehouseReceiptMapper.selectById(parent.getParentId());
            }
        }
        traceInfo.setHistoryReceipts(history);

        // 背书历史
        List<ReceiptEndorsement> endorsements = receiptEndorsementMapper.selectList(
            new LambdaQueryWrapper<ReceiptEndorsement>()
                .eq(ReceiptEndorsement::getReceiptId, receiptId)
                .orderByAsc(ReceiptEndorsement::getCreateTime)
        );
        traceInfo.setEndorsementHistory(endorsements);

        // 操作历史
        List<ReceiptOperationLog> operations = receiptOperationLogMapper.selectList(
            new LambdaQueryWrapper<ReceiptOperationLog>()
                .like(ReceiptOperationLog::getSourceReceiptIds, receiptId.toString())
                .orderByAsc(ReceiptOperationLog::getCreateTime)
        );
        traceInfo.setOperationHistory(operations);

        return traceInfo;
    }

    // ==================== ABAC权限校验 ====================

    /**
     * 校验当前用户是否为仓单持有人
     */
    protected void checkReceiptOwner(WarehouseReceipt receipt) {
        Long currentEntId = CurrentUser.getEntId();
        if (currentEntId == null) {
            throw new RuntimeException("未获取到当前用户企业信息");
        }
        if (!receipt.getOwnerEntId().equals(currentEntId)) {
            throw new RuntimeException("无权限操作：非仓单持有人");
        }
    }

    /**
     * 校验仓单是否可操作（未锁定且在库）
     */
    protected void checkReceiptOperable(WarehouseReceipt receipt) {
        if (receipt.getIsLocked()) {
            throw new RuntimeException("仓单已锁定，禁止操作");
        }
        if (receipt.getStatus() != WarehouseReceipt.STATUS_IN_STOCK) {
            throw new RuntimeException("仓单状态不允许此操作");
        }
    }

    /**
     * 校验当前用户是否为仓储方
     */
    protected void checkWarehouseRole(Integer entRole) {
        if (!EntRoleConstant.isWarehouse(entRole)) {
            throw new RuntimeException("无权限操作：仅仓储方可执行此操作");
        }
    }

    /**
     * 校验当前用户是否为金融机构
     */
    protected void checkFinancialRole(Integer entRole) {
        if (!EntRoleConstant.isFinancialInstitution(entRole)) {
            throw new RuntimeException("无权限操作：仅金融机构可执行此操作");
        }
    }

    /**
     * 校验当前用户是否为仓储方或系统管理员
     */
    protected void checkWarehouseOrAdmin(Integer entRole) {
        if (!EntRoleConstant.isWarehouse(entRole) && !CurrentUser.isAdmin()) {
            throw new RuntimeException("无权限操作");
        }
    }

    /**
     * 校验当前用户是否为金融机构或系统管理员
     */
    protected void checkFinancialOrAdmin(Integer entRole) {
        if (!EntRoleConstant.isFinancialInstitution(entRole) && !CurrentUser.isAdmin()) {
            throw new RuntimeException("无权限操作");
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 生成SHA-256哈希
     */
    private byte[] generateHash(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(data.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256算法不可用", e);
        }
    }
}
