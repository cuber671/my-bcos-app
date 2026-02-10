package com.fisco.app.entity.enterprise;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.PrePersist;
import javax.persistence.Table;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 企业信用评级历史实体类
 * 记录企业信用评级的变更历史
 */
@Data
@Entity
@Table(name = "credit_rating_history", indexes = {
    @Index(name = "idx_rating_enterprise", columnList = "enterprise_address"),
    @Index(name = "idx_rating_changed_by", columnList = "changed_by"),
    @Index(name = "idx_rating_changed_at", columnList = "changed_at")
})
@ApiModel(value = "信用评级历史", description = "企业信用评级变更记录实体")
@Schema(name = "信用评级历史")
public class CreditRatingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "主键ID", hidden = true)
    private Long id;

    @Column(name = "enterprise_address", nullable = false, length = 42)
    @ApiModelProperty(value = "企业区块链地址", required = true, example = "0x1234567890abcdef1234567890abcdef12345678")
    private String enterpriseAddress;

    @Column(name = "enterprise_name", length = 255)
    @ApiModelProperty(value = "企业名称", example = "供应商A")
    private String enterpriseName;

    @Column(name = "old_rating", nullable = false)
    @ApiModelProperty(value = "原评级", required = true, example = "60")
    private Integer oldRating;

    @Column(name = "new_rating", nullable = false)
    @ApiModelProperty(value = "新评级", required = true, example = "75")
    private Integer newRating;

    @Column(name = "change_reason", columnDefinition = "TEXT")
    @ApiModelProperty(value = "变更原因", example = "按时还款记录良好，提升评级")
    private String changeReason;

    @Column(name = "changed_by", nullable = false, length = 100)
    @ApiModelProperty(value = "操作人用户名", required = true, example = "admin")
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    @ApiModelProperty(value = "变更时间", required = true)
    private LocalDateTime changedAt;

    @Column(name = "tx_hash", length = 128)
    @ApiModelProperty(value = "区块链交易哈希", example = "0xabcdef...")
    private String txHash;

    @PrePersist
    protected void onCreate() {
        if (changedAt == null) {
            changedAt = LocalDateTime.now();
        }
    }
}
