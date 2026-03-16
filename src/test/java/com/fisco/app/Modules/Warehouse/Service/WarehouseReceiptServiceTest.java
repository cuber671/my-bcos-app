package com.fisco.app.Modules.Warehouse.Service;

import com.fisco.app.Modules.Warehouse.Entity.WarehouseReceipt;
import com.fisco.app.Modules.Warehouse.Entity.StockOrder;
import com.fisco.app.Modules.Warehouse.Entity.ReceiptEndorsement;
import com.fisco.app.Modules.Warehouse.Mapper.WarehouseReceiptMapper;
import com.fisco.app.Modules.Warehouse.Mapper.StockOrderMapper;
import com.fisco.app.Modules.Warehouse.Mapper.WarehouseMapper;
import com.fisco.app.Modules.Warehouse.Mapper.ReceiptEndorsementMapper;
import com.fisco.app.Modules.Warehouse.Mapper.ReceiptOperationLogMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;

import org.fisco.bcos.sdk.v3.model.TransactionReceipt;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * WarehouseReceiptService 单元测试
 *
 * 测试仓单管理模块的业务逻辑
 * 覆盖：入库、开单、背书转让、质押、解押、出库
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WarehouseReceiptServiceTest {

    @Mock
    private WarehouseReceiptMapper warehouseReceiptMapper;

    @Mock
    private StockOrderMapper stockOrderMapper;

    @Mock
    private ReceiptEndorsementMapper receiptEndorsementMapper;

    @Mock
    private ReceiptOperationLogMapper receiptOperationLogMapper;

    @Mock
    private WarehouseMapper warehouseMapper;

    @Mock
    private WarehouseReceiptContractService warehouseContractService;

    @InjectMocks
    private WarehouseReceiptServiceImpl warehouseReceiptService;

    // 测试数据
    private static final Long TEST_RECEIPT_ID = 5001L;
    private static final Long TEST_STOCK_ORDER_ID = 6001L;
    private static final Long TEST_ENTERPRISE_ID = 2001L;
    private static final Long TEST_WAREHOUSE_ID = 7001L;
    private static final Long TEST_USER_ID = 8001L;
    private static final Long TEST_USER_ID_2 = 8002L;
    private static final String TEST_ON_CHAIN_ID = "chainReceipt001";
    private static final String TEST_STOCK_NO = "STOCK20260313001";

    // ==================== 入库单测试 ====================

    @Test
    @Order(1)
    @DisplayName("申请入库成功")
    void applyStockIn_shouldSuccess() {
        // Arrange
        doAnswer((Answer<Integer>) invocation -> {
            StockOrder order = invocation.getArgument(0);
            order.setId(TEST_STOCK_ORDER_ID);
            return 1;
        }).when(stockOrderMapper).insert(any(StockOrder.class));
        when(stockOrderMapper.updateById(any(StockOrder.class))).thenReturn(1);

        // Act
        Long orderId = warehouseReceiptService.applyStockIn(
            TEST_WAREHOUSE_ID, TEST_ENTERPRISE_ID, TEST_USER_ID,
            "钢材", new BigDecimal("100.000"), "吨", null
        );

        // Assert
        assertNotNull(orderId);
    }

    @Test
    @Order(2)
    @DisplayName("确认入库单成功")
    void confirmStockOrder_shouldSuccess() {
        // Arrange
        StockOrder order = createTestStockOrder();
        order.setStatus(StockOrder.STATUS_PENDING);
        when(stockOrderMapper.selectById(TEST_STOCK_ORDER_ID)).thenReturn(order);
        when(stockOrderMapper.updateById(any(StockOrder.class))).thenReturn(1);

        // Act
        boolean result = warehouseReceiptService.confirmStockOrder(TEST_STOCK_ORDER_ID);

        // Assert
        assertTrue(result);
    }

    @Test
    @Order(3)
    @DisplayName("确认入库单失败 - 订单不存在")
    void confirmStockOrder_shouldFail_whenNotFound() {
        // Arrange
        when(stockOrderMapper.selectById(TEST_STOCK_ORDER_ID)).thenReturn(null);

        // Act
        boolean result = warehouseReceiptService.confirmStockOrder(TEST_STOCK_ORDER_ID);

        // Assert
        assertFalse(result);
    }

    @Test
    @Order(4)
    @DisplayName("取消入库单成功")
    void cancelStockOrder_shouldSuccess() {
        // Arrange
        StockOrder order = createTestStockOrder();
        order.setStatus(StockOrder.STATUS_PENDING);
        when(stockOrderMapper.selectById(TEST_STOCK_ORDER_ID)).thenReturn(order);
        when(stockOrderMapper.updateById(any(StockOrder.class))).thenReturn(1);

        // Act
        boolean result = warehouseReceiptService.cancelStockOrder(TEST_STOCK_ORDER_ID);

        // Assert
        assertTrue(result);
    }

    @Test
    @Order(5)
    @DisplayName("根据ID查询入库单")
    void getStockOrderById_shouldReturnOrder() {
        // Arrange
        StockOrder order = createTestStockOrder();
        when(stockOrderMapper.selectById(TEST_STOCK_ORDER_ID)).thenReturn(order);

        // Act
        StockOrder result = warehouseReceiptService.getStockOrderById(TEST_STOCK_ORDER_ID);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_STOCK_ORDER_ID, result.getId());
    }

    @Test
    @Order(6)
    @DisplayName("根据企业ID查询入库单列表")
    void getStockOrdersByEntId_shouldReturnList() {
        // Arrange
        List<StockOrder> orders = Arrays.asList(createTestStockOrder());
        when(stockOrderMapper.selectList(any())).thenReturn(orders);

        // Act
        List<StockOrder> result = warehouseReceiptService.getStockOrdersByEntId(TEST_ENTERPRISE_ID);

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    // ==================== 仓单签发测试 ====================

    @Test
    @Order(10)
    @DisplayName("签发仓单成功")
    void mintReceipt_shouldSuccess() {
        // Arrange
        StockOrder order = createTestStockOrder();
        order.setStatus(StockOrder.STATUS_CONFIRMED);
        when(stockOrderMapper.selectById(TEST_STOCK_ORDER_ID)).thenReturn(order);
        // Mock blockchain service to return a non-null receipt using doAnswer
        doAnswer((Answer<TransactionReceipt>) invocation -> {
            return new TransactionReceipt();
        }).when(warehouseContractService).issueReceipt(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        // Mock insert to set ID
        doAnswer((Answer<Integer>) invocation -> {
            WarehouseReceipt receipt = invocation.getArgument(0);
            receipt.setId(TEST_RECEIPT_ID);
            return 1;
        }).when(warehouseReceiptMapper).insert(any(WarehouseReceipt.class));

        // Act
        Long receiptId = warehouseReceiptService.mintReceipt(
            TEST_STOCK_ORDER_ID, TEST_USER_ID, TEST_ON_CHAIN_ID
        );

        // Assert
        assertNotNull(receiptId);
    }

    @Test
    @Order(11)
    @DisplayName("签发仓单失败 - 入库单未确认")
    void mintReceipt_shouldFail_whenStockOrderNotConfirmed() {
        // Arrange
        StockOrder order = createTestStockOrder();
        order.setStatus(StockOrder.STATUS_PENDING);
        when(stockOrderMapper.selectById(TEST_STOCK_ORDER_ID)).thenReturn(order);

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
            warehouseReceiptService.mintReceipt(TEST_STOCK_ORDER_ID, TEST_USER_ID, TEST_ON_CHAIN_ID)
        );
    }

    @Test
    @Order(12)
    @DisplayName("根据ID查询仓单")
    void getReceiptById_shouldReturnReceipt() {
        // Arrange
        WarehouseReceipt receipt = createTestReceipt();
        when(warehouseReceiptMapper.selectById(TEST_RECEIPT_ID)).thenReturn(receipt);

        // Act
        WarehouseReceipt result = warehouseReceiptService.getReceiptById(TEST_RECEIPT_ID);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_RECEIPT_ID, result.getId());
    }

    @Test
    @Order(13)
    @DisplayName("根据企业ID查询仓单列表")
    void getReceiptsByEntId_shouldReturnList() {
        // Arrange
        List<WarehouseReceipt> receipts = Arrays.asList(createTestReceipt());
        when(warehouseReceiptMapper.selectList(any())).thenReturn(receipts);

        // Act
        List<WarehouseReceipt> result = warehouseReceiptService.getReceiptsByEntId(TEST_ENTERPRISE_ID);

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    @Order(14)
    @DisplayName("查询企业在库仓单列表")
    void getInStockReceipts_shouldReturnList() {
        // Arrange
        List<WarehouseReceipt> receipts = Arrays.asList(createTestReceipt());
        when(warehouseReceiptMapper.selectList(any())).thenReturn(receipts);

        // Act
        List<WarehouseReceipt> result = warehouseReceiptService.getInStockReceipts(TEST_ENTERPRISE_ID);

        // Assert
        assertNotNull(result);
    }

    // ==================== 背书转让测试 ====================

    @Test
    @Order(20)
    @DisplayName("发起背书转让成功")
    void launchEndorsement_shouldSuccess() {
        // Arrange
        WarehouseReceipt receipt = createTestReceipt();
        receipt.setStatus(WarehouseReceipt.STATUS_IN_STOCK);
        receipt.setIsLocked(false);
        when(warehouseReceiptMapper.selectById(TEST_RECEIPT_ID)).thenReturn(receipt);
        // Mock insert to set ID
        doAnswer((Answer<Integer>) invocation -> {
            ReceiptEndorsement endorsement = invocation.getArgument(0);
            endorsement.setId(TEST_RECEIPT_ID);
            return 1;
        }).when(receiptEndorsementMapper).insert(any(ReceiptEndorsement.class));
        when(warehouseReceiptMapper.updateById(any(WarehouseReceipt.class))).thenReturn(1);

        // Act
        Long endorsementId = warehouseReceiptService.launchEndorsement(
            TEST_RECEIPT_ID, TEST_USER_ID, TEST_ENTERPRISE_ID, "signatureHash123"
        );

        // Assert
        assertNotNull(endorsementId);
    }

    @Test
    @Order(21)
    @DisplayName("发起背书转让失败 - 仓单已锁定")
    void launchEndorsement_shouldFail_whenLocked() {
        // Arrange
        WarehouseReceipt receipt = createTestReceipt();
        receipt.setIsLocked(true);
        when(warehouseReceiptMapper.selectById(TEST_RECEIPT_ID)).thenReturn(receipt);

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
            warehouseReceiptService.launchEndorsement(
                TEST_RECEIPT_ID, TEST_USER_ID, TEST_ENTERPRISE_ID, "signatureHash123"
            )
        );
    }

    @Test
    @Order(22)
    @DisplayName("确认背书转让成功")
    void confirmEndorsement_shouldSuccess() {
        // Arrange
        ReceiptEndorsement endorsement = createTestEndorsement();
        endorsement.setStatus(ReceiptEndorsement.STATUS_PENDING);
        when(receiptEndorsementMapper.selectById(TEST_RECEIPT_ID)).thenReturn(endorsement);

        WarehouseReceipt receipt = createTestReceipt();
        receipt.setOnChainId(TEST_ON_CHAIN_ID);
        when(warehouseReceiptMapper.selectById(endorsement.getReceiptId())).thenReturn(receipt);
        // Mock blockchain service to return non-null receipt
        doAnswer((Answer<TransactionReceipt>) invocation -> new TransactionReceipt())
            .when(warehouseContractService).confirmEndorsement(anyString(), any(), any());
        when(receiptEndorsementMapper.updateById(any(ReceiptEndorsement.class))).thenReturn(1);
        when(warehouseReceiptMapper.updateById(any(WarehouseReceipt.class))).thenReturn(1);

        // Act
        boolean result = warehouseReceiptService.confirmEndorsement(
            TEST_RECEIPT_ID, TEST_USER_ID_2, true
        );

        // Assert
        assertTrue(result);
    }

    @Test
    @Order(23)
    @DisplayName("拒绝背书转让成功")
    void confirmEndorsement_shouldReject() {
        // Arrange
        ReceiptEndorsement endorsement = createTestEndorsement();
        endorsement.setStatus(ReceiptEndorsement.STATUS_PENDING);
        when(receiptEndorsementMapper.selectById(TEST_RECEIPT_ID)).thenReturn(endorsement);

        WarehouseReceipt receipt = createTestReceipt();
        when(warehouseReceiptMapper.selectById(endorsement.getReceiptId())).thenReturn(receipt);
        when(receiptEndorsementMapper.updateById(any(ReceiptEndorsement.class))).thenReturn(1);
        when(warehouseReceiptMapper.updateById(any(WarehouseReceipt.class))).thenReturn(1);

        // Act
        boolean result = warehouseReceiptService.confirmEndorsement(
            TEST_RECEIPT_ID, TEST_USER_ID_2, false
        );

        // Assert
        assertTrue(result);
    }

    @Test
    @Order(24)
    @DisplayName("撤回背书转让成功")
    void revokeEndorsement_shouldSuccess() {
        // Arrange
        ReceiptEndorsement endorsement = createTestEndorsement();
        endorsement.setStatus(ReceiptEndorsement.STATUS_PENDING);
        when(receiptEndorsementMapper.selectById(TEST_RECEIPT_ID)).thenReturn(endorsement);
        when(receiptEndorsementMapper.updateById(any(ReceiptEndorsement.class))).thenReturn(1);

        WarehouseReceipt receipt = createTestReceipt();
        when(warehouseReceiptMapper.selectById(endorsement.getReceiptId())).thenReturn(receipt);
        when(warehouseReceiptMapper.updateById(any(WarehouseReceipt.class))).thenReturn(1);

        // Act
        boolean result = warehouseReceiptService.revokeEndorsement(TEST_RECEIPT_ID);

        // Assert
        assertTrue(result);
    }

    // ==================== 质押/解锁测试 ====================

    @Test
    @Order(30)
    @DisplayName("仓单质押成功")
    void lockReceipt_shouldSuccess() {
        // Arrange
        WarehouseReceipt receipt = createTestReceipt();
        receipt.setIsLocked(false);
        receipt.setOnChainId(TEST_ON_CHAIN_ID);
        when(warehouseReceiptMapper.selectById(TEST_RECEIPT_ID)).thenReturn(receipt);
        when(warehouseReceiptMapper.updateById(any(WarehouseReceipt.class))).thenReturn(1);

        // Act
        boolean result = warehouseReceiptService.lockReceipt(TEST_RECEIPT_ID, "LOAN001");

        // Assert
        assertTrue(result);
    }

    @Test
    @Order(31)
    @DisplayName("仓单质押失败 - 已质押")
    void lockReceipt_shouldFail_whenAlreadyLocked() {
        // Arrange
        WarehouseReceipt receipt = createTestReceipt();
        receipt.setIsLocked(true);
        when(warehouseReceiptMapper.selectById(TEST_RECEIPT_ID)).thenReturn(receipt);

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
            warehouseReceiptService.lockReceipt(TEST_RECEIPT_ID, "LOAN001")
        );
    }

    @Test
    @Order(32)
    @DisplayName("仓单解锁成功")
    void unlockReceipt_shouldSuccess() {
        // Arrange
        WarehouseReceipt receipt = createTestReceipt();
        receipt.setIsLocked(true);
        receipt.setOnChainId(TEST_ON_CHAIN_ID);
        when(warehouseReceiptMapper.selectById(TEST_RECEIPT_ID)).thenReturn(receipt);
        when(warehouseReceiptMapper.updateById(any(WarehouseReceipt.class))).thenReturn(1);

        // Act
        boolean result = warehouseReceiptService.unlockReceipt(TEST_RECEIPT_ID);

        // Assert
        assertTrue(result);
    }

    @Test
    @Order(33)
    @DisplayName("仓单解锁失败 - 未质押")
    void unlockReceipt_shouldFail_whenNotLocked() {
        // Arrange
        WarehouseReceipt receipt = createTestReceipt();
        receipt.setIsLocked(false);
        when(warehouseReceiptMapper.selectById(TEST_RECEIPT_ID)).thenReturn(receipt);

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
            warehouseReceiptService.unlockReceipt(TEST_RECEIPT_ID)
        );
    }

    // ==================== 核销出库测试 ====================

    @Test
    @Order(40)
    @DisplayName("申请核销出库成功")
    void applyBurn_shouldSuccess() {
        // Arrange
        WarehouseReceipt receipt = createTestReceipt();
        receipt.setIsLocked(false);
        when(warehouseReceiptMapper.selectById(TEST_RECEIPT_ID)).thenReturn(receipt);
        // Mock insert to set ID
        doAnswer((Answer<Integer>) invocation -> {
            StockOrder order = invocation.getArgument(0);
            order.setId(TEST_STOCK_ORDER_ID);
            return 1;
        }).when(stockOrderMapper).insert(any(StockOrder.class));
        when(warehouseReceiptMapper.updateById(any(WarehouseReceipt.class))).thenReturn(1);

        // Act
        Long stockOrderId = warehouseReceiptService.applyBurn(
            TEST_RECEIPT_ID, TEST_USER_ID, "signatureHash123"
        );

        // Assert
        assertNotNull(stockOrderId);
    }

    @Test
    @Order(41)
    @DisplayName("申请核销出库失败 - 已锁定")
    void applyBurn_shouldFail_whenLocked() {
        // Arrange
        WarehouseReceipt receipt = createTestReceipt();
        receipt.setIsLocked(true);
        when(warehouseReceiptMapper.selectById(TEST_RECEIPT_ID)).thenReturn(receipt);

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
            warehouseReceiptService.applyBurn(TEST_RECEIPT_ID, TEST_USER_ID, "signatureHash123")
        );
    }

    @Test
    @Order(42)
    @DisplayName("确认核销出库成功")
    void confirmBurn_shouldSuccess() {
        // Arrange
        StockOrder order = createTestStockOrder();
        order.setStatus(StockOrder.STATUS_PENDING);
        when(stockOrderMapper.selectById(TEST_STOCK_ORDER_ID)).thenReturn(order);

        WarehouseReceipt receipt = createTestReceipt();
        receipt.setStatus(WarehouseReceipt.STATUS_IN_TRANSIT);
        receipt.setOnChainId(TEST_ON_CHAIN_ID);
        when(warehouseReceiptMapper.selectOne(any())).thenReturn(receipt);
        // Mock blockchain service to return non-null receipt
        doAnswer((Answer<TransactionReceipt>) invocation -> new TransactionReceipt())
            .when(warehouseContractService).burnReceipt(anyString(), any());
        when(warehouseReceiptMapper.updateById(any(WarehouseReceipt.class))).thenReturn(1);
        when(stockOrderMapper.updateById(any(StockOrder.class))).thenReturn(1);

        // Act
        boolean result = warehouseReceiptService.confirmBurn(TEST_STOCK_ORDER_ID, TEST_USER_ID);

        // Assert
        assertTrue(result);
    }

    @Test
    @Order(43)
    @DisplayName("确认核销出库失败 - 订单不存在")
    void confirmBurn_shouldFail_whenNotFound() {
        // Arrange
        when(stockOrderMapper.selectById(TEST_STOCK_ORDER_ID)).thenReturn(null);

        // Act
        boolean result = warehouseReceiptService.confirmBurn(TEST_STOCK_ORDER_ID, TEST_USER_ID);

        // Assert
        assertFalse(result);
    }

    // ==================== 辅助方法 ====================

    private StockOrder createTestStockOrder() {
        StockOrder order = new StockOrder();
        order.setId(TEST_STOCK_ORDER_ID);
        order.setStockNo(TEST_STOCK_NO);
        order.setWarehouseId(TEST_WAREHOUSE_ID);
        order.setEntId(TEST_ENTERPRISE_ID);
        order.setUserId(TEST_USER_ID);
        order.setGoodsName("钢材");
        order.setWeight(new BigDecimal("100.000"));
        order.setUnit("吨");
        order.setStatus(StockOrder.STATUS_PENDING);
        return order;
    }

    private WarehouseReceipt createTestReceipt() {
        WarehouseReceipt receipt = new WarehouseReceipt();
        receipt.setId(TEST_RECEIPT_ID);
        receipt.setWarehouseId(TEST_WAREHOUSE_ID);
        receipt.setOnChainId(TEST_ON_CHAIN_ID);
        receipt.setOwnerEntId(TEST_ENTERPRISE_ID);
        receipt.setOwnerUserId(TEST_USER_ID);
        receipt.setGoodsName("钢材");
        receipt.setWeight(new BigDecimal("100.000"));
        receipt.setUnit("吨");
        receipt.setStatus(WarehouseReceipt.STATUS_IN_STOCK);
        receipt.setIsLocked(false);
        return receipt;
    }

    private ReceiptEndorsement createTestEndorsement() {
        ReceiptEndorsement endorsement = new ReceiptEndorsement();
        endorsement.setId(TEST_RECEIPT_ID);
        endorsement.setReceiptId(TEST_RECEIPT_ID);
        endorsement.setTransferorEntId(TEST_ENTERPRISE_ID);
        endorsement.setTransferorUserId(TEST_USER_ID);
        endorsement.setTransfereeEntId(TEST_ENTERPRISE_ID);
        endorsement.setSignatureHash("signatureHash123");
        endorsement.setStatus(ReceiptEndorsement.STATUS_PENDING);
        return endorsement;
    }
}
