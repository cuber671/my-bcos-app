package com.fisco.app.dto.bill;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 票据融资申请响应DTO
 */
@Data
@ApiModel(value = "票据融资申请响应", description = "票据融资申请操作的结果")
public class FinanceApplicationResponse {

    @ApiModelProperty(value = "融资申请ID")
    private String id;

    @ApiModelProperty(value = "票据ID")
    private String billId;

    @ApiModelProperty(value = "票据编号")
    private String billNo;

    @ApiModelProperty(value = "票面金额")
    private BigDecimal billFaceValue;

    @ApiModelProperty(value = "申请人ID")
    private String applicantId;

    @ApiModelProperty(value = "申请人名称")
    private String applicantName;

    @ApiModelProperty(value = "金融机构ID")
    private String financialInstitutionId;

    @ApiModelProperty(value = "金融机构名称")
    private String financialInstitutionName;

    @ApiModelProperty(value = "融资金额")
    private BigDecimal financeAmount;

    @ApiModelProperty(value = "融资利率（%）")
    private BigDecimal financeRate;

    @ApiModelProperty(value = "融资期限（天）")
    private Integer financePeriod;

    @ApiModelProperty(value = "批准金额")
    private BigDecimal approvedAmount;

    @ApiModelProperty(value = "批准利率（%）")
    private BigDecimal approvedRate;

    @ApiModelProperty(value = "实际放款金额")
    private BigDecimal actualAmount;

    @ApiModelProperty(value = "申请状态")
    private String status;

    @ApiModelProperty(value = "申请日期")
    private LocalDateTime applyDate;

    @ApiModelProperty(value = "审核日期")
    private LocalDateTime approveDate;

    @ApiModelProperty(value = "审核意见")
    private String approvalComments;

    @ApiModelProperty(value = "拒绝原因")
    private String rejectionReason;

    @ApiModelProperty(value = "放款日期")
    private LocalDateTime disbursementDate;

    @ApiModelProperty(value = "还款日期")
    private LocalDateTime repaymentDate;

    @ApiModelProperty(value = "区块链交易哈希")
    private String txHash;
}
