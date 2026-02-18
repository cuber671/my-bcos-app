package com.fisco.app.dto.blockchain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 取消交易请求DTO
 *
 * @author FISCO BCOS Team
 * @since 2026-02-10
 */
@Data
@ApiModel(value = "取消交易请求", description = "取消待处理交易的请求参数")
public class CancelTransactionRequestDTO {

    @ApiModelProperty(value = "要取消的交易哈希", required = true)
    @NotBlank(message = "交易哈希不能为空")
    private String transactionHash;

    @ApiModelProperty(value = "取消原因")
    private String reason;
}
