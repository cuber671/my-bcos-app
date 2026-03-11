package com.fisco.app.Modules.Warehouse.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 仓单拆分/合并记录实体类
 *
 * 对应数据库表: t_receipt_operation_log
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Data
@TableName("t_receipt_operation_log")
public class ReceiptOperationLog {

    /**
     * 操作记录ID - 雪花算法生成
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 操作类型: 1-拆分(Split), 2-合并(Merge)
     */
    @TableField("op_type")
    private Integer opType;

    /**
     * 来源单据ID列表 (逗号分隔)
     */
    @TableField("source_receipt_ids")
    private String sourceReceiptIds;

    /**
     * 生成单据ID列表 (逗号分隔)
     */
    @TableField("target_receipt_ids")
    private String targetReceiptIds;

    /**
     * 操作总重量
     */
    @TableField("total_weight")
    private BigDecimal totalWeight;

    /**
     * 申请企业ID
     */
    @TableField("apply_ent_id")
    private Long applyEntId;

    /**
     * 申请操作人ID
     */
    @TableField("apply_user_id")
    private Long applyUserId;

    /**
     * 执行企业ID (仓储方)
     */
    @TableField("execute_ent_id")
    private Long executeEntId;

    /**
     * 执行操作人ID
     */
    @TableField("execute_user_id")
    private Long executeUserId;

    /**
     * 区块链交易哈希
     */
    @TableField("tx_hash")
    private String txHash;

    /**
     * 记录状态: 1-待操作, 2-已完成, 3-已驳回
     */
    private Integer status;

    /**
     * 操作备注
     */
    private String remark;

    /**
     * 申请时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 完成时间
     */
    @TableField("finish_time")
    private LocalDateTime finishTime;

    // ==================== 操作类型常量 ====================

    /** 拆分 (Split) */
    public static final int OP_TYPE_SPLIT = 1;

    /** 合并 (Merge) */
    public static final int OP_TYPE_MERGE = 2;

    // ==================== 状态常量 ====================

    /** 待操作 */
    public static final int STATUS_PENDING = 1;

    /** 已完成 */
    public static final int STATUS_COMPLETED = 2;

    /** 已驳回 */
    public static final int STATUS_REJECTED = 3;
}
