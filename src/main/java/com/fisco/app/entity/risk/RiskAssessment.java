package com.fisco.app.entity.risk;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.PrePersist;
import javax.persistence.Table;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 风险评估实体类
 * 记录企业风险评估历史
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "risk_assessment", indexes = {
    @Index(name = "idx_enterprise", columnList = "enterprise_address"),
    @Index(name = "idx_assessment_time", columnList = "assessment_time"),
    @Index(name = "idx_risk_level", columnList = "risk_level")
})
@ApiModel(value = "风险评估")
public class RiskAssessment {

    @Id
    @Column(name = "id", nullable = false, unique = true, length = 36)
    @ApiModelProperty(value = "评估ID", required = true)
    private String id;

    @Column(name = "enterprise_address", nullable = false, length = 42)
    @ApiModelProperty(value = "企业地址", required = true)
    private String enterpriseAddress;

    @Column(name = "enterprise_name", length = 200)
    @ApiModelProperty(value = "企业名称")
    private String enterpriseName;

    @Column(name = "assessment_type", nullable = false, length = 20)
    @ApiModelProperty(value = "评估类型", required = true)
    private String assessmentType;

    @Column(name = "assessment_time", nullable = false)
    @ApiModelProperty(value = "评估时间", required = true)
    private LocalDateTime assessmentTime;

    @Column(name = "risk_level", nullable = false, length = 20)
    @ApiModelProperty(value = "风险等级", required = true)
    private String riskLevel;

    @Column(name = "risk_score")
    @ApiModelProperty(value = "风险评分（0-100）")
    private Integer riskScore;

    @Column(name = "credit_score")
    @ApiModelProperty(value = "信用评分")
    private Integer creditScore;

    @Column(name = "overdue_count")
    @ApiModelProperty(value = "逾期次数")
    private Integer overdueCount;

    @Column(name = "overdue_amount")
    @ApiModelProperty(value = "逾期金额（分）")
    private Long overdueAmount;

    @Column(name = "overdue_rate")
    @ApiModelProperty(value = "逾期率")
    private java.math.BigDecimal overdueRate;

    @Column(name = "total_liability")
    @ApiModelProperty(value = "总负债（分）")
    private Long totalLiability;

    @Column(name = "transaction_count")
    @ApiModelProperty(value = "交易次数")
    private Integer transactionCount;

    @Column(name = "warning_count")
    @ApiModelProperty(value = "风险预警数量")
    private Integer warningCount;

    @Column(name = "risk_factors", columnDefinition = "TEXT")
    @ApiModelProperty(value = "风险因素权重分析（JSON格式）")
    private String riskFactors;

    @Column(name = "recommendations", columnDefinition = "TEXT")
    @ApiModelProperty(value = "改进建议（JSON格式）")
    private String recommendations;

    @Column(name = "created_at", nullable = false, updatable = false)
    @ApiModelProperty(value = "创建时间", required = true)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null || id.isEmpty()) {
            id = java.util.UUID.randomUUID().toString();
        }
        createdAt = LocalDateTime.now();
    }

    /**
     * 风险等级枚举
     */
    public enum RiskLevel {
        VERY_LOW("极低风险"),
        LOW("低风险"),
        MEDIUM("中等风险"),
        HIGH("高风险"),
        VERY_HIGH("极高风险");

        private final String description;

        RiskLevel(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
