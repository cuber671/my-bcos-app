package com.fisco.app.dto.enterprise;

import java.time.LocalDateTime;
import java.util.List;

import com.fisco.app.entity.enterprise.CreditRatingHistory;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 企业信用评分DTO
 * 包含当前评分、评分等级、历史记录和趋势数据
 */
@Data
@ApiModel(value = "企业信用评分", description = "企业信用评分及历史记录")
public class EnterpriseCreditScoreDTO {

    @ApiModelProperty(value = "当前评分", required = true, example = "75")
    private Integer currentRating;

    @ApiModelProperty(value = "评分等级", required = true, example = "良好")
    private String ratingLevel;

    @ApiModelProperty(value = "评级历史记录（最近20条）")
    private List<CreditRatingHistoryRecord> history;

    @ApiModelProperty(value = "评分趋势数据")
    private List<TrendData> trend;

    /**
     * 评级历史记录DTO（简化版，不直接返回实体）
     */
    @Data
    @ApiModel(value = "评级历史记录", description = "评级变更历史")
    public static class CreditRatingHistoryRecord {

        @ApiModelProperty(value = "原评级", example = "60")
        private Integer oldRating;

        @ApiModelProperty(value = "新评级", example = "75")
        private Integer newRating;

        @ApiModelProperty(value = "变更原因", example = "按时还款记录良好")
        private String changeReason;

        @ApiModelProperty(value = "操作人", example = "admin")
        private String changedBy;

        @ApiModelProperty(value = "变更时间", example = "2026-02-08T10:00:00")
        private LocalDateTime changedAt;

        @ApiModelProperty(value = "区块链交易哈希", example = "0xabcdef...")
        private String txHash;

        /**
         * 从CreditRatingHistory实体转换为DTO
         */
        public static CreditRatingHistoryRecord fromEntity(CreditRatingHistory history) {
            CreditRatingHistoryRecord dto = new CreditRatingHistoryRecord();
            dto.setOldRating(history.getOldRating());
            dto.setNewRating(history.getNewRating());
            dto.setChangeReason(history.getChangeReason());
            dto.setChangedBy(history.getChangedBy());
            dto.setChangedAt(history.getChangedAt());
            dto.setTxHash(history.getTxHash());
            return dto;
        }
    }

    /**
     * 趋势数据DTO
     */
    @Data
    @ApiModel(value = "趋势数据", description = "评分趋势数据点")
    public static class TrendData {

        @ApiModelProperty(value = "日期", example = "2026-02-08")
        private String date;

        @ApiModelProperty(value = "评分", example = "75")
        private Integer rating;
    }
}
