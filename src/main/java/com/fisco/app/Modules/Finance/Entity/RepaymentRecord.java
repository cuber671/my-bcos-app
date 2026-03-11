package com.fisco.app.Modules.Finance.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 还款记录实体类
 *
 * 对应数据库表: t_repayment_record
 *
 * 记录应收款项的还款流水信息，支持以下还款类型：
 * 1. 现金还款 - 债务人通过银行转账等方式还款
 * 2. 仓单抵债 - 债务人使用仓单所有权转让来抵销债务
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Data
@TableName("t_repayment_record")
public class RepaymentRecord {

    /**
     * 还款记录唯一主键ID - 雪花算法生成
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 关联应收款ID
     */
    @TableField("receivable_id")
    private Long receivableId;

    /**
     * 还款编号：业务展示单号（如：REP20260220001）
     */
    @TableField("repayment_no")
    private String repaymentNo;

    /**
     * 还款类型：1-现金还款；2-仓单抵债
     */
    @TableField("repayment_type")
    private Integer repaymentType;

    /**
     * 还款金额
     */
    @TableField("amount")
    private BigDecimal amount;

    /**
     * 币种
     */
    @TableField("currency")
    private String currency;

    /**
     * 付款凭证：现金还款时的转账凭证号
     */
    @TableField("payment_voucher")
    private String paymentVoucher;

    /**
     * 仓单ID：仓单抵债时关联的仓单ID
     */
    @TableField("receipt_id")
    private Long receiptId;

    /**
     * 抵债价格：仓单抵债时的评估价值
     */
    @TableField("offset_price")
    private BigDecimal offsetPrice;

    /**
     * 签名哈希：债务人数字签名
     */
    @TableField("signature_hash")
    private String signatureHash;

    /**
     * 还款时间
     */
    @TableField("repayment_time")
    private LocalDateTime repaymentTime;

    /**
     * 区块链交易哈希
     */
    @TableField("chain_tx_hash")
    private String chainTxHash;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 最后修改时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
