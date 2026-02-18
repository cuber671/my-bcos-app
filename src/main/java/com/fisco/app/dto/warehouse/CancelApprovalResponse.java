package com.fisco.app.dto.warehouse;
import java.time.LocalDateTime;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 仓单作废审核响应DTO
 */
@Data
@ApiModel(value = "仓单作废审核响应", description = "作废申请审核结果")
public class CancelApprovalResponse {

    @ApiModelProperty(value = "申请ID", example = "app-uuid-001")
    private String applicationId;

    @ApiModelProperty(value = "仓单ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String receiptId;

    @ApiModelProperty(value = "仓单编号", example = "EWR20260126000001")
    private String receiptNo;

    @ApiModelProperty(value = "作废原因", example = "货物质量问题")
    private String cancelReason;

    @ApiModelProperty(value = "作废类型", example = "QUALITY_ISSUE")
    private String cancelType;

    @ApiModelProperty(value = "审核前状态", example = "CANCELLING")
    private String previousStatus;

    @ApiModelProperty(value = "审核后状态", example = "CANCELLED")
    private String currentStatus;

    @ApiModelProperty(value = "审核结果", example = "APPROVED")
    private String approvalResult;

    @ApiModelProperty(value = "审核结果描述", example = "已批准")
    private String approvalResultDesc;

    @ApiModelProperty(value = "仓单状态", example = "CANCELLED")
    private String receiptStatus;

    @ApiModelProperty(value = "仓单状态描述", example = "已作废")
    private String receiptStatusDesc;

    @ApiModelProperty(value = "区块链交易哈希", example = "0xabc123...")
    private String txHash;

    @ApiModelProperty(value = "区块号", example = "12345")
    private Long blockNumber;

    @ApiModelProperty(value = "审核时间", example = "2026-02-02T11:00:00")
    private LocalDateTime reviewTime;

    @ApiModelProperty(value = "审核人", example = "管理员")
    private String reviewerName;

    @ApiModelProperty(value = "是否成功", example = "true")
    private Boolean success;

    @ApiModelProperty(value = "消息", example = "作废申请审核通过，仓单已作废")
    private String message;

    /**
     * 创建审核通过的响应
     */
    public static CancelApprovalResponse approved(
            String applicationId,
            String receiptId,
            String receiptNo,
            String cancelReason,
            String cancelType,
            String reviewerName,
            String txHash,
            Long blockNumber) {
        CancelApprovalResponse response = new CancelApprovalResponse();
        response.setApplicationId(applicationId);
        response.setReceiptId(receiptId);
        response.setReceiptNo(receiptNo);
        response.setCancelReason(cancelReason);
        response.setCancelType(cancelType);
        response.setPreviousStatus("CANCELLING");
        response.setCurrentStatus("CANCELLED");
        response.setApprovalResult("APPROVED");
        response.setApprovalResultDesc("已批准");
        response.setReceiptStatus("CANCELLED");
        response.setReceiptStatusDesc("已作废");
        response.setReviewTime(LocalDateTime.now());
        response.setReviewerName(reviewerName);
        response.setTxHash(txHash);
        response.setBlockNumber(blockNumber);
        response.setSuccess(true);
        response.setMessage("作废申请审核通过，仓单已作废");
        return response;
    }

    /**
     * 创建审核拒绝的响应
     */
    public static CancelApprovalResponse rejected(
            String applicationId,
            String receiptId,
            String receiptNo,
            String reviewerName,
            String rejectionReason) {
        CancelApprovalResponse response = new CancelApprovalResponse();
        response.setApplicationId(applicationId);
        response.setReceiptId(receiptId);
        response.setReceiptNo(receiptNo);
        response.setPreviousStatus("CANCELLING");
        response.setCurrentStatus("NORMAL");
        response.setApprovalResult("REJECTED");
        response.setApprovalResultDesc("已拒绝");
        response.setReviewTime(LocalDateTime.now());
        response.setReviewerName(reviewerName);
        response.setSuccess(false);
        response.setMessage("作废申请审核拒绝：" + rejectionReason);
        return response;
    }
}
