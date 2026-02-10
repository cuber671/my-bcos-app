package com.fisco.app.dto.bill;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 票据承兑响应DTO
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-09
 * @version 1.0
 */
@Data
@ApiModel(value = "票据承兑响应", description = "票据承兑操作结果")
public class BillAcceptanceResponse {

    @ApiModelProperty(value = "票据ID", example = "b1a2b3c4-d5e6-7890-abcd-ef1234567890")
    private String billId;

    @ApiModelProperty(value = "票据编号", example = "BIL20260200000001")
    private String billNo;

    @ApiModelProperty(value = "承兑类型", example = "FULL_ACCEPTANCE")
    private AcceptBillRequest.AcceptanceType acceptanceType;

    @ApiModelProperty(value = "承兑金额（分）", example = "100000000")
    private Long acceptanceAmount;

    @ApiModelProperty(value = "承兑时间", example = "2026-02-09T10:00:00")
    private LocalDateTime acceptanceTime;

    @ApiModelProperty(value = "承兑备注", example = "确认承兑该票据")
    private String acceptanceRemarks;

    @ApiModelProperty(value = "承兑人地址", example = "0x1234567890abcdef1234567890abcdef12345678")
    private String acceptorAddress;

    @ApiModelProperty(value = "区块链交易哈希", example = "0xabcdef...")
    private String txHash;

    @ApiModelProperty(value = "操作结果", example = "success")
    private String result;

    @ApiModelProperty(value = "消息", example = "票据承兑成功")
    private String message;
}
