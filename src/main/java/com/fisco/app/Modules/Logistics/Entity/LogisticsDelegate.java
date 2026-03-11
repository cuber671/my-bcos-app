package com.fisco.app.Modules.Logistics.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 电子物流委派单实体类
 *
 * 对应数据库表: t_logistics_delegate
 *
 * 记录物流委派单的完整信息，支持三种业务场景：
 * 1. 直接移库（从仓库A到仓库B）
 * 2. 转让后移库（先卖掉，买家再换仓库）
 * 3. 发往指定仓库（企业发货入库）
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Data
@TableName("t_logistics_delegate")
public class LogisticsDelegate {

    /**
     * 委派单唯一主键ID - 雪花算法生成
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 业务展示编号（如：DPDO20260220001）
     */
    @TableField("voucher_no")
    private String voucherNo;

    /**
     * 场景类型：1-直接移库；2-转让后移库；3-发货入库
     */
    @TableField("business_scene")
    private Integer businessScene;

    /**
     * 关联原仓单ID：场景1和2必填
     */
    @TableField("receipt_id")
    private Long receiptId;

    /**
     * 背书ID：场景2必填，用于核实买家身份
     */
    @TableField("endorse_id")
    private Long endorseId;

    /**
     * 本次运输数量：核心字段！记录本次拉走的具体数值
     */
    @TableField("transport_quantity")
    private BigDecimal transportQuantity;

    /**
     * 计量单位：如"吨"、"千克"、"件"
     */
    @TableField("unit")
    private String unit;

    /**
     * 授权企业：发起物流申请的货主/买方ID
     */
    @TableField("owner_ent_id")
    private Long ownerEntId;

    /**
     * 承运企业：指定的物流方公司ID
     */
    @TableField("carrier_ent_id")
    private Long carrierEntId;

    /**
     * 起运地ID：货物目前存放的仓库
     */
    @TableField("source_wh_id")
    private Long sourceWhId;

    /**
     * 目的地仓库ID：发往监管仓时必填
     */
    @TableField("target_wh_id")
    private Long targetWhId;

    /**
     * 到货处理动作：1-生成新仓单；2-并入已有仓单
     */
    @TableField("action_on_arrival")
    private Integer actionOnArrival;

    /**
     * 目标仓单ID：当执行增量入库时，指定并入哪张老单
     */
    @TableField("target_receipt_id")
    private Long targetReceiptId;

    /**
     * 司机ID
     */
    @TableField("driver_id")
    private String driverId;

    /**
     * 司机姓名
     */
    @TableField("driver_name")
    private String driverName;

    /**
     * 车牌号
     */
    @TableField("vehicle_no")
    private String vehicleNo;

    /**
     * 提货授权码
     */
    @TableField("auth_code")
    private String authCode;

    /**
     * 提货二维码（动态加密）
     */
    @TableField("pickup_qr_code")
    private String pickupQrCode;

    /**
     * 货主数字签名
     */
    @TableField("auth_signature")
    private String authSignature;

    /**
     * 状态：1-待指派, 2-已调度, 3-运输中, 4-已交付, 5-已失效
     */
    @TableField("status")
    private Integer status;

    /**
     * 凭证有效期
     */
    @TableField("valid_until")
    private LocalDateTime validUntil;

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
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 最后修改时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ==================== 业务场景常量 ====================

    /**
     * 业务场景 - 直接移库
     */
    public static final int SCENE_DIRECT_TRANSFER = 1;

    /**
     * 业务场景 - 转让后移库
     */
    public static final int SCENE_TRANSFER_THEN_TRANSFER = 2;

    /**
     * 业务场景 - 发货入库
     */
    public static final int SCENE_DELIVERY_TO_WAREHOUSE = 3;

    // ==================== 状态常量 ====================

    /**
     * 状态 - 待指派
     */
    public static final int STATUS_PENDING = 1;

    /**
     * 状态 - 已调度
     */
    public static final int STATUS_ASSIGNED = 2;

    /**
     * 状态 - 运输中
     */
    public static final int STATUS_IN_TRANSIT = 3;

    /**
     * 状态 - 已交付
     */
    public static final int STATUS_DELIVERED = 4;

    /**
     * 状态 - 已失效
     */
    public static final int STATUS_INVALID = 5;

    // ==================== 到货处理动作常量 ====================

    /**
     * 到货处理动作 - 生成新仓单
     */
    public static final int ACTION_CREATE_NEW_RECEIPT = 1;

    /**
     * 到货处理动作 - 并入已有仓单
     */
    public static final int ACTION_MERGE_EXISTING_RECEIPT = 2;

    // ==================== 便捷方法 ====================

    /**
     * 判断是否为待指派状态
     */
    public boolean isPending() {
        return STATUS_PENDING == this.status;
    }

    /**
     * 判断是否为运输中状态
     */
    public boolean isInTransit() {
        return STATUS_IN_TRANSIT == this.status;
    }

    /**
     * 判断是否为已交付状态
     */
    public boolean isDelivered() {
        return STATUS_DELIVERED == this.status;
    }

    /**
     * 判断是否已失效
     */
    public boolean isInvalid() {
        return STATUS_INVALID == this.status;
    }

    /**
     * 判断是否在有效期内
     */
    public boolean isValid() {
        return this.validUntil != null && LocalDateTime.now().isBefore(this.validUntil);
    }

    /**
     * 获取状态描述
     */
    public String getStatusDesc() {
        switch (this.status) {
            case STATUS_PENDING: return "待指派";
            case STATUS_ASSIGNED: return "已调度";
            case STATUS_IN_TRANSIT: return "运输中";
            case STATUS_DELIVERED: return "已交付";
            case STATUS_INVALID: return "已失效";
            default: return "未知";
        }
    }

    /**
     * 获取业务场景描述
     */
    public String getBusinessSceneDesc() {
        switch (this.businessScene) {
            case SCENE_DIRECT_TRANSFER: return "直接移库";
            case SCENE_TRANSFER_THEN_TRANSFER: return "转让后移库";
            case SCENE_DELIVERY_TO_WAREHOUSE: return "发货入库";
            default: return "未知";
        }
    }
}
