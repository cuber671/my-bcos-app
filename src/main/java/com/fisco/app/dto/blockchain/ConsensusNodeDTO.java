package com.fisco.app.dto.blockchain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigInteger;

/**
 * 共识节点DTO
 * 描述参与共识的节点信息
 *
 * @author FISCO BCOS Team
 * @since 2026-02-10
 */
@Data
@ApiModel(value = "共识节点", description = "参与共识的节点详细信息")
public class ConsensusNodeDTO {

    @ApiModelProperty(value = "节点ID", example = "node_0")
    private String nodeId;

    @ApiModelProperty(value = "节点名称", example = "共识节点1")
    private String nodeName;

    @ApiModelProperty(value = "节点类型", notes = "SEALER/OBSERVER", example = "SEALER")
    private String nodeType;

    @ApiModelProperty(value = "节点权重", notes = "仅SEALER类型有效", example = "1")
    private BigInteger weight;

    @ApiModelProperty(value = "节点状态", notes = "ACTIVE/INACTIVE", example = "ACTIVE")
    private String status;

    @ApiModelProperty(value = "节点IP地址（脱敏）", example = "192.168.*.*")
    private String ipAddress;

    @ApiModelProperty(value = "节点端口", example = "20200")
    private Integer port;

    @ApiModelProperty(value = "所属群组ID", example = "group0")
    private String groupId;

    @ApiModelProperty(value = "是否为主节点", notes = "当前视图的主节点", example = "false")
    private Boolean isLeader;
}
