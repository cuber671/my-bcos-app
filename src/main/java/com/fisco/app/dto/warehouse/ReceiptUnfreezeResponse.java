package com.fisco.app.dto.warehouse;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.time.LocalDateTime;

/**
 * 仓单解冻响应DTO
 */
@Data
@ApiModel(value = "仓单解冻响应", description = "仓单解冻操作结果")
public class ReceiptUnfreezeResponse {

    @ApiModelProperty(value = "仓单ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String receiptId;

    @ApiModelProperty(value = "仓单编号", example = "EWR20260126000001")
    private String receiptNo;

    @ApiModelProperty(value = "解冻前状态", example = "FROZEN")
    private String previousStatus;

    @ApiModelProperty(value = "解冻后状态", example = "NORMAL")
    private String currentStatus;

    @ApiModelProperty(value = "解冻后状态描述", example = "正常")
    private String statusDesc;

    @ApiModelProperty(value = "解冻原因", example = "法律纠纷已解决，根据法院解冻通知书解冻")
    private String unfreezeReason;

    @ApiModelProperty(value = "相关文件编号", example = "法院解冻通知书编号：[2026]沪01执123-1号")
    private String referenceNo;

    @ApiModelProperty(value = "解冻时间", example = "2026-01-27T16:30:00")
    private LocalDateTime unfreezeTime;

    @ApiModelProperty(value = "操作人", example = "李四（仓储方主管）")
    private String operatorName;

    @ApiModelProperty(value = "是否成功", example = "true")
    private Boolean success;

    @ApiModelProperty(value = "消息", example = "仓单解冻成功")
    private String message;

    /**
     * 创建解冻成功的响应
     */
    public static ReceiptUnfreezeResponse success(
            String receiptId,
            String receiptNo,
            String targetStatus,
            String unfreezeReason,
            String referenceNo,
            String operatorName) {
        ReceiptUnfreezeResponse response = new ReceiptUnfreezeResponse();
        response.setReceiptId(receiptId);
        response.setReceiptNo(receiptNo);
        response.setPreviousStatus("FROZEN");
        response.setCurrentStatus(targetStatus);
        response.setStatusDesc(getStatusDesc(targetStatus));
        response.setUnfreezeReason(unfreezeReason);
        response.setReferenceNo(referenceNo);
        response.setUnfreezeTime(LocalDateTime.now());
        response.setOperatorName(operatorName);
        response.setSuccess(true);
        response.setMessage("仓单解冻成功");
        return response;
    }

    /**
     * 获取状态描述
     */
    private static String getStatusDesc(String status) {
        switch (status) {
            case "NORMAL":
                return "正常";
            case "PLEDGED":
                return "已质押";
            case "TRANSFERRED":
                return "已转让";
            default:
                return "未知";
        }
    }
}
