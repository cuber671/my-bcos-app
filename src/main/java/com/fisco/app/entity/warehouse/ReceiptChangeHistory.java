package com.fisco.app.entity.warehouse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 仓单变更历史实体类
 *
 * 功能：记录仓单信息变更的完整历史，确保可追溯性
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
@Table(name = "receipt_change_history", indexes = {
    @Index(name = "idx_receipt", columnList = "receipt_id"),
    @Index(name = "idx_change_time", columnList = "change_time"),
    @Index(name = "idx_operator", columnList = "operator_id"),
    @Index(name = "idx_receipt_no", columnList = "receipt_no")
})
@ApiModel(value = "仓单变更历史", description = "仓单信息变更记录")
public class ReceiptChangeHistory {

    @Id
    @Column(name = "id", nullable = false, unique = true, length = 36)
    @ApiModelProperty(value = "变更记录ID（UUID格式）", required = true)
    private String id;

    @Column(name = "receipt_id", nullable = false, length = 36)
    @ApiModelProperty(value = "仓单ID", required = true)
    private String receiptId;

    @Column(name = "receipt_no", nullable = false, length = 64)
    @ApiModelProperty(value = "仓单编号", required = true)
    private String receiptNo;

    @Column(name = "change_type", nullable = false, length = 50)
    @ApiModelProperty(value = "变更类型", required = true)
    private String changeType;

    @Column(name = "change_reason", nullable = false, columnDefinition = "TEXT")
    @ApiModelProperty(value = "变更原因", required = true)
    private String changeReason;

    @Column(name = "before_value", columnDefinition = "TEXT")
    @ApiModelProperty(value = "变更前值（JSON格式）")
    private String beforeValue;

    @Column(name = "after_value", columnDefinition = "TEXT")
    @ApiModelProperty(value = "变更后值（JSON格式）")
    private String afterValue;

    @Column(name = "changed_fields", columnDefinition = "TEXT")
    @ApiModelProperty(value = "变更字段列表（JSON数组）")
    private String changedFields;

    @Column(name = "operator_id", nullable = false, length = 36)
    @ApiModelProperty(value = "操作人ID", required = true)
    private String operatorId;

    @Column(name = "operator_name", length = 100)
    @ApiModelProperty(value = "操作人姓名")
    private String operatorName;

    @Column(name = "change_time", nullable = false)
    @ApiModelProperty(value = "变更时间", required = true)
    private LocalDateTime changeTime;

    @Column(name = "remarks", columnDefinition = "TEXT")
    @ApiModelProperty(value = "备注")
    private String remarks;

    @PrePersist
    protected void onCreate() {
        changeTime = LocalDateTime.now();
    }

    /**
     * 变更类型枚举
     */
    public enum ChangeType {
        PRICE_ADJUSTMENT("价格调整"),
        LOCATION_CHANGE("位置变更"),
        EXPIRY_EXTENSION("有效期延长"),
        INFO_UPDATE("信息更新"),
        OTHER("其他");

        private final String name;

        ChangeType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
