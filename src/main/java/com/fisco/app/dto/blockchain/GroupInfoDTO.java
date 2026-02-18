package com.fisco.app.dto.blockchain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 群组信息DTO
 *
 * @author FISCO BCOS Team
 * @since 2026-02-10
 */
@Data
@ApiModel(value = "群组信息", description = "FISCO BCOS群组信息")
public class GroupInfoDTO {

    @ApiModelProperty(value = "群组ID", example = "group0")
    private String groupId;

    @ApiModelProperty(value = "群组名称")
    private String groupName;

    @ApiModelProperty(value = "群组状态", notes = "RUNNING/STOPPED")
    private String groupStatus;

    @ApiModelProperty(value = "节点数量")
    private Integer nodeCount;

    @ApiModelProperty(value = "共识模式", notes = "pbft/rbft")
    private String consensusMode;

    @ApiModelProperty(value = "区块生成时间（毫秒）")
    private Integer blockGenerationTime;
}
