package com.fisco.app.Modules.Warehouse.Controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fisco.app.Common.Annotation.StockOrderOwnership;
import com.fisco.app.Common.Annotation.WarehousePermissionCheck;
import com.fisco.app.Common.Annotation.WarehouseReceiptOwnership;
import com.fisco.app.Common.Annotation.WarehouseStatusCheck;
import com.fisco.app.Common.Utils.CurrentUser;
import com.fisco.app.Common.Utils.Result;
import com.fisco.app.Modules.Warehouse.Entity.ReceiptEndorsement;
import com.fisco.app.Modules.Warehouse.Entity.ReceiptOperationLog;
import com.fisco.app.Modules.Warehouse.Entity.StockOrder;
import com.fisco.app.Modules.Warehouse.Entity.Warehouse;
import com.fisco.app.Modules.Warehouse.Entity.WarehouseReceipt;
import com.fisco.app.Modules.Warehouse.Service.WarehouseReceiptService;
import com.fisco.app.Modules.Warehouse.Service.WarehouseReceiptService.TraceInfo;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 仓单管理 Controller
 *
 * 提供仓单全生命周期管理的 REST API
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Api(tags = "仓单管理")
@RestController
@RequestMapping("/api/v1/warehouse")
public class WarehouseReceiptController {

    private static final Logger logger = LoggerFactory.getLogger(WarehouseReceiptController.class);

    @Autowired
    private WarehouseReceiptService warehouseReceiptService;

    // ==================== 入库单管理 ====================

    @ApiOperation("申请入库")
    @PostMapping("/stock-in/apply")
    public Result<Long> applyStockIn(@RequestBody StockInApplyRequest request) {
        try {
            // 参数校验
            if (request.getWarehouseId() == null) {
                return Result.error(400, "仓库ID不能为空");
            }
            if (request.getGoodsName() == null || request.getGoodsName().isEmpty()) {
                return Result.error(400, "货物名称不能为空");
            }
            if (request.getWeight() == null) {
                return Result.error(400, "货物重量不能为空");
            }
            if (request.getUnit() == null || request.getUnit().isEmpty()) {
                return Result.error(400, "计量单位不能为空");
            }

            // 仅从JWT获取用户信息，防止越权
            Long entId = CurrentUser.getEntId();
            Long userId = CurrentUser.getUserId();

            if (entId == null || userId == null) {
                return Result.error(401, "无法获取当前用户信息，请先登录");
            }

            Long stockOrderId = warehouseReceiptService.applyStockIn(
                    request.getWarehouseId(),
                    entId,
                    userId,
                    request.getGoodsName(),
                    request.getWeight(),
                    request.getUnit(),
                    request.getAttachmentUrl()
            );
            return Result.success(stockOrderId);
        } catch (Exception e) {
            logger.error("申请入库失败", e);
            return Result.error(500, e.getMessage());
        }
    }

    @ApiOperation("确认入库单（仓储方操作）")
    @WarehousePermissionCheck(allowedRoles = {9}, errorMessage = "无权限操作：仅仓储方可确认入库单")
    @PostMapping("/stock-in/{stockOrderId}/confirm")
    public Result<Boolean> confirmStockOrder(@PathVariable String stockOrderId) {
        try {
            StockOrder order = getStockOrderByIdOrStockNo(stockOrderId);
            if (order == null) {
                return Result.error(404, "入库单不存在");
            }
            boolean success = warehouseReceiptService.confirmStockOrder(order.getId());
            return Result.success(success);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("确认入库单失败", e);
            return Result.error(500, "确认入库单失败");
        }
    }

    @ApiOperation("取消入库单")
    @WarehousePermissionCheck(allowedRoles = {9}, errorMessage = "无权限操作：仅仓储方可取消入库单")
    @PostMapping("/stock-in/{stockOrderId}/cancel")
    public Result<Boolean> cancelStockOrder(@PathVariable String stockOrderId) {
        try {
            StockOrder order = getStockOrderByIdOrStockNo(stockOrderId);
            if (order == null) {
                return Result.error(404, "入库单不存在");
            }
            boolean success = warehouseReceiptService.cancelStockOrder(order.getId());
            return Result.success(success);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("取消入库单失败", e);
            return Result.error(500, "取消入库单失败");
        }
    }

    @ApiOperation("查询入库单")
    @StockOrderOwnership(paramName = "stockOrderId", errorMessage = "无权限操作：非入库单所属企业无权查询")
    @GetMapping("/stock-in/{stockOrderId}")
    public Result<StockOrder> getStockOrderById(@PathVariable String stockOrderId) {
        try {
            StockOrder stockOrder = getStockOrderByIdOrStockNo(stockOrderId);
            if (stockOrder == null) {
                return Result.error(404, "入库单不存在");
            }
            return Result.success(stockOrder);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @ApiOperation("查询企业入库单列表")
    @GetMapping("/stock-in/list")
    public Result<List<StockOrder>> getStockOrdersByEntId() {
        Long entId = CurrentUser.getEntId();
        if (entId == null) {
            return Result.error(401, "无法获取当前用户企业信息，请先登录");
        }
        List<StockOrder> list = warehouseReceiptService.getStockOrdersByEntId(entId);
        return Result.success(list);
    }

    // ==================== 仓单签发 ====================

    @ApiOperation("签发仓单（仓储方操作）")
    @WarehousePermissionCheck(allowedRoles = {9}, errorMessage = "无权限操作：仅仓储方可签发仓单")
    @PostMapping("/receipt/mint")
    public Result<Long> mintReceipt(@RequestBody MintReceiptRequest request) {
        try {
            // 参数校验
            if (request.getStockOrderId() == null) {
                return Result.error(400, "入库单ID不能为空");
            }

            // 仅从JWT获取用户信息，防止越权
            Long userId = CurrentUser.getUserId();

            if (userId == null) {
                return Result.error(401, "无法获取当前用户信息，请先登录");
            }

            Long receiptId = warehouseReceiptService.mintReceipt(
                    request.getStockOrderId(),
                    userId,
                    request.getOnChainId()
            );
            return Result.success(receiptId);
        } catch (Exception e) {
            logger.error("签发仓单失败", e);
            return Result.error(500, e.getMessage());
        }
    }

    // ==================== 仓单查询 ====================

    @ApiOperation("根据ID查询仓单")
    @WarehouseReceiptOwnership(paramName = "receiptId", errorMessage = "无权限操作：非仓单相关方无权查询")
    @GetMapping("/receipt/{receiptId}")
    public Result<WarehouseReceipt> getReceiptById(@PathVariable String receiptId) {
        try {
            Long id = parseId(receiptId, "仓单ID");
            WarehouseReceipt receipt = warehouseReceiptService.getReceiptById(id);
            return Result.success(receipt);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @ApiOperation("根据链上ID查询仓单")
    @GetMapping("/receipt/by-chain/{onChainId}")
    public Result<WarehouseReceipt> getReceiptByOnChainId(@PathVariable String onChainId) {
        WarehouseReceipt receipt = warehouseReceiptService.getReceiptByOnChainId(onChainId);
        return Result.success(receipt);
    }

    @ApiOperation("查询企业仓单列表")
    @GetMapping("/receipt/list")
    public Result<List<WarehouseReceipt>> getReceiptsByEntId() {
        Long entId = CurrentUser.getEntId();
        if (entId == null) {
            return Result.error(401, "无法获取当前用户企业信息，请先登录");
        }
        List<WarehouseReceipt> list = warehouseReceiptService.getReceiptsByEntId(entId);
        return Result.success(list);
    }

    @ApiOperation("查询企业在库仓单")
    @GetMapping("/receipt/in-stock")
    public Result<List<WarehouseReceipt>> getInStockReceipts() {
        Long entId = CurrentUser.getEntId();
        if (entId == null) {
            return Result.error(401, "无法获取当前用户企业信息，请先登录");
        }
        List<WarehouseReceipt> list = warehouseReceiptService.getInStockReceipts(entId);
        return Result.success(list);
    }

    // ==================== 背书转让 ====================

    @ApiOperation("发起背书转让")
    @WarehouseReceiptOwnership(paramName = "receiptId", fromBody = true, errorMessage = "无权限操作：仅仓单持有人可发起背书转让")
    @WarehouseStatusCheck(paramName = "receiptId", requiredLocked = false, errorMessage = "仓单已锁定，无法发起背书转让")
    @PostMapping("/endorsement/launch")
    public Result<Long> launchEndorsement(@RequestBody LaunchEndorsementRequest request) {
        try {
            // 仅从JWT获取用户信息，防止越权
            Long userId = CurrentUser.getUserId();

            if (userId == null) {
                return Result.error(401, "无法获取当前用户信息，请先登录");
            }

            Long endorsementId = warehouseReceiptService.launchEndorsement(
                    request.getReceiptId(),
                    userId,
                    request.getTransfereeEntId(),
                    request.getSignatureHash()
            );
            return Result.success(endorsementId);
        } catch (Exception e) {
            logger.error("发起背书转让失败", e);
            return Result.error(500, e.getMessage());
        }
    }

    @ApiOperation("确认/拒绝背书转让")
    @PostMapping("/endorsement/{endorsementId}/confirm")
    public Result<Boolean> confirmEndorsement(
            @PathVariable String endorsementId,
            @RequestParam Boolean accept) {
        try {
            Long id = parseId(endorsementId, "背书ID");
            // 校验权限：仅被背书目标企业可确认（特殊业务校验，保留手动检查）
            warehouseReceiptService.checkEndorsementTargetPermission(id);

            // 仅从JWT获取用户信息，防止越权
            Long userId = CurrentUser.getUserId();
            if (userId == null) {
                return Result.error(401, "无法获取当前用户信息，请先登录");
            }

            boolean success = warehouseReceiptService.confirmEndorsement(id, userId, accept);
            return Result.success(success);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("确认背书转让失败", e);
            return Result.error(500, e.getMessage());
        }
    }

    @ApiOperation("撤回背书")
    @PostMapping("/endorsement/{endorsementId}/revoke")
    public Result<Boolean> revokeEndorsement(@PathVariable String endorsementId) {
        try {
            Long id = parseId(endorsementId, "背书ID");
            // 校验权限：仅背书发起方可撤回（特殊业务校验，保留手动检查）
            warehouseReceiptService.checkEndorsementInitiatorPermission(id);

            boolean success = warehouseReceiptService.revokeEndorsement(id);
            return Result.success(success);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("撤回背书失败", e);
            return Result.error(500, "撤回背书失败");
        }
    }

    @ApiOperation("查询仓单背书记录")
    @WarehouseReceiptOwnership(paramName = "receiptId", errorMessage = "无权限操作：非仓单相关方无权查询背书记录")
    @GetMapping("/endorsement/list")
    public Result<List<ReceiptEndorsement>> getEndorsementsByReceiptId(@RequestParam Long receiptId) {
        List<ReceiptEndorsement> list = warehouseReceiptService.getEndorsementsByReceiptId(receiptId);
        return Result.success(list);
    }

    // ==================== 拆分/合并 ====================

    @ApiOperation("发起拆分申请")
    @WarehouseReceiptOwnership(paramName = "receiptId", fromBody = true, errorMessage = "无权限操作：仅仓单持有人可发起拆分申请")
    @WarehouseStatusCheck(paramName = "receiptId", requiredLocked = false, requiredStatus = {1}, errorMessage = "仓单已锁定或状态不满足拆分条件")
    @PostMapping("/split/apply")
    public Result<Long> applySplit(@RequestBody ApplySplitRequest request) {
        try {
            // 仅从JWT获取用户信息，防止越权
            Long userId = CurrentUser.getUserId();
            if (userId == null) {
                return Result.error(401, "无法获取当前用户信息，请先登录");
            }

            Long opLogId = warehouseReceiptService.applySplit(
                    request.getReceiptId(),
                    userId,
                    request.getTargetWeights()
            );
            return Result.success(opLogId);
        } catch (Exception e) {
            logger.error("发起拆分申请失败", e);
            return Result.error(500, e.getMessage());
        }
    }

    @ApiOperation("发起合并申请")
    @WarehouseReceiptOwnership(paramName = "receiptId", fromBody = true, errorMessage = "无权限操作：仅仓单持有人可发起合并申请")
    @WarehouseStatusCheck(paramName = "receiptId", requiredLocked = false, requiredStatus = {1}, errorMessage = "仓单已锁定或状态不满足合并条件")
    @PostMapping("/merge/apply")
    public Result<Long> applyMerge(@RequestBody ApplyMergeRequest request) {
        try {
            // 仅从JWT获取用户信息，防止越权
            Long userId = CurrentUser.getUserId();
            if (userId == null) {
                return Result.error(401, "无法获取当前用户信息，请先登录");
            }

            Long opLogId = warehouseReceiptService.applyMerge(
                    request.getReceiptIds(),
                    userId
            );
            return Result.success(opLogId);
        } catch (Exception e) {
            logger.error("发起合并申请失败", e);
            return Result.error(500, e.getMessage());
        }
    }

    @ApiOperation("执行/驳回拆分合并（仓储方操作）")
    @WarehousePermissionCheck(allowedRoles = {9}, errorMessage = "无权限操作：仅仓储方可执行拆分合并")
    @PostMapping("/split-merge/{opLogId}/execute")
    public Result<Boolean> executeSplitMerge(
            @PathVariable String opLogId,
            @RequestParam Boolean execute) {
        try {
            Long id = parseId(opLogId, "操作记录ID");
            // 仅从JWT获取用户信息，防止越权
            Long userId = CurrentUser.getUserId();
            if (userId == null) {
                return Result.error(401, "无法获取当前用户信息，请先登录");
            }

            boolean success = warehouseReceiptService.executeSplitMerge(id, userId, execute);
            return Result.success(success);
        } catch (Exception e) {
            logger.error("执行拆分合并失败", e);
            return Result.error(500, e.getMessage());
        }
    }

    @ApiOperation("查询拆分合并记录")
    @GetMapping("/split-merge/{opLogId}")
    public Result<ReceiptOperationLog> getOperationLogById(@PathVariable String opLogId) {
        try {
            Long id = parseId(opLogId, "操作记录ID");
            ReceiptOperationLog opLog = warehouseReceiptService.getOperationLogById(id);
            return Result.success(opLog);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    // ==================== 质押/解押 ====================

    @ApiOperation("质押锁定仓单（金融机构操作）")
    @WarehousePermissionCheck(allowedRoles = {6}, errorMessage = "无权限操作：仅金融机构可进行质押锁定")
    @WarehouseStatusCheck(paramName = "receiptId", requiredLocked = false, errorMessage = "仓单已锁定，无法重复质押")
    @PostMapping("/receipt/{receiptId}/lock")
    public Result<Boolean> lockReceipt(
            @PathVariable String receiptId,
            @RequestBody Map<String, Object> params) {
        try {
            Long id = parseId(receiptId, "仓单ID");
            String loanId = params.get("loanId") != null ? params.get("loanId").toString() : null;
            if (loanId == null || loanId.isEmpty()) {
                return Result.error(400, "贷款ID不能为空");
            }
            boolean success = warehouseReceiptService.lockReceipt(id, loanId);
            return Result.success(success);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("质押锁定仓单失败", e);
            return Result.error(500, "质押锁定仓单失败");
        }
    }

    @ApiOperation("还款解押仓单（金融机构操作）")
    @WarehousePermissionCheck(allowedRoles = {6}, errorMessage = "无权限操作：仅金融机构可进行还款解押")
    @PostMapping("/receipt/{receiptId}/unlock")
    public Result<Boolean> unlockReceipt(@PathVariable String receiptId) {
        try {
            Long id = parseId(receiptId, "仓单ID");
            boolean success = warehouseReceiptService.unlockReceipt(id);
            return Result.success(success);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("还款解押仓单失败", e);
            return Result.error(500, "还款解押仓单失败");
        }
    }

    // ==================== 核销出库 ====================

    @ApiOperation("申请核销出库")
    @WarehouseReceiptOwnership(paramName = "receiptId", fromBody = true, errorMessage = "无权限操作：仅仓单持有人可申请核销出库")
    @WarehouseStatusCheck(paramName = "receiptId", requiredLocked = false, errorMessage = "仓单已锁定，无法申请核销出库")
    @PostMapping("/burn/apply")
    public Result<Long> applyBurn(@RequestBody ApplyBurnRequest request) {
        try {
            // 仅从JWT获取用户信息，防止越权
            Long userId = CurrentUser.getUserId();

            if (userId == null) {
                return Result.error(401, "无法获取当前用户信息，请先登录");
            }

            Long stockOrderId = warehouseReceiptService.applyBurn(
                    request.getReceiptId(),
                    userId,
                    request.getSignatureHash()
            );
            return Result.success(stockOrderId);
        } catch (Exception e) {
            logger.error("申请核销出库失败", e);
            return Result.error(500, e.getMessage());
        }
    }

    @ApiOperation("确认核销出库（仓储方操作）")
    @WarehousePermissionCheck(allowedRoles = {9}, errorMessage = "无权限操作：仅仓储方可确认核销出库")
    @PostMapping("/burn/{stockOrderId}/confirm")
    public Result<Boolean> confirmBurn(@PathVariable String stockOrderId) {
        try {
            Long id = parseId(stockOrderId, "入库单ID");
            // 仅从JWT获取用户信息，防止越权
            Long userId = CurrentUser.getUserId();
            if (userId == null) {
                return Result.error(401, "无法获取当前用户信息，请先登录");
            }

            boolean success = warehouseReceiptService.confirmBurn(id, userId);
            return Result.success(success);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("确认核销出库失败", e);
            return Result.error(500, "确认核销出库失败");
        }
    }

    // ==================== 仓库管理 ====================

    @ApiOperation("创建仓库")
    @WarehousePermissionCheck(allowedRoles = {9}, errorMessage = "无权限操作：仅仓储方可创建仓库")
    @PostMapping("/warehouse/create")
    public Result<Long> createWarehouse(@RequestBody CreateWarehouseRequest request) {
        try {
            // 参数校验
            if (request.getName() == null || request.getName().isEmpty()) {
                return Result.error(400, "仓库名称不能为空");
            }
            if (request.getAddress() == null || request.getAddress().isEmpty()) {
                return Result.error(400, "仓库地址不能为空");
            }

            // 仅从JWT获取企业信息，防止越权
            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "无法获取当前企业信息，请先登录");
            }

            Long warehouseId = warehouseReceiptService.createWarehouse(
                    entId,
                    request.getName(),
                    request.getAddress(),
                    request.getContactUser(),
                    request.getContactPhone()
            );
            return Result.success(warehouseId);
        } catch (Exception e) {
            logger.error("创建仓库失败", e);
            return Result.error(500, e.getMessage());
        }
    }

    @ApiOperation("查询仓库列表")
    @WarehousePermissionCheck(allowedRoles = {9}, errorMessage = "无权限操作：仅仓储方可查看仓库列表")
    @GetMapping("/warehouse/list")
    public Result<List<Warehouse>> getWarehousesByEntId() {
        Long entId = CurrentUser.getEntId();
        if (entId == null) {
            return Result.error(401, "无法获取当前用户企业信息，请先登录");
        }
        List<Warehouse> list = warehouseReceiptService.getWarehousesByEntId(entId);
        return Result.success(list);
    }

    // ==================== 溯源查询 ====================

    @ApiOperation("全路径溯源查询")
    @WarehouseReceiptOwnership(paramName = "receiptId", errorMessage = "无权限操作：非仓单相关方无权查询溯源信息")
    @GetMapping("/receipt/{receiptId}/trace")
    public Result<TraceInfo> traceReceipt(@PathVariable String receiptId) {
        try {
            Long id = parseId(receiptId, "仓单ID");
            TraceInfo traceInfo = warehouseReceiptService.traceReceipt(id);
            return Result.success(traceInfo);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    // ==================== Request DTOs ====================

    public static class StockInApplyRequest {
        private Long warehouseId;
        private Long entId;
        private Long userId;
        private String goodsName;
        private BigDecimal weight;
        private String unit;
        private String attachmentUrl;

        public Long getWarehouseId() { return warehouseId; }
        public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
        public Long getEntId() { return entId; }
        public void setEntId(Long entId) { this.entId = entId; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getGoodsName() { return goodsName; }
        public void setGoodsName(String goodsName) { this.goodsName = goodsName; }
        public BigDecimal getWeight() { return weight; }
        public void setWeight(BigDecimal weight) { this.weight = weight; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        public String getAttachmentUrl() { return attachmentUrl; }
        public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }
    }

    public static class MintReceiptRequest {
        private Long stockOrderId;
        private Long warehouseUserId;
        private String onChainId;

        public Long getStockOrderId() { return stockOrderId; }
        public void setStockOrderId(Long stockOrderId) { this.stockOrderId = stockOrderId; }
        public Long getWarehouseUserId() { return warehouseUserId; }
        public void setWarehouseUserId(Long warehouseUserId) { this.warehouseUserId = warehouseUserId; }
        public String getOnChainId() { return onChainId; }
        public void setOnChainId(String onChainId) { this.onChainId = onChainId; }
    }

    public static class LaunchEndorsementRequest {
        private Long receiptId;
        private Long transferorUserId;
        private Long transfereeEntId;
        private String signatureHash;

        public Long getReceiptId() { return receiptId; }
        public void setReceiptId(Long receiptId) { this.receiptId = receiptId; }
        public Long getTransferorUserId() { return transferorUserId; }
        public void setTransferorUserId(Long transferorUserId) { this.transferorUserId = transferorUserId; }
        public Long getTransfereeEntId() { return transfereeEntId; }
        public void setTransfereeEntId(Long transfereeEntId) { this.transfereeEntId = transfereeEntId; }
        public String getSignatureHash() { return signatureHash; }
        public void setSignatureHash(String signatureHash) { this.signatureHash = signatureHash; }
    }

    public static class ApplySplitRequest {
        private Long receiptId;
        private Long applyUserId;
        private BigDecimal[] targetWeights;

        public Long getReceiptId() { return receiptId; }
        public void setReceiptId(Long receiptId) { this.receiptId = receiptId; }
        public Long getApplyUserId() { return applyUserId; }
        public void setApplyUserId(Long applyUserId) { this.applyUserId = applyUserId; }
        public BigDecimal[] getTargetWeights() { return targetWeights; }
        public void setTargetWeights(BigDecimal[] targetWeights) { this.targetWeights = targetWeights; }
    }

    public static class ApplyMergeRequest {
        private List<Long> receiptIds;
        private Long applyUserId;

        public List<Long> getReceiptIds() { return receiptIds; }
        public void setReceiptIds(List<Long> receiptIds) { this.receiptIds = receiptIds; }
        public Long getApplyUserId() { return applyUserId; }
        public void setApplyUserId(Long applyUserId) { this.applyUserId = applyUserId; }
    }

    public static class ApplyBurnRequest {
        private Long receiptId;
        private Long applyUserId;
        private String signatureHash;

        public Long getReceiptId() { return receiptId; }
        public void setReceiptId(Long receiptId) { this.receiptId = receiptId; }
        public Long getApplyUserId() { return applyUserId; }
        public void setApplyUserId(Long applyUserId) { this.applyUserId = applyUserId; }
        public String getSignatureHash() { return signatureHash; }
        public void setSignatureHash(String signatureHash) { this.signatureHash = signatureHash; }
    }

    /**
     * 解析字符串ID为Long类型
     */
    private Long parseId(String idStr, String fieldName) {
        if (idStr == null || idStr.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        try {
            return Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + "格式错误，应为数字: " + idStr);
        }
    }

    /**
     * 根据ID或编号查询入库单
     * 支持数字ID或字符串stockNo查询
     */
    private StockOrder getStockOrderByIdOrStockNo(String stockOrderIdOrNo) {
        if (stockOrderIdOrNo == null || stockOrderIdOrNo.trim().isEmpty()) {
            throw new IllegalArgumentException("入库单ID或编号不能为空");
        }
        // 判断是否为数字（ID）
        if (stockOrderIdOrNo.matches("^\\d+$")) {
            Long id = parseId(stockOrderIdOrNo, "入库单ID");
            return warehouseReceiptService.getStockOrderById(id);
        } else {
            // 字符串按stockNo查询
            return warehouseReceiptService.getStockOrderByStockNo(stockOrderIdOrNo);
        }
    }

    public static class CreateWarehouseRequest {
        private Long entId;
        private String name;
        private String address;
        private String contactUser;
        private String contactPhone;

        public Long getEntId() { return entId; }
        public void setEntId(Long entId) { this.entId = entId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getContactUser() { return contactUser; }
        public void setContactUser(String contactUser) { this.contactUser = contactUser; }
        public String getContactPhone() { return contactPhone; }
        public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    }
}
