package com.fisco.app.dto.receivable;

import com.fisco.app.entity.receivable.Receivable;
import com.fisco.app.entity.risk.OverdueRemindRecord;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 逾期账款查询请求DTO
 */
@Data
@ApiModel(value = "逾期账款查询请求", description = "用于查询逾期账款的请求参数")
public class OverdueQueryRequest {

    @ApiModelProperty(value = "逾期等级", notes = "MILD-轻度, MODERATE-中度, SEVERE-重度, BAD_DEBT-坏账", example = "MILD")
    private String overdueLevel;

    @ApiModelProperty(value = "供应商地址", example = "0x1234567890abcdef")
    private String supplierAddress;

    @ApiModelProperty(value = "核心企业地址", example = "0xabcdef1234567890")
    private String coreEnterpriseAddress;

    @ApiModelProperty(value = "资金方地址", example = "0x567890abcdef1234")
    private String financierAddress;

    @ApiModelProperty(value = "应收账款状态", notes = "CREATED, CONFIRMED, FINANCED, REPAID, DEFAULTED, CANCELLED", example = "FINANCED")
    private Receivable.ReceivableStatus status;

    @ApiModelProperty(value = "逾期天数最小值", example = "1")
    private Integer overdueDaysMin;

    @ApiModelProperty(value = "逾期天数最大值", example = "180")
    private Integer overdueDaysMax;

    @ApiModelProperty(value = "到期日期开始", example = "2024-01-01T00:00:00")
    private LocalDateTime dueDateStart;

    @ApiModelProperty(value = "到期日期结束", example = "2024-12-31T23:59:59")
    private LocalDateTime dueDateEnd;

    @ApiModelProperty(value = "催收类型", notes = "EMAIL, SMS, PHONE, LETTER, LEGAL", example = "EMAIL")
    private OverdueRemindRecord.RemindType remindType;

    @ApiModelProperty(value = "是否已催收", example = "false")
    private Boolean reminded;

    @ApiModelProperty(value = "页码（从0开始）", example = "0")
    private Integer page = 0;

    @ApiModelProperty(value = "每页大小", example = "10")
    private Integer size = 10;

    @ApiModelProperty(value = "排序字段", example = "dueDate")
    private String sortBy = "dueDate";

    @ApiModelProperty(value = "排序方向", notes = "ASC-升序, DESC-降序", example = "DESC")
    private String sortDirection = "DESC";
}
