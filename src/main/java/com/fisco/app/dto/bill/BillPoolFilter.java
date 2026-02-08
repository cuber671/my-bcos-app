package com.fisco.app.dto.bill;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;


/**
 * 票据池筛选条件DTO
 * 用于查询票据池时的筛选条件
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-03
 */
@Data
@ApiModel(value = "票据池筛选条件", description = "查询票据池的筛选参数")
public class BillPoolFilter {

    // ==================== 基础筛选 ====================

    @ApiModelProperty(value = "票据类型", notes = "BANK_ACCEPTANCE_BILL-银行承兑汇票, COMMERCIAL_ACCEPTANCE_BILL-商业承兑汇票")
    private String billType;

    @ApiModelProperty(value = "最小面值")
    private BigDecimal minAmount;

    @ApiModelProperty(value = "最大面值")
    private BigDecimal maxAmount;

    @ApiModelProperty(value = "货币类型")
    private String currency;

    // ==================== 期限筛选 ====================

    @ApiModelProperty(value = "最小剩余天数", example = "30")
    private Integer minRemainingDays;

    @ApiModelProperty(value = "最大剩余天数", example = "180")
    private Integer maxRemainingDays;

    // ==================== 承兑人筛选 ====================

    @ApiModelProperty(value = "承兑人类型", notes = "BANK-银行, ENTERPRISE-企业")
    private String acceptorType;

    @ApiModelProperty(value = "最低承兑人评级", notes = "AAA, AA, A, BBB, BB, B, CCC")
    private String minRating;

    @ApiModelProperty(value = "承兑人名称（模糊查询）")
    private String acceptorName;

    // ==================== 风险筛选 ====================

    @ApiModelProperty(value = "风险等级", notes = "LOW-低风险, MEDIUM-中等风险, HIGH-高风险")
    private String riskLevel;

    @ApiModelProperty(value = "最大风险评分", notes = "0-100，只显示小于等于此分数的票据")
    private Integer maxRiskScore;

    // ==================== 收益筛选 ====================

    @ApiModelProperty(value = "最低收益率（%）")
    private BigDecimal minReturnRate;

    @ApiModelProperty(value = "最高收益率（%）")
    private BigDecimal maxReturnRate;

    // ==================== 持票人筛选 ====================

    @ApiModelProperty(value = "持票人类型", notes = "ENTERPRISE-企业, FINANCIAL_INSTITUTION-金融机构")
    private String holderType;

    @ApiModelProperty(value = "持票人ID（查询特定持票人的票据）")
    private String holderId;

    // ==================== 分页和排序 ====================

    @ApiModelProperty(value = "页码（从0开始）", example = "0")
    private Integer page = 0;

    @ApiModelProperty(value = "每页大小", example = "20")
    private Integer size = 20;

    @ApiModelProperty(value = "排序字段", notes = "remainingDays-剩余天数, faceValue-面值, expectedReturn-收益率, riskScore-风险评分")
    private String sortBy = "remainingDays";

    @ApiModelProperty(value = "排序方向", notes = "ASC-升序, DESC-降序")
    private String sortOrder = "ASC";
}
