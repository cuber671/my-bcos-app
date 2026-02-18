package com.fisco.app.dto.warehouse;

import java.time.LocalDateTime;
import java.util.List;

import com.fisco.app.entity.warehouse.ReceiptMergeApplication;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 仓单合并响应DTO
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-09
 * @version 1.0
 */
@Data
@ApiModel(value = "仓单合并响应", description = "仓单合并操作的响应信息")
public class ReceiptMergeResponse {

    @ApiModelProperty(value = "合并申请ID", example = "merge-uuid-001")
    private String applicationId;

    @ApiModelProperty(value = "申请状态", example = "PENDING")
    private String status;

    @ApiModelProperty(value = "请求状态（冗余字段，与status相同）", example = "PENDING")
    private String requestStatus;

    @ApiModelProperty(value = "源仓单列表")
    private List<ElectronicWarehouseReceiptResponse> sourceReceipts;

    @ApiModelProperty(value = "合并后的仓单（审核通过后生成）")
    private ElectronicWarehouseReceiptResponse mergedReceipt;

    @ApiModelProperty(value = "合并数量", example = "3")
    private Integer mergeCount;

    @ApiModelProperty(value = "合并后总价值", example = "1500000.00")
    private java.math.BigDecimal totalValue;

    @ApiModelProperty(value = "合并后总数量", example = "3000.00")
    private java.math.BigDecimal totalQuantity;

    @ApiModelProperty(value = "合并类型", notes = "HORIZONTAL-水平合并（同类型货物）, VERTICAL-垂直合并（不同等级货物）")
    private ReceiptMergeApplication.MergeType mergeType;

    @ApiModelProperty(value = "申请人ID", example = "USER-001")
    private String applicantId;

    @ApiModelProperty(value = "申请时间", example = "2026-02-09T10:30:00")
    private LocalDateTime applicationTime;

    @ApiModelProperty(value = "审核人ID", example = "ADMIN-001")
    private String reviewerId;

    @ApiModelProperty(value = "审核时间", example = "2026-02-09T14:00:00")
    private LocalDateTime reviewTime;

    @ApiModelProperty(value = "拒绝原因")
    private String rejectionReason;

    @ApiModelProperty(value = "操作结果", example = "success")
    private String result;

    @ApiModelProperty(value = "消息", example = "仓单合并申请已提交")
    private String message;
}
