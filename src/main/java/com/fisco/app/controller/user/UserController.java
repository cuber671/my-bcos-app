package com.fisco.app.controller.user;

import org.springframework.security.core.Authentication;

import com.fisco.app.entity.user.User;
import com.fisco.app.security.annotations.RequireEnterpriseAccess;
import com.fisco.app.security.annotations.RequireEnterpriseAdmin;
import com.fisco.app.vo.Result;
import org.springframework.data.domain.Pageable;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fisco.app.service.user.UserService;

import javax.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;


/**
 * 用户管理Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Api(tags = "用户管理")
public class UserController {

    private final UserService userService;
    private final com.fisco.app.service.user.UserActivityService userActivityService;
    private final com.fisco.app.service.user.UserPermissionService userPermissionService;

    /**
     * 创建用户
     * POST /api/users
     */
    @PostMapping
    @ApiOperation(value = "创建用户", notes = "创建新用户账户")
    public Result<User> createUser(
            @Valid @RequestBody CreateUserRequest request,
            Authentication authentication) {
        String operator = authentication.getName();

        log.info("==================== 接收到用户创建请求 ====================");
        log.info("用户基本信息: username={}, realName={}, email={}, phone={}",
                 request.getUsername(), request.getRealName(), request.getEmail(), request.getPhone());
        log.info("用户类型和职位: userType={}, department={}, position={}",
                 request.getUserType(), request.getDepartment(), request.getPosition());
        log.info("操作人: {}", operator);

        long startTime = System.currentTimeMillis();

        try {
            User user = new User();
            user.setUsername(request.getUsername());
            user.setPassword(request.getPassword());
            user.setRealName(request.getRealName());
            user.setEmail(request.getEmail());
            user.setPhone(request.getPhone());
            // 注意：enterpriseId从邀请码中获取，不从request获取
            user.setEnterpriseId(null); // 将在Service中通过邀请码设置
            user.setUserType(request.getUserType());
            user.setDepartment(request.getDepartment());
            user.setPosition(request.getPosition());

            log.debug("调用UserService创建用户");
            User created = userService.createUser(user, operator);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 用户创建请求处理完成: userId={}, username={}, 耗时={}ms",
                     created.getId(), created.getUsername(), duration);
            log.info("==================== 用户创建请求结束 ====================");

            return Result.success("用户创建成功", created);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 用户创建请求处理失败: username={}, 耗时={}ms, error={}",
                     request.getUsername(), duration, e.getMessage(), e);
            log.info("==================== 用户创建请求失败（结束） ====================");
            throw e;
        }
    }

    /**
     * 更新用户信息
     * PUT /api/users/{userId}
     */
    @PutMapping("/{userId}")
    @ApiOperation(value = "更新用户信息", notes = "更新指定用户的信息（包括用户角色）")
    @RequireEnterpriseAdmin
    public Result<User> updateUser(
            @ApiParam(value = "用户ID", required = true) @PathVariable @NonNull String userId,
            @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication) {
        String operator = authentication.getName();

        User updatedUser = new User();
        updatedUser.setUsername(request.getUsername());
        updatedUser.setRealName(request.getRealName());
        updatedUser.setEmail(request.getEmail());
        updatedUser.setPhone(request.getPhone());
        updatedUser.setDepartment(request.getDepartment());
        updatedUser.setPosition(request.getPosition());
        updatedUser.setAvatarUrl(request.getAvatarUrl());
        updatedUser.setUserType(request.getUserType());  // 支持更新用户类型

        User updated = userService.updateUser(userId, updatedUser, operator);
        return Result.success("用户信息更新成功", updated);
    }

    /**
     * 获取用户信息
     * GET /api/users/{userId}
     */
    @GetMapping("/{userId}")
    @ApiOperation(value = "获取用户信息", notes = "根据ID查询用户详细信息")
    public Result<User> getUser(
            @ApiParam(value = "用户ID", required = true) @PathVariable @NonNull String userId) {
        User user = userService.getUserById(userId);
        return Result.success(user);
    }

    /**
     * 获取当前登录用户信息
     * GET /api/users/me
     */
    @GetMapping("/me")
    @ApiOperation(value = "获取当前用户信息", notes = "获取当前登录用户的详细信息")
    public Result<User> getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        return Result.success(user);
    }

    /**
     * 修改密码
     * PUT /api/users/{userId}/password
     */
    @PutMapping("/{userId}/password")
    @ApiOperation(value = "修改密码", notes = "用户修改自己的密码")
    public Result<String> changePassword(
            @ApiParam(value = "用户ID", required = true) @PathVariable @NonNull String userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return Result.success("密码修改成功");
    }

    /**
     * 重置密码（管理员）
     * PUT /api/users/{userId}/reset-password
     */
    @PutMapping("/{userId}/reset-password")
    @ApiOperation(value = "重置密码", notes = "管理员重置指定用户的密码")
    @RequireEnterpriseAdmin
    public Result<String> resetPassword(
            @ApiParam(value = "用户ID", required = true) @PathVariable @NonNull String userId,
            @Valid @RequestBody ResetPasswordRequest request,
            Authentication authentication) {
        String operator = authentication.getName();
        userService.resetPassword(userId, request.getNewPassword(), operator);
        return Result.success("密码重置成功");
    }

    /**
     * 设置用户状态
     * PUT /api/users/{userId}/status
     */
    @PutMapping("/{userId}/status")
    @ApiOperation(value = "设置用户状态", notes = "启用/禁用/锁定用户")
    @RequireEnterpriseAdmin
    public Result<String> setUserStatus(
            @ApiParam(value = "用户ID", required = true) @PathVariable @NonNull String userId,
            @Valid @RequestBody SetUserStatusRequest request,
            Authentication authentication) {
        String operator = authentication.getName();
        userService.setUserStatus(userId, request.getStatus(), operator);
        return Result.success("用户状态设置成功");
    }

    /**
     * 删除用户
     * DELETE /api/users/{userId}
     */
    @DeleteMapping("/{userId}")
    @ApiOperation(value = "删除用户", notes = "删除指定用户")
    @RequireEnterpriseAdmin
    public Result<String> deleteUser(
            @ApiParam(value = "用户ID", required = true) @PathVariable @NonNull String userId,
            Authentication authentication) {
        userService.deleteUser(userId);
        return Result.success("用户删除成功");
    }

    /**
     * 分页查询用户列表
     * GET /api/users
     */
    @GetMapping
    @ApiOperation(value = "查询用户列表", notes = "分页查询用户列表")
    public Result<Page<User>> getUsers(
            @ApiParam(value = "页码", example = "0") @RequestParam(defaultValue = "0") int page,
            @ApiParam(value = "每页大小", example = "10") @RequestParam(defaultValue = "10") int size,
            @ApiParam(value = "排序字段", example = "createdAt") @RequestParam(defaultValue = "createdAt") String sortBy,
            @ApiParam(value = "排序方向", example = "DESC") @RequestParam(defaultValue = "DESC") String sortDir) {

        Sort.Direction direction = Sort.Direction.fromString(sortDir != null ? sortDir : "DESC");
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy != null ? sortBy : "createdAt"));
        Page<User> users = userService.getUsers(pageable);

        return Result.success(users);
    }

    /**
     * 获取企业的用户列表
     * GET /api/users/enterprise/{enterpriseId}
     */
    @GetMapping("/enterprise/{enterpriseId}")
    @ApiOperation(value = "获取企业用户", notes = "查询指定企业的所有用户")
    public Result<java.util.List<User>> getEnterpriseUsers(
            @ApiParam(value = "企业ID", required = true) @PathVariable @NonNull String enterpriseId) {
        java.util.List<User> users = userService.getUsersByEnterpriseId(enterpriseId);
        return Result.success(users);
    }

    /**
     * 搜索用户
     * GET /api/users/search
     */
    @GetMapping("/search")
    @ApiOperation(value = "搜索用户", notes = "按真实姓名搜索用户")
    public Result<java.util.List<User>> searchUsers(
            @ApiParam(value = "搜索关键词", required = true) @RequestParam String keyword) {
        java.util.List<User> users = userService.searchUsersByRealName(keyword);
        return Result.success(users);
    }

    /**
     * 获取企业的待审核用户列表
     * GET /api/users/enterprise/{enterpriseId}/pending
     */
    @GetMapping("/enterprise/{enterpriseId}/pending")
    @ApiOperation(value = "获取待审核用户", notes = "查询指定企业的所有待审核用户")
    @RequireEnterpriseAccess
    public Result<java.util.List<User>> getPendingUsers(
            @ApiParam(value = "企业ID", required = true) @PathVariable @NonNull String enterpriseId) {
        java.util.List<User> users = userService.getPendingUsersByEnterprise(enterpriseId);
        return Result.success(users);
    }

    /**
     * 审核通过用户注册
     * PUT /api/users/{userId}/approve
     */
    @PutMapping("/{userId}/approve")
    @ApiOperation(value = "审核通过用户", notes = "企业审核通过用户注册申请")
    @RequireEnterpriseAdmin
    public Result<String> approveUser(
            @ApiParam(value = "用户ID", required = true) @PathVariable @NonNull String userId,
            Authentication authentication) {
        String approver = authentication.getName();

        log.info("==================== 接收到用户审核通过请求 ====================");
        log.info("审核信息: userId={}, approver={}", userId, approver);

        long startTime = System.currentTimeMillis();

        try {
            log.debug("调用UserService审核用户");
            userService.approveUserRegistration(userId, approver, authentication);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 用户审核通过请求处理完成: userId={}, 耗时={}ms", userId, duration);
            log.info("==================== 用户审核通过请求结束 ====================");

            return Result.success("用户审核通过");

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 用户审核通过请求处理失败: userId={}, 耗时={}ms, error={}",
                     userId, duration, e.getMessage(), e);
            log.info("==================== 用户审核通过请求失败（结束） ====================");
            throw e;
        }
    }

    /**
     * 拒绝用户注册
     * PUT /api/users/{userId}/reject
     */
    @PutMapping("/{userId}/reject")
    @ApiOperation(value = "拒绝用户注册", notes = "企业拒绝用户注册申请")
    @RequireEnterpriseAdmin
    public Result<String> rejectUser(
            @ApiParam(value = "用户ID", required = true) @PathVariable @NonNull String userId,
            @RequestBody(required = false) String reason,
            Authentication authentication) {
        String approver = authentication.getName();
        userService.rejectUserRegistration(userId, approver, reason, authentication);
        return Result.success("用户注册已拒绝");
    }

    /**
     * 解锁用户
     * POST /api/users/{userId}/unlock
     */
    @PostMapping("/{userId}/unlock")
    @ApiOperation(value = "解锁用户", notes = "解锁被锁定的用户账户")
    @RequireEnterpriseAdmin
    public Result<String> unlockUser(
            @ApiParam(value = "用户ID", required = true) @PathVariable @NonNull String userId,
            Authentication authentication) {
        String operator = authentication.getName();
        userService.unlockUser(userId, operator);
        return Result.success("用户解锁成功");
    }

    /**
     * 查询用户活动日志
     * GET /api/users/activity
     */
    @GetMapping("/activity")
    @ApiOperation(value = "查询用户活动日志", notes = "分页查询用户登录、操作等活动记录")
    @RequireEnterpriseAdmin
    public Result<org.springframework.data.domain.Page<com.fisco.app.dto.user.UserActivityDTO>> getUserActivities(
            @ApiParam(value = "用户ID") @RequestParam(required = false) String userId,
            @ApiParam(value = "用户名") @RequestParam(required = false) String username,
            @ApiParam(value = "活动类型") @RequestParam(required = false) com.fisco.app.entity.user.UserActivity.ActivityType activityType,
            @ApiParam(value = "操作模块") @RequestParam(required = false) String module,
            @ApiParam(value = "操作结果") @RequestParam(required = false) com.fisco.app.entity.user.UserActivity.ActivityResult result,
            @ApiParam(value = "IP地址") @RequestParam(required = false) String ipAddress,
            @ApiParam(value = "开始时间", example = "2024-01-01T00:00:00") @RequestParam(required = false) java.time.LocalDateTime startDate,
            @ApiParam(value = "结束时间", example = "2024-12-31T23:59:59") @RequestParam(required = false) java.time.LocalDateTime endDate,
            @ApiParam(value = "页码", example = "0") @RequestParam(defaultValue = "0") int page,
            @ApiParam(value = "每页大小", example = "10") @RequestParam(defaultValue = "10") int size,
            @ApiParam(value = "排序字段", example = "createdAt") @RequestParam(defaultValue = "createdAt") String sortBy,
            @ApiParam(value = "排序方向", example = "DESC") @RequestParam(defaultValue = "DESC") String sortDir) {

        com.fisco.app.dto.user.UserActivityQueryRequest request = new com.fisco.app.dto.user.UserActivityQueryRequest();
        request.setUserId(userId);
        request.setUsername(username);
        request.setActivityType(activityType);
        request.setModule(module);
        request.setResult(result);
        request.setIpAddress(ipAddress);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setPage(page);
        request.setSize(size);
        request.setSortField(sortBy);
        request.setSortDirection(sortDir);

        org.springframework.data.domain.Page<com.fisco.app.dto.user.UserActivityDTO> activities =
            userActivityService.queryActivities(request);

        return Result.success(activities);
    }

    /**
     * 设置用户权限
     * POST /api/users/{userId}/permissions
     */
    @PostMapping("/{userId}/permissions")
    @ApiOperation(value = "设置用户权限", notes = "为用户设置细粒度权限列表")
    @RequireEnterpriseAdmin
    public Result<java.util.List<com.fisco.app.dto.user.UserPermissionDTO>> setUserPermissions(
            @ApiParam(value = "用户ID", required = true) @PathVariable @NonNull String userId,
            @Valid @RequestBody com.fisco.app.dto.user.SetUserPermissionsRequest request,
            Authentication authentication) {
        String operator = authentication.getName();

        java.util.List<com.fisco.app.dto.user.UserPermissionDTO> permissions =
            userPermissionService.setUserPermissions(userId, request.getPermissions(), operator);

        return Result.success("用户权限设置成功", permissions);
    }

    /**
     * 获取用户权限
     * GET /api/users/{userId}/permissions
     */
    @GetMapping("/{userId}/permissions")
    @ApiOperation(value = "获取用户权限", notes = "查询用户的所有权限")
    @RequireEnterpriseAdmin
    public Result<java.util.List<com.fisco.app.dto.user.UserPermissionDTO>> getUserPermissions(
            @ApiParam(value = "用户ID", required = true) @PathVariable @NonNull String userId) {

        java.util.List<com.fisco.app.dto.user.UserPermissionDTO> permissions =
            userPermissionService.getUserPermissions(userId);

        return Result.success(permissions);
    }

    /**
     * 删除用户权限
     * DELETE /api/users/{userId}/permissions/{permissionId}
     */
    @DeleteMapping("/{userId}/permissions/{permissionId}")
    @ApiOperation(value = "删除用户权限", notes = "删除用户的指定权限")
    @RequireEnterpriseAdmin
    public Result<String> deleteUserPermission(
            @ApiParam(value = "用户ID", required = true) @PathVariable @NonNull String userId,
            @ApiParam(value = "权限ID", required = true) @PathVariable @NonNull String permissionId,
            Authentication authentication) {
        String operator = authentication.getName();
        userPermissionService.deleteUserPermission(userId, permissionId, operator);
        return Result.success("用户权限删除成功");
    }

    /**
     * 启用/禁用用户权限
     * PUT /api/users/{userId}/permissions/{permissionId}/toggle
     */
    @PutMapping("/{userId}/permissions/{permissionId}/toggle")
    @ApiOperation(value = "启用/禁用用户权限", notes = "启用或禁用用户的指定权限")
    @RequireEnterpriseAdmin
    public Result<String> toggleUserPermission(
            @ApiParam(value = "用户ID", required = true) @PathVariable @NonNull String userId,
            @ApiParam(value = "权限ID", required = true) @PathVariable @NonNull String permissionId,
            @ApiParam(value = "是否启用", required = true) @RequestParam boolean enabled,
            Authentication authentication) {
        String operator = authentication.getName();
        userPermissionService.toggleUserPermission(userId, permissionId, enabled, operator);
        return Result.success(enabled ? "用户权限已启用" : "用户权限已禁用");
    }

    // ==================== DTO类 ====================

    @Data
    @Schema(name = "创建用户请求")
    public static class CreateUserRequest {
        @NotBlank(message = "用户名不能为空")
        @io.swagger.annotations.ApiModelProperty(value = "用户名（登录账号）", required = true, example = "zhangsan")
        private String username;

        @NotBlank(message = "密码不能为空")
        @io.swagger.annotations.ApiModelProperty(value = "登录密码", required = true, example = "Pass123456")
        private String password;

        @NotBlank(message = "真实姓名不能为空")
        @io.swagger.annotations.ApiModelProperty(value = "真实姓名", required = true, example = "张三")
        private String realName;

        @io.swagger.annotations.ApiModelProperty(value = "电子邮箱", example = "zhangsan@example.com")
        private String email;

        @io.swagger.annotations.ApiModelProperty(value = "手机号码", example = "13800138000")
        private String phone;

        @io.swagger.annotations.ApiModelProperty(value = "用户类型", example = "ENTERPRISE_USER",
            notes = "可选值: ADMIN-系统管理员, ENTERPRISE_ADMIN-企业管理员, ENTERPRISE_USER-企业用户, AUDITOR-审计员, OPERATOR-操作员")
        private User.UserType userType;

        @io.swagger.annotations.ApiModelProperty(value = "部门", example = "财务部")
        private String department;

        @io.swagger.annotations.ApiModelProperty(value = "职位", example = "财务经理")
        private String position;
    }

    @Data
    @Schema(name = "更新用户请求")
    public static class UpdateUserRequest {
        @NotBlank(message = "用户名不能为空")
        @io.swagger.annotations.ApiModelProperty(value = "用户名", required = true)
        private String username;

        @io.swagger.annotations.ApiModelProperty(value = "真实姓名")
        private String realName;

        @io.swagger.annotations.ApiModelProperty(value = "电子邮箱")
        private String email;

        @io.swagger.annotations.ApiModelProperty(value = "手机号码")
        private String phone;

        @io.swagger.annotations.ApiModelProperty(value = "部门")
        private String department;

        @io.swagger.annotations.ApiModelProperty(value = "职位")
        private String position;

        @io.swagger.annotations.ApiModelProperty(value = "头像URL")
        private String avatarUrl;

        @io.swagger.annotations.ApiModelProperty(value = "用户类型", notes = "ADMIN-系统管理员, ENTERPRISE_ADMIN-企业管理员, ENTERPRISE_USER-企业用户, AUDITOR-审计员, OPERATOR-操作员", example = "ENTERPRISE_USER")
        private User.UserType userType;
    }

    @Data
    @Schema(name = "修改密码请求")
    public static class ChangePasswordRequest {
        @NotBlank(message = "原密码不能为空")
        @io.swagger.annotations.ApiModelProperty(value = "原密码", required = true)
        private String oldPassword;

        @NotBlank(message = "新密码不能为空")
        @io.swagger.annotations.ApiModelProperty(value = "新密码", required = true)
        private String newPassword;
    }

    @Data
    @Schema(name = "重置密码请求")
    public static class ResetPasswordRequest {
        @NotBlank(message = "新密码不能为空")
        @io.swagger.annotations.ApiModelProperty(value = "新密码", required = true)
        private String newPassword;
    }

    @Data
    @Schema(name = "设置用户状态请求")
    public static class SetUserStatusRequest {
        @NotNull(message = "用户状态不能为空")
        @io.swagger.annotations.ApiModelProperty(value = "用户状态", required = true)
        private User.UserStatus status;
    }
}
