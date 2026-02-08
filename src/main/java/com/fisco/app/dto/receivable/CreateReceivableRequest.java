package com.fisco.app.dto.receivable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import lombok.Data;
import org.springframework.lang.NonNull;

/**
 * 创建应收账款请求DTO
 */
@Data
@ApiModel(value = "创建应收账款请求", description = "用于创建应收账款的请求参数")
@Schema(name = "创建应收账款请求")
public class CreateReceivableRequest {

    @NonNull
    @NotBlank(message = "应收账款ID不能为空")
    @ApiModelProperty(value = "应收账款ID（UUID格式）", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    @NotBlank(message = "核心企业地址不能为空")
    @ApiModelProperty(value = "核心企业地址", required = true, example = "0xabcdef1234567890")
    private String coreEnterpriseAddress;

    @NotNull(message = "应收金额不能为空")
    @Positive(message = "应收金额必须大于0")
    @ApiModelProperty(value = "应收金额", required = true, example = "500000.00")
    private java.math.BigDecimal amount;

    @ApiModelProperty(value = "币种", example = "CNY")
    private String currency = "CNY";

    @NotNull(message = "出票日期不能为空")
    @ApiModelProperty(value = "出票日期", required = true, example = "2024-01-13T10:00:00")
    private LocalDateTime issueDate;

    @NotNull(message = "到期日期不能为空")
    @ApiModelProperty(value = "到期日期", required = true, example = "2024-04-13T10:00:00")
    private LocalDateTime dueDate;

    @ApiModelProperty(value = "描述", example = "原材料采购款")
    private String description;
}
