package com.fisco.app.entity.credit;

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
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.springframework.lang.NonNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fisco.app.enums.CreditAdjustRequestStatus;
import com.fisco.app.enums.CreditAdjustType;

import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 信用额度调整申请实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@Entity
@Table(name = "credit_limit_adjust_request", indexes = {
    @Index(name = "idx_credit_limit_id", columnList = "credit_limit_id"),
    @Index(name = "idx_adjust_type", columnList = "adjust_type"),
    @Index(name = "idx_request_status", columnList = "request_status"),
    @Index(name = "idx_requester_address", columnList = "requester_address"),
    @Index(name = "idx_request_date", columnList = "request_date")
})
@Schema(name = "信用额度调整申请")
public class CreditLimitAdjustRequest {

    /**
     * 申请ID（UUID格式，主键）
     */
    @Id
    @Column(name = "id", nullable = false, unique = true, length = 36)
    @ApiModelProperty(value = "申请ID（UUID格式）", required = true, example = "d4e5f6g7-h8i9-0123-defg-345678901234")
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
     * 调整类型（增加/减少/重置）
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "adjust_type", nullable = false, length = 20)
    @ApiModelProperty(value = "调整类型", required = true, notes = "INCREASE-增加额度, DECREASE-减少额度, RESET-重置额度", example = "INCREASE")
    @NotNull(message = "调整类型不能为空")
    private CreditAdjustType adjustType;

    /**
     * 当前额度（单位：分）
     */
    @Column(name = "current_limit", nullable = false)
    @ApiModelProperty(value = "当前额度（单位：分）", required = true, example = "100000000", notes = "100000000分 = 1000000.00元")
    @NotNull(message = "当前额度不能为空")
    @Min(value = 0, message = "当前额度不能为负数")
    private Long currentLimit;

    /**
     * 调整后额度（单位：分）
     */
    @Column(name = "new_limit", nullable = false)
    @ApiModelProperty(value = "调整后额度（单位：分）", required = true, example = "150000000", notes = "150000000分 = 1500000.00元")
    @NotNull(message = "调整后额度不能为空")
    @Min(value = 0, message = "调整后额度不能为负数")
    private Long newLimit;

    /**
     * 调整金额（单位：分，可为正数或负数）
     */
    @Column(name = "adjust_amount", nullable = false)
    @ApiModelProperty(value = "调整金额（单位：分）", required = true, example = "50000000", notes = "50000000分 = 500000.00元，增加时为正数，减少时为负数")
    @NotNull(message = "调整金额不能为空")
    private Long adjustAmount;

    /**
     * 申请原因
     */
    @Column(name = "request_reason", nullable = false, columnDefinition = "TEXT")
    @ApiModelProperty(value = "申请原因", required = true, example = "企业信用评级提升，申请增加融资额度")
    @NotBlank(message = "申请原因不能为空")
    @Size(max = 1000, message = "申请原因长度不能超过1000")
    private String requestReason;

    /**
     * 申请人地址（区块链地址）
     */
    @Column(name = "requester_address", nullable = false, length = 42)
    @ApiModelProperty(value = "申请人地址", required = true, example = "0x1234567890abcdef1234567890abcdef12345678")
    @NotNull(message = "申请人地址不能为空")
    @Size(min = 42, max = 42, message = "申请人地址必须是42位")
    private String requesterAddress;

    /**
     * 申请人姓名（冗余字段，方便查询）
     */
    @Column(name = "requester_name", nullable = false, length = 100)
    @ApiModelProperty(value = "申请人姓名", required = true, example = "张三")
    @NotBlank(message = "申请人姓名不能为空")
    @Size(max = 100, message = "申请人姓名长度不能超过100")
    private String requesterName;

    /**
     * 申请日期
     */
    @Column(name = "request_date", nullable = false)
    @ApiModelProperty(value = "申请日期", required = true, example = "2026-01-15T10:30:00")
    @NotNull(message = "申请日期不能为空")
    private LocalDateTime requestDate;

    /**
     * 申请状态（待审批/已通过/已拒绝）
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "request_status", nullable = false, length = 20)
    @ApiModelProperty(value = "申请状态", required = true, notes = "PENDING-待审批, APPROVED-已通过, REJECTED-已拒绝", example = "PENDING")
    private CreditAdjustRequestStatus requestStatus = CreditAdjustRequestStatus.PENDING;

    /**
     * 审批人地址（区块链地址）
     */
    @Column(name = "approver_address", length = 42)
    @ApiModelProperty(value = "审批人地址", example = "0x9876543210fedcba9876543210fedcba98765432")
    @Size(min = 42, max = 42, message = "审批人地址必须是42位")
    private String approverAddress;

    /**
     * 审批人姓名（冗余字段，方便查询）
     */
    @Column(name = "approver_name", length = 100)
    @ApiModelProperty(value = "审批人姓名", example = "李四")
    @Size(max = 100, message = "审批人姓名长度不能超过100")
    private String approverName;

    /**
     * 审批日期
     */
    @Column(name = "approve_date")
    @ApiModelProperty(value = "审批日期", example = "2026-01-16T14:00:00")
    private LocalDateTime approveDate;

    /**
     * 审批意见
     */
    @Column(name = "approve_reason", columnDefinition = "TEXT")
    @ApiModelProperty(value = "审批意见", example = "同意增加额度，企业信用良好")
    private String approveReason;

    /**
     * 拒绝原因
     */
    @Column(name = "reject_reason", columnDefinition = "TEXT")
    @ApiModelProperty(value = "拒绝原因", example = "企业存在逾期记录，暂不符合增加额度条件")
    private String rejectReason;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @ApiModelProperty(value = "创建时间", hidden = true)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at", nullable = false)
    @ApiModelProperty(value = "更新时间", hidden = true)
    private LocalDateTime updatedAt;

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

    @PrePersist
    protected void onCreate() {
        if (id == null || id.isEmpty()) {
            id = java.util.UUID.randomUUID().toString();
        }
        if (requestDate == null) {
            requestDate = LocalDateTime.now();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 是否已审批
     * @return true-已审批（通过或拒绝），false-待审批
     */
    @Transient
    @ApiModelProperty(value = "是否已审批", example = "false")
    public boolean isProcessed() {
        return requestStatus == CreditAdjustRequestStatus.APPROVED
            || requestStatus == CreditAdjustRequestStatus.REJECTED;
    }

    /**
     * 是否已通过
     * @return true-已通过，false-未通过
     */
    @Transient
    @ApiModelProperty(value = "是否已通过", example = "false")
    public boolean isApproved() {
        return requestStatus == CreditAdjustRequestStatus.APPROVED;
    }

    /**
     * 是否已拒绝
     * @return true-已拒绝，false-未拒绝
     */
    @Transient
    @ApiModelProperty(value = "是否已拒绝", example = "false")
    public boolean isRejected() {
        return requestStatus == CreditAdjustRequestStatus.REJECTED;
    }
}
