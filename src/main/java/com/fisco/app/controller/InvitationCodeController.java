package com.fisco.app.controller;

import com.fisco.app.annotation.Audited;
import com.fisco.app.entity.InvitationCode;
import com.fisco.app.security.annotations.RequireEnterpriseAdmin;
import com.fisco.app.service.EnterpriseService;
import com.fisco.app.service.InvitationCodeService;
import com.fisco.app.service.PermissionAuditService;
import com.fisco.app.vo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.lang.NonNull;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 邀请码管理Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/invitation-codes")
@RequiredArgsConstructor
@Api(tags = "邀请码管理")
public class InvitationCodeController {

    private final InvitationCodeService invitationCodeService;
    private final EnterpriseService enterpriseService;
    private final PermissionAuditService auditService;

    /**
     * 生成邀请码
     * POST /api/invitation-codes/generate
     *
     * 权限说明：
     * - 企业管理员：只能为本企业生成邀请码
     * - 系统管理员：可以为任何企业生成邀请码
     *
     * created_by 获取规则：
     * - 从 JWT Token 中提取用户标识
     * - 管理员：使用 admin.username（如 "admin"）
     * - 企业用户：使用 enterprise.address（区块链地址，如 "0x123..."）
     */
    @PostMapping("/generate")
    @ApiOperation(value = "生成邀请码", notes = "为企业生成新的邀请码。created_by从Token中自动提取。")
    @RequireEnterpriseAdmin
    @Audited(
        module = "INVITATION_CODE",
        actionType = "CREATE",
        actionDesc = "生成邀请码",
        entityType = "InvitationCode",
        logRequest = true,
        logResponse = true
    )
    public Result<InvitationCode> generateCode(
            @Valid @RequestBody GenerateCodeRequest request,
            Authentication authentication) {
        com.fisco.app.security.UserAuthentication userAuth =
            (com.fisco.app.security.UserAuthentication) authentication;

        String enterpriseId = java.util.Objects.requireNonNull(request.getEnterpriseId());

        // 从Token中提取创建者标识
        // - 管理员登录：username（如 "admin", "auditor01"）
        // - 企业登录：区块链地址（如 "0x1234567890abcdef..."）
        String createdBy = authentication.getName();
        String loginType = userAuth.getLoginType();

        log.info("邀请码生成请求: enterpriseId={}, createdBy={}, loginType={}, maxUses={}, daysValid={}",
                 enterpriseId, createdBy, loginType, request.getMaxUses(), request.getDaysValid());

        // 权限验证：企业管理员只能为本企业生成邀请码
        if (!userAuth.isSystemAdmin()) {
            if (!userAuth.getEnterpriseId().equals(enterpriseId)) {
                auditService.logAccessDenied(
                    authentication,
                    "INVITATION_CODE_GENERATE",
                    enterpriseId,
                    "generateCode",
                    "尝试为其他企业生成邀请码",
                    null
                );
                log.warn("权限拒绝: createdBy={} 尝试为企业 {} 生成邀请码", createdBy, enterpriseId);
                throw new com.fisco.app.exception.BusinessException("只能为本企业生成邀请码");
            }
        }

        // 验证企业存在
        enterpriseService.getEnterpriseById(enterpriseId);

        // 生成邀请码（createdBy已从Token提取）
        InvitationCode code = invitationCodeService.generateCode(
                enterpriseId,
                createdBy,  // 从Token获取的创建者标识
                request.getMaxUses(),
                request.getDaysValid()
        );

        // 根据创建时间判断是新生成还是返回现有邀请码
        String message;
        java.time.LocalDateTime fiveSecondsAgo = java.time.LocalDateTime.now().minusSeconds(5);
        if (code.getCreatedAt().isAfter(fiveSecondsAgo)) {
            message = "邀请码生成成功";
            log.info("邀请码生成成功: code={}, enterpriseId={}, createdBy={}, loginType={}",
                     code.getCode(), enterpriseId, createdBy, loginType);
        } else {
            message = "企业已存在邀请码，返回现有邀请码";
            log.info("返回现有邀请码: code={}, enterpriseId={}, createdBy={}, loginType={}",
                     code.getCode(), enterpriseId, createdBy, loginType);
        }

        return Result.success(message, code);
    }

    /**
     * 获取企业的所有邀请码
     * GET /api/invitation-codes/enterprise/{enterpriseId}
     */
    @GetMapping("/enterprise/{enterpriseId}")
    @ApiOperation(value = "获取企业邀请码", notes = "查询指定企业的所有邀请码")
    @com.fisco.app.security.annotations.RequireEnterpriseAccess(param = "enterpriseId")
    public Result<List<InvitationCode>> getCodesByEnterprise(
            @ApiParam(value = "企业ID", required = true) @PathVariable @NonNull String enterpriseId) {
        List<InvitationCode> codes = invitationCodeService.getCodesByEnterprise(enterpriseId);
        return Result.success(codes);
    }

    /**
     * 获取企业的所有有效邀请码
     * GET /api/invitation-codes/enterprise/{enterpriseId}/active
     */
    @GetMapping("/enterprise/{enterpriseId}/active")
    @ApiOperation(value = "获取有效邀请码", notes = "查询指定企业的所有有效邀请码")
    @com.fisco.app.security.annotations.RequireEnterpriseAccess(param = "enterpriseId")
    public Result<List<InvitationCode>> getActiveCodesByEnterprise(
            @ApiParam(value = "企业ID", required = true) @PathVariable @NonNull String enterpriseId) {
        List<InvitationCode> codes = invitationCodeService.getActiveCodesByEnterprise(enterpriseId);
        return Result.success(codes);
    }

    /**
     * 验证邀请码
     * GET /api/invitation-codes/validate/{code}
     *
     * 安全说明：只返回验证状态，不返回完整的邀请码信息，防止信息泄露
     */
    @GetMapping("/validate/{code}")
    @ApiOperation(value = "验证邀请码", notes = "验证邀请码是否有效（仅返回状态）")
    public Result<java.util.Map<String, Object>> validateCode(
            @ApiParam(value = "邀请码", required = true) @PathVariable String code) {

        com.fisco.app.entity.InvitationCode invitationCode =
            invitationCodeService.validateCode(code);

        // 只返回验证状态，不返回详细信息（防止信息泄露）
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("valid", invitationCode.isValid());
        result.put("expired", invitationCode.isExpired());
        result.put("maxUsesReached", invitationCode.isMaxUsesReached());
        result.put("status", invitationCode.getStatus().name());

        // 不返回企业信息、创建时间等敏感数据
        return Result.success("验证完成", result);
    }

    /**
     * 禁用邀请码
     * PUT /api/invitation-codes/{codeId}/disable
     */
    @PutMapping("/{codeId}/disable")
    @ApiOperation(value = "禁用邀请码", notes = "禁用指定的邀请码")
    @RequireEnterpriseAdmin
    @Audited(
        module = "INVITATION_CODE",
        actionType = "UPDATE",
        actionDesc = "禁用邀请码",
        entityType = "InvitationCode",
        logRequest = true,
        logResponse = true
    )
    public Result<String> disableCode(
            @ApiParam(value = "邀请码ID", required = true) @PathVariable Long codeId,
            Authentication authentication) {

        com.fisco.app.security.UserAuthentication userAuth =
            (com.fisco.app.security.UserAuthentication) authentication;

        // 获取邀请码信息
        com.fisco.app.entity.InvitationCode code = invitationCodeService.getCodeById(codeId);

        // 权限验证：企业管理员只能操作本企业的邀请码
        if (!userAuth.isSystemAdmin()) {
            if (!userAuth.getEnterpriseId().equals(code.getEnterpriseId())) {
                auditService.logAccessDenied(
                    authentication,
                    "INVITATION_CODE_DISABLE",
                    code.getEnterpriseId(),
                    "disableCode",
                    "尝试禁用其他企业的邀请码",
                    null
                );
                throw new com.fisco.app.exception.BusinessException("只能操作本企业的邀请码");
            }
        }

        invitationCodeService.disableCode(codeId);
        return Result.success("邀请码已禁用");
    }

    /**
     * 启用邀请码
     * PUT /api/invitation-codes/{codeId}/enable
     */
    @PutMapping("/{codeId}/enable")
    @ApiOperation(value = "启用邀请码", notes = "启用指定的邀请码")
    @RequireEnterpriseAdmin
    @Audited(
        module = "INVITATION_CODE",
        actionType = "UPDATE",
        actionDesc = "启用邀请码",
        entityType = "InvitationCode",
        logRequest = true,
        logResponse = true
    )
    public Result<String> enableCode(
            @ApiParam(value = "邀请码ID", required = true) @PathVariable Long codeId,
            Authentication authentication) {

        com.fisco.app.security.UserAuthentication userAuth =
            (com.fisco.app.security.UserAuthentication) authentication;

        // 获取邀请码信息
        com.fisco.app.entity.InvitationCode code = invitationCodeService.getCodeById(codeId);

        // 权限验证：企业管理员只能操作本企业的邀请码
        if (!userAuth.isSystemAdmin()) {
            if (!userAuth.getEnterpriseId().equals(code.getEnterpriseId())) {
                auditService.logAccessDenied(
                    authentication,
                    "INVITATION_CODE_ENABLE",
                    code.getEnterpriseId(),
                    "enableCode",
                    "尝试启用其他企业的邀请码",
                    null
                );
                throw new com.fisco.app.exception.BusinessException("只能操作本企业的邀请码");
            }
        }

        invitationCodeService.enableCode(codeId);
        return Result.success("邀请码已启用");
    }

    /**
     * 删除邀请码
     * DELETE /api/invitation-codes/{codeId}
     */
    @DeleteMapping("/{codeId}")
    @ApiOperation(value = "删除邀请码", notes = "删除指定的邀请码")
    @RequireEnterpriseAdmin
    @Audited(
        module = "INVITATION_CODE",
        actionType = "DELETE",
        actionDesc = "删除邀请码",
        entityType = "InvitationCode",
        logRequest = true,
        logResponse = true
    )
    public Result<String> deleteCode(
            @ApiParam(value = "邀请码ID", required = true) @PathVariable Long codeId,
            Authentication authentication) {

        com.fisco.app.security.UserAuthentication userAuth =
            (com.fisco.app.security.UserAuthentication) authentication;

        // 获取邀请码信息
        com.fisco.app.entity.InvitationCode code = invitationCodeService.getCodeById(codeId);

        // 权限验证：企业管理员只能操作本企业的邀请码
        if (!userAuth.isSystemAdmin()) {
            if (!userAuth.getEnterpriseId().equals(code.getEnterpriseId())) {
                auditService.logAccessDenied(
                    authentication,
                    "INVITATION_CODE_DELETE",
                    code.getEnterpriseId(),
                    "deleteCode",
                    "尝试删除其他企业的邀请码",
                    null
                );
                throw new com.fisco.app.exception.BusinessException("只能操作本企业的邀请码");
            }
        }

        invitationCodeService.deleteCode(codeId);
        return Result.success("邀请码已删除");
    }

    /**
     * 获取邀请码详情
     * GET /api/invitation-codes/{codeId}
     */
    @GetMapping("/{codeId}")
    @ApiOperation(value = "获取邀请码详情", notes = "查询邀请码详细信息")
    @RequireEnterpriseAdmin
    @Audited(
        module = "INVITATION_CODE",
        actionType = "QUERY",
        actionDesc = "查询邀请码详情",
        entityType = "InvitationCode",
        logRequest = true,
        logResponse = false
    )
    public Result<InvitationCode> getCodeById(
            @ApiParam(value = "邀请码ID", required = true) @PathVariable Long codeId,
            Authentication authentication) {

        com.fisco.app.security.UserAuthentication userAuth =
            (com.fisco.app.security.UserAuthentication) authentication;

        // 获取邀请码信息
        com.fisco.app.entity.InvitationCode code = invitationCodeService.getCodeById(codeId);

        // 权限验证：企业管理员只能查看本企业的邀请码
        if (!userAuth.isSystemAdmin()) {
            if (!userAuth.getEnterpriseId().equals(code.getEnterpriseId())) {
                auditService.logAccessDenied(
                    authentication,
                    "INVITATION_CODE_VIEW",
                    code.getEnterpriseId(),
                    "getCodeById",
                    "尝试查看其他企业的邀请码",
                    null
                );
                throw new com.fisco.app.exception.BusinessException("只能查看本企业的邀请码");
            }
        }

        return Result.success(code);
    }

    // ==================== DTO类 ====================

    /**
     * 生成邀请码请求DTO
     */
    @Data
    @Schema(name = "生成邀请码请求")
    public static class GenerateCodeRequest {
        @NotNull(message = "企业ID不能为空")
        @io.swagger.annotations.ApiModelProperty(
            value = "企业UUID",
            required = true,
            example = "550e8400-e29b-41d4-a716-446655440000",
            notes = "企业的唯一标识符（UUID格式）"
        )
        private String enterpriseId;

        @io.swagger.annotations.ApiModelProperty(value = "最大使用次数", example = "100")
        private Integer maxUses;

        @io.swagger.annotations.ApiModelProperty(value = "有效期（天）", example = "30")
        private Integer daysValid;

        @io.swagger.annotations.ApiModelProperty(value = "备注信息", example = "用于招聘财务人员")
        private String remarks;
    }
}
