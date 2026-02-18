package com.fisco.app.dto.bill;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 票据合并请求DTO
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-09
 * @version 1.0
 */
@Data
@ApiModel(value = "票据合并请求", description = "将多笔票据合并为一笔大额票据的请求参数")
public class MergeBillsRequest {

    @NotEmpty(message = "票据ID列表不能为空")
    @Min(value = 2, message = "至少需要2张票据才能合并")
    @ApiModelProperty(value = "待合并的票据ID列表", required = true, example = "[\"bill1\", \"bill2\", \"bill3\"]")
    private List<String> billIds;

    @NotNull(message = "合并类型不能为空")
    @ApiModelProperty(value = "合并类型", required = true, notes = "AMOUNT-金额合并, PERIOD-期限合并, FULL-完全合并", example = "AMOUNT")
    private MergeType mergeType;

    @Size(max = 500, message = "合并原因不能超过500个字符")
    @ApiModelProperty(value = "合并原因", example = "简化票据管理")
    private String mergeReason;

    /**
     * 合并类型枚举
     */
    public enum MergeType {
        AMOUNT,  // 金额合并 - 累加金额
        PERIOD,  // 期限合并 - 以最晚到期日为准
        FULL     // 完全合并 - 金额和期限都合并
    }
}
