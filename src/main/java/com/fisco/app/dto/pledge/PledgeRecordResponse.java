package com.fisco.app.dto.pledge;

import com.fisco.app.entity.pledge.PledgeRecord;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 质押记录响应DTO
 */
@Data
@ApiModel(value = "质押记录响应", description = "仓单质押记录的详细信息")
public class PledgeRecordResponse {

    @ApiModelProperty(value = "质押记录ID", example = "1")
    private Long id;

    @ApiModelProperty(value = "仓单ID")
    private String receiptId;

    @ApiModelProperty(value = "仓单编号")
    private String receiptNo;

    @ApiModelProperty(value = "背书ID")
    private String endorsementId;

    @ApiModelProperty(value = "背书编号")
    private String endorsementNo;

    @ApiModelProperty(value = "质押申请ID（已废弃）")
    @Deprecated
    private Long applicationId;

    @ApiModelProperty(value = "申请编号（已废弃）")
    @Deprecated
    private String applicationNo;

    @ApiModelProperty(value = "货主企业ID")
    private String ownerId;

    @ApiModelProperty(value = "货主企业名称")
    private String ownerName;

    @ApiModelProperty(value = "金融机构ID")
    private String financialInstitutionId;

    @ApiModelProperty(value = "金融机构名称")
    private String financialInstitutionName;

    @ApiModelProperty(value = "金融机构区块链地址")
    private String financialInstitutionAddress;

    @ApiModelProperty(value = "质押前持有人地址")
    private String previousHolderAddress;

    @ApiModelProperty(value = "质押金额（元）", example = "100000.00")
    private BigDecimal pledgeAmount;

    @ApiModelProperty(value = "年化利率（%）", example = "5.50")
    private BigDecimal interestRate;

    @ApiModelProperty(value = "质押开始日期", example = "2026-01-27")
    private LocalDate pledgeStartDate;

    @ApiModelProperty(value = "质押结束日期", example = "2026-04-27")
    private LocalDate pledgeEndDate;

    @ApiModelProperty(value = "质押状态", notes = "ACTIVE-质押中, RELEASED-已释放, LIQUIDATED-已清算")
    private PledgeRecord.PledgeStatus status;

    @ApiModelProperty(value = "状态描述", example = "质押中")
    private String statusDesc;

    @ApiModelProperty(value = "质押时间")
    private LocalDateTime pledgeTime;

    @ApiModelProperty(value = "释放时间")
    private LocalDateTime releaseTime;

    @ApiModelProperty(value = "清算时间")
    private LocalDateTime liquidationTime;

    @ApiModelProperty(value = "质押上链交易哈希")
    private String txHash;

    @ApiModelProperty(value = "质押区块号")
    private Long blockNumber;

    @ApiModelProperty(value = "释放上链交易哈希")
    private String releaseTxHash;

    @ApiModelProperty(value = "释放区块号")
    private Long releaseBlockNumber;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updatedAt;

    /**
     * 从实体转换为响应DTO
     */
    public static PledgeRecordResponse fromEntity(PledgeRecord entity) {
        if (entity == null) {
            return null;
        }

        PledgeRecordResponse response = new PledgeRecordResponse();
        response.setId(entity.getId());
        response.setReceiptId(entity.getReceiptId());
        response.setReceiptNo(entity.getReceiptNo());
        response.setEndorsementId(entity.getEndorsementId());
        response.setEndorsementNo(entity.getEndorsementNo());
        response.setOwnerId(entity.getOwnerId());
        response.setOwnerName(entity.getOwnerName());
        response.setFinancialInstitutionId(entity.getFinancialInstitutionId());
        response.setFinancialInstitutionName(entity.getFinancialInstitutionName());
        response.setFinancialInstitutionAddress(entity.getFinancialInstitutionAddress());
        response.setPreviousHolderAddress(entity.getPreviousHolderAddress());
        response.setPledgeAmount(entity.getPledgeAmount());
        response.setInterestRate(entity.getInterestRate());
        response.setPledgeStartDate(entity.getPledgeStartDate());
        response.setPledgeEndDate(entity.getPledgeEndDate());
        response.setStatus(entity.getStatus());
        response.setStatusDesc(getStatusDescription(entity.getStatus()));
        response.setPledgeTime(entity.getPledgeTime());
        response.setReleaseTime(entity.getReleaseTime());
        response.setLiquidationTime(entity.getLiquidationTime());
        response.setTxHash(entity.getTxHash());
        response.setBlockNumber(entity.getBlockNumber());
        response.setReleaseTxHash(entity.getReleaseTxHash());
        response.setReleaseBlockNumber(entity.getReleaseBlockNumber());
        response.setRemark(entity.getRemark());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());

        return response;
    }

    /**
     * 获取状态描述
     */
    private static String getStatusDescription(PledgeRecord.PledgeStatus status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case ACTIVE:
                return "质押中";
            case RELEASED:
                return "已释放";
            case LIQUIDATED:
                return "已清算";
            default:
                return "未知";
        }
    }
}
