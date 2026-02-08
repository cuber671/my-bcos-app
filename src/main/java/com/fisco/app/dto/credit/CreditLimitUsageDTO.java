package com.fisco.app.dto.credit;

import com.fisco.app.enums.CreditUsageType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 信用额度使用记录DTO
 */
@Data
@ApiModel(value = "信用额度使用记录DTO", description = "额度使用记录详细信息")
public class CreditLimitUsageDTO {

    @ApiModelProperty(value = "记录ID", example = "b2c3d4e5-f6g7-8901-bcde-f12345678901")
    private String id;

    @ApiModelProperty(value = "额度ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String creditLimitId;

    @ApiModelProperty(value = "使用类型", notes = "USE-使用, RELEASE-释放, FREEZE-冻结, UNFREEZE-解冻", example = "USE")
    private CreditUsageType usageType;

    @ApiModelProperty(value = "业务类型", example = "FINANCING_APPLICATION", notes = "FINANCING_APPLICATION-融资申请, GUARANTEE_APPLICATION-担保申请, CREDIT_PURCHASE-赊账采购")
    private String businessType;

    @ApiModelProperty(value = "业务ID", example = "c3d4e5f6-g7h8-9012-cdef-234567890123")
    private String businessId;

    @ApiModelProperty(value = "使用金额（元）", example = "50000.00")
    private BigDecimal amount;

    @ApiModelProperty(value = "使用前可用额度（元）", example = "1000000.00")
    private BigDecimal beforeAvailable;

    @ApiModelProperty(value = "使用后可用额度（元）", example = "950000.00")
    private BigDecimal afterAvailable;

    @ApiModelProperty(value = "使用前已使用额度（元）", example = "0.00")
    private BigDecimal beforeUsed;

    @ApiModelProperty(value = "使用后已使用额度（元）", example = "50000.00")
    private BigDecimal afterUsed;

    @ApiModelProperty(value = "使用前冻结额度（元）", example = "0.00")
    private BigDecimal beforeFrozen;

    @ApiModelProperty(value = "使用后冻结额度（元）", example = "0.00")
    private BigDecimal afterFrozen;

    @ApiModelProperty(value = "操作人地址", example = "0x1234567890abcdef1234567890abcdef12345678")
    private String operatorAddress;

    @ApiModelProperty(value = "操作人姓名", example = "张三")
    private String operatorName;

    @ApiModelProperty(value = "使用日期", example = "2026-01-15T10:30:00")
    private LocalDateTime usageDate;

    @ApiModelProperty(value = "备注说明", example = "融资申请FNA001占用额度")
    private String remark;

    @ApiModelProperty(value = "创建时间", example = "2026-01-15T10:30:00")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "区块链交易哈希", example = "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890")
    private String txHash;
}
