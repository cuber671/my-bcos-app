package com.fisco.app.Modules.Logistics.Service;

import com.fisco.app.Modules.Logistics.Entity.LogisticsDelegate;
import com.fisco.app.Modules.Logistics.Entity.LogisticsTrack;
import com.fisco.app.Modules.Logistics.Mapper.LogisticsDelegateMapper;
import com.fisco.app.Modules.Logistics.Mapper.LogisticsTrackMapper;
import com.fisco.app.Modules.Logistics.Service.impl.LogisticsServiceImpl;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * LogisticsService 单元测试
 *
 * 测试物流委派模块的业务逻辑
 * 覆盖：委派单创建、状态流转、轨迹追踪
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LogisticsServiceTest {

    @Mock
    private LogisticsDelegateMapper delegateMapper;

    @Mock
    private LogisticsTrackMapper trackMapper;

    @Mock
    private LogisticsContractService contractService;

    @InjectMocks
    private LogisticsServiceImpl logisticsService;

    // 测试数据
    private static final Long TEST_DELEGATE_ID = 8001L;
    private static final Long TEST_ENTERPRISE_ID = 2001L;
    private static final Long TEST_RECEIPT_ID = 5001L;
    private static final Long TEST_WAREHOUSE_ID = 7001L;
    private static final String TEST_VOUCHER_NO = "LOG20260313001";

    // ==================== 委派单查询测试 ====================

    @Test
    @Order(10)
    @DisplayName("根据ID查询委派单")
    void getDelegateById_shouldReturnDelegate() {
        // Arrange
        LogisticsDelegate delegate = createTestDelegate();
        when(delegateMapper.selectById(TEST_DELEGATE_ID)).thenReturn(delegate);

        // Act
        LogisticsDelegate result = logisticsService.getDelegateById(TEST_DELEGATE_ID);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_DELEGATE_ID, result.getId());
    }

    @Test
    @Order(11)
    @DisplayName("根据凭证号查询委派单")
    void getDelegateByVoucherNo_shouldReturnDelegate() {
        // Arrange
        LogisticsDelegate delegate = createTestDelegate();
        when(delegateMapper.selectByVoucherNo(TEST_VOUCHER_NO)).thenReturn(delegate);

        // Act
        LogisticsDelegate result = logisticsService.getDelegateByVoucherNo(TEST_VOUCHER_NO);

        // Assert
        assertNotNull(result);
    }

    @Test
    @Order(12)
    @DisplayName("根据凭证号查询委派单 - 不存在")
    void getDelegateByVoucherNo_shouldReturnNull_whenNotExists() {
        // Arrange
        when(delegateMapper.selectByVoucherNo(TEST_VOUCHER_NO)).thenReturn(null);

        // Act
        LogisticsDelegate result = logisticsService.getDelegateByVoucherNo(TEST_VOUCHER_NO);

        // Assert
        assertNull(result);
    }

    // ==================== 轨迹追踪测试 ====================

    @Test
    @Order(30)
    @DisplayName("上报轨迹成功")
    void reportTrack_shouldSuccess() {
        // Arrange
        when(trackMapper.insert(any(LogisticsTrack.class))).thenReturn(1);

        // Act
        LogisticsTrack track = createTestTrack();
        LogisticsTrack result = logisticsService.reportTrack(track);

        // Assert
        assertNotNull(result);
    }

    @Test
    @Order(31)
    @DisplayName("查询轨迹列表")
    void listTracks_shouldReturnList() {
        // Arrange
        List<LogisticsTrack> tracks = Arrays.asList(createTestTrack());
        when(trackMapper.selectByVoucherNo(TEST_VOUCHER_NO)).thenReturn(tracks);

        // Act
        List<LogisticsTrack> result = logisticsService.listTracks(TEST_VOUCHER_NO);

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    @Order(32)
    @DisplayName("查询轨迹列表 - 无数据")
    void listTracks_shouldReturnEmptyList_whenNoData() {
        // Arrange
        when(trackMapper.selectByVoucherNo(TEST_VOUCHER_NO)).thenReturn(Collections.emptyList());

        // Act
        List<LogisticsTrack> result = logisticsService.listTracks(TEST_VOUCHER_NO);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @Order(33)
    @DisplayName("查询偏航记录列表")
    void listDeviations_shouldReturnList() {
        // Arrange
        List<LogisticsTrack> tracks = Arrays.asList(createTestTrack());
        when(trackMapper.selectDeviationByVoucherNo(TEST_VOUCHER_NO)).thenReturn(tracks);

        // Act
        List<LogisticsTrack> result = logisticsService.listDeviations(TEST_VOUCHER_NO);

        // Assert
        assertNotNull(result);
    }

    @Test
    @Order(34)
    @DisplayName("查询最新轨迹")
    void getLatestTrack_shouldReturnTrack() {
        // Arrange
        LogisticsTrack track = createTestTrack();
        when(trackMapper.selectLatestByVoucherNo(TEST_VOUCHER_NO)).thenReturn(track);

        // Act
        LogisticsTrack result = logisticsService.getLatestTrack(TEST_VOUCHER_NO);

        // Assert
        assertNotNull(result);
    }

    // ==================== 辅助方法 ====================

    private LogisticsDelegate createTestDelegate() {
        LogisticsDelegate delegate = new LogisticsDelegate();
        delegate.setId(TEST_DELEGATE_ID);
        delegate.setVoucherNo(TEST_VOUCHER_NO);
        delegate.setBusinessScene(LogisticsDelegate.SCENE_DIRECT_TRANSFER);
        delegate.setReceiptId(TEST_RECEIPT_ID);
        delegate.setOwnerEntId(TEST_ENTERPRISE_ID);
        delegate.setCarrierEntId(TEST_ENTERPRISE_ID);
        delegate.setSourceWhId(TEST_WAREHOUSE_ID);
        delegate.setTargetWhId(TEST_WAREHOUSE_ID);
        delegate.setTransportQuantity(new BigDecimal("100.000"));
        delegate.setUnit("吨");
        delegate.setStatus(LogisticsDelegate.STATUS_PENDING);
        delegate.setValidUntil(LocalDateTime.now().plusDays(7));
        return delegate;
    }

    private LogisticsTrack createTestTrack() {
        LogisticsTrack track = new LogisticsTrack();
        track.setVoucherNo(TEST_VOUCHER_NO);
        track.setLatitude(new BigDecimal("39.9042"));
        track.setLongitude(new BigDecimal("116.4074"));
        track.setLocationName("北京");
        track.setStatus(LogisticsTrack.STATUS_IN_TRANSIT);
        track.setIsDeviation(LogisticsTrack.DEVIATION_NO);
        track.setEventTime(LocalDateTime.now());
        return track;
    }
}
