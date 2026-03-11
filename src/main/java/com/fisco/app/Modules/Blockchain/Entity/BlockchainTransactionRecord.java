package com.fisco.app.Modules.Blockchain.Entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 链上交易审计记录实体
 * 用于关联 txHash 与 JWT 登录记录，实现链上行为审计回溯
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Data
@TableName("blockchain_transaction_record")
public class BlockchainTransactionRecord {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 区块链交易哈希
     */
    private String txHash;

    /**
     * JWT唯一标识
     */
    private String jti;

    /**
     * 操作用户ID
     */
    private Long userId;

    /**
     * 企业ID
     */
    private Long entId;

    /**
     * 区块链地址
     */
    private String blockchainAddress;

    /**
     * 操作类型
     */
    private String operation;

    /**
     * 合约名称
     */
    private String contractName;

    /**
     * 链ID
     */
    private String chainId;

    /**
     * 群组ID
     */
    private String groupId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
