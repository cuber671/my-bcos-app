package com.fisco.app.dto.risk;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 风险评估响应DTO
 */
@Data
@ApiModel(value = "风险评估响应", description = "企业风险评估结果")
public class RiskAssessmentResponse {

    @ApiModelProperty(value = "企业地址", example = "0x1234567890abcdef")
    private String enterpriseAddress;

    @ApiModelProperty(value = "企业名称", example = "某某科技有限公司")
    private String enterpriseName;

    @ApiModelProperty(value = "评估时间", example = "2026-02-03T10:30:00")
    private LocalDateTime assessmentTime;

    @ApiModelProperty(value = "风险等级", example = "LOW")
    private RiskLevel riskLevel;

    @ApiModelProperty(value = "风险评分（0-100）", example = "85", notes = "分数越高风险越低")
    private Integer riskScore;

    @ApiModelProperty(value = "信用评分", example = "750")
    private Integer creditScore;

    // 逾期风险指标
    @ApiModelProperty(value = "逾期次数", example = "2")
    private Integer overdueCount;

    @ApiModelProperty(value = "逾期金额（分）", example = "10000000")
    private Long overdueAmount;

    @ApiModelProperty(value = "逾期率", example = "0.05")
    private BigDecimal overdueRate;

    @ApiModelProperty(value = "平均逾期天数", example = "45")
    private Double averageOverdueDays;

    // 财务指标
    @ApiModelProperty(value = "总负债（分）", example = "500000000")
    private Long totalLiability;

    @ApiModelProperty(value = "资产负债率", example = "0.6")
    private BigDecimal assetLiabilityRatio;

    @ApiModelProperty(value = "流动比率", example = "1.5")
    private BigDecimal currentRatio;

    // 交易行为指标
    @ApiModelProperty(value = "交易次数", example = "100")
    private Integer transactionCount;

    @ApiModelProperty(value = "交易成功率", example = "0.95")
    private BigDecimal transactionSuccessRate;

    @ApiModelProperty(value = "平均交易金额（分）", example = "5000000")
    private Long averageTransactionAmount;

    // 风险预警
    @ApiModelProperty(value = "风险预警数量", example = "3")
    private Integer warningCount;

    @ApiModelProperty(value = "风险预警列表")
    private java.util.List<RiskWarning> warnings;

    // 风险因素分析
    @ApiModelProperty(value = "风险因素权重分析", example = "{\"逾期\": 0.4, \"财务\": 0.3, \"交易\": 0.2, \"其他\": 0.1}")
    private Map<String, BigDecimal> riskFactors;

    @ApiModelProperty(value = "改进建议", example = "[\"降低逾期率\", \"增加流动资金\"]")
    private java.util.List<String> recommendations;

    /**
     * 风险等级枚举
     */
    public enum RiskLevel {
        VERY_LOW("极低风险", 90, 100),
        LOW("低风险", 75, 89),
        MEDIUM("中等风险", 60, 74),
        HIGH("高风险", 40, 59),
        VERY_HIGH("极高风险", 0, 39);

        private final String description;
        private final int minScore;
        private final int maxScore;

        RiskLevel(String description, int minScore, int maxScore) {
            this.description = description;
            this.minScore = minScore;
            this.maxScore = maxScore;
        }

        public String getDescription() {
            return description;
        }

        public static RiskLevel fromScore(int score) {
            for (RiskLevel level : values()) {
                if (score >= level.minScore && score <= level.maxScore) {
                    return level;
                }
            }
            return VERY_HIGH;
        }
    }

    /**
     * 风险预警
     */
    @Data
    @io.swagger.annotations.ApiModel(value = "风险预警")
    public static class RiskWarning {
        @ApiModelProperty(value = "预警类型", example = "OVERDUE")
        private String warningType;

        @ApiModelProperty(value = "预警等级", example = "HIGH")
        private String warningLevel;

        @ApiModelProperty(value = "预警信息", example = "逾期率超过阈值")
        private String warningMessage;

        @ApiModelProperty(value = "预警时间", example = "2026-02-03T10:30:00")
        private LocalDateTime warningTime;
    }
}
