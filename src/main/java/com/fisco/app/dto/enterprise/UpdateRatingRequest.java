package com.fisco.app.dto.enterprise;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 更新信用评级请求DTO
 */
@Data
@ApiModel(value = "更新评级请求", description = "管理员更新企业信用评级的请求参数")
public class UpdateRatingRequest {

    @NotNull(message = "信用评级不能为空")
    @Min(value = 0, message = "信用评级不能小于0")
    @Max(value = 100, message = "信用评级不能大于100")
    @ApiModelProperty(value = "新信用评级(0-100)", required = true, example = "75", notes = "0-100分，分数越高信用越好")
    private Integer creditRating;

    @Size(max = 500, message = "变更原因不能超过500个字符")
    @ApiModelProperty(value = "变更原因", example = "按时还款记录良好，经营状况稳定", notes = "可选，建议提供详细的评级变更原因以便追溯")
    private String reason;
}
