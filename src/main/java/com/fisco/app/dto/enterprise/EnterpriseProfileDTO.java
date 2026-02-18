package com.fisco.app.dto.enterprise;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 企业画像DTO
 * 包含企业的基本信息、交易习惯、经营状况和风险指标
 */
@Data
@ApiModel(value = "企业画像", description = "企业全方位画像信息")
public class EnterpriseProfileDTO {

    // ==================== 基本信息 ====================

    @ApiModelProperty(value = "企业ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String enterpriseId;

    @ApiModelProperty(value = "企业名称", example = "供应商A")
    private String name;

    @ApiModelProperty(value = "统一社会信用代码", example = "91110000MA001234XY")
    private String creditCode;

    @ApiModelProperty(value = "企业角色", example = "SUPPLIER")
    private String role;

    @ApiModelProperty(value = "企业状态", example = "ACTIVE")
    private String status;

    @ApiModelProperty(value = "信用评级(0-100)", example = "75")
    private Integer creditRating;

    @ApiModelProperty(value = "授信额度", example = "1000000.00")
    private BigDecimal creditLimit;

    @ApiModelProperty(value = "注册时间", example = "2026-01-01T10:00:00")
    private LocalDateTime registeredAt;

    // ==================== 交易习惯 ====================

    @ApiModelProperty(value = "交易习惯统计")
    private TransactionHabitsDTO transactionHabits;

    // ==================== 经营状况 ====================

    @ApiModelProperty(value = "经营状况统计")
    private OperatingStatusDTO operatingStatus;

    // ==================== 风险指标 ====================

    @ApiModelProperty(value = "风险指标")
    private RiskMetricsDTO riskMetrics;

    /**
     * 交易习惯DTO
     */
    @Data
    @ApiModel(value = "交易习惯", description = "企业交易行为统计")
    public static class TransactionHabitsDTO {

        @ApiModelProperty(value = "交易总数", example = "150")
        private Long totalTransactions;

        @ApiModelProperty(value = "票据数量", example = "50")
        private Long billCount;

        @ApiModelProperty(value = "应收账款数量", example = "80")
        private Long receivableCount;

        @ApiModelProperty(value = "仓单数量", example = "20")
        private Long warehouseReceiptCount;

        @ApiModelProperty(value = "交易总金额（分）", example = "100000000")
        private Long totalAmount;

        @ApiModelProperty(value = "平均交易额（分）", example = "666666")
        private Long averageTransactionAmount;

        @ApiModelProperty(value = "月均交易频次", example = "12.5")
        private Double monthlyTransactionFrequency;

        @ApiModelProperty(value = "交易成功率", example = "98.5")
        private Double transactionSuccessRate;

        @ApiModelProperty(value = "最近交易时间", example = "2026-02-08T15:30:00")
        private LocalDateTime lastTransactionTime;
    }

    /**
     * 经营状况DTO
     */
    @Data
    @ApiModel(value = "经营状况", description = "企业经营数据统计")
    public static class OperatingStatusDTO {

        @ApiModelProperty(value = "融资总额（分）", example = "50000000")
        private Long totalFinancingAmount;

        @ApiModelProperty(value = "融资笔数", example = "15")
        private Integer financingCount;

        @ApiModelProperty(value = "已还款金额（分）", example = "30000000")
        private Long repaidAmount;

        @ApiModelProperty(value = "待还款金额（分）", example = "20000000")
        private Long pendingRepaymentAmount;

        @ApiModelProperty(value = "逾期次数", example = "2")
        private Integer overdueCount;

        @ApiModelProperty(value = "逾期金额（分）", example = "500000")
        private Long overdueAmount;

        @ApiModelProperty(value = "资产数量统计", example = "{\"bills\": 50, \"receivables\": 80, \"receipts\": 20}")
        private AssetCountDTO assetCounts;
    }

    /**
     * 风险指标DTO
     */
    @Data
    @ApiModel(value = "风险指标", description = "企业风险评估数据")
    public static class RiskMetricsDTO {

        @ApiModelProperty(value = "风险等级", example = "LOW")
        private String riskLevel;

        @ApiModelProperty(value = "风险评分", example = "85")
        private Integer riskScore;

        @ApiModelProperty(value = "预警数量", example = "1")
        private Integer warningCount;

        @ApiModelProperty(value = "最近评估时间", example = "2026-02-01T10:00:00")
        private LocalDateTime lastAssessmentTime;
    }

    /**
     * 资产数量统计DTO
     */
    @Data
    @ApiModel(value = "资产数量统计", description = "各类资产数量统计")
    public static class AssetCountDTO {

        @ApiModelProperty(value = "票据数量", example = "50")
        private Long bills;

        @ApiModelProperty(value = "应收账款数量", example = "80")
        private Long receivables;

        @ApiModelProperty(value = "仓单数量", example = "20")
        private Long warehouseReceipts;
    }
}
