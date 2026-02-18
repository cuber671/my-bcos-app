package com.fisco.app.repository.notification;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fisco.app.entity.notification.NotificationSubscription;

/**
 * 通知订阅Repository
 */
@Repository
public interface NotificationSubscriptionRepository extends JpaRepository<NotificationSubscription, String> {

    /**
     * 根据用户ID查找所有订阅
     */
    List<NotificationSubscription> findByUserId(String userId);

    /**
     * 根据用户ID和通知类型查找订阅
     */
    Optional<NotificationSubscription> findByUserIdAndNotificationType(String userId, String notificationType);

    /**
     * 根据用户ID查找已订阅的类型
     */
    @Query("SELECT ns.notificationType FROM NotificationSubscription ns WHERE ns.userId = :userId AND ns.isSubscribed = true")
    List<String> findSubscribedTypesByUserId(@Param("userId") String userId);

    /**
     * 根据用户ID和订阅状态查找订阅
     */
    List<NotificationSubscription> findByUserIdAndIsSubscribed(String userId, Boolean isSubscribed);

    /**
     * 根据通知类型查找所有订阅该类型的用户
     */
    List<NotificationSubscription> findByNotificationTypeAndIsSubscribed(String notificationType, Boolean isSubscribed);

    /**
     * 删除用户的所有订阅
     */
    void deleteByUserId(String userId);
}
