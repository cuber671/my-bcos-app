package com.fisco.app.service.warehouse;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fisco.app.aspect.RequireOnChain;
import com.fisco.app.dto.receivable.SplitApplicationRequest;
import com.fisco.app.dto.receivable.SplitApplicationResponse;
import com.fisco.app.dto.receivable.SplitApprovalRequest;
import com.fisco.app.dto.receivable.SplitApprovalResponse;
import com.fisco.app.dto.receivable.SplitDetailRequest;
import com.fisco.app.dto.warehouse.CancelApplicationRequest;
import com.fisco.app.dto.warehouse.CancelApplicationResponse;
import com.fisco.app.dto.warehouse.CancelApprovalRequest;
import com.fisco.app.dto.warehouse.CancelApprovalResponse;
import com.fisco.app.dto.warehouse.DeliveryUpdateRequest;
import com.fisco.app.dto.warehouse.ElectronicWarehouseReceiptCreateRequest;
import com.fisco.app.dto.warehouse.ElectronicWarehouseReceiptQueryRequest;
import com.fisco.app.dto.warehouse.ElectronicWarehouseReceiptResponse;
import com.fisco.app.dto.warehouse.ElectronicWarehouseReceiptUpdateRequest;
import com.fisco.app.dto.warehouse.FreezeApplicationResponse;
import com.fisco.app.dto.warehouse.FreezeApplicationReviewRequest;
import com.fisco.app.dto.warehouse.FreezeApplicationReviewResponse;
import com.fisco.app.dto.warehouse.FreezeApplicationSubmitRequest;
import com.fisco.app.dto.warehouse.ReceiptApprovalRequest;
import com.fisco.app.dto.warehouse.ReceiptApprovalResponse;
import com.fisco.app.dto.warehouse.ReceiptFreezeRequest;
import com.fisco.app.dto.warehouse.ReceiptFreezeResponse;
import com.fisco.app.dto.warehouse.ReceiptUnfreezeRequest;
import com.fisco.app.dto.warehouse.ReceiptUnfreezeResponse;
import com.fisco.app.entity.warehouse.ElectronicWarehouseReceipt;
import com.fisco.app.entity.warehouse.ReceiptCancelApplication;
import com.fisco.app.entity.warehouse.ReceiptFreezeApplication;
import com.fisco.app.entity.warehouse.ReceiptSplitApplication;
import com.fisco.app.repository.warehouse.ElectronicWarehouseReceiptRepository;
import com.fisco.app.repository.warehouse.ReceiptFreezeApplicationRepository;
import com.fisco.app.repository.warehouse.ReceiptSplitApplicationRepository;
import com.fisco.app.security.PermissionChecker;
import com.fisco.app.security.UserAuthentication;
import com.fisco.app.service.blockchain.ContractService;

import lombok.extern.slf4j.Slf4j;

/**
 * 电子仓单Service
 */
@Slf4j
@Service
public class ElectronicWarehouseReceiptService {

    @Autowired
    public ElectronicWarehouseReceiptRepository repository; // public for controller access

    @Autowired
    private PermissionChecker permissionChecker;

    @Autowired
    private ContractService contractService;

    @Autowired
    private ReceiptFreezeApplicationRepository freezeApplicationRepository;

    @Value("${app.admin.enabled:false}")
    private boolean adminFreezeOnly; // 是否只有管理员可以冻结

    /**
     * 创建仓单
     */
    @Transactional
    public ElectronicWarehouseReceiptResponse createReceipt(ElectronicWarehouseReceiptCreateRequest request) {
        log.info("创建仓单, 仓储企业: {}, 货主企业: {}", request.getWarehouseId(), request.getOwnerId());

        // 1. 权限验证：检查当前用户是否可以为本企业创建仓单
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        permissionChecker.checkCreateReceiptPermission(authentication, request.getOwnerId(), null);

        // 1. 检查仓单编号唯一性
        if (repository.findByReceiptNo(request.getReceiptNo()).isPresent()) {
            throw new RuntimeException("仓单编号已存在: " + request.getReceiptNo());
        }

        // 2. 验证业务规则
        // 验证有效期必须晚于入库时间（至少同一天或之后）
        if (!request.getExpiryDate().isAfter(request.getStorageDate())) {
            throw new RuntimeException("有效期必须晚于入库时间");
        }

        // 计算并验证总价值
        BigDecimal calculatedTotal = request.getQuantity().multiply(request.getUnitPrice());
        if (request.getTotalValue().compareTo(calculatedTotal) != 0) {
            log.warn("总价值与计算值不符，使用计算值: 传入={}, 计算={}", request.getTotalValue(), calculatedTotal);
        }

        // 3. 创建仓单对象
        ElectronicWarehouseReceipt receipt = new ElectronicWarehouseReceipt();
        receipt.setId(Objects.requireNonNull(UUID.randomUUID().toString()));
        receipt.setReceiptNo(request.getReceiptNo());
        receipt.setWarehouseId(request.getWarehouseId());
        receipt.setWarehouseAddress(request.getWarehouseAddress());
        receipt.setWarehouseName(request.getWarehouseName());
        receipt.setOwnerId(request.getOwnerId());
        receipt.setOwnerAddress(request.getOwnerAddress());
        receipt.setOwnerName(request.getOwnerName());
        receipt.setHolderAddress(request.getHolderAddress());

        // 初始化当前持单人：优先使用货主名称，如果为空则使用货主ID作为默认值
        String currentHolder = request.getOwnerName();
        if (currentHolder == null || currentHolder.trim().isEmpty()) {
            currentHolder = request.getOwnerId();
            log.info("货主名称为空，使用货主ID作为当前持单人: {}", currentHolder);
        }
        receipt.setCurrentHolder(currentHolder);

        receipt.setGoodsName(request.getGoodsName());
        receipt.setUnit(request.getUnit());
        receipt.setQuantity(request.getQuantity());
        receipt.setUnitPrice(request.getUnitPrice());
        // 使用计算的总价值确保数据一致性
        receipt.setTotalValue(calculatedTotal);
        receipt.setMarketPrice(request.getMarketPrice());
        receipt.setWarehouseLocation(request.getWarehouseLocation());
        receipt.setStorageLocation(request.getStorageLocation());
        receipt.setStorageDate(request.getStorageDate());
        receipt.setExpiryDate(request.getExpiryDate());

        // 设置初始状态为草稿
        receipt.setReceiptStatus(ElectronicWarehouseReceipt.ReceiptStatus.DRAFT);

        // 设置批次号
        receipt.setBatchNo(request.getBatchNo());

        // 设置操作人信息
        receipt.setOwnerOperatorId(request.getOwnerOperatorId());
        receipt.setOwnerOperatorName(request.getOwnerOperatorName());
        receipt.setWarehouseOperatorId(request.getWarehouseOperatorId());
        receipt.setWarehouseOperatorName(request.getWarehouseOperatorName());

        // 设置备注
        receipt.setRemarks(request.getRemarks());

        // 设置审计信息（实际项目中应从SecurityContext获取当前用户）
        receipt.setCreatedBy(request.getOwnerOperatorId() != null ? request.getOwnerOperatorId() : "system");
        receipt.setUpdatedBy(request.getOwnerOperatorId() != null ? request.getOwnerOperatorId() : "system");

        // 4. 保存仓单
        ElectronicWarehouseReceipt saved = repository.save(receipt);
        log.info("仓单创建成功, ID: {}, 编号: {}, 状态: {}", saved.getId(), saved.getReceiptNo(), saved.getReceiptStatus());

        return ElectronicWarehouseReceiptResponse.fromEntity(saved);
    }

    /**
     * 更新仓单
     */
    @Transactional
    public ElectronicWarehouseReceiptResponse updateReceipt(@NonNull String id, ElectronicWarehouseReceiptUpdateRequest request) {
        log.info("更新仓单, ID: {}", id);

        ElectronicWarehouseReceipt receipt = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("仓单不存在: " + id));

        // 只有草稿或正常状态可以更新
        if (receipt.getReceiptStatus() != ElectronicWarehouseReceipt.ReceiptStatus.DRAFT
                && receipt.getReceiptStatus() != ElectronicWarehouseReceipt.ReceiptStatus.NORMAL) {
            throw new RuntimeException("只有草稿或正常状态的仓单可以更新, 当前状态: " + String.valueOf(receipt.getReceiptStatus()));
        }

        // 更新允许修改的字段
        if (request.getUnitPrice() != null) {
            receipt.setUnitPrice(request.getUnitPrice());
            // 重新计算总价值
            receipt.setTotalValue(request.getUnitPrice().multiply(receipt.getQuantity()));
        }
        if (request.getWarehouseLocation() != null) {
            receipt.setWarehouseLocation(request.getWarehouseLocation());
        }
        if (request.getStorageLocation() != null) {
            receipt.setStorageLocation(request.getStorageLocation());
        }
        if (request.getRemarks() != null) {
            receipt.setRemarks(request.getRemarks());
        }
        if (request.getExpiryDate() != null) {
            receipt.setExpiryDate(request.getExpiryDate());
        }

        ElectronicWarehouseReceipt updated = repository.save(receipt);
        log.info("仓单更新成功, ID: {}", updated.getId());

        return ElectronicWarehouseReceiptResponse.fromEntity(updated);
    }

    /**
     * 更新仓单（增强版：记录变更历史）
     *
     * @param id 仓单ID
     * @param request 更新请求（包含变更原因和变更类型）
     * @param operatorId 操作人ID
     * @param operatorName 操作人姓名
     * @return 更新后的仓单响应
     */
    @Transactional
    @SuppressWarnings("null")
    public ElectronicWarehouseReceiptResponse updateReceiptWithHistory(
            @NonNull String id,
            com.fisco.app.dto.warehouse.UpdateReceiptRequest request,
            String operatorId,
            String operatorName) {

        log.info("更新仓单（记录变更历史）: id={}, changeType={}, operator={}",
            id, request.getChangeType(), operatorName);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // ==================== 第1步：查询并验证仓单 ====================
        ElectronicWarehouseReceipt receipt = repository.findById(id)
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException("仓单不存在: " + id));

        // 只有草稿或正常状态可以更新
        if (receipt.getReceiptStatus() != ElectronicWarehouseReceipt.ReceiptStatus.DRAFT
                && receipt.getReceiptStatus() != ElectronicWarehouseReceipt.ReceiptStatus.NORMAL) {
            throw new com.fisco.app.exception.BusinessException(
                "只有草稿或正常状态的仓单可以更新。当前状态: " + receipt.getReceiptStatus());
        }

        // ==================== 第2步：权限验证 ====================
        if (!(auth instanceof UserAuthentication)) {
            throw new com.fisco.app.exception.BusinessException("无效的认证信息");
        }
        UserAuthentication userAuth = (UserAuthentication) auth;

        // 货主企业或仓储企业可以变更
        if (!userAuth.getEnterpriseId().equals(receipt.getOwnerId()) &&
            !userAuth.getEnterpriseId().equals(receipt.getWarehouseId()) &&
            !userAuth.isSystemAdmin()) {
            throw new com.fisco.app.exception.BusinessException("无权限更新此仓单");
        }

        // ==================== 第3步：记录变更前的值 ====================
        java.util.Map<String, Object> beforeValue = new java.util.HashMap<>();
        java.util.List<String> changedFields = new java.util.ArrayList<>();

        if (request.getUnitPrice() != null) {
            beforeValue.put("unitPrice", receipt.getUnitPrice());
            changedFields.add("unitPrice");
        }
        if (request.getMarketPrice() != null) {
            beforeValue.put("marketPrice", receipt.getMarketPrice());
            changedFields.add("marketPrice");
        }
        if (request.getWarehouseLocation() != null) {
            beforeValue.put("warehouseLocation", receipt.getWarehouseLocation());
            changedFields.add("warehouseLocation");
        }
        if (request.getStorageLocation() != null) {
            beforeValue.put("storageLocation", receipt.getStorageLocation());
            changedFields.add("storageLocation");
        }
        if (request.getExpiryDate() != null) {
            beforeValue.put("expiryDate", receipt.getExpiryDate());
            changedFields.add("expiryDate");
        }
        if (request.getRemarks() != null) {
            beforeValue.put("remarks", receipt.getRemarks());
            changedFields.add("remarks");
        }

        // ==================== 第4步：执行更新 ====================
        if (request.getUnitPrice() != null) {
            // 单价调整时，需要重新计算总价值
            receipt.setUnitPrice(request.getUnitPrice());
            // 防止空指针异常：确保quantity不为null
            if (receipt.getQuantity() != null) {
                receipt.setTotalValue(request.getUnitPrice().multiply(receipt.getQuantity()));
            } else {
                throw new com.fisco.app.exception.BusinessException("仓单数量不能为空，无法计算总价值");
            }
            log.info("单价调整: {} -> {}, 总价值重新计算: {}",
                beforeValue.get("unitPrice"), request.getUnitPrice(), receipt.getTotalValue());
        }
        if (request.getMarketPrice() != null) {
            receipt.setMarketPrice(request.getMarketPrice());
        }
        if (request.getWarehouseLocation() != null) {
            receipt.setWarehouseLocation(request.getWarehouseLocation());
        }
        if (request.getStorageLocation() != null) {
            receipt.setStorageLocation(request.getStorageLocation());
        }
        if (request.getExpiryDate() != null) {
            // 有效期延长：新有效期必须晚于当前有效期
            if (receipt.getExpiryDate() != null &&
                request.getExpiryDate().isBefore(receipt.getExpiryDate())) {
                throw new com.fisco.app.exception.BusinessException(
                    "新有效期必须晚于或等于当前有效期");
            }
            receipt.setExpiryDate(request.getExpiryDate());
            log.info("有效期延长: {} -> {}", beforeValue.get("expiryDate"), request.getExpiryDate());
        }
        if (request.getRemarks() != null) {
            receipt.setRemarks(request.getRemarks());
        }

        receipt.setUpdatedBy(operatorId);

        // 保存仓单
        ElectronicWarehouseReceipt updated = repository.save(receipt);

        // ==================== 第5步：记录变更后的值 ====================
        java.util.Map<String, Object> afterValue = new java.util.HashMap<>();

        if (request.getUnitPrice() != null) {
            afterValue.put("unitPrice", updated.getUnitPrice());
            afterValue.put("totalValue", updated.getTotalValue()); // 总价值也变了
        }
        if (request.getMarketPrice() != null) {
            afterValue.put("marketPrice", updated.getMarketPrice());
        }
        if (request.getWarehouseLocation() != null) {
            afterValue.put("warehouseLocation", updated.getWarehouseLocation());
        }
        if (request.getStorageLocation() != null) {
            afterValue.put("storageLocation", updated.getStorageLocation());
        }
        if (request.getExpiryDate() != null) {
            afterValue.put("expiryDate", updated.getExpiryDate());
        }
        if (request.getRemarks() != null) {
            afterValue.put("remarks", updated.getRemarks());
        }

        // ==================== 第6步：创建变更历史记录 ====================
        com.fisco.app.entity.warehouse.ReceiptChangeHistory history =
            new com.fisco.app.entity.warehouse.ReceiptChangeHistory();
        history.setId(java.util.UUID.randomUUID().toString());
        history.setReceiptId(updated.getId());
        history.setReceiptNo(updated.getReceiptNo());
        history.setChangeType(request.getChangeType().name());
        history.setChangeReason(request.getChangeReason());

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            history.setBeforeValue(mapper.writeValueAsString(beforeValue));
            history.setAfterValue(mapper.writeValueAsString(afterValue));
            history.setChangedFields(mapper.writeValueAsString(changedFields));
        } catch (Exception e) {
            log.error("转换变更数据为JSON失败", e);
            throw new com.fisco.app.exception.BusinessException("变更数据格式错误");
        }

        history.setOperatorId(operatorId);
        history.setOperatorName(operatorName);
        history.setChangeTime(java.time.LocalDateTime.now());
        history.setRemarks(request.getRemarks());

        changeHistoryRepository.save(history);

        log.info("仓单更新成功（已记录变更历史）: id={}, changedFields={}",
            updated.getId(), changedFields);

        return ElectronicWarehouseReceiptResponse.fromEntity(updated);
    }

    /**
     * 查询仓单变更历史
     *
     * @param receiptId 仓单ID
     * @return 变更历史列表
     */
    public java.util.List<com.fisco.app.entity.warehouse.ReceiptChangeHistory> getReceiptChangeHistory(
            @NonNull String receiptId) {

        log.info("查询仓单变更历史: receiptId={}", receiptId);

        // ==================== 权限验证 ====================
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof UserAuthentication)) {
            throw new com.fisco.app.exception.BusinessException("无效的认证信息");
        }
        UserAuthentication userAuth = (UserAuthentication) auth;

        // 验证仓单是否存在并检查权限
        ElectronicWarehouseReceipt receipt = repository.findById(receiptId)
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException("仓单不存在: " + receiptId));

        // 只有货主企业、仓储企业或系统管理员可以查询变更历史
        if (!userAuth.getEnterpriseId().equals(receipt.getOwnerId()) &&
            !userAuth.getEnterpriseId().equals(receipt.getWarehouseId()) &&
            !userAuth.isSystemAdmin()) {
            throw new com.fisco.app.exception.BusinessException("无权限查询此仓单的变更历史");
        }

        java.util.List<com.fisco.app.entity.warehouse.ReceiptChangeHistory> history =
                changeHistoryRepository.findByReceiptIdOrderByChangeTimeDesc(receiptId);

        log.info("找到 {} 条变更历史记录", history.size());

        return history;
    }

    /**
     * 根据ID查询仓单
     */
    public ElectronicWarehouseReceiptResponse getReceiptById(@NonNull String id) {
        ElectronicWarehouseReceipt receipt = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("仓单不存在: " + id));
        return ElectronicWarehouseReceiptResponse.fromEntity(receipt);
    }

    /**
     * 根据仓单编号查询
     */
    public ElectronicWarehouseReceiptResponse getReceiptByNo(String receiptNo) {
        ElectronicWarehouseReceipt receipt = repository.findByReceiptNo(receiptNo)
                .orElseThrow(() -> new RuntimeException("仓单不存在: " + receiptNo));
        return ElectronicWarehouseReceiptResponse.fromEntity(receipt);
    }

    /**
     * 分页查询仓单
     */
    public Page<ElectronicWarehouseReceiptResponse> queryReceipts(ElectronicWarehouseReceiptQueryRequest request) {
        log.info("分页查询仓单, page: {}, size: {}", request.getPage(), request.getSize());

        // 构建分页参数
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        // 注意: 实际项目中应该使用Specification或QueryDSL实现复杂条件查询
        // 这里简化处理,仅使用基础分页
        Page<ElectronicWarehouseReceipt> page = repository.findAll(pageable);

        return page.map(ElectronicWarehouseReceiptResponse::fromEntity);
    }

    /**
     * 查询货主的仓单列表
     */
    public List<ElectronicWarehouseReceiptResponse> getReceiptsByOwner(String ownerId) {
        List<ElectronicWarehouseReceipt> receipts = repository.findByOwnerId(ownerId);
        return receipts.stream()
                .map(ElectronicWarehouseReceiptResponse::fromEntity)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 查询持单人的仓单列表
     */
    public List<ElectronicWarehouseReceiptResponse> getReceiptsByHolder(String holderAddress) {
        List<ElectronicWarehouseReceipt> receipts = repository.findByHolderAddress(holderAddress);
        return receipts.stream()
                .map(ElectronicWarehouseReceiptResponse::fromEntity)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 查询仓储企业的仓单列表
     */
    public List<ElectronicWarehouseReceiptResponse> getReceiptsByWarehouse(String warehouseId) {
        List<ElectronicWarehouseReceipt> receipts = repository.findByWarehouseId(warehouseId);
        return receipts.stream()
                .map(ElectronicWarehouseReceiptResponse::fromEntity)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 查询即将过期的仓单（7天内）
     */
    public List<ElectronicWarehouseReceiptResponse> getExpiringReceipts() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryThreshold = now.plusDays(7);
        List<ElectronicWarehouseReceipt> receipts = repository.findExpiringReceipts(now, expiryThreshold);
        return receipts.stream()
                .map(ElectronicWarehouseReceiptResponse::fromEntity)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 查询已过期的仓单
     */
    public List<ElectronicWarehouseReceiptResponse> getExpiredReceipts() {
        LocalDateTime now = LocalDateTime.now();
        List<ElectronicWarehouseReceipt> receipts = repository.findExpiredReceipts(now);
        return receipts.stream()
                .map(ElectronicWarehouseReceiptResponse::fromEntity)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 更新仓单状态
     */
    @Transactional
    public void updateReceiptStatus(@NonNull String id, ElectronicWarehouseReceipt.ReceiptStatus status) {
        log.info("更新仓单状态, ID: {}, 状态: {}", id, status);

        ElectronicWarehouseReceipt receipt = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("仓单不存在: " + id));

        receipt.setReceiptStatus(status);
        repository.save(receipt);
    }

    /**
     * 更新区块链上链状态
     */
    @Transactional
    public void updateBlockchainStatus(@NonNull String id, ElectronicWarehouseReceipt.BlockchainStatus status,
                                        String txHash, Long blockNumber) {
        log.info("更新区块链状态, ID: {}, 状态: {}, txHash: {}", id, status, txHash);

        ElectronicWarehouseReceipt receipt = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("仓单不存在: " + id));

        receipt.setBlockchainStatus(status);
        if (txHash != null) {
            receipt.setTxHash(txHash);
        }
        if (blockNumber != null) {
            receipt.setBlockNumber(blockNumber);
        }
        receipt.setBlockchainTimestamp(LocalDateTime.now());

        repository.save(receipt);
    }

    /**
     * 软删除仓单
     */
    @Transactional
    public void deleteReceipt(@NonNull String id) {
        log.info("删除仓单, ID: {}", id);

        ElectronicWarehouseReceipt receipt = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("仓单不存在: " + id));

        // 只有草稿状态可以删除
        if (receipt.getReceiptStatus() != ElectronicWarehouseReceipt.ReceiptStatus.DRAFT) {
            throw new RuntimeException("只有草稿状态的仓单可以删除, 当前状态: " + String.valueOf(receipt.getReceiptStatus()));
        }

        receipt.setDeletedAt(LocalDateTime.now());
        receipt.setDeletedBy("system");  // 实际应该从SecurityContext获取
        repository.save(receipt);

        log.info("仓单删除成功, ID: {}", id);
    }

    /**
     * 统计货主的仓单数量
     */
    public Long countByOwner(String ownerId) {
        return repository.countByOwnerId(ownerId);
    }

    /**
     * 统计仓储企业的仓单数量
     */
    public Long countByWarehouse(String warehouseId) {
        return repository.countByWarehouseId(warehouseId);
    }

    /**
     * 更新持单人（背书后调用）
     */
    @Transactional
    public void updateHolder(@NonNull String id, String newHolderAddress, String newHolderName) {
        log.info("更新持单人, ID: {}, 新持单人: {}", id, newHolderAddress);

        ElectronicWarehouseReceipt receipt = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("仓单不存在: " + id));

        receipt.setHolderAddress(newHolderAddress);
        receipt.setCurrentHolder(newHolderName);

        // 增加背书次数
        if (receipt.getEndorsementCount() == null) {
            receipt.setEndorsementCount(0);
        }
        receipt.setEndorsementCount(receipt.getEndorsementCount() + 1);
        receipt.setLastEndorsementDate(LocalDateTime.now());

        repository.save(receipt);
    }

    /**
     * 更新实际提货时间
     * 要求仓单必须已上链（blockchainStatus = SYNCED）
     */
    @RequireOnChain(value = "提货", allowFailed = false)
    @Transactional
    public ElectronicWarehouseReceiptResponse updateActualDeliveryDate(@NonNull String id, DeliveryUpdateRequest request) {
        log.info("更新实际提货时间, ID: {}, 提货时间: {}", id, request.getActualDeliveryDate());

        // 1. 查询仓单
        ElectronicWarehouseReceipt receipt = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("仓单不存在: " + id));

        // 2. 验证仓单状态
        if (receipt.getReceiptStatus() == ElectronicWarehouseReceipt.ReceiptStatus.DELIVERED) {
            throw new RuntimeException("该仓单已经提货，无法重复提货");
        }

        if (receipt.getReceiptStatus() == ElectronicWarehouseReceipt.ReceiptStatus.DRAFT) {
            throw new RuntimeException("草稿状态的仓单无法提货，请先激活仓单");
        }

        if (receipt.getReceiptStatus() == ElectronicWarehouseReceipt.ReceiptStatus.CANCELLED) {
            throw new RuntimeException("已取消的仓单无法提货");
        }

        if (receipt.getReceiptStatus() == ElectronicWarehouseReceipt.ReceiptStatus.EXPIRED) {
            throw new RuntimeException("已过期的仓单无法提货，请联系管理员");
        }

        // 3. 验证提货时间不能早于入库时间
        if (request.getActualDeliveryDate().isBefore(receipt.getStorageDate())) {
            throw new RuntimeException("提货时间不能早于入库时间");
        }

        // 4. 如果仓单已质押，需要先解押
        if (receipt.getReceiptStatus() == ElectronicWarehouseReceipt.ReceiptStatus.PLEDGED) {
            log.warn("仓单当前为质押状态，提货将自动解押, ID: {}", id);
            // 这里可以添加解押逻辑，或者要求先手动解押
        }

        // 5. 如果仓单已冻结，需要先解冻
        if (receipt.getReceiptStatus() == ElectronicWarehouseReceipt.ReceiptStatus.FROZEN) {
            throw new RuntimeException("仓单已冻结，无法提货，请先解冻");
        }

        // 6. 更新提货信息
        receipt.setActualDeliveryDate(request.getActualDeliveryDate());
        receipt.setDeliveryPersonName(request.getDeliveryPersonName());
        receipt.setDeliveryPersonContact(request.getDeliveryPersonContact());
        receipt.setDeliveryNo(request.getDeliveryNo());
        receipt.setVehiclePlate(request.getVehiclePlate());
        receipt.setDriverName(request.getDriverName());

        // 7. 更新仓单状态为已提货
        receipt.setReceiptStatus(ElectronicWarehouseReceipt.ReceiptStatus.DELIVERED);

        // 8. 合并备注信息
        if (request.getRemarks() != null && !request.getRemarks().trim().isEmpty()) {
            String existingRemarks = receipt.getRemarks();
            if (existingRemarks != null && !existingRemarks.trim().isEmpty()) {
                receipt.setRemarks(existingRemarks + "\n[提货] " + request.getRemarks());
            } else {
                receipt.setRemarks("[提货] " + request.getRemarks());
            }
        }

        ElectronicWarehouseReceipt saved = repository.save(receipt);
        log.info("实际提货时间更新成功, ID: {}, 提货时间: {}", id, request.getActualDeliveryDate());

        return ElectronicWarehouseReceiptResponse.fromEntity(saved);
    }

    /**
     * 仓储方审核仓单入库
     * 状态流转：DRAFT → PENDING_ONCHAIN → NORMAL / ONCHAIN_FAILED
     */
    @Transactional
    public ReceiptApprovalResponse approveReceipt(ReceiptApprovalRequest request, String approverId, String approverName) {
        // 参数提取和null检查
        String receiptId = request.getReceiptId();
        String warehouseId = request.getWarehouseId();

        log.info("审核仓单, ID: {}, 审核结果: {}, 审核方: {}",
                receiptId, request.getApprovalResult(), warehouseId);

        // 0. 权限验证：检查当前用户是否为指定的仓储企业
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        permissionChecker.checkReceiptApprovalPermission(authentication, warehouseId, null);

        // 1. 查询仓单
        ElectronicWarehouseReceipt receipt = repository.findById(receiptId)
                .orElseThrow(() -> new RuntimeException("仓单不存在: " + receiptId));

        // 2. 验证仓单状态（只有草稿状态可以审核）
        if (receipt.getReceiptStatus() != ElectronicWarehouseReceipt.ReceiptStatus.DRAFT) {
            throw new RuntimeException("只有草稿状态的仓单可以审核, 当前状态: " + receipt.getReceiptStatus());
        }

        // 3. 验证审核方是否为该仓单的仓储方
        if (!receipt.getWarehouseId().equals(warehouseId)) {
            throw new RuntimeException("只有指定的仓储企业可以审核此仓单");
        }

        // 4. 根据审核结果处理
        if ("APPROVED".equals(request.getApprovalResult())) {
            // ==================== 审核通过 ====================

            // 4.1 更新仓单状态为 PENDING_ONCHAIN（待上链）
            receipt.setReceiptStatus(ElectronicWarehouseReceipt.ReceiptStatus.PENDING_ONCHAIN);

            // 4.2 添加审核意见到备注
            String remarks = receipt.getRemarks() != null
                    ? receipt.getRemarks() + "\n[审核通过] " + request.getApprovalComments()
                    : "[审核通过] " + request.getApprovalComments();
            receipt.setRemarks(remarks);

            // 4.3 先保存到数据库（此时状态为 PENDING_ONCHAIN）
            repository.save(receipt);
            log.info("仓单状态更新为PENDING_ONCHAIN, ID: {}", receiptId);

            // 4.4 上链到区块链（两步：创建 + 验证）
            try {
                log.info("开始将仓单上链, ID: {}", receiptId);

                // 步骤1: 创建仓单（合约状态：Created）
                String txHash1 = contractService.createReceiptOnChain(receipt);
                log.info("仓单创建上链成功, ID: {}, txHash: {}", receiptId, txHash1);

                // 步骤2: 验证仓单（合约状态：Created → Verified）
                String txHash2 = contractService.verifyReceiptOnChain(receiptId);
                log.info("仓单验证上链成功, ID: {}, txHash: {}", receiptId, txHash2);

                // 步骤3: 获取区块号（使用验证交易的哈希）
                Long blockNumber = contractService.getBlockNumber(txHash2);

                // 步骤4: 更新状态为 NORMAL，同时更新区块链信息
                receipt.setReceiptStatus(ElectronicWarehouseReceipt.ReceiptStatus.NORMAL);
                receipt.setTxHash(txHash2);
                receipt.setBlockNumber(blockNumber);
                receipt.setBlockchainStatus(ElectronicWarehouseReceipt.BlockchainStatus.SYNCED);
                receipt.setBlockchainTimestamp(LocalDateTime.now());

                // 保存更新
                repository.save(receipt);

                log.info("仓单上链完成，状态变更为NORMAL, ID: {}, txHash: {}, blockNumber: {}",
                         receiptId, txHash2, blockNumber);

            } catch (Exception e) {
                // 上链失败处理：状态变更为 ONCHAIN_FAILED
                log.error("仓单上链失败, ID: {}, 错误: {}", receiptId, e.getMessage(), e);

                // 更新状态为 ONCHAIN_FAILED
                receipt.setReceiptStatus(ElectronicWarehouseReceipt.ReceiptStatus.ONCHAIN_FAILED);
                receipt.setBlockchainStatus(ElectronicWarehouseReceipt.BlockchainStatus.FAILED);
                receipt.setBlockchainTimestamp(LocalDateTime.now());

                // 在备注中记录上链失败信息
                String failureRemarks = receipt.getRemarks() +
                        "\n[上链失败] " + e.getMessage() +
                        "\n[提示] 可以重试上链或回滚到草稿状态";
                receipt.setRemarks(failureRemarks);

                // 保存更新
                repository.save(receipt);

                log.warn("仓单上链失败已记录, ID: {}, 状态设置为ONCHAIN_FAILED", receiptId);
            }

            return ReceiptApprovalResponse.approved(
                    receipt.getId(),
                    receipt.getReceiptNo(),
                    approverName,
                    request.getApprovalComments()
            );

        } else if ("REJECTED".equals(request.getApprovalResult())) {
            // 审核拒绝
            String rejectionReason = request.getRejectionReason() != null
                    ? request.getRejectionReason()
                    : "审核方未提供具体原因";

            receipt.setRemarks(receipt.getRemarks() != null
                    ? receipt.getRemarks() + "\n[审核拒绝] " + rejectionReason
                    : "[审核拒绝] " + rejectionReason);

            // 注意：审核拒绝时状态保持DRAFT，允许货主修改后重新提交
            repository.save(receipt);
            log.info("仓单审核拒绝, ID: {}, 原因: {}", request.getReceiptId(), rejectionReason);

            return ReceiptApprovalResponse.rejected(
                    receipt.getId(),
                    receipt.getReceiptNo(),
                    approverName,
                    rejectionReason
            );

        } else {
            throw new RuntimeException("无效的审核结果: " + request.getApprovalResult());
        }
    }

    /**
     * 重试失败的仓单上链
     * 状态流转：ONCHAIN_FAILED → NORMAL / 保持 ONCHAIN_FAILED
     *
     * @param receiptId 仓单ID
     * @return 交易哈希
     * @throws RuntimeException 如果重试失败
     */
    @Transactional
    public String retryReceiptOnChain(@NonNull String receiptId) {
        log.info("重试仓单上链, ID: {}", receiptId);

        // 1. 查询仓单
        ElectronicWarehouseReceipt receipt = repository.findById(receiptId)
                .orElseThrow(() -> new RuntimeException("仓单不存在: " + receiptId));

        // 2. 验证状态（必须是 ONCHAIN_FAILED 或 blockchainStatus 为 FAILED）
        if (receipt.getReceiptStatus() != ElectronicWarehouseReceipt.ReceiptStatus.ONCHAIN_FAILED
                && receipt.getBlockchainStatus() != ElectronicWarehouseReceipt.BlockchainStatus.FAILED) {
            throw new RuntimeException("只能重试上链失败的仓单, 当前状态: "
                    + receipt.getReceiptStatus() + ", 区块链状态: " + receipt.getBlockchainStatus());
        }

        // 3. 重试上链（两步：创建 + 验证）
        try {
            log.info("开始重试上链, ID: {}", receiptId);

            // 步骤1: 创建仓单
            String txHash1 = contractService.createReceiptOnChain(receipt);
            log.info("仓单创建上链成功, ID: {}, txHash: {}", receiptId, txHash1);

            // 步骤2: 验证仓单
            String txHash2 = contractService.verifyReceiptOnChain(receiptId);
            log.info("仓单验证上链成功, ID: {}, txHash: {}", receiptId, txHash2);

            // 步骤3: 获取区块号
            Long blockNumber = contractService.getBlockNumber(txHash2);

            // 步骤4: 更新状态为 NORMAL，同时更新区块链信息
            receipt.setReceiptStatus(ElectronicWarehouseReceipt.ReceiptStatus.NORMAL);
            receipt.setTxHash(txHash2);
            receipt.setBlockNumber(blockNumber);
            receipt.setBlockchainStatus(ElectronicWarehouseReceipt.BlockchainStatus.SYNCED);
            receipt.setBlockchainTimestamp(LocalDateTime.now());

            // 清除之前的失败备注
            String remarks = receipt.getRemarks();
            if (remarks != null && remarks.contains("[上链失败]")) {
                // 移除上链失败的备注
                remarks = remarks.replaceAll("\\n?\\[上链失败\\].*?(?=\\n|$)", "");
                // 添加重试成功备注
                remarks = remarks + "\n[重试上链成功] txHash: " + txHash2;
                receipt.setRemarks(remarks);
            }

            repository.save(receipt);

            log.info("仓单重试上链成功，状态变更为NORMAL, ID: {}, txHash: {}, blockNumber: {}",
                     receiptId, txHash2, blockNumber);
            return txHash2;

        } catch (Exception e) {
            log.error("仓单重试上链失败, ID: {}", receiptId, e);

            // 保持 ONCHAIN_FAILED 状态，但在备注中记录重试失败
            String remarks = receipt.getRemarks();
            String failureRemark = "\n[重试上链失败] " + LocalDateTime.now() + ": " + e.getMessage();
            receipt.setRemarks(remarks + failureRemark);
            repository.save(receipt);

            throw new RuntimeException("重试上链失败: " + e.getMessage(), e);
        }
    }

    /**
     * 查询待审核的仓单
     */
    public List<ElectronicWarehouseReceiptResponse> getPendingReceipts(String warehouseId) {
        log.info("查询待审核仓单, 仓储企业: {}", warehouseId);

        List<ElectronicWarehouseReceipt> receipts = repository.findByWarehouseIdAndReceiptStatus(
                warehouseId,
                ElectronicWarehouseReceipt.ReceiptStatus.DRAFT
        );

        log.info("找到 {} 条待审核仓单", receipts.size());
        return receipts.stream()
                .map(ElectronicWarehouseReceiptResponse::fromEntity)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 放弃重试上链，回滚到草稿状态
     * 状态流转：ONCHAIN_FAILED → DRAFT
     * 允许货主修改后重新提交审核
     *
     * @param receiptId 仓单ID
     * @param reason 回滚原因
     * @return 回滚后的仓单信息
     */
    @Transactional
    public ElectronicWarehouseReceiptResponse rollbackToDraft(@NonNull String receiptId, String reason) {
        log.info("回滚仓单到草稿状态, ID: {}, 原因: {}", receiptId, reason);

        // 1. 查询仓单
        ElectronicWarehouseReceipt receipt = repository.findById(receiptId)
                .orElseThrow(() -> new RuntimeException("仓单不存在: " + receiptId));

        // 2. 验证状态（必须是 ONCHAIN_FAILED 状态）
        if (receipt.getReceiptStatus() != ElectronicWarehouseReceipt.ReceiptStatus.ONCHAIN_FAILED) {
            throw new RuntimeException("只有上链失败的仓单可以回滚到草稿, 当前状态: "
                    + receipt.getReceiptStatus());
        }

        // 3. 检查仓单是否已进行其他操作（如已质押、已转让等）
        if (receipt.getIsFinanced() != null && receipt.getIsFinanced()) {
            throw new RuntimeException("该仓单已进行融资，无法回滚到草稿");
        }

        if (receipt.getEndorsementCount() != null && receipt.getEndorsementCount() > 0) {
            throw new RuntimeException("该仓单已进行背书转让，无法回滚到草稿");
        }

        // 4. 更新状态为 DRAFT
        receipt.setReceiptStatus(ElectronicWarehouseReceipt.ReceiptStatus.DRAFT);
        receipt.setBlockchainStatus(ElectronicWarehouseReceipt.BlockchainStatus.PENDING);

        // 5. 清空区块链相关信息
        receipt.setTxHash(null);
        receipt.setBlockNumber(null);

        // 6. 添加回滚备注
        String rollbackReason = reason != null && !reason.trim().isEmpty()
                ? reason
                : "放弃重试上链";
        String remarks = receipt.getRemarks() != null
                ? receipt.getRemarks() + "\n[回滚到草稿] " + LocalDateTime.now() + ": " + rollbackReason
                : "[回滚到草稿] " + LocalDateTime.now() + ": " + rollbackReason;
        receipt.setRemarks(remarks);

        // 7. 保存更新
        ElectronicWarehouseReceipt saved = repository.save(receipt);

        log.info("仓单回滚到草稿成功, ID: {}", receiptId);

        return ElectronicWarehouseReceiptResponse.fromEntity(saved);
    }

    /**
     * 查询上链失败的仓单列表
     * 用于货主查看需要处理（重试或回滚）的仓单
     *
     * @param ownerId 货主企业ID（可选，如果为null则查询所有）
     * @return 上链失败的仓单列表
     */
    public List<ElectronicWarehouseReceiptResponse> getOnChainFailedReceipts(String ownerId) {
        log.info("查询上链失败的仓单, 货主: {}", ownerId);

        List<ElectronicWarehouseReceipt> receipts;
        if (ownerId != null && !ownerId.trim().isEmpty()) {
            receipts = repository.findByOwnerIdAndReceiptStatus(
                    ownerId,
                    ElectronicWarehouseReceipt.ReceiptStatus.ONCHAIN_FAILED
            );
        } else {
            receipts = repository.findByReceiptStatus(
                    ElectronicWarehouseReceipt.ReceiptStatus.ONCHAIN_FAILED
            );
        }

        log.info("找到 {} 条上链失败的仓单", receipts.size());
        return receipts.stream()
                .map(ElectronicWarehouseReceiptResponse::fromEntity)
                .collect(java.util.stream.Collectors.toList());
    }

    // ==================== 冻结/解冻功能 ====================

    /**
     * 冻结仓单
     * 状态流转：NORMAL/PLEDGED/TRANSFERRED → FROZEN
     *
     * @param request 冻结请求
     * @param operatorId 操作人ID
     * @param operatorName 操作人姓名
     * @return 冻结响应
     */
    @Transactional
    public ReceiptFreezeResponse freezeReceipt(ReceiptFreezeRequest request, String operatorId, String operatorName) {
        log.info("冻结仓单, ID: {}, 操作方类型: {}, 冻结原因: {}",
                request.getReceiptId(), request.getOperatorType(), request.getFreezeReason());

        // 1. 查询仓单
        ElectronicWarehouseReceipt receipt = repository.findById(request.getReceiptId())
                .orElseThrow(() -> new RuntimeException("仓单不存在: " + request.getReceiptId()));

        // 2. 记录当前状态
        String previousStatus = receipt.getReceiptStatus().name();

        // 3. 验证仓单状态（只有正常、已质押、已转让状态可以冻结）
        if (receipt.getReceiptStatus() != ElectronicWarehouseReceipt.ReceiptStatus.NORMAL
                && receipt.getReceiptStatus() != ElectronicWarehouseReceipt.ReceiptStatus.PLEDGED
                && receipt.getReceiptStatus() != ElectronicWarehouseReceipt.ReceiptStatus.TRANSFERRED) {
            throw new RuntimeException("只能冻结正常、已质押或已转让状态的仓单, 当前状态: " + previousStatus);
        }

        // 4. 验证仓单是否已冻结
        if (receipt.getReceiptStatus() == ElectronicWarehouseReceipt.ReceiptStatus.FROZEN) {
            throw new RuntimeException("仓单已经是冻结状态，无需重复冻结");
        }

        // 5. 权限验证：根据操作方类型验证权限
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        validateFreezePermission(authentication, request, receipt);

        // 6. 更新仓单状态为冻结
        receipt.setReceiptStatus(ElectronicWarehouseReceipt.ReceiptStatus.FROZEN);

        // 7. 在备注中记录冻结信息
        String freezeRemarks = String.format(
                "\n[冻结操作] 时间: %s | 操作方: %s | 操作类型: %s | 冻结原因: %s | 相关文件: %s | 操作人: %s",
                LocalDateTime.now(),
                request.getOperatorType(),
                request.getFreezeType(),
                request.getFreezeReason(),
                request.getReferenceNo() != null ? request.getReferenceNo() : "无",
                operatorName
        );

        if (request.getRemarks() != null && !request.getRemarks().trim().isEmpty()) {
            freezeRemarks += " | 备注: " + request.getRemarks();
        }

        String existingRemarks = receipt.getRemarks();
        if (existingRemarks != null && !existingRemarks.trim().isEmpty()) {
            receipt.setRemarks(existingRemarks + freezeRemarks);
        } else {
            receipt.setRemarks(freezeRemarks.substring(1)); // 去掉开头的换行符
        }

        // 8. 更新审计信息
        receipt.setUpdatedBy(operatorId);

        // 9. 保存仓单
        repository.save(receipt);

        log.info("仓单冻结成功, ID: {}, 原状态: {}, 新状态: FROZEN",
                request.getReceiptId(), previousStatus);

        return ReceiptFreezeResponse.success(
                receipt.getId(),
                receipt.getReceiptNo(),
                previousStatus,
                request.getOperatorType(),
                request.getFreezeReason(),
                request.getFreezeType(),
                request.getReferenceNo(),
                operatorName
        );
    }

    /**
     * 解冻仓单
     * 状态流转：FROZEN → NORMAL/PLEDGED/TRANSFERRED
     *
     * @param request 解冻请求
     * @param operatorId 操作人ID
     * @param operatorName 操作人姓名
     * @return 解冻响应
     */
    @Transactional
    public ReceiptUnfreezeResponse unfreezeReceipt(ReceiptUnfreezeRequest request, String operatorId, String operatorName) {
        log.info("解冻仓单, ID: {}, 目标状态: {}, 解冻原因: {}",
                request.getReceiptId(), request.getTargetStatus(), request.getUnfreezeReason());

        // 1. 查询仓单
        ElectronicWarehouseReceipt receipt = repository.findById(request.getReceiptId())
                .orElseThrow(() -> new RuntimeException("仓单不存在: " + request.getReceiptId()));

        // 2. 验证仓单状态（只有冻结状态可以解冻）
        if (receipt.getReceiptStatus() != ElectronicWarehouseReceipt.ReceiptStatus.FROZEN) {
            throw new RuntimeException("只能解冻已冻结状态的仓单, 当前状态: " + receipt.getReceiptStatus());
        }

        // 3. 验证目标状态
        ElectronicWarehouseReceipt.ReceiptStatus targetStatus;
        try {
            targetStatus = ElectronicWarehouseReceipt.ReceiptStatus.valueOf(request.getTargetStatus());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("无效的目标状态: " + request.getTargetStatus());
        }

        // 目标状态只能是正常、已质押或已转让
        if (targetStatus != ElectronicWarehouseReceipt.ReceiptStatus.NORMAL
                && targetStatus != ElectronicWarehouseReceipt.ReceiptStatus.PLEDGED
                && targetStatus != ElectronicWarehouseReceipt.ReceiptStatus.TRANSFERRED) {
            throw new RuntimeException("解冻后的目标状态只能是正常、已质押或已转让");
        }

        // 4. 权限验证
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        validateUnfreezePermission(authentication, request, receipt);

        // 5. 更新仓单状态
        receipt.setReceiptStatus(targetStatus);

        // 6. 在备注中记录解冻信息
        String unfreezeRemarks = String.format(
                "\n[解冻操作] 时间: %s | 目标状态: %s | 解冻原因: %s | 相关文件: %s | 操作人: %s",
                LocalDateTime.now(),
                targetStatus.name(),
                request.getUnfreezeReason(),
                request.getReferenceNo() != null ? request.getReferenceNo() : "无",
                operatorName
        );

        if (request.getRemarks() != null && !request.getRemarks().trim().isEmpty()) {
            unfreezeRemarks += " | 备注: " + request.getRemarks();
        }

        String existingRemarks = receipt.getRemarks();
        if (existingRemarks != null && !existingRemarks.trim().isEmpty()) {
            receipt.setRemarks(existingRemarks + unfreezeRemarks);
        } else {
            receipt.setRemarks(unfreezeRemarks.substring(1)); // 去掉开头的换行符
        }

        // 7. 更新审计信息
        receipt.setUpdatedBy(operatorId);

        // 8. 保存仓单
        repository.save(receipt);

        log.info("仓单解冻成功, ID: {}, 原状态: FROZEN, 新状态: {}",
                request.getReceiptId(), targetStatus);

        return ReceiptUnfreezeResponse.success(
                receipt.getId(),
                receipt.getReceiptNo(),
                targetStatus.name(),
                request.getUnfreezeReason(),
                request.getReferenceNo(),
                operatorName
        );
    }

    /**
     * 查询已冻结的仓单列表
     *
     * @param enterpriseId 企业ID（可选，不传则查询所有）
     * @return 已冻结的仓单列表
     */
    public List<ElectronicWarehouseReceiptResponse> getFrozenReceipts(String enterpriseId) {
        log.info("查询已冻结的仓单, 企业: {}", enterpriseId);

        List<ElectronicWarehouseReceipt> receipts;
        if (enterpriseId != null && !enterpriseId.trim().isEmpty()) {
            // 查询指定企业相关的已冻结仓单（作为货主、仓储方或持单人）
            receipts = repository.findByOwnerIdAndReceiptStatus(
                    enterpriseId,
                    ElectronicWarehouseReceipt.ReceiptStatus.FROZEN
            );
            // 如果没有找到，再尝试查询作为仓储方的
            if (receipts.isEmpty()) {
                receipts = repository.findByWarehouseIdAndReceiptStatus(
                        enterpriseId,
                        ElectronicWarehouseReceipt.ReceiptStatus.FROZEN
                );
            }
        } else {
            receipts = repository.findByReceiptStatus(
                    ElectronicWarehouseReceipt.ReceiptStatus.FROZEN
            );
        }

        log.info("找到 {} 条已冻结的仓单", receipts.size());
        return receipts.stream()
                .map(ElectronicWarehouseReceiptResponse::fromEntity)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 验证冻结权限
     * 根据操作方类型和当前用户权限进行验证
     */
    private void validateFreezePermission(Authentication authentication,
                                         ReceiptFreezeRequest request,
                                         ElectronicWarehouseReceipt receipt) {
        // 1. 仓储方只能冻结自己仓库的仓单
        // 2. 资金方只能冻结已融资给自己的仓单
        // 3. 平台方和法院可以冻结所有仓单

        String operatorType = request.getOperatorType();
        String operatorEnterpriseId = request.getOperatorEnterpriseId();

        switch (operatorType) {
            case "WAREHOUSE":
                // 仓储方只能冻结自己仓库的仓单
                if (!receipt.getWarehouseId().equals(operatorEnterpriseId)) {
                    throw new RuntimeException("仓储方只能冻结自己仓库的仓单");
                }
                break;
            case "FINANCIER":
                // 资金方只能冻结已融资给自己的仓单
                if (receipt.getFinancierAddress() == null ||
                    !receipt.getFinancierAddress().equals(operatorEnterpriseId)) {
                    throw new RuntimeException("资金方只能冻结已融资给自己的仓单");
                }
                break;
            case "PLATFORM":
            case "COURT":
                // 平台方和法院可以冻结所有仓单，不需要额外验证
                break;
            default:
                throw new RuntimeException("不支持的操作方类型: " + operatorType);
        }
    }

    /**
     * 验证解冻权限
     */
    private void validateUnfreezePermission(Authentication authentication,
                                           ReceiptUnfreezeRequest request,
                                           ElectronicWarehouseReceipt receipt) {
        // 只有管理员可以解冻
        if (adminFreezeOnly) {
            if (!hasAdminRole(authentication)) {
                throw new RuntimeException("只有管理员可以解冻仓单");
            }
        }
    }

    // ==================== 冻结申请-审核流程 ====================

    /**
     * 仓储方提交冻结申请
     *
     * @param request 冻结申请请求
     * @param applicantId 申请人ID
     * @param applicantName 申请人姓名
     * @return 申请响应
     */
    @Transactional
    public FreezeApplicationResponse submitFreezeApplication(
            FreezeApplicationSubmitRequest request,
            String applicantId,
            String applicantName) {
        log.info("提交冻结申请, 仓单ID: {}, 仓储企业: {}, 申请人: {}",
                request.getReceiptId(), request.getWarehouseId(), applicantName);

        // 1. 查询仓单
        ElectronicWarehouseReceipt receipt = repository.findById(request.getReceiptId())
                .orElseThrow(() -> new RuntimeException("仓单不存在: " + request.getReceiptId()));

        // 2. 验证仓单状态
        if (receipt.getReceiptStatus() != ElectronicWarehouseReceipt.ReceiptStatus.NORMAL
                && receipt.getReceiptStatus() != ElectronicWarehouseReceipt.ReceiptStatus.PLEDGED
                && receipt.getReceiptStatus() != ElectronicWarehouseReceipt.ReceiptStatus.TRANSFERRED) {
            throw new RuntimeException("只能申请冻结正常、已质押或已转让状态的仓单, 当前状态: " + receipt.getReceiptStatus());
        }

        if (receipt.getReceiptStatus() == ElectronicWarehouseReceipt.ReceiptStatus.FROZEN) {
            throw new RuntimeException("仓单已经是冻结状态，无需申请");
        }

        // 3. 验证申请仓储方是否为该仓单的仓储方
        if (!receipt.getWarehouseId().equals(request.getWarehouseId())) {
            throw new RuntimeException("只有仓单的仓储企业可以申请冻结");
        }

        // 4. 检查是否已有待审核的申请
        List<ReceiptFreezeApplication> pendingApps = freezeApplicationRepository
                .findByWarehouseIdAndStatus(request.getWarehouseId(), "PENDING");
        boolean hasPending = pendingApps.stream()
                .anyMatch(app -> app.getReceiptId().equals(request.getReceiptId()));
        if (hasPending) {
            throw new RuntimeException("该仓单已有待审核的冻结申请，请勿重复提交");
        }

        // 5. 创建冻结申请记录
        ReceiptFreezeApplication application = new ReceiptFreezeApplication();
        application.setId(UUID.randomUUID().toString());
        application.setReceiptId(request.getReceiptId());
        application.setReceiptNo(receipt.getReceiptNo());
        application.setWarehouseId(request.getWarehouseId());
        application.setWarehouseAddress(receipt.getWarehouseAddress());
        application.setWarehouseName(receipt.getWarehouseName());
        application.setFreezeReason(request.getFreezeReason());
        application.setFreezeType(request.getFreezeType());
        application.setReferenceNo(request.getReferenceNo());
        application.setApplicantId(applicantId);
        application.setApplicantName(applicantName);
        application.setRequestStatus("PENDING");
        application.setRemarks(request.getRemarks());

        // 6. 保存申请
        ReceiptFreezeApplication saved = freezeApplicationRepository.save(application);

        log.info("冻结申请提交成功, 申请ID: {}, 仓单ID: {}", saved.getId(), request.getReceiptId());

        return FreezeApplicationResponse.success(
                saved.getId(),
                saved.getReceiptId(),
                saved.getReceiptNo(),
                applicantName
        );
    }

    /**
     * 管理员审核冻结申请
     *
     * @param request 审核请求
     * @param reviewerId 审核人ID
     * @param reviewerName 审核人姓名
     * @return 审核响应
     */
    @Transactional
    public FreezeApplicationReviewResponse reviewFreezeApplication(
            FreezeApplicationReviewRequest request,
            String reviewerId,
            String reviewerName) {
        log.info("审核冻结申请, 申请ID: {}, 审核结果: {}, 审核人: {}",
                request.getApplicationId(), request.getReviewResult(), reviewerName);

        // 1. 查询申请
        ReceiptFreezeApplication application = freezeApplicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new RuntimeException("冻结申请不存在: " + request.getApplicationId()));

        // 2. 验证申请状态
        if (!"PENDING".equals(application.getRequestStatus())) {
            throw new RuntimeException("只能审核待审核状态的申请, 当前状态: " + application.getRequestStatus());
        }

        // 3. 验证管理员权限
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!hasAdminRole(authentication)) {
            throw new RuntimeException("只有管理员可以审核冻结申请");
        }

        // 4. 查询仓单
        String receiptId = application.getReceiptId();
        if (receiptId == null || receiptId.trim().isEmpty()) {
            throw new RuntimeException("申请中的仓单ID不能为空");
        }
        ElectronicWarehouseReceipt receipt = repository.findById(receiptId)
                .orElseThrow(() -> new RuntimeException("仓单不存在: " + receiptId));

        // 5. 根据审核结果处理
        if ("APPROVED".equals(request.getReviewResult())) {
            // ==================== 审核通过 ====================

            // 5.1 验证仓单状态（可能已经变化）
            if (receipt.getReceiptStatus() == ElectronicWarehouseReceipt.ReceiptStatus.FROZEN) {
                throw new RuntimeException("仓单已被冻结，无法再次冻结");
            }
            if (receipt.getReceiptStatus() != ElectronicWarehouseReceipt.ReceiptStatus.NORMAL
                    && receipt.getReceiptStatus() != ElectronicWarehouseReceipt.ReceiptStatus.PLEDGED
                    && receipt.getReceiptStatus() != ElectronicWarehouseReceipt.ReceiptStatus.TRANSFERRED) {
                throw new RuntimeException("仓单状态已变更，无法冻结: " + receipt.getReceiptStatus());
            }

            // 5.2 更新申请状态
            application.setRequestStatus("APPROVED");
            application.setReviewerId(reviewerId);
            application.setReviewerName(reviewerName);
            application.setReviewComments(request.getReviewComments());
            application.setReviewTime(LocalDateTime.now());
            application.setFreezeTime(LocalDateTime.now());

            // 5.3 冻结仓单并上链
            try {
                // 上链冻结
                String txHash = contractService.freezeReceiptOnChain(
                        receipt.getId(),
                        application.getFreezeReason(),
                        application.getReferenceNo()
                );

                // 获取区块号
                Long blockNumber = contractService.getBlockNumber(txHash);

                // 更新申请的上链信息
                application.setFreezeTxHash(txHash);
                application.setBlockNumber(blockNumber);

                // 更新仓单状态
                receipt.setReceiptStatus(ElectronicWarehouseReceipt.ReceiptStatus.FROZEN);

                // 在备注中记录冻结信息
                String freezeRemarks = String.format(
                        "\n[冻结操作] 时间: %s | 申请ID: %s | 操作方: ADMIN | 操作类型: %s | 冻结原因: %s | 相关文件: %s | 操作人: %s | 交易哈希: %s",
                        LocalDateTime.now(),
                        application.getId(),
                        application.getFreezeType(),
                        application.getFreezeReason(),
                        application.getReferenceNo() != null ? application.getReferenceNo() : "无",
                        reviewerName,
                        txHash
                );

                if (request.getRemarks() != null && !request.getRemarks().trim().isEmpty()) {
                    freezeRemarks += " | 备注: " + request.getRemarks();
                }

                String existingRemarks = receipt.getRemarks();
                if (existingRemarks != null && !existingRemarks.trim().isEmpty()) {
                    receipt.setRemarks(existingRemarks + freezeRemarks);
                } else {
                    receipt.setRemarks(freezeRemarks.substring(1));
                }

                receipt.setUpdatedBy(reviewerId);

                // 保存申请和仓单
                freezeApplicationRepository.save(application);
                repository.save(receipt);

                log.info("冻结申请审核通过，仓单已冻结并上链, 申请ID: {}, 仓单ID: {}, txHash: {}",
                        application.getId(), receipt.getId(), txHash);

                return FreezeApplicationReviewResponse.approved(
                        application.getId(),
                        receipt.getId(),
                        receipt.getReceiptNo(),
                        reviewerName,
                        txHash,
                        blockNumber
                );

            } catch (Exception e) {
                log.error("冻结仓单上链失败, 申请ID: {}, 仓单ID: {}, 错误: {}",
                        application.getId(), receipt.getId(), e.getMessage(), e);
                // 更新申请状态为失败，但不抛出异常，让前端可以处理
                application.setRequestStatus("FAILED");
                application.setReviewComments("上链失败: " + e.getMessage());
                freezeApplicationRepository.save(application);
                throw new RuntimeException("冻结仓单上链失败: " + e.getMessage(), e);
            }

        } else if ("REJECTED".equals(request.getReviewResult())) {
            // ==================== 审核拒绝 ====================

            String rejectionReason = request.getRejectionReason() != null
                    ? request.getRejectionReason()
                    : "审核方未提供具体原因";

            application.setRequestStatus("REJECTED");
            application.setReviewerId(reviewerId);
            application.setReviewerName(reviewerName);
            application.setReviewComments(request.getReviewComments());
            application.setRejectionReason(rejectionReason);
            application.setReviewTime(LocalDateTime.now());

            freezeApplicationRepository.save(application);

            log.info("冻结申请审核拒绝, 申请ID: {}, 原因: {}", application.getId(), rejectionReason);

            return FreezeApplicationReviewResponse.rejected(
                    application.getId(),
                    receipt.getId(),
                    receipt.getReceiptNo(),
                    reviewerName,
                    rejectionReason
            );

        } else {
            throw new RuntimeException("无效的审核结果: " + request.getReviewResult());
        }
    }

    /**
     * 查询待审核的冻结申请列表
     *
     * @return 待审核的冻结申请列表
     */
    public List<FreezeApplicationResponse> getPendingFreezeApplications() {
        log.info("查询待审核的冻结申请");

        List<ReceiptFreezeApplication> applications = freezeApplicationRepository.findPendingApplications();

        log.info("找到 {} 条待审核的冻结申请", applications.size());
        return applications.stream()
                .map(FreezeApplicationResponse::fromEntity)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 查询指定仓储企业的冻结申请列表
     *
     * @param warehouseId 仓储企业ID
     * @param status 申请状态（可选）
     * @return 冻结申请列表
     */
    public List<FreezeApplicationResponse> getFreezeApplicationsByWarehouse(
            String warehouseId,
            String status) {
        log.info("查询仓储企业的冻结申请, 仓储企业: {}, 状态: {}", warehouseId, status);

        List<ReceiptFreezeApplication> applications;
        if (status != null && !status.trim().isEmpty()) {
            applications = freezeApplicationRepository.findByWarehouseIdAndStatus(warehouseId, status);
        } else {
            applications = freezeApplicationRepository.findByWarehouseIdAndStatus(warehouseId, "PENDING");
        }

        log.info("找到 {} 条冻结申请", applications.size());
        return applications.stream()
                .map(FreezeApplicationResponse::fromEntity)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 检查用户是否具有管理员角色
     * 支持以下情况：
     * 1. User.UserType.ADMIN - 系统管理员
     * 2. loginType="ADMIN" - 管理员登录
     * 3. role="SUPER_ADMIN" - 超级管理员
     */
    private boolean hasAdminRole(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        // 获取所有权限
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> {
                    String authority = auth.getAuthority();
                    return "ROLE_ADMIN".equals(authority)
                        || "ROLE_SUPER_ADMIN".equals(authority)
                        || "ADMIN".equals(auth.getAuthority());
                });
    }

    // ==================== 仓单拆分功能 ====================

    @Autowired
    private ReceiptSplitApplicationRepository splitApplicationRepository;

    @Autowired
    private com.fisco.app.repository.warehouse.ReceiptCancelApplicationRepository cancelApplicationRepository;

    /**
     * 提交仓单拆分申请
     */
    @Transactional
    @SuppressWarnings("null")
    public SplitApplicationResponse submitSplitApplication(
            SplitApplicationRequest request,
            String applicantId,
            String applicantName) {

        log.info("提交仓单拆分申请: parentReceiptId={}, applicant={}",
            request.getParentReceiptId(), applicantName);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // ==================== 第1步：权限验证 ====================
        if (!(auth instanceof UserAuthentication)) {
            throw new com.fisco.app.exception.BusinessException("无效的认证信息");
        }
        UserAuthentication userAuth = (UserAuthentication) auth;

        // 查询父仓单
        ElectronicWarehouseReceipt parentReceipt = repository.findById(request.getParentReceiptId())
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException("仓单不存在: " + request.getParentReceiptId()));

        // 验证持单人权限（基于区块链地址）
        permissionChecker.checkHolderPermission(
            auth,
            parentReceipt.getHolderAddress(),
            "申请拆分仓单"
        );

        // 验证是否是货主企业
        if (!userAuth.getEnterpriseId().equals(parentReceipt.getOwnerId())) {
            throw new com.fisco.app.exception.BusinessException("只有货主企业可以申请拆分仓单");
        }

        // ==================== 第2步：边界检查 ====================
        validateSplitRequest(parentReceipt, request.getSplits());

        // ==================== 第3步：检查是否有待处理的拆分申请 ====================
        if (splitApplicationRepository.existsByParentReceiptIdAndRequestStatus(parentReceipt.getId(), "PENDING")) {
            throw new com.fisco.app.exception.BusinessException("该仓单已有待审核的拆分申请，请勿重复提交");
        }

        // ==================== 第4步：创建拆分申请 ====================
        ReceiptSplitApplication application = new ReceiptSplitApplication();
        application.setId(java.util.UUID.randomUUID().toString());
        application.setParentReceiptId(parentReceipt.getId());
        application.setParentReceiptNo(parentReceipt.getReceiptNo());
        application.setSplitReason(request.getSplitReason());
        application.setSplitCount(request.getSplits().size());

        // 将拆分详情转换为JSON存储
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            application.setSplitDetails(mapper.writeValueAsString(request.getSplits()));
        } catch (Exception e) {
            log.error("转换拆分详情为JSON失败", e);
            throw new com.fisco.app.exception.BusinessException("拆分详情格式错误");
        }

        application.setRequestStatus("PENDING");
        application.setApplicantId(applicantId);
        application.setApplicantName(applicantName);
        application.setRemarks(request.getRemarks());

        // 保存申请
        ReceiptSplitApplication savedApp = splitApplicationRepository.save(application);

        log.info("拆分申请创建成功: applicationId={}, parentReceiptId={}",
            savedApp.getId(), savedApp.getParentReceiptId());

        return SplitApplicationResponse.success(
            savedApp.getId(),
            parentReceipt.getId(),
            parentReceipt.getReceiptNo(),
            request.getSplitReason(),
            request.getSplits().size(),
            applicantId,
            applicantName
        );
    }

    /**
     * 审核仓单拆分申请
     */
    @Transactional
    @SuppressWarnings("null")
    public SplitApprovalResponse approveSplitApplication(
            SplitApprovalRequest request,
            String reviewerId,
            String reviewerName) {

        log.info("审核仓单拆分申请: applicationId={}, reviewer={}, result={}",
            request.getApplicationId(), reviewerName, request.getApprovalResult());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // ==================== 第1步：查询申请和仓单 ====================
        ReceiptSplitApplication application = splitApplicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException("拆分申请不存在"));

        ElectronicWarehouseReceipt parentReceipt = repository.findById(application.getParentReceiptId())
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException("父仓单不存在"));

        // ==================== 第2步：权限验证 ====================
        if (!(auth instanceof UserAuthentication)) {
            throw new com.fisco.app.exception.BusinessException("无效的认证信息");
        }
        UserAuthentication userAuth = (UserAuthentication) auth;

        // 系统管理员可以审核所有拆分申请
        if (!userAuth.isSystemAdmin() &&
            !userAuth.getEnterpriseId().equals(parentReceipt.getWarehouseId())) {
            throw new com.fisco.app.exception.BusinessException("无权限审核此拆分申请");
        }

        // ==================== 第3步：状态验证 ====================
        if (!"PENDING".equals(application.getRequestStatus())) {
            throw new com.fisco.app.exception.BusinessException(
                "该拆分申请已被" + application.getRequestStatus() + "，无法重复审核");
        }

        if (parentReceipt.getReceiptStatus() != ElectronicWarehouseReceipt.ReceiptStatus.NORMAL) {
            throw new com.fisco.app.exception.BusinessException(
                "仓单状态已变更，无法拆分。当前状态: " + parentReceipt.getReceiptStatus());
        }

        // ==================== 第4步：处理审核结果 ====================
        if ("APPROVED".equals(request.getApprovalResult())) {
            // 审核通过，执行拆分
            return executeSplit(application, parentReceipt, reviewerId, reviewerName);
        } else if ("REJECTED".equals(request.getApprovalResult())) {
            // 审核拒绝
            application.setRequestStatus("REJECTED");
            application.setReviewerId(reviewerId);
            application.setReviewerName(reviewerName);
            application.setReviewTime(java.time.LocalDateTime.now());
            application.setReviewComments(request.getApprovalComments());
            splitApplicationRepository.save(application);

            log.info("拆分申请审核拒绝: applicationId={}, reason={}",
                application.getId(), request.getApprovalComments());

            return SplitApprovalResponse.rejected(
                application.getId(),
                parentReceipt.getId(),
                parentReceipt.getReceiptNo(),
                reviewerName,
                request.getApprovalComments() != null ? request.getApprovalComments() : "未填写原因"
            );
        } else {
            throw new com.fisco.app.exception.BusinessException("无效的审核结果: " + request.getApprovalResult());
        }
    }

    /**
     * 执行仓单拆分
     */
    @SuppressWarnings("null")
    private SplitApprovalResponse executeSplit(
            ReceiptSplitApplication application,
            ElectronicWarehouseReceipt parentReceipt,
            String reviewerId,
            String reviewerName) {

        log.info("开始执行仓单拆分: parentReceiptId={}, splitCount={}",
            parentReceipt.getId(), application.getSplitCount());

        try {
            // ==================== 第1步：解析拆分详情 ====================
            java.util.List<SplitDetailRequest> splits;
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                splits = mapper.readValue(application.getSplitDetails(),
                    new com.fasterxml.jackson.core.type.TypeReference<java.util.List<SplitDetailRequest>>() {});
            } catch (Exception e) {
                log.error("解析拆分详情失败", e);
                throw new com.fisco.app.exception.BusinessException("拆分详情格式错误");
            }

            // ==================== 第2步：重新验证拆分规则 ====================
            validateSplitRequest(parentReceipt, splits);

            // ==================== 第3步：更新父仓单状态为拆分中 ====================
            parentReceipt.setReceiptStatus(ElectronicWarehouseReceipt.ReceiptStatus.SPLITTING);
            repository.save(parentReceipt);

            // ==================== 第4步：生成子仓单 ====================
            java.util.List<ElectronicWarehouseReceipt> childReceipts = new java.util.ArrayList<>();
            java.util.List<String> childIds = new java.util.ArrayList<>();
            java.util.List<BigInteger> childQuantities = new java.util.ArrayList<>();
            java.util.List<BigInteger> childTotalValues = new java.util.ArrayList<>();

            for (int i = 0; i < splits.size(); i++) {
                SplitDetailRequest split = splits.get(i);
                ElectronicWarehouseReceipt childReceipt = createChildReceipt(
                    parentReceipt,
                    split,
                    i + 1,
                    reviewerId,
                    reviewerName
                );

                childReceipts.add(childReceipt);
                childIds.add(childReceipt.getId());

                // 转换为BigInteger（智能合约使用uint256，需要乘以100保留2位小数）
                childQuantities.add(split.getQuantity()
                    .multiply(new java.math.BigDecimal("100"))
                    .toBigInteger());
                childTotalValues.add(split.getTotalValue()
                    .multiply(new java.math.BigDecimal("100"))
                    .toBigInteger());
            }

            // ==================== 第5步：保存所有子仓单 ====================
            for (ElectronicWarehouseReceipt child : childReceipts) {
                repository.save(child);
                log.debug("保存子仓单: id={}, receiptNo={}", child.getId(), child.getReceiptNo());
            }

            // ==================== 第6步：上链操作 ====================
            String txHash = contractService.splitReceiptOnChain(
                parentReceipt.getId(),
                childIds,
                splits.size()
            );

            log.info("拆分上链成功: txHash={}", txHash);

            // 获取区块号
            Long blockNumber = contractService.getBlockNumber(txHash);

            // ==================== 第7步：更新所有仓单的区块链信息 ====================
            // 父仓单
            parentReceipt.setReceiptStatus(ElectronicWarehouseReceipt.ReceiptStatus.SPLIT);
            parentReceipt.setSplitTime(java.time.LocalDateTime.now());
            parentReceipt.setSplitCount(splits.size());
            parentReceipt.setTxHash(txHash);
            parentReceipt.setBlockNumber(blockNumber);
            parentReceipt.setBlockchainStatus(ElectronicWarehouseReceipt.BlockchainStatus.SYNCED);
            parentReceipt.setBlockchainTimestamp(java.time.LocalDateTime.now());
            repository.save(parentReceipt);

            // 子仓单
            for (ElectronicWarehouseReceipt child : childReceipts) {
                child.setTxHash(txHash);
                child.setBlockNumber(blockNumber);
                child.setBlockchainStatus(ElectronicWarehouseReceipt.BlockchainStatus.SYNCED);
                child.setBlockchainTimestamp(java.time.LocalDateTime.now());
                repository.save(child);
            }

            // ==================== 第8步：更新申请状态 ====================
            application.setRequestStatus("APPROVED");
            application.setReviewerId(reviewerId);
            application.setReviewerName(reviewerName);
            application.setReviewTime(java.time.LocalDateTime.now());
            application.setSplitTxHash(txHash);
            application.setBlockNumber(blockNumber);
            splitApplicationRepository.save(application);

            log.info("仓单拆分执行完成: parentReceiptId={}, childCount={}, txHash={}",
                parentReceipt.getId(), childIds.size(), txHash);

            // ==================== 第9步：返回成功响应 ====================
            return SplitApprovalResponse.approved(
                application.getId(),
                parentReceipt.getId(),
                parentReceipt.getReceiptNo(),
                application.getSplitReason(),
                application.getSplitCount(),
                childIds,
                reviewerName,
                txHash,
                blockNumber
            );

        } catch (Exception e) {
            log.error("拆分执行失败，执行回滚: parentReceiptId={}", parentReceipt.getId(), e);

            // ==================== 回滚处理 ====================
            // 回滚1: 删除所有子仓单
            try {
                // 简化处理：恢复父仓单状态
                parentReceipt.setReceiptStatus(ElectronicWarehouseReceipt.ReceiptStatus.NORMAL);
                parentReceipt.setBlockchainStatus(ElectronicWarehouseReceipt.BlockchainStatus.SYNCED);
                repository.save(parentReceipt);
                log.info("已恢复父仓单状态: parentReceiptId={}", parentReceipt.getId());
            } catch (Exception rollbackException) {
                log.error("回滚失败", rollbackException);
            }

            // 更新申请状态为失败
            application.setRequestStatus("FAILED");
            application.setReviewComments("执行失败: " + e.getMessage());
            splitApplicationRepository.save(application);

            throw new com.fisco.app.exception.BusinessException("拆分执行失败: " + e.getMessage());
        }
    }

    /**
     * 查询子仓单列表
     */
    @SuppressWarnings("null")
    public java.util.List<ElectronicWarehouseReceiptResponse> getChildReceipts(String parentReceiptId) {
        log.info("查询子仓单列表: parentReceiptId={}", parentReceiptId);

        // 验证父仓单存在
        repository.findById(parentReceiptId)
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException("父仓单不存在"));

        // 查询子仓单
        java.util.List<ElectronicWarehouseReceipt> children = repository.findByParentReceiptId(parentReceiptId);

        log.info("找到 {} 个子仓单", children.size());

        return children.stream()
                .map(ElectronicWarehouseReceiptResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 查询父仓单
     */
    @SuppressWarnings("null")
    public ElectronicWarehouseReceiptResponse getParentReceipt(String childReceiptId) {
        log.info("查询父仓单: childReceiptId={}", childReceiptId);

        ElectronicWarehouseReceipt child = repository.findById(childReceiptId)
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException("仓单不存在"));

        String parentReceiptId = child.getParentReceiptId();
        if (parentReceiptId == null) {
            throw new com.fisco.app.exception.BusinessException("该仓单不是拆分生成的子仓单");
        }

        ElectronicWarehouseReceipt parent = repository.findById(parentReceiptId)
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException("父仓单不存在"));

        log.info("找到父仓单: parentId={}", parentReceiptId);

        return ElectronicWarehouseReceiptResponse.fromEntity(parent);
    }

    /**
     * 验证拆分请求
     */
    private void validateSplitRequest(
            ElectronicWarehouseReceipt parentReceipt,
            java.util.List<SplitDetailRequest> splits) {

        log.info("开始验证拆分请求: parentReceiptId={}, splitCount={}",
            parentReceipt.getId(), splits.size());

        // ==================== 第1层：仓单状态检查 ====================
        if (parentReceipt.getReceiptStatus() != ElectronicWarehouseReceipt.ReceiptStatus.NORMAL) {
            throw new com.fisco.app.exception.BusinessException(
                "只能拆分正常状态的仓单，当前状态: " + parentReceipt.getReceiptStatus());
        }

        if (parentReceipt.getBlockchainStatus() != ElectronicWarehouseReceipt.BlockchainStatus.SYNCED) {
            throw new com.fisco.app.exception.BusinessException("只能拆分已上链的仓单");
        }

        if (parentReceipt.getReceiptStatus() == ElectronicWarehouseReceipt.ReceiptStatus.FROZEN) {
            throw new com.fisco.app.exception.BusinessException("已冻结的仓单不能拆分");
        }

        if (parentReceipt.getReceiptStatus() == ElectronicWarehouseReceipt.ReceiptStatus.PLEDGED) {
            throw new com.fisco.app.exception.BusinessException("已质押的仓单不能拆分，请先释放");
        }

        if (parentReceipt.getReceiptStatus() == ElectronicWarehouseReceipt.ReceiptStatus.EXPIRED) {
            throw new com.fisco.app.exception.BusinessException("已过期的仓单不能拆分");
        }

        if (parentReceipt.getReceiptStatus() == ElectronicWarehouseReceipt.ReceiptStatus.SPLIT ||
            parentReceipt.getReceiptStatus() == ElectronicWarehouseReceipt.ReceiptStatus.SPLITTING) {
            throw new com.fisco.app.exception.BusinessException("该仓单已被拆分，无法再次拆分");
        }

        // ==================== 第2层：拆分规则检查 ====================
        if (splits == null || splits.size() < 2) {
            throw new com.fisco.app.exception.BusinessException("至少需要拆分成2个子仓单");
        }

        if (splits.size() > 10) {
            throw new com.fisco.app.exception.BusinessException("单次拆分最多生成10个子仓单");
        }

        // ==================== 第3层：数量和价值检查 ====================
        java.math.BigDecimal totalSplitQuantity = java.math.BigDecimal.ZERO;
        java.math.BigDecimal totalSplitValue = java.math.BigDecimal.ZERO;

        for (int i = 0; i < splits.size(); i++) {
            SplitDetailRequest split = splits.get(i);

            // 数量必须大于0
            if (split.getQuantity() == null || split.getQuantity().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                throw new com.fisco.app.exception.BusinessException(
                    String.format("第%d个子仓单的数量必须大于0", i + 1));
            }

            // 单价必须大于0
            if (split.getUnitPrice() == null || split.getUnitPrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                throw new com.fisco.app.exception.BusinessException(
                    String.format("第%d个子仓单的单价必须大于0", i + 1));
            }

            // 计算总价值
            java.math.BigDecimal splitValue = split.getQuantity().multiply(split.getUnitPrice());

            // 验证计算的总价值是否正确
            if (split.getTotalValue() == null ||
                split.getTotalValue().compareTo(splitValue) != 0) {
                throw new com.fisco.app.exception.BusinessException(
                    String.format("第%d个子仓单的总价值计算错误: 数量 × 单价 = %s，但提供的是 %s",
                        i + 1, splitValue, split.getTotalValue()));
            }

            totalSplitQuantity = totalSplitQuantity.add(split.getQuantity());
            totalSplitValue = totalSplitValue.add(split.getTotalValue());
        }

        // 拆分后的数量总和必须等于原仓单数量
        if (totalSplitQuantity.compareTo(parentReceipt.getQuantity()) != 0) {
            throw new com.fisco.app.exception.BusinessException(
                String.format("拆分后的数量总和(%s)必须等于原仓单数量(%s)",
                    totalSplitQuantity, parentReceipt.getQuantity()));
        }

        // 拆分后的总价值必须等于原仓单价值
        if (totalSplitValue.compareTo(parentReceipt.getTotalValue()) != 0) {
            throw new com.fisco.app.exception.BusinessException(
                String.format("拆分后的总价值(%s)必须等于原仓单价值(%s)",
                    totalSplitValue, parentReceipt.getTotalValue()));
        }

        // ==================== 第4层：货物信息检查 ====================
        String parentGoodsName = parentReceipt.getGoodsName();
        for (int i = 0; i < splits.size(); i++) {
            if (!parentGoodsName.equals(splits.get(i).getGoodsName())) {
                throw new com.fisco.app.exception.BusinessException(
                    String.format("第%d个子仓单的货物名称必须与父仓单一致", i + 1));
            }
        }

        String parentUnit = parentReceipt.getUnit();
        for (int i = 0; i < splits.size(); i++) {
            if (!parentUnit.equals(splits.get(i).getUnit())) {
                throw new com.fisco.app.exception.BusinessException(
                    String.format("第%d个子仓单的计量单位必须与父仓单一致", i + 1));
            }
        }

        for (int i = 0; i < splits.size(); i++) {
            if (splits.get(i).getUnitPrice().compareTo(parentReceipt.getUnitPrice()) != 0) {
                throw new com.fisco.app.exception.BusinessException(
                    String.format("第%d个子仓单的单价必须与父仓单一致", i + 1));
            }
        }

        // ==================== 第5层：存储位置检查 ====================
        java.util.Set<String> locations = new java.util.HashSet<>();
        for (int i = 0; i < splits.size(); i++) {
            String location = splits.get(i).getStorageLocation();
            if (location == null || location.trim().isEmpty()) {
                throw new com.fisco.app.exception.BusinessException(
                    String.format("第%d个子仓单的存储位置不能为空", i + 1));
            }
            if (locations.contains(location)) {
                throw new com.fisco.app.exception.BusinessException(
                    String.format("第%d个子仓单的存储位置重复", i + 1));
            }
            locations.add(location);
        }

        log.info("拆分请求验证通过: parentReceiptId={}", parentReceipt.getId());
    }

    /**
     * 创建子仓单
     */
    @SuppressWarnings("null")
    private ElectronicWarehouseReceipt createChildReceipt(
            ElectronicWarehouseReceipt parent,
            SplitDetailRequest split,
            int index,
            String operatorId,
            String operatorName) {

        ElectronicWarehouseReceipt child = new ElectronicWarehouseReceipt();

        // 继承父仓单的所有基础信息
        child.setId(java.util.UUID.randomUUID().toString());
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
        child.setRemarks(split.getRemarks());

        // 生成子仓单编号
        child.setReceiptNo(generateChildReceiptNo(parent.getReceiptNo(), index));

        // 设置初始状态
        child.setReceiptStatus(ElectronicWarehouseReceipt.ReceiptStatus.NORMAL);
        child.setBlockchainStatus(ElectronicWarehouseReceipt.BlockchainStatus.PENDING);
        child.setEndorsementCount(0);
        child.setCurrentHolder(parent.getCurrentHolder());

        // 审计信息
        child.setCreatedBy(parent.getCreatedBy());
        child.setCreatedAt(java.time.LocalDateTime.now());
        child.setUpdatedAt(java.time.LocalDateTime.now());
        child.setUpdatedBy(operatorId);

        log.info("创建子仓单: id={}, receiptNo={}, quantity={}",
            child.getId(), child.getReceiptNo(), child.getQuantity());

        return child;
    }

    /**
     * 生成子仓单编号
     */
    private String generateChildReceiptNo(String parentReceiptNo, int index) {
        return parentReceiptNo + "-" + String.format("%02d", index);
    }

    // ==================== 仓单作废相关方法 ====================

    /**
     * 提交仓单作废申请
     */
    @Transactional
    @SuppressWarnings("null")
    public CancelApplicationResponse submitCancelApplication(
            CancelApplicationRequest request,
            String applicantId,
            String applicantName) {

        log.info("提交仓单作废申请: receiptId={}, applicant={}",
            request.getReceiptId(), applicantName);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // ==================== 第1步：权限验证 ====================
        if (!(auth instanceof UserAuthentication)) {
            throw new com.fisco.app.exception.BusinessException("无效的认证信息");
        }
        UserAuthentication userAuth = (UserAuthentication) auth;

        // 查询仓单
        ElectronicWarehouseReceipt receipt = repository.findById(request.getReceiptId())
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException("仓单不存在: " + request.getReceiptId()));

        // 验证持单人权限（基于区块链地址）
        permissionChecker.checkHolderPermission(
            auth,
            receipt.getHolderAddress(),
            "申请作废仓单"
        );

        // 验证是否是货主企业
        if (!userAuth.getEnterpriseId().equals(receipt.getOwnerId())) {
            throw new com.fisco.app.exception.BusinessException("只有货主企业可以申请作废仓单");
        }

        // ==================== 第2步：边界检查 ====================
        validateCancelRequest(receipt);

        // ==================== 第3步：检查是否有待处理的作废申请 ====================
        if (cancelApplicationRepository.existsPendingCancelApplication(receipt.getId())) {
            throw new com.fisco.app.exception.BusinessException("该仓单已有待审核的作废申请，请勿重复提交");
        }

        // ==================== 第4步：创建作废申请 ====================
        ReceiptCancelApplication application = new ReceiptCancelApplication();
        application.setId(java.util.UUID.randomUUID().toString());
        application.setReceiptId(receipt.getId());
        application.setCancelReason(request.getCancelReason());
        application.setCancelType(request.getCancelType());
        application.setEvidence(request.getEvidence());
        application.setReferenceNo(request.getReferenceNo());
        application.setRequestStatus("PENDING");
        application.setApplicantId(applicantId);
        application.setApplicantName(applicantName);
        application.setRemarks(request.getRemarks());

        // 保存申请
        ReceiptCancelApplication savedApp = cancelApplicationRepository.save(application);

        // 更新仓单状态为作废中
        receipt.setReceiptStatus(ElectronicWarehouseReceipt.ReceiptStatus.CANCELLING);
        repository.save(receipt);

        log.info("作废申请创建成功: applicationId={}, receiptId={}",
            savedApp.getId(), savedApp.getReceiptId());

        return CancelApplicationResponse.success(
            savedApp.getId(),
            receipt.getId(),
            receipt.getReceiptNo(),
            "NORMAL",
            "CANCELLING",
            request.getCancelReason(),
            request.getCancelType(),
            applicantId,
            applicantName
        );
    }

    /**
     * 审核仓单作废申请
     */
    @Transactional
    @SuppressWarnings("null")
    public CancelApprovalResponse approveCancelApplication(
            CancelApprovalRequest request,
            String reviewerId,
            String reviewerName) {

        log.info("审核仓单作废申请: applicationId={}, reviewer={}, result={}",
            request.getApplicationId(), reviewerName, request.getApprovalResult());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // ==================== 第1步：查询申请和仓单 ====================
        ReceiptCancelApplication application = cancelApplicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException("作废申请不存在"));

        ElectronicWarehouseReceipt receipt = repository.findById(application.getReceiptId())
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException("仓单不存在"));

        // ==================== 第2步：权限验证 ====================
        if (!(auth instanceof UserAuthentication)) {
            throw new com.fisco.app.exception.BusinessException("无效的认证信息");
        }
        UserAuthentication userAuth = (UserAuthentication) auth;

        // 系统管理员可以审核所有作废申请
        if (!userAuth.isSystemAdmin() &&
            !userAuth.getEnterpriseId().equals(receipt.getWarehouseId())) {
            throw new com.fisco.app.exception.BusinessException("无权限审核此作废申请");
        }

        // ==================== 第3步：状态验证 ====================
        if (!"PENDING".equals(application.getRequestStatus())) {
            throw new com.fisco.app.exception.BusinessException(
                "该作废申请已被" + application.getRequestStatus() + "，无法重复审核");
        }

        if (receipt.getReceiptStatus() != ElectronicWarehouseReceipt.ReceiptStatus.CANCELLING) {
            throw new com.fisco.app.exception.BusinessException(
                "仓单状态已变更，无法作废。当前状态: " + receipt.getReceiptStatus());
        }

        // ==================== 第4步：处理审核结果 ====================
        if ("APPROVED".equals(request.getApprovalResult())) {
            // 审核通过，执行作废
            return executeCancel(application, receipt, reviewerId, reviewerName);
        } else if ("REJECTED".equals(request.getApprovalResult())) {
            // 审核拒绝
            application.setRequestStatus("REJECTED");
            application.setReviewerId(reviewerId);
            application.setReviewerName(reviewerName);
            application.setReviewTime(java.time.LocalDateTime.now());
            application.setReviewComments(request.getApprovalComments());
            cancelApplicationRepository.save(application);

            // 恢复仓单状态
            receipt.setReceiptStatus(ElectronicWarehouseReceipt.ReceiptStatus.NORMAL);
            repository.save(receipt);

            log.info("作废申请审核拒绝: applicationId={}, reason={}",
                application.getId(), request.getApprovalComments());

            return CancelApprovalResponse.rejected(
                application.getId(),
                receipt.getId(),
                receipt.getReceiptNo(),
                reviewerName,
                request.getApprovalComments() != null ? request.getApprovalComments() : "未填写原因"
            );
        } else {
            throw new com.fisco.app.exception.BusinessException("无效的审核结果: " + request.getApprovalResult());
        }
    }

    /**
     * 执行仓单作废
     */
    private CancelApprovalResponse executeCancel(
            ReceiptCancelApplication application,
            ElectronicWarehouseReceipt receipt,
            String reviewerId,
            String reviewerName) {

        log.info("开始执行仓单作废: receiptId={}", receipt.getId());

        try {
            // ==================== 第1步：更新仓单状态为已作废 ====================
            receipt.setReceiptStatus(ElectronicWarehouseReceipt.ReceiptStatus.CANCELLED);
            receipt.setCancelReason(application.getCancelReason());
            receipt.setCancelType(application.getCancelType());
            receipt.setCancelTime(java.time.LocalDateTime.now());
            receipt.setCancelledBy(reviewerId);
            receipt.setReferenceNo(application.getReferenceNo());
            repository.save(receipt);

            // ==================== 第2步：上链操作（可选） ====================
            String txHash = null;
            Long blockNumber = null;
            try {
                txHash = contractService.cancelReceiptOnChain(
                    receipt.getId(),
                    application.getCancelReason()
                );

                if (txHash != null) {
                    blockNumber = contractService.getBlockNumber(txHash);
                    receipt.setTxHash(txHash);
                    receipt.setBlockNumber(blockNumber);
                    receipt.setBlockchainStatus(ElectronicWarehouseReceipt.BlockchainStatus.SYNCED);
                    receipt.setBlockchainTimestamp(java.time.LocalDateTime.now());
                    repository.save(receipt);
                }

                log.info("作废上链成功: txHash={}", txHash);
            } catch (Exception e) {
                log.warn("作废上链失败（不影响作废操作）: {}", e.getMessage());
            }

            // ==================== 第3步：更新申请状态 ====================
            application.setRequestStatus("APPROVED");
            application.setReviewerId(reviewerId);
            application.setReviewerName(reviewerName);
            application.setReviewTime(java.time.LocalDateTime.now());
            application.setReviewComments("审核通过");
            application.setCancelTxHash(txHash);
            application.setBlockNumber(blockNumber);
            cancelApplicationRepository.save(application);

            log.info("仓单作废执行完成: receiptId={}", receipt.getId());

            // ==================== 第4步：返回成功响应 ====================
            return CancelApprovalResponse.approved(
                application.getId(),
                receipt.getId(),
                receipt.getReceiptNo(),
                application.getCancelReason(),
                application.getCancelType(),
                reviewerName,
                txHash,
                blockNumber
            );

        } catch (Exception e) {
            log.error("作废执行失败: receiptId={}", receipt.getId(), e);
            throw new com.fisco.app.exception.BusinessException("作废执行失败: " + e.getMessage());
        }
    }

    /**
     * 查询已作废的仓单列表（带权限验证）
     */
    public java.util.List<ElectronicWarehouseReceiptResponse> getCancelledReceipts(
            String enterpriseId,
            Authentication auth) {

        log.info("查询已作废的仓单: enterpriseId={}", enterpriseId);

        // ==================== 权限验证 ====================
        if (!(auth instanceof UserAuthentication)) {
            throw new com.fisco.app.exception.BusinessException("无效的认证信息");
        }
        UserAuthentication userAuth = (UserAuthentication) auth;

        // 如果提供了enterpriseId，验证权限
        if (enterpriseId != null && !enterpriseId.isEmpty()) {
            // 管理员可以查询任何企业
            if (!userAuth.isSystemAdmin() &&
                !userAuth.getEnterpriseId().equals(enterpriseId)) {
                throw new com.fisco.app.exception.BusinessException(
                    "无权限查询该企业的已作废仓单");
            }
        } else {
            // 未提供enterpriseId，默认查询当前用户企业的
            enterpriseId = userAuth.getEnterpriseId();
            log.info("未指定企业ID，使用当前用户企业: {}", enterpriseId);
        }

        // 查询指定企业的已作废仓单
        java.util.List<ElectronicWarehouseReceipt> receipts =
            repository.findByOwnerIdAndReceiptStatus(
                enterpriseId,
                ElectronicWarehouseReceipt.ReceiptStatus.CANCELLED
            );

        log.info("找到 {} 个已作废的仓单（企业: {}）", receipts.size(), enterpriseId);

        return receipts.stream()
                .map(ElectronicWarehouseReceiptResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 查询待审核的作废申请列表（带权限验证）
     */
    public java.util.List<ReceiptCancelApplication> getPendingCancelApplications(Authentication auth) {
        log.info("查询待审核的作废申请");

        // ==================== 权限验证 ====================
        if (!(auth instanceof UserAuthentication)) {
            throw new com.fisco.app.exception.BusinessException("无效的认证信息");
        }
        UserAuthentication userAuth = (UserAuthentication) auth;

        java.util.List<ReceiptCancelApplication> applications;

        // 管理员可以查询所有待审核申请
        if (userAuth.isSystemAdmin()) {
            applications = cancelApplicationRepository.findPendingApplications();
            log.info("管理员查询所有待审核的作废申请");
        }
        // 仓储企业只能查询自己仓单的待审核申请
        else if (userAuth.getEnterpriseId() != null) {
            applications = cancelApplicationRepository.findPendingApplicationsByWarehouse(
                userAuth.getEnterpriseId());
            log.info("仓储企业查询待审核的作废申请: warehouseId={}", userAuth.getEnterpriseId());
        } else {
            throw new com.fisco.app.exception.BusinessException("无权限查询待审核的作废申请");
        }

        log.info("找到 {} 个待审核的作废申请", applications.size());

        return applications;
    }

    /**
     * 验证作废请求
     */
    private void validateCancelRequest(ElectronicWarehouseReceipt receipt) {
        log.info("开始验证作废请求: receiptId={}, status={}",
            receipt.getId(), receipt.getReceiptStatus());

        // 状态检查：只能作废NORMAL或ONCHAIN_FAILED状态的仓单
        if (receipt.getReceiptStatus() != ElectronicWarehouseReceipt.ReceiptStatus.NORMAL &&
            receipt.getReceiptStatus() != ElectronicWarehouseReceipt.ReceiptStatus.ONCHAIN_FAILED) {
            throw new com.fisco.app.exception.BusinessException(
                "只有正常或上链失败状态的仓单可以申请作废。当前状态: " + receipt.getReceiptStatus());
        }

        // 业务规则检查
        if (receipt.getReceiptStatus() == ElectronicWarehouseReceipt.ReceiptStatus.DELIVERED) {
            throw new com.fisco.app.exception.BusinessException("已提货的仓单不能作废");
        }

        if (receipt.getReceiptStatus() == ElectronicWarehouseReceipt.ReceiptStatus.PLEDGED) {
            throw new com.fisco.app.exception.BusinessException("已质押的仓单需要先释放才能作废");
        }

        if (receipt.getReceiptStatus() == ElectronicWarehouseReceipt.ReceiptStatus.FROZEN) {
            throw new com.fisco.app.exception.BusinessException("已冻结的仓单需要先解冻才能作废");
        }

        if (receipt.getReceiptStatus() == ElectronicWarehouseReceipt.ReceiptStatus.SPLIT ||
            receipt.getReceiptStatus() == ElectronicWarehouseReceipt.ReceiptStatus.SPLITTING) {
            throw new com.fisco.app.exception.BusinessException("已拆分或拆分中的仓单不能作废");
        }

        log.info("作废请求验证通过");
    }

    // ==================== 仓单合并功能 ====================

    @Autowired
    private com.fisco.app.repository.warehouse.ReceiptMergeApplicationRepository mergeApplicationRepository;

    @Autowired
    private com.fisco.app.repository.warehouse.ReceiptChangeHistoryRepository changeHistoryRepository;

    /**
     * 提交仓单合并申请
     *
     * @param request 合并请求
     * @param applicantId 申请人ID
     * @param applicantName 申请人姓名
     * @return 合并申请响应
     */
    @Transactional
    @SuppressWarnings("null")
    public com.fisco.app.dto.warehouse.ReceiptMergeResponse submitMergeApplication(
            com.fisco.app.dto.warehouse.MergeReceiptsRequest request,
            String applicantId,
            String applicantName) {

        log.info("提交仓单合并申请: receiptCount={}, mergeType={}, applicant={}",
            request.getReceiptIds().size(), request.getMergeType(), applicantName);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // ==================== 第1步：权限验证 ====================
        if (!(auth instanceof UserAuthentication)) {
            throw new com.fisco.app.exception.BusinessException("无效的认证信息");
        }
        UserAuthentication userAuth = (UserAuthentication) auth;

        // ==================== 第2步：查询并验证源仓单 ====================
        if (request.getReceiptIds().size() < 2 || request.getReceiptIds().size() > 10) {
            throw new com.fisco.app.exception.BusinessException("合并仓单数量必须在2-10个之间");
        }

        // 验证源仓单ID是否有重复
        long distinctCount = request.getReceiptIds().stream().distinct().count();
        if (distinctCount != request.getReceiptIds().size()) {
            throw new com.fisco.app.exception.BusinessException("源仓单ID列表中存在重复ID");
        }

        java.util.List<ElectronicWarehouseReceipt> sourceReceipts = new java.util.ArrayList<>();
        String firstOwnerId = null;

        for (String receiptId : request.getReceiptIds()) {
            ElectronicWarehouseReceipt receipt = repository.findById(receiptId)
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException("仓单不存在: " + receiptId));

            // 验证状态：所有仓单必须为NORMAL状态
            if (receipt.getReceiptStatus() != ElectronicWarehouseReceipt.ReceiptStatus.NORMAL) {
                throw new com.fisco.app.exception.BusinessException(
                    "仓单" + receipt.getReceiptNo() + "状态不正常，当前状态: " + receipt.getReceiptStatus());
            }

            // 验证所有权：所有仓单必须属于同一货主企业
            if (firstOwnerId == null) {
                firstOwnerId = receipt.getOwnerId();
            } else if (!firstOwnerId.equals(receipt.getOwnerId())) {
                throw new com.fisco.app.exception.BusinessException(
                    "所有仓单必须属于同一货主企业");
            }

            // 验证持单人权限
            permissionChecker.checkHolderPermission(
                auth,
                receipt.getHolderAddress(),
                "申请合并仓单"
            );

            // 验证是否是货主企业
            if (!userAuth.getEnterpriseId().equals(receipt.getOwnerId())) {
                throw new com.fisco.app.exception.BusinessException(
                    "只有货主企业可以申请合并仓单");
            }

            // 检查是否有待处理的合并申请
            if (mergeApplicationRepository.existsByRequestStatusAndSourceReceiptIdsContaining(
                "PENDING", receiptId)) {
                throw new com.fisco.app.exception.BusinessException(
                    "仓单" + receipt.getReceiptNo() + "已有待审核的合并申请，请勿重复提交");
            }

            sourceReceipts.add(receipt);
        }

        // ==================== 第3步：验证合并类型和业务规则 ====================
        validateMergeRequest(sourceReceipts, request.getMergeType());

        // ==================== 第4步：计算合并后的数量和价值 ====================
        BigDecimal totalQuantity = sourceReceipts.stream()
            .map(ElectronicWarehouseReceipt::getQuantity)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalValue = sourceReceipts.stream()
            .map(ElectronicWarehouseReceipt::getTotalValue)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ==================== 第5步：创建合并申请 ====================
        com.fisco.app.entity.warehouse.ReceiptMergeApplication application =
            new com.fisco.app.entity.warehouse.ReceiptMergeApplication();
        application.setId(java.util.UUID.randomUUID().toString());

        // 将源仓单ID列表转换为JSON存储
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            application.setSourceReceiptIds(mapper.writeValueAsString(request.getReceiptIds()));

            // 构建合并明细
            java.util.Map<String, Object> mergeDetails = new java.util.HashMap<>();
            mergeDetails.put("sourceReceipts", sourceReceipts.stream()
                .map(r -> {
                    java.util.Map<String, Object> info = new java.util.HashMap<>();
                    info.put("id", r.getId());
                    info.put("receiptNo", r.getReceiptNo());
                    info.put("quantity", r.getQuantity());
                    info.put("totalValue", r.getTotalValue());
                    info.put("goodsName", r.getGoodsName());
                    return info;
                })
                .collect(java.util.stream.Collectors.toList()));
            mergeDetails.put("totalQuantity", totalQuantity);
            mergeDetails.put("totalValue", totalValue);
            mergeDetails.put("mergeType", request.getMergeType().name());

            application.setMergeDetails(mapper.writeValueAsString(mergeDetails));
        } catch (Exception e) {
            log.error("转换合并详情为JSON失败", e);
            throw new com.fisco.app.exception.BusinessException("合并详情格式错误");
        }

        application.setMergeType(request.getMergeType().name());
        application.setTotalQuantity(totalQuantity);
        application.setTotalValue(totalValue);
        application.setRequestStatus("PENDING");
        application.setApplicantId(applicantId);
        application.setApplicantName(applicantName);
        application.setRemarks(request.getRemarks());

        // 保存申请
        com.fisco.app.entity.warehouse.ReceiptMergeApplication savedApp = mergeApplicationRepository.save(application);

        log.info("合并申请创建成功: applicationId={}, sourceReceiptCount={}, totalValue={}",
            savedApp.getId(), request.getReceiptIds().size(), totalValue);

        // 构建响应
        return buildMergeResponse(savedApp, sourceReceipts, null, "success", "仓单合并申请已提交");
    }

    /**
     * 审核仓单合并申请
     *
     * @param request 审核请求
     * @param reviewerId 审核人ID
     * @param reviewerName 审核人姓名
     * @return 审核响应
     */
    @Transactional
    @SuppressWarnings("null")
    public com.fisco.app.dto.warehouse.ReceiptMergeResponse approveMergeApplication(
            com.fisco.app.dto.warehouse.MergeApprovalRequest request,
            String reviewerId,
            String reviewerName) {

        log.info("审核仓单合并申请: applicationId={}, reviewer={}, result={}",
            request.getApplicationId(), reviewerName, request.getApproved());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // ==================== 第1步：查询申请和源仓单 ====================
        com.fisco.app.entity.warehouse.ReceiptMergeApplication application =
            mergeApplicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException("合并申请不存在"));

        // 解析源仓单ID列表
        java.util.List<String> sourceReceiptIds;
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            sourceReceiptIds = mapper.readValue(application.getSourceReceiptIds(),
                new com.fasterxml.jackson.core.type.TypeReference<java.util.List<String>>() {});
        } catch (Exception e) {
            log.error("解析源仓单ID列表失败", e);
            throw new com.fisco.app.exception.BusinessException("源仓单ID列表格式错误");
        }

        java.util.List<ElectronicWarehouseReceipt> sourceReceipts = new java.util.ArrayList<>();
        for (String receiptId : sourceReceiptIds) {
            ElectronicWarehouseReceipt receipt = repository.findById(receiptId)
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException("源仓单不存在: " + receiptId));
            sourceReceipts.add(receipt);
        }

        // ==================== 第2步：权限验证 ====================
        if (!(auth instanceof UserAuthentication)) {
            throw new com.fisco.app.exception.BusinessException("无效的认证信息");
        }
        UserAuthentication userAuth = (UserAuthentication) auth;

        // 系统管理员可以审核所有合并申请
        ElectronicWarehouseReceipt firstReceipt = sourceReceipts.get(0);
        if (!userAuth.isSystemAdmin() &&
            !userAuth.getEnterpriseId().equals(firstReceipt.getWarehouseId())) {
            throw new com.fisco.app.exception.BusinessException("无权限审核此合并申请");
        }

        // ==================== 第3步：状态验证 ====================
        if (!"PENDING".equals(application.getRequestStatus())) {
            throw new com.fisco.app.exception.BusinessException(
                "该合并申请已被" + application.getRequestStatus() + "，无法重复审核");
        }

        for (ElectronicWarehouseReceipt receipt : sourceReceipts) {
            if (receipt.getReceiptStatus() != ElectronicWarehouseReceipt.ReceiptStatus.NORMAL) {
                throw new com.fisco.app.exception.BusinessException(
                    "仓单" + receipt.getReceiptNo() + "状态已变更，无法合并。当前状态: " + receipt.getReceiptStatus());
            }
        }

        // ==================== 第4步：处理审核结果 ====================
        if (Boolean.TRUE.equals(request.getApproved())) {
            // 审核通过，执行合并
            return executeMerge(application, sourceReceipts, reviewerId, reviewerName);
        } else {
            // 审核拒绝
            application.setRequestStatus("REJECTED");
            application.setReviewerId(reviewerId);
            application.setReviewerName(reviewerName);
            application.setReviewTime(java.time.LocalDateTime.now());
            application.setReviewComments(request.getReviewComments());
            mergeApplicationRepository.save(application);

            log.info("合并申请审核拒绝: applicationId={}, reason={}",
                application.getId(), request.getReviewComments());

            return buildMergeResponse(application, sourceReceipts, null, "rejected",
                request.getReviewComments() != null ? request.getReviewComments() : "未填写原因");
        }
    }

    /**
     * 执行仓单合并
     */
    @SuppressWarnings("null")
    private com.fisco.app.dto.warehouse.ReceiptMergeResponse executeMerge(
            com.fisco.app.entity.warehouse.ReceiptMergeApplication application,
            java.util.List<ElectronicWarehouseReceipt> sourceReceipts,
            String reviewerId,
            String reviewerName) {

        log.info("开始执行仓单合并: sourceReceiptCount={}, totalValue={}",
            sourceReceipts.size(), application.getTotalValue());

        try {
            // ==================== 第1步：更新源仓单状态为MERGING ====================
            for (ElectronicWarehouseReceipt receipt : sourceReceipts) {
                receipt.setReceiptStatus(ElectronicWarehouseReceipt.ReceiptStatus.MERGING);
                repository.save(receipt);
                log.info("源仓单状态更新为MERGING: receiptId={}", receipt.getId());
            }

            // ==================== 第2步：创建合并后的仓单 ====================
            ElectronicWarehouseReceipt mergedReceipt = new ElectronicWarehouseReceipt();
            mergedReceipt.setId(java.util.UUID.randomUUID().toString());

            // 生成新的仓单编号（使用M前缀标识合并仓单）
            String receiptNo = generateMergeReceiptNo(sourceReceipts.size());
            mergedReceipt.setReceiptNo(receiptNo);

            // 继承第一个源仓单的基本信息
            ElectronicWarehouseReceipt firstReceipt = sourceReceipts.get(0);
            mergedReceipt.setOwnerId(firstReceipt.getOwnerId());
            mergedReceipt.setOwnerName(firstReceipt.getOwnerName());
            mergedReceipt.setWarehouseId(firstReceipt.getWarehouseId());
            mergedReceipt.setWarehouseName(firstReceipt.getWarehouseName());
            mergedReceipt.setWarehouseLocation(firstReceipt.getWarehouseLocation());
            mergedReceipt.setStorageLocation(firstReceipt.getStorageLocation());

            // 合并货物信息
            mergedReceipt.setGoodsName(firstReceipt.getGoodsName());
            mergedReceipt.setGoodsType(firstReceipt.getGoodsType());
            mergedReceipt.setQuantity(application.getTotalQuantity());
            mergedReceipt.setUnit(firstReceipt.getUnit());
            mergedReceipt.setUnitPrice(firstReceipt.getUnitPrice());
            mergedReceipt.setTotalValue(application.getTotalValue());
            mergedReceipt.setMarketPrice(firstReceipt.getMarketPrice());

            // 设置日期信息
            mergedReceipt.setWarehouseEntryDate(firstReceipt.getWarehouseEntryDate());
            mergedReceipt.setExpiryDate(firstReceipt.getExpiryDate());

            // 设置持有人信息
            mergedReceipt.setHolderAddress(firstReceipt.getHolderAddress());

            // 设置合并相关信息
            mergedReceipt.setMergeCount(sourceReceipts.size());
            mergedReceipt.setMergeTime(java.time.LocalDateTime.now());
            mergedReceipt.setSourceReceiptIds(application.getSourceReceiptIds());

            // 设置状态
            mergedReceipt.setReceiptStatus(ElectronicWarehouseReceipt.ReceiptStatus.NORMAL);
            mergedReceipt.setBlockchainStatus(ElectronicWarehouseReceipt.BlockchainStatus.PENDING);

            // 设置创建人信息
            mergedReceipt.setCreatedBy(reviewerId);
            mergedReceipt.setUpdatedBy(reviewerId);

            // 保存合并仓单
            ElectronicWarehouseReceipt savedMergedReceipt = repository.save(mergedReceipt);

            log.info("合并仓单创建成功: mergedReceiptId={}, receiptNo={}, mergeCount={}",
                savedMergedReceipt.getId(), savedMergedReceipt.getReceiptNo(), sourceReceipts.size());

            // ==================== 第3步：更新源仓单状态为MERGED ====================
            for (ElectronicWarehouseReceipt receipt : sourceReceipts) {
                receipt.setReceiptStatus(ElectronicWarehouseReceipt.ReceiptStatus.MERGED);
                repository.save(receipt);
                log.info("源仓单状态更新为MERGED: receiptId={}", receipt.getId());
            }

            // ==================== 第4步：更新合并申请状态 ====================
            application.setRequestStatus("APPROVED");
            application.setMergedReceiptId(savedMergedReceipt.getId());
            application.setReviewerId(reviewerId);
            application.setReviewerName(reviewerName);
            application.setReviewTime(java.time.LocalDateTime.now());
            mergeApplicationRepository.save(application);

            log.info("合并执行完成: applicationId={}, mergedReceiptId={}",
                application.getId(), savedMergedReceipt.getId());

            // 构建响应
            return buildMergeResponse(application, sourceReceipts,
                convertToResponse(savedMergedReceipt), "success", "仓单合并成功");

        } catch (Exception e) {
            log.error("执行仓单合并失败", e);
            // 回滚源仓单状态
            for (ElectronicWarehouseReceipt receipt : sourceReceipts) {
                if (receipt.getReceiptStatus() == ElectronicWarehouseReceipt.ReceiptStatus.MERGING) {
                    receipt.setReceiptStatus(ElectronicWarehouseReceipt.ReceiptStatus.NORMAL);
                    repository.save(receipt);
                }
            }
            throw new com.fisco.app.exception.BusinessException("仓单合并失败: " + e.getMessage());
        }
    }

    /**
     * 生成合并仓单编号
     */
    private String generateMergeReceiptInfo(int sourceCount) {
        // 格式: MERGE-YYYYMMDD-序号
        String dateStr = java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "MERGE-" + dateStr + "-" + randomStr;
    }

    /**
     * 生成合并仓单编号
     */
    private String generateMergeReceiptNo(int sourceCount) {
        return generateMergeReceiptInfo(sourceCount);
    }

    /**
     * 验证合并请求
     */
    private void validateMergeRequest(
            java.util.List<ElectronicWarehouseReceipt> sourceReceipts,
            com.fisco.app.entity.warehouse.ReceiptMergeApplication.MergeType mergeType) {

        log.info("验证合并请求: receiptCount={}, mergeType={}",
            sourceReceipts.size(), mergeType);

        // 数量合并：验证货物名称、单位、单价必须相同
        if (mergeType == com.fisco.app.entity.warehouse.ReceiptMergeApplication.MergeType.QUANTITY) {
            String goodsName = sourceReceipts.get(0).getGoodsName();
            String unit = sourceReceipts.get(0).getUnit();
            BigDecimal unitPrice = sourceReceipts.get(0).getUnitPrice();

            for (int i = 1; i < sourceReceipts.size(); i++) {
                ElectronicWarehouseReceipt receipt = sourceReceipts.get(i);
                if (!java.util.Objects.equals(goodsName, receipt.getGoodsName())) {
                    throw new com.fisco.app.exception.BusinessException(
                        "数量合并要求所有仓单的货物名称必须相同");
                }
                if (!java.util.Objects.equals(unit, receipt.getUnit())) {
                    throw new com.fisco.app.exception.BusinessException(
                        "数量合并要求所有仓单的单位必须相同");
                }
                if (unitPrice != null && receipt.getUnitPrice() != null &&
                    unitPrice.compareTo(receipt.getUnitPrice()) != 0) {
                    throw new com.fisco.app.exception.BusinessException(
                        "数量合并要求所有仓单的单价必须相同");
                }
            }
        }

        log.info("合并请求验证通过");
    }

    /**
     * 构建合并响应
     */
    @SuppressWarnings("null")
    private com.fisco.app.dto.warehouse.ReceiptMergeResponse buildMergeResponse(
            com.fisco.app.entity.warehouse.ReceiptMergeApplication application,
            java.util.List<ElectronicWarehouseReceipt> sourceReceipts,
            com.fisco.app.dto.warehouse.ElectronicWarehouseReceiptResponse mergedReceipt,
            String result,
            String message) {

        com.fisco.app.dto.warehouse.ReceiptMergeResponse response =
            new com.fisco.app.dto.warehouse.ReceiptMergeResponse();

        response.setApplicationId(application.getId());
        response.setRequestStatus(application.getRequestStatus());
        response.setMergeType(com.fisco.app.entity.warehouse.ReceiptMergeApplication.MergeType.valueOf(
            application.getMergeType()));
        response.setTotalQuantity(application.getTotalQuantity());
        response.setTotalValue(application.getTotalValue());
        response.setMergeCount(sourceReceipts.size());

        response.setSourceReceipts(sourceReceipts.stream()
            .map(this::convertToResponse)
            .collect(java.util.stream.Collectors.toList()));

        response.setMergedReceipt(mergedReceipt);
        response.setApplicantId(application.getApplicantId());
        response.setApplicationTime(application.getCreatedAt());
        response.setReviewerId(application.getReviewerId());
        response.setReviewTime(application.getReviewTime());
        response.setRejectionReason(application.getReviewComments());
        response.setResult(result);
        response.setMessage(message);

        return response;
    }

    /**
     * 查询待审核的合并申请
     */
    @SuppressWarnings("null")
    public java.util.List<com.fisco.app.entity.warehouse.ReceiptMergeApplication> getPendingMergeApplications() {
        log.info("查询待审核的仓单合并申请");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (!(auth instanceof UserAuthentication)) {
            throw new com.fisco.app.exception.BusinessException("无效的认证信息");
        }

        UserAuthentication userAuth = (UserAuthentication) auth;
        java.util.List<com.fisco.app.entity.warehouse.ReceiptMergeApplication> applications;

        // 系统管理员可以查看所有待审核申请
        if (userAuth.isSystemAdmin()) {
            applications = mergeApplicationRepository.findByRequestStatus("PENDING");
            log.info("管理员查询所有待审核的合并申请");
        }
        // 仓储企业只能查询自己仓单的待审核申请
        else if (userAuth.getEnterpriseId() != null) {
            // 查询所有PENDING状态的申请，然后过滤出属于该仓储企业的
            applications = mergeApplicationRepository.findByRequestStatus("PENDING")
                .stream()
                .filter(app -> {
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        java.util.List<String> sourceIds = mapper.readValue(app.getSourceReceiptIds(),
                            new com.fasterxml.jackson.core.type.TypeReference<java.util.List<String>>() {});
                        return sourceIds.stream().anyMatch(id -> {
                            return repository.findById(id).map(receipt ->
                                userAuth.getEnterpriseId().equals(receipt.getWarehouseId())).orElse(false);
                        });
                    } catch (Exception e) {
                        log.error("解析源仓单ID列表失败", e);
                        return false;
                    }
                })
                .collect(java.util.stream.Collectors.toList());
            log.info("仓储企业查询待审核的合并申请: warehouseId={}", userAuth.getEnterpriseId());
        } else {
            throw new com.fisco.app.exception.BusinessException("无权限查询待审核的合并申请");
        }

        log.info("找到 {} 个待审核的合并申请", applications.size());

        return applications;
    }

    /**
     * 将 ElectronicWarehouseReceipt 实体转换为 Response DTO
     *
     * @param receipt 仓单实体
     * @return 响应DTO
     */
    private ElectronicWarehouseReceiptResponse convertToResponse(ElectronicWarehouseReceipt receipt) {
        if (receipt == null) {
            return null;
        }

        ElectronicWarehouseReceiptResponse response = new ElectronicWarehouseReceiptResponse();

        // 基础信息
        response.setId(receipt.getId());
        response.setReceiptNo(receipt.getReceiptNo());
        response.setWarehouseId(receipt.getWarehouseId());
        response.setWarehouseAddress(receipt.getWarehouseAddress());
        response.setWarehouseName(receipt.getWarehouseName());
        response.setOwnerId(receipt.getOwnerId());
        response.setOwnerAddress(receipt.getOwnerAddress());
        response.setOwnerName(receipt.getOwnerName());
        response.setHolderAddress(receipt.getHolderAddress());

        // 货物信息
        response.setGoodsName(receipt.getGoodsName());
        response.setUnit(receipt.getUnit());
        response.setQuantity(receipt.getQuantity());
        response.setUnitPrice(receipt.getUnitPrice());
        response.setTotalValue(receipt.getTotalValue());
        response.setMarketPrice(receipt.getMarketPrice());

        // 仓储信息
        response.setWarehouseLocation(receipt.getWarehouseLocation());
        response.setStorageLocation(receipt.getStorageLocation());
        response.setStorageDate(receipt.getStorageDate());
        response.setExpiryDate(receipt.getExpiryDate());
        response.setActualDeliveryDate(receipt.getActualDeliveryDate());
        response.setDeliveryPersonName(receipt.getDeliveryPersonName());
        response.setDeliveryPersonContact(receipt.getDeliveryPersonContact());
        response.setDeliveryNo(receipt.getDeliveryNo());
        response.setVehiclePlate(receipt.getVehiclePlate());
        response.setDriverName(receipt.getDriverName());

        // 状态管理
        response.setReceiptStatus(receipt.getReceiptStatus() != null ? receipt.getReceiptStatus().name() : null);
        response.setParentReceiptId(receipt.getParentReceiptId());
        response.setBatchNo(receipt.getBatchNo());

        // 企业和操作人
        response.setOwnerOperatorId(receipt.getOwnerOperatorId());
        response.setOwnerOperatorName(receipt.getOwnerOperatorName());
        response.setWarehouseOperatorId(receipt.getWarehouseOperatorId());
        response.setWarehouseOperatorName(receipt.getWarehouseOperatorName());

        // 融资信息
        response.setIsFinanced(receipt.getIsFinanced());
        response.setFinanceAmount(receipt.getFinanceAmount());
        response.setFinanceRate(receipt.getFinanceRate());
        response.setFinanceDate(receipt.getFinanceDate());
        response.setFinancierAddress(receipt.getFinancierAddress());
        response.setPledgeContractNo(receipt.getPledgeContractNo());

        // 背书统计
        response.setEndorsementCount(receipt.getEndorsementCount());
        response.setLastEndorsementDate(receipt.getLastEndorsementDate());
        response.setCurrentHolder(receipt.getCurrentHolder());

        // 区块链
        response.setTxHash(receipt.getTxHash());
        response.setBlockchainStatus(receipt.getBlockchainStatus() != null ? receipt.getBlockchainStatus().name() : null);
        response.setBlockNumber(receipt.getBlockNumber());
        response.setBlockchainTimestamp(receipt.getBlockchainTimestamp());

        // 其他
        response.setRemarks(receipt.getRemarks());

        // 拆分相关
        response.setSplitTime(receipt.getSplitTime());
        response.setSplitCount(receipt.getSplitCount());

        // 作废相关
        response.setCancelReason(receipt.getCancelReason());
        response.setCancelType(receipt.getCancelType());
        response.setCancelTime(receipt.getCancelTime());
        response.setCancelledBy(receipt.getCancelledBy());
        response.setReferenceNo(receipt.getReferenceNo());

        // 审计
        response.setCreatedAt(receipt.getCreatedAt());
        response.setUpdatedAt(receipt.getUpdatedAt());
        response.setCreatedBy(receipt.getCreatedBy());
        response.setUpdatedBy(receipt.getUpdatedBy());
        response.setDeletedAt(receipt.getDeletedAt());
        response.setDeletedBy(receipt.getDeletedBy());

        return response;
    }
}
