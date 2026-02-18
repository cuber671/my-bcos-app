package com.fisco.app.dto.warehouse;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 仓单变更请求DTO（增强版）
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-09
 * @version 1.0
 */
@Data
@ApiModel(value = "仓单变更请求", description = "仓单信息变更请求参数")
public class UpdateReceiptRequest {

    // ========== 可变更字段 ==========

    @ApiModelProperty(value = "单价（元）", example = "4500.00")
    @DecimalMin(value = "0.01", message = "单价必须大于0")
    private BigDecimal unitPrice;

    @ApiModelProperty(value = "市场参考价格（元）", example = "4600.00")
    @DecimalMin(value = "0", message = "市场价格不能为负数")
    private BigDecimal marketPrice;

    @ApiModelProperty(value = "仓库详细地址", example = "上海市浦东新区XX路XX号")
    private String warehouseLocation;

    @ApiModelProperty(value = "存储位置", example = "A区03栋12排5层货架")
    private String storageLocation;

    @ApiModelProperty(value = "有效期", example = "2026-07-26T23:59:59")
    private LocalDateTime expiryDate;

    @ApiModelProperty(value = "备注信息", example = "备注：货物为一级品")
    private String remarks;

    // ========== 变更原因必填 ==========

    @NotNull(message = "变更原因不能为空")
    @ApiModelProperty(value = "变更原因", required = true, example = "市场价格调整")
    private String changeReason;

    @NotNull(message = "变更类型不能为空")
    @ApiModelProperty(value = "变更类型", required = true)
    private ChangeType changeType;

    /**
     * 变更类型枚举
     */
    public enum ChangeType {
        PRICE_ADJUSTMENT("价格调整", "单价、市场价格调整"),
        LOCATION_CHANGE("位置变更", "仓库位置、存储位置变更"),
        EXPIRY_EXTENSION("有效期延长", "延长仓单有效期"),
        INFO_UPDATE("信息更新", "备注等信息更新"),
        OTHER("其他", "其他类型的变更");

        private final String name;
        private final String description;

        ChangeType(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }
    }
}
