package com.fisco.app.entity.credit;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.springframework.lang.NonNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fisco.app.enums.CreditUsageType;

import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 信用额度使用记录实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@Entity
@Table(name = "credit_limit_usage", indexes = {
    @Index(name = "idx_credit_limit_id", columnList = "credit_limit_id"),
    @Index(name = "idx_usage_type", columnList = "usage_type"),
    @Index(name = "idx_business_type", columnList = "business_type"),
    @Index(name = "idx_business_id", columnList = "business_id"),
    @Index(name = "idx_usage_date", columnList = "usage_date")
})
@Schema(name = "信用额度使用记录")
public class CreditLimitUsage {

    /**
     * 记录ID（UUID格式，主键）
     */
    @Id
    @Column(name = "id", nullable = false, unique = true, length = 36)
    @ApiModelProperty(value = "记录ID（UUID格式）", required = true, example = "b2c3d4e5-f6g7-8901-bcde-f12345678901")
    private String id;

    /**
     * 额度ID（外键）
     */
    @Column(name = "credit_limit_id", nullable = false, length = 36)
    @ApiModelProperty(value = "额度ID", required = true, example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    @NotNull(message = "额度ID不能为空")
    @Getter(onMethod_ = @__({@NonNull}))
    private String creditLimitId;

    /**
     * 使用类型（使用/释放/冻结/解冻）
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "usage_type", nullable = false, length = 20)
    @ApiModelProperty(value = "使用类型", required = true, notes = "USE-使用, RELEASE-释放, FREEZE-冻结, UNFREEZE-解冻", example = "USE")
    @NotNull(message = "使用类型不能为空")
    private CreditUsageType usageType;

    /**
     * 业务类型（融资申请/担保申请/赊账等）
     */
    @Column(name = "business_type", nullable = false, length = 50)
    @ApiModelProperty(value = "业务类型", required = true, notes = "FINANCING_APPLICATION-融资申请, GUARANTEE_APPLICATION-担保申请, CREDIT_PURCHASE-赊账采购", example = "FINANCING_APPLICATION")
    @NotBlank(message = "业务类型不能为空")
    private String businessType;

    /**
     * 业务ID（关联业务表的主键）
     */
    @Column(name = "business_id", nullable = false, length = 36)
    @ApiModelProperty(value = "业务ID", required = true, example = "c3d4e5f6-g7h8-9012-cdef-234567890123", notes = "关联业务表的主键，如融资申请ID、担保申请ID等")
    @NotBlank(message = "业务ID不能为空")
    private String businessId;

    /**
     * 使用金额（单位：分，正数表示增加使用或冻结，负数表示释放或解冻）
     */
    @Column(name = "amount", nullable = false)
    @ApiModelProperty(value = "使用金额（单位：分）", required = true, example = "5000000", notes = "5000000分 = 50000.00元，正数表示使用/冻结，负数表示释放/解冻")
    @NotNull(message = "使用金额不能为空")
    private Long amount;

    /**
     * 使用前可用额度（单位：分）
     */
    @Column(name = "before_available", nullable = false)
    @ApiModelProperty(value = "使用前可用额度（单位：分）", required = true, example = "100000000", notes = "100000000分 = 1000000.00元")
    @NotNull(message = "使用前可用额度不能为空")
    private Long beforeAvailable;

    /**
     * 使用后可用额度（单位：分）
     */
    @Column(name = "after_available", nullable = false)
    @ApiModelProperty(value = "使用后可用额度（单位：分）", required = true, example = "95000000", notes = "95000000分 = 950000.00元")
    @NotNull(message = "使用后可用额度不能为空")
    private Long afterAvailable;

    /**
     * 使用前已使用额度（单位：分）
     */
    @Column(name = "before_used", nullable = false)
    @ApiModelProperty(value = "使用前已使用额度（单位：分）", required = true, example = "0")
    @NotNull(message = "使用前已使用额度不能为空")
    private Long beforeUsed;

    /**
     * 使用后已使用额度（单位：分）
     */
    @Column(name = "after_used", nullable = false)
    @ApiModelProperty(value = "使用后已使用额度（单位：分）", required = true, example = "5000000")
    @NotNull(message = "使用后已使用额度不能为空")
    private Long afterUsed;

    /**
     * 使用前冻结额度（单位：分）
     */
    @Column(name = "before_frozen", nullable = false)
    @ApiModelProperty(value = "使用前冻结额度（单位：分）", required = true, example = "0")
    @NotNull(message = "使用前冻结额度不能为空")
    private Long beforeFrozen;

    /**
     * 使用后冻结额度（单位：分）
     */
    @Column(name = "after_frozen", nullable = false)
    @ApiModelProperty(value = "使用后冻结额度（单位：分）", required = true, example = "0")
    @NotNull(message = "使用后冻结额度不能为空")
    private Long afterFrozen;

    /**
     * 操作人地址（区块链地址）
     */
    @Column(name = "operator_address", length = 42)
    @ApiModelProperty(value = "操作人地址", example = "0x1234567890abcdef1234567890abcdef12345678")
    @Size(min = 42, max = 42, message = "操作人地址必须是42位")
    private String operatorAddress;

    /**
     * 操作人姓名（冗余字段，方便查询）
     */
    @Column(name = "operator_name", length = 100)
    @ApiModelProperty(value = "操作人姓名", example = "张三")
    private String operatorName;

    /**
     * 使用日期
     */
    @Column(name = "usage_date", nullable = false)
    @ApiModelProperty(value = "使用日期", required = true, example = "2026-01-15T10:30:00")
    @NotNull(message = "使用日期不能为空")
    private LocalDateTime usageDate;

    /**
     * 备注说明
     */
    @Column(name = "remark", columnDefinition = "TEXT")
    @ApiModelProperty(value = "备注说明", example = "融资申请FNA001占用额度")
    private String remark;

    /**
     * 关联的信用额度（多对一关联）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_limit_id", insertable = false, updatable = false)
    @JsonIgnore
    @ApiModelProperty(value = "关联的信用额度", hidden = true)
    private CreditLimit creditLimit;

    /**
     * 区块链交易哈希
     */
    @Column(name = "tx_hash", length = 66)
    @ApiModelProperty(value = "区块链交易哈希", example = "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890")
    @Size(max = 66, message = "交易哈希最多66位")
    private String txHash;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @ApiModelProperty(value = "创建时间", hidden = true)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null || id.isEmpty()) {
            id = java.util.UUID.randomUUID().toString();
        }
        if (usageDate == null) {
            usageDate = LocalDateTime.now();
        }
        createdAt = LocalDateTime.now();
    }

    /**
     * 获取金额（元，用于显示）
     * @return 金额（元，保留2位小数）
     */
    @Transient
    @ApiModelProperty(value = "使用金额（元）", example = "50000.00", notes = "用于显示，自动从分转换为元")
    public BigDecimal getAmountInYuan() {
        return amount != null ? new BigDecimal(amount).divide(new BigDecimal(100), 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }
}
