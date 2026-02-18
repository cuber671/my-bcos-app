package com.fisco.app.dto.pledge;

import com.fisco.app.entity.pledge.PledgeApplication;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 质押申请响应DTO
 */
@Data
@ApiModel(value = "质押申请响应", description = "质押申请的详细信息")
public class PledgeApplicationResponse {

    @ApiModelProperty(value = "申请ID", example = "1")
    private Long id;

    @ApiModelProperty(value = "申请编号", example = "PLG202601270001")
    private String applicationNo;

    @ApiModelProperty(value = "仓单ID")
    private String receiptId;

    @ApiModelProperty(value = "仓单编号")
    private String receiptNo;

    @ApiModelProperty(value = "货主企业ID")
    private String ownerId;

    @ApiModelProperty(value = "货主企业名称")
    private String ownerName;

    @ApiModelProperty(value = "金融机构ID")
    private String financialInstitutionId;

    @ApiModelProperty(value = "金融机构名称")
    private String financialInstitutionName;

    @ApiModelProperty(value = "质押金额（元）", example = "100000.00")
    private BigDecimal pledgeAmount;

    @ApiModelProperty(value = "质押率", example = "0.70")
    private BigDecimal pledgeRatio;

    @ApiModelProperty(value = "仓单总价值", example = "150000.00")
    private BigDecimal receiptValue;

    @ApiModelProperty(value = "质押开始日期", example = "2026-01-27")
    private LocalDate pledgeStartDate;

    @ApiModelProperty(value = "质押结束日期", example = "2026-04-27")
    private LocalDate pledgeEndDate;

    @ApiModelProperty(value = "实际批准金额（元）", example = "100000.00")
    private BigDecimal approvedAmount;

    @ApiModelProperty(value = "年化利率（%）", example = "5.50")
    private BigDecimal interestRate;

    @ApiModelProperty(value = "申请状态", notes = "PENDING-待审核, APPROVED-已批准, REJECTED-已拒绝, RELEASED-已释放")
    private PledgeApplication.ApplicationStatus status;

    @ApiModelProperty(value = "状态描述", example = "待审核")
    private String statusDesc;

    @ApiModelProperty(value = "申请时间")
    private LocalDateTime applyTime;

    @ApiModelProperty(value = "审核时间")
    private LocalDateTime approvalTime;

    @ApiModelProperty(value = "审核人ID")
    private String approverId;

    @ApiModelProperty(value = "审核人姓名")
    private String approverName;

    @ApiModelProperty(value = "拒绝原因")
    private String rejectionReason;

    @ApiModelProperty(value = "区块链交易哈希")
    private String txHash;

    @ApiModelProperty(value = "区块号")
    private Long blockNumber;

    @ApiModelProperty(value = "上链时间")
    private LocalDateTime blockchainTime;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updatedAt;

    /**
     * 从实体转换为响应DTO
     */
    public static PledgeApplicationResponse fromEntity(PledgeApplication entity) {
        if (entity == null) {
            return null;
        }

        PledgeApplicationResponse response = new PledgeApplicationResponse();
        response.setId(entity.getId());
        response.setApplicationNo(entity.getApplicationNo());
        response.setReceiptId(entity.getReceiptId());
        response.setReceiptNo(entity.getReceiptNo());
        response.setOwnerId(entity.getOwnerId());
        response.setOwnerName(entity.getOwnerName());
        response.setFinancialInstitutionId(entity.getFinancialInstitutionId());
        response.setFinancialInstitutionName(entity.getFinancialInstitutionName());
        response.setPledgeAmount(entity.getPledgeAmount());
        response.setPledgeRatio(entity.getPledgeRatio());
        response.setReceiptValue(entity.getReceiptValue());
        response.setPledgeStartDate(entity.getPledgeStartDate());
        response.setPledgeEndDate(entity.getPledgeEndDate());
        response.setApprovedAmount(entity.getApprovedAmount());
        response.setInterestRate(entity.getInterestRate());
        response.setStatus(entity.getStatus());
        response.setStatusDesc(getStatusDescription(entity.getStatus()));
        response.setApplyTime(entity.getApplyTime());
        response.setApprovalTime(entity.getApprovalTime());
        response.setApproverId(entity.getApproverId());
        response.setApproverName(entity.getApproverName());
        response.setRejectionReason(entity.getRejectionReason());
        response.setTxHash(entity.getTxHash());
        response.setBlockNumber(entity.getBlockNumber());
        response.setBlockchainTime(entity.getBlockchainTime());
        response.setRemark(entity.getRemark());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());

        return response;
    }

    /**
     * 获取状态描述
     */
    private static String getStatusDescription(PledgeApplication.ApplicationStatus status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case PENDING:
                return "待审核";
            case APPROVED:
                return "已批准";
            case REJECTED:
                return "已拒绝";
            case RELEASED:
                return "已释放";
            default:
                return "未知";
        }
    }
}
