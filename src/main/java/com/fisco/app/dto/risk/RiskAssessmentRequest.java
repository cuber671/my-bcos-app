package com.fisco.app.dto.risk;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotBlank;

import java.time.LocalDateTime;

/**
 * 风险评估请求DTO
 */
@Data
@ApiModel(value = "风险评估请求", description = "用于评估企业风险的请求参数")
public class RiskAssessmentRequest {

    @NotBlank(message = "企业地址不能为空")
    @ApiModelProperty(value = "企业地址", required = true, example = "0x1234567890abcdef")
    private String enterpriseAddress;

    @ApiModelProperty(value = "评估类型", notes = "CREDIT-信用风险评估, OVERDUE-逾期风险评估, COMPREHENSIVE-综合风险评估", example = "COMPREHENSIVE")
    private RiskAssessmentType assessmentType;

    @ApiModelProperty(value = "评估基准时间", example = "2026-02-03T10:30:00")
    private LocalDateTime assessmentTime;

    @ApiModelProperty(value = "评估周期（月）", example = "12", notes = "用于计算历史数据的时间范围")
    private Integer assessmentPeriod = 12;

    /**
     * 风险评估类型枚举
     */
    public enum RiskAssessmentType {
        CREDIT("信用风险评估"),
        OVERDUE("逾期风险评估"),
        COMPREHENSIVE("综合风险评估");

        private final String description;

        RiskAssessmentType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
