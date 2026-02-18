package com.fisco.app.dto.warehouse;

import com.fisco.app.entity.warehouse.ReceiptMergeApplication;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 仓单合并请求DTO
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-09
 * @version 1.0
 */
@Data
@ApiModel(value = "仓单合并请求", description = "用于合并仓单的请求参数")
public class MergeReceiptsRequest {

    @NotEmpty(message = "仓单ID列表不能为空")
    @ApiModelProperty(value = "仓单ID列表", required = true, notes = "至少包含2个仓单，最多10个")
    private List<String> receiptIds;

    @NotNull(message = "合并类型不能为空")
    @ApiModelProperty(value = "合并类型", required = true)
    private ReceiptMergeApplication.MergeType mergeType;

    @ApiModelProperty(value = "合并原因", example = "合并多个仓单便于统一融资")
    private String mergeReason;

    @ApiModelProperty(value = "备注")
    private String remarks;
}
