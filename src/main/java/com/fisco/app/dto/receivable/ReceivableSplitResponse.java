package com.fisco.app.dto.receivable;

import com.fisco.app.entity.receivable.Receivable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 应收账款拆分响应DTO
 */
@Data
@ApiModel(value = "应收账款拆分响应", description = "应收账款拆分操作的响应信息")
public class ReceivableSplitResponse {

    @ApiModelProperty(value = "原应收账款ID", example = "REC-001")
    private String originalReceivableId;

    @ApiModelProperty(value = "拆分申请ID", example = "SPLIT-APP-001")
    private String applicationId;

    @ApiModelProperty(value = "拆分状态", example = "PENDING")
    private String status;

    @ApiModelProperty(value = "拆分数量", example = "3")
    private Integer splitCount;

    @ApiModelProperty(value = "拆分后的应收账款列表")
    private List<Receivable> splitReceivables;

    @ApiModelProperty(value = "申请人ID", example = "USER-001")
    private String applicantId;

    @ApiModelProperty(value = "申请时间", example = "2026-02-03T10:30:00")
    private LocalDateTime applicationTime;

    @ApiModelProperty(value = "审核人ID", example = "ADMIN-001")
    private String approverId;

    @ApiModelProperty(value = "审核时间", example = "2026-02-03T14:00:00")
    private LocalDateTime approvalTime;

    @ApiModelProperty(value = "拒绝原因")
    private String rejectionReason;
}
