package com.fisco.app.service.warehouse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fisco.app.dto.endorsement.EwrEndorsementChainResponse;
import com.fisco.app.dto.endorsement.EwrEndorsementConfirmRequest;
import com.fisco.app.dto.endorsement.EwrEndorsementCreateRequest;
import com.fisco.app.entity.warehouse.ElectronicWarehouseReceipt;
import com.fisco.app.entity.warehouse.EwrEndorsementChain;
import com.fisco.app.repository.warehouse.ElectronicWarehouseReceiptRepository;
import com.fisco.app.repository.warehouse.EwrEndorsementChainRepository;
import com.fisco.app.service.blockchain.ContractService;

import lombok.extern.slf4j.Slf4j;

/**
 * 背书链Service
 */
@Slf4j
@Service
public class EwrEndorsementChainService {

    @Autowired
    private EwrEndorsementChainRepository endorsementRepository;

    @Autowired
    private ElectronicWarehouseReceiptRepository receiptRepository;

    @Autowired
    private ContractService contractService;

    /**
     * 创建背书请求
     */
    @Transactional
    public EwrEndorsementChainResponse createEndorsement(EwrEndorsementCreateRequest request, String operatorFromId, String operatorFromName) {
        log.info("创建背书请求, 仓单ID: {}, 被背书方: {}", request.getReceiptId(), request.getEndorseTo());

        // 1. 查询仓单
        ElectronicWarehouseReceipt receipt = receiptRepository.findById(request.getReceiptId())
                .orElseThrow(() -> new RuntimeException("仓单不存在: " + request.getReceiptId()));

        // 2. 验证仓单状态
        if (receipt.getReceiptStatus() != ElectronicWarehouseReceipt.ReceiptStatus.NORMAL) {
            throw new RuntimeException("只有正常状态的仓单可以背书, 当前状态: " + String.valueOf(receipt.getReceiptStatus()));
        }

        // 3. 验证持单人（背书方必须是当前持单人）
        if (!receipt.getHolderAddress().equalsIgnoreCase(request.getEndorseFrom())) {
            throw new RuntimeException("只有当前持单人可以发起背书");
        }

        // 4. 检查是否有待确认的背书
        if (endorsementRepository.existsPendingEndorsement(request.getReceiptId())) {
            throw new RuntimeException("该仓单存在待确认的背书,请先处理");
        }

        // 5. 生成背书编号
        String endorsementNo = generateEndorsementNo();

        // 6. 创建背书记录
        EwrEndorsementChain endorsement = new EwrEndorsementChain();
        endorsement.setId(Objects.requireNonNull(UUID.randomUUID().toString()));
        endorsement.setReceiptId(request.getReceiptId());
        endorsement.setReceiptNo(receipt.getReceiptNo());
        endorsement.setEndorsementNo(endorsementNo);
        endorsement.setEndorseFrom(receipt.getHolderAddress());
        endorsement.setEndorseFromName(receipt.getCurrentHolder());
        endorsement.setEndorseTo(request.getEndorseTo());
        endorsement.setEndorseToName(request.getEndorseToName());
        endorsement.setEndorsementType(EwrEndorsementChain.EndorsementType.valueOf(request.getEndorsementType()));
        endorsement.setEndorsementReason(request.getEndorsementReason());
        endorsement.setTransferPrice(request.getTransferPrice());
        endorsement.setTransferAmount(request.getTransferAmount());
        endorsement.setOperatorFromId(operatorFromId);
        endorsement.setOperatorFromName(operatorFromName);
        endorsement.setOperatorToId(request.getOperatorToId());
        endorsement.setOperatorToName(request.getOperatorToName());
        endorsement.setEndorsementStatus(EwrEndorsementChain.EndorsementStatus.PENDING);
        endorsement.setEndorsementTime(LocalDateTime.now());
        endorsement.setRemarks(request.getRemarks());

        // 7. 保存货物信息快照
        endorsement.setGoodsSnapshot(buildGoodsSnapshot(receipt));

        EwrEndorsementChain saved = endorsementRepository.save(endorsement);
        log.info("背书请求创建成功, ID: {}", saved.getId());

        return EwrEndorsementChainResponse.fromEntity(saved);
    }

    /**
     * 确认背书
     */
    @Transactional
    public EwrEndorsementChainResponse confirmEndorsement(EwrEndorsementConfirmRequest request, String confirmerId, String confirmerName) {
        log.info("确认背书, ID: {}, 状态: {}", request.getId(), request.getConfirmStatus());

        // 1. 查询背书记录
        EwrEndorsementChain endorsement = endorsementRepository.findById(request.getId())
                .orElseThrow(() -> new RuntimeException("背书记录不存在: " + request.getId()));

        // 2. 验证背书状态
        if (endorsement.getEndorsementStatus() != EwrEndorsementChain.EndorsementStatus.PENDING) {
            throw new RuntimeException("只有待确认状态的背书可以确认, 当前状态: " + String.valueOf(endorsement.getEndorsementStatus()));
        }

        // 3. 更新背书状态
        if ("CONFIRMED".equals(request.getConfirmStatus())) {
            endorsement.setEndorsementStatus(EwrEndorsementChain.EndorsementStatus.CONFIRMED);
            endorsement.setConfirmedTime(LocalDateTime.now());
            endorsement.setRemarks(endorsement.getRemarks() != null
                    ? endorsement.getRemarks() + "\n" + request.getRemarks()
                    : request.getRemarks());

            // 4. 更新仓单的持单人
            ElectronicWarehouseReceipt receipt = receiptRepository.findById(endorsement.getReceiptId())
                    .orElseThrow(() -> new RuntimeException("仓单不存在: " + endorsement.getReceiptId()));

            receipt.setHolderAddress(endorsement.getEndorseTo());
            receipt.setCurrentHolder(endorsement.getEndorseToName());

            // 增加背书次数
            if (receipt.getEndorsementCount() == null) {
                receipt.setEndorsementCount(0);
            }
            receipt.setEndorsementCount(receipt.getEndorsementCount() + 1);
            receipt.setLastEndorsementDate(LocalDateTime.now());

            // 5. 更新仓单状态为已转让
            receipt.setReceiptStatus(ElectronicWarehouseReceipt.ReceiptStatus.TRANSFERRED);

            receiptRepository.save(receipt);

            log.info("背书确认成功, 仓单持单人已更新");

            // 6. 自动上链
            try {
                log.info("开始上链背书信息, 仓单ID: {}, 新持单人: {}",
                         endorsement.getReceiptId(), endorsement.getEndorseTo());

                String txHash = contractService.transferReceiptOnChain(
                    endorsement.getReceiptId(),
                    endorsement.getEndorseTo(),
                    endorsement.getTransferPrice()
                );

                Long blockNumber = contractService.getBlockNumber(txHash);

                // 更新背书记录的区块链信息
                endorsement.setTxHash(txHash);
                endorsement.setBlockNumber(blockNumber);
                endorsement.setBlockchainTimestamp(LocalDateTime.now());

                log.info("背书上链成功: txHash={}, blockNumber={}", txHash, blockNumber);

            } catch (Exception e) {
                log.error("背书上链失败，但数据库已更新: {}", e.getMessage(), e);
                // 可选：标记上链失败状态，后续可通过接口重试
                endorsement.setRemarks(endorsement.getRemarks() != null
                        ? endorsement.getRemarks() + "\n上链失败: " + e.getMessage()
                        : "上链失败: " + e.getMessage());
            }

        } else if ("CANCELLED".equals(request.getConfirmStatus())) {
            endorsement.setEndorsementStatus(EwrEndorsementChain.EndorsementStatus.CANCELLED);
            endorsement.setRemarks(endorsement.getRemarks() != null
                    ? endorsement.getRemarks() + "\n" + request.getRemarks()
                    : request.getRemarks());

            log.info("背书已取消");
        } else {
            throw new RuntimeException("无效的确认状态: " + request.getConfirmStatus());
        }

        EwrEndorsementChain saved = endorsementRepository.save(endorsement);
        return EwrEndorsementChainResponse.fromEntity(saved);
    }

    /**
     * 查询仓单的背书链
     */
    public List<EwrEndorsementChainResponse> getEndorsementChain(String receiptId) {
        log.info("查询背书链, 仓单ID: {}", receiptId);

        List<EwrEndorsementChain> chain = endorsementRepository.findConfirmedEndorsementChainByReceiptId(receiptId);
        return chain.stream()
                .map(EwrEndorsementChainResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 查询待确认的背书
     */
    public List<EwrEndorsementChainResponse> getPendingEndorsements(String endorseTo) {
        log.info("查询待确认背书, 被背书方: {}", endorseTo);

        List<EwrEndorsementChain> endorsements = endorsementRepository.findPendingEndorsementsByEndorseTo(endorseTo);
        return endorsements.stream()
                .map(EwrEndorsementChainResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 根据ID查询背书
     */
    public EwrEndorsementChainResponse getEndorsementById(@NonNull String id) {
        EwrEndorsementChain endorsement = endorsementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("背书记录不存在: " + id));
        return EwrEndorsementChainResponse.fromEntity(endorsement);
    }

    /**
     * 根据背书编号查询
     */
    public EwrEndorsementChainResponse getEndorsementByNo(String endorsementNo) {
        EwrEndorsementChain endorsement = endorsementRepository.findByEndorsementNo(endorsementNo)
                .orElseThrow(() -> new RuntimeException("背书记录不存在: " + endorsementNo));
        return EwrEndorsementChainResponse.fromEntity(endorsement);
    }

    /**
     * 查询企业发起的背书（作为转出方）
     */
    public List<EwrEndorsementChainResponse> getEndorsementsByEndorseFrom(String endorseFrom) {
        List<EwrEndorsementChain> endorsements = endorsementRepository.findConfirmedEndorsementsByEndorseFrom(endorseFrom);
        return endorsements.stream()
                .map(EwrEndorsementChainResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 查询企业接收的背书（作为转入方）
     */
    public List<EwrEndorsementChainResponse> getEndorsementsByEndorseTo(String endorseTo) {
        List<EwrEndorsementChain> endorsements = endorsementRepository.findConfirmedEndorsementsByEndorseTo(endorseTo);
        return endorsements.stream()
                .map(EwrEndorsementChainResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 查询经手人的背书记录
     */
    public List<EwrEndorsementChainResponse> getEndorsementsByOperator(String operatorId) {
        List<EwrEndorsementChain> endorsements = endorsementRepository.findByOperatorId(operatorId);
        return endorsements.stream()
                .map(EwrEndorsementChainResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 统计仓单的背书次数
     */
    public Long countEndorsements(String receiptId) {
        return endorsementRepository.countConfirmedEndorsementsByReceiptId(receiptId);
    }

    /**
     * 更新区块链上链信息
     */
    @Transactional
    public void updateBlockchainInfo(@NonNull String endorsementId, String txHash, Long blockNumber) {
        log.info("更新背书上链信息, ID: {}, txHash: {}", endorsementId, txHash);

        EwrEndorsementChain endorsement = endorsementRepository.findById(endorsementId)
                .orElseThrow(() -> new RuntimeException("背书记录不存在: " + endorsementId));

        endorsement.setTxHash(txHash);
        endorsement.setBlockNumber(blockNumber);
        endorsement.setBlockchainTimestamp(LocalDateTime.now());

        endorsementRepository.save(endorsement);
    }

    /**
     * 生成背书编号
     * 格式: END+yyyyMMdd+6位流水号
     */
    private String generateEndorsementNo() {
        String dateStr = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 简化实现: 实际项目中应该使用数据库序列或Redis分布式锁
        String sequence = String.format("%06d", System.currentTimeMillis() % 1000000);
        return "END" + dateStr + sequence;
    }

    /**
     * 构建货物信息快照（JSON格式）
     */
    private String buildGoodsSnapshot(ElectronicWarehouseReceipt receipt) {
        StringBuilder snapshot = new StringBuilder();
        snapshot.append("{");
        snapshot.append("\"goods_name\":\"").append(receipt.getGoodsName()).append("\",");
        snapshot.append("\"quantity\":").append(receipt.getQuantity()).append(",");
        snapshot.append("\"unit_price\":").append(receipt.getUnitPrice()).append(",");
        snapshot.append("\"total_value\":").append(receipt.getTotalValue()).append(",");
        snapshot.append("\"storage_date\":\"").append(receipt.getStorageDate()).append("\",");
        snapshot.append("\"warehouse_location\":\"").append(receipt.getWarehouseLocation()).append("\"");
        snapshot.append("}");
        return snapshot.toString();
    }
}
