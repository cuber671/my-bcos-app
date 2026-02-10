package com.fisco.app.dto.bill;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 票据合并响应DTO
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-09
 * @version 1.0
 */
@Data
@ApiModel(value = "票据合并响应", description = "票据合并操作结果")
public class BillMergeResponse {

    @ApiModelProperty(value = "合并申请ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String applicationId;

    @ApiModelProperty(value = "合并后的票据ID", example = "m1a2b3c4-d5e6-7890-abcd-ef1234567890")
    private String mergedBillId;

    @ApiModelProperty(value = "合并后的票据编号", example = "BIL20260200000099")
    private String mergedBillNo;

    @ApiModelProperty(value = "合并类型", example = "AMOUNT")
    private MergeBillsRequest.MergeType mergeType;

    @ApiModelProperty(value = "源票据列表")
    private List<SourceBillInfo> sourceBills;

    @ApiModelProperty(value = "合并后总金额（分）", example = "3000000")
    private Long totalAmount;

    @ApiModelProperty(value = "合并后到期日期", example = "2026-08-09T10:00:00")
    private LocalDateTime mergedDueDate;

    @ApiModelProperty(value = "合并时间", example = "2026-02-09T10:00:00")
    private LocalDateTime mergeTime;

    @ApiModelProperty(value = "区块链交易哈希", example = "0xabcdef...")
    private String txHash;

    @ApiModelProperty(value = "操作结果", example = "success")
    private String result;

    @ApiModelProperty(value = "消息", example = "票据合并成功")
    private String message;

    /**
     * 源票据信息DTO
     */
    @Data
    @ApiModel(value = "源票据信息", description = "被合并的源票据信息")
    public static class SourceBillInfo {

        @ApiModelProperty(value = "源票据ID")
        private String billId;

        @ApiModelProperty(value = "源票据编号")
        private String billNo;

        @ApiModelProperty(value = "源票据金额（分）")
        private Long amount;

        @ApiModelProperty(value = "源票据到期日期")
        private LocalDateTime dueDate;
    }
}
