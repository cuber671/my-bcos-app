package com.fisco.app.dto.endorsement;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.springframework.lang.NonNull;

import java.math.BigDecimal;

import javax.validation.constraints.*;

/**
 * 创建背书请求DTO
 */
@Data
@ApiModel(value = "创建背书请求", description = "创建背书请求参数")
public class EwrEndorsementCreateRequest {

    @ApiModelProperty(value = "仓单ID", required = true, example = "ewr-uuid-001")
    @NonNull
    @NotBlank(message = "仓单ID不能为空")
    @Size(max = 36, message = "仓单ID长度不能超过36")
    private String receiptId;

    @ApiModelProperty(value = "背书企业地址（转出方）", required = true, example = "0xabcdabcdabcdabcdabcdabcdabcdabcdabcdabcd")
    @NonNull
    @NotBlank(message = "背书企业地址不能为空")
    @Size(min = 42, max = 42, message = "背书企业地址必须是42位")
    @Pattern(regexp = "^0x[a-fA-F0-9]{40}$", message = "背书企业地址格式不正确")
    private String endorseFrom;

    @ApiModelProperty(value = "被背书企业地址（转入方）", required = true, example = "0x1234567890abcdef1234567890abcdef12345678")
    @NonNull
    @NotBlank(message = "被背书企业地址不能为空")
    @Size(min = 42, max = 42, message = "被背书企业地址必须是42位")
    @Pattern(regexp = "^0x[a-fA-F0-9]{40}$", message = "被背书企业地址格式不正确")
    private String endorseTo;

    @ApiModelProperty(value = "被背书企业名称", example = "YY物流有限公司")
    @Size(max = 255, message = "企业名称长度不能超过255")
    private String endorseToName;

    @ApiModelProperty(value = "背书类型", required = true, example = "TRANSFER", notes = "TRANSFER-转让, PLEDGE-质押, RELEASE-解押, CANCEL-撤销")
    @NotBlank(message = "背书类型不能为空")
    @Pattern(regexp = "^(TRANSFER|PLEDGE|RELEASE|CANCEL)$", message = "背书类型不正确")
    private String endorsementType;

    @ApiModelProperty(value = "背书原因说明", example = "货物所有权转让")
    @Size(max = 500, message = "背书原因长度不能超过500")
    private String endorsementReason;

    @ApiModelProperty(value = "转让价格（元）", example = "4600.00")
    @DecimalMin(value = "0", message = "转让价格不能为负数")
    @Digits(integer = 18, fraction = 2, message = "转让价格格式不正确")
    private BigDecimal transferPrice;

    @ApiModelProperty(value = "转让金额", example = "4600000.00")
    @DecimalMin(value = "0", message = "转让金额不能为负数")
    @Digits(integer = 18, fraction = 2, message = "转让金额格式不正确")
    private BigDecimal transferAmount;

    @ApiModelProperty(value = "转入方经手人ID", example = "user-uuid-002")
    @Size(max = 36, message = "经手人ID长度不能超过36")
    private String operatorToId;

    @ApiModelProperty(value = "转入方经手人姓名", example = "赵六（转入企业员工）")
    @Size(max = 100, message = "经手人姓名长度不能超过100")
    private String operatorToName;

    @ApiModelProperty(value = "备注信息", example = "备注：背书协议已签署")
    @Size(max = 1000, message = "备注长度不能超过1000")
    private String remarks;
}
