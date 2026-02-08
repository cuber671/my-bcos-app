package com.fisco.app.dto.receivable;

import com.fisco.app.entity.risk.BadDebtRecord;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 坏账查询响应DTO
 */
@Data
@ApiModel(value = "坏账查询响应", description = "坏账查询结果")
public class BadDebtQueryResponse {

    @ApiModelProperty(value = "坏账记录列表")
    private List<BadDebtDTO> content;

    @ApiModelProperty(value = "当前页码", example = "0")
    private int pageNumber;

    @ApiModelProperty(value = "每页大小", example = "10")
    private int pageSize;

    @ApiModelProperty(value = "总页数", example = "3")
    private int totalPages;

    @ApiModelProperty(value = "总记录数", example = "25")
    private long totalElements;

    @ApiModelProperty(value = "是否第一页", example = "true")
    private boolean first;

    @ApiModelProperty(value = "是否最后一页", example = "false")
    private boolean last;

    @ApiModelProperty(value = "坏账统计信息")
    private BadDebtStatistics statistics;

    /**
     * 坏账DTO
     */
    @Data
    @ApiModel(value = "坏账DTO")
    public static class BadDebtDTO {
        @ApiModelProperty(value = "记录ID", example = "850e8400-e29b-41d4-a716-446655440003")
        private String id;

        @ApiModelProperty(value = "应收账款ID", example = "650e8400-e29b-41d4-a716-446655440001")
        private String receivableId;

        @ApiModelProperty(value = "供应商地址", example = "0x1234567890abcdef")
        private String supplierAddress;

        @ApiModelProperty(value = "资金方地址", example = "0x567890abcdef1234")
        private String financierAddress;

        @ApiModelProperty(value = "坏账类型", notes = "OVERDUE_180-逾期180天+, BANKRUPTCY-破产, DISPUTE-争议, OTHER-其他", example = "OVERDUE_180")
        private BadDebtRecord.BadDebtType badDebtType;

        @ApiModelProperty(value = "本金金额", example = "500000.00")
        private BigDecimal principalAmount;

        @ApiModelProperty(value = "逾期天数", example = "200")
        private Integer overdueDays;

        @ApiModelProperty(value = "累计罚息金额", example = "50000.00")
        private BigDecimal totalPenaltyAmount;

        @ApiModelProperty(value = "总损失金额", example = "550000.00")
        private BigDecimal totalLossAmount;

        @ApiModelProperty(value = "坏账原因", example = "逾期180天以上，债务人失联")
        private String badDebtReason;

        @ApiModelProperty(value = "回收状态", notes = "NOT_RECOVERED-未回收, PARTIAL_RECOVERED-部分回收, FULL_RECOVERED-全额回收", example = "NOT_RECOVERED")
        private BadDebtRecord.RecoveryStatus recoveryStatus;

        @ApiModelProperty(value = "已回收金额", example = "0.00")
        private BigDecimal recoveredAmount;

        @ApiModelProperty(value = "回收日期", example = "2024-08-01T10:00:00")
        private LocalDateTime recoveryDate;

        @ApiModelProperty(value = "创建时间", example = "2024-07-01T10:00:00")
        private LocalDateTime createdAt;
    }

    /**
     * 坏账统计信息
     */
    @Data
    @ApiModel(value = "坏账统计信息")
    public static class BadDebtStatistics {
        @ApiModelProperty(value = "坏账总数量", example = "25")
        private long totalCount;

        @ApiModelProperty(value = "坏账总本金", example = "10000000.00")
        private BigDecimal totalPrincipalAmount;

        @ApiModelProperty(value = "坏账总损失金额", example = "10500000.00")
        private BigDecimal totalLossAmount;

        @ApiModelProperty(value = "已回收总金额", example = "2000000.00")
        private BigDecimal totalRecoveredAmount;

        @ApiModelProperty(value = "回收率（百分比）", example = "19.05")
        private BigDecimal recoveryRate;

        @ApiModelProperty(value = "未回收坏账数量", example = "15")
        private long notRecoveredCount;

        @ApiModelProperty(value = "部分回收坏账数量", example = "8")
        private long partialRecoveredCount;

        @ApiModelProperty(value = "全额回收坏账数量", example = "2")
        private long fullRecoveredCount;
    }
}
