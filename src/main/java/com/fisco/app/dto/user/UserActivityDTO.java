package com.fisco.app.dto.user;

import com.fisco.app.entity.user.UserActivity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户活动日志DTO
 */
@Data
@ApiModel(value = "用户活动日志DTO", description = "用户活动日志数据传输对象")
public class UserActivityDTO {

    @ApiModelProperty(value = "活动ID", example = "1")
    private Long id;

    @ApiModelProperty(value = "用户ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String userId;

    @ApiModelProperty(value = "用户名", example = "zhangsan")
    private String username;

    @ApiModelProperty(value = "真实姓名", example = "张三")
    private String realName;

    @ApiModelProperty(value = "活动类型", example = "LOGIN")
    private UserActivity.ActivityType activityType;

    @ApiModelProperty(value = "活动描述", example = "用户登录系统")
    private String description;

    @ApiModelProperty(value = "操作模块", example = "USER")
    private String module;

    @ApiModelProperty(value = "操作结果", example = "SUCCESS")
    private UserActivity.ActivityResult result;

    @ApiModelProperty(value = "请求方法", example = "POST")
    private String requestMethod;

    @ApiModelProperty(value = "请求URL", example = "/api/users/login")
    private String requestUrl;

    @ApiModelProperty(value = "请求IP地址", example = "192.168.1.100")
    private String ipAddress;

    @ApiModelProperty(value = "用户代理", example = "Mozilla/5.0...")
    private String userAgent;

    @ApiModelProperty(value = "浏览器类型", example = "Chrome")
    private String browser;

    @ApiModelProperty(value = "操作系统", example = "Windows 10")
    private String os;

    @ApiModelProperty(value = "设备类型", example = "PC")
    private String device;

    @ApiModelProperty(value = "位置信息", example = "中国 北京")
    private String location;

    @ApiModelProperty(value = "失败原因")
    private String failureReason;

    @ApiModelProperty(value = "操作时长（毫秒）", example = "150")
    private Long duration;

    @ApiModelProperty(value = "额外数据")
    private String extraData;

    @ApiModelProperty(value = "创建时间", example = "2024-01-01T10:00:00")
    private LocalDateTime createdAt;

    /**
     * 从实体转换为DTO
     */
    public static UserActivityDTO fromEntity(UserActivity entity) {
        if (entity == null) {
            return null;
        }

        UserActivityDTO dto = new UserActivityDTO();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setUsername(entity.getUsername());
        dto.setRealName(entity.getRealName());
        dto.setActivityType(entity.getActivityType());
        dto.setDescription(entity.getDescription());
        dto.setModule(entity.getModule());
        dto.setResult(entity.getResult());
        dto.setRequestMethod(entity.getRequestMethod());
        dto.setRequestUrl(entity.getRequestUrl());
        dto.setIpAddress(entity.getIpAddress());
        dto.setUserAgent(entity.getUserAgent());
        dto.setBrowser(entity.getBrowser());
        dto.setOs(entity.getOs());
        dto.setDevice(entity.getDevice());
        dto.setLocation(entity.getLocation());
        dto.setFailureReason(entity.getFailureReason());
        dto.setDuration(entity.getDuration());
        dto.setExtraData(entity.getExtraData());
        dto.setCreatedAt(entity.getCreatedAt());

        return dto;
    }
}