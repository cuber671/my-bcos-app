package com.fisco.app.dto.credit;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 单个额度可用余额查询响应DTO
 *
 * 查询单个额度的可用余额、使用率等详细信息
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-09
 * @version 1.0
 */
@Data
@ApiModel(value = "额度可用余额查询响应", description = "单个额度的可用余额详细信息")
public class CreditLimitAvailableResponse {

    @ApiModelProperty(value = "额度ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String id;

    @ApiModelProperty(value = "企业地址", example = "0x1234567890abcdef1234567890abcdef12345678")
    private String enterpriseAddress;

    @ApiModelProperty(value = "企业名称", example = "供应商A")
    private String enterpriseName;

    @ApiModelProperty(value = "额度类型", notes = "FINANCING-融资额度, GUARANTEE-担保额度, CREDIT-赊账额度", example = "FINANCING")
    private String limitType;

    @ApiModelProperty(value = "额度类型名称", example = "融资额度")
    private String limitTypeName;

    @ApiModelProperty(value = "总额度（元）", example = "1000000.00")
    private BigDecimal totalLimit;

    @ApiModelProperty(value = "已使用额度（元）", example = "300000.00")
    private BigDecimal usedLimit;

    @ApiModelProperty(value = "冻结额度（元）", example = "100000.00")
    private BigDecimal frozenLimit;

    @ApiModelProperty(value = "可用额度（元）", example = "600000.00", notes = "总额度 - 已使用额度 - 冻结额度")
    private BigDecimal availableLimit;

    @ApiModelProperty(value = "使用率（%）", example = "30.0", notes = "已使用额度 / 总额度 * 100")
    private Double usageRate;

    @ApiModelProperty(value = "预警阈值（%）", example = "80.0")
    private Integer warningThreshold;

    @ApiModelProperty(value = "是否需要预警", example = "false", notes = "使用率 >= 预警阈值时为true")
    private Boolean needsWarning;

    @ApiModelProperty(value = "额度状态", notes = "ACTIVE-生效中, FROZEN-已冻结, EXPIRED-已失效, CANCELLED-已取消", example = "ACTIVE")
    private String status;

    @ApiModelProperty(value = "额度状态名称", example = "生效中")
    private String statusName;

    @ApiModelProperty(value = "生效日期", example = "2026-01-01T00:00:00")
    private LocalDateTime effectiveDate;

    @ApiModelProperty(value = "失效日期", example = "2027-01-01T00:00:00")
    private LocalDateTime expiryDate;

    @ApiModelProperty(value = "距离失效天数", example = "180", notes = "null表示永久有效")
    private Integer daysUntilExpiry;

    @ApiModelProperty(value = "查询时间", example = "2026-02-09T15:30:00")
    private LocalDateTime queriedAt;
}
