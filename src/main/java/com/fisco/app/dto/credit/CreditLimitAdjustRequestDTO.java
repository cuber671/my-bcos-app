package com.fisco.app.dto.credit;

import com.fisco.app.enums.CreditAdjustType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;
import org.springframework.lang.NonNull;

/**
 * 信用额度调整申请请求DTO
 */
@Data
@ApiModel(value = "信用额度调整申请请求", description = "用于申请调整企业信用额度的请求参数")
public class CreditLimitAdjustRequestDTO {

    @ApiModelProperty(value = "额度ID", required = true, example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    @NotBlank(message = "额度ID不能为空")
    @Getter(onMethod_ = @__({@NonNull}))
    private String creditLimitId;

    @ApiModelProperty(value = "调整类型", required = true, notes = "INCREASE-增加额度, DECREASE-减少额度, RESET-重置额度", example = "INCREASE")
    @NotNull(message = "调整类型不能为空")
    @Getter(onMethod_ = @__({@NonNull}))
    private CreditAdjustType adjustType;

    @ApiModelProperty(value = "调整后额度（元）", required = true, example = "1500000.00", notes = "系统会自动转换为分存储")
    @NotNull(message = "调整后额度不能为空")
    @DecimalMin(value = "0.01", message = "调整后额度必须大于0")
    @Getter(onMethod_ = @__({@NonNull}))
    private java.math.BigDecimal newLimit;

    @ApiModelProperty(value = "申请原因", required = true, example = "企业信用评级提升，申请增加融资额度")
    @NotBlank(message = "申请原因不能为空")
    @Size(max = 1000, message = "申请原因长度不能超过1000")
    @Getter(onMethod_ = @__({@NonNull}))
    private String requestReason;

    @ApiModelProperty(value = "申请人地址（系统自动获取，无需传入）", hidden = true)
    private String requesterAddress;
}
