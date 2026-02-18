package com.fisco.app.config;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * 缓存配置
 * 使用 Caffeine 作为高性能本地缓存实现
 * 生产环境建议使用 Redis 等分布式缓存
 *
 * @author FISCO BCOS
 * @since 2025-02-09
 */
@SuppressWarnings("nullness")
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 配置 Caffeine 缓存管理器
     */
    @SuppressWarnings("null")
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // 配置默认缓存策略
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(500)
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .recordStats();
        cacheManager.setCaffeine(caffeine);

        // 预定义缓存名称
        cacheManager.setCacheNames(Arrays.asList(
                "enterpriseAccess",
                "userRole",
                "userRoles",
                "blockchainStats"  // 区块链统计数据缓存
        ));

        return cacheManager;
    }
}
