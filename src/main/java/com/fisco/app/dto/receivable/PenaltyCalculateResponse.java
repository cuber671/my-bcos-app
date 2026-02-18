package com.fisco.app.dto.receivable;

import com.fisco.app.entity.risk.OverduePenaltyRecord;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 罚息计算响应DTO
 */
@Data
@ApiModel(value = "罚息计算响应", description = "罚息计算结果")
public class PenaltyCalculateResponse {

    @ApiModelProperty(value = "罚息记录ID", example = "750e8400-e29b-41d4-a716-446655440002")
    private String id;

    @ApiModelProperty(value = "应收账款ID", example = "650e8400-e29b-41d4-a716-446655440001")
    private String receivableId;

    @ApiModelProperty(value = "罚息类型", notes = "AUTO-自动计算, MANUAL-手动计算", example = "AUTO")
    private OverduePenaltyRecord.PenaltyType penaltyType;

    @ApiModelProperty(value = "本金金额", example = "500000.00")
    private BigDecimal principalAmount;

    @ApiModelProperty(value = "逾期天数", example = "45")
    private Integer overdueDays;

    @ApiModelProperty(value = "日利率", example = "0.0005")
    private BigDecimal dailyRate;

    @ApiModelProperty(value = "本次罚息金额", example = "1125.00")
    private BigDecimal penaltyAmount;

    @ApiModelProperty(value = "累计罚息金额", example = "12500.00")
    private BigDecimal totalPenaltyAmount;

    @ApiModelProperty(value = "计算起始日期", example = "2024-01-01T00:00:00")
    private LocalDateTime calculateStartDate;

    @ApiModelProperty(value = "计算结束日期", example = "2024-02-15T00:00:00")
    private LocalDateTime calculateEndDate;

    @ApiModelProperty(value = "计算日期", example = "2024-02-15T10:00:00")
    private LocalDateTime calculateDate;

    @ApiModelProperty(value = "更新后的应收账款累计罚息", example = "12500.00")
    private BigDecimal updatedReceivablePenaltyAmount;
}
