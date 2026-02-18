package com.fisco.app.entity.warehouse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import java.time.LocalDateTime;

import javax.persistence.*;

/**
 * 仓单作废申请实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "receipt_cancel_application", indexes = {
    @Index(name = "idx_receipt", columnList = "receipt_id"),
    @Index(name = "idx_status", columnList = "request_status"),
    @Index(name = "idx_created_at", columnList = "created_at"),
    @Index(name = "idx_applicant", columnList = "applicant_id")
})
@ApiModel(value = "仓单作废申请", description = "仓单作废申请记录")
public class ReceiptCancelApplication {

    @Id
    @Column(name = "id", nullable = false, unique = true, length = 36)
    @ApiModelProperty(value = "申请ID（UUID格式）", required = true)
    private String id;

    @Column(name = "receipt_id", nullable = false, length = 36)
    @ApiModelProperty(value = "仓单ID（UUID）", required = true)
    private String receiptId;

    @Column(name = "cancel_reason", nullable = false, columnDefinition = "TEXT")
    @ApiModelProperty(value = "作废原因", required = true, example = "货物质量问题")
    private String cancelReason;

    @Column(name = "cancel_type", nullable = false, length = 50)
    @ApiModelProperty(value = "作废类型", required = true, example = "QUALITY_ISSUE")
    private String cancelType;

    @Column(name = "evidence", columnDefinition = "TEXT")
    @ApiModelProperty(value = "证明材料", example = "质量检验报告")
    private String evidence;

    @Column(name = "reference_no", length = 100)
    @ApiModelProperty(value = "参考编号", example = "法律文书号")
    private String referenceNo;

    @Column(name = "request_status", nullable = false, length = 20)
    @ApiModelProperty(value = "申请状态", required = true, example = "PENDING")
    private String requestStatus;

    @Column(name = "applicant_id", length = 36)
    @ApiModelProperty(value = "申请人ID", example = "user-uuid-001")
    private String applicantId;

    @Column(name = "applicant_name", length = 100)
    @ApiModelProperty(value = "申请人姓名", example = "张三")
    private String applicantName;

    @Column(name = "reviewer_id", length = 36)
    @ApiModelProperty(value = "审核人ID")
    private String reviewerId;

    @Column(name = "reviewer_name", length = 100)
    @ApiModelProperty(value = "审核人姓名")
    private String reviewerName;

    @Column(name = "review_time")
    @ApiModelProperty(value = "审核时间")
    private LocalDateTime reviewTime;

    @Column(name = "review_comments", columnDefinition = "TEXT")
    @ApiModelProperty(value = "审核意见", example = "审核通过")
    private String reviewComments;

    @Column(name = "cancel_tx_hash", length = 128)
    @ApiModelProperty(value = "作废上链交易哈希")
    private String cancelTxHash;

    @Column(name = "block_number")
    @ApiModelProperty(value = "区块号")
    private Long blockNumber;

    @Column(name = "remarks", columnDefinition = "TEXT")
    @ApiModelProperty(value = "备注")
    private String remarks;

    @Column(name = "created_at", nullable = false)
    @ApiModelProperty(value = "申请时间")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (requestStatus == null) {
            requestStatus = "PENDING";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
