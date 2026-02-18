package com.fisco.app.controller.blockchain;

import com.fisco.app.dto.blockchain.*;
import com.fisco.app.service.blockchain.NodeService;
import com.fisco.app.vo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 节点管理控制器
 * 提供节点查询、状态监控、P2P连接管理、同步状态查询等REST API
 *
 * @author FISCO BCOS Team
 * @since 2026-02-10
 */
@RestController
@RequestMapping("/api/blockchain/node")
@RequiredArgsConstructor
@Api(tags = "NodeManagement")
@Slf4j
public class NodeController {

    private final NodeService nodeService;

    /**
     * 获取节点列表
     * GET /api/blockchain/node/list
     */
    @GetMapping("/list")
    @ApiOperation(value = "获取节点列表",
        notes = "获取区块链网络中的节点列表。" +
                "返回信息：" +
                "- 节点ID、IP地址（已脱敏）、端口号；" +
                "- 连接状态（CONNECTED/DISCONNECTED）；" +
                "- 节点角色（sealer/observer）；" +
                "- 所属群组ID。" +
                "安全说明：" +
                "- IP地址已脱敏处理（如：192.168.*.*）；" +
                "- 可通过配置blockchain.node.ip.mask.enabled控制是否脱敏。" +
                "【公开接口】无需认证即可访问。")
    @ApiResponses({
        @ApiResponse(code = 200, message = "获取成功", response = NodeDTO.class, responseContainer = "List"),
        @ApiResponse(code = 500, message = "服务器内部错误")
    })
    public Result<List<NodeDTO>> getNodeList() {
        try {
            List<NodeDTO> nodes = nodeService.getNodeList();
            return Result.success("获取节点列表成功", nodes);
        } catch (Exception e) {
            log.error("Failed to get node list", e);
            return Result.error("获取节点列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取节点统计信息
     * GET /api/blockchain/node/statistics
     */
    @GetMapping("/statistics")
    @ApiOperation(value = "获取节点统计信息",
        notes = "获取节点的统计信息，包括节点数量、群组信息等。" +
                "返回信息：" +
                "- 总节点数、在线节点数、离线节点数；" +
                "- 共识节点数（Sealer）、观察节点数（Observer）；" +
                "- 群组列表（群组ID、名称、状态、共识模式等）。" +
                "【公开接口】无需认证即可访问。")
    @ApiResponses({
        @ApiResponse(code = 200, message = "获取成功", response = NodeStatisticsDTO.class),
        @ApiResponse(code = 500, message = "服务器内部错误")
    })
    public Result<NodeStatisticsDTO> getNodeStatistics() {
        try {
            NodeStatisticsDTO stats = nodeService.getNodeStatistics();
            return Result.success("获取节点统计成功", stats);
        } catch (Exception e) {
            log.error("Failed to get node statistics", e);
            return Result.error("获取节点统计失败: " + e.getMessage());
        }
    }

    /**
     * 查询节点状态
     * GET /api/blockchain/node/{nodeId}/status
     */
    @GetMapping("/{nodeId}/status")
    @ApiOperation(value = "查询节点状态",
        notes = "查询指定节点的详细状态信息。" +
                "参数说明：" +
                "- nodeId: 节点ID（如：node_0、node_1等）；" +
                "返回信息：" +
                "- 节点ID、在线状态、当前区块高度；" +
                "- 节点版本、视图编号、运行时长；" +
                "- 最后更新时间。" +
                "【公开接口】无需认证即可访问。")
    @ApiResponses({
        @ApiResponse(code = 200, message = "获取成功", response = NodeStatusDTO.class),
        @ApiResponse(code = 400, message = "节点ID格式错误"),
        @ApiResponse(code = 404, message = "节点不存在"),
        @ApiResponse(code = 500, message = "服务器内部错误")
    })
    public Result<NodeStatusDTO> getNodeStatus(
            @ApiParam(value = "节点ID", required = true, example = "node_0") @PathVariable String nodeId) {
        try {
            NodeStatusDTO status = nodeService.getNodeStatus(nodeId);
            return Result.success("获取节点状态成功", status);
        } catch (Exception e) {
            log.error("Failed to get node status: nodeId={}", nodeId, e);
            return Result.error("获取节点状态失败: " + e.getMessage());
        }
    }

    /**
     * 查询节点P2P连接
     * GET /api/blockchain/node/{nodeId}/peers
     */
    @GetMapping("/{nodeId}/peers")
    @ApiOperation(value = "查询节点P2P连接",
        notes = "查询节点的对等连接（P2P）信息。" +
                "参数说明：" +
                "- nodeId: 节点ID；" +
                "返回信息：" +
                "- 节点ID；" +
                "- 对等节点列表（节点ID、IP地址等）。" +
                "安全说明：" +
                "- IP地址已脱敏处理。" +
                "【公开接口】无需认证即可访问。")
    @ApiResponses({
        @ApiResponse(code = 200, message = "获取成功", response = NodePeerDTO.class),
        @ApiResponse(code = 400, message = "节点ID格式错误"),
        @ApiResponse(code = 404, message = "节点不存在"),
        @ApiResponse(code = 500, message = "服务器内部错误")
    })
    public Result<NodePeerDTO> getNodePeers(
            @ApiParam(value = "节点ID", required = true, example = "node_0") @PathVariable String nodeId) {
        try {
            NodePeerDTO peers = nodeService.getNodePeers(nodeId);
            return Result.success("获取P2P连接成功", peers);
        } catch (Exception e) {
            log.error("Failed to get node peers: nodeId={}", nodeId, e);
            return Result.error("获取P2P连接失败: " + e.getMessage());
        }
    }

    /**
     * 查询同步状态
     * GET /api/blockchain/node/sync
     */
    @GetMapping("/sync")
    @ApiOperation(value = "查询同步状态",
        notes = "查询节点区块同步状态。" +
                "返回信息：" +
                "- 当前区块号、网络最高区块号；" +
                "- 同步进度（百分比）；" +
                "- 是否正在同步；" +
                "- 同步状态（SYNCED-已同步/SYNCING-同步中/BEHIND-落后）。" +
                "状态说明：" +
                "- SYNCED: 节点已同步到最新区块；" +
                "- SYNCING: 节点正在同步，落后区块数小于等于阈值（10）；" +
                "- BEHIND: 节点严重落后，需要加快同步。" +
                "【公开接口】无需认证即可访问。")
    @ApiResponses({
        @ApiResponse(code = 200, message = "获取成功", response = NodeSyncStatusDTO.class),
        @ApiResponse(code = 500, message = "服务器内部错误")
    })
    public Result<NodeSyncStatusDTO> getSyncStatus() {
        try {
            NodeSyncStatusDTO syncStatus = nodeService.getSyncStatus();
            return Result.success("获取同步状态成功", syncStatus);
        } catch (Exception e) {
            log.error("Failed to get sync status", e);
            return Result.error("获取同步状态失败: " + e.getMessage());
        }
    }

    /**
     * 查询群组列表
     * GET /api/blockchain/node/groups
     */
    @GetMapping("/groups")
    @ApiOperation(value = "查询群组列表",
        notes = "获取区块链网络的群组信息列表。" +
                "返回信息：" +
                "- 群组ID、群组名称；" +
                "- 群组状态（RUNNING/STOPPED）；" +
                "- 群组内的节点数量；" +
                "- 共识模式（pbft/rbft等）。" +
                "注意事项：" +
                "- FISCO BCOS SDK v3的Client绑定到特定群组；" +
                "- 本接口返回当前连接的群组信息。" +
                "【公开接口】无需认证即可访问。")
    @ApiResponses({
        @ApiResponse(code = 200, message = "获取成功", response = GroupInfoDTO.class, responseContainer = "List"),
        @ApiResponse(code = 500, message = "服务器内部错误")
    })
    public Result<List<GroupInfoDTO>> getGroupList() {
        try {
            List<GroupInfoDTO> groups = nodeService.getGroupList();
            return Result.success("获取群组列表成功", groups);
        } catch (Exception e) {
            log.error("Failed to get group list", e);
            return Result.error("获取群组列表失败: " + e.getMessage());
        }
    }

    /**
     * 查询群组详情
     * GET /api/blockchain/node/group/{groupId}
     */
    @GetMapping("/group/{groupId}")
    @ApiOperation(value = "查询群组详情",
        notes = "获取指定群组的详细信息。" +
                "参数说明：" +
                "- groupId: 群组ID（如：group0、group1等）；" +
                "返回信息：" +
                "- 群组ID、群组名称；" +
                "- 群组状态（RUNNING/STOPPED）；" +
                "- 群组内的节点数量；" +
                "- 共识模式（pbft/rbft等）；" +
                "- 平均出块时间（毫秒）。" +
                "【公开接口】无需认证即可访问。")
    @ApiResponses({
        @ApiResponse(code = 200, message = "获取成功", response = GroupInfoDTO.class),
        @ApiResponse(code = 400, message = "群组ID格式错误"),
        @ApiResponse(code = 404, message = "群组不存在"),
        @ApiResponse(code = 500, message = "服务器内部错误")
    })
    public Result<GroupInfoDTO> getGroupInfo(
            @ApiParam(value = "群组ID", required = true, example = "group0") @PathVariable String groupId) {
        try {
            GroupInfoDTO groupInfo = nodeService.getGroupInfo(groupId);
            return Result.success("获取群组信息成功", groupInfo);
        } catch (Exception e) {
            log.error("Failed to get group info: groupId={}", groupId, e);
            return Result.error("获取群组信息失败: " + e.getMessage());
        }
    }
}
