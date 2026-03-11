package com.fisco.app.Modules.Test.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fisco.app.Common.Utils.Result;

import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * 健康检查Controller
 * 提供 /api/v1/health 匿名接口用于服务健康检查
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @Autowired
    private DataSource dataSource;

    /**
     * 健康检查接口 - 匿名访问
     * 用于负载均衡器或监控系统检测服务状态
     */
    @GetMapping("/health")
    public Result<HealthStatus> health() {
        HealthStatus status = new HealthStatus();
        status.setStatus("UP");
        status.setTimestamp(System.currentTimeMillis());

        // 检查数据库连接
        try (Connection conn = dataSource.getConnection()) {
            if (conn.isValid(5)) {
                status.setDatabase("UP");
            } else {
                status.setDatabase("DOWN");
                status.setStatus("DEGRADED");
            }
        } catch (Exception e) {
            status.setDatabase("DOWN");
            status.setStatus("DEGRADED");
            log.warn("数据库健康检查失败: {}", e.getMessage());
        }

        log.debug("健康检查: status={}", status.getStatus());
        return Result.success(status);
    }

    /**
     * 健康状态
     */
    public static class HealthStatus {
        private String status;
        private String database;
        private Long timestamp;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getDatabase() {
            return database;
        }

        public void setDatabase(String database) {
            this.database = database;
        }

        public Long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Long timestamp) {
            this.timestamp = timestamp;
        }
    }
}
