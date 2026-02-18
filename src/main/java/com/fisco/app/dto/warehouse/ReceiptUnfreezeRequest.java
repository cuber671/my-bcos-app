package com.fisco.app.dto.warehouse;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.springframework.lang.NonNull;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 仓单解冻请求DTO
 */
@Data
@ApiModel(value = "仓单解冻请求", description = "解冻仓单的请求参数")
public class ReceiptUnfreezeRequest {

    @ApiModelProperty(value = "仓单ID", required = true, example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    @NonNull
    @NotNull(message = "仓单ID不能为空")
    private String receiptId;

    @ApiModelProperty(value = "操作方企业ID", required = true, example = "warehouse-uuid-001",
            notes = "执行解冻操作的企业ID（必须与冻结时的操作方一致，或具有更高权限）")
    @NonNull
    @NotNull(message = "操作方企业ID不能为空")
    private String operatorEnterpriseId;

    @ApiModelProperty(value = "解冻原因", required = true, example = "法律纠纷已解决，根据法院解冻通知书解冻")
    @NotBlank(message = "解冻原因不能为空")
    @Size(max = 500, message = "解冻原因长度不能超过500")
    private String unfreezeReason;

    @ApiModelProperty(value = "目标状态", required = true, example = "NORMAL",
            notes = "解冻后的目标状态：NORMAL-正常, PLEDGED-已质押, TRANSFERRED-已转让")
    @NonNull
    @NotNull(message = "目标状态不能为空")
    private String targetStatus;

    @ApiModelProperty(value = "相关文件编号", example = "法院解冻通知书编号：[2026]沪01执123-1号")
    @Size(max = 100, message = "相关文件编号长度不能超过100")
    private String referenceNo;

    @ApiModelProperty(value = "备注", example = "恢复正常状态，可以进行提货操作")
    @Size(max = 200, message = "备注长度不能超过200")
    private String remarks;
}
