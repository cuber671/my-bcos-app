package com.fisco.app.controller.pledge;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fisco.app.exception.BusinessException;

import com.fisco.app.dto.pledge.PledgeApplicationQueryRequest;
import com.fisco.app.dto.pledge.PledgeConfirmRequest;
import com.fisco.app.dto.pledge.PledgeConfirmResponse;
import com.fisco.app.dto.pledge.PledgeInitiateRequest;
import com.fisco.app.dto.pledge.PledgeInitiateResponse;
import com.fisco.app.dto.pledge.PledgeRecordResponse;
import com.fisco.app.dto.pledge.PledgeReleaseRequest;
import com.fisco.app.service.pledge.PledgeService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;

/**
 * 仓单质押融资管理Controller（简化版）
 */
@Slf4j
@RestController
@RequestMapping("/api/ewr/pledge")
@Api(tags = "仓单质押融资管理")
public class PledgeManagementController {

    @Autowired
    private PledgeService pledgeService;

    /**
     * 发起质押
     */
    @PostMapping("/initiate")
    @ApiOperation(value = "发起质押", notes = "货主发起仓单质押，直接冻结仓单（NORMAL → FROZEN），创建PLEDGE背书")
    @ApiResponses({
            @ApiResponse(code = 200, message = "质押发起成功", response = PledgeInitiateResponse.class),
            @ApiResponse(code = 400, message = "请求参数错误"),
            @ApiResponse(code = 403, message = "无权限操作"),
            @ApiResponse(code = 404, message = "仓单或金融机构不存在"),
            @ApiResponse(code = 409, message = "仓单已存在待确认背书")
    })
    public ResponseEntity<PledgeInitiateResponse> initiatePledge(
            @ApiParam(value = "质押发起信息", required = true) @Valid @RequestBody PledgeInitiateRequest request,
            BindingResult bindingResult,
            @ApiParam(value = "当前用户认证信息") Authentication authentication) {

        log.info("收到质押发起, 仓单ID: {}, 金融机构ID: {}, 质押金额: {}",
                request.getReceiptId(), request.getFinancialInstitutionId(), request.getPledgeAmount());

        // 1. 基础验证失败会自动由@Valid处理
        if (bindingResult.hasErrors()) {
            log.warn("质押发起请求参数验证失败: {}", bindingResult.getAllErrors());
            return ResponseEntity.badRequest().build();
        }

        // 2. 业务规则验证
        try {
            validatePledgeBusinessRules(request);
        } catch (BusinessException e) {
            log.warn("质押业务规则验证失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        // 3. 从认证信息获取货主ID和名称
        String ownerId = authentication.getName();
        String ownerName = authentication.getPrincipal() != null ?
                authentication.getPrincipal().toString() : "货主";

        PledgeInitiateResponse response = pledgeService.initiatePledge(request, ownerId, ownerName);

        return ResponseEntity.ok(response);
    }

    /**
     * 确认质押
     */
    @PostMapping("/confirm")
    @ApiOperation(value = "确认质押", notes = "金融机构确认质押。批准：FROZEN → PLEDGED；拒绝：FROZEN → NORMAL")
    @ApiResponses({
            @ApiResponse(code = 200, message = "质押确认成功", response = PledgeConfirmResponse.class),
            @ApiResponse(code = 400, message = "请求参数错误"),
            @ApiResponse(code = 403, message = "无权限操作"),
            @ApiResponse(code = 404, message = "背书不存在")
    })
    public ResponseEntity<PledgeConfirmResponse> confirmPledge(
            @ApiParam(value = "质押确认信息", required = true) @Valid @RequestBody PledgeConfirmRequest request,
            @ApiParam(value = "审核人ID") @RequestHeader(value = "X-User-Id", required = false) String userId,
            @ApiParam(value = "审核人姓名") @RequestHeader(value = "X-User-Name", required = false) String userName,
            @ApiParam(value = "当前用户认证信息") Authentication authentication) {

        log.info("收到质押确认, 背书ID: {}, 确认结果: {}", request.getEndorsementId(), request.getConfirmResult());

        // 从认证信息或请求头获取审核人信息
        String confirmerId = userId != null ? userId : authentication.getName();
        String confirmerName = userName != null ? userName : "金融机构人员";

        PledgeConfirmResponse response = pledgeService.confirmPledge(request, confirmerId, confirmerName);

        return ResponseEntity.ok(response);
    }

    /**
     * 还款释放质押
     */
    @PostMapping("/release")
    @ApiOperation(value = "释放质押", notes = "货主还款后释放质押仓单（PLEDGED → NORMAL），创建RELEASE背书")
    @ApiResponses({
            @ApiResponse(code = 200, message = "质押释放成功"),
            @ApiResponse(code = 400, message = "请求参数错误或还款金额不足"),
            @ApiResponse(code = 403, message = "无权限操作"),
            @ApiResponse(code = 404, message = "背书或融资记录不存在")
    })
    public ResponseEntity<Map<String, Object>> releasePledge(
            @ApiParam(value = "质押释放信息", required = true) @Valid @RequestBody PledgeReleaseRequest request,
            @ApiParam(value = "当前用户认证信息") Authentication authentication) {

        log.info("收到质押释放请求, 仓单ID: {}, 背书ID: {}, 还款金额: {}",
                request.getReceiptId(), request.getEndorsementId(), request.getRepayAmount());

        // 从认证信息获取货主ID
        String ownerId = authentication.getName();

        Map<String, Object> response = pledgeService.releasePledge(request, ownerId);

        return ResponseEntity.ok(response);
    }

    /**
     * 查询待确认的质押背书
     */
    @GetMapping("/pending/{financialInstitutionAddress}")
    @ApiOperation(value = "查询待确认质押", notes = "查询指定金融机构的待确认质押背书列表")
    @ApiResponses({
            @ApiResponse(code = 200, message = "查询成功", response = List.class)
    })
    public ResponseEntity<List<PledgeInitiateResponse>> getPendingPledges(
            @ApiParam(value = "金融机构地址", required = true) @PathVariable String financialInstitutionAddress) {

        log.info("查询待确认质押, 金融机构地址: {}", financialInstitutionAddress);

        List<PledgeInitiateResponse> pendingPledges =
                pledgeService.getPendingPledges(financialInstitutionAddress);

        return ResponseEntity.ok(pendingPledges);
    }

    /**
     * 查询仓单的质押历史
     */
    @GetMapping("/history/{receiptId}")
    @ApiOperation(value = "查询质押历史", notes = "查询指定仓单的所有质押历史记录，包括当前和历史的质押信息")
    @ApiResponses({
            @ApiResponse(code = 200, message = "查询成功", response = List.class),
            @ApiResponse(code = 404, message = "仓单不存在")
    })
    public ResponseEntity<List<PledgeRecordResponse>> getPledgeHistory(
            @ApiParam(value = "仓单ID", required = true) @PathVariable @NonNull String receiptId) {

        log.info("查询质押历史, 仓单ID: {}", receiptId);

        List<PledgeRecordResponse> history = pledgeService.getPledgeHistory(receiptId);

        return ResponseEntity.ok(history);
    }

    /**
     * 分页查询质押记录
     */
    @PostMapping("/query")
    @ApiOperation(value = "分页查询质押记录", notes = "支持多条件查询：仓单ID、货主ID、金融机构ID、状态")
    @ApiResponses({
            @ApiResponse(code = 200, message = "查询成功", response = Page.class),
            @ApiResponse(code = 400, message = "请求参数错误")
    })
    public ResponseEntity<Page<PledgeRecordResponse>> queryPledgeRecords(
            @ApiParam(value = "查询条件", required = true) @RequestBody PledgeApplicationQueryRequest request) {

        log.info("分页查询质押记录, page: {}, size: {}", request.getPage(), request.getSize());

        Page<PledgeRecordResponse> page = pledgeService.queryPledgeRecords(
                request.getReceiptId(),
                request.getOwnerId(),
                request.getFinancialInstitutionId(),
                request.getStatus(),
                request.toPageable()
        );

        return ResponseEntity.ok(page);
    }

    /**
     * 查询仓单的当前质押状态
     */
    @GetMapping("/status/{receiptId}")
    @ApiOperation(value = "查询质押状态", notes = "查询指定仓单的当前质押状态")
    @ApiResponses({
            @ApiResponse(code = 200, message = "查询成功"),
            @ApiResponse(code = 404, message = "仓单不存在")
    })
    public ResponseEntity<Map<String, Object>> getPledgeStatus(
            @ApiParam(value = "仓单ID", required = true) @PathVariable @NonNull String receiptId) {

        log.info("查询质押状态, 仓单ID: {}", receiptId);

        // 这里可以调用service方法查询质押状态
        Map<String, Object> status = new java.util.HashMap<>();
        status.put("receiptId", receiptId);
        status.put("hasPledge", false);
        status.put("message", "请使用质押记录查询接口获取详细状态");

        return ResponseEntity.ok(status);
    }

    /**
     * 验证质押业务规则
     *
     * @param request 质押发起请求
     * @throws BusinessException 如果业务规则验证失败
     */
    private void validatePledgeBusinessRules(PledgeInitiateRequest request) {
        // 验证日期逻辑
        if (request.getPledgeStartDate().isAfter(request.getPledgeEndDate())) {
            throw new BusinessException("质押开始日期不能晚于质押结束日期");
        }

        // 验证质押期限（30-365天）
        long daysBetween = ChronoUnit.DAYS.between(request.getPledgeStartDate(), request.getPledgeEndDate());
        if (daysBetween < 30 || daysBetween > 365) {
            throw new BusinessException("质押期限必须在30-365天之间");
        }

        // 验证质押金额与质押率的合理性（已经在DTO层验证了金额和质押率的基本范围）
        // 这里可以添加更复杂的业务逻辑验证
        log.debug("质押业务规则验证通过: 期限={}天", daysBetween);
    }

    // ==================== 兼容性接口（已废弃）====================

    /**
     * @deprecated 使用 /initiate 接口替代
     */
    @Deprecated
    @PostMapping("/apply")
    @ApiOperation(value = "【已废弃】创建质押申请", notes = "请使用 /initiate 接口替代")
    public ResponseEntity<Map<String, Object>> applyPledge(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("success", false);
        response.put("message", "该接口已废弃，请使用 /initiate 接口");
        response.put("newEndpoint", "/api/ewr/pledge/initiate");

        return ResponseEntity.ok(response);
    }

    /**
     * @deprecated 使用 /confirm 接口替代
     */
    @Deprecated
    @PostMapping("/approve")
    @ApiOperation(value = "【已废弃】审核质押申请", notes = "请使用 /confirm 接口替代")
    public ResponseEntity<Map<String, Object>> approvePledge(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("success", false);
        response.put("message", "该接口已废弃，请使用 /confirm 接口");
        response.put("newEndpoint", "/api/ewr/pledge/confirm");

        return ResponseEntity.ok(response);
    }
}
