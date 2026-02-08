package com.fisco.app.event;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.fisco.app.security.PermissionCacheService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 权限更新事件监听器
 * 监听用户和企业变更事件，自动更新权限缓存
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionUpdateListener {

    private final PermissionCacheService permissionCacheService;

    /**
     * 处理用户更新事件
     * 当用户信息（角色、企业）变更时清除缓存
     */
    @Async
    @EventListener
    public void handleUserUpdatedEvent(UserUpdatedEvent event) {
        log.info("检测到用户信息更新，清除权限缓存: username={}, enterpriseId={}",
                event.getUsername(), event.getEnterpriseId());

        // 清除用户权限缓存
        permissionCacheService.evictUserPermissions(event.getUsername());

        // 如果企业ID也变更了，清除旧企业的缓存
        if (event.getOldEnterpriseId() != null &&
                !event.getOldEnterpriseId().equals(event.getEnterpriseId())) {
            permissionCacheService.evictEnterprisePermissions(event.getOldEnterpriseId());
        }

        log.info("权限缓存已清除: username={}", event.getUsername());
    }

    /**
     * 处理企业更新事件
     * 当企业信息变更时清除相关缓存
     */
    @Async
    @EventListener
    public void handleEnterpriseUpdatedEvent(EnterpriseUpdatedEvent event) {
        log.info("检测到企业信息更新，清除权限缓存: enterpriseId={}", event.getEnterpriseId());

        // 清除企业权限缓存
        permissionCacheService.evictEnterprisePermissions(event.getEnterpriseId());

        log.info("企业权限缓存已清除: enterpriseId={}", event.getEnterpriseId());
    }

    /**
     * 处理用户删除事件
     */
    @Async
    @EventListener
    public void handleUserDeletedEvent(UserDeletedEvent event) {
        log.info("检测到用户删除，清除权限缓存: username={}", event.getUsername());

        permissionCacheService.evictUserPermissions(event.getUsername());

        log.info("已删除用户的权限缓存已清除: username={}", event.getUsername());
    }
}
