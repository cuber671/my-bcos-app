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
 * 物流轨迹记录实体类
 *
 * 对应数据库表: t_logistics_track
 *
 * 记录物流运输过程中的轨迹信息，用于追踪货物位置和检测偏航
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Data
@TableName("t_logistics_track")
public class LogisticsTrack {

    /**
     * 轨迹记录ID - 雪花算法生成
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 委派单编号
     */
    @TableField("voucher_no")
    private String voucherNo;

    /**
     * 纬度
     */
    @TableField("latitude")
    private BigDecimal latitude;

    /**
     * 经度
     */
    @TableField("longitude")
    private BigDecimal longitude;

    /**
     * 位置名称
     */
    @TableField("location_name")
    private String locationName;

    /**
     * 位置描述
     */
    @TableField("location_desc")
    private String locationDesc;

    /**
     * 状态：1-已提货, 2-运输中, 3-已到达
     */
    @TableField("status")
    private Integer status;

    /**
     * 偏离距离(米)
     */
    @TableField("deviation_distance")
    private BigDecimal deviationDistance;

    /**
     * 是否偏航：0-否, 1-是
     */
    @TableField("is_deviation")
    private Integer isDeviation;

    /**
     * 事件时间
     */
    @TableField("event_time")
    private LocalDateTime eventTime;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    // ==================== 状态常量 ====================

    /**
     * 状态 - 已提货
     */
    public static final int STATUS_PICKED_UP = 1;

    /**
     * 状态 - 运输中
     */
    public static final int STATUS_IN_TRANSIT = 2;

    /**
     * 状态 - 已到达
     */
    public static final int STATUS_ARRIVED = 3;

    // ==================== 偏航常量 ====================

    /**
     * 未偏航
     */
    public static final int DEVIATION_NO = 0;

    /**
     * 偏航
     */
    public static final int DEVIATION_YES = 1;

    // ==================== 便捷方法 ====================

    /**
     * 判断是否偏航
     */
    public boolean isDeviation() {
        return DEVIATION_YES == this.isDeviation;
    }

    /**
     * 获取状态描述
     */
    public String getStatusDesc() {
        switch (this.status) {
            case STATUS_PICKED_UP: return "已提货";
            case STATUS_IN_TRANSIT: return "运输中";
            case STATUS_ARRIVED: return "已到达";
            default: return "未知";
        }
    }
}
