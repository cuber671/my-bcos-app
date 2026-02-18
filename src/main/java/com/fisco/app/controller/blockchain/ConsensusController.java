package com.fisco.app.controller.blockchain;

import com.fisco.app.dto.blockchain.ConsensusNodeDTO;
import com.fisco.app.dto.blockchain.ConsensusNodeOperationRequestDTO;
import com.fisco.app.dto.blockchain.ConsensusStatusDTO;
import com.fisco.app.dto.blockchain.ConsensusViewDTO;
import com.fisco.app.service.blockchain.ConsensusService;
import com.fisco.app.vo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigInteger;
import java.util.List;

/**
 * 共识管理控制器
 * 提供共识状态查询、节点管理、视图查询等REST API
 *
 * @author FISCO BCOS Team
 * @since 2026-02-10
 */
@RestController
@RequestMapping("/api/blockchain/consensus")
@RequiredArgsConstructor
@Api(tags = "ConsensusManagement")
@Slf4j
public class ConsensusController {

    private final ConsensusService consensusService;

    /**
     * 查询共识状态
     * GET /api/blockchain/consensus/status
     */
    @GetMapping("/status")
    @ApiOperation(value = "查询共识状态",
        notes = "获取区块链网络的共识机制状态。" +
                "返回信息：" +
                "- 共识算法（PBFT/RBFT）；" +
                "- 共识节点数量、观察节点数量；" +
                "- 区块最大交易数、共识超时时间；" +
                "- 当前区块高度、共识状态。" +
                "【公开接口】无需认证即可访问。")
    @ApiResponses({
        @ApiResponse(code = 200, message = "获取成功", response = ConsensusStatusDTO.class),
        @ApiResponse(code = 500, message = "服务器内部错误")
    })
    public Result<ConsensusStatusDTO> getConsensusStatus() {
        try {
            ConsensusStatusDTO status = consensusService.getConsensusStatus();
            return Result.success("获取共识状态成功", status);
        } catch (Exception e) {
            log.error("Failed to get consensus status", e);
            return Result.error("获取共识状态失败，请稍后重试");
        }
    }

    /**
     * 查询共识节点列表
     * GET /api/blockchain/consensus/nodes
     */
    @GetMapping("/nodes")
    @ApiOperation(value = "查询共识节点列表",
        notes = "获取参与共识的节点列表。" +
                "返回信息：" +
                "- 节点ID、节点名称；" +
                "- 节点类型（SEALER/OBSERVER）；" +
                "- 节点权重（仅SEALER有效）；" +
                "- 节点状态、IP地址（脱敏）、端口；" +
                "- 是否为主节点。" +
                "安全说明：" +
                "- IP地址已脱敏处理；" +
                "- 显示当前群组的所有共识相关节点。" +
                "【公开接口】无需认证即可访问。")
    @ApiResponses({
        @ApiResponse(code = 200, message = "获取成功", response = ConsensusNodeDTO.class, responseContainer = "List"),
        @ApiResponse(code = 500, message = "服务器内部错误")
    })
    public Result<List<ConsensusNodeDTO>> getConsensusNodes() {
        try {
            List<ConsensusNodeDTO> nodes = consensusService.getConsensusNodes();
            return Result.success("获取共识节点列表成功", nodes);
        } catch (Exception e) {
            log.error("Failed to get consensus nodes", e);
            return Result.error("获取共识节点列表失败，请稍后重试");
        }
    }

    /**
     * 查询当前视图
     * GET /api/blockchain/consensus/view
     */
    @GetMapping("/view")
    @ApiOperation(value = "查询当前视图",
        notes = "获取PBFT/RBFT共识的当前视图信息。" +
                "返回信息：" +
                "- 当前视图编号；" +
                "- 主节点ID和主节点索引；" +
                "- 总节点数、法定人数；" +
                "- 视图切换次数、上次切换时间；" +
                "- 共识状态（CONSENSUS/VIEW_CHANGE）。" +
                "概念说明：" +
                "- 视图：PBFT协议中的概念，每个视图有一个主节点；" +
                "- 视图切换：当主节点故障时，会触发视图切换。" +
                "【公开接口】无需认证即可访问。")
    @ApiResponses({
        @ApiResponse(code = 200, message = "获取成功", response = ConsensusViewDTO.class),
        @ApiResponse(code = 500, message = "服务器内部错误")
    })
    public Result<ConsensusViewDTO> getConsensusView() {
        try {
            ConsensusViewDTO view = consensusService.getConsensusView();
            return Result.success("获取共识视图成功", view);
        } catch (Exception e) {
            log.error("Failed to get consensus view", e);
            return Result.error("获取共识视图失败，请稍后重试");
        }
    }

    /**
     * 添加共识节点
     * POST /api/blockchain/consensus/add-node
     */
    @PostMapping("/add-node")
    @ApiOperation(value = "添加共识节点",
        notes = "向群组中添加新的共识节点或观察节点。" +
                "参数说明：" +
                "- nodeId: 节点ID（必填）；" +
                "- nodeType: 节点类型（可选，SEALER/OBSERVER，默认SEALER）；" +
                "- weight: 节点权重（可选，仅SEALER有效，默认1）。" +
                "注意事项：" +
                "- 添加SEALER节点需要满足最小共识节点数要求；" +
                "- 操作需要交易确认，不是立即生效；" +
                "- OBSERVER节点不参与共识，只同步数据。" +
                "安全提示：" +
                "- 此操作会改变网络拓扑，需谨慎操作；" +
                "- 建议在低峰期执行节点变更操作。" +
                "权限要求：" +
                "- 需要群组管理员权限。" +
                "注意：当前为简化实现，未实际调用SDK的ConsensusService。")
    @ApiResponses({
        @ApiResponse(code = 200, message = "添加成功"),
        @ApiResponse(code = 400, message = "参数错误"),
        @ApiResponse(code = 401, message = "未授权"),
        @ApiResponse(code = 403, message = "权限不足"),
        @ApiResponse(code = 500, message = "服务器内部错误")
    })
    public Result<String> addConsensusNode(
            @ApiParam(value = "节点操作请求", required = true)
            @Valid @RequestBody ConsensusNodeOperationRequestDTO request) {

        try {
            // 参数验证和默认值设置
            String nodeType = request.getNodeType();
            if (nodeType == null || nodeType.trim().isEmpty()) {
                nodeType = "SEALER";
            }

            BigInteger weight = request.getWeight();
            if (weight == null && "SEALER".equalsIgnoreCase(nodeType)) {
                weight = BigInteger.ONE;
            }

            log.info("Request to add consensus node: nodeId={}, type={}, weight={}",
                sanitizeForLog(request.getNodeId()), nodeType, weight);

            // 调用服务层添加节点
            consensusService.addConsensusNode(request.getNodeId(), nodeType, weight);

            return Result.success("添加共识节点请求已提交，等待交易确认");

        } catch (IllegalArgumentException e) {
            log.warn("Invalid parameter for add consensus node: {}", e.getMessage());
            return Result.paramError("参数错误");
        } catch (Exception e) {
            log.error("Failed to add consensus node: nodeId={}", sanitizeForLog(request.getNodeId()), e);
            return Result.error("添加共识节点失败，请稍后重试");
        }
    }

    /**
     * 移除共识节点
     * DELETE /api/blockchain/consensus/remove-node
     */
    @DeleteMapping("/remove-node")
    @ApiOperation(value = "移除共识节点",
        notes = "从群组中移除共识节点或观察节点。" +
                "参数说明：" +
                "- nodeId: 要移除的节点ID（必填）。" +
                "注意事项：" +
                "- 移除节点后需保证剩余节点满足最小共识要求（2f+1）；" +
                "- 不能移除所有共识节点，至少保留一个；" +
                "- 操作需要交易确认，不是立即生效；" +
                "- 节点移除后会停止参与共识，但可能仍在P2P网络中。" +
                "安全提示：" +
                "- 此操作会影响网络可用性，需谨慎；" +
                "- 建议先确保节点数据已同步；" +
                "- 生产环境操作前应做好数据备份。" +
                "权限要求：" +
                "- 需要群组管理员权限。" +
                "注意：当前为简化实现，未实际调用SDK的ConsensusService。")
    @ApiResponses({
        @ApiResponse(code = 200, message = "移除成功"),
        @ApiResponse(code = 400, message = "参数错误"),
        @ApiResponse(code = 401, message = "未授权"),
        @ApiResponse(code = 403, message = "权限不足"),
        @ApiResponse(code = 500, message = "服务器内部错误")
    })
    public Result<String> removeConsensusNode(
            @ApiParam(value = "节点ID", required = true, example = "node_5")
            @RequestParam String nodeId) {

        try {
            log.info("Request to remove consensus node: nodeId={}", sanitizeForLog(nodeId));

            // 调用服务层移除节点
            consensusService.removeConsensusNode(nodeId);

            return Result.success("移除共识节点请求已提交，等待交易确认");

        } catch (IllegalArgumentException e) {
            log.warn("Invalid parameter for remove consensus node: {}", e.getMessage());
            return Result.paramError("参数错误");
        } catch (Exception e) {
            log.error("Failed to remove consensus node: nodeId={}", sanitizeForLog(nodeId), e);
            return Result.error("移除共识节点失败，请稍后重试");
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 清理日志中的敏感字符，防止日志注入
     */
    private String sanitizeForLog(String input) {
        if (input == null) return "";
        // 移除换行符和回车符，防止日志注入
        return input.replaceAll("[\r\n]", "_");
    }
}
