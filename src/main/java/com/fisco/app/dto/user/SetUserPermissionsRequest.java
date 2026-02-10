package com.fisco.app.dto.user;

import com.fisco.app.entity.user.UserPermission;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import org.springframework.lang.NonNull;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 设置用户权限请求DTO
 */
@Data
@ApiModel(value = "设置用户权限请求", description = "为用户设置权限列表的请求对象")
public class SetUserPermissionsRequest {

    @NonNull
    @NotNull(message = "权限列表不能为空")
    @NotEmpty(message = "权限列表不能为空")
    @ApiModelProperty(value = "权限列表", required = true, notes = "要为用户设置的权限列表")
    private List<PermissionItem> permissions;

    @Data
    @ApiModel(value = "权限项")
    public static class PermissionItem {

        @NotNull(message = "权限代码不能为空")
        @ApiModelProperty(value = "权限代码", required = true, example = "bill:create",
                notes = "格式: 模块:操作，如 bill:create, bill:read, bill:update, bill:delete")
        private String permissionCode;

        @ApiModelProperty(value = "权限名称", required = true, example = "创建票据")
        private String permissionName;

        @NotNull(message = "资源类型不能为空")
        @ApiModelProperty(value = "资源类型", required = true, example = "BILL",
                notes = "BILL-票据, RECEIVABLE-应收账款, WAREHOUSE_RECEIPT-仓单, ENTERPRISE-企业, USER-用户, CREDIT-授信")
        private UserPermission.ResourceType resourceType;

        @NotNull(message = "操作类型不能为空")
        @ApiModelProperty(value = "操作类型", required = true, example = "CREATE",
                notes = "CREATE-创建, READ-读取, UPDATE-更新, DELETE-删除, APPROVE-审核, EXPORT-导出, IMPORT-导入")
        private UserPermission.Operation operation;

        @ApiModelProperty(value = "权限范围", example = "OWN",
                notes = "ALL-全部, OWN-仅自己, DEPARTMENT-部门, ENTERPRISE-企业")
        private UserPermission.PermissionScope scope = UserPermission.PermissionScope.OWN;

        @ApiModelProperty(value = "是否启用", example = "true")
        private Boolean isEnabled = true;

        @ApiModelProperty(value = "过期时间", example = "2024-12-31T23:59:59")
        private LocalDateTime expireAt;

        @ApiModelProperty(value = "备注", example = "财务部门票据创建权限")
        private String remarks;
    }
}
