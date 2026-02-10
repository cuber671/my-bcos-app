package com.fisco.app.dto.blockchain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 区块验证响应DTO
 * 用于返回区块完整性验证结果
 */
@Data
@ApiModel(value = "区块验证响应", description = "区块完整性验证结果")
public class BlockValidationResponse {

    @ApiModelProperty(value = "区块号", example = "12345")
    private Long blockNumber;

    @ApiModelProperty(value = "是否验证通过", example = "true")
    private Boolean isValid;

    @ApiModelProperty(value = "验证消息", example = "区块验证通过")
    private String message;

    @ApiModelProperty(value = "父区块哈希是否有效", example = "true")
    private Boolean parentHashValid;

    @ApiModelProperty(value = "交易根哈希是否有效", example = "true")
    private Boolean transactionRootValid;

    @ApiModelProperty(value = "状态根哈希是否有效", example = "true")
    private Boolean stateRootValid;

    @ApiModelProperty(value = "共识签名是否有效", example = "true")
    private Boolean consensusValid;

    @ApiModelProperty(value = "验证问题列表")
    private List<ValidationIssue> issues;

    @Data
    @ApiModel(value = "验证问题", description = "验证过程中的问题详情")
    public static class ValidationIssue {

        @ApiModelProperty(value = "严重级别", example = "ERROR")
        private String severity;

        @ApiModelProperty(value = "问题描述", example = "父区块哈希不匹配")
        private String description;

        @ApiModelProperty(value = "期望值", example = "0xabc...")
        private String expected;

        @ApiModelProperty(value = "实际值", example = "0xdef...")
        private String actual;
    }
}
