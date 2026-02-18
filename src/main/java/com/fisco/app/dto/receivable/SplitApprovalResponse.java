package com.fisco.app.dto.receivable;
import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 仓单拆分审核响应DTO
 */
@Data
@ApiModel(value = "仓单拆分审核响应", description = "拆分申请审核结果")
public class SplitApprovalResponse {

    @ApiModelProperty(value = "申请ID", example = "app-uuid-001")
    private String applicationId;

    @ApiModelProperty(value = "父仓单ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String parentReceiptId;

    @ApiModelProperty(value = "父仓单编号", example = "EWR20260126000001")
    private String parentReceiptNo;

    @ApiModelProperty(value = "拆分原因", example = "部分货物用于质押融资")
    private String splitReason;

    @ApiModelProperty(value = "子仓单数量", example = "2")
    private Integer splitCount;

    @ApiModelProperty(value = "子仓单ID列表", example = "[\"child-id-1\", \"child-id-2\"]")
    private List<String> childReceiptIds;

    @ApiModelProperty(value = "审核前状态", example = "PENDING")
    private String previousStatus;

    @ApiModelProperty(value = "审核后状态", example = "APPROVED")
    private String currentStatus;

    @ApiModelProperty(value = "审核结果", example = "APPROVED")
    private String approvalResult;

    @ApiModelProperty(value = "审核结果描述", example = "已批准")
    private String approvalResultDesc;

    @ApiModelProperty(value = "父仓单状态", example = "SPLIT")
    private String parentReceiptStatus;

    @ApiModelProperty(value = "父仓单状态描述", example = "已拆分")
    private String parentReceiptStatusDesc;

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

    @ApiModelProperty(value = "消息", example = "拆分申请审核通过，仓单已拆分并上链")
    private String message;

    /**
     * 创建审核通过的响应
     */
    public static SplitApprovalResponse approved(
            String applicationId,
            String parentReceiptId,
            String parentReceiptNo,
            String splitReason,
            Integer splitCount,
            List<String> childReceiptIds,
            String reviewerName,
            String txHash,
            Long blockNumber) {
        SplitApprovalResponse response = new SplitApprovalResponse();
        response.setApplicationId(applicationId);
        response.setParentReceiptId(parentReceiptId);
        response.setParentReceiptNo(parentReceiptNo);
        response.setSplitReason(splitReason);
        response.setSplitCount(splitCount);
        response.setChildReceiptIds(childReceiptIds);
        response.setPreviousStatus("PENDING");
        response.setCurrentStatus("APPROVED");
        response.setApprovalResult("APPROVED");
        response.setApprovalResultDesc("已批准");
        response.setParentReceiptStatus("SPLIT");
        response.setParentReceiptStatusDesc("已拆分");
        response.setReviewTime(LocalDateTime.now());
        response.setReviewerName(reviewerName);
        response.setTxHash(txHash);
        response.setBlockNumber(blockNumber);
        response.setSuccess(true);
        response.setMessage("拆分申请审核通过，仓单已拆分并上链");
        return response;
    }

    /**
     * 创建审核拒绝的响应
     */
    public static SplitApprovalResponse rejected(
            String applicationId,
            String parentReceiptId,
            String parentReceiptNo,
            String reviewerName,
            String rejectionReason) {
        SplitApprovalResponse response = new SplitApprovalResponse();
        response.setApplicationId(applicationId);
        response.setParentReceiptId(parentReceiptId);
        response.setParentReceiptNo(parentReceiptNo);
        response.setPreviousStatus("PENDING");
        response.setCurrentStatus("REJECTED");
        response.setApprovalResult("REJECTED");
        response.setApprovalResultDesc("已拒绝");
        response.setReviewTime(LocalDateTime.now());
        response.setReviewerName(reviewerName);
        response.setSuccess(false);
        response.setMessage("拆分申请审核拒绝：" + rejectionReason);
        return response;
    }
}
