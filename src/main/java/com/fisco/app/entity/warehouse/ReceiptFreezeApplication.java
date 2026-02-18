package com.fisco.app.entity.warehouse;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 仓单冻结申请实体
 * 仓储方申请冻结，由管理员审核
 */
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "receipt_freeze_application", indexes = {
    @Index(name = "idx_receipt_id", columnList = "receipt_id"),
    @Index(name = "idx_warehouse_id", columnList = "warehouse_id"),
    @Index(name = "idx_request_status", columnList = "request_status"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
public class ReceiptFreezeApplication {

    @Id
    @Column(name = "id", nullable = false, unique = true, length = 36)
    @ApiModelProperty(value = "申请ID（UUID）")
    private String id;

    @Column(name = "receipt_id", nullable = false, length = 36)
    @ApiModelProperty(value = "仓单ID")
    private String receiptId;

    @Column(name = "receipt_no", nullable = false, length = 64)
    @ApiModelProperty(value = "仓单编号")
    private String receiptNo;

    @Column(name = "warehouse_id", nullable = false, length = 36)
    @ApiModelProperty(value = "仓储企业ID")
    private String warehouseId;

    @Column(name = "warehouse_address", length = 42)
    @ApiModelProperty(value = "仓储方区块链地址")
    private String warehouseAddress;

    @Column(name = "warehouse_name", length = 255)
    @ApiModelProperty(value = "仓储方名称")
    private String warehouseName;

    @Column(name = "freeze_reason", nullable = false, columnDefinition = "TEXT")
    @ApiModelProperty(value = "冻结原因")
    private String freezeReason;

    @Column(name = "freeze_type", nullable = false, length = 20)
    @ApiModelProperty(value = "冻结类型：LEGAL-法律冻结, BUSINESS-业务冻结, RISK-风险冻结")
    private String freezeType;

    @Column(name = "reference_no", length = 100)
    @ApiModelProperty(value = "相关文件编号")
    private String referenceNo;

    @Column(name = "applicant_id", nullable = false, length = 36)
    @ApiModelProperty(value = "申请人ID（仓储方操作人）")
    private String applicantId;

    @Column(name = "applicant_name", length = 100)
    @ApiModelProperty(value = "申请人姓名")
    private String applicantName;

    @Column(name = "request_status", nullable = false, length = 20)
    @ApiModelProperty(value = "申请状态：PENDING-待审核, APPROVED-已批准, REJECTED-已拒绝, CANCELLED-已撤销")
    private String requestStatus = "PENDING";

    @Column(name = "reviewer_id", length = 36)
    @ApiModelProperty(value = "审核人ID（管理员）")
    private String reviewerId;

    @Column(name = "reviewer_name", length = 100)
    @ApiModelProperty(value = "审核人姓名")
    private String reviewerName;

    @Column(name = "review_comments", columnDefinition = "TEXT")
    @ApiModelProperty(value = "审核意见")
    private String reviewComments;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    @ApiModelProperty(value = "拒绝原因")
    private String rejectionReason;

    @Column(name = "review_time")
    @ApiModelProperty(value = "审核时间")
    private LocalDateTime reviewTime;

    @Column(name = "freeze_time")
    @ApiModelProperty(value = "实际冻结时间")
    private LocalDateTime freezeTime;

    @Column(name = "freeze_tx_hash", length = 66)
    @ApiModelProperty(value = "冻结交易哈希")
    private String freezeTxHash;

    @Column(name = "block_number")
    @ApiModelProperty(value = "区块号")
    private Long blockNumber;

    @Column(name = "remarks", columnDefinition = "TEXT")
    @ApiModelProperty(value = "备注")
    private String remarks;

    @Column(name = "created_at", nullable = false)
    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updatedAt;

    // 关联关系
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id", insertable = false, updatable = false)
    @ApiModelProperty(hidden = true)
    private ElectronicWarehouseReceipt receipt;

    // 生命周期回调
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

    // 状态枚举
    public enum RequestStatus {
        PENDING,    // 待审核
        APPROVED,   // 已批准
        REJECTED,   // 已拒绝
        CANCELLED   // 已撤销
    }

    // 冻结类型枚举
    public enum FreezeType {
        LEGAL,      // 法律冻结
        BUSINESS,   // 业务冻结
        RISK        // 风险冻结
    }
}
