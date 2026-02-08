package com.fisco.app.dto.receivable;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.fisco.app.entity.risk.OverdueRemindRecord;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 催收请求DTO
 */
@Data
@ApiModel(value = "催收请求", description = "创建催收记录的请求参数")
public class RemindRequest {

    @NotNull(message = "催收类型不能为空")
    @ApiModelProperty(value = "催收类型", notes = "EMAIL-邮件, SMS-短信, PHONE-电话, LETTER-函件, LEGAL-法律", required = true, example = "EMAIL")
    private OverdueRemindRecord.RemindType remindType;

    @ApiModelProperty(value = "催收级别", notes = "NORMAL-普通, URGENT-紧急, SEVERE-严重", example = "NORMAL")
    private OverdueRemindRecord.RemindLevel remindLevel;

    @NotBlank(message = "催收内容不能为空")
    @ApiModelProperty(value = "催收内容", required = true, example = "您的应收账款已逾期，请尽快处理")
    private String remindContent;

    @ApiModelProperty(value = "催收结果", notes = "SUCCESS-成功, FAILED-失败, PENDING-待处理", example = "SUCCESS")
    private OverdueRemindRecord.RemindResult remindResult;

    @ApiModelProperty(value = "下次催收日期", example = "2024-02-08T10:00:00")
    private java.time.LocalDateTime nextRemindDate;

    @ApiModelProperty(value = "备注", example = "已电话通知，承诺一周内付款")
    private String remark;
}
