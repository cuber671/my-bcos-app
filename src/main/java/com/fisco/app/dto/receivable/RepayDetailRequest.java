package com.fisco.app.dto.receivable;

import com.fisco.app.entity.receivable.ReceivableRepaymentRecord;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 应收账款还款请求DTO（详细版）
 *
 * 支持部分还款、提前还款、逾期还款等场景，记录完整的还款信息
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-09
 * @version 1.0
 */
@Data
@ApiModel(value = "应收账款还款请求", description = "应收账款还款详细请求参数")
public class RepayDetailRequest {

    @ApiModelProperty(value = "应收账款ID", required = true, example = "REC20240113001")
    @NotBlank(message = "应收账款ID不能为空")
    @Size(max = 64, message = "应收账款ID长度不能超过64")
    private String receivableId;

    @ApiModelProperty(value = "还款类型", required = true, notes = "PARTIAL-部分还款, FULL-全额还款, EARLY-提前还款, OVERDUE-逾期还款", example = "PARTIAL")
    @NotNull(message = "还款类型不能为空")
    private ReceivableRepaymentRecord.RepaymentType repaymentType;

    @ApiModelProperty(value = "还款总金额", required = true, example = "500000.00")
    @NotNull(message = "还款金额不能为空")
    @DecimalMin(value = "0.01", message = "还款金额必须大于0")
    @Digits(integer = 18, fraction = 2, message = "还款金额格式不正确")
    private BigDecimal repaymentAmount;

    @ApiModelProperty(value = "本金金额", required = true, example = "500000.00")
    @NotNull(message = "本金金额不能为空")
    @DecimalMin(value = "0.01", message = "本金金额必须大于0")
    @Digits(integer = 18, fraction = 2, message = "本金金额格式不正确")
    private BigDecimal principalAmount;

    @ApiModelProperty(value = "利息金额", example = "5000.00")
    @DecimalMin(value = "0", message = "利息金额不能为负数")
    @Digits(integer = 18, fraction = 2, message = "利息金额格式不正确")
    private BigDecimal interestAmount = BigDecimal.ZERO;

    @ApiModelProperty(value = "罚息金额", example = "1250.00")
    @DecimalMin(value = "0", message = "罚息金额不能为负数")
    @Digits(integer = 18, fraction = 2, message = "罚息金额格式不正确")
    private BigDecimal penaltyAmount = BigDecimal.ZERO;

    @ApiModelProperty(value = "支付方式", notes = "BANK-银行转账, ALIPAY-支付宝, WECHAT-微信, OTHER-其他", example = "BANK")
    @Pattern(regexp = "^(BANK|ALIPAY|WECHAT|OTHER)$", message = "支付方式不正确")
    private String paymentMethod;

    @ApiModelProperty(value = "支付账号", example = "6222021234567890")
    @Size(max = 100, message = "支付账号长度不能超过100")
    private String paymentAccount;

    @ApiModelProperty(value = "交易流水号", example = "TXN202602091234567890")
    @Size(max = 64, message = "交易流水号长度不能超过64")
    private String transactionNo;

    @ApiModelProperty(value = "凭证URL", example = "https://example.com/voucher/abc123.pdf")
    @Size(max = 500, message = "凭证URL长度不能超过500")
    private String voucherUrl;

    @ApiModelProperty(value = "还款日期", required = true, example = "2026-02-09")
    @NotNull(message = "还款日期不能为空")
    private LocalDate paymentDate;

    @ApiModelProperty(value = "备注", example = "第一期还款")
    @Size(max = 1000, message = "备注长度不能超过1000")
    private String remark;
}
