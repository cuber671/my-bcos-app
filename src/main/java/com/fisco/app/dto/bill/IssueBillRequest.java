package com.fisco.app.dto.bill;

import com.fisco.app.entity.bill.Bill;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import lombok.Data;
import org.springframework.lang.NonNull;


/**
 * 开票请求DTO
 */
@Data
@ApiModel(value = "开票请求", description = "用于创建票据的请求参数")
@Schema(name = "开票请求")
public class IssueBillRequest {

    @NonNull
    @NotBlank(message = "票据ID不能为空")
    @ApiModelProperty(value = "票据ID（UUID格式）", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    @NotNull(message = "票据类型不能为空")
    @ApiModelProperty(value = "票据类型", required = true, example = "COMMERCIAL_BILL")
    private Bill.BillType billType;

    @NotBlank(message = "承兑人地址不能为空")
    @ApiModelProperty(value = "承兑人地址", required = true, example = "0xabcdef1234567890")
    private String acceptorAddress;

    @NotBlank(message = "受益人地址不能为空")
    @ApiModelProperty(value = "受益人地址", required = true, example = "0x1234567890abcdef")
    private String beneficiaryAddress;

    @NotNull(message = "票面金额不能为空")
    @Positive(message = "票面金额必须大于0")
    @ApiModelProperty(value = "票面金额", required = true, example = "1000000.00")
    private BigDecimal amount;

    @ApiModelProperty(value = "币种", example = "CNY")
    private String currency = "CNY";

    @NotNull(message = "出票日期不能为空")
    @ApiModelProperty(value = "出票日期", required = true, example = "2024-01-13T10:00:00")
    private LocalDateTime issueDate;

    @NotNull(message = "到期日期不能为空")
    @ApiModelProperty(value = "到期日期", required = true, example = "2024-07-13T10:00:00")
    private LocalDateTime dueDate;

    @ApiModelProperty(value = "描述", example = "货物采购款")
    private String description;
}
