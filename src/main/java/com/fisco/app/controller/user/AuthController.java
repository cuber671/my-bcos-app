package com.fisco.app.controller.user;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fisco.app.entity.enterprise.Enterprise;
import com.fisco.app.entity.user.User;
import com.fisco.app.security.JwtTokenProvider;
import com.fisco.app.security.PasswordUtil;
import com.fisco.app.service.enterprise.EnterpriseService;
import com.fisco.app.service.user.UserService;
import com.fisco.app.vo.Result;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 认证Controller
 * 处理用户登录和令牌获取
 * 支持两种认证方式：
 * 1. 用户名密码认证（User实体）- 推荐
 * 2. 企业地址+密码认证（Enterprise实体）- 向后兼容
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Api(tags = "认证管理")
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final EnterpriseService enterpriseService;
    private final UserService userService;

    /**
     * 用户登录（用户名密码方式）- 推荐
     * POST /api/auth/login
     *
     * 优势：
     * - 支持多用户
     * - 细粒度权限控制
     * - 用户级别管理
     *
     * 注意：此为公开接口，无需Token即可访问
     */
    @SuppressWarnings("null")  // Enterprise.address字段nullable=false，IDE无法推断
    @PostMapping("/login")
    @ApiOperation(value = "用户登录（公开接口）",
                  notes = "【公开接口】使用用户名和密码登录，无需Token。返回JWT Token用于后续认证。")
    @io.swagger.annotations.ApiResponses({
        @io.swagger.annotations.ApiResponse(code = 200, message = "登录成功"),
        @io.swagger.annotations.ApiResponse(code = 401, message = "用户名或密码错误"),
        @io.swagger.annotations.ApiResponse(code = 403, message = "账户已禁用"),
        @io.swagger.annotations.ApiResponse(code = 404, message = "用户不存在")
    })
    public Result<Map<String, Object>> userLogin(@Valid @RequestBody UserLoginRequest request,
                                                  HttpServletRequest httpRequest) {
        log.info("User login attempt: username={}", request.getUsername());

        try {
            // 验证用户名和密码
            User user = userService.validateLogin(request.getUsername(), request.getPassword());

            // 更新最后登录信息
            String ip = getClientIp(httpRequest);
            String userId = user.getId();
            if (userId != null) {
                userService.updateLastLogin(userId, ip);
            }

            // 获取企业区块链地址
            String enterpriseAddress = null;
            if (user.getEnterpriseId() != null) {
                try {
                    Enterprise enterprise = enterpriseService.getEnterpriseById(user.getEnterpriseId());
                    if (enterprise != null) {
                        // Enterprise.address字段在数据库中是nullable=false，不会为null
                        enterpriseAddress = enterprise.getAddress();
                    }
                } catch (Exception e) {
                    log.warn("Failed to fetch enterprise address for user: {}", user.getUsername(), e);
                }
            }

            // 生成增强的JWT令牌（包含企业ID、角色和区块链地址）
            String token = jwtTokenProvider.generateTokenWithAddress(
                user.getUsername(),
                user.getEnterpriseId(),
                user.getUserType() != null ? user.getUserType().name() : null,
                "USER",
                enterpriseAddress
            );

            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("type", "Bearer");
            response.put("userId", user.getId());
            response.put("username", user.getUsername());
            response.put("realName", user.getRealName());
            response.put("userType", user.getUserType().name());
            response.put("enterpriseId", user.getEnterpriseId());
            response.put("enterpriseAddress", enterpriseAddress);
            response.put("department", user.getDepartment());
            response.put("position", user.getPosition());
            response.put("loginType", "USER");

            log.info("User logged in successfully: username={}, realName={}, address={}",
                user.getUsername(), user.getRealName(), enterpriseAddress);

            return Result.success("登录成功", response);

        } catch (com.fisco.app.exception.BusinessException e) {
            log.warn("User login failed: username={}, error={}",
                request.getUsername(), e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 企业登录（支持多种登录方式）
     * POST /api/auth/enterprise-login
     *
     * 适用场景：
     * - 企业用户登录系统
     * - 支持用户名、邮箱、手机号、区块链地址4种登录方式
     * - 系统间调用
     */
    @PostMapping("/enterprise-login")
    @ApiOperation(value = "企业登录",
        notes = "支持多种登录方式：用户名（推荐）、邮箱、手机号、区块链地址（向后兼容）")
    @io.swagger.annotations.ApiResponses({
        @io.swagger.annotations.ApiResponse(code = 200, message = "登录成功",
            response = EnterpriseLoginResponse.class),
        @io.swagger.annotations.ApiResponse(code = 400, message = "请求参数错误：未提供有效的登录账号"),
        @io.swagger.annotations.ApiResponse(code = 401, message = "认证失败：密码错误、企业未激活或未设置密码"),
        @io.swagger.annotations.ApiResponse(code = 404, message = "企业不存在，请先注册")
    })
    public Result<EnterpriseLoginResponse> enterpriseLogin(@Valid @RequestBody EnterpriseLoginRequest request) {
        String loginAccount = request.getLoginAccount();
        String loginType = request.getLoginType();

        log.info("Enterprise login attempt: type={}, account={}", loginType, loginAccount);

        try {
            // 根据登录方式获取企业信息
            Enterprise enterprise;
            if (request.getUsername() != null && !request.getUsername().trim().isEmpty()) {
                // 用户名登录
                enterprise = enterpriseService.getEnterpriseByUsername(request.getUsername());
            } else if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
                // 邮箱登录
                enterprise = enterpriseService.getEnterpriseByEmail(request.getEmail());
            } else if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
                // 手机号登录
                enterprise = enterpriseService.getEnterpriseByPhone(request.getPhone());
            } else if (request.getAddress() != null && !request.getAddress().trim().isEmpty()) {
                // 区块链地址登录（向后兼容）
                enterprise = enterpriseService.getEnterprise(request.getAddress());
            } else {
                log.warn("Enterprise login failed: no valid login account provided");
                return Result.error("请提供有效的登录账号（用户名/邮箱/手机号/地址）");
            }

            // 验证企业状态
            if (enterprise.getStatus() != Enterprise.EnterpriseStatus.ACTIVE) {
                log.warn("Enterprise login failed: not active, type={}, account={}", loginType, loginAccount);
                return Result.error("企业账户未激活，请联系管理员");
            }

            // 验证密码
            if (enterprise.getPassword() == null || enterprise.getPassword().isEmpty()) {
                log.warn("Enterprise login failed: no password set, type={}, account={}", loginType, loginAccount);
                return Result.error("账户未设置密码，请联系管理员初始化");
            }

            if (!PasswordUtil.matches(request.getPassword(), enterprise.getPassword())) {
                log.warn("Enterprise login failed: invalid password, type={}, account={}", loginType, loginAccount);
                return Result.error("密码错误");
            }

            // 生成增强型JWT令牌（包含企业ID、角色信息和区块链地址）
            String token = jwtTokenProvider.generateTokenWithAddress(
                enterprise.getUsername() != null ? enterprise.getUsername() : enterprise.getAddress(),
                enterprise.getId(),
                enterprise.getRole().name(),
                "ENTERPRISE",
                enterprise.getAddress()  // 企业区块链地址
            );

            // 构建响应
            EnterpriseLoginResponse response = new EnterpriseLoginResponse();
            response.setToken(token);
            response.setType("Bearer");
            response.setAddress(enterprise.getAddress());
            response.setEnterpriseName(enterprise.getName());
            response.setRole(enterprise.getRole().name());
            response.setLoginType("ENTERPRISE");
            response.setLoginMethod(loginType);  // 显示使用的登录方式
            if (enterprise.getUsername() != null) {
                response.setUsername(enterprise.getUsername());  // 返回用户名
            }

            log.info("Enterprise logged in successfully: type={}, address={}, name={}",
                    loginType, enterprise.getAddress(), enterprise.getName());
            return Result.success("登录成功", response);

        } catch (com.fisco.app.exception.BusinessException.EnterpriseNotFoundException e) {
            log.warn("Enterprise login failed: not found, type={}, account={}", loginType, loginAccount);
            return Result.error("企业账户不存在，请先注册");
        }
    }

    /**
     * API密钥认证（用于程序化访问）
     * POST /api/auth/api-key
     */
    @PostMapping("/api-key")
    @ApiOperation(value = "API密钥认证", notes = "使用API密钥获取JWT令牌（适用于系统间调用）")
    public Result<Map<String, String>> authenticateWithApiKey(@Valid @RequestBody ApiKeyRequest request) {
        log.info("API key authentication attempt: apiKey={}", maskApiKey(request.getApiKey()));

        try {
            Enterprise enterprise = enterpriseService.getEnterpriseByApiKey(request.getApiKey());

            if (enterprise.getStatus() != Enterprise.EnterpriseStatus.ACTIVE) {
                log.warn("API key auth failed: enterprise not active: apiKey={}", maskApiKey(request.getApiKey()));
                return Result.error("企业账户未激活");
            }

            // 生成增强型JWT令牌（包含企业ID、角色信息和区块链地址）
            String token = jwtTokenProvider.generateTokenWithAddress(
                enterprise.getUsername() != null ? enterprise.getUsername() : enterprise.getAddress(),
                enterprise.getId(),
                enterprise.getRole().name(),
                "ENTERPRISE",
                enterprise.getAddress()  // 企业区块链地址
            );

            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            response.put("type", "Bearer");
            response.put("address", enterprise.getAddress());
            response.put("enterpriseName", enterprise.getName());
            response.put("role", enterprise.getRole().name());

            log.info("API key authentication successful: address={}", enterprise.getAddress());
            return Result.success("认证成功", response);

        } catch (com.fisco.app.exception.BusinessException e) {
            log.warn("API key auth failed: invalid API key");
            return Result.error("API密钥无效");
        }
    }

    /**
     * 验证令牌
     * POST /api/auth/validate
     */
    @PostMapping("/validate")
    @ApiOperation(value = "验证令牌", notes = "验证JWT令牌是否有效")
    public Result<Map<String, Object>> validateToken(
            @RequestHeader("Authorization") String authHeader) {

        String token = jwtTokenProvider.extractTokenFromHeader(authHeader);

        if (token == null || !jwtTokenProvider.validateToken(token)) {
            return Result.error("令牌无效或已过期");
        }

        String subject = jwtTokenProvider.getUserAddressFromToken(token);

        Map<String, Object> response = new HashMap<>();
        response.put("valid", true);
        response.put("subject", subject);

        return Result.success("令牌有效", response);
    }

    /**
     * 用户注册（使用邀请码）
     * POST /api/auth/register
     *
     * created_by 获取规则：
     * - 公开注册（无Token）：固定为 "SELF_REGISTER"
     * - 管理员代注册（有Token）：从Token提取管理员用户名
     *
     * 注意：此为公开接口，无需Token即可访问
     */
    @PostMapping("/register")
    @ApiOperation(value = "用户注册（公开接口）",
                  notes = "【公开接口】用户使用企业邀请码进行注册，注册后需要企业审核。\n" +
                          "1. 无需Token：任何人都可以使用有效的邀请码注册，created_by固定为'SELF_REGISTER'\n" +
                          "2. 携带Token：管理员可以代用户注册，created_by为管理员用户名")
    @io.swagger.annotations.ApiResponses({
        @io.swagger.annotations.ApiResponse(code = 200, message = "注册成功，返回用户信息（不包含密码）", response = User.class),
        @io.swagger.annotations.ApiResponse(code = 400, message = "请求参数错误、用户名/邮箱/手机号已存在、邀请码无效"),
        @io.swagger.annotations.ApiResponse(code = 404, message = "邀请码不存在或已过期"),
        @io.swagger.annotations.ApiResponse(code = 500, message = "服务器内部错误")
    })
    public Result<User> registerUser(
            @Valid @RequestBody com.fisco.app.dto.user.UserRegistrationRequest request,
            HttpServletRequest httpRequest,
            org.springframework.security.core.Authentication authentication) {
        log.info("User registration attempt: username={}, invitationCode={}",
                 request.getUsername(), request.getInvitationCode());

        try {
            User user = userService.registerUserWithInvitationCode(request, authentication);
            log.info("User registration successful: username={}, status=PENDING, createdBy={}",
                     user.getUsername(), user.getCreatedBy());
            return Result.success("注册成功，等待企业审核", user);
        } catch (com.fisco.app.exception.BusinessException e) {
            log.warn("User registration failed: username={}, error={}",
                     request.getUsername(), e.getMessage());
            return Result.error(e.getMessage());
        }
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
        return ip;
    }

    /**
     * 掩码API密钥（用于日志记录）
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) {
            return "***";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }

    // ==================== DTO类 ====================

    /**
     * 用户登录请求DTO
     */
    @Data
    @Schema(name = "用户登录请求")
    public static class UserLoginRequest {
        @NotBlank(message = "用户名不能为空")
        @io.swagger.annotations.ApiModelProperty(value = "用户名", required = true, example = "zhangsan")
        private String username;

        @NotBlank(message = "密码不能为空")
        @io.swagger.annotations.ApiModelProperty(value = "登录密码", required = true)
        private String password;
    }

    /**
     * 企业登录请求DTO（支持多种登录方式）
     */
    @Data
    @io.swagger.annotations.ApiModel(value = "企业登录请求",
        description = "支持4种登录方式：用户名（推荐）、邮箱、手机号、区块链地址。" +
                     "四选一，提供一种账号字段即可，必须提供密码。系统会自动识别使用哪种方式登录。")
    @io.swagger.v3.oas.annotations.media.Schema(name = "企业登录请求",
        description = "支持4种登录方式：用户名（推荐）、邮箱、手机号、区块链地址。四选一，提供一种账号即可")
    public static class EnterpriseLoginRequest {
        @io.swagger.annotations.ApiModelProperty(value = "用户名（推荐方式）",
            required = false,
            example = "enterprise_001",
            notes = "使用注册时设置的用户名登录，推荐使用此方式")
        private String username;

        @io.swagger.annotations.ApiModelProperty(value = "邮箱",
            required = false,
            example = "contact@company.com",
            notes = "使用注册时的邮箱登录")
        private String email;

        @io.swagger.annotations.ApiModelProperty(value = "手机号",
            required = false,
            example = "13800138000",
            notes = "使用注册时的手机号登录")
        private String phone;

        @io.swagger.annotations.ApiModelProperty(value = "区块链地址",
            required = false,
            example = "0x1234567890abcdef1234567890abcdef12345678",
            notes = "区块链地址登录（向后兼容，不推荐使用）")
        private String address;

        @NotBlank(message = "密码不能为空")
        @io.swagger.annotations.ApiModelProperty(value = "登录密码",
            required = true,
            example = "Pass123456",
            notes = "企业注册时设置的初始密码或修改后的密码")
        private String password;

        /**
         * 获取实际使用的登录账号
         * @return 登录账号类型和值的组合
         */
        public String getLoginAccount() {
            if (username != null && !username.trim().isEmpty()) {
                return "username:" + username.trim();
            } else if (email != null && !email.trim().isEmpty()) {
                return "email:" + email.trim();
            } else if (phone != null && !phone.trim().isEmpty()) {
                return "phone:" + phone.trim();
            } else if (address != null && !address.trim().isEmpty()) {
                return "address:" + address.trim();
            }
            return null;
        }

        /**
         * 获取登录方式描述
         * @return 登录方式描述
         */
        public String getLoginType() {
            if (username != null && !username.trim().isEmpty()) {
                return "用户名";
            } else if (email != null && !email.trim().isEmpty()) {
                return "邮箱";
            } else if (phone != null && !phone.trim().isEmpty()) {
                return "手机号";
            } else if (address != null && !address.trim().isEmpty()) {
                return "区块链地址";
            }
            return "未知";
        }
    }

    /**
     * 企业登录响应DTO
     */
    @Data
    @io.swagger.annotations.ApiModel(value = "企业登录响应", description = "企业登录成功后返回的完整信息")
    @io.swagger.v3.oas.annotations.media.Schema(name = "企业登录响应")
    public static class EnterpriseLoginResponse {

        @io.swagger.annotations.ApiModelProperty(value = "JWT访问令牌", required = true,
            example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIweDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMiJ9.xxx")
        private String token;

        @io.swagger.annotations.ApiModelProperty(value = "令牌类型", required = true, example = "Bearer")
        private String type;

        @io.swagger.annotations.ApiModelProperty(value = "区块链地址", required = true,
            example = "0x1234567890abcdef1234567890abcdef12345678")
        private String address;

        @io.swagger.annotations.ApiModelProperty(value = "企业名称", required = true, example = "供应商A")
        private String enterpriseName;

        @io.swagger.annotations.ApiModelProperty(value = "企业角色", required = true,
            example = "SUPPLIER",
            notes = "SUPPLIER-供应商, CORE_ENTERPRISE-核心企业, FINANCIAL_INSTITUTION-金融机构, REGULATOR-监管机构")
        private String role;

        @io.swagger.annotations.ApiModelProperty(value = "登录类型", required = true,
            example = "ENTERPRISE",
            notes = "固定值：ENTERPRISE（企业登录）")
        private String loginType;

        @io.swagger.annotations.ApiModelProperty(value = "登录方式", required = true,
            example = "用户名",
            notes = "实际使用的登录方式：用户名/邮箱/手机号/区块链地址",
            allowableValues = "用户名,邮箱,手机号,区块链地址")
        private String loginMethod;

        @io.swagger.annotations.ApiModelProperty(value = "用户名",
            example = "enterprise_001",
            notes = "如果企业设置了用户名则返回，用于前端显示")
        private String username;
    }

    /**
     * API密钥认证请求DTO
     */
    @Data
    @Schema(name = "API密钥请求")
    public static class ApiKeyRequest {
        @NotBlank(message = "API密钥不能为空")
        @io.swagger.annotations.ApiModelProperty(value = "API密钥", required = true)
        private String apiKey;
    }
}
