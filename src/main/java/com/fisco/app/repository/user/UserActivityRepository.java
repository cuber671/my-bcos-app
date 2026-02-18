package com.fisco.app.repository.user;

import com.fisco.app.entity.user.UserActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户活动日志Repository
 */
@Repository
public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {

    /**
     * 根据用户ID查询活动日志（按创建时间倒序）
     */
    List<UserActivity> findByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * 根据用户ID和活动类型查询
     */
    List<UserActivity> findByUserIdAndActivityTypeOrderByCreatedAtDesc(
            String userId,
            UserActivity.ActivityType activityType
    );

    /**
     * 根据用户ID和模块查询
     */
    List<UserActivity> findByUserIdAndModuleOrderByCreatedAtDesc(
            String userId,
            String module
    );

    /**
     * 根据用户ID、模块和活动类型查询
     */
    List<UserActivity> findByUserIdAndModuleAndActivityTypeOrderByCreatedAtDesc(
            String userId,
            String module,
            UserActivity.ActivityType activityType
    );

    /**
     * 根据IP地址查询
     */
    List<UserActivity> findByIpAddressOrderByCreatedAtDesc(String ipAddress);

    /**
     * 根据条件查询活动日志
     */
    @Query("SELECT ua FROM UserActivity ua WHERE " +
           "(:userId IS NULL OR ua.userId = :userId) AND " +
           "(:username IS NULL OR ua.username LIKE %:username%) AND " +
           "(:activityType IS NULL OR ua.activityType = :activityType) AND " +
           "(:module IS NULL OR ua.module = :module) AND " +
           "(:result IS NULL OR ua.result = :result) AND " +
           "(:ipAddress IS NULL OR ua.ipAddress = :ipAddress) AND " +
           "(:startDate IS NULL OR ua.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR ua.createdAt <= :endDate) " +
           "ORDER BY ua.createdAt DESC")
    List<UserActivity> findByConditions(
            @Param("userId") String userId,
            @Param("username") String username,
            @Param("activityType") UserActivity.ActivityType activityType,
            @Param("module") String module,
            @Param("result") UserActivity.ActivityResult result,
            @Param("ipAddress") String ipAddress,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 统计用户活动次数
     */
    @Query("SELECT COUNT(ua) FROM UserActivity ua WHERE ua.userId = :userId")
    Long countByUserId(@Param("userId") String userId);

    /**
     * 统计用户指定时间段内的活动次数
     */
    @Query("SELECT COUNT(ua) FROM UserActivity ua WHERE " +
           "ua.userId = :userId AND " +
           "ua.createdAt >= :startDate AND " +
           "ua.createdAt <= :endDate")
    Long countByUserIdAndDateRange(
            @Param("userId") String userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 获取用户最近的登录记录
     */
    @Query("SELECT ua FROM UserActivity ua WHERE " +
           "ua.userId = :userId AND " +
           "ua.activityType = 'LOGIN' AND " +
           "ua.result = 'SUCCESS' " +
           "ORDER BY ua.createdAt DESC")
    List<UserActivity> findRecentLoginsByUserId(@Param("userId") String userId);

    /**
     * 获取用户失败的活动记录
     */
    @Query("SELECT ua FROM UserActivity ua WHERE " +
           "ua.userId = :userId AND " +
           "ua.result = 'FAILURE' " +
           "ORDER BY ua.createdAt DESC")
    List<UserActivity> findFailedActivitiesByUserId(@Param("userId") String userId);

    /**
     * 删除指定日期之前的活动日志
     */
    @Query("DELETE FROM UserActivity ua WHERE ua.createdAt < :date")
    void deleteOldActivities(@Param("date") LocalDateTime date);
}
