package com.fisco.app.dto.endorsement;

import com.fisco.app.entity.bill.Endorsement;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotBlank;

/**
 * 票据背书请求DTO
 */
@Data
@ApiModel(value = "票据背书请求", description = "用于背书转让票据的请求参数")
@Schema(name = "票据背书请求")
public class EndorseBillRequest {

    @NotBlank(message = "被背书人地址不能为空")
    @ApiModelProperty(value = "被背书人地址（新持票人）", required = true, example = "0x1234567890abcdef")
    private String endorseeAddress;

    @ApiModelProperty(value = "背书类型", required = true, example = "NORMAL")
    private Endorsement.EndorsementType endorsementType = Endorsement.EndorsementType.NORMAL;

    @ApiModelProperty(value = "背书金额（分，部分背书时使用，null表示全额背书）", example = "100000000")
    private Long endorsementAmount;

    @ApiModelProperty(value = "背书备注", example = "转让给XX公司")
    private String remark;
}
