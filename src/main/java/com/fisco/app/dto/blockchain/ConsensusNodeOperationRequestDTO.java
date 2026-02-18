package com.fisco.app.dto.blockchain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;
import java.math.BigInteger;

/**
 * 共识节点操作请求DTO
 * 用于添加或删除共识节点
 *
 * @author FISCO BCOS Team
 * @since 2026-02-10
 */
@Data
@ApiModel(value = "共识节点操作请求", description = "添加或删除共识节点的请求参数")
public class ConsensusNodeOperationRequestDTO {

    @NotBlank(message = "节点ID不能为空")
    @Size(max = 128, message = "节点ID长度不能超过128字符")
    @ApiModelProperty(value = "节点ID", required = true, example = "node_5")
    private String nodeId;

    @ApiModelProperty(
        value = "节点权重",
        notes = "仅添加SEALER节点时需要，默认值为1",
        example = "1"
    )
    @PositiveOrZero(message = "权重必须为非负整数")
    @Max(value = 1000000, message = "权重不能超过1000000")
    private BigInteger weight;

    @ApiModelProperty(
        value = "节点类型",
        notes = "SEALER-共识节点/OBSERVER-观察节点，默认为SEALER",
        example = "SEALER"
    )
    @Pattern(regexp = "^(SEALER|OBSERVER)$", message = "节点类型必须是SEALER或OBSERVER")
    private String nodeType;
}
