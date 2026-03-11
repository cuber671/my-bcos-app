package com.fisco.app.Modules.Blockchain.Service;

import com.fisco.app.Common.Utils.AuditContext;
import com.fisco.app.Modules.Blockchain.Entity.BlockchainTransactionRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BlockchainAuditService单元测试
 */
@SpringBootTest
class BlockchainAuditServiceTest {

    @Autowired
    private BlockchainAuditService blockchainAuditService;

    @BeforeEach
    @AfterEach
    void setUp() {
        // 每个测试前后清理AuditContext
        AuditContext.clear();
    }

    /**
     * 测试记录交易映射
     */
    @Test
    void testRecordTransaction() {
        AuditContext.set(100L, 1L, "ADMIN", 1, "jti-123");
        blockchainAuditService.recordTransaction("0xabc123", "CONTRACT_DEPLOY", "TestContract");

        BlockchainTransactionRecord record = blockchainAuditService.getRecordByTxHash("0xabc123");

        assertNotNull(record, "记录应该存在");
        assertEquals("0xabc123", record.getTxHash());
        assertEquals("CONTRACT_DEPLOY", record.getOperation());
        assertEquals("TestContract", record.getContractName());
        assertEquals(100L, record.getUserId());
        assertEquals(1L, record.getEntId());
        assertEquals("jti-123", record.getJti());
    }

    /**
     * 测试根据txHash查询JTI
     */
    @Test
    void testGetJtiByTxHash() {
        AuditContext.set(100L, 1L, "ADMIN", 1, "jti-456");
        blockchainAuditService.recordTransaction("0xdef456", "CONTRACT_INVOKE", "DemoContract");

        String jti = blockchainAuditService.getJtiByTxHash("0xdef456");

        assertEquals("jti-456", jti);
    }

    /**
     * 测试根据userId查询记录
     */
    @Test
    void testGetRecordsByUserId() {
        AuditContext.set(100L, 1L, "ADMIN", 1, "jti-789");
        blockchainAuditService.recordTransaction("0xghi789", "CONTRACT_DEPLOY", "ContractA");

        List<BlockchainTransactionRecord> records = blockchainAuditService.getRecordsByUserId(100L);

        assertFalse(records.isEmpty(), "应该有记录");
        assertEquals(100L, records.get(0).getUserId());
    }

    /**
     * 测试根据entId查询记录
     */
    @Test
    void testGetRecordsByEntId() {
        AuditContext.set(100L, 1L, "ADMIN", 1, "jti-abc");
        blockchainAuditService.recordTransaction("0xjkl012", "CONTRACT_INVOKE", "ContractB");

        List<BlockchainTransactionRecord> records = blockchainAuditService.getRecordsByEntId(1L);

        assertFalse(records.isEmpty(), "应该有记录");
        assertEquals(1L, records.get(0).getEntId());
    }

    /**
     * 测试无AuditContext记录
     */
    @Test
    void testRecordWithoutAuditContext() {
        AuditContext.clear();
        blockchainAuditService.recordTransaction("0xmno345", "CONTRACT_DEPLOY", "ContractC");

        BlockchainTransactionRecord record = blockchainAuditService.getRecordByTxHash("0xmno345");

        assertNotNull(record, "记录应该存在");
        assertNull(record.getUserId(), "userId应该为null");
        assertNull(record.getEntId(), "entId应该为null");
        assertNull(record.getJti(), "jti应该为null");
    }

    /**
     * 测试查询不存在的txHash
     */
    @Test
    void testGetNonExistentTxHash() {
        BlockchainTransactionRecord record = blockchainAuditService.getRecordByTxHash("0xnotexist");
        assertNull(record, "不存在的记录应该返回null");

        String jti = blockchainAuditService.getJtiByTxHash("0xnotexist");
        assertNull(jti, "不存在的jti应该返回null");
    }
}
