package com.fisco.app.Modules.Warehouse.Entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 仓库信息实体类
 *
 * 对应数据库表: t_warehouse
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Data
@TableName("t_warehouse")
public class Warehouse {

    /**
     * 仓库ID - 自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属监管方企业ID
     */
    @TableField("ent_id")
    private Long entId;

    /**
     * 仓库名称
     */
    private String name;

    /**
     * 仓库地址
     */
    private String address;

    /**
     * 现场负责人
     */
    @TableField("contact_user")
    private String contactUser;

    /**
     * 联系电话
     */
    @TableField("contact_phone")
    private String contactPhone;

    /**
     * 仓库状态: 1-正常营业, 2-暂停接单, 3-已关闭
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

    /** 正常营业 */
    public static final int STATUS_NORMAL = 1;

    /** 暂停接单 */
    public static final int STATUS_PAUSED = 2;

    /** 已关闭 */
    public static final int STATUS_CLOSED = 3;
}
