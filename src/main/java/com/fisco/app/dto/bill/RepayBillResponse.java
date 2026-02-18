package com.fisco.app.dto.bill;

import com.fisco.app.entity.bill.RepaymentRecord;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 票据还款响应DTO
 */
@Data
@ApiModel(value = "票据还款响应", description = "票据还款操作的结果")
@Schema(name = "票据还款响应")
public class RepayBillResponse {

    @ApiModelProperty(value = "还款记录ID")
    private String id;

    @ApiModelProperty(value = "票据ID")
    private String billId;

    @ApiModelProperty(value = "还款人地址")
    private String payerAddress;

    @ApiModelProperty(value = "金融机构地址")
    private String financialInstitutionAddress;

    @ApiModelProperty(value = "票面金额")
    private BigDecimal billAmount;

    @ApiModelProperty(value = "贴现金额")
    private BigDecimal discountAmount;

    @ApiModelProperty(value = "还款金额")
    private BigDecimal paymentAmount;

    @ApiModelProperty(value = "还款类型")
    private RepaymentRecord.PaymentType paymentType;

    @ApiModelProperty(value = "本金金额")
    private BigDecimal principalAmount;

    @ApiModelProperty(value = "利息金额")
    private BigDecimal interestAmount;

    @ApiModelProperty(value = "逾期利息")
    private BigDecimal penaltyInterestAmount;

    @ApiModelProperty(value = "逾期天数")
    private Integer overdueDays;

    @ApiModelProperty(value = "还款日期")
    private LocalDateTime paymentDate;

    @ApiModelProperty(value = "到期日期")
    private LocalDateTime dueDate;

    @ApiModelProperty(value = "还款状态")
    private RepaymentRecord.PaymentStatus status;

    @ApiModelProperty(value = "区块链交易哈希")
    private String txHash;

    @ApiModelProperty(value = "还款备注")
    private String remark;
}
