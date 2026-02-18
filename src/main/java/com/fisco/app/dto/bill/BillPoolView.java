package com.fisco.app.dto.bill;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;


/**
 * 票据池视图DTO
 * 用于展示票据池中的可投资票据信息
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-03
 */
@Data
@ApiModel(value = "票据池视图", description = "票据池中的票据信息")
public class BillPoolView {

    // ==================== 票据基础信息 ====================

    @ApiModelProperty(value = "票据ID")
    private String billId;

    @ApiModelProperty(value = "票据编号")
    private String billNo;

    @ApiModelProperty(value = "票据类型", notes = "BANK_ACCEPTANCE_BILL-银行承兑汇票, COMMERCIAL_ACCEPTANCE_BILL-商业承兑汇票")
    private String billType;

    @ApiModelProperty(value = "票据面值")
    private BigDecimal faceValue;

    @ApiModelProperty(value = "货币类型")
    private String currency;

    // ==================== 时间信息 ====================

    @ApiModelProperty(value = "剩余天数")
    private Integer remainingDays;

    @ApiModelProperty(value = "到期日期")
    private LocalDateTime maturityDate;

    @ApiModelProperty(value = "开票日期")
    private LocalDateTime issueDate;

    // ==================== 承兑人信息 ====================

    @ApiModelProperty(value = "承兑人名称")
    private String acceptorName;

    @ApiModelProperty(value = "承兑人评级", notes = "AAA, AA, A, BBB, BB, B, CCC")
    private String acceptorRating;

    @ApiModelProperty(value = "承兑人类型", notes = "BANK-银行, ENTERPRISE-企业")
    private String acceptorType;

    // ==================== 当前持票人 ====================

    @ApiModelProperty(value = "当前持票人名称")
    private String currentHolderName;

    @ApiModelProperty(value = "当前持票人类型", notes = "ENTERPRISE-企业, FINANCIAL_INSTITUTION-金融机构")
    private String currentHolderType;

    // ==================== 投资指标 ====================

    @ApiModelProperty(value = "预期收益率（%）")
    private BigDecimal expectedReturn;

    @ApiModelProperty(value = "风险评分", notes = "0-100，分数越低风险越低")
    private Integer riskScore;

    @ApiModelProperty(value = "风险等级", notes = "LOW-低风险, MEDIUM-中等风险, HIGH-高风险")
    private String riskLevel;

    @ApiModelProperty(value = "是否可投资")
    private Boolean canInvest;

    @ApiModelProperty(value = "投资建议", notes = "RECOMMENDED-推荐, CAUTION-谨慎, NOT_RECOMMENDED-不推荐")
    private String investmentAdvice;

    // ==================== 统计信息 ====================

    @ApiModelProperty(value = "浏览次数")
    private Integer viewCount;

    @ApiModelProperty(value = "询价次数")
    private Integer inquiryCount;

    @ApiModelProperty(value = "投资次数")
    private Integer investmentCount;

    // ==================== 区块链信息 ====================

    @ApiModelProperty(value = "是否已上链")
    private Boolean onChain;

    @ApiModelProperty(value = "区块链交易哈希")
    private String txHash;
}
