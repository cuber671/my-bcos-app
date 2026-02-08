package com.fisco.app.dto.receivable;

import com.fisco.app.entity.risk.OverdueRemindRecord;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 催收响应DTO
 */
@Data
@ApiModel(value = "催收响应", description = "催收操作结果")
public class RemindResponse {

    @ApiModelProperty(value = "催收记录ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    @ApiModelProperty(value = "应收账款ID", example = "650e8400-e29b-41d4-a716-446655440001")
    private String receivableId;

    @ApiModelProperty(value = "催收类型", notes = "EMAIL-邮件, SMS-短信, PHONE-电话, LETTER-函件, LEGAL-法律", example = "EMAIL")
    private OverdueRemindRecord.RemindType remindType;

    @ApiModelProperty(value = "催收级别", notes = "NORMAL-普通, URGENT-紧急, SEVERE-严重", example = "NORMAL")
    private OverdueRemindRecord.RemindLevel remindLevel;

    @ApiModelProperty(value = "催收日期", example = "2024-02-01T10:00:00")
    private LocalDateTime remindDate;

    @ApiModelProperty(value = "催收内容", example = "您的应收账款已逾期，请尽快处理")
    private String remindContent;

    @ApiModelProperty(value = "催收结果", notes = "SUCCESS-成功, FAILED-失败, PENDING-待处理", example = "SUCCESS")
    private OverdueRemindRecord.RemindResult remindResult;

    @ApiModelProperty(value = "下次催收日期", example = "2024-02-08T10:00:00")
    private LocalDateTime nextRemindDate;

    @ApiModelProperty(value = "更新后的催收次数", example = "4")
    private Integer updatedRemindCount;

    @ApiModelProperty(value = "区块链交易哈希", example = "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef12")
    private String txHash;
}
