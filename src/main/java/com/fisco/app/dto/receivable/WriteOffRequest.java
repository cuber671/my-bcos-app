package com.fisco.app.dto.receivable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 坏账核销请求DTO
 *
 * 对无法回收的坏账进行财务核销处理
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-09
 * @version 1.0
 */
@Data
@ApiModel(value = "坏账核销请求", description = "应收账款坏账核销请求参数")
public class WriteOffRequest {

    @ApiModelProperty(value = "核销原因", required = true, example = "逾期180天以上，债务人失联")
    @NotBlank(message = "核销原因不能为空")
    @Size(max = 500, message = "核销原因长度不能超过500")
    private String reason;

    @ApiModelProperty(value = "核销金额（元）", required = true, example = "500000.00")
    @javax.validation.constraints.NotNull(message = "核销金额不能为空")
    @javax.validation.constraints.Digits(integer = 18, fraction = 2, message = "核销金额格式不正确")
    private BigDecimal writeOffAmount;

    @ApiModelProperty(value = "备注", example = "经法务部门确认，已启动法律程序")
    @Size(max = 1000, message = "备注长度不能超过1000")
    private String remark;
}
