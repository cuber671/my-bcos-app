package com.fisco.app.dto.warehouse;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.springframework.lang.NonNull;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 仓单冻结请求DTO
 */
@Data
@ApiModel(value = "仓单冻结请求", description = "冻结仓单的请求参数")
public class ReceiptFreezeRequest {

    @ApiModelProperty(value = "仓单ID", required = true, example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    @NonNull
    @NotNull(message = "仓单ID不能为空")
    private String receiptId;

    @ApiModelProperty(value = "操作方企业ID", required = true, example = "warehouse-uuid-001",
            notes = "执行冻结操作的企业ID（仓储方、资金方或平台方）")
    @NonNull
    @NotNull(message = "操作方企业ID不能为空")
    private String operatorEnterpriseId;

    @ApiModelProperty(value = "操作方类型", required = true, example = "WAREHOUSE",
            notes = "WAREHOUSE-仓储方, FINANCIER-资金方, PLATFORM-平台方, COURT-法院/司法")
    @NonNull
    @NotNull(message = "操作方类型不能为空")
    @Pattern(regexp = "^(WAREHOUSE|FINANCIER|PLATFORM|COURT)$",
            message = "操作方类型只能是WAREHOUSE、FINANCIER、PLATFORM或COURT")
    private String operatorType;

    @ApiModelProperty(value = "冻结原因", required = true, example = "涉及法律纠纷，根据法院要求冻结")
    @NotBlank(message = "冻结原因不能为空")
    @Size(max = 500, message = "冻结原因长度不能超过500")
    private String freezeReason;

    @ApiModelProperty(value = "冻结类型", required = true, example = "LEGAL",
            notes = "LEGAL-法律冻结, BUSINESS-业务冻结, RISK-风险冻结")
    @NonNull
    @NotNull(message = "冻结类型不能为空")
    @Pattern(regexp = "^(LEGAL|BUSINESS|RISK)$",
            message = "冻结类型只能是LEGAL、BUSINESS或RISK")
    private String freezeType;

    @ApiModelProperty(value = "相关文件编号", example = "法院裁定书编号：[2026]沪01执123号")
    @Size(max = 100, message = "相关文件编号长度不能超过100")
    private String referenceNo;

    @ApiModelProperty(value = "备注", example = "预计冻结期限30天")
    @Size(max = 200, message = "备注长度不能超过200")
    private String remarks;
}
