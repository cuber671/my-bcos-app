package com.fisco.app.dto.endorsement;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.springframework.lang.NonNull;


/**
 * 确认背书请求DTO
 */
@Data
@ApiModel(value = "确认背书请求", description = "确认背书请求参数")
public class EwrEndorsementConfirmRequest {

    @ApiModelProperty(value = "背书ID", required = true, example = "endorsement-uuid-001")
    @NonNull
    @NotBlank(message = "背书ID不能为空")
    @Size(max = 36, message = "背书ID长度不能超过36")
    private String id;

    @ApiModelProperty(value = "背书编号", required = true, example = "END20260126000001")
    @NotBlank(message = "背书编号不能为空")
    @Size(max = 64, message = "背书编号长度不能超过64")
    private String endorsementNo;

    @ApiModelProperty(value = "确认状态", required = true, example = "CONFIRMED", notes = "CONFIRMED-确认, CANCELLED-取消")
    @NotBlank(message = "确认状态不能为空")
    @Pattern(regexp = "^(CONFIRMED|CANCELLED)$", message = "确认状态不正确")
    private String confirmStatus;

    @ApiModelProperty(value = "备注信息", example = "备注：已核实背书信息")
    @Size(max = 1000, message = "备注长度不能超过1000")
    private String remarks;
}
