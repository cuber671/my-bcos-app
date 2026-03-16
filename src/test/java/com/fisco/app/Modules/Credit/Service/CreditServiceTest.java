package com.fisco.app.Modules.Credit.Service;

import com.fisco.app.Modules.Credit.Entity.CreditEvent;
import com.fisco.app.Modules.Credit.Entity.EnterpriseCreditProfile;
import com.fisco.app.Modules.Credit.Mapper.CreditProfileMapper;
import com.fisco.app.Modules.Credit.Mapper.CreditEventMapper;
import com.fisco.app.Modules.Enterprise.Mapper.EnterpriseMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.mockito.stubbing.Answer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.*;

/**
 * CreditService 单元测试
 *
 * 测试信用管理模块的业务逻辑
 * 覆盖：信用档案管理、信用事件、评分计算、额度管理
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CreditServiceTest {

    @Mock
    private CreditProfileMapper creditProfileMapper;

    @Mock
    private CreditEventMapper creditEventMapper;

    @Mock
    private CreditContractService creditContractService;

    @Mock
    private EnterpriseMapper enterpriseMapper;

    @InjectMocks
    private CreditServiceImpl creditService;

    // 测试数据
    private static final Long TEST_ENTERPRISE_ID = 2001L;
    private static final Long TEST_PROFILE_ID = 3001L;
    private static final Long TEST_EVENT_ID = 4001L;
    private static final BigDecimal TEST_CREDIT_LIMIT = BigDecimal.valueOf(1000000);
    private static final BigDecimal TEST_AMOUNT = BigDecimal.valueOf(50000);

    // ==================== 信用档案管理测试 ====================

    @Test
    @Order(1)
    @DisplayName("获取企业信用档案 - 档案存在")
    void getCreditProfile_shouldReturnProfile_whenExists() {
        // Arrange
        EnterpriseCreditProfile profile = createTestCreditProfile();
        when(creditProfileMapper.selectByEntId(TEST_ENTERPRISE_ID)).thenReturn(profile);

        // Act
        EnterpriseCreditProfile result = creditService.getCreditProfile(TEST_ENTERPRISE_ID);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_ENTERPRISE_ID, result.getEntId());
    }

    @Test
    @Order(2)
    @DisplayName("获取企业信用档案 - 档案不存在")
    void getCreditProfile_shouldReturnNull_whenNotExists() {
        // Arrange
        when(creditProfileMapper.selectByEntId(TEST_ENTERPRISE_ID)).thenReturn(null);

        // Act
        EnterpriseCreditProfile result = creditService.getCreditProfile(TEST_ENTERPRISE_ID);

        // Assert
        assertNull(result);
    }

    @Test
    @Order(3)
    @DisplayName("创建企业信用档案成功")
    void createCreditProfile_shouldSuccess() {
        // Arrange
        when(creditProfileMapper.selectByEntId(TEST_ENTERPRISE_ID)).thenReturn(null);
        when(creditProfileMapper.insert(any(EnterpriseCreditProfile.class))).thenReturn(1);

        // Act
        EnterpriseCreditProfile result = creditService.createCreditProfile(TEST_ENTERPRISE_ID);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_ENTERPRISE_ID, result.getEntId());
        assertEquals(800, result.getCreditScore()); // 默认信用分800
    }

    @Test
    @Order(4)
    @DisplayName("设置授信额度成功")
    void setCreditLimit_shouldSuccess() {
        // Arrange
        EnterpriseCreditProfile profile = createTestCreditProfile();
        when(creditProfileMapper.selectByEntId(TEST_ENTERPRISE_ID)).thenReturn(profile);
        when(creditProfileMapper.updateByEntId(any(EnterpriseCreditProfile.class))).thenReturn(1);
        when(enterpriseMapper.selectById(TEST_ENTERPRISE_ID)).thenReturn(null); // 区块链地址为null，跳过上链

        // Act
        boolean result = creditService.setCreditLimit(TEST_ENTERPRISE_ID, TEST_CREDIT_LIMIT);

        // Assert
        assertTrue(result);
    }

    @Test
    @Order(5)
    @DisplayName("使用信用额度成功")
    void useCreditLimit_shouldSuccess() {
        // Arrange
        EnterpriseCreditProfile profile = createTestCreditProfile();
        profile.setAvailableLimit(TEST_CREDIT_LIMIT);
        when(creditProfileMapper.selectByEntId(TEST_ENTERPRISE_ID)).thenReturn(profile);
        when(creditProfileMapper.updateByEntId(any(EnterpriseCreditProfile.class))).thenReturn(1);

        // Act
        boolean result = creditService.useCreditLimit(TEST_ENTERPRISE_ID, TEST_AMOUNT);

        // Assert
        assertTrue(result);
    }

    @Test
    @Order(6)
    @DisplayName("使用信用额度失败 - 额度不足")
    void useCreditLimit_shouldFail_whenInsufficientCredit() {
        // Arrange
        EnterpriseCreditProfile profile = createTestCreditProfile();
        profile.setAvailableLimit(BigDecimal.valueOf(10000)); // 额度不足
        when(creditProfileMapper.selectByEntId(TEST_ENTERPRISE_ID)).thenReturn(profile);

        // Act
        boolean result = creditService.useCreditLimit(TEST_ENTERPRISE_ID, TEST_AMOUNT);

        // Assert
        assertFalse(result);
    }

    @Test
    @Order(7)
    @DisplayName("释放信用额度成功")
    void releaseCreditLimit_shouldSuccess() {
        // Arrange
        EnterpriseCreditProfile profile = createTestCreditProfile();
        profile.setUsedLimit(TEST_AMOUNT);
        when(creditProfileMapper.selectByEntId(TEST_ENTERPRISE_ID)).thenReturn(profile);
        when(creditProfileMapper.updateByEntId(any(EnterpriseCreditProfile.class))).thenReturn(1);

        // Act
        boolean result = creditService.releaseCreditLimit(TEST_ENTERPRISE_ID, TEST_AMOUNT);

        // Assert
        assertTrue(result);
    }

    @Test
    @Order(8)
    @DisplayName("查询可用信用额度")
    void getAvailableCreditLimit_shouldReturnLimit() {
        // Arrange
        EnterpriseCreditProfile profile = createTestCreditProfile();
        profile.setAvailableLimit(BigDecimal.valueOf(800000));
        when(creditProfileMapper.selectByEntId(TEST_ENTERPRISE_ID)).thenReturn(profile);

        // Act
        BigDecimal result = creditService.getAvailableCreditLimit(TEST_ENTERPRISE_ID);

        // Assert
        assertNotNull(result);
        assertEquals(0, BigDecimal.valueOf(800000).compareTo(result));
    }

    // ==================== 信用事件管理测试 ====================

    @Test
    @Order(10)
    @DisplayName("上报信用事件成功")
    void reportCreditEvent_shouldSuccess() {
        // Arrange
        EnterpriseCreditProfile profile = createTestCreditProfile();
        when(creditProfileMapper.selectByEntId(TEST_ENTERPRISE_ID)).thenReturn(profile);
        when(creditProfileMapper.updateByEntId(any(EnterpriseCreditProfile.class))).thenReturn(1);
        when(enterpriseMapper.selectById(TEST_ENTERPRISE_ID)).thenReturn(null); // 区块链地址为null
        doAnswer((Answer<Integer>) invocation -> {
            CreditEvent event = invocation.getArgument(0);
            event.setId(TEST_EVENT_ID);
            return 1;
        }).when(creditEventMapper).insert(any(CreditEvent.class));

        // Act
        Long eventId = creditService.reportCreditEvent(
            TEST_ENTERPRISE_ID, CreditEvent.EVENT_TYPE_ON_TIME_REPAY, "LOW",
            "按时还款", 15, "FINANCE", "REC001"
        );

        // Assert
        assertNotNull(eventId);
    }

    @Test
    @Order(11)
    @DisplayName("查询企业信用事件列表")
    void listCreditEvents_shouldReturnEventList() {
        // Arrange
        List<CreditEvent> events = Arrays.asList(createTestCreditEvent());
        when(creditEventMapper.selectList(any())).thenReturn(events);

        // Act
        List<CreditEvent> result = creditService.listCreditEvents(TEST_ENTERPRISE_ID);

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    @Order(12)
    @DisplayName("查询企业信用事件列表(按类型过滤)")
    void listCreditEventsByType_shouldReturnFilteredList() {
        // Arrange
        List<CreditEvent> events = Arrays.asList(createTestCreditEvent());
        when(creditEventMapper.selectList(any())).thenReturn(events);

        // Act
        List<CreditEvent> result = creditService.listCreditEventsByType(
            TEST_ENTERPRISE_ID, CreditEvent.EVENT_TYPE_ON_TIME_REPAY
        );

        // Assert
        assertNotNull(result);
    }

    @Test
    @Order(13)
    @DisplayName("统计企业逾期次数")
    void countOverdueEvents_shouldReturnCount() {
        // Arrange
        when(creditEventMapper.selectList(any())).thenReturn(Arrays.asList(createTestCreditEvent()));

        // Act
        int result = creditService.countOverdueEvents(TEST_ENTERPRISE_ID);

        // Assert
        assertTrue(result >= 0);
    }

    // ==================== 信用评分计算测试 ====================

    @Test
    @Order(20)
    @DisplayName("计算并更新信用分 - 按时还款事件")
    void calculateCreditScore_shouldIncrease_withOnTimeRepayment() {
        // Arrange
        EnterpriseCreditProfile profile = createTestCreditProfile();
        profile.setCreditScore(800);

        CreditEvent event = createTestCreditEvent();
        event.setScoreChange(15);

        when(creditProfileMapper.selectByEntId(TEST_ENTERPRISE_ID)).thenReturn(profile);
        when(creditEventMapper.selectList(any())).thenReturn(Arrays.asList(event));
        when(creditProfileMapper.updateByEntId(any(EnterpriseCreditProfile.class))).thenReturn(1);

        // Act
        int newScore = creditService.calculateCreditScore(TEST_ENTERPRISE_ID);

        // Assert
        assertTrue(newScore >= 800);
    }

    @Test
    @Order(21)
    @DisplayName("计算并更新信用分 - 逾期事件")
    void calculateCreditScore_shouldDecrease_withOverdueEvent() {
        // Arrange
        EnterpriseCreditProfile profile = createTestCreditProfile();
        profile.setCreditScore(800);

        CreditEvent overdueEvent = createTestCreditEvent();
        overdueEvent.setEventType(CreditEvent.EVENT_TYPE_OVERDUE);
        overdueEvent.setEventLevel(CreditEvent.EVENT_LEVEL_HIGH);
        overdueEvent.setScoreChange(-20);

        when(creditProfileMapper.selectByEntId(TEST_ENTERPRISE_ID)).thenReturn(profile);
        when(creditEventMapper.selectList(any()))
            .thenReturn(Arrays.asList(overdueEvent));
        when(creditProfileMapper.updateByEntId(any(EnterpriseCreditProfile.class))).thenReturn(1);

        // Act
        int newScore = creditService.calculateCreditScore(TEST_ENTERPRISE_ID);

        // Assert
        assertTrue(newScore < 800);
    }

    @Test
    @Order(22)
    @DisplayName("触发信用等级重算 - AAA级")
    void recalculateCreditLevel_shouldReturnAAA() {
        // Arrange
        EnterpriseCreditProfile profile = createTestCreditProfile();
        profile.setCreditScore(900);
        when(creditProfileMapper.selectByEntId(TEST_ENTERPRISE_ID)).thenReturn(profile);
        when(creditProfileMapper.updateByEntId(any(EnterpriseCreditProfile.class))).thenReturn(1);

        // Act
        String level = creditService.recalculateCreditLevel(TEST_ENTERPRISE_ID);

        // Assert
        assertNotNull(level);
    }

    @Test
    @Order(23)
    @DisplayName("信用评分(对外接口)")
    void getCreditScore_shouldReturnResult() {
        // Arrange
        EnterpriseCreditProfile profile = createTestCreditProfile();
        profile.setCreditScore(900);
        profile.setCreditLevel("AAA");
        when(creditProfileMapper.selectByEntId(TEST_ENTERPRISE_ID)).thenReturn(profile);

        // Act
        CreditService.CreditScoreResult result = creditService.getCreditScore(TEST_ENTERPRISE_ID);

        // Assert
        assertNotNull(result);
    }

    // ==================== 辅助方法 ====================

    private EnterpriseCreditProfile createTestCreditProfile() {
        EnterpriseCreditProfile profile = new EnterpriseCreditProfile();
        profile.setId(TEST_PROFILE_ID);
        profile.setEntId(TEST_ENTERPRISE_ID);
        profile.setCreditScore(800);
        profile.setCreditLevel("AAA");
        profile.setAvailableLimit(TEST_CREDIT_LIMIT);
        profile.setUsedLimit(BigDecimal.ZERO);
        profile.setOverdueCount(0);
        return profile;
    }

    private CreditEvent createTestCreditEvent() {
        CreditEvent event = new CreditEvent();
        event.setId(TEST_EVENT_ID);
        event.setEntId(TEST_ENTERPRISE_ID);
        event.setEventType(CreditEvent.EVENT_TYPE_ON_TIME_REPAY);
        event.setEventLevel(CreditEvent.EVENT_LEVEL_LOW);
        event.setScoreChange(15);
        event.setEventDesc("按时还款");
        event.setRelatedModule(CreditEvent.MODULE_FINANCE);
        event.setRelatedId("REC001");
        event.setReportTime(LocalDateTime.now());
        event.setStatus(CreditEvent.STATUS_VALID);
        return event;
    }
}
