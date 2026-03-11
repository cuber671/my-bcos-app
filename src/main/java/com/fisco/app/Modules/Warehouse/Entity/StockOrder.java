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
 * 入库单实体类
 *
 * 对应数据库表: t_stock_order
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Data
@TableName("t_stock_order")
public class StockOrder {

    /**
     * 入库单ID - 雪花算法生成
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 入库单编号 - 人类可读的编号，如 "STOCK20260310T002"
     */
    @TableField("stock_no")
    private String stockNo;

    /**
     * 仓库ID
     */
    @TableField("warehouse_id")
    private Long warehouseId;

    /**
     * 申请企业ID
     */
    @TableField("ent_id")
    private Long entId;

    /**
     * 申请操作人ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 货物名称
     */
    @TableField("goods_name")
    private String goodsName;

    /**
     * 货物重量
     */
    private BigDecimal weight;

    /**
     * 计量单位
     */
    private String unit;

    /**
     * 附件URL (入库凭证)
     */
    @TableField("attachment_url")
    private String attachmentUrl;

    /**
     * 数据哈希 - 用于上链存证
     * 入库单核心数据的SHA-256哈希值，确保数据不可篡改
     */
    @TableField("data_hash")
    private String dataHash;

    /**
     * 上链交易哈希 - 区块链交易ID
     */
    @TableField("chain_tx_hash")
    private String chainTxHash;

    /**
     * 入库单状态: 1-待审核, 2-已确认(可签发仓单), 3-已取消
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 最后修改时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ==================== 状态常量 ====================

    /** 待审核 */
    public static final int STATUS_PENDING = 1;

    /** 已确认 (可签发仓单) */
    public static final int STATUS_CONFIRMED = 2;

    /** 已取消 */
    public static final int STATUS_CANCELLED = 3;
}
