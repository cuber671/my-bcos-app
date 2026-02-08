package com.fisco.app.dto.enterprise;

import com.fisco.app.entity.user.User;
import com.fisco.app.entity.enterprise.Enterprise;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;


/**
 * 用户及其企业信息DTO
 * 用于返回用户详情时包含所属企业信息
 */
@Data
@ApiModel(value = "用户企业信息", description = "用户及其所属企业的详细信息")
@Schema(name = "用户企业信息")
public class UserWithEnterpriseDTO {

    @ApiModelProperty(value = "用户ID（UUID格式）", example = "550e8400-e29b-41d4-a716-446655440000")
    private String userId;

    @ApiModelProperty(value = "用户名")
    private String username;

    @ApiModelProperty(value = "真实姓名")
    private String realName;

    @ApiModelProperty(value = "邮箱")
    private String email;

    @ApiModelProperty(value = "手机号")
    private String phone;

    @ApiModelProperty(value = "用户类型")
    private String userType;

    @ApiModelProperty(value = "用户状态")
    private String status;

    @ApiModelProperty(value = "部门")
    private String department;

    @ApiModelProperty(value = "职位")
    private String position;

    @ApiModelProperty(value = "最后登录时间")
    private String lastLoginTime;

    @ApiModelProperty(value = "登录次数")
    private Integer loginCount;

    // ========== 企业信息 ==========

    @ApiModelProperty(value = "企业ID（UUID格式）")
    private String enterpriseId;

    @ApiModelProperty(value = "企业名称")
    private String enterpriseName;

    @ApiModelProperty(value = "企业地址")
    private String enterpriseAddress;

    @ApiModelProperty(value = "企业角色")
    private String enterpriseRole;

    @ApiModelProperty(value = "企业状态")
    private String enterpriseStatus;

    @ApiModelProperty(value = "信用评级")
    private Integer creditRating;

    /**
     * 从User实体构建DTO
     */
    public static UserWithEnterpriseDTO fromEntity(User user) {
        UserWithEnterpriseDTO dto = new UserWithEnterpriseDTO();

        // 用户信息
        dto.setUserId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setRealName(user.getRealName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setUserType(user.getUserType() != null ? user.getUserType().name() : null);
        dto.setStatus(user.getStatus() != null ? user.getStatus().name() : null);
        dto.setDepartment(user.getDepartment());
        dto.setPosition(user.getPosition());

        if (user.getLastLoginTime() != null) {
            dto.setLastLoginTime(user.getLastLoginTime().toString());
        }
        dto.setLoginCount(user.getLoginCount());

        // 企业信息
        if (user.getEnterprise() != null) {
            Enterprise enterprise = user.getEnterprise();
            dto.setEnterpriseId(enterprise.getId());
            dto.setEnterpriseName(enterprise.getName());
            dto.setEnterpriseAddress(enterprise.getAddress());
            dto.setEnterpriseRole(enterprise.getRole() != null ? enterprise.getRole().name() : null);
            dto.setEnterpriseStatus(enterprise.getStatus() != null ? enterprise.getStatus().name() : null);
            dto.setCreditRating(enterprise.getCreditRating());
        } else if (user.getEnterpriseId() != null) {
            // 如果LAZY加载未触发，只设置ID
            dto.setEnterpriseId(user.getEnterpriseId());
        }

        return dto;
    }

    /**
     * 从User实体和Enterprise实体构建DTO（适用于通过ID分别查询的场景）
     */
    public static UserWithEnterpriseDTO fromEntities(User user, Enterprise enterprise) {
        UserWithEnterpriseDTO dto = new UserWithEnterpriseDTO();

        // 用户信息
        dto.setUserId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setRealName(user.getRealName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setUserType(user.getUserType() != null ? user.getUserType().name() : null);
        dto.setStatus(user.getStatus() != null ? user.getStatus().name() : null);
        dto.setDepartment(user.getDepartment());
        dto.setPosition(user.getPosition());

        if (user.getLastLoginTime() != null) {
            dto.setLastLoginTime(user.getLastLoginTime().toString());
        }
        dto.setLoginCount(user.getLoginCount());

        // 企业信息
        if (enterprise != null) {
            dto.setEnterpriseId(enterprise.getId());
            dto.setEnterpriseName(enterprise.getName());
            dto.setEnterpriseAddress(enterprise.getAddress());
            dto.setEnterpriseRole(enterprise.getRole() != null ? enterprise.getRole().name() : null);
            dto.setEnterpriseStatus(enterprise.getStatus() != null ? enterprise.getStatus().name() : null);
            dto.setCreditRating(enterprise.getCreditRating());
        }

        return dto;
    }
}
