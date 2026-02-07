package com.fisco.app.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * 日志配置类
 * 配置应用的日志行为和初始化
 */
@Slf4j
@Configuration
public class LoggingConfig {

    /**
     * 应用启动时打印配置信息
     */
    @PostConstruct
    public void init() {
        log.info("========================================");
        log.info("  FISCO BCOS 供应链金融平台");
        log.info("========================================");
        log.info("  环境: {}", System.getProperty("spring.profiles.active", "default"));
        log.info("  Java: {}", System.getProperty("java.version"));
        log.info("  工作目录: {}", System.getProperty("user.dir"));
        log.info("========================================");
        log.info("  日志配置已加载");
        log.info("  - 请求日志: 已启用");
        log.info("  - 性能监控: 已启用 (>3000ms警告)");
        log.info("  - 异常追踪: 已增强");
        log.info("  - SQL日志: DEBUG级别");
        log.info("========================================");
    }
}
