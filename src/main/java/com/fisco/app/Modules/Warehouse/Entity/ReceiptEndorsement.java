package com.fisco.app.Modules.Warehouse.Entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 背书记录实体类
 *
 * 对应数据库表: t_receipt_endorsement
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Data
@TableName("t_receipt_endorsement")
public class ReceiptEndorsement {

    /**
     * 背书记录ID - 雪花算法生成
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 关联仓单ID
     */
    @TableField("receipt_id")
    private Long receiptId;

    /**
     * 背书企业ID (转出方)
     */
    @TableField("transferor_ent_id")
    private Long transferorEntId;

    /**
     * 背书操作人ID
     */
    @TableField("transferor_user_id")
    private Long transferorUserId;

    /**
     * 被背书企业ID (接收方)
     */
    @TableField("transferee_ent_id")
    private Long transfereeEntId;

    /**
     * 接收操作人ID
     */
    @TableField("transferee_user_id")
    private Long transfereeUserId;

    /**
     * 数字签名哈希
     */
    @TableField("signature_hash")
    private String signatureHash;

    /**
     * 区块链交易哈希
     */
    @TableField("tx_hash")
    private String txHash;

    /**
     * 记录状态: 1-待签收, 2-已签收, 3-已拒绝, 4-已撤回
     */
    private Integer status;

    /**
     * 转让备注
     */
    private String remark;

    /**
     * 发起时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 完成时间
     */
    @TableField("finish_time")
    private LocalDateTime finishTime;

    // ==================== 状态常量 ====================

    /** 待签收 */
    public static final int STATUS_PENDING = 1;

    /** 已签收 */
    public static final int STATUS_CONFIRMED = 2;

    /** 已拒绝 */
    public static final int STATUS_REJECTED = 3;

    /** 已撤回 */
    public static final int STATUS_REVOKED = 4;
}
