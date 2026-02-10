package com.fisco.app.service.user;

import com.fisco.app.dto.user.UserActivityDTO;
import com.fisco.app.dto.user.UserActivityQueryRequest;
import com.fisco.app.entity.user.User;
import com.fisco.app.entity.user.UserActivity;
import com.fisco.app.repository.user.UserActivityRepository;
import com.fisco.app.repository.user.UserRepository;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 用户活动日志Service
 */
@Slf4j
@Service
@Api(tags = "用户活动日志服务")
@RequiredArgsConstructor
public class UserActivityService {

    private final UserActivityRepository userActivityRepository;
    private final UserRepository userRepository;

    /**
     * 记录用户活动
     */
    @Transactional
    public UserActivity recordActivity(@NonNull String userId,
                                       @NonNull UserActivity.ActivityType activityType,
                                       @NonNull String description,
                                       String module,
                                       UserActivity.ActivityResult result,
                                       String ipAddress,
                                       String userAgent,
                                       String failureReason,
                                       Long duration,
                                       String extraData) {
        log.debug("记录用户活动: userId={}, activityType={}, description={}",
                  userId, activityType, description);

        // 查询用户信息
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("用户不存在，仍然记录活动: userId={}", userId);
        }

        UserActivity activity = new UserActivity();
        activity.setUserId(userId);
        if (user != null) {
            activity.setUsername(user.getUsername());
            activity.setRealName(user.getRealName());
        }
        activity.setActivityType(activityType);
        activity.setDescription(description);
        activity.setModule(module);
        activity.setResult(result != null ? result : UserActivity.ActivityResult.SUCCESS);
        activity.setIpAddress(ipAddress);
        activity.setUserAgent(userAgent);
        activity.setFailureReason(failureReason);
        activity.setDuration(duration);
        activity.setExtraData(extraData);

        // 解析User-Agent获取浏览器和操作系统信息
        if (userAgent != null) {
            parseUserAgent(activity, userAgent);
        }

        UserActivity saved = userActivityRepository.save(activity);
        log.debug("用户活动记录成功: activityId={}", saved.getId());

        return saved;
    }

    /**
     * 记录登录活动
     */
    @Transactional
    public void recordLoginActivity(@NonNull String userId,
                                    String ipAddress,
                                    String userAgent,
                                    boolean success,
                                    String failureReason) {
        String description = success ? "用户登录系统" : "用户登录失败";
        UserActivity.ActivityResult result = success ?
            UserActivity.ActivityResult.SUCCESS : UserActivity.ActivityResult.FAILURE;

        recordActivity(userId, UserActivity.ActivityType.LOGIN, description,
                      "USER", result, ipAddress, userAgent, failureReason, null, null);
    }

    /**
     * 记录登出活动
     */
    @Transactional
    public void recordLogoutActivity(@NonNull String userId,
                                     String ipAddress,
                                     String userAgent) {
        recordActivity(userId, UserActivity.ActivityType.LOGOUT, "用户登出系统",
                      "USER", UserActivity.ActivityResult.SUCCESS,
                      ipAddress, userAgent, null, null, null);
    }

    /**
     * 分页查询用户活动日志
     */
    public Page<UserActivityDTO> queryActivities(UserActivityQueryRequest request) {
        log.info("查询用户活动日志: userId={}, activityType={}, module={}, result={}",
                 request.getUserId(), request.getActivityType(),
                 request.getModule(), request.getResult());

        // 创建排序
        Sort sort = Sort.by(
            "DESC".equalsIgnoreCase(request.getSortDirection()) ?
                Sort.Direction.DESC : Sort.Direction.ASC,
            request.getSortField()
        );

        // 创建分页
        Pageable pageable = PageRequest.of(
            request.getPage(),
            request.getSize(),
            sort
        );

        // 根据条件查询
        List<UserActivity> activities = userActivityRepository.findByConditions(
            request.getUserId(),
            request.getUsername(),
            request.getActivityType(),
            request.getModule(),
            request.getResult(),
            request.getIpAddress(),
            request.getStartDate(),
            request.getEndDate()
        );

        // 手动分页
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), activities.size());

        List<UserActivity> pageContent = start < activities.size()
            ? activities.subList(start, end)
            : List.of();

        List<UserActivityDTO> dtoList = Objects.requireNonNull(
            pageContent.stream()
                .map(UserActivityDTO::fromEntity)
                .collect(Collectors.toList()),
            "UserActivityDTO list should not be null"
        );

        return new PageImpl<>(dtoList, pageable, activities.size());
    }

    /**
     * 获取用户的活动日志
     */
    @SuppressWarnings("null")
    public List<UserActivityDTO> getUserActivities(@NonNull String userId) {
        log.info("获取用户活动日志: userId={}", userId);

        List<UserActivity> activities = userActivityRepository
            .findByUserIdOrderByCreatedAtDesc(userId);

        return activities.stream()
            .map(UserActivityDTO::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * 获取用户的登录记录
     */
    public List<UserActivityDTO> getUserLoginHistory(@NonNull String userId, int limit) {
        log.info("获取用户登录记录: userId={}, limit={}", userId, limit);

        List<UserActivity> activities = userActivityRepository
            .findRecentLoginsByUserId(userId);

        return activities.stream()
            .limit(limit)
            .map(UserActivityDTO::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * 获取用户的失败活动记录
     */
    public List<UserActivityDTO> getUserFailedActivities(@NonNull String userId) {
        log.info("获取用户失败活动记录: userId={}", userId);

        List<UserActivity> activities = userActivityRepository
            .findFailedActivitiesByUserId(userId);

        return activities.stream()
            .map(UserActivityDTO::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * 统计用户活动次数
     */
    public Long countUserActivities(@NonNull String userId) {
        return userActivityRepository.countByUserId(userId);
    }

    /**
     * 统计用户指定时间段内的活动次数
     */
    public Long countUserActivitiesInRange(@NonNull String userId,
                                           LocalDateTime startDate,
                                           LocalDateTime endDate) {
        return userActivityRepository.countByUserIdAndDateRange(userId, startDate, endDate);
    }

    /**
     * 删除旧的活动日志
     */
    @Transactional
    public void cleanupOldActivities(LocalDateTime beforeDate) {
        log.info("删除旧的活动日志: beforeDate={}", beforeDate);
        userActivityRepository.deleteOldActivities(beforeDate);
        log.info("旧活动日志删除完成");
    }

    /**
     * 解析User-Agent获取浏览器和操作系统信息
     */
    private void parseUserAgent(UserActivity activity, String userAgent) {
        String ua = userAgent.toLowerCase();

        // 解析浏览器
        if (ua.contains("chrome")) {
            activity.setBrowser("Chrome");
        } else if (ua.contains("firefox")) {
            activity.setBrowser("Firefox");
        } else if (ua.contains("safari") && !ua.contains("chrome")) {
            activity.setBrowser("Safari");
        } else if (ua.contains("edge")) {
            activity.setBrowser("Edge");
        } else if (ua.contains("opera")) {
            activity.setBrowser("Opera");
        } else {
            activity.setBrowser("Unknown");
        }

        // 解析操作系统
        if (ua.contains("windows nt 10.0")) {
            activity.setOs("Windows 10");
        } else if (ua.contains("windows nt 6.3")) {
            activity.setOs("Windows 8.1");
        } else if (ua.contains("windows nt 6.2")) {
            activity.setOs("Windows 8");
        } else if (ua.contains("windows nt 6.1")) {
            activity.setOs("Windows 7");
        } else if (ua.contains("windows nt 6.0")) {
            activity.setOs("Windows Vista");
        } else if (ua.contains("windows nt 5.1")) {
            activity.setOs("Windows XP");
        } else if (ua.contains("windows")) {
            activity.setOs("Windows");
        } else if (ua.contains("mac os x")) {
            activity.setOs("macOS");
        } else if (ua.contains("linux")) {
            activity.setOs("Linux");
        } else if (ua.contains("android")) {
            activity.setOs("Android");
        } else if (ua.contains("iphone") || ua.contains("ipad")) {
            activity.setOs("iOS");
        } else {
            activity.setOs("Unknown");
        }

        // 解析设备类型
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) {
            activity.setDevice("Mobile");
        } else if (ua.contains("tablet") || ua.contains("ipad")) {
            activity.setDevice("Tablet");
        } else {
            activity.setDevice("PC");
        }
    }
}
