package com.fisco.app.Modules.User.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.ApiParam;

import com.fisco.app.Common.Utils.JwtUtil;
import com.fisco.app.Common.Utils.Result;
import com.fisco.app.Modules.User.Entity.User;
import com.fisco.app.Modules.User.Service.UserService;
import com.fisco.app.Modules.User.Service.UserService.CancellationResult;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import com.fisco.app.Common.Annotation.RequireRole;

/**
 * 用户管理 Controller
 *
 * 提供用户注册、登录、信息管理等 API
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Api(tags = "用户管理")
@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    // ==================== 用户注册 ====================

    /**
     * 用户注册（员工入职）
     */
    @ApiOperation("员工注册")
    @PostMapping("/register")
    public Result<Map<String, Object>> registerUser(@RequestBody UserRegisterRequest request) {
        try {
            // 参数校验
            if (request.getUsername() == null || request.getUsername().isEmpty()) {
                return Result.error(400, "用户名不能为空");
            }
            if (request.getPassword() == null || request.getPassword().isEmpty()) {
                return Result.error(400, "密码不能为空");
            }
            if (request.getInviteCode() == null || request.getInviteCode().isEmpty()) {
                return Result.error(400, "邀请码不能为空");
            }
            if (request.getRealName() == null || request.getRealName().isEmpty()) {
                return Result.error(400, "真实姓名不能为空");
            }

            Long userId = userService.registerUser(
                    request.getUsername(),
                    request.getPassword(),
                    request.getInviteCode(),
                    request.getRealName(),
                    request.getPhone(),
                    request.getEmail()
            );

            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId);
            result.put("message", "注册成功，等待企业管理员审核");

            logger.info("员工注册成功: userId={}, username={}", userId, request.getUsername());
            return Result.success(result);

        } catch (IllegalArgumentException e) {
            logger.warn("员工注册参数错误: {}", e.getMessage());
            return Result.error(400, "请求参数无效");
        } catch (Exception e) {
            logger.error("员工注册异常", e);
            return Result.error(500, "注册失败，请稍后重试");
        }
    }

    // ==================== 用户登录 ====================

    /**
     * 用户登录
     */
    @ApiOperation("员工登录")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request) {
        try {
            if (request.getUsername() == null || request.getUsername().isEmpty()) {
                return Result.error(400, "用户名不能为空");
            }
            if (request.getPassword() == null || request.getPassword().isEmpty()) {
                return Result.error(400, "密码不能为空");
            }

            User user = userService.login(request.getUsername(), request.getPassword());
            if (user == null) {
                return Result.error(401, "用户名或密码错误");
            }

            // 生成JWT令牌
            Map<String, String> tokens = JwtUtil.createTokenPair(
                    user.getUserId(),
                    user.getEnterpriseId(),
                    user.getUserRole(),
                    null
            );

            Map<String, Object> result = new HashMap<>();
            result.put("userId", user.getUserId());
            result.put("username", user.getUsername());
            result.put("realName", user.getRealName());
            result.put("enterpriseId", user.getEnterpriseId());
            result.put("userRole", user.getUserRole());
            result.put("status", user.getStatus());
            result.put("accessToken", tokens.get("accessToken"));
            result.put("refreshToken", tokens.get("refreshToken"));

            logger.info("员工登录成功: userId={}, username={}", user.getUserId(), user.getUsername());
            return Result.success(result);

        } catch (IllegalStateException e) {
            logger.warn("员工登录失败: {}", e.getMessage());
            return Result.error(403, "无权访问");
        } catch (Exception e) {
            logger.error("员工登录异常", e);
            return Result.error(500, "登录失败，请稍后重试");
        }
    }

    // ==================== 个人信息管理 ====================

    /**
     * 获取个人资料
     */
    @ApiOperation("获取个人资料")
    @GetMapping("/profile")
    public Result<User> getProfile(javax.servlet.http.HttpServletRequest request) {
        try {
            // 仅从JWT获取userId，防止越权
            Object userIdAttr = request.getAttribute("user_id");
            if (userIdAttr == null) {
                return Result.error(401, "未登录或Token无效");
            }
            Long userId = Long.parseLong(userIdAttr.toString());

            User user = userService.getUserById(userId);
            if (user == null) {
                return Result.error(404, "用户不存在");
            }

            return Result.success(user);

        } catch (Exception e) {
            logger.error("获取个人资料异常", e);
            return Result.error(500, "获取个人资料失败");
        }
    }

    /**
     * 修改个人信息
     */
    @ApiOperation("修改个人信息")
    @PutMapping("/update")
    public Result<Void> updateUserInfo(@RequestBody UserUpdateRequest request, javax.servlet.http.HttpServletRequest httpRequest) {
        try {
            // 仅从JWT获取userId，防止越权
            Object userIdAttr = httpRequest.getAttribute("user_id");
            if (userIdAttr == null) {
                return Result.error(401, "未登录或Token无效");
            }
            Long userId = Long.parseLong(userIdAttr.toString());

            User user = new User();
            user.setUserId(userId);
            user.setRealName(request.getRealName());
            user.setPhone(request.getPhone());
            user.setEmail(request.getEmail());

            boolean success = userService.updateUserInfo(user);

            if (success) {
                logger.info("用户信息已更新: userId={}", userId);
                return Result.success(null);
            } else {
                return Result.error(500, "更新失败");
            }

        } catch (IllegalArgumentException e) {
            return Result.error(400, "请求参数无效");
        } catch (Exception e) {
            logger.error("修改个人信息异常", e);
            return Result.error(500, "修改个人信息失败");
        }
    }

    /**
     * 修改登录密码
     */
    @ApiOperation("修改登录密码")
    @PostMapping("/password")
    public Result<Void> updatePassword(@RequestBody PasswordUpdateRequest request, javax.servlet.http.HttpServletRequest httpRequest) {
        try {
            // 仅从JWT获取userId，防止越权
            Object userIdAttr = httpRequest.getAttribute("user_id");
            if (userIdAttr == null) {
                return Result.error(401, "未登录或Token无效");
            }
            Long userId = Long.parseLong(userIdAttr.toString());

            if (request.getOldPassword() == null || request.getOldPassword().isEmpty()) {
                return Result.error(400, "原密码不能为空");
            }
            if (request.getNewPassword() == null || request.getNewPassword().isEmpty()) {
                return Result.error(400, "新密码不能为空");
            }

            boolean success = userService.updatePassword(
                    userId,
                    request.getOldPassword(),
                    request.getNewPassword()
            );

            if (success) {
                logger.info("用户密码已更新: userId={}", userId);
                return Result.success(null);
            } else {
                return Result.error(500, "修改密码失败");
            }

        } catch (IllegalArgumentException e) {
            return Result.error(400, "请求参数无效");
        } catch (Exception e) {
            logger.error("修改密码异常: userId={}", request.getUserId(), e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    // ==================== 用户注销管理 ====================

    /**
     * 发起注销申请
     */
    @ApiOperation("发起注销申请")
    @PostMapping("/cancel/apply")
    public Result<CancellationResult> applyCancellation(
            @RequestBody CancellationApplyRequest request,
            javax.servlet.http.HttpServletRequest httpRequest) {
        try {
            // JWT 自动获取 userId
            if (request.getUserId() == null) {
                Object userIdAttr = httpRequest.getAttribute("user_id");
                if (userIdAttr != null) {
                    request.setUserId(Long.parseLong(userIdAttr.toString()));
                }
            }
            if (request.getUserId() == null) {
                return Result.error(400, "用户ID不能为空");
            }
            if (request.getPassword() == null || request.getPassword().isEmpty()) {
                return Result.error(400, "密码不能为空");
            }

            CancellationResult result = userService.applyCancellation(
                    request.getUserId(),
                    request.getReason(),
                    request.getPassword()
            );

            if (result.isSuccess()) {
                logger.info("用户注销申请成功: userId={}", request.getUserId());
                return Result.success(result);
            } else {
                return Result.error(400, result.getMessage());
            }

        } catch (IllegalArgumentException e) {
            return Result.error(400, "请求参数无效");
        } catch (Exception e) {
            logger.error("发起注销申请异常: userId={}", request.getUserId(), e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    /**
     * 撤回注销申请
     */
    @ApiOperation("撤回注销申请")
    @PostMapping("/cancel/revoke")
    public Result<Void> revokeCancellation(
            @RequestParam(required = false) Long userId,
            javax.servlet.http.HttpServletRequest request) {
        try {
            // JWT 自动获取 userId
            if (userId == null) {
                Object userIdAttr = request.getAttribute("user_id");
                if (userIdAttr != null) {
                    userId = Long.parseLong(userIdAttr.toString());
                }
            }
            if (userId == null) {
                return Result.error(400, "用户ID不能为空");
            }

            boolean success = userService.revokeCancellation(userId);
            if (success) {
                logger.info("用户注销申请已撤回: userId={}", userId);
                return Result.success(null);
            } else {
                return Result.error(500, "撤回注销申请失败");
            }

        } catch (IllegalArgumentException e) {
            return Result.error(400, "请求参数无效");
        } catch (Exception e) {
            logger.error("撤回注销申请异常: userId={}", userId, e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    /**
     * 获取待审核注销用户列表（仅管理员可访问）
     */
    @ApiOperation("获取待审核注销用户列表")
    @RequireRole(value = {"ADMIN"}, adminBypass = true)
    @GetMapping("/cancel/pending")
    public Result<List<User>> getPendingCancellationUsers(
            @RequestParam(required = false) Long enterpriseId,
            javax.servlet.http.HttpServletRequest request) {
        try {
            List<User> list = userService.getPendingCancellationUsers(enterpriseId);
            return Result.success(list);
        } catch (Exception e) {
            logger.error("获取待审核注销用户列表异常", e);
            return Result.error(500, "查询失败，请稍后重试");
        }
    }

    /**
     * 审核用户注销申请（仅管理员可访问）
     */
    @ApiOperation("审核用户注销申请")
    @RequireRole(value = {"ADMIN"}, adminBypass = true)
    @PostMapping("/{userId}/cancel/audit")
    public Result<Void> auditCancellation(
            @ApiParam(value = "用户ID", required = true)
            @PathVariable Long userId,
            @ApiParam(value = "审核结果: true-通过(设为已注销), false-拒绝(恢复正常)", required = true)
            @RequestBody AuditUserRequest request) {
        try {
            if (userId == null) {
                return Result.error(400, "用户ID不能为空");
            }
            if (request.getApproved() == null) {
                return Result.error(400, "审核结果不能为空");
            }

            boolean success = userService.auditCancellation(userId, request.getApproved());

            if (success) {
                logger.info("用户注销审核完成: userId={}, approved={}", userId, request.getApproved());
                return Result.success(null);
            } else {
                return Result.error(500, "审核用户注销失败");
            }
        } catch (IllegalArgumentException e) {
            return Result.error(400, "请求参数无效");
        } catch (Exception e) {
            logger.error("审核用户注销异常: userId={}", userId, e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    // ==================== 企业内管理 ====================

    /**
     * 查询企业员工列表
     */
    @ApiOperation("查询企业员工列表")
    @GetMapping("/list")
    public Result<List<User>> getUserList(
            @RequestParam(required = false) Long enterpriseId,
            javax.servlet.http.HttpServletRequest request) {
        try {
            // 如果未指定enterpriseId，从当前登录用户获取
            if (enterpriseId == null) {
                Object entIdAttr = request.getAttribute("ent_id");
                if (entIdAttr != null) {
                    enterpriseId = Long.parseLong(entIdAttr.toString());
                }
            }
            if (enterpriseId == null) {
                return Result.error(400, "企业ID不能为空，请先登录企业账号或指定enterpriseId");
            }

            List<User> list = userService.getUsersByEnterpriseId(enterpriseId);
            return Result.success(list);

        } catch (Exception e) {
            logger.error("查询企业员工列表异常: enterpriseId={}", enterpriseId, e);
            return Result.error(500, "查询失败，请稍后重试");
        }
    }

    /**
     * 获取待审核用户列表（仅管理员可访问）
     */
    @ApiOperation("获取待审核用户列表")
    @RequireRole(value = {"ADMIN"}, adminBypass = true)
    @GetMapping("/pending")
    public Result<List<User>> getPendingUsers(
            @RequestParam(required = false) Long enterpriseId,
            javax.servlet.http.HttpServletRequest request) {
        try {
            List<User> list = userService.getPendingUsers(enterpriseId);
            return Result.success(list);
        } catch (Exception e) {
            logger.error("获取待审核用户列表异常", e);
            return Result.error(500, "查询失败，请稍后重试");
        }
    }

    /**
     * 审核用户注册申请（仅管理员可访问）
     */
    @ApiOperation("审核用户注册申请")
    @RequireRole(value = {"ADMIN"}, adminBypass = true)
    @PostMapping("/{userId}/audit")
    public Result<Void> auditUser(
            @ApiParam(value = "用户ID", required = true)
            @PathVariable Long userId,
            @ApiParam(value = "审核结果: true-通过(设为正常), false-拒绝(设为冻结)", required = true)
            @RequestBody AuditUserRequest request) {
        try {
            if (userId == null) {
                return Result.error(400, "用户ID不能为空");
            }
            if (request.getApproved() == null) {
                return Result.error(400, "审核结果不能为空");
            }

            boolean success = userService.auditUser(userId, request.getApproved());

            if (success) {
                logger.info("用户审核完成: userId={}, approved={}", userId, request.getApproved());
                return Result.success(null);
            } else {
                return Result.error(500, "审核用户失败");
            }
        } catch (IllegalArgumentException e) {
            return Result.error(400, "请求参数无效");
        } catch (Exception e) {
            logger.error("审核用户异常: userId={}", userId, e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    /**
     * 获取指定用户信息
     */
    @ApiOperation("获取指定用户信息")
    @GetMapping("/{userId}")
    public Result<User> getUserById(@PathVariable Long userId) {
        try {
            if (userId == null) {
                return Result.error(400, "用户ID不能为空");
            }

            User user = userService.getUserById(userId);
            if (user == null) {
                return Result.error(404, "用户不存在");
            }

            return Result.success(user);

        } catch (Exception e) {
            logger.error("获取用户信息异常: userId={}", userId, e);
            return Result.error(500, "查询失败，请稍后重试");
        }
    }

    /**
     * 分配员工角色
     */
    @ApiOperation("分配员工角色")
    @PutMapping("/assign_role")
    public Result<Void> assignRole(@RequestBody AssignRoleRequest request) {
        try {
            if (request.getUserId() == null) {
                return Result.error(400, "用户ID不能为空");
            }
            if (request.getUserRole() == null || request.getUserRole().isEmpty()) {
                return Result.error(400, "角色不能为空");
            }

            boolean success = userService.updateUserRole(request.getUserId(), request.getUserRole());

            if (success) {
                logger.info("员工角色已分配: userId={}, role={}", request.getUserId(), request.getUserRole());
                return Result.success(null);
            } else {
                return Result.error(500, "分配角色失败");
            }

        } catch (IllegalArgumentException e) {
            return Result.error(400, "请求参数无效");
        } catch (Exception e) {
            logger.error("分配员工角色异常: userId={}", request.getUserId(), e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    /**
     * 禁用/启用员工
     */
    @ApiOperation("禁用/启用员工")
    @PutMapping("/status")
    public Result<Void> updateUserStatus(@RequestBody UserStatusRequest request) {
        try {
            if (request.getUserId() == null) {
                return Result.error(400, "用户ID不能为空");
            }
            if (request.getStatus() == null) {
                return Result.error(400, "状态不能为空");
            }

            boolean success = userService.updateUserStatus(request.getUserId(), request.getStatus());

            if (success) {
                logger.info("员工状态已更新: userId={}, status={}", request.getUserId(), request.getStatus());
                return Result.success(null);
            } else {
                return Result.error(500, "更新状态失败");
            }

        } catch (IllegalArgumentException e) {
            return Result.error(400, "请求参数无效");
        } catch (Exception e) {
            logger.error("更新员工状态异常: userId={}", request.getUserId(), e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    /**
     * 强制禁用员工
     */
    @ApiOperation("强制禁用员工")
    @PutMapping("/disable/{userId}")
    public Result<Void> disableUser(@PathVariable Long userId) {
        try {
            if (userId == null) {
                return Result.error(400, "用户ID不能为空");
            }

            boolean success = userService.disableUser(userId);

            if (success) {
                logger.info("员工已被强制禁用: userId={}", userId);
                return Result.success(null);
            } else {
                return Result.error(500, "禁用员工失败");
            }

        } catch (IllegalArgumentException e) {
            return Result.error(400, "请求参数无效");
        } catch (Exception e) {
            logger.error("强制禁用员工异常: userId={}", userId, e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    /**
     * 删除用户（离职）
     */
    @ApiOperation("删除用户（离职）")
    @DeleteMapping("/{userId}")
    public Result<Void> deleteUser(@PathVariable Long userId) {
        try {
            if (userId == null) {
                return Result.error(400, "用户ID不能为空");
            }

            boolean success = userService.deleteUser(userId);

            if (success) {
                logger.info("用户已删除: userId={}", userId);
                return Result.success(null);
            } else {
                return Result.error(500, "删除用户失败");
            }

        } catch (IllegalArgumentException e) {
            return Result.error(400, "请求参数无效");
        } catch (Exception e) {
            logger.error("删除用户异常: userId={}", userId, e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    // ==================== 请求对象 ====================

    /**
     * 用户注册请求
     */
    public static class UserRegisterRequest {
        private String username;
        private String password;
        private String inviteCode;
        private String realName;
        private String phone;
        private String email;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getInviteCode() { return inviteCode; }
        public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }
        public String getRealName() { return realName; }
        public void setRealName(String realName) { this.realName = realName; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    /**
     * 登录请求
     */
    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    /**
     * 用户信息更新请求
     */
    public static class UserUpdateRequest {
        private Long userId;
        private String realName;
        private String phone;
        private String email;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getRealName() { return realName; }
        public void setRealName(String realName) { this.realName = realName; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    /**
     * 密码更新请求
     */
    public static class PasswordUpdateRequest {
        private Long userId;
        private String oldPassword;
        private String newPassword;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getOldPassword() { return oldPassword; }
        public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }

    /**
     * 注销申请请求
     */
    public static class CancellationApplyRequest {
        private Long userId;
        private String reason;
        private String password;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    /**
     * 分配角色请求
     */
    public static class AssignRoleRequest {
        private Long userId;
        private String userRole;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getUserRole() { return userRole; }
        public void setUserRole(String userRole) { this.userRole = userRole; }
    }

    /**
     * 用户状态更新请求
     */
    public static class UserStatusRequest {
        private Long userId;
        private Integer status;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
    }

    /**
     * 用户审核请求
     */
    public static class AuditUserRequest {
        private Boolean approved;

        public Boolean getApproved() { return approved; }
        public void setApproved(Boolean approved) { this.approved = approved; }
    }
}
