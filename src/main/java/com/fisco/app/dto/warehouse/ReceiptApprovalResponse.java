package com.fisco.app.dto.warehouse;
import java.time.LocalDateTime;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 仓单审核响应DTO
 */
@Data
@ApiModel(value = "仓单审核响应", description = "仓单审核结果")
public class ReceiptApprovalResponse {

    @ApiModelProperty(value = "仓单ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String receiptId;

    @ApiModelProperty(value = "仓单编号", example = "EWR20260127000001")
    private String receiptNo;

    @ApiModelProperty(value = "审核前状态", example = "DRAFT")
    private String previousStatus;

    @ApiModelProperty(value = "审核后状态", example = "NORMAL")
    private String currentStatus;

    @ApiModelProperty(value = "审核结果", example = "APPROVED")
    private String approvalResult;

    @ApiModelProperty(value = "审核结果描述", example = "审核通过")
    private String approvalResultDesc;

    @ApiModelProperty(value = "审核意见", example = "货物已验收，数量质量符合要求")
    private String approvalComments;

    @ApiModelProperty(value = "审核时间", example = "2026-01-27T15:30:00")
    private LocalDateTime approvalTime;

    @ApiModelProperty(value = "审核人（仓储方操作员）", example = "李四（仓储方仓库管理员）")
    private String approverName;

    @ApiModelProperty(value = "是否成功", example = "true")
    private Boolean success;

    @ApiModelProperty(value = "消息", example = "仓单审核通过")
    private String message;

    /**
     * 创建审核通过的响应
     */
    public static ReceiptApprovalResponse approved(String receiptId, String receiptNo,
            String approverName, String comments) {
        ReceiptApprovalResponse response = new ReceiptApprovalResponse();
        response.setReceiptId(receiptId);
        response.setReceiptNo(receiptNo);
        response.setPreviousStatus("DRAFT");
        response.setCurrentStatus("NORMAL");
        response.setApprovalResult("APPROVED");
        response.setApprovalResultDesc("审核通过");
        response.setApprovalComments(comments);
        response.setApprovalTime(LocalDateTime.now());
        response.setApproverName(approverName);
        response.setSuccess(true);
        response.setMessage("仓单审核通过，已正式入库");
        return response;
    }

    /**
     * 创建审核拒绝的响应
     */
    public static ReceiptApprovalResponse rejected(String receiptId, String receiptNo,
            String approverName, String reason) {
        ReceiptApprovalResponse response = new ReceiptApprovalResponse();
        response.setReceiptId(receiptId);
        response.setReceiptNo(receiptNo);
        response.setPreviousStatus("DRAFT");
        response.setCurrentStatus("DRAFT");
        response.setApprovalResult("REJECTED");
        response.setApprovalResultDesc("审核拒绝");
        response.setApprovalComments(reason);
        response.setApprovalTime(LocalDateTime.now());
        response.setApproverName(approverName);
        response.setSuccess(false);
        response.setMessage("仓单审核拒绝：" + reason);
        return response;
    }
}
