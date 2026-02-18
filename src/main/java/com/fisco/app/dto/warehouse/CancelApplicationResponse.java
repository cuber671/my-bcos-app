package com.fisco.app.dto.warehouse;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.time.LocalDateTime;

/**
 * 仓单作废申请响应DTO
 */
@Data
@ApiModel(value = "仓单作废申请响应", description = "作废申请提交结果")
public class CancelApplicationResponse {

    @ApiModelProperty(value = "申请ID", example = "app-uuid-001")
    private String applicationId;

    @ApiModelProperty(value = "仓单ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String receiptId;

    @ApiModelProperty(value = "仓单编号", example = "EWR20260126000001")
    private String receiptNo;

    @ApiModelProperty(value = "仓单状态（作废前）", example = "NORMAL")
    private String previousStatus;

    @ApiModelProperty(value = "仓单状态（作废后）", example = "CANCELLING")
    private String currentStatus;

    @ApiModelProperty(value = "作废原因", example = "货物质量问题")
    private String cancelReason;

    @ApiModelProperty(value = "作废类型", example = "QUALITY_ISSUE")
    private String cancelType;

    @ApiModelProperty(value = "申请人ID", example = "user-uuid-001")
    private String applicantId;

    @ApiModelProperty(value = "申请人姓名", example = "张三")
    private String applicantName;

    @ApiModelProperty(value = "申请时间", example = "2026-02-02T10:00:00")
    private LocalDateTime applicationTime;

    @ApiModelProperty(value = "是否成功", example = "true")
    private Boolean success;

    @ApiModelProperty(value = "消息", example = "作废申请提交成功，等待审核")
    private String message;

    /**
     * 创建成功响应
     */
    public static CancelApplicationResponse success(
            String applicationId,
            String receiptId,
            String receiptNo,
            String previousStatus,
            String currentStatus,
            String cancelReason,
            String cancelType,
            String applicantId,
            String applicantName) {
        CancelApplicationResponse response = new CancelApplicationResponse();
        response.setApplicationId(applicationId);
        response.setReceiptId(receiptId);
        response.setReceiptNo(receiptNo);
        response.setPreviousStatus(previousStatus);
        response.setCurrentStatus(currentStatus);
        response.setCancelReason(cancelReason);
        response.setCancelType(cancelType);
        response.setApplicantId(applicantId);
        response.setApplicantName(applicantName);
        response.setApplicationTime(LocalDateTime.now());
        response.setSuccess(true);
        response.setMessage("作废申请提交成功，等待审核");
        return response;
    }
}
