package com.fisco.app.dto.warehouse;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 仓单统计DTO
 * 包含仓单的多维度统计数据
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-09
 * @version 1.0
 */
@Data
@ApiModel(value = "仓单统计", description = "仓单多维度统计数据")
public class WarehouseReceiptStatisticsDTO {

    // ==================== 基础统计 ====================

    @ApiModelProperty(value = "仓单总数", example = "150")
    private Long totalReceipts;

    @ApiModelProperty(value = "仓单总价值（元）", example = "45000000.00")
    private BigDecimal totalValue;

    @ApiModelProperty(value = "货物总数量", example = "10000.00")
    private BigDecimal totalQuantity;

    // ==================== 状态分布 ====================

    @ApiModelProperty(value = "状态分布统计")
    private List<StatusStatistics> statusDistribution;

    // ==================== 货物类型分布 ====================

    @ApiModelProperty(value = "货物类型分布统计")
    private List<GoodsTypeStatistics> goodsTypeDistribution;

    // ==================== 企业分布统计 ====================

    @ApiModelProperty(value = "货主企业分布统计")
    private List<EnterpriseStatistics> ownerDistribution;

    // ==================== 风险统计 ====================

    @ApiModelProperty(value = "即将过期仓单数量（7天内）", example = "5")
    private Long expiringSoonCount;

    @ApiModelProperty(value = "即将过期仓单总价值", example = "1500000.00")
    private BigDecimal expiringSoonValue;

    @ApiModelProperty(value = "已过期仓单数量", example = "2")
    private Long expiredCount;

    @ApiModelProperty(value = "已过期仓单总价值", example = "600000.00")
    private BigDecimal expiredValue;

    @ApiModelProperty(value = "已冻结仓单数量", example = "3")
    private Long frozenCount;

    @ApiModelProperty(value = "已冻结仓单总价值", example = "900000.00")
    private BigDecimal frozenValue;

    // ==================== 操作统计 ====================

    @ApiModelProperty(value = "拆分申请数量", example = "5")
    private Long splitApplicationCount;

    @ApiModelProperty(value = "合并申请数量", example = "3")
    private Long mergeApplicationCount;

    @ApiModelProperty(value = "作废申请数量", example = "2")
    private Long cancelApplicationCount;

    // ==================== 统计时间 ====================

    @ApiModelProperty(value = "统计开始时间")
    private LocalDateTime startTime;

    @ApiModelProperty(value = "统计结束时间")
    private LocalDateTime endTime;

    @ApiModelProperty(value = "统计生成时间")
    private LocalDateTime generatedAt;

    // ==================== 内部DTO类 ====================

    /**
     * 状态统计DTO
     */
    @Data
    @ApiModel(value = "状态统计", description = "仓单状态分布数据")
    public static class StatusStatistics {

        @ApiModelProperty(value = "状态", example = "NORMAL")
        private String status;

        @ApiModelProperty(value = "状态名称", example = "正常")
        private String statusName;

        @ApiModelProperty(value = "数量", example = "100")
        private Long count;

        @ApiModelProperty(value = "总价值", example = "30000000.00")
        private BigDecimal totalValue;

        @ApiModelProperty(value = "占比（%）", example = "66.67")
        private Double percentage;
    }

    /**
     * 货物类型统计DTO
     */
    @Data
    @ApiModel(value = "货物类型统计", description = "货物类型分布数据")
    public static class GoodsTypeStatistics {

        @ApiModelProperty(value = "货物名称", example = "螺纹钢")
        private String goodsName;

        @ApiModelProperty(value = "数量", example = "50")
        private Long count;

        @ApiModelProperty(value = "总数量", example = "5000.00")
        private BigDecimal totalQuantity;

        @ApiModelProperty(value = "总价值", example = "22500000.00")
        private BigDecimal totalValue;

        @ApiModelProperty(value = "平均单价", example = "4500.00")
        private BigDecimal avgUnitPrice;
    }

    /**
     * 企业统计DTO
     */
    @Data
    @ApiModel(value = "企业统计", description = "企业分布统计数据")
    public static class EnterpriseStatistics {

        @ApiModelProperty(value = "企业ID", example = "ent-uuid-001")
        private String enterpriseId;

        @ApiModelProperty(value = "企业名称", example = "XX贸易有限公司")
        private String enterpriseName;

        @ApiModelProperty(value = "仓单数量", example = "30")
        private Long receiptCount;

        @ApiModelProperty(value = "总价值", example = "9000000.00")
        private BigDecimal totalValue;
    }
}
