package com.fisco.app.entity.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 用户活动日志实体类
 * 记录用户的登录、操作等活动记录
 */
@Data
@Entity
@Table(name = "user_activity", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_activity_type", columnList = "activity_type"),
    @Index(name = "idx_created_at", columnList = "created_at"),
    @Index(name = "idx_ip_address", columnList = "ip_address")
})
@ApiModel(value = "用户活动日志", description = "用户活动日志实体")
@Schema(name = "用户活动日志")
public class UserActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "主键ID", hidden = true)
    private Long id;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false, length = 36)
    @ApiModelProperty(value = "用户ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
    private String userId;

    /**
     * 用户名（冗余字段）
     */
    @Column(name = "username", length = 50)
    @ApiModelProperty(value = "用户名", example = "zhangsan")
    private String username;

    /**
     * 真实姓名（冗余字段）
     */
    @Column(name = "real_name", length = 100)
    @ApiModelProperty(value = "真实姓名", example = "张三")
    private String realName;

    /**
     * 活动类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 30)
    @ApiModelProperty(value = "活动类型", required = true, notes = "LOGIN-登录, LOGOUT-登出, CREATE-创建, UPDATE-更新, DELETE-删除, QUERY-查询, EXPORT-导出, IMPORT-导入, APPROVE-审核, REJECT-拒绝")
    private ActivityType activityType;

    /**
     * 活动描述
     */
    @Column(name = "description", nullable = false, length = 500)
    @ApiModelProperty(value = "活动描述", required = true, example = "用户登录系统")
    private String description;

    /**
     * 操作模块
     */
    @Column(name = "module", length = 50)
    @ApiModelProperty(value = "操作模块", example = "USER", notes = "如：USER, BILL, RECEIVABLE, WAREHOUSE_RECEIPT, ENTERPRISE")
    private String module;

    /**
     * 操作结果
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "result", length = 20)
    @ApiModelProperty(value = "操作结果", example = "SUCCESS", notes = "SUCCESS-成功, FAILURE-失败")
    private ActivityResult result = ActivityResult.SUCCESS;

    /**
     * 请求方法
     */
    @Column(name = "request_method", length = 10)
    @ApiModelProperty(value = "请求方法", example = "POST")
    private String requestMethod;

    /**
     * 请求URL
     */
    @Column(name = "request_url", length = 500)
    @ApiModelProperty(value = "请求URL", example = "/api/users/login")
    private String requestUrl;

    /**
     * 请求IP地址
     */
    @Column(name = "ip_address", length = 50)
    @ApiModelProperty(value = "请求IP地址", example = "192.168.1.100")
    private String ipAddress;

    /**
     * User-Agent
     */
    @Column(name = "user_agent", length = 500)
    @ApiModelProperty(value = "用户代理", example = "Mozilla/5.0...")
    private String userAgent;

    /**
     * 浏览器类型
     */
    @Column(name = "browser", length = 50)
    @ApiModelProperty(value = "浏览器类型", example = "Chrome")
    private String browser;

    /**
     * 操作系统
     */
    @Column(name = "os", length = 50)
    @ApiModelProperty(value = "操作系统", example = "Windows 10")
    private String os;

    /**
     * 设备类型
     */
    @Column(name = "device", length = 50)
    @ApiModelProperty(value = "设备类型", example = "PC")
    private String device;

    /**
     * 位置信息（可选）
     */
    @Column(name = "location", length = 200)
    @ApiModelProperty(value = "位置信息", example = "中国 北京")
    private String location;

    /**
     * 失败原因
     */
    @Column(name = "failure_reason", columnDefinition = "TEXT")
    @ApiModelProperty(value = "失败原因", notes = "操作失败时记录原因")
    private String failureReason;

    /**
     * 操作时长（毫秒）
     */
    @Column(name = "duration")
    @ApiModelProperty(value = "操作时长（毫秒）", example = "150")
    private Long duration;

    /**
     * 额外数据（JSON格式）
     */
    @Column(name = "extra_data", columnDefinition = "TEXT")
    @ApiModelProperty(value = "额外数据", notes = "JSON格式，记录额外的活动信息")
    private String extraData;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @ApiModelProperty(value = "创建时间", required = true, example = "2024-01-01T10:00:00")
    private LocalDateTime createdAt;

    /**
     * 活动类型枚举
     */
    public enum ActivityType {
        LOGIN,      // 登录
        LOGOUT,     // 登出
        CREATE,     // 创建
        UPDATE,     // 更新
        DELETE,     // 删除
        QUERY,      // 查询
        EXPORT,     // 导出
        IMPORT,     // 导入
        APPROVE,    // 审核
        REJECT,     // 拒绝
        UNLOCK,     // 解锁
        LOCK,       // 锁定
        RESET_PASSWORD, // 重置密码
        CHANGE_PASSWORD, // 修改密码
        OTHER       // 其他
    }

    /**
     * 操作结果枚举
     */
    public enum ActivityResult {
        SUCCESS,    // 成功
        FAILURE,    // 失败
        PARTIAL     // 部分成功
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (result == null) {
            result = ActivityResult.SUCCESS;
        }
    }
}