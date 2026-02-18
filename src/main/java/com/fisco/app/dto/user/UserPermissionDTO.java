package com.fisco.app.dto.user;

import com.fisco.app.entity.user.UserPermission;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户权限DTO
 */
@Data
@ApiModel(value = "用户权限DTO", description = "用户权限数据传输对象")
public class UserPermissionDTO {

    @ApiModelProperty(value = "权限ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    @ApiModelProperty(value = "用户ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String userId;

    @ApiModelProperty(value = "权限代码", example = "bill:create")
    private String permissionCode;

    @ApiModelProperty(value = "权限名称", example = "创建票据")
    private String permissionName;

    @ApiModelProperty(value = "资源类型", example = "BILL")
    private UserPermission.ResourceType resourceType;

    @ApiModelProperty(value = "操作类型", example = "CREATE")
    private UserPermission.Operation operation;

    @ApiModelProperty(value = "权限范围", example = "OWN")
    private UserPermission.PermissionScope scope;

    @ApiModelProperty(value = "是否启用", example = "true")
    private Boolean isEnabled;

    @ApiModelProperty(value = "是否过期", example = "false")
    private Boolean isExpired;

    @ApiModelProperty(value = "过期时间", example = "2024-12-31T23:59:59")
    private LocalDateTime expireAt;

    @ApiModelProperty(value = "备注", example = "财务部门票据创建权限")
    private String remarks;

    @ApiModelProperty(value = "创建时间", example = "2024-01-01T10:00:00")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "更新时间", example = "2024-01-01T10:00:00")
    private LocalDateTime updatedAt;

    @ApiModelProperty(value = "创建者", example = "admin")
    private String createdBy;

    /**
     * 从实体转换为DTO
     */
    public static UserPermissionDTO fromEntity(UserPermission entity) {
        if (entity == null) {
            return null;
        }

        UserPermissionDTO dto = new UserPermissionDTO();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setPermissionCode(entity.getPermissionCode());
        dto.setPermissionName(entity.getPermissionName());
        dto.setResourceType(entity.getResourceType());
        dto.setOperation(entity.getOperation());
        dto.setScope(entity.getScope());
        dto.setIsEnabled(entity.getIsEnabled());
        dto.setIsExpired(entity.getIsExpired());
        dto.setExpireAt(entity.getExpireAt());
        dto.setRemarks(entity.getRemarks());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setCreatedBy(entity.getCreatedBy());

        return dto;
    }
}
