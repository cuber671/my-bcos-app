package com.fisco.app.controller.admin;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fisco.app.entity.enterprise.Enterprise;
import com.fisco.app.entity.enterprise.EnterpriseAuditLog;
import com.fisco.app.entity.user.Admin;
import com.fisco.app.security.RequireAdmin;
import com.fisco.app.service.enterprise.EnterpriseService;
import com.fisco.app.vo.Result;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 管理员企业管理Controller
 * 管理员对企业的管理操作：审核、查询、修改状态、额度、评级等
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/enterprise")
@RequiredArgsConstructor
@Api(tags = "管理员-企业管理")
public class AdminEnterpriseController {

    private final EnterpriseService enterpriseService;

    /**
     * 获取所有企业列表（分页）
     * GET /api/admin/enterprise/list
     */
    @GetMapping("/list")
    @ApiOperation(value = "获取企业列表", notes = "分页查询所有企业，支持按状态筛选")
    @RequireAdmin(RequireAdmin.AdminRole.AUDITOR)
    public Result<Page<Enterprise>> getEnterprises(
            @ApiParam(value = "页码", example = "0") @RequestParam(defaultValue = "0") int page,
            @ApiParam(value = "每页大小", example = "10") @RequestParam(defaultValue = "10") int size,
            @ApiParam(value = "企业状态（可选）") @RequestParam(required = false) Enterprise.EnterpriseStatus status) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Enterprise> enterprises;

        if (status != null) {
            enterprises = enterpriseService.getEnterprisesByStatus(status, pageable);
        } else {
            enterprises = enterpriseService.getAllEnterprises(pageable);
        }

        // 清除密码字段
        enterprises.forEach(e -> e.setPassword(null));
        return Result.success(enterprises);
    }

    /**
     * 获取待审核企业列表
     * GET /api/admin/enterprise/pending
     */
    @GetMapping("/pending")
    @ApiOperation(value = "获取待审核企业", notes = "查询所有待审核的企业")
    @RequireAdmin(RequireAdmin.AdminRole.AUDITOR)
    public Result<Page<Enterprise>> getPendingEnterprises(
            @ApiParam(value = "页码", example = "0") @RequestParam(defaultValue = "0") int page,
            @ApiParam(value = "每页大小", example = "10") @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Enterprise> pending = enterpriseService.getPendingEnterprises(pageable);
        pending.forEach(e -> e.setPassword(null));
        return Result.success(pending);
    }

    /**
     * 获取已激活企业列表
     * GET /api/admin/enterprise/active
     */
    @GetMapping("/active")
    @ApiOperation(value = "获取已激活企业", notes = "查询所有已激活的企业（status = ACTIVE）")
    @io.swagger.annotations.ApiResponses({
        @io.swagger.annotations.ApiResponse(code = 200, message = "查询成功", response = org.springframework.data.domain.Page.class),
        @io.swagger.annotations.ApiResponse(code = 401, message = "未授权访问，需要提供有效的管理员token"),
        @io.swagger.annotations.ApiResponse(code = 403, message = "权限不足，需要审核员或管理员角色"),
        @io.swagger.annotations.ApiResponse(code = 500, message = "服务器内部错误")
    })
    @RequireAdmin(RequireAdmin.AdminRole.AUDITOR)
    public Result<Page<Enterprise>> getActiveEnterprises(
            @ApiParam(value = "页码", example = "0") @RequestParam(defaultValue = "0") int page,
            @ApiParam(value = "每页大小", example = "10") @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Enterprise> active = enterpriseService.getEnterprisesByStatus(
            Enterprise.EnterpriseStatus.ACTIVE,
            pageable
        );
        active.forEach(e -> e.setPassword(null));
        return Result.success(active);
    }

    /**
     * 获取企业详细信息
     * GET /api/admin/enterprise/{id}
     */
    @GetMapping("/{id}")
    @ApiOperation(value = "获取企业详情", notes = "根据ID查询企业详细信息")
    @RequireAdmin(RequireAdmin.AdminRole.AUDITOR)
    public Result<Enterprise> getEnterprise(@NonNull @PathVariable String id) {
        Enterprise enterprise = enterpriseService.getEnterpriseById(id);
        enterprise.setPassword(null);
        return Result.success(enterprise);
    }

    /**
     * 获取企业创建者信息
     * GET /api/admin/enterprise/{id}/creator-info
     *
     * 说明：
     * - 查询企业的创建者标识（created_by）
     * - 公开注册：created_by = "SELF_REGISTER"
     * - 管理员代注册：created_by = 管理员用户名
     */
    @GetMapping("/{id}/creator-info")
    @ApiOperation(value = "获取企业创建者信息", notes = "根据ID查询企业的创建者信息。created_by字段表示企业注册方式：SELF_REGISTER（公开注册）或管理员用户名（代注册）")
    @io.swagger.annotations.ApiResponses({
        @io.swagger.annotations.ApiResponse(code = 200, message = "查询成功", response = CreatorInfoResponse.class),
        @io.swagger.annotations.ApiResponse(code = 401, message = "未授权访问，需要提供有效的管理员token"),
        @io.swagger.annotations.ApiResponse(code = 403, message = "权限不足，需要审核员或管理员角色"),
        @io.swagger.annotations.ApiResponse(code = 404, message = "企业不存在"),
        @io.swagger.annotations.ApiResponse(code = 500, message = "服务器内部错误")
    })
    @RequireAdmin(RequireAdmin.AdminRole.AUDITOR)
    public Result<CreatorInfoResponse> getCreatorInfo(@NonNull @PathVariable String id) {
        Enterprise enterprise = enterpriseService.getEnterpriseById(id);

        CreatorInfoResponse response = new CreatorInfoResponse();
        response.setEnterpriseId(enterprise.getId());
        response.setEnterpriseName(enterprise.getName());
        response.setCreatedBy(enterprise.getCreatedBy());
        response.setCreatedAt(enterprise.getRegisteredAt());

        return Result.success(response);
    }

    /**
     * 审核通过企业注册
     * PUT /api/admin/enterprise/{id}/approve
     */
    @PutMapping("/{id}/approve")
    @ApiOperation(value = "审核通过企业", notes = "管理员审核通过企业注册申请")
    @RequireAdmin(RequireAdmin.AdminRole.AUDITOR)
    public Result<Map<String, String>> approveEnterprise(
            @ApiParam(value = "企业ID", required = true) @NonNull @PathVariable String id,
            @RequestBody(required = false) ApprovalRequest request,
            HttpServletRequest httpRequest) {
        Admin admin = (Admin) httpRequest.getAttribute("currentAdmin");
        String ip = getClientIp(httpRequest);
        String reason = request != null ? request.getReason() : null;

        // 使用企业ID进行审核（推荐方式）
        enterpriseService.approveEnterpriseById(id, admin.getUsername(), reason, ip);

        Map<String, String> response = new HashMap<>();
        response.put("message", "企业已审核通过");
        response.put("enterpriseId", id);
        response.put("auditor", admin.getUsername());
        return Result.success("审核通过", response);
    }

    /**
     * 拒绝企业注册
     * PUT /api/admin/enterprise/{id}/reject
     */
    @PutMapping("/{id}/reject")
    @ApiOperation(value = "拒绝企业注册", notes = "管理员拒绝企业注册申请")
    @RequireAdmin(RequireAdmin.AdminRole.AUDITOR)
    public Result<Map<String, String>> rejectEnterprise(
            @ApiParam(value = "企业ID", required = true) @NonNull @PathVariable String id,
            @RequestBody(required = false) ApprovalRequest request,
            HttpServletRequest httpRequest) {
        Admin admin = (Admin) httpRequest.getAttribute("currentAdmin");
        String ip = getClientIp(httpRequest);
        String reason = request != null ? request.getReason() : null;

        // 使用企业ID进行拒绝（推荐方式）
        enterpriseService.rejectEnterpriseById(id, admin.getUsername(), reason, ip);

        Map<String, String> response = new HashMap<>();
        response.put("message", "企业注册已拒绝");
        response.put("enterpriseId", id);
        response.put("auditor", admin.getUsername());
        return Result.success("已拒绝", response);
    }

    /**
     * 更新企业状态
     * PUT /api/admin/enterprise/{id}/status
     */
    @PutMapping("/{id}/status")
    @ApiOperation(value = "更新企业状态", notes = "更新企业状态（激活、暂停、拉黑等）")
    @RequireAdmin(RequireAdmin.AdminRole.ADMIN)
    public Result<Map<String, String>> updateEnterpriseStatus(
            @ApiParam(value = "企业ID", required = true) @NonNull @PathVariable String id,
            @Valid @RequestBody UpdateStatusRequest request,
            HttpServletRequest httpRequest) {
        Admin admin = (Admin) httpRequest.getAttribute("currentAdmin");

        Enterprise enterprise = enterpriseService.getEnterpriseById(id);
        enterpriseService.updateEnterpriseStatus(enterprise.getAddress(), request.getStatus(), admin.getUsername());

        log.info("管理员更新企业状态: admin={}, enterpriseId={}, status={}",
                admin.getUsername(), id, request.getStatus());

        Map<String, String> response = new HashMap<>();
        response.put("message", "企业状态已更新");
        response.put("enterpriseId", id);
        response.put("status", request.getStatus().name());
        return Result.success("状态已更新", response);
    }

    /**
     * 更新信用评级
     * PUT /api/admin/enterprise/{id}/credit-rating
     */
    @PutMapping("/{id}/credit-rating")
    @ApiOperation(value = "更新信用评级", notes = "更新企业信用评级，范围0-100")
    @RequireAdmin(RequireAdmin.AdminRole.ADMIN)
    public Result<Map<String, String>> updateCreditRating(
            @ApiParam(value = "企业ID", required = true) @NonNull @PathVariable String id,
            @Valid @RequestBody UpdateCreditRatingRequest request,
            HttpServletRequest httpRequest) {
        Admin admin = (Admin) httpRequest.getAttribute("currentAdmin");

        Enterprise enterprise = enterpriseService.getEnterpriseById(id);
        // 使用4参数版本的 updateCreditRating 方法（包含 reason 参数）
        enterpriseService.updateCreditRating(
            enterprise.getAddress(),
            request.getCreditRating(),
            request.getReason(),
            admin.getUsername()
        );

        log.info("管理员更新企业信用评级: admin={}, enterpriseId={}, creditRating={}",
                admin.getUsername(), id, request.getCreditRating());

        Map<String, String> response = new HashMap<>();
        response.put("message", "信用评级已更新");
        response.put("enterpriseId", id);
        response.put("creditRating", String.valueOf(request.getCreditRating()));
        return Result.success("评级已更新", response);
    }

    /**
     * 设置授信额度
     * PUT /api/admin/enterprise/{id}/credit-limit
     */
    @PutMapping("/{id}/credit-limit")
    @ApiOperation(value = "设置授信额度", notes = "为金融机构设置企业的授信额度")
    @RequireAdmin(RequireAdmin.AdminRole.ADMIN)
    public Result<Map<String, String>> setCreditLimit(
            @ApiParam(value = "企业ID", required = true) @NonNull @PathVariable String id,
            @Valid @RequestBody UpdateCreditLimitRequest request,
            HttpServletRequest httpRequest) {
        Admin admin = (Admin) httpRequest.getAttribute("currentAdmin");

        Enterprise enterprise = enterpriseService.getEnterpriseById(id);
        enterpriseService.setCreditLimit(enterprise.getAddress(), request.getCreditLimit(), admin.getUsername());

        log.info("管理员设置企业授信额度: admin={}, enterpriseId={}, creditLimit={}",
                admin.getUsername(), id, request.getCreditLimit());

        Map<String, String> response = new HashMap<>();
        response.put("message", "授信额度已设置");
        response.put("enterpriseId", id);
        response.put("creditLimit", request.getCreditLimit().toString());
        return Result.success("额度已设置", response);
    }

    /**
     * 批量审核通过企业
     * POST /api/admin/enterprise/batch-approve
     */
    @PostMapping("/batch-approve")
    @ApiOperation(value = "批量审核企业", notes = "批量审核通过多个企业注册申请")
    @RequireAdmin(RequireAdmin.AdminRole.AUDITOR)
    public Result<Map<String, Object>> batchApprove(
            @Valid @RequestBody BatchAuditRequest request,
            HttpServletRequest httpRequest) {
        Admin admin = (Admin) httpRequest.getAttribute("currentAdmin");
        String ip = getClientIp(httpRequest);

        var result = enterpriseService.batchApproveEnterprises(request.getIds(), admin.getUsername(), ip);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "批量审核完成");
        response.put("total", result.getTotalCount());
        response.put("success", result.getSuccessCount());
        response.put("failed", result.getFailCount());
        response.put("auditor", admin.getUsername());
        return Result.success("批量审核完成", response);
    }

    /**
     * 批量拒绝企业
     * POST /api/admin/enterprise/batch-reject
     */
    @PostMapping("/batch-reject")
    @ApiOperation(value = "批量拒绝企业", notes = "批量拒绝多个企业注册申请")
    @RequireAdmin(RequireAdmin.AdminRole.AUDITOR)
    public Result<Map<String, Object>> batchReject(
            @Valid @RequestBody BatchAuditRequest request,
            HttpServletRequest httpRequest) {
        Admin admin = (Admin) httpRequest.getAttribute("currentAdmin");
        String ip = getClientIp(httpRequest);

        var result = enterpriseService.batchRejectEnterprises(request.getIds(), admin.getUsername(),
                request.getReason(), ip);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "批量拒绝完成");
        response.put("total", result.getTotalCount());
        response.put("success", result.getSuccessCount());
        response.put("failed", result.getFailCount());
        response.put("auditor", admin.getUsername());
        return Result.success("批量拒绝完成", response);
    }

    /**
     * 获取企业审核历史
     * GET /api/admin/enterprise/{id}/audit-history
     */
    @GetMapping("/{id}/audit-history")
    @ApiOperation(value = "获取企业审核历史", notes = "查询指定企业的审核操作历史")
    @RequireAdmin(RequireAdmin.AdminRole.AUDITOR)
    public Result<List<EnterpriseAuditLog>> getEnterpriseAuditHistory(@NonNull @PathVariable String id) {
        Enterprise enterprise = enterpriseService.getEnterpriseById(id);
        List<EnterpriseAuditLog> history = enterpriseService.getEnterpriseAuditHistory(enterprise.getAddress());
        return Result.success(history);
    }

    /**
     * 获取所有审核日志（分页）
     * GET /api/admin/enterprise/audit-logs
     */
    @GetMapping("/audit-logs")
    @ApiOperation(value = "获取所有审核日志", notes = "分页查询所有企业的审核操作日志")
    @RequireAdmin(RequireAdmin.AdminRole.AUDITOR)
    public Result<Page<EnterpriseAuditLog>> getAllAuditLogs(
            @ApiParam(value = "页码", example = "0") @RequestParam(defaultValue = "0") int page,
            @ApiParam(value = "每页大小", example = "20") @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<EnterpriseAuditLog> logs = enterpriseService.getAllAuditLogs(pageable);
        return Result.success(logs);
    }

    /**
     * 获取审核统计信息
     * GET /api/admin/enterprise/audit-stats
     */
    @GetMapping("/audit-stats")
    @ApiOperation(value = "获取审核统计", notes = "统计各个审核人的审核次数")
    @RequireAdmin(RequireAdmin.AdminRole.AUDITOR)
    public Result<Map<String, Long>> getAuditorStatistics() {
        Map<String, Long> stats = enterpriseService.getAuditorStatistics();
        return Result.success(stats);
    }

    /**
     * 搜索企业
     * GET /api/admin/enterprise/search
     */
    @GetMapping("/search")
    @ApiOperation(value = "搜索企业", notes = "按名称、信用代码或用户名搜索企业")
    @RequireAdmin(RequireAdmin.AdminRole.AUDITOR)
    public Result<List<Enterprise>> searchEnterprises(
            @ApiParam(value = "搜索关键词", required = true) @RequestParam String keyword) {
        List<Enterprise> results = enterpriseService.searchEnterprises(keyword);
        results.forEach(e -> e.setPassword(null));
        return Result.success(results);
    }

    /**
     * 获取待删除企业列表（注销审核中）
     * GET /api/admin/enterprise/pending-deletion
     */
    @GetMapping("/pending-deletion")
    @ApiOperation(value = "获取待删除企业", notes = "查询所有申请注销待审核的企业")
    @io.swagger.annotations.ApiResponses({
        @io.swagger.annotations.ApiResponse(code = 200, message = "查询成功", response = org.springframework.data.domain.Page.class),
        @io.swagger.annotations.ApiResponse(code = 401, message = "未授权访问，需要提供有效的管理员token"),
        @io.swagger.annotations.ApiResponse(code = 403, message = "权限不足，需要审核员或管理员角色"),
        @io.swagger.annotations.ApiResponse(code = 500, message = "服务器内部错误")
    })
    @RequireAdmin(RequireAdmin.AdminRole.AUDITOR)
    public Result<Page<Enterprise>> getPendingDeletionEnterprises(
            @ApiParam(value = "页码", example = "0") @RequestParam(defaultValue = "0") int page,
            @ApiParam(value = "每页大小", example = "10") @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Enterprise> pending = enterpriseService.getEnterprisesByStatus(
            Enterprise.EnterpriseStatus.PENDING_DELETION,
            pageable
        );
        pending.forEach(e -> e.setPassword(null));
        return Result.success(pending);
    }

    /**
     * 审核通过企业注销
     * PUT /api/admin/enterprise/{id}/approve-deletion
     *
     * 支持通过企业ID（UUID）或区块链地址进行操作
     */
    @PutMapping("/{id}/approve-deletion")
    @ApiOperation(value = "审核通过企业注销",
        notes = "管理员审核通过企业注销申请，将删除企业及其所有关联数据。支持通过企业ID或区块链地址操作")
    @io.swagger.annotations.ApiResponses({
        @io.swagger.annotations.ApiResponse(
            code = 200,
            message = "注销成功，企业已从区块链和数据库删除",
            response = Map.class
        ),
        @io.swagger.annotations.ApiResponse(
            code = 400,
            message = "请求参数错误或企业状态不允许注销。" +
                      "只有PENDING_DELETION状态的企业才能通过注销审核。"
        ),
        @io.swagger.annotations.ApiResponse(
            code = 401,
            message = "未授权访问，需要在请求头中提供有效的管理员JWT token"
        ),
        @io.swagger.annotations.ApiResponse(
            code = 403,
            message = "权限不足，需要AUDITOR或ADMIN角色"
        ),
        @io.swagger.annotations.ApiResponse(
            code = 404,
            message = "企业不存在，请检查企业ID或区块链地址是否正确"
        ),
        @io.swagger.annotations.ApiResponse(
            code = 500,
            message = "服务器内部错误。可能原因：区块链操作失败、数据库删除失败等"
        )
    })
    @RequireAdmin(RequireAdmin.AdminRole.AUDITOR)
    public Result<Map<String, String>> approveEnterpriseDeletion(
            @ApiParam(value = "企业ID或区块链地址", required = true, example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890 或 0x1234567890abcdef...")
            @NonNull @PathVariable String id,
            @RequestBody(required = false) ApprovalRequest request,
            HttpServletRequest httpRequest) {
        Admin admin = (Admin) httpRequest.getAttribute("currentAdmin");
        String ip = getClientIp(httpRequest);
        String reason = request != null ? request.getReason() : null;

        log.info("管理员审核通过企业注销: operator={}, id={}, reason={}", admin.getUsername(), id, reason);

        // 根据id格式判断是企业ID还是区块链地址
        Enterprise enterprise;
        if (id.startsWith("0x") && id.length() == 42) {
            // 区块链地址（42字符，以0x开头）
            log.debug("通过区块链地址查找企业: address={}", id);
            enterprise = enterpriseService.getEnterprise(id);
        } else {
            // 企业ID（UUID格式）
            log.debug("通过企业ID查找: id={}", id);
            enterprise = enterpriseService.getEnterpriseById(id);
        }

        // 执行注销操作
        enterpriseService.approveEnterpriseDeletion(
            enterprise.getAddress(),
            admin.getUsername(),
            reason,
            ip
        );

        Map<String, String> response = new HashMap<>();
        response.put("message", "企业已注销");
        response.put("enterpriseId", id);
        response.put("enterpriseName", enterprise.getName());
        response.put("enterpriseAddress", enterprise.getAddress());
        response.put("auditor", admin.getUsername());
        response.put("auditTime", java.time.LocalDateTime.now().toString());

        log.info("企业注销审核通过完成: operator={}, enterprise={}, name={}",
                 admin.getUsername(), enterprise.getAddress(), enterprise.getName());

        return Result.success("注销完成", response);
    }

    /**
     * 拒绝企业注销申请
     * PUT /api/admin/enterprise/{id}/reject-deletion
     *
     * 支持通过企业ID（UUID）或区块链地址进行操作
     */
    @PutMapping("/{id}/reject-deletion")
    @ApiOperation(value = "拒绝企业注销",
        notes = "管理员拒绝企业注销申请，企业状态恢复为ACTIVE。支持通过企业ID或区块链地址操作")
    @io.swagger.annotations.ApiResponses({
        @io.swagger.annotations.ApiResponse(
            code = 200,
            message = "拒绝成功，企业状态已恢复为ACTIVE",
            response = Map.class
        ),
        @io.swagger.annotations.ApiResponse(
            code = 400,
            message = "请求参数错误或企业状态不允许拒绝。" +
                      "只有PENDING_DELETION状态的企业才能拒绝注销。"
        ),
        @io.swagger.annotations.ApiResponse(
            code = 401,
            message = "未授权访问，需要在请求头中提供有效的管理员JWT token"
        ),
        @io.swagger.annotations.ApiResponse(
            code = 403,
            message = "权限不足，需要AUDITOR或ADMIN角色"
        ),
        @io.swagger.annotations.ApiResponse(
            code = 404,
            message = "企业不存在，请检查企业ID或区块链地址是否正确"
        ),
        @io.swagger.annotations.ApiResponse(
            code = 500,
            message = "服务器内部错误"
        )
    })
    @RequireAdmin(RequireAdmin.AdminRole.AUDITOR)
    public Result<Map<String, String>> rejectEnterpriseDeletion(
            @ApiParam(value = "企业ID或区块链地址", required = true, example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890 或 0x1234567890abcdef...")
            @NonNull @PathVariable String id,
            @RequestBody(required = false) ApprovalRequest request,
            HttpServletRequest httpRequest) {
        Admin admin = (Admin) httpRequest.getAttribute("currentAdmin");
        String ip = getClientIp(httpRequest);
        String reason = request != null ? request.getReason() : null;

        log.info("管理员拒绝企业注销: operator={}, id={}, reason={}", admin.getUsername(), id, reason);

        // 根据id格式判断是企业ID还是区块链地址
        Enterprise enterprise;
        if (id.startsWith("0x") && id.length() == 42) {
            // 区块链地址（42字符，以0x开头）
            log.debug("通过区块链地址查找企业: address={}", id);
            enterprise = enterpriseService.getEnterprise(id);
        } else {
            // 企业ID（UUID格式）
            log.debug("通过企业ID查找: id={}", id);
            enterprise = enterpriseService.getEnterpriseById(id);
        }

        // 执行拒绝操作
        enterpriseService.rejectEnterpriseDeletion(
            enterprise.getAddress(),
            admin.getUsername(),
            reason,
            ip
        );

        Map<String, String> response = new HashMap<>();
        response.put("message", "注销申请已拒绝");
        response.put("enterpriseId", id);
        response.put("enterpriseName", enterprise.getName());
        response.put("enterpriseAddress", enterprise.getAddress());
        response.put("status", "ACTIVE");
        response.put("auditor", admin.getUsername());
        response.put("auditTime", java.time.LocalDateTime.now().toString());

        log.info("企业注销申请已拒绝: operator={}, enterprise={}, name={}",
                 admin.getUsername(), enterprise.getAddress(), enterprise.getName());

        return Result.success("已拒绝", response);
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
     * 审核请求DTO
     */
    @Data
    @io.swagger.annotations.ApiModel(
        value = "审核请求",
        description = "管理员审核企业注册或注销申请时提供的审核意见"
    )
    public static class ApprovalRequest {

        @io.swagger.annotations.ApiModelProperty(
            value = "审核意见",
            notes = "管理员审核时填写的意见或原因，可选填。例如：资料齐全，审核通过",
            example = "资料齐全，符合注册要求，审核通过",
            required = false,
            dataType = "string"
        )
        private String reason;
    }

    /**
     * 更新状态请求DTO
     */
    @Data
    public static class UpdateStatusRequest {
        @NotNull(message = "状态不能为空")
        @io.swagger.annotations.ApiModelProperty(value = "企业状态", required = true, notes = "ACTIVE-已激活, SUSPENDED-已暂停, BLACKLISTED-已拉黑")
        private Enterprise.EnterpriseStatus status;
    }

    /**
     * 更新信用评级请求DTO
     */
    @Data
    public static class UpdateCreditRatingRequest {
        @NotNull(message = "信用评级不能为空")
        @io.swagger.annotations.ApiModelProperty(value = "信用评级(0-100)", required = true, example = "75")
        private Integer creditRating;

        @io.swagger.annotations.ApiModelProperty(value = "变更原因", example = "按时还款记录良好，经营状况稳定", notes = "可选，建议提供详细的评级变更原因以便追溯")
        private String reason;
    }

    /**
     * 更新授信额度请求DTO
     */
    @Data
    public static class UpdateCreditLimitRequest {
        @NotNull(message = "授信额度不能为空")
        @io.swagger.annotations.ApiModelProperty(value = "授信额度", required = true, example = "1000000.00")
        private BigDecimal creditLimit;
    }

    /**
     * 批量审核请求DTO
     */
    @Data
    public static class BatchAuditRequest {
        @io.swagger.annotations.ApiModelProperty(value = "企业ID列表", required = true)
        private List<String> ids;

        @io.swagger.annotations.ApiModelProperty(value = "审核/拒绝理由", example = "批量审核")
        private String reason;
    }

    /**
     * 创建者信息响应DTO
     */
    @Data
    @io.swagger.annotations.ApiModel(description = "企业创建者信息")
    public static class CreatorInfoResponse {
        @io.swagger.annotations.ApiModelProperty(value = "企业ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        private String enterpriseId;

        @io.swagger.annotations.ApiModelProperty(value = "企业名称", example = "供应商A")
        private String enterpriseName;

        @io.swagger.annotations.ApiModelProperty(value = "创建者标识", example = "SELF_REGISTER",
            notes = "企业注册方式：SELF_REGISTER-公开自主注册；admin/auditor01-管理员代注册（管理员用户名）")
        private String createdBy;

        @io.swagger.annotations.ApiModelProperty(value = "创建时间", example = "2026-01-20T10:00:00")
        private java.time.LocalDateTime createdAt;
    }

}
