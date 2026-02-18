package com.fisco.app.dto.endorsement;

import com.fisco.app.entity.bill.Endorsement;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 票据背书响应DTO
 */
@Data
@ApiModel(value = "票据背书响应", description = "票据背书操作的结果")
@Schema(name = "票据背书响应")
public class EndorsementResponse {

    @ApiModelProperty(value = "背书记录ID")
    private String id;

    @ApiModelProperty(value = "票据ID")
    private String billId;

    @ApiModelProperty(value = "背书人地址")
    private String endorserAddress;

    @ApiModelProperty(value = "被背书人地址")
    private String endorseeAddress;

    @ApiModelProperty(value = "背书类型")
    private Endorsement.EndorsementType endorsementType;

    @ApiModelProperty(value = "背书金额（分）")
    private Long endorsementAmount;

    @ApiModelProperty(value = "背书日期")
    private LocalDateTime endorsementDate;

    @ApiModelProperty(value = "背书序号")
    private Integer endorsementSequence;

    @ApiModelProperty(value = "区块链交易哈希")
    private String txHash;

    @ApiModelProperty(value = "背书备注")
    private String remark;
}
