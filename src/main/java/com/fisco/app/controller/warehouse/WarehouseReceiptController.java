package com.fisco.app.controller.warehouse;

import com.fisco.app.dto.warehouse.CreateWarehouseReceiptRequest;
import com.fisco.app.dto.warehouse.ReleaseReceiptRequest;
import com.fisco.app.dto.warehouse.ReleaseReceiptResponse;
import com.fisco.app.entity.pledge.ReleaseRecord;
import com.fisco.app.entity.warehouse.WarehouseReceipt;
import com.fisco.app.service.warehouse.WarehouseReceiptService;
import com.fisco.app.vo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.List;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仓单管理Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/warehouse-receipt")
@RequiredArgsConstructor
@Api(tags = "仓单管理")
public class WarehouseReceiptController {

    private final WarehouseReceiptService warehouseReceiptService;

    /**
     * 创建仓单
     * POST /api/warehouse-receipt
     */
    @PostMapping
    @ApiOperation(value = "创建仓单", notes = "货主创建仓单，使用CreateReceiptParams结构体封装参数，避免Solidity 16变量限制")
    public Result<WarehouseReceipt> createReceipt(
            @ApiParam(value = "创建仓单请求参数（结构体封装）", required = true) @Valid @RequestBody CreateWarehouseReceiptRequest request,
            Authentication authentication) {
        // 从认证上下文获取用户地址（由JWT过滤器设置）
        String ownerAddress = authentication.getName();
        log.info("创建仓单请求: 货主地址={}", ownerAddress);

        WarehouseReceipt saved = warehouseReceiptService.createReceipt(request, ownerAddress);
        return Result.success("仓单创建成功", saved);
    }

    /**
     * 获取仓单信息
     * GET /api/warehouse-receipt/{receiptId}
     */
    @GetMapping("/{receiptId}")
    @ApiOperation(value = "获取仓单详情", notes = "根据仓单ID查询详细信息")
    public Result<WarehouseReceipt> getReceipt(
            @ApiParam(value = "仓单ID", required = true) @PathVariable @NonNull String receiptId) {
        WarehouseReceipt receipt = warehouseReceiptService.getReceipt(receiptId);
        return Result.success(receipt);
    }

    /**
     * 释放仓单
     * POST /api/warehouse-receipt/{receiptId}/release
     */
    @PostMapping("/{receiptId}/release")
    @ApiOperation(value = "释放仓单", notes = "释放已质押的仓单")
    public Result<ReleaseReceiptResponse> releaseReceipt(
            @ApiParam(value = "仓单ID", required = true) @PathVariable @NonNull String receiptId,
            @ApiParam(value = "释放请求参数", required = true) @Valid @RequestBody ReleaseReceiptRequest request) {
        log.info("仓单释放请求: receiptId={}, releaseType={}", receiptId, request.getReleaseType());

        ReleaseReceiptResponse response = warehouseReceiptService.releaseReceipt(receiptId, request);
        return Result.success("仓单释放成功", response);
    }

    /**
     * 获取仓单释放历史
     * GET /api/warehouse-receipt/{receiptId}/releases
     */
    @GetMapping("/{receiptId}/releases")
    @ApiOperation(value = "获取仓单释放历史", notes = "查询仓单的所有释放记录")
    public Result<List<ReleaseRecord>> getReleaseHistory(
            @ApiParam(value = "仓单ID", required = true) @PathVariable @NonNull String receiptId) {
        log.info("查询仓单释放历史: receiptId={}", receiptId);

        List<ReleaseRecord> history = warehouseReceiptService.getReleaseHistory(receiptId);
        return Result.success("查询成功", history);
    }
}
