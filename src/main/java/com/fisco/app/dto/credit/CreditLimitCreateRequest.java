package com.fisco.app.dto.credit;

import com.fisco.app.enums.CreditLimitType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDateTime;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.Data;

/**
 * 创建信用额度请求DTO
 */
@Data
@ApiModel(value = "创建信用额度请求", description = "用于创建企业信用额度的请求参数")
public class CreditLimitCreateRequest {

    @ApiModelProperty(value = "企业地址", required = true, example = "0x1234567890abcdef1234567890abcdef12345678")
    @NotBlank(message = "企业地址不能为空")
    @Size(min = 42, max = 42, message = "区块链地址必须是42位")
    @Pattern(regexp = "^0x[a-fA-F0-9]{40}$", message = "区块链地址格式不正确")
    private String enterpriseAddress;

    @ApiModelProperty(value = "额度类型", required = true, notes = "FINANCING-融资额度, GUARANTEE-担保额度, CREDIT-赊账额度", example = "FINANCING")
    @NotNull(message = "额度类型不能为空")
    private CreditLimitType limitType;

    @ApiModelProperty(value = "总额度（元）", required = true, example = "1000000.00", notes = "系统会自动转换为分存储")
    @NotNull(message = "总额度不能为空")
    @DecimalMin(value = "0.01", message = "总额度必须大于0")
    private java.math.BigDecimal totalLimit;

    @ApiModelProperty(value = "预警阈值（百分比）", example = "80", notes = "默认为80，表示80%")
    @Min(value = 1, message = "预警阈值必须大于0")
    @Max(value = 100, message = "预警阈值不能超过100")
    private Integer warningThreshold = 80;

    @ApiModelProperty(value = "生效日期", required = true, example = "2026-01-01T00:00:00")
    @NotNull(message = "生效日期不能为空")
    private LocalDateTime effectiveDate;

    @ApiModelProperty(value = "失效日期", example = "2027-01-01T00:00:00", notes = "为空表示永久有效")
    private LocalDateTime expiryDate;

    @ApiModelProperty(value = "审批人地址", example = "0x9876543210fedcba9876543210fedcba98765432")
    @Size(min = 42, max = 42, message = "审批人地址必须是42位")
    private String approverAddress;

    @ApiModelProperty(value = "审批原因", example = "新企业注册，初始化信用额度")
    private String approveReason;
}
