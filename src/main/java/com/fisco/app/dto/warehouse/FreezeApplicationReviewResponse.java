package com.fisco.app.dto.warehouse;
import java.time.LocalDateTime;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 冻结申请审核响应DTO
 */
@Data
@ApiModel(value = "冻结申请审核响应", description = "冻结申请审核结果")
public class FreezeApplicationReviewResponse {

    @ApiModelProperty(value = "申请ID", example = "app-uuid-001")
    private String applicationId;

    @ApiModelProperty(value = "仓单ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String receiptId;

    @ApiModelProperty(value = "仓单编号", example = "EWR20260126000001")
    private String receiptNo;

    @ApiModelProperty(value = "审核前状态", example = "PENDING")
    private String previousStatus;

    @ApiModelProperty(value = "审核后状态", example = "APPROVED")
    private String currentStatus;

    @ApiModelProperty(value = "审核结果", example = "APPROVED")
    private String reviewResult;

    @ApiModelProperty(value = "审核结果描述", example = "已批准")
    private String reviewResultDesc;

    @ApiModelProperty(value = "仓单状态", example = "FROZEN")
    private String receiptStatus;

    @ApiModelProperty(value = "仓单状态描述", example = "已冻结")
    private String receiptStatusDesc;

    @ApiModelProperty(value = "审核时间", example = "2026-01-27T11:00:00")
    private LocalDateTime reviewTime;

    @ApiModelProperty(value = "审核人", example = "管理员")
    private String reviewerName;

    @ApiModelProperty(value = "区块链交易哈希", example = "0xabc123...")
    private String txHash;

    @ApiModelProperty(value = "区块号", example = "12345")
    private Long blockNumber;

    @ApiModelProperty(value = "是否成功", example = "true")
    private Boolean success;

    @ApiModelProperty(value = "消息", example = "冻结申请审核通过，仓单已冻结并上链")
    private String message;

    /**
     * 创建审核通过的响应
     */
    public static FreezeApplicationReviewResponse approved(
            String applicationId,
            String receiptId,
            String receiptNo,
            String reviewerName,
            String txHash,
            Long blockNumber) {
        FreezeApplicationReviewResponse response = new FreezeApplicationReviewResponse();
        response.setApplicationId(applicationId);
        response.setReceiptId(receiptId);
        response.setReceiptNo(receiptNo);
        response.setPreviousStatus("PENDING");
        response.setCurrentStatus("APPROVED");
        response.setReviewResult("APPROVED");
        response.setReviewResultDesc("已批准");
        response.setReceiptStatus("FROZEN");
        response.setReceiptStatusDesc("已冻结");
        response.setReviewTime(LocalDateTime.now());
        response.setReviewerName(reviewerName);
        response.setTxHash(txHash);
        response.setBlockNumber(blockNumber);
        response.setSuccess(true);
        response.setMessage("冻结申请审核通过，仓单已冻结并上链");
        return response;
    }

    /**
     * 创建审核拒绝的响应
     */
    public static FreezeApplicationReviewResponse rejected(
            String applicationId,
            String receiptId,
            String receiptNo,
            String reviewerName,
            String rejectionReason) {
        FreezeApplicationReviewResponse response = new FreezeApplicationReviewResponse();
        response.setApplicationId(applicationId);
        response.setReceiptId(receiptId);
        response.setReceiptNo(receiptNo);
        response.setPreviousStatus("PENDING");
        response.setCurrentStatus("REJECTED");
        response.setReviewResult("REJECTED");
        response.setReviewResultDesc("已拒绝");
        response.setReviewTime(LocalDateTime.now());
        response.setReviewerName(reviewerName);
        response.setSuccess(false);
        response.setMessage("冻结申请审核拒绝：" + rejectionReason);
        return response;
    }
}
