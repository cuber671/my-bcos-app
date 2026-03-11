package com.fisco.app.Modules.Warehouse.Service;

import java.math.BigDecimal;
import java.util.List;

import com.fisco.app.Modules.Warehouse.Entity.ReceiptEndorsement;
import com.fisco.app.Modules.Warehouse.Entity.ReceiptOperationLog;
import com.fisco.app.Modules.Warehouse.Entity.StockOrder;
import com.fisco.app.Modules.Warehouse.Entity.WarehouseReceipt;

/**
 * 仓单业务服务接口
 *
 * 提供仓单全生命周期管理，包括入库、签发、背书、拆分合并、质押、核销等功能
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
public interface WarehouseReceiptService {

    // ==================== 权限校验 ====================

    /**
     * 校验仓储方权限
     */
    void checkWarehousePermission(Integer entRole);

    /**
     * 校验金融机构权限
     */
    void checkFinancialPermission(Integer entRole);

    /**
     * 校验仓单持有人权限
     */
    void checkReceiptOwnerPermission(Long receiptId);

    /**
     * 校验被背书企业权限（确认背书转让时使用）
     */
    void checkEndorsementTargetPermission(Long endorsementId);

    /**
     * 校验背书发起方权限（撤回背书时使用）
     */
    void checkEndorsementInitiatorPermission(Long endorsementId);

    /**
     * 校验仓单状态值是否有效
     * 使用 @ValidWarehouseReceiptStatus 注解进行校验
     *
     * @param status 仓单状态值
     * @throws IllegalArgumentException 如果状态值无效
     */
    void validateReceiptStatus(Integer status);

    // ==================== 入库单上链存证 ====================

    /**
     * 计算入库单数据哈希
     * 对入库单核心数据进行SHA-256哈希，用于数据完整性校验
     *
     * @param stockOrder 入库单实体
     * @return SHA-256哈希值（64位十六进制字符串）
     */
    String calculateStockOrderHash(StockOrder stockOrder);

    /**
     * 存储入库单上链交易哈希
     * 记录区块链交易ID，便于后续溯源
     *
     * @param stockOrderId 入库单ID
     * @param chainTxHash 区块链交易哈希
     * @return 是否更新成功
     */
    boolean saveStockOrderChainTxHash(Long stockOrderId, String chainTxHash);

    // ==================== 入库单管理 ====================

    /**
     * 申请入库
     *
     * @param warehouseId 仓库ID
     * @param entId 申请企业ID
     * @param userId 申请操作人ID
     * @param goodsName 货物名称
     * @param weight 货物重量
     * @param unit 计量单位
     * @param attachmentUrl 附件URL
     * @return 入库单ID
     */
    Long applyStockIn(Long warehouseId, Long entId, Long userId, String goodsName,
            BigDecimal weight, String unit, String attachmentUrl);

    /**
     * 确认入库单（可签发仓单）
     *
     * @param stockOrderId 入库单ID
     * @return 是否成功
     */
    boolean confirmStockOrder(Long stockOrderId);

    /**
     * 取消入库单
     *
     * @param stockOrderId 入库单ID
     * @return 是否成功
     */
    boolean cancelStockOrder(Long stockOrderId);

    /**
     * 根据ID查询入库单
     *
     * @param stockOrderId 入库单ID
     * @return 入库单信息
     */
    StockOrder getStockOrderById(Long stockOrderId);

    /**
     * 根据入库单编号查询入库单
     *
     * @param stockNo 入库单编号
     * @return 入库单信息
     */
    StockOrder getStockOrderByStockNo(String stockNo);

    /**
     * 根据企业ID查询入库单列表
     *
     * @param entId 企业ID
     * @return 入库单列表
     */
    List<StockOrder> getStockOrdersByEntId(Long entId);

    // ==================== 仓单签发 ====================

    /**
     * 签发仓单（基于确认的入库单）
     *
     * @param stockOrderId 入库单ID
     * @param warehouseUserId 仓储方操作人ID
     * @param onChainId 链上TokenID
     * @return 仓单ID
     */
    Long mintReceipt(Long stockOrderId, Long warehouseUserId, String onChainId);

    /**
     * 根据ID查询仓单
     *
     * @param receiptId 仓单ID
     * @return 仓单信息
     */
    WarehouseReceipt getReceiptById(Long receiptId);

    /**
     * 根据链上ID查询仓单
     *
     * @param onChainId 链上TokenID
     * @return 仓单信息
     */
    WarehouseReceipt getReceiptByOnChainId(String onChainId);

    /**
     * 根据企业ID查询仓单列表
     *
     * @param entId 企业ID
     * @return 仓单列表
     */
    List<WarehouseReceipt> getReceiptsByEntId(Long entId);

    /**
     * 查询企业持有的所有在库仓单
     *
     * @param entId 企业ID
     * @return 在库仓单列表
     */
    List<WarehouseReceipt> getInStockReceipts(Long entId);

    // ==================== 背书转让 ====================

    /**
     * 发起背书转让
     *
     * @param receiptId 仓单ID
     * @param transferorUserId 转出方操作人ID
     * @param transfereeEntId 接收方企业ID
     * @param signatureHash 数字签名哈希
     * @return 背书记录ID
     */
    Long launchEndorsement(Long receiptId, Long transferorUserId, Long transfereeEntId,
            String signatureHash);

    /**
     * 确认/拒绝背书
     *
     * @param endorsementId 背书记录ID
     * @param transfereeUserId 接收方操作人ID
     * @param accept 是否接受
     * @return 是否成功
     */
    boolean confirmEndorsement(Long endorsementId, Long transfereeUserId, boolean accept);

    /**
     * 撤回背书
     *
     * @param endorsementId 背书记录ID
     * @return 是否成功
     */
    boolean revokeEndorsement(Long endorsementId);

    /**
     * 根据仓单ID查询背书记录
     *
     * @param receiptId 仓单ID
     * @return 背书记录列表
     */
    List<ReceiptEndorsement> getEndorsementsByReceiptId(Long receiptId);

    // ==================== 拆分/合并 ====================

    /**
     * 发起拆分申请
     *
     * @param receiptId 原仓单ID
     * @param applyUserId 申请操作人ID
     * @param targetWeights 目标重量数组
     * @return 操作记录ID
     */
    Long applySplit(Long receiptId, Long applyUserId, BigDecimal[] targetWeights);

    /**
     * 发起合并申请
     *
     * @param receiptIds 原仓单ID列表
     * @param applyUserId 申请操作人ID
     * @return 操作记录ID
     */
    Long applyMerge(List<Long> receiptIds, Long applyUserId);

    /**
     * 执行/驳回拆分合并操作
     *
     * @param opLogId 操作记录ID
     * @param executeUserId 执行操作人ID
     * @param execute 是否执行
     * @return 是否成功
     */
    boolean executeSplitMerge(Long opLogId, Long executeUserId, boolean execute);

    /**
     * 查询拆分合并记录
     *
     * @param opLogId 操作记录ID
     * @return 操作记录
     */
    ReceiptOperationLog getOperationLogById(Long opLogId);

    // ==================== 质押/解押 ====================

    /**
     * 质押锁定仓单
     *
     * @param receiptId 仓单ID
     * @param loanId 融资单号
     * @return 是否成功
     */
    boolean lockReceipt(Long receiptId, String loanId);

    /**
     * 还款解押仓单
     *
     * @param receiptId 仓单ID
     * @return 是否成功
     */
    boolean unlockReceipt(Long receiptId);

    // ==================== 核销出库 ====================

    /**
     * 申请核销出库
     *
     * @param receiptId 仓单ID
     * @param applyUserId 申请操作人ID
     * @param signatureHash 数字签名哈希
     * @return 入库单ID
     */
    Long applyBurn(Long receiptId, Long applyUserId, String signatureHash);

    /**
     * 确认核销出库
     *
     * @param stockOrderId 出库单ID
     * @param warehouseUserId 仓储方操作人ID
     * @return 是否成功
     */
    boolean confirmBurn(Long stockOrderId, Long warehouseUserId);

    // ==================== 仓库管理 ====================

    /**
     * 创建仓库
     *
     * @param entId 所属企业ID
     * @param name 仓库名称
     * @param address 仓库地址
     * @param contactUser 现场负责人
     * @param contactPhone 联系电话
     * @return 仓库ID
     */
    Long createWarehouse(Long entId, String name, String address, String contactUser,
            String contactPhone);

    /**
     * 查询仓库列表
     *
     * @param entId 所属企业ID
     * @return 仓库列表
     */
    List<com.fisco.app.Modules.Warehouse.Entity.Warehouse> getWarehousesByEntId(Long entId);

    // ==================== 溯源查询 ====================

    /**
     * 全路径溯源查询
     *
     * @param receiptId 仓单ID
     * @return 溯源信息
     */
    TraceInfo traceReceipt(Long receiptId);

    /**
     * 溯源信息
     */
    class TraceInfo {
        private WarehouseReceipt currentReceipt;
        private List<WarehouseReceipt> historyReceipts;
        private List<ReceiptEndorsement> endorsementHistory;
        private List<ReceiptOperationLog> operationHistory;

        public WarehouseReceipt getCurrentReceipt() { return currentReceipt; }
        public void setCurrentReceipt(WarehouseReceipt currentReceipt) { this.currentReceipt = currentReceipt; }
        public List<WarehouseReceipt> getHistoryReceipts() { return historyReceipts; }
        public void setHistoryReceipts(List<WarehouseReceipt> historyReceipts) { this.historyReceipts = historyReceipts; }
        public List<ReceiptEndorsement> getEndorsementHistory() { return endorsementHistory; }
        public void setEndorsementHistory(List<ReceiptEndorsement> endorsementHistory) { this.endorsementHistory = endorsementHistory; }
        public List<ReceiptOperationLog> getOperationHistory() { return operationHistory; }
        public void setOperationHistory(List<ReceiptOperationLog> operationHistory) { this.operationHistory = operationHistory; }
    }
}
