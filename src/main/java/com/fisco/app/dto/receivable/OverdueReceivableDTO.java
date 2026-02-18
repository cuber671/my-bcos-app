package com.fisco.app.dto.receivable;

import com.fisco.app.entity.receivable.Receivable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 逾期账款DTO
 */
@Data
@ApiModel(value = "逾期账款DTO", description = "逾期账款详细信息")
public class OverdueReceivableDTO {

    @ApiModelProperty(value = "应收账款ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    @ApiModelProperty(value = "供应商地址", example = "0x1234567890abcdef")
    private String supplierAddress;

    @ApiModelProperty(value = "核心企业地址", example = "0xabcdef1234567890")
    private String coreEnterpriseAddress;

    @ApiModelProperty(value = "应收金额", example = "500000.00")
    private BigDecimal amount;

    @ApiModelProperty(value = "币种", example = "CNY")
    private String currency;

    @ApiModelProperty(value = "出票日期", example = "2024-01-13T10:00:00")
    private LocalDateTime issueDate;

    @ApiModelProperty(value = "到期日期", example = "2024-04-13T10:00:00")
    private LocalDateTime dueDate;

    @ApiModelProperty(value = "描述", example = "原材料采购款")
    private String description;

    @ApiModelProperty(value = "状态", notes = "CREATED, CONFIRMED, FINANCED, REPAID, DEFAULTED, CANCELLED", example = "FINANCED")
    private Receivable.ReceivableStatus status;

    @ApiModelProperty(value = "当前持有人地址", example = "0x1234567890abcdef")
    private String currentHolder;

    @ApiModelProperty(value = "资金方地址", example = "0x567890abcdef1234")
    private String financierAddress;

    @ApiModelProperty(value = "融资金额", example = "450000.00")
    private BigDecimal financeAmount;

    @ApiModelProperty(value = "融资利率(基点)", example = "500")
    private Integer financeRate;

    @ApiModelProperty(value = "融资日期", example = "2024-01-13T15:00:00")
    private LocalDateTime financeDate;

    @ApiModelProperty(value = "逾期等级", notes = "MILD-轻度, MODERATE-中度, SEVERE-重度, BAD_DEBT-坏账", example = "MILD")
    private String overdueLevel;

    @ApiModelProperty(value = "逾期天数", example = "45")
    private Integer overdueDays;

    @ApiModelProperty(value = "累计罚息金额", example = "1125.00")
    private BigDecimal penaltyAmount;

    @ApiModelProperty(value = "最后催收日期", example = "2024-02-01T10:00:00")
    private LocalDateTime lastRemindDate;

    @ApiModelProperty(value = "催收次数", example = "3")
    private Integer remindCount;

    @ApiModelProperty(value = "坏账认定日期", example = "2024-07-01T00:00:00")
    private LocalDateTime badDebtDate;

    @ApiModelProperty(value = "坏账原因", example = "逾期180天以上，债务人失联")
    private String badDebtReason;

    @ApiModelProperty(value = "创建时间", example = "2024-01-13T10:00:00")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "更新时间", example = "2024-02-01T15:30:00")
    private LocalDateTime updatedAt;
}
