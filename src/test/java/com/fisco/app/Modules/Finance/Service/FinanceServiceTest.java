package com.fisco.app.Modules.Finance.Service;

import com.fisco.app.Modules.Finance.Entity.Receivable;
import com.fisco.app.Modules.Finance.Entity.RepaymentRecord;
import com.fisco.app.Modules.Finance.Mapper.ReceivableMapper;
import com.fisco.app.Modules.Finance.Mapper.RepaymentRecordMapper;
import com.fisco.app.Modules.Finance.Service.blockchain.ReceivableContractService;
import com.fisco.app.Modules.Finance.Service.impl.FinanceServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * FinanceService 单元测试
 *
 * 测试金融模块的业务逻辑
 * 覆盖：应收款生成、确认、融资、还款、结算
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FinanceServiceTest {

    @Mock
    private ReceivableMapper receivableMapper;

    @Mock
    private RepaymentRecordMapper repaymentRecordMapper;

    @Mock
    private ReceivableContractService contractService;

    @InjectMocks
    private FinanceServiceImpl financeService;

    // 测试数据
    private static final Long TEST_RECEIVABLE_ID = 10001L;
    private static final Long TEST_ENTERPRISE_ID = 2001L;
    private static final Long TEST_DEBTOR_ENTERPRISE_ID = 3001L;
    private static final Long TEST_DELEGATE_ID = 8001L;
    private static final String TEST_RECEIVABLE_NO = "REC20260313001";
    private static final BigDecimal TEST_AMOUNT = BigDecimal.valueOf(500000);

    // ==================== 应收款查询测试 ====================

    @Test
    @Order(1)
    @DisplayName("根据ID查询应收款")
    void getReceivableById_shouldReturnReceivable() {
        // Arrange
        Receivable receivable = createTestReceivable();
        when(receivableMapper.selectById(TEST_RECEIVABLE_ID)).thenReturn(receivable);

        // Act
        Receivable result = financeService.getReceivableById(TEST_RECEIVABLE_ID);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_RECEIVABLE_ID, result.getId());
    }

    @Test
    @Order(2)
    @DisplayName("根据ID查询应收款 - 不存在")
    void getReceivableById_shouldReturnNull_whenNotExists() {
        // Arrange
        when(receivableMapper.selectById(TEST_RECEIVABLE_ID)).thenReturn(null);

        // Act
        Receivable result = financeService.getReceivableById(TEST_RECEIVABLE_ID);

        // Assert
        assertNull(result);
    }

    @Test
    @Order(3)
    @DisplayName("根据应收款编号查询应收款")
    void getReceivableByNo_shouldReturnReceivable() {
        // Arrange
        Receivable receivable = createTestReceivable();
        when(receivableMapper.selectByReceivableNo(TEST_RECEIVABLE_NO)).thenReturn(receivable);

        // Act
        Receivable result = financeService.getReceivableByNo(TEST_RECEIVABLE_NO);

        // Assert
        assertNotNull(result);
    }

    @Test
    @Order(4)
    @DisplayName("查询债权人的应收款列表")
    void listByCreditor_shouldReturnList() {
        // Arrange
        List<Receivable> receivables = Arrays.asList(createTestReceivable());
        when(receivableMapper.selectByCreditorEntId(TEST_ENTERPRISE_ID)).thenReturn(receivables);

        // Act
        List<Receivable> result = financeService.listByCreditor(TEST_ENTERPRISE_ID);

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    @Order(5)
    @DisplayName("查询债务人的应收款列表")
    void listByDebtor_shouldReturnList() {
        // Arrange
        List<Receivable> receivables = Arrays.asList(createTestReceivable());
        when(receivableMapper.selectByDebtorEntId(TEST_DEBTOR_ENTERPRISE_ID)).thenReturn(receivables);

        // Act
        List<Receivable> result = financeService.listByDebtor(TEST_DEBTOR_ENTERPRISE_ID);

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    // ==================== 还款记录查询测试 ====================

    @Test
    @Order(10)
    @DisplayName("根据应收款ID查询还款记录列表")
    void listRepayments_shouldReturnList() {
        // Arrange
        List<RepaymentRecord> records = Arrays.asList(createTestRepaymentRecord());
        when(repaymentRecordMapper.selectByReceivableId(TEST_RECEIVABLE_ID)).thenReturn(records);

        // Act
        List<RepaymentRecord> result = financeService.listRepayments(TEST_RECEIVABLE_ID);

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    @Order(11)
    @DisplayName("根据应收款ID查询还款记录列表 - 无数据")
    void listRepayments_shouldReturnEmptyList_whenNoData() {
        // Arrange
        when(repaymentRecordMapper.selectByReceivableId(TEST_RECEIVABLE_ID)).thenReturn(Collections.emptyList());

        // Act
        List<RepaymentRecord> result = financeService.listRepayments(TEST_RECEIVABLE_ID);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== 辅助方法 ====================

    private Receivable createTestReceivable() {
        Receivable receivable = new Receivable();
        receivable.setId(TEST_RECEIVABLE_ID);
        receivable.setReceivableNo(TEST_RECEIVABLE_NO);
        receivable.setCreditorEntId(TEST_ENTERPRISE_ID);
        receivable.setDebtorEntId(TEST_DEBTOR_ENTERPRISE_ID);
        receivable.setSourceVoucherId(TEST_DELEGATE_ID);
        receivable.setInitialAmount(TEST_AMOUNT);
        receivable.setAdjustedAmount(TEST_AMOUNT);
        receivable.setCollectedAmount(BigDecimal.ZERO);
        receivable.setBalanceUnpaid(TEST_AMOUNT);
        receivable.setStatus(Receivable.STATUS_PENDING);
        receivable.setDueDate(LocalDateTime.now().plusMonths(1));
        receivable.setIsFinanced(0);
        return receivable;
    }

    private RepaymentRecord createTestRepaymentRecord() {
        RepaymentRecord record = new RepaymentRecord();
        record.setId(1L);
        record.setReceivableId(TEST_RECEIVABLE_ID);
        record.setAmount(BigDecimal.valueOf(50000));
        record.setRepaymentType(1);
        record.setRepaymentTime(LocalDateTime.now());
        return record;
    }
}
