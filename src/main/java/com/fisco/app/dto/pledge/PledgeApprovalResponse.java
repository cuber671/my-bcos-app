package com.fisco.app.dto.pledge;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 质押审核响应DTO
 */
@Data
@Builder
@AllArgsConstructor
@ApiModel(value = "质押审核响应", description = "质押审核结果")
public class PledgeApprovalResponse {

    @ApiModelProperty(value = "申请ID", example = "1")
    private Long applicationId;

    @ApiModelProperty(value = "申请编号", example = "PLG202601270001")
    private String applicationNo;

    @ApiModelProperty(value = "审核结果", notes = "APPROVED-批准, REJECTED-拒绝")
    private String approvalResult;

    @ApiModelProperty(value = "审核结果描述", example = "批准")
    private String approvalResultDesc;

    @ApiModelProperty(value = "实际批准金额（元）", example = "100000.00")
    private BigDecimal approvedAmount;

    @ApiModelProperty(value = "年化利率（%）", example = "5.50")
    private BigDecimal interestRate;

    @ApiModelProperty(value = "应还金额（本金+利息）", example = "101375.00")
    private BigDecimal repaymentAmount;

    @ApiModelProperty(value = "区块链交易哈希", notes = "上链成功时返回")
    private String txHash;

    @ApiModelProperty(value = "区块号", notes = "上链成功时返回")
    private Long blockNumber;

    @ApiModelProperty(value = "仓单ID")
    private String receiptId;

    @ApiModelProperty(value = "仓单状态", notes = "PLEDGED-已质押")
    private String receiptStatus;

    @ApiModelProperty(value = "原持有人地址")
    private String previousHolderAddress;

    @ApiModelProperty(value = "新持有人地址（金融机构）")
    private String currentHolderAddress;

    @ApiModelProperty(value = "审核时间")
    private LocalDateTime approvalTime;

    @ApiModelProperty(value = "审核人ID")
    private String approverId;

    @ApiModelProperty(value = "审核人姓名")
    private String approverName;

    @ApiModelProperty(value = "拒绝原因", notes = "拒绝时返回")
    private String rejectionReason;

    @ApiModelProperty(value = "审核意见")
    private String remark;

    @ApiModelProperty(value = "消息", example = "质押申请审核成功")
    private String message;
}
