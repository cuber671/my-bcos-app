package com.fisco.app.dto.receivable;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;


/**
 * 仓单拆分申请响应DTO
 */
@Data
@ApiModel(value = "仓单拆分申请响应", description = "拆分申请提交结果")
public class SplitApplicationResponse {

    @ApiModelProperty(value = "申请ID", example = "app-uuid-001")
    private String applicationId;

    @ApiModelProperty(value = "父仓单ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String parentReceiptId;

    @ApiModelProperty(value = "父仓单编号", example = "EWR20260126000001")
    private String parentReceiptNo;

    @ApiModelProperty(value = "申请状态", example = "PENDING")
    private String requestStatus;

    @ApiModelProperty(value = "申请状态描述", example = "待审核")
    private String requestStatusDesc;

    @ApiModelProperty(value = "拆分原因", example = "部分货物用于质押融资")
    private String splitReason;

    @ApiModelProperty(value = "子仓单数量", example = "2")
    private Integer splitCount;

    @ApiModelProperty(value = "申请人ID", example = "user-uuid-001")
    private String applicantId;

    @ApiModelProperty(value = "申请人姓名", example = "张三")
    private String applicantName;

    @ApiModelProperty(value = "申请时间", example = "2026-02-02T10:30:00")
    private String applicationTime;

    @ApiModelProperty(value = "消息", example = "拆分申请提交成功，等待审核")
    private String message;

    /**
     * 创建成功的响应
     */
    public static SplitApplicationResponse success(
            String applicationId,
            String parentReceiptId,
            String parentReceiptNo,
            String splitReason,
            Integer splitCount,
            String applicantId,
            String applicantName) {
        SplitApplicationResponse response = new SplitApplicationResponse();
        response.setApplicationId(applicationId);
        response.setParentReceiptId(parentReceiptId);
        response.setParentReceiptNo(parentReceiptNo);
        response.setRequestStatus("PENDING");
        response.setRequestStatusDesc("待审核");
        response.setSplitReason(splitReason);
        response.setSplitCount(splitCount);
        response.setApplicantId(applicantId);
        response.setApplicantName(applicantName);
        response.setApplicationTime(java.time.LocalDateTime.now().toString());
        response.setMessage("拆分申请提交成功，等待审核");
        return response;
    }
}
