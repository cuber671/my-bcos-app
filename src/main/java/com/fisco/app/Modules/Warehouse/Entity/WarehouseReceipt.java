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
 * 电子仓单实体类
 *
 * 对应数据库表: t_warehouse_receipt
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Data
@TableName("t_warehouse_receipt")
public class WarehouseReceipt {

    /**
     * 仓单ID - 雪花算法生成
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 物理仓库ID
     */
    @TableField("warehouse_id")
    private Long warehouseId;

    /**
     * 链上资产唯一标识 (TokenID)
     */
    @TableField("on_chain_id")
    private String onChainId;

    /**
     * 当前货权人企业ID
     */
    @TableField("owner_ent_id")
    private Long ownerEntId;

    /**
     * 当前操作人ID
     */
    @TableField("owner_user_id")
    private Long ownerUserId;

    /**
     * 监管方企业ID (仓储企业)
     */
    @TableField("warehouse_ent_id")
    private Long warehouseEntId;

    /**
     * 监管方操作人ID
     */
    @TableField("warehouse_user_id")
    private Long warehouseUserId;

    /**
     * 货物名称 (如: 螺纹钢、大豆)
     */
    @TableField("goods_name")
    private String goodsName;

    /**
     * 货物重量/数量
     */
    private BigDecimal weight;

    /**
     * 计量单位
     */
    private String unit;

    /**
     * 父节点ID - 用于记录拆分来源
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 原始节点ID - 用于全路径追溯
     */
    @TableField("root_id")
    private Long rootId;

    /**
     * 质押锁定状态: false-未锁定, true-已锁定
     */
    @TableField("is_locked")
    private Boolean isLocked;

    /**
     * 质押贷款ID - 关联融资业务
     */
    @TableField("loan_id")
    private String loanId;

    /**
     * 仓单状态: 1-在库, 2-待转让, 3-已拆分/合并, 4-已核销, 5-物流转运中
     */
    private Integer status;

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

    /** 在库 */
    public static final int STATUS_IN_STOCK = 1;

    /** 待转让 */
    public static final int STATUS_PENDING_TRANSFER = 2;

    /** 已拆分/合并 */
    public static final int STATUS_SPLIT_MERGED = 3;

    /** 已核销 */
    public static final int STATUS_BURNED = 4;

    /** 物流转运中 */
    public static final int STATUS_IN_TRANSIT = 5;
}
