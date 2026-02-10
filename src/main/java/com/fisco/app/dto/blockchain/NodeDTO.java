package com.fisco.app.dto.blockchain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 节点信息DTO
 *
 * @author FISCO BCOS Team
 * @since 2026-02-10
 */
@Data
@ApiModel(value = "节点信息", description = "区块链节点基本信息")
public class NodeDTO {

    @ApiModelProperty(value = "节点ID")
    private String nodeId;

    @ApiModelProperty(value = "节点IP地址（脱敏）", example = "192.168.*.*")
    private String ipAddress;

    @ApiModelProperty(value = "节点端口")
    private Integer port;

    @ApiModelProperty(value = "连接状态", notes = "CONNECTED/DISCONNECTED")
    private String connectionStatus;

    @ApiModelProperty(value = "节点角色", notes = "sealer/observer")
    private String role;

    @ApiModelProperty(value = "群组ID", example = "group0")
    private String groupId;
}
