package com.fisco.app.dto.receivable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 还款记录响应DTO
 *
 * 返回还款记录的详细信息
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-09
 * @version 1.0
 */
@Data
@ApiModel(value = "还款记录响应", description = "还款记录详细信息")
public class RepaymentRecordResponse {

    @ApiModelProperty(value = "记录ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    @ApiModelProperty(value = "应收账款ID", example = "REC20240113001")
    private String receivableId;

    @ApiModelProperty(value = "还款类型", example = "PARTIAL")
    private String repaymentType;

    @ApiModelProperty(value = "还款类型名称", example = "部分还款")
    private String repaymentTypeName;

    @ApiModelProperty(value = "还款总金额", example = "500000.00")
    private BigDecimal repaymentAmount;

    @ApiModelProperty(value = "本金金额", example = "500000.00")
    private BigDecimal principalAmount;

    @ApiModelProperty(value = "利息金额", example = "5000.00")
    private BigDecimal interestAmount;

    @ApiModelProperty(value = "罚息金额", example = "1250.00")
    private BigDecimal penaltyAmount;

    @ApiModelProperty(value = "还款人地址", example = "0xabcdef1234567890")
    private String payerAddress;

    @ApiModelProperty(value = "收款人地址", example = "0x1234567890abcdef")
    private String receiverAddress;

    @ApiModelProperty(value = "还款日期", example = "2026-02-09")
    private LocalDate paymentDate;

    @ApiModelProperty(value = "实际还款时间", example = "2026-02-09T14:30:00")
    private LocalDateTime actualPaymentTime;

    @ApiModelProperty(value = "支付方式", example = "BANK")
    private String paymentMethod;

    @ApiModelProperty(value = "支付方式名称", example = "银行转账")
    private String paymentMethodName;

    @ApiModelProperty(value = "支付账号", example = "6222021234567890")
    private String paymentAccount;

    @ApiModelProperty(value = "交易流水号", example = "TXN202602091234567890")
    private String transactionNo;

    @ApiModelProperty(value = "凭证URL", example = "https://example.com/voucher/abc123.pdf")
    private String voucherUrl;

    @ApiModelProperty(value = "提前还款天数", example = "15")
    private Integer earlyPaymentDays;

    @ApiModelProperty(value = "逾期天数", example = "30")
    private Integer overdueDays;

    @ApiModelProperty(value = "备注", example = "第一期还款")
    private String remark;

    @ApiModelProperty(value = "状态", example = "CONFIRMED")
    private String status;

    @ApiModelProperty(value = "状态名称", example = "已确认")
    private String statusName;

    @ApiModelProperty(value = "区块链交易哈希", example = "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef12")
    private String txHash;

    @ApiModelProperty(value = "创建时间", example = "2026-02-09T14:30:00")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "创建人地址", example = "0xabcdef1234567890")
    private String createdBy;
}
