package com.fisco.app.controller.enterprise;

import com.fisco.app.dto.enterprise.EnterpriseRegistrationRequest;
import com.fisco.app.dto.enterprise.EnterpriseRegistrationResponse;
import com.fisco.app.entity.enterprise.Enterprise;
import com.fisco.app.security.RequireEnterprise;
import com.fisco.app.service.enterprise.EnterpriseService;
import com.fisco.app.vo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 企业自我管理Controller
 * 企业用户管理自己的企业信息
 */
@Slf4j
@RestController
@RequestMapping("/api/enterprise")
@RequiredArgsConstructor
@Validated
@Api(tags = "企业自我管理")
public class EnterpriseController {

    private final EnterpriseService enterpriseService;

    /**
     * 企业注册（公开接口，无需认证）
     * POST /api/enterprise/register
     *
     * 注意：此为公开接口，无需Token即可访问
     */
    @PostMapping("/register")
    @ApiOperation(value = "企业注册（公开接口）",
                  notes = "【公开接口】企业用户注册账号，需要等待管理员审核。注册后自动生成区块链地址和API密钥。")
    @io.swagger.annotations.ApiResponses({
        @io.swagger.annotations.ApiResponse(code = 200, message = "注册成功，返回企业信息（包含区块链地址和API密钥）", response = EnterpriseRegistrationResponse.class),
        @io.swagger.annotations.ApiResponse(code = 400, message = "请求参数错误、企业名称/信用代码/用户名/邮箱/手机号已存在"),
        @io.swagger.annotations.ApiResponse(code = 500, message = "服务器内部错误")
    })
    public Result<EnterpriseRegistrationResponse> register(
            @Valid @RequestBody EnterpriseRegistrationRequest request,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        log.info("接收到企业注册请求: name={}, creditCode={}, ip={}",
                 request.getName(), request.getCreditCode(), ipAddress);

        // 使用DTO的转换方法
        Enterprise enterprise = request.toEnterprise();

        // 公开接口自主注册，created_by固定为SELF_REGISTER（不可修改）
        Enterprise registered = enterpriseService.registerEnterprise(
            enterprise,
            request.getInitialPassword(),
            "SELF_REGISTER",  // createdBy固定值
            ipAddress
        );

        EnterpriseRegistrationResponse response = EnterpriseRegistrationResponse.fromEntity(registered);

        log.info("企业注册成功: id={}, name={}, address={}, creditCode={}, createdBy={}",
                 registered.getId(), registered.getName(), registered.getAddress(),
                 registered.getCreditCode(), registered.getCreatedBy());
        return Result.success("企业注册成功，请等待管理员审核", response);
    }

    /**
     * 获取当前登录企业的信息
     * GET /api/enterprise/me
     */
    @GetMapping("/me")
    @ApiOperation(value = "获取企业信息", notes = "获取当前登录企业的详细信息")
    @RequireEnterprise
    public Result<Enterprise> getCurrentEnterprise(HttpServletRequest request) {
        Enterprise enterprise = (Enterprise) request.getAttribute("currentEnterprise");
        enterprise.setPassword(null); // 清除密码字段
        return Result.success(enterprise);
    }

    /**
     * 获取企业信息（通过ID，只能查看自己的信息）
     * GET /api/enterprise/{id}
     */
    @GetMapping("/{id}")
    @ApiOperation(value = "获取企业信息", notes = "根据ID查询企业详细信息（只能查看自己的企业）")
    @RequireEnterprise
    public Result<Enterprise> getEnterprise(
            @ApiParam(value = "企业ID", required = true, example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
            @PathVariable @NonNull String id,
            HttpServletRequest httpRequest) {
        Enterprise currentEnterprise = (Enterprise) httpRequest.getAttribute("currentEnterprise");

        // 验证是否查询自己的企业信息
        if (!currentEnterprise.getId().equals(id)) {
            return Result.error("无权查看其他企业的信息");
        }

        Enterprise enterprise = enterpriseService.getEnterpriseById(id);
        enterprise.setPassword(null);
        return Result.success(enterprise);
    }

    /**
     * 更新企业信息
     * PUT /api/enterprise/me
     */
    @PutMapping("/me")
    @ApiOperation(value = "更新企业信息",
                  notes = "企业更新自己的部分信息。允许更新：企业地址、邮箱、手机号、备注")
    @RequireEnterprise
    public Result<Enterprise> updateEnterprise(
            @Valid @RequestBody UpdateEnterpriseRequest request,
            HttpServletRequest httpRequest) {
        Enterprise currentEnterprise = (Enterprise) httpRequest.getAttribute("currentEnterprise");

        log.info("企业请求更新信息: enterpriseId={}, address={}, updatedFields={}",
                 currentEnterprise.getId(), currentEnterprise.getAddress(),
                 request.getUpdatedFields());

        // 更新允许修改的字段（使用企业ID而不是地址）
        Enterprise updated = enterpriseService.updateEnterpriseInfo(
            currentEnterprise.getId(),  // ✅ 使用ID更准确
            request.getEnterpriseAddress(),
            request.getEmail(),
            request.getPhone(),
            request.getRemarks(),
            currentEnterprise.getUsername()  // updatedBy: 企业自己的用户名
        );

        updated.setPassword(null);
        return Result.success("企业信息更新成功", updated);
    }

    /**
     * 修改企业密码
     * PUT /api/enterprise/me/password
     */
    @PutMapping("/me/password")
    @ApiOperation(value = "修改企业密码", notes = "企业修改自己的登录密码")
    @RequireEnterprise
    public Result<String> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest) {
        Enterprise currentEnterprise = (Enterprise) httpRequest.getAttribute("currentEnterprise");

        log.info("企业请求修改密码: enterpriseId={}, address={}",
                 currentEnterprise.getId(), currentEnterprise.getAddress());

        enterpriseService.changeEnterprisePassword(
            currentEnterprise.getAddress(),
            request.getOldPassword(),
            request.getNewPassword(),
            currentEnterprise.getUsername()  // updatedBy: 企业自己的用户名
        );

        return Result.success("密码修改成功");
    }

    /**
     * 请求企业注销
     * DELETE /api/enterprise/me
     */
    @DeleteMapping("/me")
    @ApiOperation(value = "请求企业注销", notes = "企业主动申请注销账户，需要管理员审核")
    @RequireEnterprise
    public Result<String> requestEnterpriseDeletion(
            @Valid @RequestBody DeletionRequest request,
            HttpServletRequest httpRequest) {
        Enterprise currentEnterprise = (Enterprise) httpRequest.getAttribute("currentEnterprise");
        String ipAddress = getClientIp(httpRequest);

        log.info("企业请求注销: enterpriseId={}, address={}, reason={}",
                 currentEnterprise.getId(), currentEnterprise.getAddress(), request.getReason());

        enterpriseService.requestEnterpriseDeletion(
            currentEnterprise.getAddress(),
            request.getReason(),
            currentEnterprise.getAddress(),  // requester
            ipAddress
        );

        return Result.success("注销申请已提交，请等待管理员审核");
    }

    /**
     * 获取企业统计信息
     * GET /api/enterprise/me/stats
     *
     * 注：当前版本返回企业基本信息和信用数据。
     * 后续版本可以扩展添加业务统计数据，如：
     * - 应收账款数量和金额（需要集成 ReceivableService）
     * - 仓单数量和总价值（需要集成 WarehouseReceiptService）
     * - 发票数量和总金额（需要集成 BillService）
     * - 交易统计等
     */
    @GetMapping("/me/stats")
    @ApiOperation(value = "获取企业统计", notes = "获取当前企业的统计信息")
    @RequireEnterprise
    public Result<Map<String, Object>> getEnterpriseStats(HttpServletRequest httpRequest) {
        Enterprise currentEnterprise = (Enterprise) httpRequest.getAttribute("currentEnterprise");

        Map<String, Object> stats = new HashMap<>();
        stats.put("enterpriseId", currentEnterprise.getId());
        stats.put("name", currentEnterprise.getName());
        stats.put("address", currentEnterprise.getAddress());
        stats.put("status", currentEnterprise.getStatus().name());
        stats.put("role", currentEnterprise.getRole().name());
        stats.put("creditRating", currentEnterprise.getCreditRating());
        stats.put("creditLimit", currentEnterprise.getCreditLimit());

        return Result.success(stats);
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取客户端真实IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
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
     * 更新企业信息请求DTO
     */
    @io.swagger.annotations.ApiModel(value = "更新企业信息请求", description = "企业自我管理时更新部分信息")
    public static class UpdateEnterpriseRequest {
        @io.swagger.annotations.ApiModelProperty(value = "企业地址", example = "北京市朝阳区xx路xx号")
        private String enterpriseAddress;

        @io.swagger.annotations.ApiModelProperty(value = "企业邮箱", example = "contact@company.com")
        private String email;

        @io.swagger.annotations.ApiModelProperty(value = "企业联系电话", example = "13800138000")
        private String phone;

        @io.swagger.annotations.ApiModelProperty(value = "备注信息", example = "这是一家从事xxx的企业")
        private String remarks;

        // Getters and Setters
        public String getEnterpriseAddress() {
            return enterpriseAddress;
        }

        public void setEnterpriseAddress(String enterpriseAddress) {
            this.enterpriseAddress = enterpriseAddress;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getRemarks() {
            return remarks;
        }

        public void setRemarks(String remarks) {
            this.remarks = remarks;
        }

        /**
         * 获取本次更新的字段列表（用于日志记录）
         */
        public String getUpdatedFields() {
            java.util.List<String> fields = new java.util.ArrayList<>();
            if (enterpriseAddress != null && !enterpriseAddress.trim().isEmpty()) {
                fields.add("enterpriseAddress");
            }
            if (email != null && !email.trim().isEmpty()) {
                fields.add("email");
            }
            if (phone != null && !phone.trim().isEmpty()) {
                fields.add("phone");
            }
            if (remarks != null && !remarks.trim().isEmpty()) {
                fields.add("remarks");
            }
            return fields.isEmpty() ? "none" : String.join(", ", fields);
        }
    }

    /**
     * 修改密码请求DTO
     */
    @io.swagger.annotations.ApiModel(value = "修改密码请求")
    public static class ChangePasswordRequest {
        @NotBlank(message = "原密码不能为空")
        @io.swagger.annotations.ApiModelProperty(value = "原密码", required = true)
        private String oldPassword;

        @NotBlank(message = "新密码不能为空")
        @io.swagger.annotations.ApiModelProperty(value = "新密码", required = true)
        private String newPassword;

        public String getOldPassword() {
            return oldPassword;
        }

        public void setOldPassword(String oldPassword) {
            this.oldPassword = oldPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }

    /**
     * 注销请求DTO
     */
    @io.swagger.annotations.ApiModel(value = "企业注销请求")
    public static class DeletionRequest {
        @NotBlank(message = "注销理由不能为空")
        @io.swagger.annotations.ApiModelProperty(value = "注销理由", required = true, example = "业务调整，不再使用此平台")
        private String reason;

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}
