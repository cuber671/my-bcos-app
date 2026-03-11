package com.fisco.app.Modules.Test.Controller;

import com.fisco.app.Common.Config.BlockchainConfig;
import com.fisco.app.Common.Utils.Result;
import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * BlockchainTestController 单元测试
 *
 * 测试覆盖：
 * - UT001: testBlockchain() - 功能开关关闭
 * - UT002: testBlockchain() - Client为空
 * - UT003: testBlockchain() - 连接成功
 * - UT004: blockchainHealth() - SDK未初始化
 */
@ExtendWith(MockitoExtension.class)
class BlockchainTestControllerTest {

    @Mock
    private Client client;

    @Mock
    private CryptoKeyPair cryptoKeyPair;

    @Mock
    private BlockchainConfig blockchainConfig;

    @InjectMocks
    private BlockchainTestController blockchainTestController;

    /**
     * UT001: 测试testBlockchain() - 功能开关关闭
     */
    @Test
    void testBlockchain_FeatureDisabled() {
        ReflectionTestUtils.setField(blockchainTestController, "fiscoEnabled", false);
        when(blockchainConfig.isEnabled()).thenReturn(true);

        Result<Map<String, Object>> result = blockchainTestController.testBlockchain();

        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(false, result.getData().get("enabled"));
        assertEquals("DISABLED", result.getData().get("status"));
    }

    /**
     * UT002: 测试testBlockchain() - Client为空
     */
    @Test
    void testBlockchain_ClientNull() {
        ReflectionTestUtils.setField(blockchainTestController, "fiscoEnabled", true);
        ReflectionTestUtils.setField(blockchainTestController, "client", null);
        when(blockchainConfig.isEnabled()).thenReturn(true);

        Result<Map<String, Object>> result = blockchainTestController.testBlockchain();

        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals("null", result.getData().get("client"));
        assertEquals("ERROR", result.getData().get("status"));
    }

    /**
     * UT003: 测试testBlockchain() - 连接成功
     */
    @Test
    void testBlockchain_Connected() {
        ReflectionTestUtils.setField(blockchainTestController, "fiscoEnabled", true);
        ReflectionTestUtils.setField(blockchainTestController, "client", client);
        ReflectionTestUtils.setField(blockchainTestController, "cryptoKeyPair", cryptoKeyPair);
        when(blockchainConfig.isEnabled()).thenReturn(true);

        Result<Map<String, Object>> result = blockchainTestController.testBlockchain();

        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(true, result.getData().get("enabled"));
        assertEquals(true, result.getData().get("connected"));
    }

    /**
     * UT004: 测试blockchainHealth() - SDK未初始化
     */
    @Test
    void testBlockchainHealth_SdkNotInitialized() {
        ReflectionTestUtils.setField(blockchainTestController, "fiscoEnabled", true);
        ReflectionTestUtils.setField(blockchainTestController, "client", null);

        Result<Map<String, Object>> result = blockchainTestController.blockchainHealth();

        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals("DOWN", result.getData().get("status"));
    }

    /**
     * UT005: 测试响应包含必要字段
     */
    @Test
    void testBlockchain_ResponseFields() {
        ReflectionTestUtils.setField(blockchainTestController, "fiscoEnabled", true);
        ReflectionTestUtils.setField(blockchainTestController, "client", client);
        ReflectionTestUtils.setField(blockchainTestController, "cryptoKeyPair", cryptoKeyPair);
        when(blockchainConfig.isEnabled()).thenReturn(true);

        Result<Map<String, Object>> result = blockchainTestController.testBlockchain();

        assertNotNull(result.getData());
        assertNotNull(result.getData().get("enabled"));
        assertNotNull(result.getData().get("configEnabled"));
        assertNotNull(result.getData().get("status"));
    }
}
