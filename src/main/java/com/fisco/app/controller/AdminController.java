package com.fisco.app.controller;

import com.fisco.app.entity.Admin;
import com.fisco.app.security.JwtTokenProvider;
import com.fisco.app.service.AdminService;
import com.fisco.app.vo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.Map;

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
}
