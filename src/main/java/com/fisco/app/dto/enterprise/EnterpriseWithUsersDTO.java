package com.fisco.app.dto.enterprise;
import com.fisco.app.entity.user.User;
import com.fisco.app.entity.enterprise.Enterprise;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 企业及其用户列表DTO
 * 用于返回企业详情时包含其所有用户信息
 */
@Data
@ApiModel(value = "企业用户信息", description = "企业及其所有用户的详细信息")
@Schema(name = "企业用户信息")
public class EnterpriseWithUsersDTO {

    @ApiModelProperty(value = "企业ID（UUID格式）")
    private String enterpriseId;

    @ApiModelProperty(value = "企业地址")
    private String address;

    @ApiModelProperty(value = "企业名称")
    private String name;

    @ApiModelProperty(value = "统一社会信用代码")
    private String creditCode;

    @ApiModelProperty(value = "企业地址")
    private String enterpriseAddress;

    @ApiModelProperty(value = "企业角色")
    private String role;

    @ApiModelProperty(value = "企业状态")
    private String status;

    @ApiModelProperty(value = "信用评级")
    private Integer creditRating;

    @ApiModelProperty(value = "授信额度")
    private String creditLimit;

    @ApiModelProperty(value = "注册时间")
    private String registeredAt;

    @ApiModelProperty(value = "用户数量")
    private Integer userCount;

    @ApiModelProperty(value = "用户列表")
    private List<UserInfo> users;

    /**
     * 用户信息内部类
     */
    @Data
    @Schema(name = "用户信息")
    public static class UserInfo {
        @ApiModelProperty(value = "用户ID（UUID格式）", example = "550e8400-e29b-41d4-a716-446655440000")
        private String id;

        @ApiModelProperty(value = "用户名")
        private String username;

        @ApiModelProperty(value = "真实姓名")
        private String realName;

        @ApiModelProperty(value = "用户类型")
        private String userType;

        @ApiModelProperty(value = "用户状态")
        private String status;

        @ApiModelProperty(value = "部门")
        private String department;

        @ApiModelProperty(value = "职位")
        private String position;

        @ApiModelProperty(value = "邮箱")
        private String email;

        @ApiModelProperty(value = "手机号")
        private String phone;

        @ApiModelProperty(value = "登录次数")
        private Integer loginCount;
    }

    /**
     * 从Enterprise实体构建DTO
     */
    public static EnterpriseWithUsersDTO fromEntity(Enterprise enterprise) {
        EnterpriseWithUsersDTO dto = new EnterpriseWithUsersDTO();

        // 企业信息
        dto.setEnterpriseId(enterprise.getId());
        dto.setAddress(enterprise.getAddress());
        dto.setName(enterprise.getName());
        dto.setCreditCode(enterprise.getCreditCode());
        dto.setEnterpriseAddress(enterprise.getEnterpriseAddress());
        dto.setRole(enterprise.getRole() != null ? enterprise.getRole().name() : null);
        dto.setStatus(enterprise.getStatus() != null ? enterprise.getStatus().name() : null);
        dto.setCreditRating(enterprise.getCreditRating());
        dto.setCreditLimit(enterprise.getCreditLimit() != null ? enterprise.getCreditLimit().toString() : "0");

        if (enterprise.getRegisteredAt() != null) {
            dto.setRegisteredAt(enterprise.getRegisteredAt().toString());
        }

        // 用户列表
        if (enterprise.getUsers() != null && !enterprise.getUsers().isEmpty()) {
            dto.setUserCount(enterprise.getUsers().size());
            dto.setUsers(enterprise.getUsers().stream()
                .map(EnterpriseWithUsersDTO::toUserInfo)
                .collect(Collectors.toList()));
        } else {
            dto.setUserCount(0);
            dto.setUsers(new ArrayList<>());
        }

        return dto;
    }

    /**
     * 从Enterprise实体和用户列表构建DTO（适用于通过ID分别查询的场景）
     */
    public static EnterpriseWithUsersDTO fromEntities(Enterprise enterprise, List<User> users) {
        EnterpriseWithUsersDTO dto = new EnterpriseWithUsersDTO();

        // 企业信息
        dto.setEnterpriseId(enterprise.getId());
        dto.setAddress(enterprise.getAddress());
        dto.setName(enterprise.getName());
        dto.setCreditCode(enterprise.getCreditCode());
        dto.setEnterpriseAddress(enterprise.getEnterpriseAddress());
        dto.setRole(enterprise.getRole() != null ? enterprise.getRole().name() : null);
        dto.setStatus(enterprise.getStatus() != null ? enterprise.getStatus().name() : null);
        dto.setCreditRating(enterprise.getCreditRating());
        dto.setCreditLimit(enterprise.getCreditLimit() != null ? enterprise.getCreditLimit().toString() : "0");

        if (enterprise.getRegisteredAt() != null) {
            dto.setRegisteredAt(enterprise.getRegisteredAt().toString());
        }

        // 用户列表
        if (users != null && !users.isEmpty()) {
            dto.setUserCount(users.size());
            dto.setUsers(users.stream()
                .map(EnterpriseWithUsersDTO::toUserInfo)
                .collect(Collectors.toList()));
        } else {
            dto.setUserCount(0);
            dto.setUsers(new ArrayList<>());
        }

        return dto;
    }

    /**
     * 将User实体转换为UserInfo
     */
    private static UserInfo toUserInfo(User user) {
        UserInfo userInfo = new UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setRealName(user.getRealName());
        userInfo.setUserType(user.getUserType() != null ? user.getUserType().name() : null);
        userInfo.setStatus(user.getStatus() != null ? user.getStatus().name() : null);
        userInfo.setDepartment(user.getDepartment());
        userInfo.setPosition(user.getPosition());
        userInfo.setEmail(user.getEmail());
        userInfo.setPhone(user.getPhone());
        userInfo.setLoginCount(user.getLoginCount());
        return userInfo;
    }
}
