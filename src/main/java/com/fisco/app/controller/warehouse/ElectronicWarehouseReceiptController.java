package com.fisco.app.controller.warehouse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fisco.app.dto.receivable.SplitApplicationRequest;
import com.fisco.app.dto.receivable.SplitApplicationResponse;
import com.fisco.app.dto.receivable.SplitApprovalRequest;
import com.fisco.app.dto.receivable.SplitApprovalResponse;
import com.fisco.app.dto.warehouse.CancelApplicationRequest;
import com.fisco.app.dto.warehouse.CancelApplicationResponse;
import com.fisco.app.dto.warehouse.CancelApprovalRequest;
import com.fisco.app.dto.warehouse.CancelApprovalResponse;
import com.fisco.app.dto.warehouse.DeliveryUpdateRequest;
import com.fisco.app.dto.warehouse.ElectronicWarehouseReceiptCreateRequest;
import com.fisco.app.dto.warehouse.ElectronicWarehouseReceiptQueryRequest;
import com.fisco.app.dto.warehouse.ElectronicWarehouseReceiptResponse;
import com.fisco.app.dto.warehouse.ElectronicWarehouseReceiptUpdateRequest;
import com.fisco.app.dto.warehouse.FreezeApplicationResponse;
import com.fisco.app.dto.warehouse.FreezeApplicationReviewRequest;
import com.fisco.app.dto.warehouse.FreezeApplicationReviewResponse;
import com.fisco.app.dto.warehouse.FreezeApplicationSubmitRequest;
import com.fisco.app.dto.warehouse.ReceiptApprovalRequest;
import com.fisco.app.dto.warehouse.ReceiptApprovalResponse;
import com.fisco.app.dto.warehouse.ReceiptFreezeResponse;
import com.fisco.app.dto.warehouse.ReceiptUnfreezeResponse;
import com.fisco.app.dto.warehouse.MergeReceiptsRequest;
import com.fisco.app.dto.warehouse.ReceiptMergeResponse;
import com.fisco.app.dto.warehouse.MergeApprovalRequest;
import com.fisco.app.dto.warehouse.UpdateReceiptRequest;
import com.fisco.app.dto.warehouse.WarehouseReceiptStatisticsDTO;
import com.fisco.app.entity.warehouse.ElectronicWarehouseReceipt;
import com.fisco.app.entity.warehouse.ReceiptCancelApplication;
import com.fisco.app.entity.warehouse.ReceiptMergeApplication;
import com.fisco.app.entity.warehouse.ReceiptChangeHistory;
import com.fisco.app.repository.warehouse.ElectronicWarehouseReceiptRepository;
import com.fisco.app.service.warehouse.ElectronicWarehouseReceiptService;
import com.fisco.app.service.warehouse.WarehouseReceiptStatisticsService;
import com.fisco.app.security.UserAuthentication;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;

/**
 * 电子仓单Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/ewr")
@Api(tags = "电子仓单管理")
public class ElectronicWarehouseReceiptController {

    @Autowired
    private ElectronicWarehouseReceiptService receiptService;

    @Autowired
    private ElectronicWarehouseReceiptRepository repository;

    @Autowired
    private WarehouseReceiptStatisticsService statisticsService;

    /**
     * 创建仓单
     */
    @PostMapping("/create")
    @ApiOperation(value = "创建电子仓单", notes = "创建新的电子仓单")
    @ApiResponses({
            @ApiResponse(code = 200, message = "仓单创建成功", response = ElectronicWarehouseReceiptResponse.class),
            @ApiResponse(code = 400, message = "请求参数错误"),
            @ApiResponse(code = 403, message = "无权限操作"),
            @ApiResponse(code = 409, message = "仓单编号已存在")
    })
    public ResponseEntity<ElectronicWarehouseReceiptResponse> createReceipt(
            @Valid @RequestBody ElectronicWarehouseReceiptCreateRequest request) {
        log.info("收到创建仓单请求, 仓单编号: {}", request.getReceiptNo());
        ElectronicWarehouseReceiptResponse response = receiptService.createReceipt(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 更新仓单
     */
    @PutMapping("/update/{id}")
    @ApiOperation(value = "更新电子仓单", notes = "更新电子仓单信息（仅草稿或正常状态可更新）")
    @ApiResponses({
            @ApiResponse(code = 200, message = "仓单更新成功", response = ElectronicWarehouseReceiptResponse.class),
            @ApiResponse(code = 400, message = "请求参数错误或仓单状态不允许更新"),
            @ApiResponse(code = 403, message = "无权限操作"),
            @ApiResponse(code = 404, message = "仓单不存在")
    })
    public ResponseEntity<ElectronicWarehouseReceiptResponse> updateReceipt(
            @PathVariable @NonNull String id,
            @Valid @RequestBody ElectronicWarehouseReceiptUpdateRequest request) {
        log.info("收到更新仓单请求, ID: {}", id);
        ElectronicWarehouseReceiptResponse response = receiptService.updateReceipt(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 根据ID查询仓单
     */
    @GetMapping("/{id}")
    @ApiOperation(value = "查询仓单详情", notes = "根据ID查询仓单详细信息")
    @ApiResponses({
            @ApiResponse(code = 200, message = "查询成功", response = ElectronicWarehouseReceiptResponse.class),
            @ApiResponse(code = 404, message = "仓单不存在")
    })
    public ResponseEntity<ElectronicWarehouseReceiptResponse> getReceiptById(
            @ApiParam(value = "仓单ID", required = true) @PathVariable @NonNull String id) {
        log.info("查询仓单, ID: {}", id);
        ElectronicWarehouseReceiptResponse response = receiptService.getReceiptById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 根据仓单编号查询
     */
    @GetMapping("/by-no/{receiptNo}")
    @ApiOperation(value = "根据编号查询仓单", notes = "根据仓单编号查询")
    public ResponseEntity<ElectronicWarehouseReceiptResponse> getReceiptByNo(
            @ApiParam(value = "仓单编号", required = true) @PathVariable String receiptNo) {
        log.info("查询仓单, 编号: {}", receiptNo);
        ElectronicWarehouseReceiptResponse response = receiptService.getReceiptByNo(receiptNo);
        return ResponseEntity.ok(response);
    }

    /**
     * 分页查询仓单
     */
    @PostMapping("/query")
    @ApiOperation(value = "分页查询仓单", notes = "支持多条件查询和分页")
    public ResponseEntity<Page<ElectronicWarehouseReceiptResponse>> queryReceipts(
            @Valid @RequestBody ElectronicWarehouseReceiptQueryRequest request) {
        log.info("分页查询仓单, page: {}, size: {}", request.getPage(), request.getSize());
        Page<ElectronicWarehouseReceiptResponse> page = receiptService.queryReceipts(request);
        return ResponseEntity.ok(page);
    }

    /**
     * 查询货主的仓单列表
     */
    @GetMapping("/by-owner/{ownerId}")
    @ApiOperation(value = "查询货主的仓单", notes = "查询指定货主企业的所有仓单")
    public ResponseEntity<List<ElectronicWarehouseReceiptResponse>> getReceiptsByOwner(
            @ApiParam(value = "货主企业ID", required = true) @PathVariable String ownerId) {
        log.info("查询货主仓单, 货主ID: {}", ownerId);
        List<ElectronicWarehouseReceiptResponse> responses = receiptService.getReceiptsByOwner(ownerId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 查询持单人的仓单列表
     */
    @GetMapping("/by-holder/{holderAddress}")
    @ApiOperation(value = "查询持单人的仓单", notes = "查询指定持单地址的所有仓单")
    public ResponseEntity<List<ElectronicWarehouseReceiptResponse>> getReceiptsByHolder(
            @ApiParam(value = "持单人地址", required = true) @PathVariable String holderAddress) {
        log.info("查询持单人仓单, 地址: {}", holderAddress);
        List<ElectronicWarehouseReceiptResponse> responses = receiptService.getReceiptsByHolder(holderAddress);
        return ResponseEntity.ok(responses);
    }

    /**
     * 查询仓储企业的仓单列表
     */
    @GetMapping("/by-warehouse/{warehouseId}")
    @ApiOperation(value = "查询仓储企业的仓单", notes = "查询指定仓储企业的所有仓单")
    public ResponseEntity<List<ElectronicWarehouseReceiptResponse>> getReceiptsByWarehouse(
            @ApiParam(value = "仓储企业ID", required = true) @PathVariable String warehouseId) {
        log.info("查询仓储企业仓单, 仓储ID: {}", warehouseId);
        List<ElectronicWarehouseReceiptResponse> responses = receiptService.getReceiptsByWarehouse(warehouseId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 查询即将过期的仓单（7天内）
     */
    @GetMapping("/expiring")
    @ApiOperation(value = "查询即将过期的仓单", notes = "查询7天内将要过期的仓单")
    public ResponseEntity<List<ElectronicWarehouseReceiptResponse>> getExpiringReceipts() {
        log.info("查询即将过期的仓单");
        List<ElectronicWarehouseReceiptResponse> responses = receiptService.getExpiringReceipts();
        return ResponseEntity.ok(responses);
    }

    /**
     * 查询已过期的仓单
     */
    @GetMapping("/expired")
    @ApiOperation(value = "查询已过期的仓单", notes = "查询已经过期但未处理的仓单")
    public ResponseEntity<List<ElectronicWarehouseReceiptResponse>> getExpiredReceipts() {
        log.info("查询已过期的仓单");
        List<ElectronicWarehouseReceiptResponse> responses = receiptService.getExpiredReceipts();
        return ResponseEntity.ok(responses);
    }

    /**
     * 更新仓单状态
     */
    @PutMapping("/status/{id}")
    @ApiOperation(value = "更新仓单状态", notes = "更新仓单状态（如：质押、冻结、解冻等）")
    public ResponseEntity<Void> updateReceiptStatus(
            @PathVariable @NonNull String id,
            @RequestParam String status) {
        log.info("更新仓单状态, ID: {}, 状态: {}", id, status);
        ElectronicWarehouseReceipt.ReceiptStatus receiptStatus =
                ElectronicWarehouseReceipt.ReceiptStatus.valueOf(status);
        receiptService.updateReceiptStatus(id, receiptStatus);
        return ResponseEntity.ok().build();
    }

    /**
     * 更新区块链上链状态
     */
    @PutMapping("/blockchain/{id}")
    @ApiOperation(value = "更新区块链状态", notes = "更新仓单的区块链上链状态")
    public ResponseEntity<Void> updateBlockchainStatus(
            @PathVariable @NonNull String id,
            @RequestParam String status,
            @RequestParam(required = false) String txHash,
            @RequestParam(required = false) Long blockNumber) {
        log.info("更新区块链状态, ID: {}, 状态: {}, txHash: {}", id, status, txHash);
        ElectronicWarehouseReceipt.BlockchainStatus blockchainStatus =
                ElectronicWarehouseReceipt.BlockchainStatus.valueOf(status);
        receiptService.updateBlockchainStatus(id, blockchainStatus, txHash, blockNumber);
        return ResponseEntity.ok().build();
    }

    /**
     * 删除仓单（软删除）
     */
    @DeleteMapping("/{id}")
    @ApiOperation(value = "删除仓单", notes = "软删除仓单（仅草稿状态可删除）")
    public ResponseEntity<Void> deleteReceipt(
            @ApiParam(value = "仓单ID", required = true) @PathVariable @NonNull String id) {
        log.info("删除仓单, ID: {}", id);
        receiptService.deleteReceipt(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 统计货主的仓单数量
     */
    @GetMapping("/count/owner/{ownerId}")
    @ApiOperation(value = "统计货主仓单数量", notes = "统计指定货主的仓单总数")
    public ResponseEntity<Long> countByOwner(
            @ApiParam(value = "货主企业ID", required = true) @PathVariable String ownerId) {
        Long count = receiptService.countByOwner(ownerId);
        return ResponseEntity.ok(count);
    }

    /**
     * 统计仓储企业的仓单数量
     */
    @GetMapping("/count/warehouse/{warehouseId}")
    @ApiOperation(value = "统计仓储企业仓单数量", notes = "统计指定仓储企业的仓单总数")
    public ResponseEntity<Long> countByWarehouse(
            @ApiParam(value = "仓储企业ID", required = true) @PathVariable String warehouseId) {
        Long count = receiptService.countByWarehouse(warehouseId);
        return ResponseEntity.ok(count);
    }

    /**
     * 仓储方审核仓单入库
     */
    @PostMapping("/approve")
    @ApiOperation(value = "审核仓单入库", notes = "仓储方审核仓单，审核通过后状态变为NORMAL（正常入库）。" +
            "只有草稿状态的仓单可以审核。审核拒绝时仓单保持草稿状态，货主可修改后重新提交。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "审核成功", response = ReceiptApprovalResponse.class),
            @ApiResponse(code = 400, message = "请求参数错误或仓单状态不允许审核"),
            @ApiResponse(code = 403, message = "无权限操作"),
            @ApiResponse(code = 404, message = "仓单不存在")
    })
    public ResponseEntity<ReceiptApprovalResponse> approveReceipt(
            @Valid @RequestBody ReceiptApprovalRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Name", required = false) String userName) {
        log.info("收到仓单审核请求, 仓单ID: {}, 审核结果: {}", request.getReceiptId(), request.getApprovalResult());

        // 实际项目中应该从SecurityContext获取当前用户信息
        String approverId = userId != null ? userId : "system-user";
        String approverName = userName != null ? userName : "系统用户";

        ReceiptApprovalResponse response = receiptService.approveReceipt(request, approverId, approverName);
        return ResponseEntity.ok(response);
    }

    /**
     * 查询待审核的仓单
     */
    @GetMapping("/pending/{warehouseId}")
    @ApiOperation(value = "查询待审核仓单", notes = "查询指定仓储企业的待审核仓单列表（草稿状态）")
    public ResponseEntity<List<ElectronicWarehouseReceiptResponse>> getPendingReceipts(
            @ApiParam(value = "仓储企业ID", required = true) @PathVariable String warehouseId) {
        log.info("查询待审核仓单, 仓储企业: {}", warehouseId);
        List<ElectronicWarehouseReceiptResponse> responses = receiptService.getPendingReceipts(warehouseId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 更新实际提货时间
     */
    @PutMapping("/delivery/{id}")
    @ApiOperation(value = "更新实际提货时间", notes = "记录货物实际提货时间，状态自动变为已提货。" +
            "只有正常、转让、质押状态的仓单可以提货。草稿、冻结、过期、取消状态的仓单无法提货。" +
            "提货时间不能早于入库时间。")
    public ResponseEntity<ElectronicWarehouseReceiptResponse> updateActualDeliveryDate(
            @ApiParam(value = "仓单ID", required = true) @PathVariable @NonNull String id,
            @Valid @RequestBody DeliveryUpdateRequest request) {
        log.info("更新实际提货时间, ID: {}, 提货时间: {}", id, request.getActualDeliveryDate());

        ElectronicWarehouseReceiptResponse response = receiptService.updateActualDeliveryDate(id, request);

        return ResponseEntity.ok(response);
    }

    /**
     * 重试仓单上链
     */
    @PostMapping("/retry-blockchain/{id}")
    @ApiOperation(value = "重试仓单上链", notes = "重试将上链失败的仓单上传到区块链。只有状态为ONCHAIN_FAILED的仓单可以重试。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "重试成功", response = Map.class),
            @ApiResponse(code = 400, message = "仓单状态不允许重试"),
            @ApiResponse(code = 404, message = "仓单不存在"),
            @ApiResponse(code = 500, message = "上链失败")
    })
    public ResponseEntity<Map<String, Object>> retryReceiptBlockchain(
            @ApiParam(value = "仓单ID", required = true) @PathVariable @NonNull String id) {
        log.info("重试仓单上链, ID: {}", id);

        String txHash = receiptService.retryReceiptOnChain(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "仓单上链成功，状态已变更为NORMAL");
        response.put("txHash", txHash);
        response.put("receiptId", id);

        return ResponseEntity.ok(response);
    }

    /**
     * 放弃重试上链，回滚到草稿状态
     */
    @PostMapping("/rollback-to-draft/{id}")
    @ApiOperation(value = "回滚到草稿", notes = "放弃重试上链，将上链失败的仓单回滚到草稿状态。" +
            "只有状态为ONCHAIN_FAILED的仓单可以回滚。回滚后货主可以修改仓单并重新提交审核。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "回滚成功", response = ElectronicWarehouseReceiptResponse.class),
            @ApiResponse(code = 400, message = "仓单状态不允许回滚或已进行其他操作"),
            @ApiResponse(code = 404, message = "仓单不存在")
    })
    public ResponseEntity<ElectronicWarehouseReceiptResponse> rollbackToDraft(
            @ApiParam(value = "仓单ID", required = true) @PathVariable @NonNull String id,
            @RequestBody(required = false) Map<String, String> requestBody) {
        log.info("回滚仓单到草稿状态, ID: {}", id);

        String reason = requestBody != null ? requestBody.get("reason") : null;
        ElectronicWarehouseReceiptResponse response = receiptService.rollbackToDraft(id, reason);

        return ResponseEntity.ok(response);
    }

    /**
     * 查询上链失败的仓单列表
     */
    @GetMapping("/onchain-failed")
    @ApiOperation(value = "查询上链失败的仓单", notes = "查询所有上链失败的仓单列表。货主可用于查看需要处理（重试或回滚）的仓单。")
    public ResponseEntity<List<ElectronicWarehouseReceiptResponse>> getOnChainFailedReceipts(
            @ApiParam(value = "货主企业ID（可选，不传则查询所有）") @RequestParam(required = false) String ownerId) {
        log.info("查询上链失败的仓单, 货主: {}", ownerId);

        List<ElectronicWarehouseReceiptResponse> responses = receiptService.getOnChainFailedReceipts(ownerId);

        return ResponseEntity.ok(responses);
    }

    /**
     * 查询待上链的仓单列表
     */
    @GetMapping("/pending-onchain")
    @ApiOperation(value = "查询待上链的仓单", notes = "查询正在上链中的仓单列表（PENDING_ONCHAIN状态）")
    public ResponseEntity<List<ElectronicWarehouseReceiptResponse>> getPendingOnChainReceipts() {
        log.info("查询待上链的仓单");

        List<ElectronicWarehouseReceipt> receipts = repository.findByReceiptStatus(
                ElectronicWarehouseReceipt.ReceiptStatus.PENDING_ONCHAIN
        );

        List<ElectronicWarehouseReceiptResponse> responses = receipts.stream()
                .map(ElectronicWarehouseReceiptResponse::fromEntity)
                .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    // ==================== 冻结/解冻接口 ====================

    /**
     * 冻结仓单
     */
    @PostMapping("/freeze")
    @ApiOperation(value = "冻结仓单", notes = "冻结指定仓单，冻结后仓单无法进行提货、转让等操作。" +
            "只有正常、已质押、已转让状态的仓单可以冻结。" +
            "支持仓储方、资金方、平台方、法院等不同操作方进行冻结。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "仓单冻结成功", response = ReceiptFreezeResponse.class),
            @ApiResponse(code = 400, message = "请求参数错误或仓单状态不允许冻结"),
            @ApiResponse(code = 403, message = "无权限操作"),
            @ApiResponse(code = 404, message = "仓单不存在")
    })
    public ResponseEntity<ReceiptFreezeResponse> freezeReceipt(
            @Valid @RequestBody com.fisco.app.dto.warehouse.ReceiptFreezeRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Name", required = false) String userName) {
        log.info("收到冻结仓单请求, 仓单ID: {}, 操作方类型: {}", request.getReceiptId(), request.getOperatorType());

        // 实际项目中应该从SecurityContext获取当前用户信息
        String operatorId = userId != null ? userId : "system-user";
        String operatorName = userName != null ? userName : "系统用户";

        ReceiptFreezeResponse response = receiptService.freezeReceipt(request, operatorId, operatorName);
        return ResponseEntity.ok(response);
    }

    /**
     * 解冻仓单
     */
    @PostMapping("/unfreeze")
    @ApiOperation(value = "解冻仓单", notes = "解冻已冻结的仓单，恢复正常操作。" +
            "只有冻结状态的仓单可以解冻。" +
            "解冻后可以恢复到正常、已质押或已转让状态。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "仓单解冻成功", response = ReceiptUnfreezeResponse.class),
            @ApiResponse(code = 400, message = "请求参数错误或仓单状态不允许解冻"),
            @ApiResponse(code = 403, message = "无权限操作"),
            @ApiResponse(code = 404, message = "仓单不存在")
    })
    public ResponseEntity<ReceiptUnfreezeResponse> unfreezeReceipt(
            @Valid @RequestBody com.fisco.app.dto.warehouse.ReceiptUnfreezeRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Name", required = false) String userName) {
        log.info("收到解冻仓单请求, 仓单ID: {}, 目标状态: {}", request.getReceiptId(), request.getTargetStatus());

        // 实际项目中应该从SecurityContext获取当前用户信息
        String operatorId = userId != null ? userId : "system-user";
        String operatorName = userName != null ? userName : "系统用户";

        ReceiptUnfreezeResponse response = receiptService.unfreezeReceipt(request, operatorId, operatorName);
        return ResponseEntity.ok(response);
    }

    // ==================== 冻结申请-审核接口 ====================

    /**
     * 仓储方提交冻结申请
     */
    @PostMapping("/freeze-application/submit")
    @ApiOperation(value = "提交冻结申请", notes = "仓储方提交仓单冻结申请，由管理员审核。" +
            "只有正常、已质押、已转让状态的仓单可以申请冻结。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "申请提交成功", response = FreezeApplicationResponse.class),
            @ApiResponse(code = 400, message = "请求参数错误或仓单状态不允许申请冻结"),
            @ApiResponse(code = 403, message = "无权限操作"),
            @ApiResponse(code = 404, message = "仓单不存在"),
            @ApiResponse(code = 409, message = "已有待审核的申请，请勿重复提交")
    })
    public ResponseEntity<FreezeApplicationResponse> submitFreezeApplication(
            @Valid @RequestBody FreezeApplicationSubmitRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Name", required = false) String userName) {
        log.info("收到冻结申请提交请求, 仓单ID: {}, 仓储企业: {}", request.getReceiptId(), request.getWarehouseId());

        // 实际项目中应该从SecurityContext获取当前用户信息
        String applicantId = userId != null ? userId : "warehouse-user";
        String applicantName = userName != null ? userName : "仓储用户";

        FreezeApplicationResponse response = receiptService.submitFreezeApplication(request, applicantId, applicantName);
        return ResponseEntity.ok(response);
    }

    /**
     * 管理员审核冻结申请
     */
    @PostMapping("/freeze-application/review")
    @ApiOperation(value = "审核冻结申请", notes = "管理员审核仓单冻结申请。" +
            "审核通过后，仓单将被冻结并上链。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "审核成功", response = FreezeApplicationReviewResponse.class),
            @ApiResponse(code = 400, message = "请求参数错误或申请状态不允许审核"),
            @ApiResponse(code = 403, message = "无权限操作（需要管理员权限）"),
            @ApiResponse(code = 404, message = "冻结申请不存在")
    })
    public ResponseEntity<FreezeApplicationReviewResponse> reviewFreezeApplication(
            @Valid @RequestBody FreezeApplicationReviewRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Name", required = false) String userName) {
        log.info("收到冻结申请审核请求, 申请ID: {}, 审核结果: {}", request.getApplicationId(), request.getReviewResult());

        // 实际项目中应该从SecurityContext获取当前用户信息
        String reviewerId = userId != null ? userId : "admin-user";
        String reviewerName = userName != null ? userName : "管理员";

        FreezeApplicationReviewResponse response = receiptService.reviewFreezeApplication(request, reviewerId, reviewerName);
        return ResponseEntity.ok(response);
    }

    /**
     * 查询待审核的冻结申请列表（管理员使用）
     */
    @GetMapping("/freeze-application/pending")
    @ApiOperation(value = "查询待审核的冻结申请", notes = "查询所有待审核的冻结申请列表。" +
            "管理员使用，用于查看需要审核的冻结申请。")
    public ResponseEntity<List<FreezeApplicationResponse>> getPendingFreezeApplications() {
        log.info("查询待审核的冻结申请");

        List<FreezeApplicationResponse> responses = receiptService.getPendingFreezeApplications();

        return ResponseEntity.ok(responses);
    }

    /**
     * 查询指定仓储企业的冻结申请列表
     */
    @GetMapping("/freeze-application/by-warehouse/{warehouseId}")
    @ApiOperation(value = "查询仓储企业的冻结申请", notes = "查询指定仓储企业的冻结申请列表。" +
            "可以按状态筛选，不传状态则查询待审核的申请。")
    public ResponseEntity<List<FreezeApplicationResponse>> getFreezeApplicationsByWarehouse(
            @ApiParam(value = "仓储企业ID", required = true) @PathVariable String warehouseId,
            @ApiParam(value = "申请状态（可选）") @RequestParam(required = false) String status) {
        log.info("查询仓储企业的冻结申请, 仓储企业: {}, 状态: {}", warehouseId, status);

        List<FreezeApplicationResponse> responses = receiptService.getFreezeApplicationsByWarehouse(warehouseId, status);

        return ResponseEntity.ok(responses);
    }

    /**
     * 查询已冻结的仓单列表
     */
    @GetMapping("/frozen")
    @ApiOperation(value = "查询已冻结的仓单", notes = "查询已冻结状态的仓单列表。" +
            "可以按企业ID筛选，不传则查询所有已冻结仓单。")
    public ResponseEntity<List<ElectronicWarehouseReceiptResponse>> getFrozenReceipts(
            @ApiParam(value = "企业ID（可选，不传则查询所有）") @RequestParam(required = false) String enterpriseId) {
        log.info("查询已冻结的仓单, 企业: {}", enterpriseId);

        List<ElectronicWarehouseReceiptResponse> responses = receiptService.getFrozenReceipts(enterpriseId);

        return ResponseEntity.ok(responses);
    }

    // ==================== 仓单拆分相关接口 ====================

    /**
     * 提交仓单拆分申请
     */
    @PostMapping("/split/apply")
    @ApiOperation(value = "提交仓单拆分申请", notes = "货主企业提交仓单拆分申请。" +
            "只有正常状态(NORMAL)的仓单可以申请拆分。拆分数量必须在2-10个之间。" +
            "所有子仓单的货物名称、计量单位、单价必须与父仓单一致。" +
            "所有子仓单的数量之和必须等于父仓单数量。" +
            "所有子仓单的总价值之和必须等于父仓单总价值。" +
            "存储位置不能重复。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "拆分申请提交成功", response = SplitApplicationResponse.class),
            @ApiResponse(code = 400, message = "请求参数错误或仓单状态不允许拆分"),
            @ApiResponse(code = 403, message = "无权限操作（只有货主企业可以申请拆分）"),
            @ApiResponse(code = 404, message = "仓单不存在"),
            @ApiResponse(code = 409, message = "该仓单已有待审核的拆分申请")
    })
    public ResponseEntity<SplitApplicationResponse> submitSplitApplication(
            @Valid @RequestBody SplitApplicationRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Name", required = false) String userName) {
        log.info("收到仓单拆分申请, 父仓单ID: {}, 拆分数量: {}",
            request.getParentReceiptId(), request.getSplits().size());

        // 实际项目中应该从SecurityContext获取当前用户信息
        String applicantId = userId != null ? userId : "user-001";
        String applicantName = userName != null ? userName : "货主企业";

        SplitApplicationResponse response = receiptService.submitSplitApplication(
            request, applicantId, applicantName);
        return ResponseEntity.ok(response);
    }

    /**
     * 审核仓单拆分申请
     */
    @PostMapping("/split/approve")
    @ApiOperation(value = "审核仓单拆分申请", notes = "管理员或仓储企业审核拆分申请。" +
            "审核通过后自动执行拆分操作，生成子仓单并上链。" +
            "父仓单状态变为SPLIT（已拆分），子仓单状态为NORMAL。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "审核完成", response = SplitApprovalResponse.class),
            @ApiResponse(code = 400, message = "申请状态不允许审核或仓单状态已变更"),
            @ApiResponse(code = 403, message = "无权限审核（只有管理员或仓储企业可以审核）"),
            @ApiResponse(code = 404, message = "拆分申请不存在")
    })
    public ResponseEntity<SplitApprovalResponse> approveSplitApplication(
            @Valid @RequestBody SplitApprovalRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Name", required = false) String userName) {
        log.info("收到仓单拆分审核请求, 申请ID: {}, 审核结果: {}",
            request.getApplicationId(), request.getApprovalResult());

        // 实际项目中应该从SecurityContext获取当前用户信息
        String reviewerId = userId != null ? userId : "admin-user";
        String reviewerName = userName != null ? userName : "管理员";

        SplitApprovalResponse response = receiptService.approveSplitApplication(
            request, reviewerId, reviewerName);
        return ResponseEntity.ok(response);
    }

    /**
     * 查询子仓单列表
     */
    @GetMapping("/split/children/{parentReceiptId}")
    @ApiOperation(value = "查询子仓单列表", notes = "根据父仓单ID查询所有子仓单列表")
    @ApiResponses({
            @ApiResponse(code = 200, message = "查询成功", response = ElectronicWarehouseReceiptResponse.class, responseContainer = "List"),
            @ApiResponse(code = 404, message = "父仓单不存在")
    })
    public ResponseEntity<List<ElectronicWarehouseReceiptResponse>> getChildReceipts(
            @ApiParam(value = "父仓单ID", required = true) @PathVariable String parentReceiptId) {
        log.info("查询子仓单列表, 父仓单ID: {}", parentReceiptId);

        List<ElectronicWarehouseReceiptResponse> responses = receiptService.getChildReceipts(parentReceiptId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 查询父仓单
     */
    @GetMapping("/split/parent/{childReceiptId}")
    @ApiOperation(value = "查询父仓单", notes = "根据子仓单ID查询父仓单信息")
    @ApiResponses({
            @ApiResponse(code = 200, message = "查询成功", response = ElectronicWarehouseReceiptResponse.class),
            @ApiResponse(code = 400, message = "该仓单不是拆分生成的子仓单"),
            @ApiResponse(code = 404, message = "仓单不存在")
    })
    public ResponseEntity<ElectronicWarehouseReceiptResponse> getParentReceipt(
            @ApiParam(value = "子仓单ID", required = true) @PathVariable String childReceiptId) {
        log.info("查询父仓单, 子仓单ID: {}", childReceiptId);

        ElectronicWarehouseReceiptResponse response = receiptService.getParentReceipt(childReceiptId);
        return ResponseEntity.ok(response);
    }

    // ==================== 仓单作废相关接口 ====================

    /**
     * 提交仓单作废申请
     */
    @PostMapping("/cancel/apply")
    @ApiOperation(value = "提交仓单作废申请",
        notes = "货主企业提交仓单作废申请。" +
                "权限要求：当前持单人（货主企业）。" +
                "只有正常状态(NORMAL)或上链失败状态(ONCHAIN_FAILED)的仓单可以申请作废。" +
                "已提货、已质押、已冻结、已拆分的仓单不能作废。" +
                "作废后的仓单无法恢复。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "作废申请提交成功", response = CancelApplicationResponse.class),
            @ApiResponse(code = 400, message = "请求参数错误或仓单状态不允许作废"),
            @ApiResponse(code = 403, message = "无权限操作（只有当前持单人/货主企业可以申请作废）"),
            @ApiResponse(code = 404, message = "仓单不存在"),
            @ApiResponse(code = 409, message = "该仓单已有待审核的作废申请")
    })
    public ResponseEntity<CancelApplicationResponse> submitCancelApplication(
            @Valid @RequestBody CancelApplicationRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Name", required = false) String userName) {
        log.info("收到仓单作废申请, 仓单ID: {}, 作废类型: {}",
            request.getReceiptId(), request.getCancelType());

        // 实际项目中应该从SecurityContext获取当前用户信息
        String applicantId = userId != null ? userId : "user-001";
        String applicantName = userName != null ? userName : "货主企业";

        CancelApplicationResponse response = receiptService.submitCancelApplication(
            request, applicantId, applicantName);
        return ResponseEntity.ok(response);
    }

    /**
     * 审核仓单作废申请
     */
    @PostMapping("/cancel/approve")
    @ApiOperation(value = "审核仓单作废申请",
        notes = "管理员或仓储企业审核作废申请。" +
                "权限要求：系统管理员或关联的仓储企业。" +
                "审核通过后仓单状态变为CANCELLED（已作废），无法恢复。" +
                "审核拒绝后仓单状态恢复为NORMAL。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "审核完成", response = CancelApprovalResponse.class),
            @ApiResponse(code = 400, message = "申请状态不允许审核或仓单状态已变更"),
            @ApiResponse(code = 403, message = "无权限审核（只有系统管理员或关联的仓储企业可以审核）"),
            @ApiResponse(code = 404, message = "作废申请不存在")
    })
    public ResponseEntity<CancelApprovalResponse> approveCancelApplication(
            @Valid @RequestBody CancelApprovalRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Name", required = false) String userName) {
        log.info("收到仓单作废审核请求, 申请ID: {}, 审核结果: {}",
            request.getApplicationId(), request.getApprovalResult());

        // 实际项目中应该从SecurityContext获取当前用户信息
        String reviewerId = userId != null ? userId : "admin-user";
        String reviewerName = userName != null ? userName : "管理员";

        CancelApprovalResponse response = receiptService.approveCancelApplication(
            request, reviewerId, reviewerName);
        return ResponseEntity.ok(response);
    }

    /**
     * 查询待审核的作废申请
     */
    @GetMapping("/cancel/pending")
    @ApiOperation(value = "查询待审核的作废申请",
        notes = "查询待审核的作废申请列表。" +
                "权限要求：系统管理员可查询所有待审核申请；" +
                "仓储企业只能查询自己仓单的待审核申请。" +
                "用于管理员和仓储企业查看需要审核的作废申请。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "查询成功", response = ReceiptCancelApplication.class, responseContainer = "List"),
            @ApiResponse(code = 403, message = "无权限查询")
    })
    public ResponseEntity<java.util.List<ReceiptCancelApplication>> getPendingCancelApplications() {
        log.info("查询待审核的作废申请");

        // 从SecurityContext获取当前用户认证信息
        org.springframework.security.core.Authentication auth =
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        java.util.List<ReceiptCancelApplication> applications =
            receiptService.getPendingCancelApplications(auth);
        return ResponseEntity.ok(applications);
    }

    /**
     * 查询已作废的仓单
     */
    @GetMapping("/cancelled")
    @ApiOperation(value = "查询已作废的仓单",
        notes = "查询已作废状态的仓单列表。" +
                "权限要求：默认只返回当前用户企业的已作废仓单；" +
                "系统管理员可以通过enterpriseId参数查询指定企业的已作废仓单。" +
                "可以按企业ID筛选，不传则查询当前企业的已作废仓单。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "查询成功", response = ElectronicWarehouseReceiptResponse.class, responseContainer = "List"),
            @ApiResponse(code = 403, message = "无权限查询该企业的已作废仓单")
    })
    public ResponseEntity<List<ElectronicWarehouseReceiptResponse>> getCancelledReceipts(
            @ApiParam(value = "企业ID（可选，不传则查询当前企业）") @RequestParam(required = false) String enterpriseId) {
        log.info("查询已作废的仓单, 企业: {}", enterpriseId);

        // 从SecurityContext获取当前用户认证信息
        org.springframework.security.core.Authentication auth =
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        List<ElectronicWarehouseReceiptResponse> responses =
            receiptService.getCancelledReceipts(enterpriseId, auth);
        return ResponseEntity.ok(responses);
    }

    // ==================== 仓单合并功能 ====================

    /**
     * 提交仓单合并申请
     */
    @PostMapping("/merge/apply")
    @ApiOperation(value = "提交仓单合并申请",
        notes = "将多个仓单合并为一个仓单。" +
                "业务规则：" +
                "1. 合并数量：2-10个仓单；" +
                "2. 状态要求：所有源仓单必须为NORMAL状态；" +
                "3. 所有权要求：所有源仓单必须属于同一货主企业；" +
                "4. 合并类型：QUANTITY-数量合并（货物名称、单位、单价必须相同），" +
                "            VALUE-价值合并（不同货物按价值比例合并），" +
                "            FULL-完全合并；" +
                "5. 流程：提交申请 → 审核 → 执行合并。" +
                "权限要求：货主企业可以提交合并申请。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "合并申请提交成功", response = ReceiptMergeResponse.class),
            @ApiResponse(code = 400, message = "请求参数错误"),
            @ApiResponse(code = 403, message = "无权限操作"),
            @ApiResponse(code = 404, message = "仓单不存在")
    })
    public ResponseEntity<ReceiptMergeResponse> submitMergeApplication(
            @Valid @RequestBody MergeReceiptsRequest request) {

        log.info("收到仓单合并申请: receiptCount={}", request.getReceiptIds().size());

        // 从SecurityContext获取当前用户认证信息
        org.springframework.security.core.Authentication auth =
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        if (!(auth instanceof UserAuthentication)) {
            throw new RuntimeException("无效的认证信息");
        }

        UserAuthentication userAuth = (UserAuthentication) auth;

        ReceiptMergeResponse response = receiptService.submitMergeApplication(
            request,
            userAuth.getUserId(),
            userAuth.getUsername()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 审核仓单合并申请
     */
    @PostMapping("/merge/approve")
    @ApiOperation(value = "审核仓单合并申请",
        notes = "审核仓单合并申请，通过后自动执行合并操作。" +
                "审核通过：创建合并仓单，源仓单状态变更为MERGED；" +
                "审核拒绝：源仓单保持原状态，申请状态变更为REJECTED。" +
                "权限要求：系统管理员或仓储企业可以审核合并申请。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "审核成功", response = ReceiptMergeResponse.class),
            @ApiResponse(code = 400, message = "请求参数错误"),
            @ApiResponse(code = 403, message = "无权限审核"),
            @ApiResponse(code = 404, message = "合并申请不存在")
    })
    public ResponseEntity<ReceiptMergeResponse> approveMergeApplication(
            @Valid @RequestBody MergeApprovalRequest request) {

        log.info("收到仓单合并审核: applicationId={}, approved={}",
            request.getApplicationId(), request.getApproved());

        // 从SecurityContext获取当前用户认证信息
        org.springframework.security.core.Authentication auth =
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        if (!(auth instanceof UserAuthentication)) {
            throw new RuntimeException("无效的认证信息");
        }

        UserAuthentication userAuth = (UserAuthentication) auth;

        ReceiptMergeResponse response = receiptService.approveMergeApplication(
            request,
            userAuth.getUserId(),
            userAuth.getUsername()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 查询待审核的合并申请
     */
    @GetMapping("/merge/pending")
    @ApiOperation(value = "查询待审核的合并申请",
        notes = "查询待审核的仓单合并申请列表。" +
                "权限要求：系统管理员可查询所有待审核申请；" +
                "仓储企业只能查询自己仓单的待审核申请。" +
                "用于管理员和仓储企业查看需要审核的合并申请。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "查询成功", response = ReceiptMergeApplication.class, responseContainer = "List"),
            @ApiResponse(code = 403, message = "无权限查询")
    })
    public ResponseEntity<java.util.List<ReceiptMergeApplication>> getPendingMergeApplications() {
        log.info("查询待审核的合并申请");

        java.util.List<ReceiptMergeApplication> applications =
            receiptService.getPendingMergeApplications();
        return ResponseEntity.ok(applications);
    }

    // ==================== 仓单变更管理功能 ====================

    /**
     * 更新仓单（增强版：记录变更历史）
     */
    @PutMapping("/update-with-history/{id}")
    @ApiOperation(value = "更新仓单（记录变更历史）",
        notes = "更新仓单信息并记录完整的变更历史。" +
                "可更新字段：单价、市场价格、仓库位置、存储位置、有效期、备注。" +
                "业务规则：" +
                "1. 可变更状态：DRAFT、NORMAL；" +
                "2. 价格调整时自动重新计算总价值；" +
                "3. 有效期延长：新有效期必须晚于或等于当前有效期；" +
                "4. 所有变更都会记录到变更历史表（变更前后对比）；" +
                "5. 支持多种变更类型：价格调整、位置变更、有效期延长、信息更新等。" +
                "权限要求：货主企业或仓储企业可以变更。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "更新成功", response = ElectronicWarehouseReceiptResponse.class),
            @ApiResponse(code = 400, message = "请求参数错误"),
            @ApiResponse(code = 403, message = "无权限更新"),
            @ApiResponse(code = 404, message = "仓单不存在")
    })
    public ResponseEntity<ElectronicWarehouseReceiptResponse> updateReceiptWithHistory(
            @PathVariable @NonNull String id,
            @Valid @RequestBody UpdateReceiptRequest request) {

        log.info("收到仓单更新请求（记录变更历史）: id={}, changeType={}",
            id, request.getChangeType());

        // 从SecurityContext获取当前用户认证信息
        org.springframework.security.core.Authentication auth =
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        if (!(auth instanceof UserAuthentication)) {
            throw new RuntimeException("无效的认证信息");
        }

        UserAuthentication userAuth = (UserAuthentication) auth;

        ElectronicWarehouseReceiptResponse response = receiptService.updateReceiptWithHistory(
            id,
            request,
            userAuth.getUserId(),
            userAuth.getUsername()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 查询仓单变更历史
     */
    @GetMapping("/{id}/change-history")
    @ApiOperation(value = "查询仓单变更历史",
        notes = "查询指定仓单的所有变更历史记录，包括变更前后的值对比。" +
                "返回完整的变更历史列表，按时间倒序排列。" +
                "用于审计追踪和问题排查。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "查询成功", response = ReceiptChangeHistory.class, responseContainer = "List"),
            @ApiResponse(code = 404, message = "仓单不存在")
    })
    public ResponseEntity<java.util.List<ReceiptChangeHistory>> getReceiptChangeHistory(
            @PathVariable @NonNull String id) {

        log.info("查询仓单变更历史: receiptId={}", id);

        java.util.List<ReceiptChangeHistory> history =
            receiptService.getReceiptChangeHistory(id);
        return ResponseEntity.ok(history);
    }

    // ==================== 仓单统计功能 ====================

    /**
     * 查询仓单统计
     */
    @GetMapping("/statistics")
    @ApiOperation(value = "查询仓单统计",
        notes = "查询仓单的多维度统计数据，支持管理决策。" +
                "统计维度：" +
                "1. 基础统计：仓单总数、总价值、总数量；" +
                "2. 状态分布：各状态仓单的数量和价值占比；" +
                "3. 货物类型分布：按货物名称统计（数量、总价值、平均单价）；" +
                "4. 企业分布：按货主企业统计（Top 10）；" +
                "5. 风险统计：即将过期（7天内）、已过期、已冻结仓单数量和价值；" +
                "6. 操作统计：拆分、合并、作废申请数量。" +
                "参数说明：" +
                "- startTime: 统计开始时间（可选），不传则统计所有时间；" +
                "- endTime: 统计结束时间（可选），不传则统计所有时间。" +
                "权限要求：所有登录用户可以查询统计数据。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "查询成功", response = WarehouseReceiptStatisticsDTO.class)
    })
    public ResponseEntity<WarehouseReceiptStatisticsDTO> getReceiptStatistics(
            @RequestParam(required = false) java.time.LocalDateTime startTime,
            @RequestParam(required = false) java.time.LocalDateTime endTime) {

        log.info("查询仓单统计: startTime={}, endTime={}", startTime, endTime);

        WarehouseReceiptStatisticsDTO statistics =
            statisticsService.getReceiptStatistics(startTime, endTime);
        return ResponseEntity.ok(statistics);
    }
}
