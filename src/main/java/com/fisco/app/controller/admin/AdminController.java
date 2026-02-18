package com.fisco.app.controller.admin;

import com.fisco.app.entity.user.Admin;
import com.fisco.app.security.JwtTokenProvider;
import com.fisco.app.security.RequireAdmin;
import com.fisco.app.service.user.AdminService;
import com.fisco.app.vo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.lang.NonNull;

/**
 * 管理员认证Controller
 * 处理管理员登录、令牌获取等
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
@Api(tags = "管理员认证管理")
public class AdminController {

    private final JwtTokenProvider jwtTokenProvider;
    private final AdminService adminService;

    /**
     * 管理员登录
     * POST /api/admin/auth/login
     *
     * 注意：此为公开接口，无需Token即可访问
     */
    @PostMapping("/login")
    @ApiOperation(value = "管理员登录（公开接口）",
                  notes = "【公开接口】管理员使用用户名和密码登录，返回JWT Token用于后续认证。")
    @io.swagger.annotations.ApiResponses({
        @io.swagger.annotations.ApiResponse(code = 200, message = "登录成功"),
        @io.swagger.annotations.ApiResponse(code = 401, message = "用户名或密码错误"),
        @io.swagger.annotations.ApiResponse(code = 403, message = "管理员账户已禁用"),
        @io.swagger.annotations.ApiResponse(code = 404, message = "管理员不存在")
    })
    public Result<Map<String, Object>> adminLogin(@Valid @RequestBody AdminLoginRequest request,
                                                   HttpServletRequest httpRequest) {
        log.info("Admin login attempt: username={}", request.getUsername());

        try {
            // 验证用户名和密码
            Admin admin = adminService.validateLogin(request.getUsername(), request.getPassword());

            // 更新最后登录信息
            String ip = getClientIp(httpRequest);
            String adminId = admin.getId();
            if (adminId == null) {
                log.error("Admin ID is null after validation for username={}", admin.getUsername());
                return Result.error("管理员信息异常");
            }
            if (ip == null) {
                ip = "unknown";
            }
            adminService.updateLastLogin(adminId, ip);

            // 生成增强的JWT令牌（包含角色信息）
            String token = jwtTokenProvider.generateEnhancedToken(
                admin.getUsername(),
                null,  // 管理员不属于企业
                admin.getRole().name(),
                "ADMIN"
            );

            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("type", "Bearer");
            response.put("adminId", admin.getId());
            response.put("username", admin.getUsername());
            response.put("realName", admin.getRealName());
            response.put("role", admin.getRole().name());
            response.put("email", admin.getEmail());
            response.put("loginType", "ADMIN");

            log.info("Admin logged in successfully: username={}, realName={}, role={}",
                    admin.getUsername(), admin.getRealName(), admin.getRole());

            return Result.success("登录成功", response);

        } catch (com.fisco.app.exception.BusinessException e) {
            log.warn("Admin login failed: username={}, error={}",
                    request.getUsername(), e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 验证管理员令牌
     * POST /api/admin/auth/validate
     *
     * 注意：此为公开接口，但需要在请求头中提供Token
     */
    @PostMapping("/validate")
    @ApiOperation(value = "验证管理员令牌（公开接口）",
                  notes = "【公开接口】验证JWT令牌是否有效。需要在请求头中提供Token。")
    @io.swagger.annotations.ApiResponses({
        @io.swagger.annotations.ApiResponse(code = 200, message = "令牌有效"),
        @io.swagger.annotations.ApiResponse(code = 401, message = "令牌无效或已过期")
    })
    public Result<Map<String, Object>> validateToken(
            @RequestHeader("Authorization") String authHeader) {

        String token = jwtTokenProvider.extractTokenFromHeader(authHeader);

        if (token == null || !jwtTokenProvider.validateToken(token)) {
            return Result.error("令牌无效或已过期");
        }

        String subject = jwtTokenProvider.getUserAddressFromToken(token);

        Map<String, Object> response = new HashMap<>();
        response.put("valid", true);
        response.put("username", subject);

        return Result.success("令牌有效", response);
    }

    /**
     * 获取当前管理员信息
     * GET /api/admin/auth/me
     *
     * 需要在请求头中提供有效的Token
     */
    @GetMapping("/me")
    @RequireAdmin(RequireAdmin.AdminRole.AUDITOR)
    @ApiOperation(value = "获取当前管理员信息",
                  notes = "获取当前登录管理员的详细信息。需要在请求头中提供有效的Token。")
    @io.swagger.annotations.ApiResponses({
        @io.swagger.annotations.ApiResponse(code = 200, message = "获取成功"),
        @io.swagger.annotations.ApiResponse(code = 401, message = "未提供Token或Token无效")
    })
    public Result<Admin> getCurrentAdmin(HttpServletRequest request) {
        Admin admin = (Admin) request.getAttribute("currentAdmin");
        if (admin == null) {
            log.warn("Failed to get current admin from request attribute");
            return Result.error("未获取到管理员信息，请重新登录");
        }

        // 清除密码字段，避免返回敏感信息
        admin.setPassword(null);

        log.debug("Get current admin info: username={}, realName={}, role={}",
                admin.getUsername(), admin.getRealName(), admin.getRole());

        return Result.success(admin);
    }

    /**
     * 修改当前管理员密码
     * PUT /api/admin/auth/change-password
     *
     * 需要在请求头中提供有效的Token
     */
    @PutMapping("/change-password")
    @RequireAdmin(RequireAdmin.AdminRole.AUDITOR)
    @ApiOperation(value = "修改当前管理员密码",
                  notes = "管理员修改自己的密码。需要提供旧密码和新密码。")
    @io.swagger.annotations.ApiResponses({
        @io.swagger.annotations.ApiResponse(code = 200, message = "密码修改成功"),
        @io.swagger.annotations.ApiResponse(code = 400, message = "旧密码错误或新密码不符合要求"),
        @io.swagger.annotations.ApiResponse(code = 401, message = "未提供Token或Token无效")
    })
    public Result<String> changePassword(
            @Valid @RequestBody AdminChangePasswordRequest request,
            HttpServletRequest httpRequest) {

        Admin admin = (Admin) httpRequest.getAttribute("currentAdmin");
        if (admin == null) {
            log.warn("Failed to get current admin from request attribute during password change");
            return Result.error("未获取到管理员信息，请重新登录");
        }

        String adminId = Objects.requireNonNull(admin.getId(), "Admin ID cannot be null");
        String updatedBy = Objects.requireNonNull(admin.getUsername(), "Admin username cannot be null");

        try {
            adminService.changePassword(
                    adminId,
                    request.getOldPassword(),
                    request.getNewPassword(),
                    updatedBy
            );

            log.info("Admin password changed successfully: adminId={}, username={}",
                    adminId, admin.getUsername());

            return Result.success("密码修改成功，请使用新密码重新登录");

        } catch (com.fisco.app.exception.BusinessException e) {
            log.warn("Admin password change failed: adminId={}, error={}",
                    adminId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 刷新访问令牌
     * POST /api/admin/auth/refresh-token
     *
     * 使用当前有效的Token获取新的访问令牌
     */
    @PostMapping("/refresh-token")
    @RequireAdmin(RequireAdmin.AdminRole.AUDITOR)
    @ApiOperation(value = "刷新访问令牌",
                  notes = "在令牌有效期内刷新访问令牌。需要在请求头中提供当前有效的Token。")
    @io.swagger.annotations.ApiResponses({
        @io.swagger.annotations.ApiResponse(code = 200, message = "令牌刷新成功"),
        @io.swagger.annotations.ApiResponse(code = 401, message = "当前令牌已过期，请重新登录")
    })
    public Result<Map<String, String>> refreshToken(
            @RequestHeader("Authorization") String authHeader) {

        String token = jwtTokenProvider.extractTokenFromHeader(authHeader);

        // 验证当前令牌是否有效
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            log.warn("Token refresh failed: invalid or expired token");
            return Result.error("当前令牌已过期，请重新登录");
        }

        try {
            // 从令牌中提取信息
            String username = jwtTokenProvider.getUserAddressFromToken(token);
            String role = jwtTokenProvider.getRoleFromToken(token);
            String loginType = jwtTokenProvider.getLoginTypeFromToken(token);

            // 生成新令牌
            String newToken = jwtTokenProvider.generateEnhancedToken(
                    username,
                    null,  // 管理员不属于企业
                    role,
                    loginType != null ? loginType : "ADMIN"
            );

            Map<String, String> response = new HashMap<>();
            response.put("token", newToken);
            response.put("type", "Bearer");

            log.info("Admin token refreshed successfully: username={}", username);
            return Result.success("令牌刷新成功", response);

        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            return Result.error("令牌刷新失败，请重新登录");
        }
    }

    /**
     * 管理员登出
     * POST /api/admin/auth/logout
     *
     * JWT无状态认证，登出操作由前端删除Token即可
     * 后端仅记录登出日志用于审计
     */
    @PostMapping("/logout")
    @RequireAdmin(RequireAdmin.AdminRole.AUDITOR)
    @ApiOperation(value = "管理员登出",
                  notes = "管理员退出登录。前端需要删除存储的Token。后端记录登出日志用于审计。")
    @io.swagger.annotations.ApiResponses({
        @io.swagger.annotations.ApiResponse(code = 200, message = "登出成功")
    })
    public Result<String> logout(HttpServletRequest request) {
        Admin admin = (Admin) request.getAttribute("currentAdmin");
        if (admin != null) {
            log.info("Admin logged out: username={}, realName={}, role={}",
                    admin.getUsername(), admin.getRealName(), admin.getRole());
        } else {
            log.info("Admin logout attempt: no admin info in request");
        }

        return Result.success("登出成功");
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        // 处理多个IP的情况（X-Forwarded-For可能包含多个IP）
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    // ==================== DTO类 ====================

    /**
     * 管理员登录请求DTO
     */
    @Data
    public static class AdminLoginRequest {
        @NotBlank(message = "用户名不能为空")
        @org.springframework.lang.NonNull
        @io.swagger.annotations.ApiModelProperty(value = "管理员用户名", required = true, example = "admin")
        private String username;

        @NotBlank(message = "密码不能为空")
        @org.springframework.lang.NonNull
        @io.swagger.annotations.ApiModelProperty(value = "登录密码", required = true)
        private String password;
    }

    /**
     * 管理员修改密码请求DTO
     */
    @Data
    @io.swagger.annotations.ApiModel(value = "管理员修改密码请求", description = "管理员修改自己的密码")
    public static class AdminChangePasswordRequest {
        @NonNull
        @NotBlank(message = "旧密码不能为空")
        @io.swagger.annotations.ApiModelProperty(value = "旧密码", required = true, notes = "当前使用的密码")
        private String oldPassword;

        @NonNull
        @NotBlank(message = "新密码不能为空")
        @io.swagger.annotations.ApiModelProperty(value = "新密码", required = true, notes = "新设置的密码")
        private String newPassword;
    }

    /**
     * 管理员刷新令牌请求DTO
     * 注意：当前实现从Authorization header获取token，此DTO为未来扩展预留
     */
    @Data
    @io.swagger.annotations.ApiModel(value = "管理员刷新令牌请求", description = "刷新访问令牌（当前从header获取token）")
    public static class AdminRefreshTokenRequest {
        @io.swagger.annotations.ApiModelProperty(value = "刷新令牌", notes = "当前实现从Authorization header获取，此字段保留用于未来扩展")
        private String refreshToken;
    }
}
