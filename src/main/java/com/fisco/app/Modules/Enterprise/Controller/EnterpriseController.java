package com.fisco.app.Modules.Enterprise.Controller;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fisco.app.Common.Config.AdminConfig;
import com.fisco.app.Common.Annotation.RequireRole;
import com.fisco.app.Common.Utils.JwtUtil;
import com.fisco.app.Common.Utils.Result;
import com.fisco.app.Modules.Enterprise.Entity.Enterprise;
import com.fisco.app.Modules.Enterprise.Entity.InvitationCode;
import com.fisco.app.Modules.Enterprise.Service.EnterpriseContractService.EnterpriseInfo;
import com.fisco.app.Modules.Enterprise.Service.EnterpriseService;
import com.fisco.app.Modules.User.Entity.User;
import com.fisco.app.Modules.User.Service.UserService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * 企业管理 Controller
 *
 * 提供企业注册、信息查询、状态管理等 API
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Api(tags = "企业管理")
@RestController
@RequestMapping("/api/v1/enterprise")
public class EnterpriseController {

    private static final Logger logger = LoggerFactory.getLogger(EnterpriseController.class);

    @Autowired
    private EnterpriseService enterpriseService;

    @Autowired
    private UserService userService;

    @Autowired
    private AdminConfig adminConfig;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ==================== 企业注册 ====================

    /**
     * 注册企业
     *
     * 调用EnterpriseService完成数据库存储和区块链上链
     */
    @ApiOperation("注册企业")
    @PostMapping("/register")
    public Result<Map<String, Object>> registerEnterprise(
            @ApiParam(value = "企业注册信息", required = true) @RequestBody EnterpriseRegisterRequest request) {
        try {
            // 参数校验
            if (request.getUsername() == null || request.getUsername().isEmpty()) {
                return Result.error(400, "用户名不能为空");
            }
            if (request.getPassword() == null || request.getPassword().isEmpty()) {
                return Result.error(400, "登录密码不能为空");
            }
            if (request.getPayPassword() == null || request.getPayPassword().isEmpty()) {
                return Result.error(400, "交易密码不能为空");
            }
            if (request.getEnterpriseName() == null || request.getEnterpriseName().isEmpty()) {
                return Result.error(400, "企业名称不能为空");
            }
            if (request.getOrgCode() == null || request.getOrgCode().isEmpty()) {
                return Result.error(400, "统一社会信用代码不能为空");
            }
            if (request.getEntRole() == null) {
                return Result.error(400, "企业角色不能为空");
            }

            // 调用Service完成注册（数据库+区块链）
            Long entId = enterpriseService.registerEnterprise(
                    request.getUsername(),
                    request.getPassword(),
                    request.getPayPassword(),
                    request.getEnterpriseName(),
                    request.getOrgCode(),
                    request.getEntRole(),
                    request.getLocalAddress(),
                    request.getContactPhone()
            );

            // 查询注册后的企业信息
            Enterprise enterprise = enterpriseService.getEnterpriseById(entId);

            // 构建响应
            Map<String, Object> result = new HashMap<>();
            result.put("entId", entId);
            result.put("username", enterprise.getUsername());
            result.put("enterpriseName", enterprise.getEnterpriseName());
            result.put("orgCode", enterprise.getOrgCode());
            result.put("entRole", enterprise.getEntRole());
            result.put("status", enterprise.getStatus());
            result.put("blockchainAddress", enterprise.getBlockchainAddress());

            logger.info("企业注册成功: entId={}, username={}, blockchainAddress={}",
                    entId, request.getUsername(), enterprise.getBlockchainAddress());
            return Result.success(result);

        } catch (IllegalArgumentException e) {
            logger.warn("企业注册参数错误: {}", e.getMessage());
            return Result.error(400, "请求参数无效");
        } catch (Exception e) {
            logger.error("注册企业异常", e);
            return Result.error(500, "注册失败，请稍后重试");
        }
    }

    // ==================== 企业查询 ====================

    /**
     * 根据地址获取企业信息（链上）
     */
    @ApiOperation("获取企业信息（链上）")
    @GetMapping("/chain/{address}")
    public Result<EnterpriseInfoResponse> getEnterpriseFromChain(
            @ApiParam(value = "企业区块链地址", required = true)
            @PathVariable String address) {
        try {
            if (address == null || address.isEmpty()) {
                return Result.error(400, "企业地址不能为空");
            }

            EnterpriseInfo info = enterpriseService.getEnterpriseFromChain(address);
            if (info == null) {
                return Result.error(404, "企业不存在: " + address);
            }

            return Result.success(convertToResponse(info));

        } catch (IllegalArgumentException e) {
            return Result.error(400, "请求参数无效");
        } catch (Exception e) {
            logger.error("获取企业信息异常: address={}", address, e);
            return Result.error(500, "查询失败，请稍后重试");
        }
    }

    /**
     * 根据信用代码查询企业（链上）
     */
    @ApiOperation("根据信用代码查询企业（链上）")
    @GetMapping("/chain/code/{creditCode}")
    public Result<String> getEnterpriseByCreditCodeFromChain(
            @ApiParam(value = "统一社会信用代码", required = true)
            @PathVariable String creditCode) {
        try {
            if (creditCode == null || creditCode.isEmpty()) {
                return Result.error(400, "信用代码不能为空");
            }

            String address = enterpriseService.getEnterpriseAddressByOrgCode(creditCode);
            if (address == null || address.isEmpty()) {
                return Result.error(404, "企业不存在: " + creditCode);
            }

            return Result.success(address);

        } catch (IllegalArgumentException e) {
            return Result.error(400, "请求参数无效");
        } catch (Exception e) {
            logger.error("根据信用代码查询企业异常: creditCode={}", creditCode, e);
            return Result.error(500, "查询失败，请稍后重试");
        }
    }

    /**
     * 获取企业列表（链上）
     */
    @ApiOperation("获取企业列表（链上）")
    @GetMapping("/chain/list")
    public Result<List<String>> getEnterpriseListFromChain() {
        try {
            List<String> list = enterpriseService.getEnterpriseListFromChain();
            return Result.success(list);
        } catch (Exception e) {
            logger.error("获取企业列表异常", e);
            return Result.error(500, "查询失败，请稍后重试");
        }
    }

    /**
     * 根据ID获取企业信息（数据库）
     */
    @ApiOperation("根据ID获取企业信息")
    @RequireRole(value = {"ADMIN", "ENTERPRISE"}, adminBypass = true)
    @GetMapping("/{entId}")
    public Result<Enterprise> getEnterpriseById(
            @ApiParam(value = "企业ID", required = true)
            @PathVariable Long entId) {
        try {
            if (entId == null) {
                return Result.error(400, "企业ID不能为空");
            }

            Enterprise enterprise = enterpriseService.getEnterpriseById(entId);
            if (enterprise == null) {
                return Result.error(404, "企业不存在");
            }

            return Result.success(enterprise);

        } catch (Exception e) {
            logger.error("获取企业信息异常: entId={}", entId, e);
            return Result.error(500, "查询失败，请稍后重试");
        }
    }

    /**
     * 获取企业列表（数据库）
     */
    @ApiOperation("获取企业列表")
    @RequireRole(value = {"ADMIN"}, adminBypass = true)
    @GetMapping("/list")
    public Result<List<Enterprise>> getEnterpriseList(
            @ApiParam(value = "状态过滤")
            @RequestParam(required = false) Integer status,
            @ApiParam(value = "角色过滤")
            @RequestParam(required = false) Integer entRole) {
        try {
            List<Enterprise> list = enterpriseService.listEnterprises(status, entRole);
            return Result.success(list);
        } catch (Exception e) {
            logger.error("获取企业列表异常", e);
            return Result.error(500, "查询失败，请稍后重试");
        }
    }

    /**
     * 获取待审核企业列表
     * 仅系统管理员可访问
     */
    @ApiOperation("获取待审核企业列表")
    @RequireRole(value = {"ADMIN"}, adminBypass = true)
    @GetMapping("/pending")
    public Result<List<Enterprise>> getPendingEnterprises() {
        try {
            List<Enterprise> list = enterpriseService.listEnterprises(0, null); // status=0 表示待审核
            return Result.success(list);
        } catch (Exception e) {
            logger.error("获取待审核企业列表异常", e);
            return Result.error(500, "查询失败，请稍后重试");
        }
    }

    // ==================== 企业状态管理 ====================

    /**
     * 更新企业状态（数据库+链上）
     * 需要系统管理员权限
     */
    @ApiOperation("更新企业状态")
    @RequireRole(value = {"ADMIN"}, adminBypass = true)
    @PutMapping("/{entId}/status")
    public Result<Map<String, Object>> updateEnterpriseStatus(
            @ApiParam(value = "企业ID", required = true)
            @PathVariable Long entId,
            @ApiParam(value = "状态更新信息", required = true)
            @RequestBody StatusRequest request) {
        try {
            if (entId == null) {
                return Result.error(400, "企业ID不能为空");
            }
            if (request.getStatus() == null) {
                return Result.error(400, "状态不能为空");
            }

            // 更新数据库状态
            boolean dbSuccess = enterpriseService.updateEnterpriseStatus(entId, request.getStatus().intValue());

            // 更新链上状态
            String txHash = null;
            try {
                txHash = enterpriseService.updateEnterpriseStatusOnChain(entId, request.getStatus().intValue());
            } catch (Exception e) {
                logger.warn("链上状态更新失败，将仅更新数据库: entId={}", entId, e);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("dbStatus", dbSuccess ? "success" : "failed");
            result.put("chainTxHash", txHash);
            result.put("status", request.getStatus());

            if (dbSuccess) {
                return Result.success(result);
            } else {
                return Result.error(500, "更新企业状态失败");
            }

        } catch (IllegalArgumentException e) {
            return Result.error(400, "请求参数无效");
        } catch (Exception e) {
            logger.error("更新企业状态异常: entId={}", entId, e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    /**
     * 审核企业申请
     * 将企业从待审核状态(0)变更为正常状态(1)或拒绝(2)
     * 仅系统管理员可访问
     */
    @ApiOperation("审核企业申请")
    @RequireRole(value = {"ADMIN"}, adminBypass = true)
    @PostMapping("/{entId}/audit")
    public Result<Map<String, Object>> auditEnterprise(
            @ApiParam(value = "企业ID", required = true)
            @PathVariable Long entId,
            @ApiParam(value = "审核信息", required = true)
            @RequestBody AuditRequest request) {
        try {
            if (entId == null) {
                return Result.error(400, "企业ID不能为空");
            }
            if (request.getApproved() == null) {
                return Result.error(400, "审核结果不能为空");
            }

            // 获取企业当前状态
            Enterprise enterprise = enterpriseService.getEnterpriseById(entId);
            if (enterprise == null) {
                return Result.error(404, "企业不存在");
            }
            if (enterprise.getStatus() != 0) {
                return Result.error(400, "该企业不是待审核状态，无法重复审核");
            }

            // 审核结果：通过设为正常(1)，拒绝设为冻结(2)
            int newStatus = request.getApproved() ? 1 : 2;
            String action = request.getApproved() ? "通过" : "拒绝";

            // 更新数据库状态
            boolean dbSuccess = enterpriseService.updateEnterpriseStatus(entId, newStatus);

            // 链上操作
            String txHash = null;
            try {
                if (request.getApproved()) {
                    // 审核通过：先注册上链，再更新状态
                    txHash = enterpriseService.registerEnterpriseOnChain(entId);
                    if (txHash != null) {
                        txHash = enterpriseService.updateEnterpriseStatusOnChain(entId, newStatus);
                    }
                } else {
                    // 审核拒绝：直接更新状态
                    txHash = enterpriseService.updateEnterpriseStatusOnChain(entId, newStatus);
                }
            } catch (Exception e) {
                logger.warn("链上操作失败，将仅更新数据库: entId={}", entId, e);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("enterpriseId", entId);
            result.put("enterpriseName", enterprise.getEnterpriseName());
            result.put("action", action);
            result.put("newStatus", newStatus);
            result.put("dbStatus", dbSuccess ? "success" : "failed");
            result.put("chainTxHash", txHash);

            if (dbSuccess) {
                return Result.success(result);
            } else {
                return Result.error(500, "审核企业失败");
            }

        } catch (IllegalArgumentException e) {
            return Result.error(400, "请求参数无效");
        } catch (Exception e) {
            logger.error("审核企业异常: entId={}", entId, e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    /**
     * 更新企业信用评级（链上）
     */
    @ApiOperation("更新企业信用评级")
    @RequireRole(value = {"ADMIN"}, adminBypass = true)
    @PutMapping("/{entId}/rating")
    public Result<Map<String, Object>> updateCreditRating(
            @ApiParam(value = "企业ID", required = true)
            @PathVariable Long entId,
            @ApiParam(value = "信用评级信息", required = true)
            @RequestBody RatingRequest request) {
        try {
            if (entId == null) {
                return Result.error(400, "企业ID不能为空");
            }
            if (request.getRating() == null) {
                return Result.error(400, "评级不能为空");
            }

            String txHash = enterpriseService.updateCreditRatingOnChain(entId, request.getRating());

            Map<String, Object> result = new HashMap<>();
            result.put("txHash", txHash);
            result.put("rating", request.getRating());

            return Result.success(result);

        } catch (IllegalArgumentException e) {
            return Result.error(400, "请求参数无效");
        } catch (Exception e) {
            logger.error("更新企业信用评级异常: entId={}", entId, e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    /**
     * 设置企业授信额度（链上）
     */
    @ApiOperation("设置企业授信额度")
    @RequireRole(value = {"ADMIN"}, adminBypass = true)
    @PutMapping("/{entId}/credit-limit")
    public Result<Map<String, Object>> setCreditLimit(
            @ApiParam(value = "企业ID", required = true)
            @PathVariable Long entId,
            @ApiParam(value = "授信额度信息", required = true)
            @RequestBody CreditLimitRequest request) {
        try {
            if (entId == null) {
                return Result.error(400, "企业ID不能为空");
            }
            if (request.getCreditLimit() == null) {
                return Result.error(400, "授信额度不能为空");
            }

            String txHash = enterpriseService.setCreditLimitOnChain(entId, request.getCreditLimit().longValue());

            Map<String, Object> result = new HashMap<>();
            result.put("txHash", txHash);
            result.put("creditLimit", request.getCreditLimit());

            return Result.success(result);

        } catch (IllegalArgumentException e) {
            return Result.error(400, "请求参数无效");
        } catch (Exception e) {
            logger.error("设置企业授信额度异常: entId={}", entId, e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    // ==================== 企业登录 ====================

    /**
     * 企业登录
     */
    @ApiOperation("企业登录")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(
            @ApiParam(value = "登录信息", required = true) @RequestBody LoginRequest request) {
        try {
            if (request.getUsername() == null || request.getUsername().isEmpty()) {
                return Result.error(400, "用户名不能为空");
            }
            if (request.getPassword() == null || request.getPassword().isEmpty()) {
                return Result.error(400, "密码不能为空");
            }

            Enterprise enterprise = enterpriseService.login(request.getUsername(), request.getPassword());
            if (enterprise == null) {
                return Result.error(401, "用户名或密码错误");
            }

            // 生成JWT令牌
            Map<String, String> tokens = JwtUtil.createTokenPair(
                    enterprise.getEntId(),
                    enterprise.getEntId(),
                    "ENTERPRISE",
                    null,
                    enterprise.getEntRole()
            );

            Map<String, Object> result = new HashMap<>();
            result.put("entId", enterprise.getEntId());
            result.put("username", enterprise.getUsername());
            result.put("enterpriseName", enterprise.getEnterpriseName());
            result.put("entRole", enterprise.getEntRole());
            result.put("status", enterprise.getStatus());
            result.put("blockchainAddress", enterprise.getBlockchainAddress());
            result.put("accessToken", tokens.get("accessToken"));
            result.put("refreshToken", tokens.get("refreshToken"));

            logger.info("企业登录成功: entId={}, username={}", enterprise.getEntId(), enterprise.getUsername());
            return Result.success(result);

        } catch (IllegalStateException e) {
            logger.warn("企业登录失败: {}", e.getMessage());
            return Result.error(403, "无权访问");
        } catch (Exception e) {
            logger.error("企业登录异常", e);
            return Result.error(500, "登录失败，请稍后重试");
        }
    }

    /**
     * 系统管理员登录
     */
    @ApiOperation("系统管理员登录")
    @PostMapping("/admin/login")
    public Result<Map<String, Object>> adminLogin(
            @ApiParam(value = "登录信息", required = true) @RequestBody LoginRequest request) {
        try {
            if (request.getUsername() == null || request.getUsername().isEmpty()) {
                return Result.error(400, "用户名不能为空");
            }
            if (request.getPassword() == null || request.getPassword().isEmpty()) {
                return Result.error(400, "密码不能为空");
            }

            // 校验管理员账户
            if (!adminConfig.getUsername().equals(request.getUsername())) {
                logger.warn("管理员登录失败: 用户名错误, username={}", request.getUsername());
                return Result.error(401, "用户名或密码错误");
            }
            if (!passwordEncoder.matches(request.getPassword(), adminConfig.getPassword())) {
                logger.warn("管理员登录失败: 密码错误, username={}", request.getUsername());
                return Result.error(401, "用户名或密码错误");
            }

            // 生成JWT令牌 (scope=1 表示系统管理员)
            Map<String, String> tokens = JwtUtil.createTokenPair(
                    0L,  // 系统管理员无企业ID
                    0L,
                    "ADMIN",
                    adminConfig.getScope()  // scope=1
            );

            Map<String, Object> result = new HashMap<>();
            result.put("username", adminConfig.getUsername());
            result.put("scope", adminConfig.getScope());
            result.put("accessToken", tokens.get("accessToken"));
            result.put("refreshToken", tokens.get("refreshToken"));

            logger.info("系统管理员登录成功: username={}", adminConfig.getUsername());
            return Result.success(result);

        } catch (Exception e) {
            logger.error("管理员登录异常", e);
            return Result.error(500, "登录失败，请稍后重试");
        }
    }

    // ==================== 密码管理 ====================

    /**
     * 修改登录密码
     */
    @ApiOperation("修改登录密码")
    @PutMapping("/password/login")
    public Result<Void> updateLoginPassword(
            @ApiParam(value = "密码更新信息", required = true) @RequestBody PasswordUpdateRequest request) {
        try {
            if (request.getEntId() == null) {
                return Result.error(400, "企业ID不能为空");
            }
            if (request.getOldPassword() == null || request.getOldPassword().isEmpty()) {
                return Result.error(400, "原密码不能为空");
            }
            if (request.getNewPassword() == null || request.getNewPassword().isEmpty()) {
                return Result.error(400, "新密码不能为空");
            }

            boolean success = enterpriseService.updateLoginPassword(
                    request.getEntId(),
                    request.getOldPassword(),
                    request.getNewPassword()
            );

            if (success) {
                logger.info("企业登录密码已更新: entId={}", request.getEntId());
                return Result.success(null);
            } else {
                return Result.error(500, "修改密码失败");
            }

        } catch (IllegalArgumentException e) {
            return Result.error(400, "请求参数无效");
        } catch (Exception e) {
            logger.error("修改登录密码异常: entId={}", request.getEntId(), e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    /**
     * 重置交易密码
     */
    @ApiOperation("重置交易密码")
    @PutMapping("/password/pay")
    public Result<Void> updatePayPassword(
            @ApiParam(value = "交易密码重置信息", required = true) @RequestBody PasswordUpdateRequest request) {
        try {
            if (request.getEntId() == null) {
                return Result.error(400, "企业ID不能为空");
            }
            if (request.getOldPassword() == null || request.getOldPassword().isEmpty()) {
                return Result.error(400, "原交易密码不能为空");
            }
            if (request.getNewPassword() == null || request.getNewPassword().isEmpty()) {
                return Result.error(400, "新交易密码不能为空");
            }

            boolean success = enterpriseService.updatePayPassword(
                    request.getEntId(),
                    request.getOldPassword(),
                    request.getNewPassword()
            );

            if (success) {
                logger.info("企业交易密码已重置: entId={}", request.getEntId());
                return Result.success(null);
            } else {
                return Result.error(500, "重置交易密码失败");
            }

        } catch (IllegalArgumentException e) {
            return Result.error(400, "请求参数无效");
        } catch (Exception e) {
            logger.error("重置交易密码异常: entId={}", request.getEntId(), e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    // ==================== 企业详情 ====================

    /**
     * 获取企业详情
     */
    @ApiOperation("获取企业详情")
    @GetMapping("/detail")
    public Result<EnterpriseDetailResponse> getEnterpriseDetail(
            @ApiParam(value = "企业ID", required = false)
            @RequestParam(required = false) Long entId,
            javax.servlet.http.HttpServletRequest request) {
        try {
            // 如果未指定entId，从当前登录用户获取企业ID
            if (entId == null) {
                Object entIdAttr = request.getAttribute("ent_id");
                if (entIdAttr != null) {
                    entId = Long.parseLong(entIdAttr.toString());
                }
            }
            if (entId == null) {
                return Result.error(400, "企业ID不能为空，请先登录企业账号或指定entId");
            }

            Enterprise enterprise = enterpriseService.getEnterpriseById(entId);
            if (enterprise == null) {
                return Result.error(404, "企业不存在");
            }

            return Result.success(convertToDetailResponse(enterprise));

        } catch (Exception e) {
            logger.error("获取企业详情异常: entId={}", entId, e);
            return Result.error(500, "查询失败，请稍后重试");
        }
    }

    // ==================== 邀请码管理 ====================

    /**
     * 生成邀请码
     */
    @ApiOperation("生成邀请码")
    @GetMapping("/invite-codes")
    public Result<InvitationCodeResponse> generateInvitationCode(
            @ApiParam(value = "企业ID，不传则从JWT自动获取")
            @RequestParam(required = false) Long entId,
            @ApiParam(value = "最大使用次数")
            @RequestParam(required = false) Integer maxUses,
            @ApiParam(value = "过期天数")
            @RequestParam(required = false) Integer expireDays,
            @ApiParam(value = "备注")
            @RequestParam(required = false) String remark,
            javax.servlet.http.HttpServletRequest request) {
        try {
            // JWT 自动获取 entId
            if (entId == null) {
                Object entIdAttr = request.getAttribute("ent_id");
                if (entIdAttr != null) {
                    entId = Long.parseLong(entIdAttr.toString());
                }
            }
            if (entId == null) {
                return Result.error(400, "企业ID不能为空，请先登录企业账号或指定entId");
            }

            String code = enterpriseService.generateInvitationCode(entId, maxUses, expireDays, remark);

            InvitationCodeResponse response = new InvitationCodeResponse();
            response.setCode(code);
            response.setMaxUses(maxUses != null ? maxUses : 1);
            response.setExpireDays(expireDays);
            response.setRemark(remark);

            logger.info("生成邀请码成功: entId={}, code={}", entId, code);
            return Result.success(response);

        } catch (IllegalArgumentException e) {
            return Result.error(400, "请求参数无效");
        } catch (Exception e) {
            logger.error("生成邀请码异常: entId={}", entId, e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    /**
     * 查询邀请码列表
     */
    @ApiOperation("查询邀请码列表")
    @GetMapping("/invite-codes/list")
    public Result<List<InvitationCode>> listInvitationCodes(
            @ApiParam(value = "企业ID，不传则从JWT自动获取")
            @RequestParam(required = false) Long entId,
            javax.servlet.http.HttpServletRequest request) {
        try {
            // JWT 自动获取 entId
            if (entId == null) {
                Object entIdAttr = request.getAttribute("ent_id");
                if (entIdAttr != null) {
                    entId = Long.parseLong(entIdAttr.toString());
                }
            }
            if (entId == null) {
                return Result.error(400, "企业ID不能为空，请先登录企业账号或指定entId");
            }

            List<InvitationCode> list = enterpriseService.listInvitationCodes(entId);
            return Result.success(list);

        } catch (Exception e) {
            logger.error("查询邀请码列表异常: entId={}", entId, e);
            return Result.error(500, "查询失败，请稍后重试");
        }
    }

    /**
     * 删除邀请码
     */
    @ApiOperation("删除邀请码")
    @DeleteMapping("/invite-codes/{codeId}")
    public Result<Void> deleteInvitationCode(
            @ApiParam(value = "邀请码ID", required = true)
            @PathVariable Long codeId) {
        try {
            if (codeId == null) {
                return Result.error(400, "邀请码ID不能为空");
            }

            boolean success = enterpriseService.deleteInvitationCode(codeId);
            if (success) {
                logger.info("删除邀请码成功: codeId={}", codeId);
                return Result.success(null);
            } else {
                return Result.error(500, "删除邀请码失败");
            }

        } catch (IllegalArgumentException e) {
            return Result.error(400, "请求参数无效");
        } catch (Exception e) {
            logger.error("删除邀请码异常: codeId={}", codeId, e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    // ==================== 企业注销管理 ====================

    /**
     * 发起注销申请
     */
    @ApiOperation("发起注销申请")
    @PostMapping("/cancellation/apply")
    public Result<EnterpriseService.CancellationResult> applyCancellation(
            @ApiParam(value = "企业ID", required = true)
            @RequestParam Long entId,
            @ApiParam(value = "注销原因")
            @RequestParam(required = false) String reason) {
        try {
            if (entId == null) {
                return Result.error(400, "企业ID不能为空");
            }

            EnterpriseService.CancellationResult result = enterpriseService.applyCancellation(entId, reason);

            if (result.isSuccess()) {
                logger.info("企业注销申请成功: entId={}", entId);
                return Result.success(result);
            } else {
                return Result.error(400, result.getMessage());
            }

        } catch (IllegalArgumentException e) {
            return Result.error(400, "请求参数无效");
        } catch (Exception e) {
            logger.error("发起注销申请异常: entId={}", entId, e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    /**
     * 撤回注销申请
     */
    @ApiOperation("撤回注销申请")
    @PostMapping("/cancellation/revoke")
    public Result<Void> revokeCancellation(
            @ApiParam(value = "企业ID", required = true)
            @RequestParam Long entId) {
        try {
            if (entId == null) {
                return Result.error(400, "企业ID不能为空");
            }

            boolean success = enterpriseService.revokeCancellation(entId);
            if (success) {
                logger.info("企业注销申请已撤回: entId={}", entId);
                return Result.success(null);
            } else {
                return Result.error(500, "撤回注销申请失败");
            }

        } catch (IllegalArgumentException e) {
            return Result.error(400, "请求参数无效");
        } catch (Exception e) {
            logger.error("撤回注销申请异常: entId={}", entId, e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    /**
     * 获取待审核注销企业列表（仅管理员可访问）
     */
    @ApiOperation("获取待审核注销企业列表")
    @RequireRole(value = {"ADMIN"}, adminBypass = true)
    @GetMapping("/cancellation/pending")
    public Result<List<Enterprise>> getPendingCancellationEnterprises() {
        try {
            List<Enterprise> list = enterpriseService.getPendingCancellationEnterprises();
            return Result.success(list);
        } catch (Exception e) {
            logger.error("获取待审核注销企业列表异常", e);
            return Result.error(500, "查询失败，请稍后重试");
        }
    }

    /**
     * 审核企业注销申请（仅管理员可访问）
     */
    @ApiOperation("审核企业注销申请")
    @RequireRole(value = {"ADMIN"}, adminBypass = true)
    @PostMapping("/{entId}/cancellation/audit")
    public Result<Void> auditCancellation(
            @ApiParam(value = "企业ID", required = true) @PathVariable Long entId,
            @ApiParam(value = "审核结果", required = true) @RequestBody AuditRequest request) {
        try {
            if (entId == null) {
                return Result.error(400, "企业ID不能为空");
            }
            if (request.getApproved() == null) {
                return Result.error(400, "审核结果不能为空");
            }

            boolean success = enterpriseService.auditCancellation(entId, request.getApproved());

            if (success) {
                logger.info("企业注销审核完成: entId={}, approved={}", entId, request.getApproved());
                return Result.success(null);
            } else {
                return Result.error(500, "审核企业注销失败");
            }
        } catch (IllegalArgumentException e) {
            return Result.error(400, "请求参数无效");
        } catch (Exception e) {
            logger.error("审核企业注销异常: entId={}", entId, e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    /**
     * 查询企业资产余额（用于注销前校验）
     */
    @ApiOperation("查询企业资产余额（从JWT自动获取企业ID）")
    @GetMapping("/asset-balance")
    public Result<EnterpriseService.AssetBalance> checkAssetBalance(javax.servlet.http.HttpServletRequest request) {
        try {
            // 仅从JWT获取enterpriseId，防止越权
            Object entIdAttr = request.getAttribute("ent_id");
            if (entIdAttr == null) {
                return Result.error(401, "未登录或Token无效");
            }
            Long enterpriseId = Long.parseLong(entIdAttr.toString());

            EnterpriseService.AssetBalance balance = enterpriseService.checkAssetBalance(enterpriseId);
            return Result.success(balance);

        } catch (Exception e) {
            logger.error("查询资产余额异常", e);
            return Result.error(500, "查询资产余额失败");
        }
    }

    // ==================== 员工管理 ====================

    /**
     * 员工列表查询
     * 从JWT自动获取企业ID，也可手动指定
     */
    @ApiOperation("员工列表查询（从JWT自动获取企业ID）")
    @GetMapping("/info_user")
    public Result<List<User>> getUserList(
            @ApiParam(value = "企业ID，不传则从JWT自动获取")
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

            // 查询该企业的员工列表
            List<User> list = userService.getUsersByEnterpriseId(enterpriseId);
            return Result.success(list);

        } catch (Exception e) {
            logger.error("查询员工列表异常: enterpriseId={}", enterpriseId, e);
            return Result.error(500, "查询失败，请稍后重试");
        }
    }

    /**
     * 获取指定用户信息
     * 查询该企业内指定员工的信息，需校验企业归属
     */
    @ApiOperation("获取指定用户信息")
    @GetMapping("/get_user")
    public Result<User> getUser(
            @ApiParam(value = "企业ID", required = true)
            @RequestParam Long entId,
            @ApiParam(value = "用户ID", required = true)
            @RequestParam Long userId) {
        try {
            if (entId == null) {
                return Result.error(400, "企业ID不能为空");
            }
            if (userId == null) {
                return Result.error(400, "用户ID不能为空");
            }

            // 查询用户信息
            User user = userService.getUserById(userId);
            if (user == null) {
                return Result.error(404, "用户不存在");
            }

            // 校验用户是否属于该企业
            if (!entId.equals(user.getEnterpriseId())) {
                logger.warn("权限校验失败: userId={} 不属于 entId={}", userId, entId);
                return Result.error(403, "无权限访问该用户信息");
            }

            return Result.success(user);

        } catch (Exception e) {
            logger.error("获取用户信息异常: userId={}", userId, e);
            return Result.error(500, "查询失败，请稍后重试");
        }
    }

    /**
     * 管理企业用户
     * 修改员工信息（角色、状态）或移除成员，需校验企业归属和权限
     */
    @ApiOperation("管理企业用户")
    @PutMapping("/users/{userId}")
    public Result<Void> manageUser(
            @ApiParam(value = "企业ID", required = true) @RequestParam Long entId,
            @ApiParam(value = "用户ID", required = true) @PathVariable Long userId,
            @ApiParam(value = "用户管理信息", required = true) @RequestBody ManageUserRequest request) {
        try {
            if (entId == null) {
                return Result.error(400, "企业ID不能为空");
            }
            if (userId == null) {
                return Result.error(400, "用户ID不能为空");
            }

            // 获取用户信息
            User user = userService.getUserById(userId);
            if (user == null) {
                return Result.error(404, "用户不存在");
            }

            // 校验用户是否属于该企业
            if (!entId.equals(user.getEnterpriseId())) {
                logger.warn("权限校验失败: userId={} 不属于 entId={}", userId, entId);
                return Result.error(403, "无权限管理该用户");
            }

            // 修改角色
            if (request.getUserRole() != null && !request.getUserRole().isEmpty()) {
                userService.updateUserRole(userId, request.getUserRole());
            }

            // 修改状态
            if (request.getStatus() != null) {
                userService.updateUserStatus(userId, request.getStatus());
            }

            logger.info("管理企业用户成功: entId={}, userId={}, role={}, status={}",
                    entId, userId, request.getUserRole(), request.getStatus());
            return Result.success(null);

        } catch (IllegalArgumentException e) {
            return Result.error(400, "请求参数无效");
        } catch (Exception e) {
            logger.error("管理企业用户异常: userId={}", userId, e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    // ==================== 请求/响应对象 ====================

    /**
     * 企业注册请求
     */
    static class EnterpriseRegisterRequest {
        private String username;
        private String password;
        private String payPassword;
        private String enterpriseName;
        private String orgCode;
        private Integer entRole;
        private String localAddress;
        private String contactPhone;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getPayPassword() { return payPassword; }
        public void setPayPassword(String payPassword) { this.payPassword = payPassword; }
        public String getEnterpriseName() { return enterpriseName; }
        public void setEnterpriseName(String enterpriseName) { this.enterpriseName = enterpriseName; }
        public String getOrgCode() { return orgCode; }
        public void setOrgCode(String orgCode) { this.orgCode = orgCode; }
        public Integer getEntRole() { return entRole; }
        public void setEntRole(Integer entRole) { this.entRole = entRole; }
        public String getLocalAddress() { return localAddress; }
        public void setLocalAddress(String localAddress) { this.localAddress = localAddress; }
        public String getContactPhone() { return contactPhone; }
        public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    }

    /**
     * 状态更新请求
     */
    static class StatusRequest {
        private BigInteger status;

        public BigInteger getStatus() {
            return status;
        }

        public void setStatus(BigInteger status) {
            this.status = status;
        }
    }

    /**
     * 企业审核请求
     */
    static class AuditRequest {
        private Boolean approved;

        public Boolean getApproved() {
            return approved;
        }

        public void setApproved(Boolean approved) {
            this.approved = approved;
        }
    }

    /**
     * 评级更新请求
     */
    static class RatingRequest {
        private String rating;

        public String getRating() {
            return rating;
        }

        public void setRating(String rating) {
            this.rating = rating;
        }
    }

    /**
     * 授信额度请求
     */
    static class CreditLimitRequest {
        private BigInteger creditLimit;

        public BigInteger getCreditLimit() {
            return creditLimit;
        }

        public void setCreditLimit(BigInteger creditLimit) {
            this.creditLimit = creditLimit;
        }
    }

    /**
     * 企业信息响应
     */
    static class EnterpriseInfoResponse {
        private String address;
        private String creditCode;
        private String roleName;
        private BigInteger role;
        private BigInteger status;
        private BigInteger creditLimit;
        private BigInteger creditRating;
        private BigInteger createdAt;
        private boolean active;

        // Getters and Setters
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getCreditCode() { return creditCode; }
        public void setCreditCode(String creditCode) { this.creditCode = creditCode; }
        public String getRoleName() { return roleName; }
        public void setRoleName(String roleName) { this.roleName = roleName; }
        public BigInteger getRole() { return role; }
        public void setRole(BigInteger role) { this.role = role; }
        public BigInteger getStatus() { return status; }
        public void setStatus(BigInteger status) { this.status = status; }
        public BigInteger getCreditLimit() { return creditLimit; }
        public void setCreditLimit(BigInteger creditLimit) { this.creditLimit = creditLimit; }
        public BigInteger getCreditRating() { return creditRating; }
        public void setCreditRating(BigInteger creditRating) { this.creditRating = creditRating; }
        public BigInteger getCreatedAt() { return createdAt; }
        public void setCreatedAt(BigInteger createdAt) { this.createdAt = createdAt; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }

    /**
     * 转换 EnterpriseInfo 为响应对象
     */
    private EnterpriseInfoResponse convertToResponse(EnterpriseInfo info) {
        EnterpriseInfoResponse response = new EnterpriseInfoResponse();
        response.setAddress(info.getAddress());
        response.setCreditCode(info.getCreditCode());
        response.setRole(info.getRole());
        response.setRoleName(info.getRoleName());
        response.setStatus(info.getStatus());
        response.setCreditLimit(info.getCreditLimit());
        response.setCreditRating(info.getCreditRating());
        response.setCreatedAt(info.getCreatedAt());
        response.setActive(info.isActive());
        return response;
    }

    // ==================== 新增请求/响应类 ====================

    /**
     * 登录请求
     */
    static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    /**
     * 密码更新请求
     */
    static class PasswordUpdateRequest {
        private Long entId;
        private String oldPassword;
        private String newPassword;

        public Long getEntId() { return entId; }
        public void setEntId(Long entId) { this.entId = entId; }
        public String getOldPassword() { return oldPassword; }
        public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }

    /**
     * 企业详情响应
     */
    static class EnterpriseDetailResponse {
        private Long entId;
        private String username;
        private String enterpriseName;
        private String orgCode;
        private String localAddress;
        private String contactPhone;
        private Integer entRole;
        private String entRoleName;
        private Integer status;
        private String statusName;
        private String blockchainAddress;
        private java.time.LocalDateTime createTime;
        private java.time.LocalDateTime updateTime;

        // Getters and Setters
        public Long getEntId() { return entId; }
        public void setEntId(Long entId) { this.entId = entId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEnterpriseName() { return enterpriseName; }
        public void setEnterpriseName(String enterpriseName) { this.enterpriseName = enterpriseName; }
        public String getOrgCode() { return orgCode; }
        public void setOrgCode(String orgCode) { this.orgCode = orgCode; }
        public String getLocalAddress() { return localAddress; }
        public void setLocalAddress(String localAddress) { this.localAddress = localAddress; }
        public String getContactPhone() { return contactPhone; }
        public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
        public Integer getEntRole() { return entRole; }
        public void setEntRole(Integer entRole) { this.entRole = entRole; }
        public String getEntRoleName() { return entRoleName; }
        public void setEntRoleName(String entRoleName) { this.entRoleName = entRoleName; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
        public String getStatusName() { return statusName; }
        public void setStatusName(String statusName) { this.statusName = statusName; }
        public String getBlockchainAddress() { return blockchainAddress; }
        public void setBlockchainAddress(String blockchainAddress) { this.blockchainAddress = blockchainAddress; }
        public java.time.LocalDateTime getCreateTime() { return createTime; }
        public void setCreateTime(java.time.LocalDateTime createTime) { this.createTime = createTime; }
        public java.time.LocalDateTime getUpdateTime() { return updateTime; }
        public void setUpdateTime(java.time.LocalDateTime updateTime) { this.updateTime = updateTime; }
    }

    /**
     * 转换 Enterprise 为详情响应对象
     */
    private EnterpriseDetailResponse convertToDetailResponse(Enterprise enterprise) {
        EnterpriseDetailResponse response = new EnterpriseDetailResponse();
        response.setEntId(enterprise.getEntId());
        response.setUsername(enterprise.getUsername());
        response.setEnterpriseName(enterprise.getEnterpriseName());
        response.setOrgCode(enterprise.getOrgCode());
        response.setLocalAddress(enterprise.getLocalAddress());
        response.setContactPhone(enterprise.getContactPhone());
        response.setEntRole(enterprise.getEntRole());
        response.setEntRoleName(getEntRoleName(enterprise.getEntRole()));
        response.setStatus(enterprise.getStatus());
        response.setStatusName(getStatusName(enterprise.getStatus()));
        response.setBlockchainAddress(enterprise.getBlockchainAddress());
        response.setCreateTime(enterprise.getCreateTime());
        response.setUpdateTime(enterprise.getUpdateTime());
        return response;
    }

    /**
     * 获取企业角色名称
     */
    private String getEntRoleName(Integer entRole) {
        if (entRole == null) return null;
        switch (entRole) {
            case 1: return "核心企业";
            case 2: return "金融机构";
            case 3: return "供应商";
            case 4: return "经销商";
            default: return "未知角色";
        }
    }

    /**
     * 获取企业状态名称
     */
    private String getStatusName(Integer status) {
        if (status == null) return null;
        switch (status) {
            case 0: return "待审核";
            case 1: return "正常";
            case 2: return "已冻结";
            case 3: return "注销中";
            case 4: return "已注销";
            default: return "未知状态";
        }
    }

    /**
     * 邀请码响应
     */
    static class InvitationCodeResponse {
        private String code;
        private Integer maxUses;
        private Integer expireDays;
        private String remark;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public Integer getMaxUses() { return maxUses; }
        public void setMaxUses(Integer maxUses) { this.maxUses = maxUses; }
        public Integer getExpireDays() { return expireDays; }
        public void setExpireDays(Integer expireDays) { this.expireDays = expireDays; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }

    /**
     * 管理企业用户请求
     */
    static class ManageUserRequest {
        private String userRole;
        private Integer status;

        public String getUserRole() { return userRole; }
        public void setUserRole(String userRole) { this.userRole = userRole; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
    }
}
