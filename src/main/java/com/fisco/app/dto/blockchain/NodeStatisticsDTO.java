package com.fisco.app.dto.blockchain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 节点统计信息DTO
 *
 * @author FISCO BCOS Team
 * @since 2026-02-10
 */
@Data
@ApiModel(value = "节点统计信息", description = "区块链网络节点统计信息")
public class NodeStatisticsDTO {

    @ApiModelProperty(value = "总节点数")
    private Integer totalNodes;

    @ApiModelProperty(value = "在线节点数")
    private Integer onlineNodes;

    @ApiModelProperty(value = "离线节点数")
    private Integer offlineNodes;

    @ApiModelProperty(value = "共识节点数（Sealer）")
    private Integer sealerNodes;

    @ApiModelProperty(value = "观察节点数（Observer）")
    private Integer observerNodes;

    @ApiModelProperty(value = "群组列表")
    private List<GroupInfoDTO> groups;
}
