package com.fisco.app.entity.receivable;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 应收账款转让记录实体类
 *
 * 记录应收账款的转让历史，包括融资、转让等场景
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-09
 * @version 1.0
 */
@Data
@Entity
@Table(name = "receivable_transfer", indexes = {
    @Index(name = "idx_receivable", columnList = "receivable_id"),
    @Index(name = "idx_from", columnList = "from_address"),
    @Index(name = "idx_to", columnList = "to_address"),
    @Index(name = "idx_timestamp", columnList = "timestamp")
})
@ApiModel(value = "ReceivableTransfer", description = "应收账款转让记录")
public class ReceivableTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "转让记录ID", example = "1")
    private Long id;

    @Column(name = "receivable_id", length = 64, nullable = false)
    @ApiModelProperty(value = "应收账款ID", required = true, example = "REC20240113001")
    private String receivableId;

    @Column(name = "from_address", length = 42, nullable = false)
    @ApiModelProperty(value = "转出方地址", required = true, example = "0x1234567890abcdef")
    private String fromAddress;

    @Column(name = "to_address", length = 42, nullable = false)
    @ApiModelProperty(value = "转入方地址", required = true, example = "0xabcdef1234567890")
    private String toAddress;

    @Column(name = "amount", precision = 20, scale = 2, nullable = false)
    @ApiModelProperty(value = "转让金额", required = true, example = "500000.00")
    private BigDecimal amount;

    @Column(name = "transfer_type", length = 20, nullable = false)
    @ApiModelProperty(value = "转让类型", required = true, notes = "financing-融资, transfer-转让, repayment-还款", example = "financing")
    private String transferType;

    @Column(name = "timestamp", nullable = false)
    @ApiModelProperty(value = "时间戳", required = true, example = "2026-02-09T14:30:00")
    private LocalDateTime timestamp;

    @Column(name = "tx_hash", length = 66)
    @ApiModelProperty(value = "区块链交易哈希", example = "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef12")
    private String txHash;
}
