package com.fisco.app.repository.notification;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fisco.app.entity.notification.Notification;

/**
 * 通知Repository
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    /**
     * 根据接收者ID查找通知
     */
    List<Notification> findByRecipientId(String recipientId);

    /**
     * 根据接收者ID和状态查找通知
     */
    List<Notification> findByRecipientIdAndStatus(String recipientId, Notification.NotificationStatus status);

    /**
     * 根据接收者ID和状态分页查找通知
     */
    Page<Notification> findByRecipientIdAndStatus(String recipientId, Notification.NotificationStatus status, Pageable pageable);

    /**
     * 根据接收者ID分页查找通知
     */
    Page<Notification> findByRecipientId(String recipientId, Pageable pageable);

    /**
     * 统计接收者的未读通知数量
     */
    Long countByRecipientIdAndStatus(String recipientId, Notification.NotificationStatus status);

    /**
     * 统计接收者的通知总数
     */
    Long countByRecipientId(String recipientId);

    /**
     * 根据类型和接收者查找通知
     */
    List<Notification> findByTypeAndRecipientId(Notification.NotificationType type, String recipientId);

    /**
     * 根据优先级和接收者查找通知
     */
    List<Notification> findByPriorityAndRecipientId(Notification.NotificationPriority priority, String recipientId);

    /**
     * 根据业务类型和业务ID查找通知
     */
    List<Notification> findByBusinessTypeAndBusinessId(String businessType, String businessId);

    /**
     * 查找未读的紧急通知
     */
    @Query("SELECT n FROM Notification n WHERE n.recipientId = :recipientId AND n.status = 'UNREAD' AND n.priority = 'URGENT' ORDER BY n.createdAt DESC")
    List<Notification> findUnreadUrgentNotifications(@Param("recipientId") String recipientId);

    /**
     * 查找未读的高优先级通知
     */
    @Query("SELECT n FROM Notification n WHERE n.recipientId = :recipientId AND n.status = 'UNREAD' AND n.priority = 'HIGH' ORDER BY n.createdAt DESC")
    List<Notification> findUnreadHighPriorityNotifications(@Param("recipientId") String recipientId);

    /**
     * 根据接收者ID、类型和状态分页查找通知
     */
    Page<Notification> findByRecipientIdAndTypeAndStatus(String recipientId, Notification.NotificationType type, Notification.NotificationStatus status, Pageable pageable);

    /**
     * 根据多个ID查找通知
     */
    List<Notification> findByIdIn(List<String> ids);

    /**
     * 查找过期的通知
     */
    @Query("SELECT n FROM Notification n WHERE n.expireAt < :currentTime AND n.status != 'DELETED'")
    List<Notification> findExpiredNotifications(@Param("currentTime") LocalDateTime currentTime);

    /**
     * 统计各状态的通知数量
     */
    @Query("SELECT n.status, COUNT(n) FROM Notification n WHERE n.recipientId = :recipientId GROUP BY n.status")
    List<Object[]> countByStatusGroupBy(@Param("recipientId") String recipientId);

    /**
     * 统计各类型的通知数量
     */
    @Query("SELECT n.type, COUNT(n) FROM Notification n WHERE n.recipientId = :recipientId GROUP BY n.type")
    List<Object[]> countByTypeGroupBy(@Param("recipientId") String recipientId);

    /**
     * 统计各优先级的通知数量
     */
    @Query("SELECT n.priority, COUNT(n) FROM Notification n WHERE n.recipientId = :recipientId GROUP BY n.priority")
    List<Object[]> countByPriorityGroupBy(@Param("recipientId") String recipientId);

    /**
     * 删除指定时间之前的已删除通知
     */
    @Query("DELETE FROM Notification n WHERE n.status = 'DELETED' AND n.updatedAt < :beforeTime")
    void deleteOldDeletedNotifications(@Param("beforeTime") LocalDateTime beforeTime);

    /**
     * 根据业务类型和业务ID查找未读通知
     */
    @Query("SELECT n FROM Notification n WHERE n.businessType = :businessType AND n.businessId = :businessId AND n.status = 'UNREAD'")
    List<Notification> findUnreadByBusiness(@Param("businessType") String businessType, @Param("businessId") String businessId);

    /**
     * 根据标题关键字模糊查询
     */
    @Query("SELECT n FROM Notification n WHERE n.recipientId = :recipientId AND n.title LIKE %:keyword%")
    Page<Notification> findByRecipientIdAndTitleContaining(@Param("recipientId") String recipientId, @Param("keyword") String keyword, Pageable pageable);

    /**
     * 根据内容关键字模糊查询
     */
    @Query("SELECT n FROM Notification n WHERE n.recipientId = :recipientId AND n.content LIKE %:keyword%")
    Page<Notification> findByRecipientIdAndContentContaining(@Param("recipientId") String recipientId, @Param("keyword") String keyword, Pageable pageable);

    /**
     * 复杂查询
     */
    @Query("SELECT n FROM Notification n WHERE " +
           "(:recipientId IS NULL OR n.recipientId = :recipientId) AND " +
           "(:recipientType IS NULL OR n.recipientType = :recipientType) AND " +
           "(:type IS NULL OR n.type = :type) AND " +
           "(:status IS NULL OR n.status = :status) AND " +
           "(:priority IS NULL OR n.priority = :priority) AND " +
           "(:businessType IS NULL OR n.businessType = :businessType) AND " +
           "(:createdAtStart IS NULL OR n.createdAt >= :createdAtStart) AND " +
           "(:createdAtEnd IS NULL OR n.createdAt <= :createdAtEnd) AND " +
           "(:titleKeyword IS NULL OR n.title LIKE %:titleKeyword%) AND " +
           "(:contentKeyword IS NULL OR n.content LIKE %:contentKeyword%)")
    Page<Notification> findByConditions(
            @Param("recipientId") String recipientId,
            @Param("recipientType") Notification.RecipientType recipientType,
            @Param("type") Notification.NotificationType type,
            @Param("status") Notification.NotificationStatus status,
            @Param("priority") Notification.NotificationPriority priority,
            @Param("businessType") String businessType,
            @Param("createdAtStart") LocalDateTime createdAtStart,
            @Param("createdAtEnd") LocalDateTime createdAtEnd,
            @Param("titleKeyword") String titleKeyword,
            @Param("contentKeyword") String contentKeyword,
            Pageable pageable
    );
}
