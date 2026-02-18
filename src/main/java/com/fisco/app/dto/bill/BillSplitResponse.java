package com.fisco.app.dto.bill;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 票据拆分响应DTO
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-09
 * @version 1.0
 */
@Data
@ApiModel(value = "票据拆分响应", description = "票据拆分操作结果")
public class BillSplitResponse {

    @ApiModelProperty(value = "拆分申请ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String applicationId;

    @ApiModelProperty(value = "父票据ID", example = "b1a2b3c4-d5e6-7890-abcd-ef1234567890")
    private String parentBillId;

    @ApiModelProperty(value = "父票据编号", example = "BIL20260200000001")
    private String parentBillNo;

    @ApiModelProperty(value = "拆分方案", example = "EQUAL")
    private SplitBillRequest.SplitScheme splitScheme;

    @ApiModelProperty(value = "拆分数量", example = "3")
    private Integer splitCount;

    @ApiModelProperty(value = "子票据列表")
    private List<ChildBillInfo> childBills;

    @ApiModelProperty(value = "拆分时间", example = "2026-02-09T10:00:00")
    private LocalDateTime splitTime;

    @ApiModelProperty(value = "区块链交易哈希", example = "0xabcdef...")
    private String txHash;

    @ApiModelProperty(value = "操作结果", example = "success")
    private String result;

    @ApiModelProperty(value = "消息", example = "票据拆分成功")
    private String message;

    /**
     * 子票据信息DTO
     */
    @Data
    @ApiModel(value = "子票据信息", description = "拆分后的子票据信息")
    public static class ChildBillInfo {

        @ApiModelProperty(value = "子票据ID")
        private String billId;

        @ApiModelProperty(value = "子票据编号")
        private String billNo;

        @ApiModelProperty(value = "子票据金额（分）")
        private Long amount;

        @ApiModelProperty(value = "到期日期")
        private LocalDateTime dueDate;

        @ApiModelProperty(value = "当前持票人地址")
        private String currentHolder;
    }
}
