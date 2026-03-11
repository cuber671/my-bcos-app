package com.fisco.app.Modules.Finance.Service;

import java.math.BigDecimal;
import java.util.List;

import com.fisco.app.Modules.Finance.Entity.Receivable;
import com.fisco.app.Modules.Finance.Entity.RepaymentRecord;

/**
 * 金融服务接口
 *
 * 定义金融模块的核心业务方法，包括：
 * - 应收款生成、确认、调整
 * - 还款记录（现金还款、仓单抵债）
 * - 账务流水查询
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
public interface FinanceService {

    // ==================== 应收款操作 ====================

    /**
     * 预生成应收款（物流送达时自动调用）
     *
     * 根据物流单信息生成应收款，初始金额 = 实际重量 × 单价
     * 状态设为"待确认"
     *
     * @param voucherId 物流单ID
     * @param unitPrice 单价
     * @return 生成结果
     */
    Receivable generateReceivable(Long voucherId, BigDecimal unitPrice);

    /**
     * 根据ID查询应收款
     *
     * @param id 应收款ID
     * @return 应收款
     */
    Receivable getReceivableById(Long id);

    /**
     * 根据应收款编号查询应收款
     *
     * @param receivableNo 应收款编号
     * @return 应收款
     */
    Receivable getReceivableByNo(String receivableNo);

    /**
     * 查询债权人的应收款列表
     *
     * @param creditorEntId 债权人ID
     * @return 应收款列表
     */
    List<Receivable> listByCreditor(Long creditorEntId);

    /**
     * 查询债务人的应收款列表
     *
     * @param debtorEntId 债务人ID
     * @return 应收款列表
     */
    List<Receivable> listByDebtor(Long debtorEntId);

    /**
     * 账单确权确认（债务人数字签名确认）
     *
     * 确认后账单正式生效，具备法律效力
     * 状态从"待确认"变为"生效中"
     *
     * @param receivableId 应收款ID
     * @param signature 债务人数字签名
     * @return 确认结果
     */
    Receivable confirmReceivable(Long receivableId, String signature);

    /**
     * 金额动态修正
     *
     * 处理损耗或拆分：
     * - adjustType=1: 物流损耗扣减
     * - adjustType=2: 仓单拆分同步
     *
     * @param receivableId 应收款ID
     * @param adjustType 调整类型
     * @param amount 调整金额
     * @return 调整结果
     */
    Receivable adjustReceivable(Long receivableId, Integer adjustType, BigDecimal amount);

    // ==================== 还款操作 ====================

    /**
     * 现金分批还款
     *
     * 记录本次还款额，增加 collected_amount
     * 校验还款金额 ≤ 待还余额
     *
     * @param receivableId 应收款ID
     * @param amount 还款金额
     * @param paymentVoucher 付款凭证
     * @return 还款记录
     */
    RepaymentRecord cashRepayment(Long receivableId, BigDecimal amount, String paymentVoucher);

    /**
     * 仓单抵债核销
     *
     * 验证仓单所有权，确认后仓单 owner_id 变更为债权人
     * 同时按抵债价值冲减账单余额
     *
     * @param receivableId 应收款ID
     * @param receiptId 仓单ID
     * @param offsetPrice 抵债价格
     * @param signatureHash 签名哈希
     * @return 还款记录
     */
    RepaymentRecord offsetWithCollateral(Long receivableId, Long receiptId, BigDecimal offsetPrice, String signatureHash);

    /**
     * 根据应收款ID查询还款记录列表
     *
     * @param receivableId 应收款ID
     * @return 还款记录列表
     */
    List<RepaymentRecord> listRepayments(Long receivableId);

    // ==================== 仓单拆分联动 ====================

    /**
     * 仓单拆分联动 - 当仓单拆分时自动调用
     *
     * @param parentReceiptId 原仓单ID
     * @param childReceiptIds 拆分后的新仓单ID列表
     * @return 拆分后的应收款列表
     */
    List<Receivable> syncReceiptSplit(Long parentReceiptId, List<Long> childReceiptIds);

    // ==================== 融资结算操作 ====================

    /**
     * 应收款融资
     *
     * 债权人用应收款向金融机构申请融资
     * 融资后应收款状态变为"已融资"
     *
     * @param receivableId 应收款ID
     * @param financeAmount 融资金额
     * @param financeEntId 金融机构ID（融资方）
     * @return 融资结果
     */
    Receivable financeReceivable(Long receivableId, BigDecimal financeAmount, Long financeEntId);

    /**
     * 应收款结算
     *
     * 融资还款后结算应收款
     * 结算后应收款状态变为"已结清"
     *
     * @param receivableId 应收款ID
     * @return 结算结果
     */
    Receivable settleReceivable(Long receivableId);
}
