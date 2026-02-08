package com.fisco.app.repository.notification;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fisco.app.entity.notification.NotificationSendLog;

/**
 * 通知发送日志Repository
 */
@Repository
public interface NotificationSendLogRepository extends JpaRepository<NotificationSendLog, String> {

    /**
     * 根据通知ID查找发送日志
     */
    List<NotificationSendLog> findByNotificationId(String notificationId);

    /**
     * 根据接收者ID查找发送日志
     */
    List<NotificationSendLog> findByRecipientId(String recipientId);

    /**
     * 根据通知ID和渠道查找发送日志
     */
    List<NotificationSendLog> findByNotificationIdAndChannel(String notificationId, String channel);

    /**
     * 根据通知ID和状态查找发送日志
     */
    List<NotificationSendLog> findByNotificationIdAndStatus(String notificationId, String status);

    /**
     * 根据状态查找发送日志
     */
    List<NotificationSendLog> findByStatus(String status);

    /**
     * 统计通知的发送次数
     */
    Long countByNotificationId(String notificationId);

    /**
     * 统计通知的成功发送次数
     */
    Long countByNotificationIdAndStatus(String notificationId, String status);

    /**
     * 查找失败的发送记录
     */
    List<NotificationSendLog> findByStatusAndRetryCountLessThan(String status, Integer retryCount);
}
