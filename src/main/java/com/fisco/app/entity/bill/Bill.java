package com.fisco.app.entity.bill;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 票据实体类（完整版）
 *
 * 功能：
 * 1. 支付功能：替代现金，跨区域清算
 * 2. 融资功能：贴现、质押、转贴现
 * 3. 信用担保：无条件付款承诺
 * 4. 结算功能：背书转让抵消债务
 * 5. 汇兑功能：无现金资金划转
 * 6. 权利证明：债权凭证
 * 7. 风险管理：锁定金额、到期日、主体
 *
 * 票据类型：
 * - BANK_ACCEPTANCE_BILL: 银行承兑汇票
 * - COMMERCIAL_ACCEPTANCE_BILL: 商业承兑汇票
 * - BANK_NOTE: 银行本票
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-02
 * @version 2.0
 */
@Data
@Entity
@Table(name = "bill", indexes = {
    @Index(name = "idx_bill_no", columnList = "bill_no"),
    @Index(name = "idx_bill_type", columnList = "bill_type"),
    @Index(name = "idx_bill_status", columnList = "bill_status"),
    @Index(name = "idx_drawer_id", columnList = "drawer_id"),
    @Index(name = "idx_drawee_id", columnList = "drawee_id"),
    @Index(name = "idx_payee_id", columnList = "payee_id"),
    @Index(name = "idx_current_holder_id", columnList = "current_holder_id"),
    @Index(name = "idx_due_date", columnList = "due_date"),
    @Index(name = "idx_backed_receipt_id", columnList = "backed_receipt_id"),
    @Index(name = "idx_created_at", columnList = "created_at"),
    @Index(name = "idx_parent_bill", columnList = "parent_bill_id"),
    @Index(name = "idx_guarantee", columnList = "guarantee_id")
})
@ApiModel(value = "票据实体", description = "票据主表实体（完整版）")
public class Bill {

    // ==================== 主键 ====================

    @ApiModelProperty(value = "票据ID", example = "b1a2b3c4-d5e6-7890-abcd-ef1234567890")
    @Id
    @Column(name = "bill_id", length = 36)
    private String billId;

    // ==================== 基础信息 ====================

    @ApiModelProperty(value = "票据编号", example = "BIL20260200000001")
    @Column(name = "bill_no", length = 50, unique = true, nullable = false)
    private String billNo;

    @ApiModelProperty(value = "票据类型",
            notes = "BANK_ACCEPTANCE_BILL-银行承兑汇票, COMMERCIAL_ACCEPTANCE_BILL-商业承兑汇票, BANK_NOTE-银行本票",
            example = "BANK_ACCEPTANCE_BILL")
    @Column(name = "bill_type", length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    private BillType billType;

    @ApiModelProperty(value = "票面金额", example = "1000000.00")
    @Column(name = "face_value", precision = 20, scale = 2, nullable = false)
    private BigDecimal faceValue;

    @ApiModelProperty(value = "货币类型", example = "CNY")
    @Column(name = "currency", length = 10, nullable = false)
    private String currency = "CNY";

    @ApiModelProperty(value = "开票日期")
    @Column(name = "issue_date", columnDefinition = "DATETIME(6)", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime issueDate;

    @ApiModelProperty(value = "到期日期")
    @Column(name = "due_date", columnDefinition = "DATETIME(6)", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dueDate;

    // ==================== 参与方信息 ====================

    @ApiModelProperty(value = "出票人ID")
    @Column(name = "drawer_id", length = 36, nullable = false)
    private String drawerId;

    @ApiModelProperty(value = "出票人名称")
    @Column(name = "drawer_name", length = 200, nullable = false)
    private String drawerName;

    @ApiModelProperty(value = "出票人区块链地址", example = "0x1234567890abcdef1234567890abcdef12345678")
    @Column(name = "drawer_address", length = 42)
    private String drawerAddress;

    @ApiModelProperty(value = "出票人银行账号")
    @Column(name = "drawer_account", length = 100)
    private String drawerAccount;

    @ApiModelProperty(value = "承兑人ID")
    @Column(name = "drawee_id", length = 36, nullable = false)
    private String draweeId;

    @ApiModelProperty(value = "承兑人名称")
    @Column(name = "drawee_name", length = 200, nullable = false)
    private String draweeName;

    @ApiModelProperty(value = "承兑人区块链地址")
    @Column(name = "drawee_address", length = 42)
    private String draweeAddress;

    @ApiModelProperty(value = "承兑人银行账号")
    @Column(name = "drawee_account", length = 100)
    private String draweeAccount;

    @ApiModelProperty(value = "收款人ID")
    @Column(name = "payee_id", length = 36, nullable = false)
    private String payeeId;

    @ApiModelProperty(value = "收款人名称")
    @Column(name = "payee_name", length = 200, nullable = false)
    private String payeeName;

    @ApiModelProperty(value = "收款人区块链地址")
    @Column(name = "payee_address", length = 42)
    private String payeeAddress;

    @ApiModelProperty(value = "收款人银行账号")
    @Column(name = "payee_account", length = 100)
    private String payeeAccount;

    // ==================== 当前持票人信息 ====================

    @ApiModelProperty(value = "当前持票人ID")
    @Column(name = "current_holder_id", length = 36, nullable = false)
    private String currentHolderId;

    @ApiModelProperty(value = "当前持票人名称")
    @Column(name = "current_holder_name", length = 200, nullable = false)
    private String currentHolderName;

    @ApiModelProperty(value = "当前持票人区块链地址")
    @Column(name = "current_holder_address", length = 42)
    private String currentHolderAddress;

    // ==================== 状态信息 ====================

    @ApiModelProperty(value = "票据状态",
            notes = "DRAFT-草稿, ISSUED-已开票, ENDORSED-已背书, PLEDGED-已质押, " +
                    "DISCOUNTED-已贴现, FINANCED-已融资, FROZEN-已冻结, CANCELLED-已作废, PAID-已付款, SETTLED-已结算")
    @Column(name = "bill_status", length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    private BillStatus billStatus = BillStatus.DRAFT;

    @ApiModelProperty(value = "区块链状态",
            notes = "NOT_ONCHAIN-未上链, PENDING-待上链, ONCHAIN-已上链, FAILED-上链失败")
    @Column(name = "blockchain_status", length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    private BlockchainStatus blockchainStatus = BlockchainStatus.NOT_ONCHAIN;

    @ApiModelProperty(value = "区块链交易哈希")
    @Column(name = "blockchain_tx_hash", length = 100)
    private String blockchainTxHash;

    @ApiModelProperty(value = "上链时间")
    @Column(name = "blockchain_time", columnDefinition = "DATETIME(6)")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime blockchainTime;

    @ApiModelProperty(value = "元数据哈希（V2合约双层数据存储）", notes = "存储扩展数据的哈希值，对应V2合约的extendedDataHash")
    @Column(name = "metadata_hash", length = 66, nullable = true)
    private String metadataHash;

    // ==================== 融资信息 ====================

    @ApiModelProperty(value = "贴现率（%）", example = "4.500000")
    @Column(name = "discount_rate", precision = 10, scale = 6)
    private BigDecimal discountRate;

    @ApiModelProperty(value = "贴现金额", example = "11250.00")
    @Column(name = "discount_amount", precision = 20, scale = 2)
    private BigDecimal discountAmount;

    @ApiModelProperty(value = "贴现日期")
    @Column(name = "discount_date", columnDefinition = "DATETIME(6)")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime discountDate;

    @ApiModelProperty(value = "贴现机构ID")
    @Column(name = "discount_institution_id", length = 36)
    private String discountInstitutionId;

    @ApiModelProperty(value = "质押金额", example = "900000.00")
    @Column(name = "pledge_amount", precision = 20, scale = 2)
    private BigDecimal pledgeAmount;

    @ApiModelProperty(value = "质押机构ID")
    @Column(name = "pledge_institution_id", length = 36)
    private String pledgeInstitutionId;

    @ApiModelProperty(value = "质押期限（天）")
    @Column(name = "pledge_period")
    private Integer pledgePeriod;

    @ApiModelProperty(value = "质押日期")
    @Column(name = "pledge_date", columnDefinition = "DATETIME(6)")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime pledgeDate;

    // ==================== 仓单联动信息 ====================

    @ApiModelProperty(value = "关联的仓单质押ID")
    @Column(name = "receipt_pledge_id", length = 36)
    private String receiptPledgeId;

    @ApiModelProperty(value = "担保仓单ID")
    @Column(name = "backed_receipt_id", length = 36)
    private String backedReceiptId;

    @ApiModelProperty(value = "仓单担保价值", example = "4500000.00")
    @Column(name = "receipt_pledge_value", precision = 20, scale = 2)
    private BigDecimal receiptPledgeValue;

    // ==================== 拆分合并信息 ====================

    @ApiModelProperty(value = "父票据ID（拆分或合并后）")
    @Column(name = "parent_bill_id", length = 36)
    private String parentBillId;

    @ApiModelProperty(value = "拆分数量", example = "3")
    @Column(name = "split_count")
    private Integer splitCount;

    @ApiModelProperty(value = "合并前票据数量", example = "2")
    @Column(name = "merge_count")
    private Integer mergeCount;

    @ApiModelProperty(value = "拆分时间")
    @Column(name = "split_time", columnDefinition = "DATETIME(6)")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime splitTime;

    @ApiModelProperty(value = "合并时间")
    @Column(name = "merge_time", columnDefinition = "DATETIME(6)")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime mergeTime;

    // ==================== 承兑信息 ====================

    @ApiModelProperty(value = "承兑时间")
    @Column(name = "acceptance_time", columnDefinition = "DATETIME(6)")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime acceptanceTime;

    @ApiModelProperty(value = "承兑备注", example = "确认承兑该票据")
    @Column(name = "acceptance_remarks", length = 500)
    private String acceptanceRemarks;

    // ==================== 担保信息 ====================

    @ApiModelProperty(value = "担保记录ID")
    @Column(name = "guarantee_id", length = 36)
    private String guaranteeId;

    @ApiModelProperty(value = "是否有担保")
    @Column(name = "has_guarantee", nullable = false)
    private Boolean hasGuarantee = false;

    // ==================== 追索信息 ====================

    @ApiModelProperty(value = "是否拒付")
    @Column(name = "dishonored", nullable = false)
    private Boolean dishonored = false;

    @ApiModelProperty(value = "拒付日期")
    @Column(name = "dishonored_date", columnDefinition = "DATETIME(6)")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dishonoredDate;

    @ApiModelProperty(value = "拒付原因", example = "承兑人账户余额不足")
    @Column(name = "dishonored_reason", columnDefinition = "TEXT")
    private String dishonoredReason;

    @ApiModelProperty(value = "追索状态",
            notes = "NOT_INITIATED-未发起, INITIATED-已发起, IN_PROGRESS-进行中, COMPLETED-已完成, FAILED-失败")
    @Column(name = "recourse_status", length = 50)
    private String recourseStatus;

    // ==================== 结算信息 ====================

    @ApiModelProperty(value = "结算编号")
    @Column(name = "settlement_id", length = 36)
    private String settlementId;

    @ApiModelProperty(value = "关联债务（JSON格式）")
    @Column(name = "related_debts", columnDefinition = "TEXT")
    private String relatedDebts;

    @ApiModelProperty(value = "结算日期")
    @Column(name = "settlement_date", columnDefinition = "DATETIME(6)")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime settlementDate;

    // ==================== 交易背景信息 ====================

    @ApiModelProperty(value = "贸易合同ID")
    @Column(name = "trade_contract_id", length = 36)
    private String tradeContractId;

    @ApiModelProperty(value = "贸易金额", example = "1000000.00")
    @Column(name = "trade_amount", precision = 20, scale = 2)
    private BigDecimal tradeAmount;

    @ApiModelProperty(value = "货物描述", example = "钢材采购")
    @Column(name = "goods_description", length = 500)
    private String goodsDescription;

    @ApiModelProperty(value = "贸易日期")
    @Column(name = "trade_date", columnDefinition = "DATE")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime tradeDate;

    // ==================== 审计信息 ====================

    @ApiModelProperty(value = "创建时间")
    @Column(name = "created_at", columnDefinition = "DATETIME(6)", nullable = false, updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "更新时间")
    @Column(name = "updated_at", columnDefinition = "DATETIME(6)", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @ApiModelProperty(value = "创建人ID")
    @Column(name = "created_by", length = 36)
    private String createdBy;

    @ApiModelProperty(value = "更新人ID")
    @Column(name = "updated_by", length = 36)
    private String updatedBy;

    @ApiModelProperty(value = "备注")
    @Column(name = "remarks", length = 1000)
    private String remarks;

    // ==================== 生命周期回调 ====================

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ==================== 枚举定义 ====================

    /**
     * 票据类型枚举
     */
    public enum BillType {
        BANK_ACCEPTANCE_BILL,     // 银行承兑汇票
        COMMERCIAL_ACCEPTANCE_BILL, // 商业承兑汇票
        BANK_NOTE                  // 银行本票
    }

    /**
     * 票据状态枚举
     */
    public enum BillStatus {
        // 开票阶段
        DRAFT,                    // 草稿 - 未提交
        ISSUED,                   // 已开票 - 票据已生成

        // 流通阶段
        ENDORSED,                 // 已背书 - 已背书转让
        PLEDGED,                  // 已质押 - 已质押融资

        // 融资阶段
        DISCOUNTED,               // 已贴现 - 已贴现给银行
        FINANCED,                 // 已融资 - 已质押融资

        // 异常状态
        FROZEN,                   // 已冻结 - 法律纠纷
        CANCELLED,                // 已作废 - 票据作废

        // 结算状态
        PAID,                     // 已付款 - 已完成付款
        SETTLED                   // 已结算 - 已完成债权债务清算
    }

    /**
     * 区块链状态枚举
     */
    public enum BlockchainStatus {
        NOT_ONCHAIN,  // 未上链
        PENDING,      // 待上链
        ONCHAIN,      // 已上链
        FAILED        // 上链失败
    }
}
