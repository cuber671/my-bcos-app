package com.fisco.app.dto.blockchain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * 节点同步状态DTO
 *
 * @author FISCO BCOS Team
 * @since 2026-02-10
 */
@Data
@ApiModel(value = "节点同步状态", description = "节点区块同步状态信息")
public class NodeSyncStatusDTO {

    @ApiModelProperty(value = "是否正在同步")
    private Boolean isSyncing;

    @ApiModelProperty(value = "当前区块号")
    private BigInteger currentBlockNumber;

    @ApiModelProperty(value = "最高区块号")
    private BigInteger highestBlockNumber;

    @ApiModelProperty(value = "同步进度百分比", example = "99.50")
    private BigDecimal syncProgress;

    @ApiModelProperty(value = "同步状态", notes = "SYNCING/SYNCED/BEHIND")
    private String syncStatus;
}
