package com.fisco.app.service.warehouse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fisco.app.dto.warehouse.CreateWarehouseReceiptRequest;
import com.fisco.app.dto.warehouse.ReleaseReceiptRequest;
import com.fisco.app.dto.warehouse.ReleaseReceiptResponse;
import com.fisco.app.entity.pledge.ReleaseRecord;
import com.fisco.app.entity.warehouse.WarehouseReceipt;
import com.fisco.app.exception.BlockchainIntegrationException;
import com.fisco.app.repository.pledge.ReleaseRecordRepository;
import com.fisco.app.repository.warehouse.WarehouseReceiptRepository;
import com.fisco.app.service.blockchain.ContractService;
import com.fisco.app.service.enterprise.EnterpriseService;

import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 仓单Service
 */
@Slf4j
@ConditionalOnProperty(name = "fisco.enabled", havingValue = "true", matchIfMissing = true)
@Service
@Api(tags = "仓单服务")
@RequiredArgsConstructor
public class WarehouseReceiptService {

    private final WarehouseReceiptRepository warehouseReceiptRepository;
    private final EnterpriseService enterpriseService;
    private final ContractService contractService;
    private final ReleaseRecordRepository releaseRecordRepository;

    /**
     * 创建仓单
     */
    @Transactional(rollbackFor = Exception.class)
    public WarehouseReceipt createReceipt(CreateWarehouseReceiptRequest request, String ownerAddress) {
        log.info("创建仓单: id={}, goods={}", request.getId(), request.getGoods().getGoodsName());

        // 检查仓单ID是否已存在
        if (warehouseReceiptRepository.findById(request.getId()).isPresent()) {
            throw new com.fisco.app.exception.BusinessException("仓单ID已存在");
        }

        // 验证货主是否存在
        if (!enterpriseService.isEnterpriseValid(ownerAddress)) {
            throw new com.fisco.app.exception.BusinessException("货主不存在或未激活");
        }

        // 验证仓库是否存在
        if (!enterpriseService.isEnterpriseValid(request.getWarehouseAddress())) {
            throw new com.fisco.app.exception.BusinessException("仓库不存在或未激活");
        }

        // 验证日期
        if (request.getExpiryDate().isBefore(request.getStorageDate())) {
            throw new com.fisco.app.exception.BusinessException("过期日期必须晚于入库日期");
        }

        // 从请求构建 WarehouseReceipt 实体
        WarehouseReceipt receipt = new WarehouseReceipt();
        receipt.setId(request.getId());
        receipt.setOwnerAddress(ownerAddress);
        receipt.setWarehouseAddress(request.getWarehouseAddress());

        // 设置货物信息
        receipt.setGoodsName(request.getGoods().getGoodsName());
        receipt.setGoodsType(request.getGoods().getGoodsType());
        receipt.setQuantity(request.getGoods().getQuantity());
        receipt.setUnit(request.getGoods().getUnit());
        receipt.setUnitPrice(request.getGoods().getUnitPrice());
        receipt.setTotalPrice(request.getGoods().getTotalPrice());
        receipt.setQuality(request.getGoods().getQuality());
        receipt.setOrigin(request.getGoods().getOrigin());

        receipt.setWarehouseLocation(request.getWarehouseLocation());
        receipt.setStorageDate(request.getStorageDate());
        receipt.setExpiryDate(request.getExpiryDate());
        receipt.setStatus(WarehouseReceipt.ReceiptStatus.CREATED);
        receipt.setPledgeAmount(java.math.BigDecimal.ZERO);
        receipt.setFinanceAmount(java.math.BigDecimal.ZERO);
        receipt.setFinanceRate(0);

        // 步骤1: 保存到数据库
        WarehouseReceipt saved = warehouseReceiptRepository.save(receipt);
        log.info("仓单保存到数据库成功: id={}", saved.getId());

        // 步骤2: 调用区块链合约
        try {
            String txHash = contractService.createReceiptOnChain(saved);

            // 步骤3: 更新 txHash
            saved.setTxHash(txHash);
            WarehouseReceipt finalSaved = warehouseReceiptRepository.save(saved);

            log.info("仓单创建成功: id={}, txHash={}", finalSaved.getId(), txHash);
            return finalSaved;

        } catch (BlockchainIntegrationException e) {
            log.error("区块链调用失败，回滚数据库事务: receiptId={}, error={}",
                saved.getId(), e.getMessage(), e);
            // 抛出业务异常，触发 @Transactional 回滚
            throw new com.fisco.app.exception.BusinessException(
                500, "区块链操作失败: " + e.getMessage(), e);
        }
    }

    /**
     * 验证仓单
     */
    @Transactional
    public void verifyReceipt(@NonNull String receiptId) {
        log.info("验证仓单: id={}", receiptId);

        WarehouseReceipt receipt = warehouseReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException("仓单不存在: " + receiptId));

        if (receipt.getStatus() != WarehouseReceipt.ReceiptStatus.CREATED) {
            throw new com.fisco.app.exception.BusinessException("只能验证已创建的仓单");
        }

        // 步骤1: 调用区块链合约
        try {
            String txHash = contractService.verifyReceiptOnChain(receiptId);

            // 步骤2: 更新数据库状态和交易哈希
            receipt.setStatus(WarehouseReceipt.ReceiptStatus.VERIFIED);
            receipt.setTxHash(txHash);
            warehouseReceiptRepository.save(receipt);

            log.info("仓单验证成功: id={}, txHash={}", receiptId, txHash);

        } catch (BlockchainIntegrationException e) {
            log.error("区块链调用失败，回滚数据库事务: receiptId={}, error={}",
                receiptId, e.getMessage(), e);
            // 抛出业务异常，触发 @Transactional 回滚
            throw new com.fisco.app.exception.BusinessException(
                500, "区块链操作失败: " + e.getMessage(), e);
        }
    }

    /**
     * 质押仓单
     */
    @Transactional
    public void pledgeReceipt(@NonNull String receiptId, String financialInstitutionAddress,
                               java.math.BigDecimal pledgeAmount) {
        log.info("质押仓单: id={}, institution={}, amount={}",
                 receiptId, financialInstitutionAddress, pledgeAmount);

        WarehouseReceipt receipt = warehouseReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException("仓单不存在: " + receiptId));

        if (receipt.getStatus() != WarehouseReceipt.ReceiptStatus.VERIFIED) {
            throw new com.fisco.app.exception.BusinessException("只能质押已验证的仓单");
        }

        if (pledgeAmount.compareTo(receipt.getTotalPrice()) > 0) {
            throw new com.fisco.app.exception.BusinessException("质押金额不能超过仓单总价值");
        }

        // 验证金融机构是否存在
        if (!enterpriseService.isEnterpriseValid(financialInstitutionAddress)) {
            throw new com.fisco.app.exception.BusinessException("金融机构不存在或未激活");
        }

        // 步骤1: 调用区块链合约
        try {
            String txHash = contractService.pledgeReceiptOnChain(
                receiptId,
                financialInstitutionAddress,
                pledgeAmount
            );

            // 步骤2: 更新数据库状态和交易哈希
            receipt.setStatus(WarehouseReceipt.ReceiptStatus.PLEDGED);
            receipt.setPledgeAmount(pledgeAmount);
            receipt.setFinanceAmount(pledgeAmount);
            receipt.setTxHash(txHash);
            warehouseReceiptRepository.save(receipt);

            log.info("仓单质押成功: id={}, txHash={}", receiptId, txHash);

        } catch (BlockchainIntegrationException e) {
            log.error("区块链调用失败，回滚数据库事务: receiptId={}, error={}",
                receiptId, e.getMessage(), e);
            // 抛出业务异常，触发 @Transactional 回滚
            throw new com.fisco.app.exception.BusinessException(
                500, "区块链操作失败: " + e.getMessage(), e);
        }
    }

    /**
     * 释放仓单
     */
    @Transactional
    public ReleaseReceiptResponse releaseReceipt(@NonNull String receiptId, ReleaseReceiptRequest request) {
        log.info("释放仓单: receiptId={}, releaseType={}", receiptId, request.getReleaseType());

        // 步骤1: 验证仓单
        WarehouseReceipt receipt = warehouseReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException("仓单不存在: " + receiptId));

        // 验证仓单状态 - 只有已质押或已融资的仓单可以释放
        if (receipt.getStatus() != WarehouseReceipt.ReceiptStatus.PLEDGED &&
            receipt.getStatus() != WarehouseReceipt.ReceiptStatus.FINANCED) {
            throw new com.fisco.app.exception.BusinessException.InvalidStatusException(
                "只能释放已质押或已融资的仓单");
        }

        // 步骤2: 调用区块链合约
        try {
            String txHash = contractService.releaseReceiptOnChain(receiptId);

            // 步骤3: 创建释放记录
            ReleaseRecord releaseRecord = new ReleaseRecord();
            releaseRecord.setReceiptId(receiptId);
            releaseRecord.setOwnerAddress(receipt.getOwnerAddress());
            releaseRecord.setFinancialInstitutionAddress(receipt.getFinancialInstitution());
            releaseRecord.setPledgeAmount(receipt.getPledgeAmount());
            releaseRecord.setFinanceAmount(receipt.getFinanceAmount());
            releaseRecord.setFinanceRate(receipt.getFinanceRate());
            releaseRecord.setFinanceDate(receipt.getFinanceDate());
            releaseRecord.setReleaseType(request.getReleaseType());
            releaseRecord.setRepaymentAmount(request.getRepaymentAmount());
            releaseRecord.setInterestAmount(request.getInterestAmount());
            releaseRecord.setRemark(request.getRemark());
            releaseRecord.setTxHash(txHash);

            ReleaseRecord savedRecord = releaseRecordRepository.save(releaseRecord);

            // 步骤4: 更新仓单状态
            receipt.setStatus(WarehouseReceipt.ReceiptStatus.RELEASED);
            receipt.setReleaseDate(java.time.LocalDateTime.now());
            receipt.setTxHash(txHash);
            warehouseReceiptRepository.save(receipt);

            // 步骤5: 构建响应
            ReleaseReceiptResponse response = new ReleaseReceiptResponse();
            response.setId(savedRecord.getId());
            response.setId(savedRecord.getId());
            response.setOwnerAddress(savedRecord.getOwnerAddress());
            response.setFinancialInstitutionAddress(savedRecord.getFinancialInstitutionAddress());
            response.setPledgeAmount(savedRecord.getPledgeAmount());
            response.setFinanceAmount(savedRecord.getFinanceAmount());
            response.setFinanceRate(savedRecord.getFinanceRate());
            response.setFinanceDate(savedRecord.getFinanceDate());
            response.setReleaseDate(savedRecord.getReleaseDate());
            response.setReleaseType(savedRecord.getReleaseType());
            response.setRepaymentAmount(savedRecord.getRepaymentAmount());
            response.setInterestAmount(savedRecord.getInterestAmount());
            response.setTxHash(savedRecord.getTxHash());
            response.setRemark(savedRecord.getRemark());

            log.info("仓单释放成功: receiptId={}, releaseType={}, txHash={}",
                receiptId, request.getReleaseType(), txHash);

            return response;

        } catch (BlockchainIntegrationException e) {
            log.error("区块链调用失败，回滚数据库事务: receiptId={}, error={}",
                receiptId, e.getMessage(), e);
            // 抛出业务异常，触发 @Transactional 回滚
            throw new com.fisco.app.exception.BusinessException(
                500, "区块链操作失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取仓单的释放记录
     */
    public java.util.List<ReleaseRecord> getReleaseHistory(@NonNull String receiptId) {
        log.info("查询仓单释放历史: receiptId={}", receiptId);

        // 验证仓单是否存在
        if (!warehouseReceiptRepository.findById(receiptId).isPresent()) {
            throw new com.fisco.app.exception.BusinessException("仓单不存在: " + receiptId);
        }

        return releaseRecordRepository.findByReceiptIdOrderByReleaseDateDesc(receiptId);
    }

    /**
     * 获取仓单信息
     */
    public WarehouseReceipt getReceipt(@NonNull String receiptId) {
        return warehouseReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException("仓单不存在: " + receiptId));
    }
}
