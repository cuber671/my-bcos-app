package com.fisco.app.dto.receivable;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import java.util.List;

/**
 * 应收账款合并请求DTO
 */
@Data
@ApiModel(value = "应收账款合并请求", description = "用于合并应收账款的请求参数")
public class ReceivableMergeRequest {

    @NotEmpty(message = "应收账款ID列表不能为空")
    @ApiModelProperty(value = "应收账款ID列表", required = true, example = "[\"REC-001\", \"REC-002\", \"REC-003\"]")
    private List<String> receivableIds;

    @NotNull(message = "合并类型不能为空")
    @ApiModelProperty(value = "合并类型", required = true, notes = "AMOUNT-金额合并(保留最长期限), PERIOD-期限合并(金额相加,期限取平均), FULL-完全合并")
    private MergeType mergeType;

    @ApiModelProperty(value = "备注", example = "合并多个小额应收账款便于融资")
    private String remark;

    /**
     * 合并类型枚举
     */
    public enum MergeType {
        AMOUNT("金额合并", "保留最长期限，金额相加"),
        PERIOD("期限合并", "金额相加，期限按金额加权平均"),
        FULL("完全合并", "金额和期限都重新计算");

        private final String name;
        private final String description;

        MergeType(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }
    }
}
