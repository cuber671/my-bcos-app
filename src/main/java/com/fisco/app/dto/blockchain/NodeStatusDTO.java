package com.fisco.app.dto.blockchain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigInteger;
import java.time.LocalDateTime;

/**
 * 节点状态详情DTO
 *
 * @author FISCO BCOS Team
 * @since 2026-02-10
 */
@Data
@ApiModel(value = "节点状态详情", description = "区块链节点详细状态信息")
public class NodeStatusDTO {

    @ApiModelProperty(value = "节点ID")
    private String nodeId;

    @ApiModelProperty(value = "是否在线")
    private Boolean online;

    @ApiModelProperty(value = "区块高度")
    private BigInteger blockNumber;

    @ApiModelProperty(value = "PBFT视图")
    private Integer view;

    @ApiModelProperty(value = "节点版本")
    private String version;

    @ApiModelProperty(value = "运行时长（秒）")
    private Long uptime;

    @ApiModelProperty(value = "最后更新时间")
    private LocalDateTime lastUpdated;
}
