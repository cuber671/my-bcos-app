package com.fisco.app.dto.user;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户注册请求DTO
 */
@Data
@ApiModel(value = "用户注册请求", description = "用户使用邀请码进行注册的请求参数")
@Schema(name = "用户注册请求")
public class UserRegistrationRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50之间")
    @ApiModelProperty(value = "用户名（登录账号）", required = true, example = "zhangsan")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度必须在6-100之间")
    @ApiModelProperty(value = "登录密码", required = true, example = "password123")
    private String password;

    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 100, message = "真实姓名长度不能超过100")
    @ApiModelProperty(value = "真实姓名", required = true, example = "张三")
    private String realName;

    @Email(message = "邮箱格式不正确")
    @ApiModelProperty(value = "电子邮箱", example = "zhangsan@example.com")
    private String email;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @ApiModelProperty(value = "手机号码", example = "13800138000")
    private String phone;

    @NotBlank(message = "邀请码不能为空")
    @ApiModelProperty(value = "企业邀请码", required = true, example = "INV2024011812345678")
    private String invitationCode;

    @ApiModelProperty(value = "部门", example = "财务部")
    @Size(max = 100, message = "部门长度不能超过100")
    private String department;

    @ApiModelProperty(value = "职位", example = "财务经理")
    @Size(max = 100, message = "职位长度不能超过100")
    private String position;

    @ApiModelProperty(value = "注册备注信息", example = "希望能加入贵公司")
    @Size(max = 500, message = "备注长度不能超过500")
    private String remarks;
}
