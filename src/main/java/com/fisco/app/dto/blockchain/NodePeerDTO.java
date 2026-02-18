package com.fisco.app.dto.blockchain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 节点P2P连接信息DTO
 *
 * @author FISCO BCOS Team
 * @since 2026-02-10
 */
@Data
@ApiModel(value = "节点P2P连接", description = "节点的对等连接信息")
public class NodePeerDTO {

    @ApiModelProperty(value = "节点ID")
    private String nodeId;

    @ApiModelProperty(value = "对等节点列表")
    private List<PeerInfo> peers;

    /**
     * 对等节点信息
     */
    @Data
    @ApiModel(value = "对等节点信息")
    public static class PeerInfo {

        @ApiModelProperty(value = "节点ID")
        private String nodeId;

        @ApiModelProperty(value = "IP地址（脱敏）")
        private String ipAddress;
    }
}
