package com.fisco.app.Common.Config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * 系统管理员配置
 * 从 application.yml 加载管理员账户配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "admin")
public class AdminConfig {

    /**
     * 管理员用户名
     */
    private String username = "admin";

    /**
     * BCrypt加密后的密码
     */
    private String password = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi";

    /**
     * 权限范围: 1=超级管理员
     */
    private Integer scope = 1;

    /**
     * 检查是否为有效的管理员配置
     */
    public boolean isValid() {
        return username != null && !username.isEmpty()
            && password != null && !password.isEmpty()
            && scope != null;
    }
}
