package com.fisco.app.Modules.Auth;

import java.util.HashMap;
import java.util.Map;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fisco.app.Common.DTO.RefreshTokenRequestDTO;
import com.fisco.app.Common.DTO.TokenResponseDTO;
import com.fisco.app.Common.Service.TokenService;
import com.fisco.app.Common.Utils.JwtUtil;
import com.fisco.app.Modules.Enterprise.Entity.Enterprise;
import com.fisco.app.Modules.Enterprise.Service.EnterpriseService;
import com.fisco.app.Modules.User.Entity.User;
import com.fisco.app.Modules.User.Service.UserService;

import lombok.extern.slf4j.Slf4j;

/**
 * 认证控制器 - 双令牌策略
 * 提供登录、刷新Token等接口
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final TokenService tokenService;
    private final EnterpriseService enterpriseService;
    private final UserService userService;

    /**
     * 构造函数注入服务
     *
     * @param tokenService Token服务
     * @param enterpriseService 企业服务
     * @param userService 用户服务
     */
    public AuthController(TokenService tokenService,
                          EnterpriseService enterpriseService,
                          UserService userService) {
        this.tokenService = tokenService;
        this.enterpriseService = enterpriseService;
        this.userService = userService;
    }

    /**
     * 登录接口 - 验证凭证并生成双令牌
     *
     * @param loginRequest 登录请求（包含username、password、loginType）
     * @return 令牌响应
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, Object> loginRequest) {
        try {
            // 提取登录参数
            String username = loginRequest.containsKey("username")
                    ? loginRequest.get("username").toString() : null;
            String password = loginRequest.containsKey("password")
                    ? loginRequest.get("password").toString() : null;
            String loginType = loginRequest.containsKey("loginType")
                    ? loginRequest.get("loginType").toString() : "ENTERPRISE";

            // 参数校验
            if (username == null || username.isEmpty()) {
                return buildErrorResponse(400, "用户名不能为空");
            }
            if (password == null || password.isEmpty()) {
                return buildErrorResponse(400, "密码不能为空");
            }

            Long userId;
            Long entId;
            String role;
            Integer scope;

            // 根据登录类型验证凭证
            if ("USER".equalsIgnoreCase(loginType)) {
                // 用户登录
                User user = userService.login(username, password);
                if (user == null) {
                    return buildErrorResponse(401, "用户名或密码错误");
                }
                userId = user.getUserId();
                entId = user.getEnterpriseId();
                role = user.getUserRole() != null ? user.getUserRole() : "USER";
                scope = 1;
            } else {
                // 企业登录（默认）
                Enterprise enterprise = enterpriseService.login(username, password);
                if (enterprise == null) {
                    return buildErrorResponse(401, "用户名或密码错误");
                }
                userId = null;
                entId = enterprise.getEntId();
                role = "ENTERPRISE";
                scope = 5;
            }

            // 生成令牌对
            Map<String, String> tokenPair = tokenService.generateTokenPair(userId, entId, role, scope);

            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "登录成功");

            TokenResponseDTO tokenResponse = TokenResponseDTO.of(
                    tokenPair.get("accessToken"),
                    tokenPair.get("refreshToken"),
                    JwtUtil.ACCESS_TOKEN_EXPIRATION / 1000, // 转换为秒
                    userId,
                    entId
            );
            response.put("data", tokenResponse);

            log.info("用户登录成功，用户ID: {}, 企业ID: {}", userId, entId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            // 业务异常返回400或401
            log.warn("登录业务异常: {}", e.getMessage());
            return buildErrorResponse(400, e.getMessage());
        } catch (Exception e) {
            log.error("登录失败: {}", e.getMessage(), e);
            return buildErrorResponse(500, "登录失败，请稍后重试");
        }
    }

    /**
     * 构建错误响应
     *
     * @param code 状态码
     * @param message 错误消息
     * @return 错误响应
     */
    private ResponseEntity<Map<String, Object>> buildErrorResponse(int code, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("code", code);
        error.put("message", message);
        if (code >= 500) {
            return ResponseEntity.status(code).body(error);
        } else if (code >= 400) {
            return ResponseEntity.badRequest().body(error);
        } else {
            return ResponseEntity.status(code).body(error);
        }
    }

    /**
     * 刷新Token接口
     * 使用Refresh Token获取新的Access Token
     *
     * @param request 刷新Token请求
     * @return 新的令牌响应
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken(@Valid @RequestBody RefreshTokenRequestDTO request) {
        try {
            // 验证请求参数
            if (!request.isValid()) {
                Map<String, Object> error = new HashMap<>();
                error.put("code", 400);
                error.put("message", "Refresh Token不能为空");
                return ResponseEntity.badRequest().body(error);
            }

            // 刷新Token
            Map<String, String> newTokenPair = tokenService.refreshToken(request.getRefreshToken());

            if (newTokenPair == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("code", 401);
                error.put("message", "Refresh Token无效或已过期");
                return ResponseEntity.status(401).body(error);
            }

            // 解析获取用户信息
            String accessToken = newTokenPair.get("accessToken");
            Map<String, Object> userInfo = tokenService.parseAccessToken(accessToken);

            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "Token刷新成功");

            TokenResponseDTO tokenResponse = TokenResponseDTO.of(
                    accessToken,
                    newTokenPair.get("refreshToken"),
                    JwtUtil.ACCESS_TOKEN_EXPIRATION / 1000,
                    (Long) userInfo.get("userId"),
                    (Long) userInfo.get("entId")
            );
            response.put("data", tokenResponse);

            log.info("Token刷新成功，用户ID: {}", userInfo.get("userId"));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Token刷新失败: {}", e.getMessage(), e);
            return buildErrorResponse(500, "Token刷新失败，请稍后重试");
        }
    }

    /**
     * 健康检查接口 - 验证Token有效性
     *
     * @param request 包含Access Token的请求
     * @return Token信息
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestBody Map<String, String> request) {
        try {
            String accessToken = request.get("accessToken");
            if (accessToken == null || accessToken.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("code", 400);
                error.put("message", "Access Token不能为空");
                return ResponseEntity.badRequest().body(error);
            }

            // 验证Token
            boolean valid = tokenService.validateAccessToken(accessToken);

            Map<String, Object> response = new HashMap<>();
            if (valid) {
                // 解析用户信息
                Map<String, Object> userInfo = tokenService.parseAccessToken(accessToken);
                response.put("code", 200);
                response.put("message", "Token有效");
                response.put("data", userInfo);
            } else {
                response.put("code", 401);
                response.put("message", "Token无效或已过期");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Token验证失败: {}", e.getMessage(), e);
            return buildErrorResponse(500, "Token验证失败，请稍后重试");
        }
    }
}
