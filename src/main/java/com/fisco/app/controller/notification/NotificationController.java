package com.fisco.app.controller.notification;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fisco.app.dto.notification.NotificationBatchMarkRequest;
import com.fisco.app.dto.notification.NotificationCreateRequest;
import com.fisco.app.dto.notification.NotificationDTO;
import com.fisco.app.dto.notification.NotificationQueryRequest;
import com.fisco.app.dto.notification.NotificationStatisticsDTO;
import com.fisco.app.dto.notification.NotificationSubscriptionRequest;
import com.fisco.app.entity.notification.Notification;
import com.fisco.app.service.notification.NotificationService;
import com.fisco.app.vo.Result;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 通知管理Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Api(tags = "通知管理")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * POST /api/notifications
     * 创建通知
     */
    @PostMapping
    @ApiOperation(value = "创建通知", notes = "创建新的通知消息")
    public Result<NotificationDTO> createNotification(
            @Valid @RequestBody NotificationCreateRequest request,
            Authentication authentication) {
        log.info("创建通知: recipientId={}, type={}, title={}",
                request.getRecipientId(), request.getType(), request.getTitle());

        String senderId = authentication.getName();
        Notification notification = notificationService.createNotification(request, senderId);

        return Result.success("通知创建成功", convertToDTO(notification));
    }

    /**
     * POST /api/notifications/template/{templateCode}
     * 使用模板创建通知
     */
    @PostMapping("/template/{templateCode}")
    @ApiOperation(value = "使用模板创建通知", notes = "根据预设模板创建通知")
    public Result<NotificationDTO> createNotificationFromTemplate(
            @ApiParam(value = "模板代码", required = true) @PathVariable @NotBlank String templateCode,
            @RequestBody Map<String, Object> params,
            Authentication authentication) {
        log.info("使用模板创建通知: templateCode={}", templateCode);

        Notification notification = notificationService.createNotificationFromTemplate(
                templateCode, authentication.getName(), params);

        return Result.success("通知创建成功", convertToDTO(notification));
    }

    /**
     * GET /api/notifications
     * 查询通知列表
     */
    @GetMapping
    @ApiOperation(value = "查询通知列表", notes = "根据条件查询通知列表，支持分页")
    public Result<Page<NotificationDTO>> queryNotifications(
            @Valid NotificationQueryRequest request,
            Authentication authentication) {
        log.info("查询通知列表: recipientId={}", request.getRecipientId());

        // 如果没有指定接收者ID，使用当前用户
        if (request.getRecipientId() == null) {
            request.setRecipientId(authentication.getName());
        }

        Page<NotificationDTO> notifications = notificationService.queryNotifications(request);

        return Result.success("查询成功", notifications);
    }

    /**
     * GET /api/notifications/{notificationId}
     * 查询通知详情
     */
    @GetMapping("/{notificationId}")
    @ApiOperation(value = "查询通知详情", notes = "获取通知的详细信息")
    public Result<NotificationDTO> getNotificationDetail(
            @ApiParam(value = "通知ID", required = true) @PathVariable @NotBlank String notificationId) {
        log.info("查询通知详情: notificationId={}", notificationId);

        NotificationDTO notification = notificationService.getNotificationDetail(notificationId);

        return Result.success("查询成功", notification);
    }

    /**
     * PUT /api/notifications/{notificationId}/read
     * 标记通知为已读
     */
    @PutMapping("/{notificationId}/read")
    @ApiOperation(value = "标记通知为已读", notes = "将指定通知标记为已读状态")
    public Result<Void> markAsRead(
            @ApiParam(value = "通知ID", required = true) @PathVariable @NotBlank String notificationId) {
        log.info("标记通知为已读: notificationId={}", notificationId);

        notificationService.markAsRead(notificationId);

        return Result.success();
    }

    /**
     * PUT /api/notifications/batch/mark
     * 批量标记通知
     */
    @PutMapping("/batch/mark")
    @ApiOperation(value = "批量标记通知", notes = "批量标记通知状态（已读、归档、删除）")
    public Result<Void> batchMarkNotifications(
            @Valid @RequestBody NotificationBatchMarkRequest request) {
        log.info("批量标记通知: count={}, targetStatus={}",
                request.getNotificationIds().size(), request.getTargetStatus());

        notificationService.batchMarkNotifications(request);

        return Result.success();
    }

    /**
     * PUT /api/notifications/read-all
     * 标记所有通知为已读
     */
    @PutMapping("/read-all")
    @ApiOperation(value = "标记所有通知为已读", notes = "将当前用户的所有未读通知标记为已读")
    public Result<Void> markAllAsRead(Authentication authentication) {
        log.info("标记所有通知为已读: userId={}", authentication.getName());

        notificationService.markAllAsRead(authentication.getName());

        return Result.success();
    }

    /**
     * DELETE /api/notifications/{notificationId}
     * 删除通知
     */
    @DeleteMapping("/{notificationId}")
    @ApiOperation(value = "删除通知", notes = "删除指定通知（软删除）")
    public Result<Void> deleteNotification(
            @ApiParam(value = "通知ID", required = true) @PathVariable @NotBlank String notificationId) {
        log.info("删除通知: notificationId={}", notificationId);

        notificationService.deleteNotification(notificationId);

        return Result.success();
    }

    /**
     * DELETE /api/notifications/batch
     * 批量删除通知
     */
    @DeleteMapping("/batch")
    @ApiOperation(value = "批量删除通知", notes = "批量删除通知（软删除）")
    public Result<Void> batchDeleteNotifications(
            @RequestBody @NotEmpty List<@NotBlank String> notificationIds) {
        log.info("批量删除通知: count={}", notificationIds.size());

        notificationService.batchDeleteNotifications(notificationIds);

        return Result.success();
    }

    /**
     * GET /api/notifications/statistics
     * 获取通知统计
     */
    @GetMapping("/statistics")
    @ApiOperation(value = "获取通知统计", notes = "获取当前用户的通知统计信息")
    public Result<NotificationStatisticsDTO> getNotificationStatistics(
            Authentication authentication) {
        log.info("获取通知统计: userId={}", authentication.getName());

        NotificationStatisticsDTO statistics = notificationService
                .getNotificationStatistics(authentication.getName());

        return Result.success("查询成功", statistics);
    }

    /**
     * PUT /api/notifications/subscriptions
     * 更新通知订阅
     */
    @PutMapping("/subscriptions")
    @ApiOperation(value = "更新通知订阅", notes = "更新用户的通知订阅偏好")
    public Result<Void> updateSubscription(
            @Valid @RequestBody NotificationSubscriptionRequest request,
            Authentication authentication) {
        log.info("更新通知订阅: userId={}", authentication.getName());

        notificationService.updateSubscription(authentication.getName(), request);

        return Result.success();
    }

    /**
     * GET /api/notifications/subscriptions
     * 查询用户订阅
     */
    @GetMapping("/subscriptions")
    @ApiOperation(value = "查询用户订阅", notes = "获取用户的通知订阅设置")
    public Result<List<com.fisco.app.entity.notification.NotificationSubscription>> getUserSubscriptions(
            Authentication authentication) {
        log.info("查询用户订阅: userId={}", authentication.getName());

        List<com.fisco.app.entity.notification.NotificationSubscription> subscriptions =
                notificationService.getUserSubscriptions(authentication.getName());

        return Result.success("查询成功", subscriptions);
    }

    /**
     * 转换为DTO
     */
    private NotificationDTO convertToDTO(Notification notification) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(notification.getId());
        dto.setRecipientId(notification.getRecipientId());
        dto.setRecipientType(Notification.RecipientType.valueOf(notification.getRecipientType()));
        dto.setSenderId(notification.getSenderId());
        dto.setSenderType(notification.getSenderType() != null ?
                Notification.SenderType.valueOf(notification.getSenderType()) : null);
        dto.setType(Notification.NotificationType.valueOf(notification.getType()));
        dto.setCategory(notification.getCategory());
        dto.setTitle(notification.getTitle());
        dto.setContent(notification.getContent());
        dto.setPriority(Notification.NotificationPriority.valueOf(notification.getPriority()));
        dto.setStatus(Notification.NotificationStatus.valueOf(notification.getStatus()));
        dto.setActionType(notification.getActionType());
        dto.setActionUrl(notification.getActionUrl());
        dto.setBusinessType(notification.getBusinessType());
        dto.setBusinessId(notification.getBusinessId());
        dto.setIsSent(notification.getIsSent());
        dto.setSentAt(notification.getSentAt());
        dto.setReadAt(notification.getReadAt());
        dto.setExpireAt(notification.getExpireAt());
        dto.setCreatedAt(notification.getCreatedAt());
        dto.setUpdatedAt(notification.getUpdatedAt());
        return dto;
    }

    /**
     * 创建通知请求
     */
    @Data
    public static class CreateNotificationRequest {
        @NotBlank(message = "接收者ID不能为空")
        private String recipientId;

        @NotNull(message = "接收者类型不能为空")
        private Notification.RecipientType recipientType;

        private String senderId;

        private Notification.SenderType senderType;

        @NotNull(message = "通知类型不能为空")
        private Notification.NotificationType type;

        private String category;

        @NotBlank(message = "通知标题不能为空")
        private String title;

        @NotBlank(message = "通知内容不能为空")
        private String content;

        private Notification.NotificationPriority priority;

        private String actionType;

        private String actionUrl;

        private String businessType;

        private String businessId;
    }
}
