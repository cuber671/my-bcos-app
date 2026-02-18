package com.fisco.app.service.notification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fisco.app.dto.notification.NotificationBatchMarkRequest;
import com.fisco.app.dto.notification.NotificationCreateRequest;
import com.fisco.app.dto.notification.NotificationDTO;
import com.fisco.app.dto.notification.NotificationQueryRequest;
import com.fisco.app.dto.notification.NotificationStatisticsDTO;
import com.fisco.app.dto.notification.NotificationSubscriptionRequest;
import com.fisco.app.entity.notification.Notification;
import com.fisco.app.entity.notification.NotificationSendLog;
import com.fisco.app.entity.notification.NotificationSubscription;
import com.fisco.app.entity.notification.NotificationTemplate;
import com.fisco.app.exception.BusinessException;
import com.fisco.app.repository.notification.NotificationRepository;
import com.fisco.app.repository.notification.NotificationSendLogRepository;
import com.fisco.app.repository.notification.NotificationSubscriptionRepository;
import com.fisco.app.repository.notification.NotificationTemplateRepository;

import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 通知Service
 */
@Slf4j
@Service
@Api(tags = "通知服务")
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository notificationTemplateRepository;
    private final NotificationSubscriptionRepository notificationSubscriptionRepository;
    private final NotificationSendLogRepository notificationSendLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * 创建通知
     */
    @Transactional
    public Notification createNotification(NotificationCreateRequest request, String senderId) {
        log.info("创建通知: recipientId={}, type={}, title={}",
                request.getRecipientId(), request.getType(), request.getTitle());

        Notification notification = new Notification();
        notification.setId(UUID.randomUUID().toString());
        notification.setRecipientId(request.getRecipientId());
        notification.setRecipientType(request.getRecipientType().name());
        notification.setSenderId(senderId);
        notification.setSenderType(request.getSenderType() != null ? request.getSenderType().name() : Notification.SenderType.SYSTEM.name());
        notification.setType(request.getType().name());
        notification.setCategory(request.getCategory());
        notification.setTitle(request.getTitle());
        notification.setContent(request.getContent());
        notification.setPriority(request.getPriority() != null ? request.getPriority().name() : Notification.NotificationPriority.NORMAL.name());
        notification.setStatus(Notification.NotificationStatus.UNREAD.name());
        notification.setActionType(request.getActionType());
        notification.setActionUrl(request.getActionUrl());
        notification.setBusinessType(request.getBusinessType());
        notification.setBusinessId(request.getBusinessId());
        notification.setExpireAt(request.getExpireAt());
        notification.setIsSent(false);

        // 转换actionParams和extraData为JSON字符串
        if (request.getActionParams() != null) {
            try {
                notification.setActionParams(objectMapper.writeValueAsString(request.getActionParams()));
            } catch (JsonProcessingException e) {
                log.warn("转换actionParams为JSON失败", e);
            }
        }
        if (request.getExtraData() != null) {
            try {
                notification.setExtraData(objectMapper.writeValueAsString(request.getExtraData()));
            } catch (JsonProcessingException e) {
                log.warn("转换extraData为JSON失败", e);
            }
        }

        notification = notificationRepository.save(notification);

        // 如果需要立即发送
        if (Boolean.TRUE.equals(request.getSendImmediately())) {
            sendNotification(notification.getId());
        }

        log.info("通知创建成功: id={}", notification.getId());
        return notification;
    }

    /**
     * 发送通知
     */
    @Async
    @Transactional
    public void sendNotification(String notificationId) {
        if (notificationId == null) {
            throw new IllegalArgumentException("通知ID不能为空");
        }
        log.info("发送通知: notificationId={}", notificationId);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException("通知不存在"));

        try {
            // 检查用户订阅偏好
            List<NotificationSubscription> subscriptions = notificationSubscriptionRepository
                    .findByUserIdAndIsSubscribed(notification.getRecipientId(), true);

            boolean shouldSendInApp = true; // 默认发送应用内通知
            boolean shouldSendEmail = false;
            boolean shouldSendSms = false;
            boolean shouldSendPush = false;

            for (NotificationSubscription subscription : subscriptions) {
                if (subscription.getNotificationType().equals(notification.getType())) {
                    shouldSendEmail = Boolean.TRUE.equals(subscription.getNotifyEmail());
                    shouldSendSms = Boolean.TRUE.equals(subscription.getNotifySms());
                    shouldSendPush = Boolean.TRUE.equals(subscription.getNotifyPush());
                    shouldSendInApp = Boolean.TRUE.equals(subscription.getNotifyInApp());
                    break;
                }
            }

            // 发送应用内通知（默认总是发送）
            if (shouldSendInApp) {
                createSendLog(notificationId, notification.getRecipientId(), NotificationSendLog.SendChannel.IN_APP.name());
            }

            // NOTE: 发送邮件通知
            if (shouldSendEmail) {
                createSendLog(notificationId, notification.getRecipientId(), NotificationSendLog.SendChannel.EMAIL.name());
            }

            // NOTE: 发送短信通知
            if (shouldSendSms) {
                createSendLog(notificationId, notification.getRecipientId(), NotificationSendLog.SendChannel.SMS.name());
            }

            // NOTE: 发送推送通知
            if (shouldSendPush) {
                createSendLog(notificationId, notification.getRecipientId(), NotificationSendLog.SendChannel.PUSH.name());
            }

            // 更新通知状态为已发送
            notification.setIsSent(true);
            notification.setSentAt(LocalDateTime.now());
            notificationRepository.save(notification);

            log.info("通知发送成功: notificationId={}", notificationId);
        } catch (Exception e) {
            log.error("发送通知失败: notificationId={}", notificationId, e);
            throw new BusinessException("发送通知失败: " + e.getMessage());
        }
    }

    /**
     * 创建发送日志
     */
    private void createSendLog(String notificationId, String recipientId, String channel) {
        NotificationSendLog log = new NotificationSendLog();
        log.setId(UUID.randomUUID().toString());
        log.setNotificationId(notificationId);
        log.setRecipientId(recipientId);
        log.setChannel(channel);
        log.setStatus(NotificationSendLog.SendStatus.SUCCESS.name());
        log.setSentAt(LocalDateTime.now());
        notificationSendLogRepository.save(log);
    }

    /**
     * 使用模板创建通知
     */
    @Transactional
    public Notification createNotificationFromTemplate(String templateCode, String recipientId,
                                                       Map<String, Object> params) {
        log.info("使用模板创建通知: templateCode={}, recipientId={}", templateCode, recipientId);

        NotificationTemplate template = notificationTemplateRepository.findByCode(templateCode)
                .orElseThrow(() -> new BusinessException("通知模板不存在: " + templateCode));

        if (!Boolean.TRUE.equals(template.getIsEnabled())) {
            throw new BusinessException("通知模板未启用: " + templateCode);
        }

        // 替换模板中的占位符
        String title = replacePlaceholders(template.getTitleTemplate(), params);
        String content = replacePlaceholders(template.getContentTemplate(), params);

        NotificationCreateRequest request = new NotificationCreateRequest();
        request.setRecipientId(recipientId);
        request.setRecipientType(Notification.RecipientType.USER);
        request.setType(Notification.NotificationType.valueOf(template.getType()));
        request.setCategory(template.getCategory());
        request.setTitle(title);
        request.setContent(content);
        request.setPriority(Notification.NotificationPriority.valueOf(template.getPriority()));
        request.setActionType(template.getActionType());

        return createNotification(request, null);
    }

    /**
     * 替换模板中的占位符
     */
    private String replacePlaceholders(String template, Map<String, Object> params) {
        if (template == null) {
            return null;
        }
        String result = template;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            result = result.replace(placeholder, value);
        }
        return result;
    }

    /**
     * 查询通知列表
     */
    public Page<NotificationDTO> queryNotifications(NotificationQueryRequest request) {
        log.info("查询通知列表: recipientId={}, type={}, status={}",
                request.getRecipientId(), request.getType(), request.getStatus());

        // 构建排序条件
        Sort sort = Sort.by(
                "DESC".equalsIgnoreCase(request.getSortDirection()) ? Sort.Direction.DESC : Sort.Direction.ASC,
                request.getSortBy()
        );
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        // 执行查询
        Page<Notification> notifications = notificationRepository.findByConditions(
                request.getRecipientId(),
                request.getRecipientType(),
                request.getType(),
                request.getStatus(),
                request.getPriority(),
                request.getBusinessType(),
                request.getCreatedAtStart(),
                request.getCreatedAtEnd(),
                request.getTitleKeyword(),
                request.getContentKeyword(),
                pageable
        );

        // 转换为DTO
        return notifications.map(this::convertToDTO);
    }

    /**
     * 查询通知详情
     */
    public NotificationDTO getNotificationDetail(String notificationId) {
        if (notificationId == null) {
            throw new IllegalArgumentException("通知ID不能为空");
        }
        log.info("查询通知详情: notificationId={}", notificationId);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException("通知不存在"));

        return convertToDTO(notification);
    }

    /**
     * 标记通知为已读
     */
    @Transactional
    public void markAsRead(String notificationId) {
        if (notificationId == null) {
            throw new IllegalArgumentException("通知ID不能为空");
        }
        log.info("标记通知为已读: notificationId={}", notificationId);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException("通知不存在"));

        if (notification.getStatus().equals(Notification.NotificationStatus.UNREAD.name())) {
            notification.setStatus(Notification.NotificationStatus.READ.name());
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
            log.info("通知已标记为已读: notificationId={}", notificationId);
        }
    }

    /**
     * 批量标记通知
     */
    @Transactional
    @SuppressWarnings("null")
    public void batchMarkNotifications(NotificationBatchMarkRequest request) {
        log.info("批量标记通知: count={}, targetStatus={}",
                request.getNotificationIds().size(), request.getTargetStatus());

        List<Notification> notifications = notificationRepository.findByIdIn(request.getNotificationIds());

        for (Notification notification : notifications) {
            notification.setStatus(request.getTargetStatus().name());
            if (request.getTargetStatus() == Notification.NotificationStatus.READ) {
                notification.setReadAt(LocalDateTime.now());
            }
        }

        Iterable<Notification> notificationsToSave = notifications;
        notificationRepository.saveAll(notificationsToSave);
        log.info("批量标记完成: count={}", notifications.size());
    }

    /**
     * 标记所有通知为已读
     */
    @Transactional
    @SuppressWarnings("null")
    public void markAllAsRead(String recipientId) {
        if (recipientId == null) {
            throw new IllegalArgumentException("接收者ID不能为空");
        }
        log.info("标记所有通知为已读: recipientId={}", recipientId);

        List<Notification> unreadNotifications = notificationRepository
                .findByRecipientIdAndStatus(recipientId, Notification.NotificationStatus.UNREAD);

        for (Notification notification : unreadNotifications) {
            notification.setStatus(Notification.NotificationStatus.READ.name());
            notification.setReadAt(LocalDateTime.now());
        }

        Iterable<Notification> unreadNotificationsToSave = unreadNotifications;
        notificationRepository.saveAll(unreadNotificationsToSave);
        log.info("标记所有通知为已读完成: count={}", unreadNotifications.size());
    }

    /**
     * 删除通知
     */
    @Transactional
    public void deleteNotification(String notificationId) {
        if (notificationId == null) {
            throw new IllegalArgumentException("通知ID不能为空");
        }
        log.info("删除通知: notificationId={}", notificationId);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException("通知不存在"));

        notification.setStatus(Notification.NotificationStatus.DELETED.name());
        notificationRepository.save(notification);

        log.info("通知已删除: notificationId={}", notificationId);
    }

    /**
     * 批量删除通知
     */
    @Transactional
    @SuppressWarnings("null")
    public void batchDeleteNotifications(List<String> notificationIds) {
        log.info("批量删除通知: count={}", notificationIds.size());

        List<Notification> notifications = notificationRepository.findByIdIn(notificationIds);

        for (Notification notification : notifications) {
            notification.setStatus(Notification.NotificationStatus.DELETED.name());
        }

        Iterable<Notification> notificationsToDelete = notifications;
        notificationRepository.saveAll(notificationsToDelete);
        log.info("批量删除完成: count={}", notifications.size());
    }

    /**
     * 获取通知统计
     */
    public NotificationStatisticsDTO getNotificationStatistics(String recipientId) {
        log.info("获取通知统计: recipientId={}", recipientId);

        NotificationStatisticsDTO statistics = new NotificationStatisticsDTO();

        // 总数
        statistics.setTotalCount(notificationRepository.countByRecipientId(recipientId));

        // 各状态统计
        statistics.setUnreadCount(notificationRepository.countByRecipientIdAndStatus(
                recipientId, Notification.NotificationStatus.UNREAD));
        statistics.setReadCount(notificationRepository.countByRecipientIdAndStatus(
                recipientId, Notification.NotificationStatus.READ));
        statistics.setArchivedCount(notificationRepository.countByRecipientIdAndStatus(
                recipientId, Notification.NotificationStatus.ARCHIVED));
        statistics.setDeletedCount(notificationRepository.countByRecipientIdAndStatus(
                recipientId, Notification.NotificationStatus.DELETED));

        // 各优先级统计
        statistics.setUrgentCount(notificationRepository.findByPriorityAndRecipientId(
                Notification.NotificationPriority.URGENT, recipientId).stream()
                .filter(n -> !n.getStatus().equals(Notification.NotificationStatus.DELETED.name()))
                .count());
        statistics.setHighPriorityCount(notificationRepository.findByPriorityAndRecipientId(
                Notification.NotificationPriority.HIGH, recipientId).stream()
                .filter(n -> !n.getStatus().equals(Notification.NotificationStatus.DELETED.name()))
                .count());
        statistics.setNormalPriorityCount(notificationRepository.findByPriorityAndRecipientId(
                Notification.NotificationPriority.NORMAL, recipientId).stream()
                .filter(n -> !n.getStatus().equals(Notification.NotificationStatus.DELETED.name()))
                .count());
        statistics.setLowPriorityCount(notificationRepository.findByPriorityAndRecipientId(
                Notification.NotificationPriority.LOW, recipientId).stream()
                .filter(n -> !n.getStatus().equals(Notification.NotificationStatus.DELETED.name()))
                .count());

        // 各类型统计
        statistics.setSystemNotificationCount(notificationRepository.findByTypeAndRecipientId(
                Notification.NotificationType.SYSTEM, recipientId).stream()
                .filter(n -> !n.getStatus().equals(Notification.NotificationStatus.DELETED.name()))
                .count());
        statistics.setApprovalNotificationCount(notificationRepository.findByTypeAndRecipientId(
                Notification.NotificationType.APPROVAL, recipientId).stream()
                .filter(n -> !n.getStatus().equals(Notification.NotificationStatus.DELETED.name()))
                .count());
        statistics.setRiskNotificationCount(notificationRepository.findByTypeAndRecipientId(
                Notification.NotificationType.RISK, recipientId).stream()
                .filter(n -> !n.getStatus().equals(Notification.NotificationStatus.DELETED.name()))
                .count());
        statistics.setWarningNotificationCount(notificationRepository.findByTypeAndRecipientId(
                Notification.NotificationType.WARNING, recipientId).stream()
                .filter(n -> !n.getStatus().equals(Notification.NotificationStatus.DELETED.name()))
                .count());
        statistics.setBusinessNotificationCount(notificationRepository.findByTypeAndRecipientId(
                Notification.NotificationType.BUSINESS, recipientId).stream()
                .filter(n -> !n.getStatus().equals(Notification.NotificationStatus.DELETED.name()))
                .count());
        statistics.setReminderNotificationCount(notificationRepository.findByTypeAndRecipientId(
                Notification.NotificationType.REMINDER, recipientId).stream()
                .filter(n -> !n.getStatus().equals(Notification.NotificationStatus.DELETED.name()))
                .count());

        return statistics;
    }

    /**
     * 更新通知订阅
     */
    @Transactional
    @SuppressWarnings("null")
    public void updateSubscription(String userId, NotificationSubscriptionRequest request) {
        log.info("更新通知订阅: userId={}", userId);

        // 订阅指定的类型
        if (request.getSubscribedTypes() != null) {
            for (String type : request.getSubscribedTypes()) {
                NotificationSubscription subscription = notificationSubscriptionRepository
                        .findByUserIdAndNotificationType(userId, type)
                        .orElse(new NotificationSubscription());

                if (subscription.getId() == null) {
                    subscription.setId(UUID.randomUUID().toString());
                    subscription.setUserId(userId);
                    subscription.setNotificationType(type);
                }

                subscription.setIsSubscribed(true);
                notificationSubscriptionRepository.save(subscription);
            }
        }

        // 取消订阅指定的类型
        if (request.getUnsubscribedTypes() != null) {
            for (String type : request.getUnsubscribedTypes()) {
                notificationSubscriptionRepository
                        .findByUserIdAndNotificationType(userId, type)
                        .ifPresent(subscription -> {
                            subscription.setIsSubscribed(false);
                            notificationSubscriptionRepository.save(subscription);
                        });
            }
        }

        // 更新所有订阅的通知方式偏好
        List<NotificationSubscription> subscriptions = notificationSubscriptionRepository.findByUserId(userId);

        for (NotificationSubscription subscription : subscriptions) {
            if (request.getNotifyEmail() != null) {
                subscription.setNotifyEmail(request.getNotifyEmail());
            }
            if (request.getNotifySms() != null) {
                subscription.setNotifySms(request.getNotifySms());
            }
            if (request.getNotifyPush() != null) {
                subscription.setNotifyPush(request.getNotifyPush());
            }
            if (request.getNotifyInApp() != null) {
                subscription.setNotifyInApp(request.getNotifyInApp());
            }
        }

        Iterable<NotificationSubscription> subscriptionsToSave = subscriptions;
        notificationSubscriptionRepository.saveAll(subscriptionsToSave);
        log.info("通知订阅更新完成: userId={}", userId);
    }

    /**
     * 查询用户订阅
     */
    public List<NotificationSubscription> getUserSubscriptions(String userId) {
        log.info("查询用户订阅: userId={}", userId);
        return notificationSubscriptionRepository.findByUserId(userId);
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

        // 转换JSON字符串为Map
        try {
            if (notification.getActionParams() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> actionParamsMap = objectMapper.readValue(notification.getActionParams(), Map.class);
                dto.setActionParams(actionParamsMap);
            }
            if (notification.getExtraData() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> extraDataMap = objectMapper.readValue(notification.getExtraData(), Map.class);
                dto.setExtraData(extraDataMap);
            }
        } catch (JsonProcessingException e) {
            log.warn("转换JSON为Map失败", e);
        }

        return dto;
    }
}
