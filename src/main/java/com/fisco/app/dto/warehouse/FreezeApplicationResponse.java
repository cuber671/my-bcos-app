package com.fisco.app.dto.warehouse;

import com.fisco.app.entity.warehouse.ReceiptFreezeApplication;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 冻结申请响应DTO
 */
@Data
@ApiModel(value = "冻结申请响应", description = "冻结申请提交结果")
public class FreezeApplicationResponse {

    @ApiModelProperty(value = "申请ID", example = "app-uuid-001")
    private String applicationId;

    @ApiModelProperty(value = "仓单ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String receiptId;

    @ApiModelProperty(value = "仓单编号", example = "EWR20260126000001")
    private String receiptNo;

    @ApiModelProperty(value = "申请状态", example = "PENDING")
    private String requestStatus;

    @ApiModelProperty(value = "申请状态描述", example = "待审核")
    private String requestStatusDesc;

    @ApiModelProperty(value = "申请时间", example = "2026-01-27T10:00:00")
    private LocalDateTime applicationTime;

    @ApiModelProperty(value = "申请人", example = "张三（仓储方操作员）")
    private String applicantName;

    @ApiModelProperty(value = "是否成功", example = "true")
    private Boolean success;

    @ApiModelProperty(value = "消息", example = "冻结申请提交成功，等待管理员审核")
    private String message;

    /**
     * 创建申请提交成功的响应
     */
    public static FreezeApplicationResponse success(
            String applicationId,
            String receiptId,
            String receiptNo,
            String applicantName) {
        FreezeApplicationResponse response = new FreezeApplicationResponse();
        response.setApplicationId(applicationId);
        response.setReceiptId(receiptId);
        response.setReceiptNo(receiptNo);
        response.setRequestStatus("PENDING");
        response.setRequestStatusDesc("待审核");
        response.setApplicationTime(LocalDateTime.now());
        response.setApplicantName(applicantName);
        response.setSuccess(true);
        response.setMessage("冻结申请提交成功，等待管理员审核");
        return response;
    }

    /**
     * 从实体转换为响应DTO
     */
    public static FreezeApplicationResponse fromEntity(ReceiptFreezeApplication entity) {
        FreezeApplicationResponse response = new FreezeApplicationResponse();
        response.setApplicationId(entity.getId());
        response.setReceiptId(entity.getReceiptId());
        response.setReceiptNo(entity.getReceiptNo());
        response.setRequestStatus(entity.getRequestStatus());
        response.setRequestStatusDesc(getRequestStatusDesc(entity.getRequestStatus()));
        response.setApplicationTime(entity.getCreatedAt());
        response.setApplicantName(entity.getApplicantName());
        response.setSuccess(true);
        response.setMessage("查询成功");
        return response;
    }

    private static String getRequestStatusDesc(String status) {
        switch (status) {
            case "PENDING":
                return "待审核";
            case "APPROVED":
                return "已批准";
            case "REJECTED":
                return "已拒绝";
            case "CANCELLED":
                return "已撤销";
            default:
                return "未知";
        }
    }
}
