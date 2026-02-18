package com.fisco.app.dto.credit;

import com.fisco.app.entity.credit.CreditLimit;
import com.fisco.app.enums.CreditLimitStatus;
import com.fisco.app.enums.CreditLimitType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 信用额度查询请求DTO
 */
@Data
@ApiModel(value = "信用额度查询请求", description = "用于查询信用额度的请求参数")
public class CreditLimitQueryRequest {

    @ApiModelProperty(value = "企业地址", example = "0x1234567890abcdef1234567890abcdef12345678")
    private String enterpriseAddress;

    @ApiModelProperty(value = "额度类型", notes = "FINANCING-融资额度, GUARANTEE-担保额度, CREDIT-赊账额度", example = "FINANCING")
    private CreditLimitType limitType;

    @ApiModelProperty(value = "额度状态", notes = "ACTIVE-生效中, FROZEN-已冻结, EXPIRED-已失效, CANCELLED-已取消", example = "ACTIVE")
    private CreditLimitStatus status;

    @ApiModelProperty(value = "风险等级", notes = "LOW-低风险, MEDIUM-中风险, HIGH-高风险", example = "LOW")
    private CreditLimit.RiskLevel riskLevel;

    @ApiModelProperty(value = "使用率最小值（百分比）", example = "0.0")
    private Double usageRateMin;

    @ApiModelProperty(value = "使用率最大值（百分比）", example = "100.0")
    private Double usageRateMax;

    @ApiModelProperty(value = "总额度最小值（单位：分）", example = "10000000")
    private Long totalLimitMin;

    @ApiModelProperty(value = "总额度最大值（单位：分）", example = "100000000")
    private Long totalLimitMax;

    @ApiModelProperty(value = "生效日期开始", example = "2026-01-01T00:00:00")
    private LocalDateTime effectiveDateStart;

    @ApiModelProperty(value = "生效日期结束", example = "2026-12-31T23:59:59")
    private LocalDateTime effectiveDateEnd;

    @ApiModelProperty(value = "到期日期开始", example = "2026-01-01T00:00:00")
    private LocalDateTime expiryDateStart;

    @ApiModelProperty(value = "到期日期结束", example = "2027-12-31T23:59:59")
    private LocalDateTime expiryDateEnd;

    @ApiModelProperty(value = "是否需要预警", example = "true", notes = "true-仅返回需要预警的额度，false-不限制")
    private Boolean needsWarning;

    @ApiModelProperty(value = "逾期次数最小值", example = "0")
    private Integer overdueCountMin;

    @ApiModelProperty(value = "逾期次数最大值", example = "10")
    private Integer overdueCountMax;

    @ApiModelProperty(value = "页码（从0开始）", example = "0")
    private Integer page = 0;

    @ApiModelProperty(value = "每页大小", example = "10")
    private Integer size = 10;

    @ApiModelProperty(value = "排序字段", example = "totalLimit", notes = "可选值: totalLimit, usedLimit, usageRate, effectiveDate, createdAt")
    private String sortBy = "createdAt";

    @ApiModelProperty(value = "排序方向", notes = "ASC-升序, DESC-降序", example = "DESC")
    private String sortDirection = "DESC";
}
