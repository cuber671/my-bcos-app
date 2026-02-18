package com.fisco.app.dto.blockchain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 发送交易请求DTO
 *
 * @author FISCO BCOS Team
 * @since 2026-02-10
 */
@Data
@ApiModel(value = "发送交易请求", description = "发送交易到区块链的请求参数")
public class SendTransactionRequestDTO {

    @ApiModelProperty(value = "接收地址（合约调用时为合约地址）", required = true, example = "0x1234567890abcdef1234567890abcdef12345678")
    @NotBlank(message = "接收地址不能为空")
    @Pattern(regexp = "^0x[a-fA-F0-9]{40}$", message = "地址格式不正确")
    private String toAddress;

    @ApiModelProperty(value = "交易值（wei）", example = "0")
    private String value = "0";

    @ApiModelProperty(value = "Gas价格（可选，不填则使用默认值）", example = "1000000000")
    private Long gasPrice;

    @ApiModelProperty(value = "Gas限制（可选，不填则估算）", example = "21000")
    private Long gasLimit;

    @ApiModelProperty(value = "交易输入数据（合约调用时的编码数据）", example = "0xa9059cbb00000000000000000000000000...")
    private String data;

    @ApiModelProperty(value = "交易类型", example = "CONTRACT_CALL")
    private String transactionType = "CONTRACT_CALL";
}
