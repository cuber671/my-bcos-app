package com.fisco.app.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 缓存配置
 * 使用ConcurrentMap作为简单的本地缓存实现
 * 生产环境建议使用Redis等分布式缓存
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 配置缓存管理器
     */
    @Bean
    public CacheManager cacheManager() {
        // 使用简单的ConcurrentMap缓存管理器
        // 注意：这是本地缓存，不适合分布式环境
        return new ConcurrentMapCacheManager(
                "enterpriseAccess",
                "userRole",
                "userRoles"
        );
    }
}
