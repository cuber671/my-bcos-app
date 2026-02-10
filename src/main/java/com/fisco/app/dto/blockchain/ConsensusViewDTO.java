package com.fisco.app.dto.blockchain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigInteger;

/**
 * 共识视图DTO
 * 描述PBFT/RBFT共识的视图信息
 *
 * @author FISCO BCOS Team
 * @since 2026-02-10
 */
@Data
@ApiModel(value = "共识视图", description = "共识机制当前的视图信息")
public class ConsensusViewDTO {

    @ApiModelProperty(value = "当前视图编号", example = "0")
    private BigInteger view;

    @ApiModelProperty(value = "主节点ID", example = "node_0")
    private String leaderNodeId;

    @ApiModelProperty(value = "主节点索引", example = "0")
    private Integer leaderIndex;

    @ApiModelProperty(value = "总节点数", example = "4")
    private Integer totalNodes;

    @ApiModelProperty(value = "法定人数", notes = "达成共识的最小节点数", example = "3")
    private Integer quorum;

    @ApiModelProperty(value = "视图切换次数", example = "0")
    private Integer viewChangeCount;

    @ApiModelProperty(value = "上次视图切换时间", example = "2026-02-10T15:30:00")
    private String lastViewChangeTime;

    @ApiModelProperty(value = "共识状态", notes = "CONSENSUS-共识中/VIEW_CHANGE-视图切换中", example = "CONSENSUS")
    private String consensusState;
}
