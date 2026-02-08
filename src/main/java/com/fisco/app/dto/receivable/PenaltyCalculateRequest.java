package com.fisco.app.dto.receivable;

import com.fisco.app.entity.risk.OverduePenaltyRecord;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import lombok.Data;

/**
 * 罚息计算请求DTO
 */
@Data
@ApiModel(value = "罚息计算请求", description = "计算逾期罚息的请求参数")
public class PenaltyCalculateRequest {

    @NotNull(message = "罚息类型不能为空")
    @ApiModelProperty(value = "罚息类型", notes = "AUTO-自动计算, MANUAL-手动计算", required = true, example = "AUTO")
    private OverduePenaltyRecord.PenaltyType penaltyType;

    @ApiModelProperty(value = "手动计算：逾期天数", example = "45")
    @Positive(message = "逾期天数必须大于0")
    private Integer overdueDays;

    @ApiModelProperty(value = "手动计算：日利率", example = "0.0005")
    private BigDecimal dailyRate;

    @ApiModelProperty(value = "手动计算：计算起始日期", example = "2024-01-01T00:00:00")
    private LocalDateTime calculateStartDate;

    @ApiModelProperty(value = "手动计算：计算结束日期", example = "2024-02-15T00:00:00")
    private LocalDateTime calculateEndDate;
}
