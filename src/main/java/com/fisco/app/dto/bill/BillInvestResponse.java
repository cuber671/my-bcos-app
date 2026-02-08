package com.fisco.app.dto.bill;


import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 票据投资响应DTO
 * 用于返回票据投资操作的结果
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-03
 */
@Data
@ApiModel(value = "票据投资响应", description = "票据投资操作的结果")
public class BillInvestResponse {

    // ==================== 投资记录信息 ====================

    @ApiModelProperty(value = "投资记录ID")
    private String investmentId;

    @ApiModelProperty(value = "票据ID")
    private String billId;

    @ApiModelProperty(value = "票据编号")
    private String billNo;

    // ==================== 投资详情 ====================

    @ApiModelProperty(value = "投资金额（实际支付）")
    private BigDecimal investAmount;

    @ApiModelProperty(value = "投资利率（%）")
    private BigDecimal investRate;

    @ApiModelProperty(value = "预期收益")
    private BigDecimal expectedReturn;

    @ApiModelProperty(value = "投资天数（票据剩余天数）")
    private Integer investmentDays;

    @ApiModelProperty(value = "到期金额（票据面值）")
    private BigDecimal maturityAmount;

    // ==================== 状态信息 ====================

    @ApiModelProperty(value = "投资状态", notes = "PENDING-待确认, CONFIRMED-已确认, COMPLETED-已完成, CANCELLED-已取消, FAILED-失败")
    private String status;

    @ApiModelProperty(value = "投资时间")
    private LocalDateTime investmentDate;

    @ApiModelProperty(value = "确认时间")
    private LocalDateTime confirmationDate;

    @ApiModelProperty(value = "完成时间")
    private LocalDateTime completionDate;

    // ==================== 转让信息 ====================

    @ApiModelProperty(value = "原持票人名称")
    private String originalHolderName;

    @ApiModelProperty(value = "投资机构名称")
    private String investorName;

    // ==================== 收益信息 ====================

    @ApiModelProperty(value = "实际收益（已结算时）")
    private BigDecimal actualReturn;

    @ApiModelProperty(value = "结算时间")
    private LocalDateTime settlementDate;

    // ==================== 区块链信息 ====================

    @ApiModelProperty(value = "关联的背书ID")
    private String endorsementId;

    @ApiModelProperty(value = "区块链交易哈希")
    private String txHash;

    @ApiModelProperty(value = "区块链确认时间")
    private LocalDateTime blockchainTime;

    // ==================== 备注信息 ====================

    @ApiModelProperty(value = "投资备注")
    private String investmentNotes;

    @ApiModelProperty(value = "拒绝原因（如果被拒绝）")
    private String rejectionReason;
}
