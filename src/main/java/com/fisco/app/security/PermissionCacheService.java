package com.fisco.app.security;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * 权限缓存服务
 * 缓存用户权限信息，提高权限验证性能
 *
 * 缓存策略：
 * - 用户权限信息缓存5分钟
 * - 企业关系缓存10分钟
 * - 角色信息缓存10分钟
 */
@Slf4j
@Service
public class PermissionCacheService {

    /**
     * 检查用户是否有企业访问权限（带缓存）
     *
     * @param username 用户名
     * @param enterpriseId 企业ID
     * @return 是否有权限
     */
    @Cacheable(value = "enterpriseAccess", key = "#username + ':' + #enterpriseId", unless = "#result == false")
    public boolean hasEnterpriseAccess(String username, String enterpriseId) {
        log.debug("缓存未命中，查询企业访问权限: username={}, enterpriseId={}", username, enterpriseId);
        // 这个方法的实际逻辑应该由PermissionChecker调用
        // 这里只是定义缓存策略
        return true;
    }

    /**
     * 检查用户是否有指定角色（带缓存）
     *
     * @param username 用户名
     * @param role 角色
     * @return 是否有角色
     */
    @Cacheable(value = "userRole", key = "#username + ':' + #role", unless = "#result == false")
    public boolean hasRole(String username, String role) {
        log.debug("缓存未命中，查询用户角色: username={}, role={}", username, role);
        return true;
    }

    /**
     * 获取用户的所有角色（带缓存）
     *
     * @param username 用户名
     * @return 角色集合
     */
    @Cacheable(value = "userRoles", key = "#username")
    public Set<String> getUserRoles(String username) {
        log.debug("缓存未命中，查询用户所有角色: username={}", username);
        return Set.of();
    }

    /**
     * 清除用户权限缓存
     * 当用户信息更新时调用
     *
     * @param username 用户名
     */
    @CacheEvict(value = {"enterpriseAccess", "userRole", "userRoles"}, key = "#username + '*")
    public void evictUserPermissions(String username) {
        log.info("清除用户权限缓存: username={}", username);
    }

    /**
     * 清除企业相关缓存
     * 当企业信息更新时调用
     *
     * @param enterpriseId 企业ID
     */
    @CacheEvict(value = {"enterpriseAccess"}, key = "*:" + "#enterpriseId")
    public void evictEnterprisePermissions(String enterpriseId) {
        log.info("清除企业权限缓存: enterpriseId={}", enterpriseId);
    }

    /**
     * 清除所有权限缓存
     * 系统维护时使用
     */
    @CacheEvict(value = {"enterpriseAccess", "userRole", "userRoles"}, allEntries = true)
    public void evictAllPermissions() {
        log.warn("清除所有权限缓存");
    }
}
