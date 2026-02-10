package com.fisco.app.entity.bill;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 票据拆分申请实体类
 *
 * 功能：记录票据拆分申请和处理结果
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
@Table(name = "bill_split_application", indexes = {
    @Index(name = "idx_parent_bill", columnList = "parent_bill_id"),
    @Index(name = "idx_applicant", columnList = "applicant_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@ApiModel(value = "票据拆分申请", description = "票据拆分申请记录")
public class BillSplitApplication {

    @Id
    @Column(name = "id", nullable = false, unique = true, length = 36)
    @ApiModelProperty(value = "申请ID（UUID格式）", required = true)
    private String id;

    @Column(name = "parent_bill_id", nullable = false, length = 36)
    @ApiModelProperty(value = "父票据ID（UUID）", required = true)
    private String parentBillId;

    @Column(name = "applicant_id", nullable = false, length = 36)
    @ApiModelProperty(value = "申请人ID（当前持票人）", required = true)
    private String applicantId;

    @Column(name = "split_scheme", nullable = false, length = 20)
    @ApiModelProperty(value = "拆分方案", required = true, notes = "EQUAL-等额, CUSTOM-自定义")
    private String splitScheme;

    @Column(name = "split_count", nullable = false)
    @ApiModelProperty(value = "拆分数量", required = true, example = "3")
    private Integer splitCount;

    @Column(name = "split_details", nullable = false, columnDefinition = "TEXT")
    @ApiModelProperty(value = "拆分明细（JSON格式）", required = true, notes = "包含每个子票据的信息")
    private String splitDetails;

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
     * 拆分方案枚举
     */
    public enum SplitScheme {
        EQUAL,   // 等额拆分
        CUSTOM   // 自定义拆分
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
