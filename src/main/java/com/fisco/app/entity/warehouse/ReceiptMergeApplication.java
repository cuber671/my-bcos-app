package com.fisco.app.entity.warehouse;

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
 * 仓单合并申请实体类
 *
 * 功能：记录仓单合并申请和处理结果
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
@Table(name = "receipt_merge_application", indexes = {
    @Index(name = "idx_applicant", columnList = "applicant_id"),
    @Index(name = "idx_merged_receipt", columnList = "merged_receipt_id"),
    @Index(name = "idx_status", columnList = "request_status"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@ApiModel(value = "仓单合并申请", description = "仓单合并申请记录")
public class ReceiptMergeApplication {

    @Id
    @Column(name = "id", nullable = false, unique = true, length = 36)
    @ApiModelProperty(value = "申请ID（UUID格式）", required = true)
    private String id;

    @Column(name = "source_receipt_ids", nullable = false, columnDefinition = "TEXT")
    @ApiModelProperty(value = "源仓单ID列表（JSON数组）", required = true)
    private String sourceReceiptIds;

    @Column(name = "merged_receipt_id", length = 36)
    @ApiModelProperty(value = "合并后的仓单ID（审核通过后生成）")
    private String mergedReceiptId;

    @Column(name = "applicant_id", nullable = false, length = 36)
    @ApiModelProperty(value = "申请人ID（货主企业）", required = true)
    private String applicantId;

    @Column(name = "applicant_name", length = 100)
    @ApiModelProperty(value = "申请人姓名")
    private String applicantName;

    @Column(name = "merge_type", nullable = false, length = 20)
    @ApiModelProperty(value = "合并类型", required = true)
    private String mergeType;

    @Column(name = "total_quantity", precision = 20, scale = 2)
    @ApiModelProperty(value = "合并后总数量")
    private BigDecimal totalQuantity;

    @Column(name = "total_value", precision = 20, scale = 2)
    @ApiModelProperty(value = "合并后总价值")
    private BigDecimal totalValue;

    @Column(name = "merge_details", nullable = false, columnDefinition = "TEXT")
    @ApiModelProperty(value = "合并明细（JSON格式）", required = true)
    private String mergeDetails;

    @Column(name = "request_status", nullable = false, length = 20)
    @ApiModelProperty(value = "申请状态", required = true)
    private String requestStatus;

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
    @ApiModelProperty(value = "审核意见")
    private String reviewComments;

    @Column(name = "merge_tx_hash", length = 128)
    @ApiModelProperty(value = "合并上链交易哈希")
    private String mergeTxHash;

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

    /**
     * 合并类型枚举
     */
    public enum MergeType {
        QUANTITY("数量合并", "相同货物名称和单价的仓单合并，数量和总价值相加"),
        VALUE("价值合并", "不同货物的仓单按价值比例合并"),
        FULL("完全合并", "完全重新计算合并后的仓单属性");

        private final String name;
        private final String description;

        MergeType(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 申请状态枚举
     */
    public enum ApplicationStatus {
        PENDING("待审核"),
        APPROVED("已通过"),
        REJECTED("已拒绝"),
        CANCELLED("已取消");

        private final String name;

        ApplicationStatus(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
