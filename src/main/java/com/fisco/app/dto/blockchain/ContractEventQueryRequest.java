package com.fisco.app.dto.blockchain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 合约事件查询请求DTO
 *
 * @author FISCO BCOS Team
 * @since 2026-02-10
 */
@Data
@ApiModel(value = "合约事件查询请求", description = "查询合约事件的请求参数")
public class ContractEventQueryRequest {

    @ApiModelProperty(value = "事件名称", example = "BillIssued")
    private String eventName;

    @ApiModelProperty(value = "起始区块号", example = "1000")
    private Long fromBlock;

    @ApiModelProperty(value = "结束区块号", example = "2000")
    private Long toBlock;

    @ApiModelProperty(value = "起始时间戳")
    private String fromTimestamp;

    @ApiModelProperty(value = "结束时间戳")
    private String toTimestamp;

    @ApiModelProperty(value = "页码", example = "0")
    private Integer page = 0;

    @ApiModelProperty(value = "每页数量", example = "20")
    private Integer size = 20;
}
