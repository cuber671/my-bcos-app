package com.fisco.app.dto.blockchain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 合约列表查询请求DTO
 *
 * @author FISCO BCOS Team
 * @since 2026-02-10
 */
@Data
@ApiModel(value = "合约列表查询请求", description = "查询合约列表的请求参数")
public class ContractListQueryRequest {

    @ApiModelProperty(value = "合约类型", example = "Generic")
    private String contractType;

    @ApiModelProperty(value = "合约状态", example = "ACTIVE")
    private String status = "ACTIVE";

    @ApiModelProperty(value = "页码", example = "0")
    private Integer page = 0;

    @ApiModelProperty(value = "每页数量", example = "10")
    private Integer size = 10;

    @ApiModelProperty(value = "排序字段", example = "deploymentTimestamp")
    private String sortBy = "deploymentTimestamp";

    @ApiModelProperty(value = "排序方向（ASC/DESC）", example = "DESC")
    private String sortOrder = "DESC";
}
