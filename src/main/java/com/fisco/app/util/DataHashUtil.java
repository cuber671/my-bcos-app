package com.fisco.app.util;

import com.fisco.app.entity.Bill;
import com.fisco.app.entity.ElectronicWarehouseReceipt;
import com.fisco.app.entity.Receivable;
import com.fisco.app.entity.WarehouseReceipt;
import lombok.extern.slf4j.Slf4j;
import org.fisco.bcos.sdk.v3.crypto.CryptoSuite;
import org.fisco.bcos.sdk.v3.model.CryptoType;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 数据哈希工具类
 * 用于计算链下数据的哈希值，确保数据完整性
 *
 * 使用 Keccak-256 算法（FISCO BCOS 标准）
 */
@Slf4j
@Component
public class DataHashUtil {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final CryptoSuite cryptoSuite;

    public DataHashUtil() {
        // FISCO BCOS v3 使用 SM2/SM3/SM4 国密算法套件，或者标准的 ECDSA/Keccak-256
        // 这里使用标准套件（Keccak-256）
        this.cryptoSuite = new CryptoSuite(CryptoType.ECDSA_TYPE);
    }

    /**
     * 计算应收账款的 dataHash
     * 包含所有链下字段：receivableId, currency, description, createdAt, updatedAt
     *
     * @param receivable 应收账款实体
     * @return 32字节哈希值
     */
    public byte[] calculateReceivableDataHash(Receivable receivable) {
        if (receivable == null) {
            throw new IllegalArgumentException("Receivable entity cannot be null");
        }

        // 按照固定顺序拼接所有链下字段
        String data = String.join("|",
            nullToString(receivable.getId()),
            nullToString(receivable.getCurrency()),
            nullToString(receivable.getDescription()),
            formatDateTime(receivable.getCreatedAt()),
            formatDateTime(receivable.getUpdatedAt())
        );

        log.debug("Calculating hash for Receivable: {}", data);
        return hash(data);
    }

    /**
     * 计算票据的 dataHash
     * 包含所有链下字段：billId, billType, currency, description, createdAt, updatedAt
     *
     * @param bill 票据实体
     * @return 32字节哈希值
     */
    public byte[] calculateBillDataHash(Bill bill) {
        if (bill == null) {
            throw new IllegalArgumentException("Bill entity cannot be null");
        }

        // 按照固定顺序拼接所有链下字段
        String data = String.join("|",
            nullToString(bill.getBillId()),
            nullToString(bill.getBillType()),
            nullToString(bill.getCurrency()),
            nullToString(bill.getRemarks()),
            formatDateTime(bill.getCreatedAt()),
            formatDateTime(bill.getUpdatedAt())
        );

        log.debug("Calculating hash for Bill: {}", data);
        return hash(data);
    }

    /**
     * 计算仓单的 dataHash
     * 包含所有链下字段：receiptId, 货物信息, warehouseLocation, createdAt, updatedAt
     *
     * @param receipt 仓单实体
     * @return 32字节哈希值
     */
    public byte[] calculateWarehouseReceiptDataHash(WarehouseReceipt receipt) {
        if (receipt == null) {
            throw new IllegalArgumentException("WarehouseReceipt entity cannot be null");
        }

        // 按照固定顺序拼接所有链下字段
        String data = String.join("|",
            nullToString(receipt.getId()),
            nullToString(receipt.getGoodsName()),
            nullToString(receipt.getGoodsType()),
            nullToString(receipt.getQuantity()),
            nullToString(receipt.getUnit()),
            nullToString(receipt.getUnitPrice()),
            nullToString(receipt.getQuality()),
            nullToString(receipt.getOrigin()),
            nullToString(receipt.getWarehouseLocation()),
            formatDateTime(receipt.getCreatedAt()),
            formatDateTime(receipt.getUpdatedAt())
        );

        log.debug("Calculating hash for WarehouseReceipt: {}", data);
        return hash(data);
    }

    /**
     * 计算电子仓单数据的哈希值（ElectronicWarehouseReceipt版本）
     *
     * @param receipt 电子仓单实体
     * @return 32字节哈希值
     */
    public byte[] calculateWarehouseReceiptDataHash(ElectronicWarehouseReceipt receipt) {
        if (receipt == null) {
            throw new IllegalArgumentException("ElectronicWarehouseReceipt entity cannot be null");
        }

        // 按照固定顺序拼接所有链下字段
        String data = String.join("|",
            nullToString(receipt.getId()),
            nullToString(receipt.getReceiptNo()),
            nullToString(receipt.getGoodsName()),
            nullToString(receipt.getQuantity()),
            nullToString(receipt.getUnit()),
            nullToString(receipt.getUnitPrice()),
            nullToString(receipt.getTotalValue()),
            nullToString(receipt.getMarketPrice()),
            nullToString(receipt.getWarehouseLocation()),
            nullToString(receipt.getStorageLocation()),
            formatDateTime(receipt.getStorageDate()),
            formatDateTime(receipt.getExpiryDate()),
            formatDateTime(receipt.getCreatedAt()),
            formatDateTime(receipt.getUpdatedAt())
        );

        log.debug("Calculating hash for ElectronicWarehouseReceipt: {}", data);
        return hash(data);
    }

    /**
     * 使用 Keccak-256 计算哈希值
     *
     * @param data 待哈希的字符串
     * @return 32字节哈希值
     */
    private byte[] hash(String data) {
        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
        byte[] hash = cryptoSuite.hash(dataBytes);
        log.debug("Hash input length: {}, Hash output length: {}", dataBytes.length, hash.length);
        return hash;
    }

    /**
     * 格式化日期时间为统一格式
     *
     * @param dateTime 日期时间
     * @return 格式化后的字符串，null 返回空字符串
     */
    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DATE_FORMATTER);
    }

    /**
     * 将对象转换为字符串，null 返回空字符串
     *
     * @param obj 对象
     * @return 字符串表示
     */
    private String nullToString(Object obj) {
        if (obj == null) {
            return "";
        }
        return obj.toString();
    }
}
