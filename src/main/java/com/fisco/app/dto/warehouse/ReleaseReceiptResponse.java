package com.fisco.app.dto.warehouse;
import com.fisco.app.entity.pledge.ReleaseRecord;


import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 仓单释放响应DTO
 */
@Data
@ApiModel(value = "仓单释放响应", description = "仓单释放操作的结果")
@Schema(name = "仓单释放响应")
public class ReleaseReceiptResponse {

    @ApiModelProperty(value = "释放记录ID")
    private String id;

    @ApiModelProperty(value = "仓单ID")
    private String receiptId;

    @ApiModelProperty(value = "所有者地址")
    private String ownerAddress;

    @ApiModelProperty(value = "金融机构地址")
    private String financialInstitutionAddress;

    @ApiModelProperty(value = "质押金额")
    private BigDecimal pledgeAmount;

    @ApiModelProperty(value = "融资金额")
    private BigDecimal financeAmount;

    @ApiModelProperty(value = "融资利率")
    private Integer financeRate;

    @ApiModelProperty(value = "融资日期")
    private LocalDateTime financeDate;

    @ApiModelProperty(value = "释放日期")
    private LocalDateTime releaseDate;

    @ApiModelProperty(value = "释放类型")
    private ReleaseRecord.ReleaseType releaseType;

    @ApiModelProperty(value = "还款金额")
    private BigDecimal repaymentAmount;

    @ApiModelProperty(value = "利息金额")
    private BigDecimal interestAmount;

    @ApiModelProperty(value = "区块链交易哈希")
    private String txHash;

    @ApiModelProperty(value = "释放备注")
    private String remark;
}
