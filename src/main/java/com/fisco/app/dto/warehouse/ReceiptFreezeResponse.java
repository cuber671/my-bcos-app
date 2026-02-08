package com.fisco.app.dto.warehouse;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.time.LocalDateTime;

/**
 * 仓单冻结响应DTO
 */
@Data
@ApiModel(value = "仓单冻结响应", description = "仓单冻结操作结果")
public class ReceiptFreezeResponse {

    @ApiModelProperty(value = "仓单ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String receiptId;

    @ApiModelProperty(value = "仓单编号", example = "EWR20260126000001")
    private String receiptNo;

    @ApiModelProperty(value = "冻结前状态", example = "NORMAL")
    private String previousStatus;

    @ApiModelProperty(value = "冻结后状态", example = "FROZEN")
    private String currentStatus;

    @ApiModelProperty(value = "冻结状态描述", example = "已冻结")
    private String statusDesc;

    @ApiModelProperty(value = "操作方类型", example = "WAREHOUSE")
    private String operatorType;

    @ApiModelProperty(value = "操作方类型描述", example = "仓储方")
    private String operatorTypeDesc;

    @ApiModelProperty(value = "冻结原因", example = "涉及法律纠纷，根据法院要求冻结")
    private String freezeReason;

    @ApiModelProperty(value = "冻结类型", example = "LEGAL")
    private String freezeType;

    @ApiModelProperty(value = "冻结类型描述", example = "法律冻结")
    private String freezeTypeDesc;

    @ApiModelProperty(value = "相关文件编号", example = "法院裁定书编号：[2026]沪01执123号")
    private String referenceNo;

    @ApiModelProperty(value = "冻结时间", example = "2026-01-27T15:30:00")
    private LocalDateTime freezeTime;

    @ApiModelProperty(value = "操作人", example = "张三（仓储方操作员）")
    private String operatorName;

    @ApiModelProperty(value = "是否成功", example = "true")
    private Boolean success;

    @ApiModelProperty(value = "消息", example = "仓单冻结成功")
    private String message;

    /**
     * 创建冻结成功的响应
     */
    public static ReceiptFreezeResponse success(
            String receiptId,
            String receiptNo,
            String previousStatus,
            String operatorType,
            String freezeReason,
            String freezeType,
            String referenceNo,
            String operatorName) {
        ReceiptFreezeResponse response = new ReceiptFreezeResponse();
        response.setReceiptId(receiptId);
        response.setReceiptNo(receiptNo);
        response.setPreviousStatus(previousStatus);
        response.setCurrentStatus("FROZEN");
        response.setStatusDesc("已冻结");
        response.setOperatorType(operatorType);
        response.setOperatorTypeDesc(getOperatorTypeDesc(operatorType));
        response.setFreezeReason(freezeReason);
        response.setFreezeType(freezeType);
        response.setFreezeTypeDesc(getFreezeTypeDesc(freezeType));
        response.setReferenceNo(referenceNo);
        response.setFreezeTime(LocalDateTime.now());
        response.setOperatorName(operatorName);
        response.setSuccess(true);
        response.setMessage("仓单冻结成功");
        return response;
    }

    /**
     * 获取操作方类型描述
     */
    private static String getOperatorTypeDesc(String operatorType) {
        switch (operatorType) {
            case "WAREHOUSE":
                return "仓储方";
            case "FINANCIER":
                return "资金方";
            case "PLATFORM":
                return "平台方";
            case "COURT":
                return "法院/司法";
            default:
                return "未知";
        }
    }

    /**
     * 获取冻结类型描述
     */
    private static String getFreezeTypeDesc(String freezeType) {
        switch (freezeType) {
            case "LEGAL":
                return "法律冻结";
            case "BUSINESS":
                return "业务冻结";
            case "RISK":
                return "风险冻结";
            default:
                return "未知";
        }
    }
}
