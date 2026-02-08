package com.fisco.app.dto.enterprise;

import com.fisco.app.entity.enterprise.Enterprise;


import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 企业注册响应DTO
 */
@Data
@ApiModel(value = "企业注册响应")
@Schema(name = "企业注册响应")
public class EnterpriseRegistrationResponse {

    @ApiModelProperty(value = "企业ID（UUID格式）", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String id;

    @ApiModelProperty(value = "区块链地址", example = "0x1234567890abcdef1234567890abcdef12345678")
    private String address;

    @ApiModelProperty(value = "企业名称", example = "供应商A")
    private String name;

    @ApiModelProperty(value = "统一社会信用代码", example = "91110000MA001234XY")
    private String creditCode;

    @ApiModelProperty(value = "企业地址", example = "北京市朝阳区")
    private String enterpriseAddress;

    @ApiModelProperty(value = "企业角色", example = "SUPPLIER")
    private Enterprise.EnterpriseRole role;

    @ApiModelProperty(value = "企业状态", example = "PENDING")
    private Enterprise.EnterpriseStatus status;

    @ApiModelProperty(value = "信用评级(0-100)", example = "60")
    private Integer creditRating;

    @ApiModelProperty(value = "授信额度", example = "1000000.00")
    private BigDecimal creditLimit;

    @ApiModelProperty(value = "API密钥（请妥善保管）", example = "INV1234567890abcd...")
    private String apiKey;

    @ApiModelProperty(value = "备注信息", example = "新注册企业")
    private String remarks;

    @ApiModelProperty(value = "注册时间")
    private LocalDateTime registeredAt;

    @ApiModelProperty(value = "区块链交易哈希（上链成功时返回）", example = "0xabc123...")
    private String txHash;

    @ApiModelProperty(value = "是否上链成功", example = "true")
    private Boolean onChainSuccess;

    @ApiModelProperty(value = "提示信息", example = "企业已成功注册，请等待管理员审核")
    private String message;

    /**
     * 从Enterprise实体创建响应DTO
     */
    public static EnterpriseRegistrationResponse fromEntity(Enterprise enterprise) {
        return fromEntity(enterprise, null, true);
    }

    /**
     * 从Enterprise实体创建响应DTO（带交易哈希）
     */
    public static EnterpriseRegistrationResponse fromEntity(Enterprise enterprise, String txHash, boolean onChainSuccess) {
        EnterpriseRegistrationResponse response = new EnterpriseRegistrationResponse();
        response.setId(enterprise.getId()); // id现在是UUID格式
        response.setAddress(enterprise.getAddress());
        response.setName(enterprise.getName());
        response.setCreditCode(enterprise.getCreditCode());
        response.setEnterpriseAddress(enterprise.getEnterpriseAddress());
        response.setRole(enterprise.getRole());
        response.setStatus(enterprise.getStatus());
        response.setCreditRating(enterprise.getCreditRating());
        response.setCreditLimit(enterprise.getCreditLimit());
        response.setApiKey(enterprise.getApiKey());
        response.setRemarks(enterprise.getRemarks());
        response.setRegisteredAt(enterprise.getRegisteredAt());
        response.setTxHash(txHash);
        response.setOnChainSuccess(onChainSuccess);

        // 根据状态返回提示信息
        if (enterprise.getStatus() == Enterprise.EnterpriseStatus.PENDING) {
            response.setMessage("企业已成功注册，请等待管理员审核");
        } else if (enterprise.getStatus() == Enterprise.EnterpriseStatus.ACTIVE) {
            response.setMessage("企业已成功注册并激活");
        }

        return response;
    }
}
