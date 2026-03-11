package com.fisco.app.Modules.Blockchain.Service;

import com.fisco.app.Modules.Blockchain.Entity.BlockchainTransactionRecord;

/**
 * 链上行为审计服务接口
 * 用于关联 txHash 与 JWT 登录记录，实现链上行为审计回溯
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
public interface BlockchainAuditService {

    /**
     * 记录交易映射
     * 在发起区块链交易后调用，记录交易与JWT的关联关系
     *
     * @param txHash       区块链交易哈希
     * @param operation    操作类型（如：CONTRACT_DEPLOY, CONTRACT_INVOKE）
     * @param contractName 合约名称
     */
    void recordTransaction(String txHash, String operation, String contractName);

    /**
     * 根据txHash查询JWT标识
     *
     * @param txHash 区块链交易哈希
     * @return JWT唯一标识(jti)，不存在返回null
     */
    String getJtiByTxHash(String txHash);

    /**
     * 根据txHash查询完整交易记录
     *
     * @param txHash 区块链交易哈希
     * @return 交易记录，不存在返回null
     */
    BlockchainTransactionRecord getRecordByTxHash(String txHash);

    /**
     * 根据userId查询其所有链上交易记录
     *
     * @param userId 用户ID
     * @return 交易记录列表
     */
    java.util.List<BlockchainTransactionRecord> getRecordsByUserId(Long userId);

    /**
     * 根据entId查询企业所有链上交易记录
     *
     * @param entId 企业ID
     * @return 交易记录列表
     */
    java.util.List<BlockchainTransactionRecord> getRecordsByEntId(Long entId);
}
