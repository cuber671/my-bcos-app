package com.fisco.app.service.blockchain;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.client.protocol.model.JsonTransactionResponse;
import org.fisco.bcos.sdk.v3.client.protocol.response.BcosTransaction;
import org.fisco.bcos.sdk.v3.client.protocol.response.BcosTransactionReceipt;
import org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fisco.app.dto.blockchain.CancelTransactionRequestDTO;
import com.fisco.app.dto.blockchain.EstimateGasRequestDTO;
import com.fisco.app.dto.blockchain.TransactionDTO;
import com.fisco.app.dto.blockchain.TransactionDetailDTO;
import com.fisco.app.dto.blockchain.TransactionReceiptDTO;
import com.fisco.app.entity.blockchain.Transaction;
import com.fisco.app.entity.blockchain.TransactionPool;
import com.fisco.app.entity.blockchain.TransactionReceiptEntity;
import com.fisco.app.exception.BlockchainIntegrationException;
import com.fisco.app.repository.blockchain.TransactionPoolRepository;
import com.fisco.app.repository.blockchain.TransactionReceiptRepository;
import com.fisco.app.repository.blockchain.TransactionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 交易管理服务
 * 提供交易查询、交易池管理、交易回执查询等核心功能
 *
 * 注意：在FISCO BCOS中，交易发送主要通过智能合约方式完成。
 * 本服务专注于交易查询和管理功能。如需发送交易，请使用ContractService调用相应合约方法。
 *
 * @author FISCO BCOS Team
 * @since 2026-02-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final Client client;
    private final CryptoKeyPair cryptoKeyPair;
    private final TransactionRepository transactionRepository;
    private final TransactionReceiptRepository receiptRepository;
    private final TransactionPoolRepository poolRepository;

    // ========== 常量定义 ==========

    private static final String ERROR_TX_NOT_FOUND = "交易不存在";
    private static final String ERROR_TX_ALREADY_CONFIRMED = "交易已确认，无法取消";
    private static final String ERROR_GAS_LIMIT_EXCEEDED = "Gas限制超过最大值";
    private static final Long MAX_GAS_LIMIT = 30000000L;

    // ========== 核心业务方法 ==========

    /**
     * 记录交易到数据库（由合约调用后自动记录）
     * 此方法供ContractService在调用合约后使用
     */
    @Transactional
    public void recordTransaction(String transactionHash, String contractAddress,
                                  String fromAddress, String toAddress, String inputData) {
        log.debug("Recording transaction: hash={}", transactionHash);

        // 检查是否已存在
        if (transactionRepository.findByTransactionHash(transactionHash).isPresent()) {
            log.warn("Transaction already recorded: hash={}", transactionHash);
            return;
        }

        Transaction tx = new Transaction();
        tx.setTransactionHash(transactionHash);
        tx.setToAddress(toAddress);
        tx.setFromAddress(fromAddress != null ? fromAddress : cryptoKeyPair.getAddress());
        tx.setValue("0");
        tx.setInputData(inputData);
        tx.setTransactionType(Transaction.TransactionType.CONTRACT_CALL);
        tx.setStatus(Transaction.TransactionStatus.PENDING.getCode());

        transactionRepository.save(tx);
        log.info("Transaction recorded: hash={}", transactionHash);
    }

    /**
     * 查询交易详情
     */
    public TransactionDetailDTO getTransactionByHash(String transactionHash) {
        log.debug("Getting transaction details: hash={}", transactionHash);

        try {
            // 1. 从数据库查询
            Transaction dbTransaction = transactionRepository.findByTransactionHash(transactionHash)
                .orElse(null);

            // 2. 从区块链查询最新状态
            BcosTransaction bcosTx = client.getTransaction(transactionHash, false);
            JsonTransactionResponse tx = null;
            if (bcosTx != null && bcosTx.getTransaction().isPresent()) {
                tx = bcosTx.getTransaction().get();
            }

            if (tx == null) {
                if (dbTransaction != null) {
                    // 数据库有记录但链上没有，可能是待处理
                    return convertToDetailDTO(dbTransaction, null, null);
                }
                throw new BlockchainIntegrationException(ERROR_TX_NOT_FOUND);
            }

            // 3. 获取交易回执
            BcosTransactionReceipt bcosReceipt = client.getTransactionReceipt(transactionHash, false);
            TransactionReceipt receipt = null;
            if (bcosReceipt != null && bcosReceipt.getTransactionReceipt() != null) {
                receipt = bcosReceipt.getTransactionReceipt();
            }

            // 4. 更新数据库记录
            if (dbTransaction != null) {
                updateTransactionRecord(dbTransaction, tx, receipt);
            }

            // 5. 构建响应
            return convertToDetailDTO(dbTransaction, tx, receipt);

        } catch (BlockchainIntegrationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get transaction: hash={}", transactionHash, e);
            throw new BlockchainIntegrationException("查询交易失败: " + e.getMessage(), e);
        }
    }

    /**
     * 查询交易池
     * 注意：FISCO BCOS不提供直接访问交易池的RPC接口，此方法返回本地数据库中待处理的交易
     */
    public List<TransactionDTO> getTransactionPool() {
        log.debug("Getting transaction pool (local pending transactions)");

        try {
            // 从数据库查询待处理交易
            List<TransactionPool> poolRecords = poolRepository.findByStatusOrderBySubmittedAtAsc(
                TransactionPool.PoolStatus.PENDING);

            return poolRecords.stream()
                .map(this::convertPoolToDTO)
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to get transaction pool", e);
            throw new BlockchainIntegrationException("查询交易池失败: " + e.getMessage(), e);
        }
    }

    /**
     * 查询账户待处理交易
     */
    public List<TransactionDTO> getPendingTransactionsByAddress(String address) {
        log.debug("Getting pending transactions for address: {}", address);

        try {
            List<TransactionPool> poolRecords = poolRepository.findByFromAddressOrderBySubmittedAtDesc(address);

            return poolRecords.stream()
                .filter(tp -> tp.getStatus() == TransactionPool.PoolStatus.PENDING)
                .map(this::convertPoolToDTO)
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to get pending transactions for address: {}", address, e);
            throw new BlockchainIntegrationException("查询待处理交易失败: " + e.getMessage(), e);
        }
    }

    /**
     * 取消交易（仅从本地数据库和交易池中移除）
     * 注意：此方法不会从链上取消交易，仅从本地记录中移除
     */
    @Transactional
    public void cancelTransaction(CancelTransactionRequestDTO request) {
        log.info("Cancelling transaction (local only): hash={}, reason={}",
                 request.getTransactionHash(), request.getReason());

        try {
            // 1. 查询交易状态
            Transaction dbTransaction = transactionRepository.findByTransactionHash(
                request.getTransactionHash()).orElse(null);

            if (dbTransaction == null) {
                throw new BlockchainIntegrationException(ERROR_TX_NOT_FOUND);
            }

            // 2. 检查是否可以取消
            if (dbTransaction.getStatus() == Transaction.TransactionStatus.SUCCESS.getCode() ||
                dbTransaction.getStatus() == Transaction.TransactionStatus.PACKED.getCode()) {
                throw new BlockchainIntegrationException(ERROR_TX_ALREADY_CONFIRMED);
            }

            // 3. 从交易池移除
            poolRepository.deleteByTransactionHashAndStatus(
                request.getTransactionHash(),
                TransactionPool.PoolStatus.PENDING
            );

            // 4. 更新交易状态为已取消
            dbTransaction.setStatus(Transaction.TransactionStatus.CANCELLED.getCode());
            dbTransaction.setErrorMessage(request.getReason());
            transactionRepository.save(dbTransaction);

            log.info("Transaction cancelled (local record updated): hash={}", request.getTransactionHash());

        } catch (BlockchainIntegrationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to cancel transaction: hash={}", request.getTransactionHash(), e);
            throw new BlockchainIntegrationException("取消交易失败: " + e.getMessage(), e);
        }
    }

    /**
     * 查询交易回执
     */
    public TransactionReceiptDTO getTransactionReceipt(String transactionHash) {
        log.debug("Getting transaction receipt: hash={}", transactionHash);

        try {
            // 1. 从数据库查询
            TransactionReceiptEntity dbReceipt = receiptRepository.findByTransactionHash(transactionHash)
                .orElse(null);

            // 2. 从区块链查询
            BcosTransactionReceipt bcosReceipt = client.getTransactionReceipt(transactionHash, false);

            TransactionReceipt receipt = null;
            if (bcosReceipt != null && bcosReceipt.getTransactionReceipt() != null) {
                receipt = bcosReceipt.getTransactionReceipt();
            }

            if (receipt == null) {
                if (dbReceipt != null) {
                    return convertToReceiptDTO(dbReceipt);
                }
                throw new BlockchainIntegrationException(ERROR_TX_NOT_FOUND);
            }

            // 3. 保存到数据库
            if (dbReceipt == null) {
                dbReceipt = saveReceiptRecord(receipt);
            } else {
                updateReceiptRecord(dbReceipt, receipt);
            }

            return convertToReceiptDTO(dbReceipt);

        } catch (BlockchainIntegrationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get transaction receipt: hash={}", transactionHash, e);
            throw new BlockchainIntegrationException("查询交易回执失败: " + e.getMessage(), e);
        }
    }

    /**
     * 估算Gas（简化版本）
     * 注意：此方法返回一个估算值，实际Gas消耗可能不同
     * 建议通过合约调用获取更准确的Gas估算
     */
    public Long estimateGas(EstimateGasRequestDTO request) {
        log.debug("Estimating gas for transaction: to={}", request.getToAddress());

        try {
            // 简化的Gas估算：基于交易类型返回固定值
            // 实际应用中应该通过调用合约来估算
            Long estimatedGas;

            if (request.getData() != null && request.getData().length() > 100) {
                // 复杂合约调用
                estimatedGas = 200000L;
            } else if (request.getData() != null && request.getData().length() > 10) {
                // 简单合约调用
                estimatedGas = 100000L;
            } else {
                // 简单转账
                estimatedGas = 21000L;
            }

            // 检查是否超过最大限制
            if (estimatedGas > MAX_GAS_LIMIT) {
                throw new BlockchainIntegrationException(ERROR_GAS_LIMIT_EXCEEDED +
                    ": " + estimatedGas + " > " + MAX_GAS_LIMIT);
            }

            // 增加10%缓冲
            estimatedGas = (long) (estimatedGas * 1.1);

            log.info("Estimated gas: {} (with 10% buffer, simplified estimate)", estimatedGas);
            log.warn("注意: 这是简化的Gas估算。对于准确估算，请通过ContractService调用合约方法。");

            return estimatedGas;

        } catch (BlockchainIntegrationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to estimate gas", e);
            throw new BlockchainIntegrationException("估算Gas失败: " + e.getMessage(), e);
        }
    }

    // ========== 辅助方法 ==========

    @SuppressWarnings("null")
    private void updateTransactionRecord(Transaction dbTransaction, JsonTransactionResponse tx,
                                       TransactionReceipt receipt) {
        if (receipt != null) {
            // 从交易回执中获取区块信息
            try {
                BigInteger blockNumber = receipt.getBlockNumber();
                if (blockNumber != null) {
                    dbTransaction.setBlockNumber(blockNumber.longValue());
                }
            } catch (Exception e) {
                log.warn("Failed to parse blockNumber from receipt");
            }

            try {
                String gasUsed = receipt.getGasUsed();
                if (gasUsed != null && !gasUsed.isEmpty()) {
                    dbTransaction.setGasUsed(Long.parseLong(gasUsed));
                }
            } catch (Exception e) {
                log.warn("Failed to parse gasUsed from receipt");
            }

            dbTransaction.setStatus(receipt.getStatus() == 0 ?
                Transaction.TransactionStatus.SUCCESS.getCode() :
                Transaction.TransactionStatus.FAILED.getCode());
            if (receipt.getStatus() != 0) {
                dbTransaction.setErrorMessage("Transaction failed with status: " + receipt.getStatus());
            }
        }

        transactionRepository.save(dbTransaction);
    }

    private TransactionReceiptEntity saveReceiptRecord(TransactionReceipt receipt) {
        TransactionReceiptEntity entity = new TransactionReceiptEntity();
        entity.setTransactionHash(receipt.getTransactionHash());

        try {
            BigInteger blockNumber = receipt.getBlockNumber();
            if (blockNumber != null) {
                entity.setBlockNumber(blockNumber.longValue());
            }
        } catch (Exception e) {
            log.warn("Failed to parse blockNumber");
        }

        try {
            String gasUsed = receipt.getGasUsed();
            if (gasUsed != null && !gasUsed.isEmpty()) {
                entity.setGasUsed(Long.parseLong(gasUsed));
            }
        } catch (Exception e) {
            log.warn("Failed to parse gasUsed");
        }

        // cumulativeGasUsed may not be available in FISCO BCOS SDK v3
        // entity.setCumulativeGasUsed(...);

        entity.setStatus(receipt.getStatus());

        return receiptRepository.save(entity);
    }

    private void updateReceiptRecord(TransactionReceiptEntity dbReceipt, TransactionReceipt receipt) {
        try {
            BigInteger blockNumber = receipt.getBlockNumber();
            if (blockNumber != null) {
                dbReceipt.setBlockNumber(blockNumber.longValue());
            }
        } catch (Exception e) {
            log.warn("Failed to parse blockNumber");
        }

        try {
            String gasUsed = receipt.getGasUsed();
            if (gasUsed != null && !gasUsed.isEmpty()) {
                dbReceipt.setGasUsed(Long.parseLong(gasUsed));
            }
        } catch (Exception e) {
            log.warn("Failed to parse gasUsed");
        }

        dbReceipt.setStatus(receipt.getStatus());

        receiptRepository.save(dbReceipt);
    }

    private TransactionDetailDTO convertToDetailDTO(Transaction dbTransaction,
                                                   JsonTransactionResponse tx,
                                                   TransactionReceipt receipt) {
        TransactionDetailDTO dto = new TransactionDetailDTO();

        if (dbTransaction != null) {
            dto.setTransactionHash(dbTransaction.getTransactionHash());
            dto.setFromAddress(dbTransaction.getFromAddress());
            dto.setToAddress(dbTransaction.getToAddress());
            dto.setValue(dbTransaction.getValue());
            dto.setGasPrice(dbTransaction.getGasPrice());
            dto.setGasLimit(dbTransaction.getGasLimit());
            dto.setInputData(dbTransaction.getInputData());
            dto.setCreatedAt(dbTransaction.getCreatedAt());
            if (dbTransaction.getTransactionType() != null) {
                dto.setTransactionType(dbTransaction.getTransactionType().name());
            }

            Transaction.TransactionStatus status = Transaction.TransactionStatus.fromCode(dbTransaction.getStatus());
            dto.setStatus(status.getDescription());
        }

        if (tx != null) {
            dto.setTransactionHash(tx.getHash());
            dto.setFromAddress(tx.getFrom());
            dto.setToAddress(tx.getTo());
            dto.setInputData(tx.getInput());

            // 解析方法ID
            if (tx.getInput() != null && tx.getInput().length() >= 10) {
                dto.setMethodId(tx.getInput().substring(0, 10));
            }
        }

        if (receipt != null) {
            TransactionReceiptDTO receiptDTO = new TransactionReceiptDTO();
            receiptDTO.setTransactionHash(receipt.getTransactionHash());

            try {
                BigInteger blockNumber = receipt.getBlockNumber();
                if (blockNumber != null) {
                    receiptDTO.setBlockNumber(blockNumber.longValue());
                }
            } catch (Exception e) {
                log.warn("Failed to parse blockNumber");
            }

            try {
                String gasUsed = receipt.getGasUsed();
                if (gasUsed != null && !gasUsed.isEmpty()) {
                    receiptDTO.setGasUsed(Long.parseLong(gasUsed));
                }
            } catch (Exception e) {
                log.warn("Failed to parse gasUsed");
            }

            // cumulativeGasUsed may not be available in FISCO BCOS SDK v3
            // receiptDTO.setCumulativeGasUsed(...);

            receiptDTO.setStatus(receipt.getStatus());
            dto.setReceipt(receiptDTO);
        }

        return dto;
    }

    private TransactionReceiptDTO convertToReceiptDTO(TransactionReceiptEntity entity) {
        TransactionReceiptDTO dto = new TransactionReceiptDTO();
        dto.setTransactionHash(entity.getTransactionHash());
        dto.setBlockNumber(entity.getBlockNumber());
        dto.setBlockHash(entity.getBlockHash());
        dto.setTransactionIndex(entity.getTransactionIndex());
        dto.setGasUsed(entity.getGasUsed());
        dto.setCumulativeGasUsed(entity.getCumulativeGasUsed());
        dto.setStatus(entity.getStatus());
        dto.setRevertReason(entity.getRevertReason());
        dto.setReceiptObtainedAt(entity.getReceiptObtainedAt());
        return dto;
    }

    private TransactionDTO convertPoolToDTO(TransactionPool pool) {
        TransactionDTO dto = new TransactionDTO();
        dto.setTransactionHash(pool.getTransactionHash());
        dto.setFrom(pool.getFromAddress());
        dto.setTo(pool.getToAddress());
        dto.setGasPrice(BigInteger.valueOf(pool.getGasPrice()));
        dto.setGasLimit(BigInteger.valueOf(pool.getGasLimit()));
        return dto;
    }
}
