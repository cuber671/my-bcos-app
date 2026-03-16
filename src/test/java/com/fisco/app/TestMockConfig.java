package com.fisco.app;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.fisco.app.Common.Config.BlockchainConfig;

/**
 * 测试配置 - 提供 Mock 的 BlockchainConfig
 * 用于测试环境，fisco.enabled=false 时不需要真正的区块链连接
 */
@TestConfiguration
public class TestMockConfig {

    /**
     * 创建一个 Mock 的 BlockchainConfig Bean
     * 由于原配置有 @Profile("!test")，测试环境不会自动加载
     * 需要显式提供一个简化版本
     */
    @Bean
    @Primary
    public BlockchainConfig blockchainConfig() {
        return new TestBlockchainConfig();
    }

    /**
     * 测试用的简化 BlockchainConfig
     * 继承原类但不使用区块链功能
     */
    public static class TestBlockchainConfig extends BlockchainConfig {
        public TestBlockchainConfig() {
            super();
        }
    }
}
