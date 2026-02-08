package com.fisco.app.dto.enterprise;

import com.fisco.app.entity.enterprise.Enterprise;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;



/**
 * 企业注册请求DTO
 */
@Data
@ApiModel(value = "企业注册请求")
@Schema(name = "企业注册请求")
public class EnterpriseRegistrationRequest {

    @ApiModelProperty(value = "区块链地址（可选，如果不提供将由系统自动生成随机地址）", required = false, example = "0x1234567890abcdef1234567890abcdef12345678")
    @Size(min = 42, max = 42, message = "区块链地址必须是42位")
    @Pattern(regexp = "^0x[a-fA-F0-9]{40}$", message = "区块链地址格式不正确，必须是0x开头的40位十六进制字符")
    private String address;

    @ApiModelProperty(value = "企业名称", required = true, example = "供应商A")
    @NotBlank(message = "企业名称不能为空")
    @Size(min = 2, max = 255, message = "企业名称长度必须在2-255之间")
    private String name;

    @ApiModelProperty(value = "统一社会信用代码", required = true, example = "91110000MA001234XY")
    @NotBlank(message = "统一社会信用代码不能为空")
    @Size(min = 18, max = 18, message = "统一社会信用代码必须是18位")
    @Pattern(regexp = "^[0-9A-HJ-NPQ-TV-Z]{18}$", message = "统一社会信用代码格式不正确")
    private String creditCode;

    @ApiModelProperty(value = "用户名（用于登录）", required = true, example = "enterprise_001", notes = "用于系统登录，建议使用英文、数字、下划线组合，如：company_abc")
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;

    @ApiModelProperty(value = "企业邮箱（可选，用于登录和通知）", example = "contact@company.com")
    @Email(message = "邮箱格式不正确")
    @Size(max = 150, message = "邮箱长度不能超过150")
    private String email;

    @ApiModelProperty(value = "企业联系电话（可选，用于登录和通知）", example = "13800138000")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @ApiModelProperty(value = "企业地址", example = "北京市朝阳区")
    @Size(max = 500, message = "企业地址长度不能超过500")
    private String enterpriseAddress;

    @ApiModelProperty(value = "企业角色", required = true, notes = "SUPPLIER-供应商, CORE_ENTERPRISE-核心企业, FINANCIAL_INSTITUTION-金融机构, REGULATOR-监管机构", example = "SUPPLIER")
    @NotNull(message = "企业角色不能为空")
    private Enterprise.EnterpriseRole role;

    @ApiModelProperty(value = "初始登录密码（6-100位）", required = true, example = "Abc123456", notes = "企业首次登录时的密码，必须包含字母和数字")
    @NotBlank(message = "初始密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度必须在6-100之间")
    private String initialPassword;

    @ApiModelProperty(value = "授信额度", example = "1000000.00")
    private java.math.BigDecimal creditLimit;

    @ApiModelProperty(value = "备注信息", example = "新注册企业")
    private String remarks;

    /**
     * 转换为Enterprise实体
     */
    public Enterprise toEnterprise() {
        Enterprise enterprise = new Enterprise();
        enterprise.setAddress(this.address);
        enterprise.setName(this.name);
        enterprise.setCreditCode(this.creditCode);
        enterprise.setUsername(this.username);
        enterprise.setEmail(this.email);
        enterprise.setPhone(this.phone);
        enterprise.setEnterpriseAddress(this.enterpriseAddress);
        enterprise.setRole(this.role);
        enterprise.setCreditLimit(this.creditLimit != null ? this.creditLimit : java.math.BigDecimal.ZERO);
        enterprise.setRemarks(this.remarks);
        return enterprise;
    }
}
