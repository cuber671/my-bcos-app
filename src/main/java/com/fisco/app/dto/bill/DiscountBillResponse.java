package com.fisco.app.dto.bill;

import com.fisco.app.entity.bill.DiscountRecord;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 票据贴现响应DTO
 */
@Data
@ApiModel(value = "票据贴现响应", description = "票据贴现操作的结果")
@Schema(name = "票据贴现响应")
public class DiscountBillResponse {

    @ApiModelProperty(value = "贴现记录ID")
    private String id;

    @ApiModelProperty(value = "票据ID")
    private String billId;

    @ApiModelProperty(value = "持票人地址")
    private String holderAddress;

    @ApiModelProperty(value = "金融机构地址")
    private String financialInstitutionAddress;

    @ApiModelProperty(value = "票面金额")
    private BigDecimal billAmount;

    @ApiModelProperty(value = "贴现金额（实际支付）")
    private BigDecimal discountAmount;

    @ApiModelProperty(value = "贴现率（%）")
    private BigDecimal discountRate;

    @ApiModelProperty(value = "贴现利息")
    private BigDecimal discountInterest;

    @ApiModelProperty(value = "贴现日期")
    private LocalDateTime discountDate;

    @ApiModelProperty(value = "到期日期")
    private LocalDateTime maturityDate;

    @ApiModelProperty(value = "贴现天数")
    private Integer discountDays;

    @ApiModelProperty(value = "区块链交易哈希")
    private String txHash;

    @ApiModelProperty(value = "贴现状态")
    private DiscountRecord.DiscountStatus status;

    @ApiModelProperty(value = "贴现备注")
    private String remark;
}
