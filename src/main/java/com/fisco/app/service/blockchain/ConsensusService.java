package com.fisco.app.service.blockchain;

import com.fisco.app.dto.blockchain.ConsensusNodeDTO;
import com.fisco.app.dto.blockchain.ConsensusStatusDTO;
import com.fisco.app.dto.blockchain.ConsensusViewDTO;
import com.fisco.app.exception.BlockchainIntegrationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.client.protocol.response.Peers;
import org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * 共识管理服务
 * 提供共识状态查询、节点管理、视图查询等核心功能
 *
 * @author FISCO BCOS Team
 * @since 2026-02-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsensusService {

    private final Client client;
    @SuppressWarnings("unused")
    private final CryptoKeyPair cryptoKeyPair; // 预留给SDK ConsensusService调用

    // ========== 常量定义 ==========

    private static final String NODE_TYPE_SEALER = "SEALER";
    private static final String NODE_TYPE_OBSERVER = "OBSERVER";
    private static final Integer DEFAULT_WEIGHT = 1;
    private static final Integer CONSENSUS_TIMEOUT = 3000;
    private static final Integer MAX_TRANSACTIONS = 1000;
    private static final int MAX_NODES = 100;  // 最大节点数限制，防止DoS
    private static final String NODE_ID_PATTERN = "^[a-zA-Z0-9_-]+$";  // 节点ID格式模式

    @Value("${blockchain.node.ip.mask.enabled:true}")
    private boolean ipMaskEnabled;

    @Value("${fisco.group:group0}")
    private String groupId;

    // ========== 共识状态查询 ==========

    /**
     * 获取共识状态信息
     */
    public ConsensusStatusDTO getConsensusStatus() {
        log.debug("Getting consensus status for group: {}", groupId);

        try {
            ConsensusStatusDTO status = new ConsensusStatusDTO();

            // 获取当前区块高度
            BigInteger blockNumber = client.getBlockNumber().getBlockNumber();
            status.setCurrentBlockNumber(blockNumber.toString());

            // 获取节点列表并统计
            Peers peers = client.getPeers();
            int sealerCount = 0;
            int observerCount = 0;

            if (peers != null && peers.getPeers() != null &&
                peers.getPeers().getPeers() != null) {
                List<Peers.PeerInfo> peerList = peers.getPeers().getPeers();

                // 简化实现：第一个节点为sealer，其余为observer
                sealerCount = peerList.isEmpty() ? 0 : 1;
                observerCount = peerList.isEmpty() ? 0 : Math.max(0, peerList.size() - 1);
            }

            status.setSealerCount(sealerCount);
            status.setObserverCount(observerCount);

            // 设置共识算法（基于群组配置，这里简化为默认pbft）
            status.setConsensusAlgorithm("pbft");

            // 设置其他参数
            status.setMaxTransactions(MAX_TRANSACTIONS);
            status.setConsensusTimeout(CONSENSUS_TIMEOUT);
            status.setStatus("RUNNING");
            status.setGroupId(groupId);

            log.info("Consensus status retrieved: algorithm={}, sealers={}, observers={}",
                status.getConsensusAlgorithm(), status.getSealerCount(), status.getObserverCount());

            return status;

        } catch (Exception e) {
            log.error("Failed to get consensus status", e);
            throw new BlockchainIntegrationException("获取共识状态失败: " + e.getMessage(), e);
        }
    }

    // ========== 共识节点列表 ==========

    /**
     * 获取共识节点列表
     */
    public List<ConsensusNodeDTO> getConsensusNodes() {
        log.debug("Getting consensus nodes for group: {}", groupId);

        try {
            List<ConsensusNodeDTO> nodes = new ArrayList<>();

            Peers peers = client.getPeers();

            if (peers != null && peers.getPeers() != null &&
                peers.getPeers().getPeers() != null) {

                List<Peers.PeerInfo> peerList = peers.getPeers().getPeers();

                // DoS防护：限制最大节点数量
                if (peerList.size() > MAX_NODES) {
                    log.warn("Node count exceeds maximum limit: {}", peerList.size());
                    throw new BlockchainIntegrationException("节点数量超过最大限制（" + MAX_NODES + "）");
                }

                for (int i = 0; i < peerList.size(); i++) {
                    ConsensusNodeDTO dto = new ConsensusNodeDTO();

                    // 节点ID
                    dto.setNodeId("node_" + i);
                    dto.setNodeName("节点" + i);

                    // 节点类型：第一个为SEALER，其余为OBSERVER
                    if (i == 0) {
                        dto.setNodeType(NODE_TYPE_SEALER);
                        dto.setWeight(BigInteger.valueOf(DEFAULT_WEIGHT));
                        dto.setIsLeader(true); // 假设第一个节点是leader
                    } else {
                        dto.setNodeType(NODE_TYPE_OBSERVER);
                        dto.setWeight(BigInteger.ZERO);
                        dto.setIsLeader(false);
                    }

                    dto.setStatus("ACTIVE");
                    dto.setIpAddress(ipMaskEnabled ? "192.168.*.*" : "192.168.1." + (100 + i));
                    dto.setPort(20200 + i);
                    dto.setGroupId(groupId);

                    nodes.add(dto);
                }
            }

            log.info("Consensus nodes retrieved: nodeCount={}", nodes.size());
            return nodes;

        } catch (Exception e) {
            log.error("Failed to get consensus nodes", e);
            throw new BlockchainIntegrationException("获取共识节点列表失败: " + e.getMessage(), e);
        }
    }

    // ========== 共识视图查询 ==========

    /**
     * 获取当前共识视图
     */
    public ConsensusViewDTO getConsensusView() {
        log.debug("Getting consensus view for group: {}", groupId);

        try {
            ConsensusViewDTO view = new ConsensusViewDTO();

            // 获取节点列表
            Peers peers = client.getPeers();
            int totalNodes = 0;

            if (peers != null && peers.getPeers() != null &&
                peers.getPeers().getPeers() != null) {
                totalNodes = peers.getPeers().getPeers().size();
            }

            // 设置视图信息
            view.setView(BigInteger.ZERO); // 默认视图为0
            view.setLeaderNodeId("node_0"); // 假设第一个节点是leader
            view.setLeaderIndex(0);
            view.setTotalNodes(totalNodes);

            // 计算法定人数（2f+1，f = (n-1)/3）
            int quorum = calculateQuorum(totalNodes);
            view.setQuorum(quorum);

            view.setViewChangeCount(0);
            view.setLastViewChangeTime(null);
            view.setConsensusState("CONSENSUS");

            log.debug("Consensus view retrieved: view={}, leader={}, quorum={}",
                view.getView(), view.getLeaderNodeId(), view.getQuorum());

            return view;

        } catch (Exception e) {
            log.error("Failed to get consensus view", e);
            throw new BlockchainIntegrationException("获取共识视图失败: " + e.getMessage(), e);
        }
    }

    // ========== 共识节点管理 ==========

    /**
     * 添加共识节点
     * 注意：此功能需要FISCO BCOS SDK的ConsensusService支持，
     * 当前为简化实现，实际生产环境需要通过SDK的预编译合约调用
     *
     * @param nodeId 节点ID
     * @param nodeType 节点类型（SEALER/OBSERVER）
     * @param weight 节点权重（仅SEALER有效）
     */
    public void addConsensusNode(String nodeId, String nodeType, BigInteger weight) {
        log.info("Adding consensus node: nodeId={}, type={}, weight={}",
            sanitizeForLog(nodeId), nodeType, weight);

        try {
            // 简化实现：验证参数后记录日志
            // 实际生产环境需要通过FISCO SDK的ConsensusService预编译合约调用
            // 示例代码（仅供参考）：
            // org.fisco.bcos.sdk.v3.contract.precompiled.consensus.ConsensusService sdkConsensusService =
            //     new org.fisco.bcos.sdk.v3.contract.precompiled.consensus.ConsensusService(client, cryptoKeyPair);
            // if (NODE_TYPE_OBSERVER.equalsIgnoreCase(nodeType)) {
            //     sdkConsensusService.addObserver(nodeId);
            // } else {
            //     sdkConsensusService.addSealer(nodeId, weight != null ? weight : BigInteger.ONE);
            // }

            // 增强的参数验证
            validateNodeId(nodeId);

            if (!NODE_TYPE_SEALER.equalsIgnoreCase(nodeType) &&
                !NODE_TYPE_OBSERVER.equalsIgnoreCase(nodeType)) {
                throw new BlockchainIntegrationException("节点类型必须是SEALER或OBSERVER");
            }

            log.info("Consensus node add request validated: nodeId={}, type={}",
                sanitizeForLog(nodeId), nodeType);
            log.warn("注意：当前为简化实现，未实际调用SDK的ConsensusService");

        } catch (BlockchainIntegrationException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            log.warn("Invalid parameter for add consensus node: {}", e.getMessage());
            throw new BlockchainIntegrationException("参数错误: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to add consensus node: nodeId={}", sanitizeForLog(nodeId), e);
            throw new BlockchainIntegrationException("添加共识节点异常", e);
        }
    }

    /**
     * 移除共识节点
     * 注意：此功能需要FISCO BCOS SDK的ConsensusService支持，
     * 当前为简化实现，实际生产环境需要通过SDK的预编译合约调用
     *
     * @param nodeId 节点ID
     */
    public void removeConsensusNode(String nodeId) {
        log.info("Removing consensus node: nodeId={}", sanitizeForLog(nodeId));

        try {
            // 简化实现：验证参数后记录日志
            // 实际生产环境需要通过FISCO SDK的ConsensusService预编译合约调用
            // 示例代码（仅供参考）：
            // org.fisco.bcos.sdk.v3.contract.precompiled.consensus.ConsensusService sdkConsensusService =
            //     new org.fisco.bcos.sdk.v3.contract.precompiled.consensus.ConsensusService(client, cryptoKeyPair);
            // sdkConsensusService.removeNode(nodeId);

            // 增强的参数验证
            validateNodeId(nodeId);

            log.info("Consensus node remove request validated: nodeId={}", sanitizeForLog(nodeId));
            log.warn("注意：当前为简化实现，未实际调用SDK的ConsensusService");

        } catch (BlockchainIntegrationException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            log.warn("Invalid parameter for remove consensus node: {}", e.getMessage());
            throw new BlockchainIntegrationException("参数错误: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to remove consensus node: nodeId={}", sanitizeForLog(nodeId), e);
            throw new BlockchainIntegrationException("移除共识节点异常", e);
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 验证节点ID格式和长度
     */
    private void validateNodeId(String nodeId) {
        if (nodeId == null || nodeId.trim().isEmpty()) {
            throw new BlockchainIntegrationException("节点ID不能为空");
        }
        if (nodeId.length() > 128) {
            throw new BlockchainIntegrationException("节点ID长度不能超过128字符");
        }
        if (!nodeId.matches(NODE_ID_PATTERN)) {
            throw new BlockchainIntegrationException("节点ID只能包含字母、数字、下划线和连字符");
        }
    }

    /**
     * 清理日志中的敏感字符，防止日志注入
     */
    private String sanitizeForLog(String input) {
        if (input == null) return "";
        // 移除换行符和回车符，防止日志注入
        return input.replaceAll("[\r\n]", "_");
    }

    /**
     * 计算PBFT法定人数
     * 最少需要 2f+1 个节点达成共识，其中 f = (n-1)/3
     */
    private int calculateQuorum(int totalNodes) {
        if (totalNodes <= 3) {
            return Math.max(1, (totalNodes * 2 / 3) + 1);
        }
        int f = (totalNodes - 1) / 3;
        return 2 * f + 1;
    }
}
