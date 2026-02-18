package com.fisco.app.entity.bill;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 票据合并申请实体类
 *
 * 功能：记录票据合并申请和处理结果
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-09
 * @version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bill_merge_application", indexes = {
    @Index(name = "idx_applicant", columnList = "applicant_id"),
    @Index(name = "idx_merged_bill", columnList = "merged_bill_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@ApiModel(value = "票据合并申请", description = "票据合并申请记录")
public class BillMergeApplication {

    @Id
    @Column(name = "id", nullable = false, unique = true, length = 36)
    @ApiModelProperty(value = "申请ID（UUID格式）", required = true)
    private String id;

    @Column(name = "source_bill_ids", nullable = false, columnDefinition = "TEXT")
    @ApiModelProperty(value = "源票据ID列表（JSON数组）", required = true)
    private String sourceBillIds;

    @Column(name = "merged_bill_id", length = 36)
    @ApiModelProperty(value = "合并后的票据ID（处理完成后生成）")
    private String mergedBillId;

    @Column(name = "applicant_id", nullable = false, length = 36)
    @ApiModelProperty(value = "申请人ID", required = true)
    private String applicantId;

    @Column(name = "merge_type", nullable = false, length = 20)
    @ApiModelProperty(value = "合并类型", required = true, notes = "AMOUNT-金额合并, PERIOD-期限合并, FULL-完全合并")
    private String mergeType;

    @Column(name = "total_amount", nullable = false, precision = 20, scale = 2)
    @ApiModelProperty(value = "合并后总金额", required = true)
    private BigDecimal totalAmount;

    @Column(name = "merge_details", nullable = false, columnDefinition = "TEXT")
    @ApiModelProperty(value = "合并明细（JSON格式）", required = true)
    private String mergeDetails;

    @Column(name = "status", nullable = false, length = 20)
    @ApiModelProperty(value = "状态", required = true, notes = "PENDING-待处理, COMPLETED-已完成, FAILED-失败")
    private String status;

    @Column(name = "processor_id", length = 36)
    @ApiModelProperty(value = "处理人ID")
    private String processorId;

    @Column(name = "processed_time")
    @ApiModelProperty(value = "处理时间")
    private LocalDateTime processedTime;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    @ApiModelProperty(value = "失败原因")
    private String failureReason;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(6)")
    @ApiModelProperty(value = "创建时间", required = true)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    @ApiModelProperty(value = "更新时间", required = true)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = "PENDING";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 合并类型枚举
     */
    public enum MergeType {
        AMOUNT,  // 金额合并
        PERIOD,  // 期限合并
        FULL     // 完全合并
    }

    /**
     * 申请状态枚举
     */
    public enum ApplicationStatus {
        PENDING,   // 待处理
        COMPLETED, // 已完成
        FAILED     // 失败
    }
}
