package com.fisco.app.Modules.Logistics.Service.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fisco.app.Modules.Logistics.Entity.LogisticsDelegate;
import com.fisco.app.Modules.Logistics.Entity.LogisticsTrack;
import com.fisco.app.Modules.Logistics.Mapper.LogisticsDelegateMapper;
import com.fisco.app.Modules.Logistics.Mapper.LogisticsTrackMapper;
import com.fisco.app.Modules.Logistics.Service.LogisticsContractService;
import com.fisco.app.Modules.Logistics.Service.LogisticsService;
import com.fisco.app.Modules.Warehouse.Entity.ReceiptEndorsement;
import com.fisco.app.Modules.Warehouse.Entity.Warehouse;
import com.fisco.app.Modules.Warehouse.Entity.WarehouseReceipt;
import com.fisco.app.Modules.Warehouse.Mapper.ReceiptEndorsementMapper;
import com.fisco.app.Modules.Warehouse.Mapper.WarehouseMapper;
import com.fisco.app.Modules.Warehouse.Mapper.WarehouseReceiptMapper;

/**
 * 物流服务实现类
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Service
public class LogisticsServiceImpl implements LogisticsService {

    private static final Logger logger = LoggerFactory.getLogger(LogisticsServiceImpl.class);

    @Autowired
    private LogisticsDelegateMapper delegateMapper;

    @Autowired
    private LogisticsTrackMapper trackMapper;

    @Autowired
    private WarehouseReceiptMapper warehouseReceiptMapper;

    @Autowired
    private WarehouseMapper warehouseMapper;

    @Autowired
    private ReceiptEndorsementMapper receiptEndorsementMapper;

    @Autowired
    private LogisticsContractService logisticsContractService;

    private static final DateTimeFormatter VOUCHER_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Random RANDOM = new Random();

    // ==================== 委派单操作 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LogisticsDelegate createDelegate(LogisticsDelegate delegate) {
        // 生成 voucherNo
        String voucherNo = generateVoucherNo();
        delegate.setVoucherNo(voucherNo);
        delegate.setStatus(LogisticsDelegate.STATUS_PENDING);

        // 校验业务场景必填字段
        validateBusinessScene(delegate);

        // 按业务场景处理
        switch (delegate.getBusinessScene()) {
            case LogisticsDelegate.SCENE_DIRECT_TRANSFER:
                // 场景1：直接移库 - 校验并锁定仓单
                handleSceneDirectTransfer(delegate);
                break;
            case LogisticsDelegate.SCENE_TRANSFER_THEN_TRANSFER:
                // 场景2：转让后移库 - 通过背书获取原仓单信息
                handleSceneTransferThenTransfer(delegate);
                break;
            case LogisticsDelegate.SCENE_DELIVERY_TO_WAREHOUSE:
                // 场景3：发货入库 - 不需要关联现有仓单
                handleSceneDeliveryToWarehouse(delegate);
                break;
            default:
                throw new IllegalArgumentException("不支持的业务场景: " + delegate.getBusinessScene());
        }

        // 设置默认有效期（7天）
        if (delegate.getValidUntil() == null) {
            delegate.setValidUntil(LocalDateTime.now().plusDays(7));
        }

        delegateMapper.insert(delegate);
        logger.info("创建物流委派单成功: voucherNo={}, ownerEntId={}, businessScene={}",
            voucherNo, delegate.getOwnerEntId(), delegate.getBusinessSceneDesc());

        // 区块链上链（失败不影响本地业务）
        try {
            var receipt = logisticsContractService.createLogisticsDelegateCore(voucherNo);
            if (receipt != null && receipt.isStatusOK() && receipt.getTransactionHash() != null) {
                delegate.setChainTxHash(receipt.getTransactionHash());
                delegateMapper.updateById(delegate);
                logger.info("物流委派单上链成功: voucherNo={}, txHash={}", voucherNo, receipt.getTransactionHash());
            } else {
                logger.warn("物流委派单上链失败，状态码: {}, voucherNo={}",
                    receipt != null ? receipt.getStatus() : "null", voucherNo);
            }
        } catch (Exception e) {
            logger.warn("物流委派单上链异常，不影响本地业务: voucherNo={}, error={}", voucherNo, e.getMessage());
        }

        return delegate;
    }

    /**
     * 处理场景1：直接移库
     * 校验并锁定仓单数量
     */
    private void handleSceneDirectTransfer(LogisticsDelegate delegate) {
        if (delegate.getReceiptId() == null) {
            throw new IllegalArgumentException("直接移库场景必须关联仓单");
        }
        WarehouseReceipt receipt = warehouseReceiptMapper.selectById(delegate.getReceiptId());
        if (receipt == null) {
            throw new IllegalArgumentException("关联仓单不存在: " + delegate.getReceiptId());
        }
        // 校验仓单状态（必须在库）
        if (receipt.getStatus() != WarehouseReceipt.STATUS_IN_STOCK) {
            throw new IllegalArgumentException("仓单状态必须为在库");
        }
        // 校验仓单持有人
        if (receipt.getOwnerEntId() != null && !receipt.getOwnerEntId().equals(delegate.getOwnerEntId())) {
            throw new IllegalArgumentException("只有仓单持有人才能发起直接移库");
        }
        // 校验仓单余额是否充足
        if (delegate.getTransportQuantity() != null && receipt.getWeight() != null) {
            if (delegate.getTransportQuantity().compareTo(receipt.getWeight()) > 0) {
                throw new IllegalArgumentException("运输数量超过仓单余额，仓单余额: " + receipt.getWeight() + delegate.getUnit());
            }
        }
        // 锁定对应数量（标记仓单已锁定）
        receipt.setIsLocked(true);
        warehouseReceiptMapper.updateById(receipt);
        logger.info("直接移库-仓单已锁定: receiptId={}, 锁定数量={}", delegate.getReceiptId(), delegate.getTransportQuantity());
    }

    /**
     * 处理场景2：转让后移库
     * 通过背书获取原仓单信息
     */
    private void handleSceneTransferThenTransfer(LogisticsDelegate delegate) {
        if (delegate.getEndorseId() == null) {
            throw new IllegalArgumentException("转让后移库场景必须关联背书");
        }
        ReceiptEndorsement endorsement = receiptEndorsementMapper.selectById(delegate.getEndorseId());
        if (endorsement == null) {
            throw new IllegalArgumentException("背书不存在: " + delegate.getEndorseId());
        }
        // 获取原仓单ID（背书转出方的仓单）
        Long receiptId = endorsement.getReceiptId();
        delegate.setReceiptId(receiptId);

        if (receiptId != null) {
            WarehouseReceipt receipt = warehouseReceiptMapper.selectById(receiptId);
            if (receipt != null) {
                // 设置起运地仓库为原仓单所在仓库
                delegate.setSourceWhId(receipt.getWarehouseId());
                // 校验仓单余额是否充足
                if (delegate.getTransportQuantity() != null && receipt.getWeight() != null) {
                    if (delegate.getTransportQuantity().compareTo(receipt.getWeight()) > 0) {
                        throw new IllegalArgumentException("运输数量超过仓单余额，仓单余额: " + receipt.getWeight() + delegate.getUnit());
                    }
                }
                // 锁定仓单
                receipt.setIsLocked(true);
                warehouseReceiptMapper.updateById(receipt);
                logger.info("转让后移库-仓单已锁定: receiptId={}, 锁定数量={}", receiptId, delegate.getTransportQuantity());
            }
        }
        logger.info("转让后移库-背书关联: endorseId={}, receiptId={}", delegate.getEndorseId(), receiptId);
    }

    /**
     * 处理场景3：发货入库
     * 无需关联现有仓单，只需指定目标仓库
     */
    private void handleSceneDeliveryToWarehouse(LogisticsDelegate delegate) {
        // 发货入库场景不需要关联仓单
        // 校验目标仓库是否存在
        if (delegate.getTargetWhId() != null) {
            Warehouse warehouse = warehouseMapper.selectById(delegate.getTargetWhId());
            if (warehouse == null) {
                throw new IllegalArgumentException("目标仓库不存在: " + delegate.getTargetWhId());
            }
            // 校验仓库状态是否正常
            if (warehouse.getStatus() == null || warehouse.getStatus() != Warehouse.STATUS_NORMAL) {
                logger.warn("目标仓库状态异常: whId={}, status={}", delegate.getTargetWhId(), warehouse.getStatus());
            }
        }
        logger.info("发货入库-创建委派单: voucherNo={}, targetWhId={}", delegate.getVoucherNo(), delegate.getTargetWhId());
    }

    /**
     * 根据物流委派单创建新仓单
     * 用于到货入库时生成新仓单
     *
     * @param delegate 物流委派单
     * @return 新创建的仓单
     */
    private WarehouseReceipt createReceiptFromDelegate(LogisticsDelegate delegate) {
        // 获取原仓单信息（如果存在）
        WarehouseReceipt sourceReceipt = null;
        if (delegate.getReceiptId() != null) {
            sourceReceipt = warehouseReceiptMapper.selectById(delegate.getReceiptId());
        }

        // 创建新仓单
        WarehouseReceipt newReceipt = new WarehouseReceipt();
        // 设置链上ID（使用UUID生成唯一标识）
        newReceipt.setOnChainId("WR" + UUID.randomUUID().toString().replace("-", "").toUpperCase());
        // 设置新仓单持有人（场景2为受让人，场景3为发货企业）
        newReceipt.setOwnerEntId(delegate.getOwnerEntId());
        // 设置仓库
        newReceipt.setWarehouseId(delegate.getTargetWhId());
        // 设置运输数量作为初始重量
        newReceipt.setWeight(delegate.getTransportQuantity());
        // 设置计量单位
        newReceipt.setUnit(delegate.getUnit());
        // 设置状态为在库
        newReceipt.setStatus(WarehouseReceipt.STATUS_IN_STOCK);

        // 如果存在原仓单，复制相关属性
        if (sourceReceipt != null) {
            // 复制品名等属性
            newReceipt.setGoodsName(sourceReceipt.getGoodsName());
            newReceipt.setWarehouseEntId(sourceReceipt.getWarehouseEntId());
        }

        warehouseReceiptMapper.insert(newReceipt);
        logger.info("创建新仓单: onChainId={}, ownerEntId={}, weight={}, warehouseId={}",
            newReceipt.getOnChainId(), newReceipt.getOwnerEntId(), newReceipt.getWeight(), newReceipt.getWarehouseId());

        return newReceipt;
    }

    @Override
    public LogisticsDelegate getDelegateById(Long id) {
        return delegateMapper.selectById(id);
    }

    @Override
    public LogisticsDelegate getDelegateByVoucherNo(String voucherNo) {
        return delegateMapper.selectByVoucherNo(voucherNo);
    }

    @Override
    public List<LogisticsDelegate> listByOwnerEntId(Long ownerEntId) {
        return delegateMapper.selectByOwnerEntId(ownerEntId);
    }

    @Override
    public List<LogisticsDelegate> listByCarrierEntId(Long carrierEntId) {
        return delegateMapper.selectByCarrierEntId(carrierEntId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LogisticsDelegate assignDriver(String voucherNo, String driverId, String driverName, String vehicleNo) {
        LogisticsDelegate delegate = delegateMapper.selectByVoucherNo(voucherNo);
        if (delegate == null) {
            throw new IllegalArgumentException("委派单不存在: " + voucherNo);
        }

        // 状态校验
        if (delegate.getStatus() != LogisticsDelegate.STATUS_PENDING) {
            throw new IllegalArgumentException("当前状态不允许指派司机，当前状态: " + delegate.getStatusDesc());
        }

        // 更新司机信息
        delegate.setDriverId(driverId);
        delegate.setDriverName(driverName);
        delegate.setVehicleNo(vehicleNo);
        delegate.setStatus(LogisticsDelegate.STATUS_ASSIGNED);

        // 生成授权码
        delegate.setAuthCode(generateAuthCode());

        // 生成动态加密二维码
        String qrCode = generatePickupQrCode(voucherNo, driverId, delegate.getAuthCode());
        delegate.setPickupQrCode(qrCode);

        // 计算二维码哈希用于链上存证
        String qrCodeHash = calculateQrCodeHash(qrCode);
        logger.info("二维码哈希计算: voucherNo={}, qrCodeHash={}", voucherNo, qrCodeHash);

        delegateMapper.updateById(delegate);
        logger.info("物流指派任务成功: voucherNo={}, driver={}, vehicleNo={}", voucherNo, driverName, vehicleNo);

        // 区块链上链（失败则抛出异常回滚本地事务）
        try {
            // 将司机信息和二维码哈希打包上链
            String driverInfo = driverId + "|" + driverName + "|" + vehicleNo + "|" + qrCodeHash;
            byte[] driverInfoHash = calculateQrCodeHashBytes(driverInfo);

            var receipt = logisticsContractService.assignCarrier(voucherNo, driverInfoHash);
            if (receipt != null && receipt.getTransactionHash() != null) {
                delegate.setChainTxHash(receipt.getTransactionHash());
                delegateMapper.updateById(delegate);
                logger.info("物流指派上链成功: voucherNo={}, txHash={}, driverInfoHash={}",
                    voucherNo, receipt.getTransactionHash(), qrCodeHash);
            } else {
                throw new RuntimeException("区块链交易回执无效");
            }
        } catch (RuntimeException e) {
            logger.error("物流指派上链失败: voucherNo={}, error={}", voucherNo, e.getMessage());
            throw e;
        }

        return delegate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LogisticsDelegate confirmPickup(String voucherNo, String authCode) {
        LogisticsDelegate delegate = delegateMapper.selectByVoucherNo(voucherNo);
        if (delegate == null) {
            throw new IllegalArgumentException("委派单不存在: " + voucherNo);
        }

        // 状态校验
        if (delegate.getStatus() != LogisticsDelegate.STATUS_ASSIGNED) {
            throw new IllegalArgumentException("当前状态不允许提货，当前状态: " + delegate.getStatusDesc());
        }

        // 授权码校验
        if (delegate.getAuthCode() == null || !delegate.getAuthCode().equals(authCode)) {
            throw new IllegalArgumentException("授权码错误");
        }

        // 更新状态为运输中
        delegate.setStatus(LogisticsDelegate.STATUS_IN_TRANSIT);

        // 如果关联了仓单，扣减仓单数量
        if (delegate.getReceiptId() != null) {
            WarehouseReceipt receipt = warehouseReceiptMapper.selectById(delegate.getReceiptId());
            if (receipt != null) {
                // 扣减数量
                BigDecimal newBalance = receipt.getWeight().subtract(delegate.getTransportQuantity());
                if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("仓单余额不足");
                }
                receipt.setWeight(newBalance);
                // 如果余额为0，注销仓单
                if (newBalance.compareTo(BigDecimal.ZERO) == 0) {
                    receipt.setStatus(WarehouseReceipt.STATUS_BURNED); // 已核销/已注销
                }
                warehouseReceiptMapper.updateById(receipt);
                logger.info("仓单数量扣减: receiptId={}, 扣减数量={}, 新余额={}",
                    delegate.getReceiptId(), delegate.getTransportQuantity(), newBalance);
            }
        }

        delegateMapper.updateById(delegate);
        logger.info("仓库提货确认成功: voucherNo={}", voucherNo);

        // 区块链上链（失败则抛出异常回滚本地事务）
        try {
            BigInteger quantity = delegate.getTransportQuantity() != null
                ? delegate.getTransportQuantity().toBigInteger()
                : BigInteger.ZERO;
            var receipt = logisticsContractService.pickup(voucherNo, quantity);
            if (receipt != null && receipt.getTransactionHash() != null) {
                delegate.setChainTxHash(receipt.getTransactionHash());
                delegateMapper.updateById(delegate);
                logger.info("提货确认上链成功: voucherNo={}, txHash={}", voucherNo, receipt.getTransactionHash());
            } else {
                throw new RuntimeException("区块链交易回执无效");
            }
        } catch (RuntimeException e) {
            logger.error("提货确认上链失败: voucherNo={}, error={}", voucherNo, e.getMessage());
            throw e;
        }

        return delegate;
    }

    /**
     * 仓库提货确认（带地理围栏校验）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LogisticsDelegate confirmPickup(String voucherNo, String authCode,
                                           BigDecimal driverLatitude, BigDecimal driverLongitude) {
        // 1. 地理围栏校验
        if (driverLatitude != null && driverLongitude != null) {
            validateGeofence(voucherNo, driverLatitude, driverLongitude);
        }

        // 2. 调用基础确认方法
        return confirmPickup(voucherNo, authCode);
    }

    /**
     * 校验地理围栏
     * 要求：司机位置与仓库距离 < 500米
     */
    private void validateGeofence(String voucherNo, BigDecimal driverLat, BigDecimal driverLon) {
        LogisticsDelegate delegate = delegateMapper.selectByVoucherNo(voucherNo);
        if (delegate == null) {
            throw new IllegalArgumentException("委派单不存在: " + voucherNo);
        }

        // 获取起运地仓库
        if (delegate.getSourceWhId() == null) {
            logger.warn("未设置起运地仓库，跳过地理围栏校验: voucherNo={}", voucherNo);
            return;
        }

        Warehouse warehouse = warehouseMapper.selectById(delegate.getSourceWhId());
        if (warehouse == null) {
            throw new IllegalArgumentException("起运地仓库不存在: " + delegate.getSourceWhId());
        }

        // 检查仓库是否有坐标信息
        // 注：如果仓库表没有坐标字段，需要先在数据库添加 latitude, longitude 字段
        // 这里假设仓库实体已有 latitude, longitude 属性
        BigDecimal warehouseLat = getWarehouseLatitude(warehouse);
        BigDecimal warehouseLon = getWarehouseLongitude(warehouse);

        if (warehouseLat == null || warehouseLon == null) {
            logger.warn("仓库缺少坐标信息，跳过地理围栏校验: warehouseId={}", delegate.getSourceWhId());
            return;
        }

        // 计算距离（使用 Haversine 公式）
        double distance = calculateDistance(
            warehouseLat.doubleValue(), warehouseLon.doubleValue(),
            driverLat.doubleValue(), driverLon.doubleValue()
        );

        // 校验距离是否在 500 米范围内
        final double GEOFENCE_RADIUS_METERS = 500.0;
        if (distance > GEOFENCE_RADIUS_METERS) {
            throw new IllegalArgumentException(
                String.format("司机位置距离仓库超过允许范围，当前距离 %.1f 米，允许范围 %d 米",
                    distance, (int) GEOFENCE_RADIUS_METERS));
        }

        logger.info("地理围栏校验通过: voucherNo={}, 仓库={}, 距离=%.1f米",
            voucherNo, warehouse.getName(), distance);
    }

    /**
     * 获取仓库纬度（扩展方法）
     * 如果仓库实体没有 latitude 字段，返回 null
     */
    private BigDecimal getWarehouseLatitude(Warehouse warehouse) {
        // 尝试通过反射获取 latitude 字段
        try {
            java.lang.reflect.Field field = warehouse.getClass().getDeclaredField("latitude");
            field.setAccessible(true);
            Object value = field.get(warehouse);
            if (value instanceof BigDecimal) {
                return (BigDecimal) value;
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // 字段不存在，忽略
        }
        return null;
    }

    /**
     * 获取仓库经度（扩展方法）
     * 如果仓库实体没有 longitude 字段，返回 null
     */
    private BigDecimal getWarehouseLongitude(Warehouse warehouse) {
        // 尝试通过反射获取 longitude 字段
        try {
            java.lang.reflect.Field field = warehouse.getClass().getDeclaredField("longitude");
            field.setAccessible(true);
            Object value = field.get(warehouse);
            if (value instanceof BigDecimal) {
                return (BigDecimal) value;
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // 字段不存在，忽略
        }
        return null;
    }

    /**
     * 使用 Haversine 公式计算两点之间的距离
     *
     * @param lat1 起点纬度
     * @param lon1 起点经度
     * @param lat2 终点纬度
     * @param lon2 终点经度
     * @return 距离（米）
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final double EARTH_RADIUS_METERS = 6371000.0;

        // 转换为弧度
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);

        // Haversine 公式
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LogisticsDelegate arrive(String voucherNo, Integer actionType, Long targetReceiptId) {
        LogisticsDelegate delegate = delegateMapper.selectByVoucherNo(voucherNo);
        if (delegate == null) {
            throw new IllegalArgumentException("委派单不存在: " + voucherNo);
        }

        // 状态校验
        if (delegate.getStatus() != LogisticsDelegate.STATUS_IN_TRANSIT) {
            throw new IllegalArgumentException("当前状态不允许到货操作，当前状态: " + delegate.getStatusDesc());
        }

        // 处理动作校验
        if (actionType == LogisticsDelegate.ACTION_CREATE_NEW_RECEIPT) {
            // 生成新仓单 - 为新买家/新仓库创建新仓单
            WarehouseReceipt newReceipt = createReceiptFromDelegate(delegate);
            if (newReceipt != null) {
                delegate.setTargetReceiptId(newReceipt.getId());
                logger.info("到货生成新仓单: voucherNo={}, newReceiptId={}, targetWhId={}",
                    voucherNo, newReceipt.getId(), delegate.getTargetWhId());
            }
        } else if (actionType == LogisticsDelegate.ACTION_MERGE_EXISTING_RECEIPT) {
            // 并入已有仓单
            if (targetReceiptId == null) {
                throw new IllegalArgumentException("增量入库时必须指定目标仓单ID");
            }
            WarehouseReceipt targetReceipt = warehouseReceiptMapper.selectById(targetReceiptId);
            if (targetReceipt == null) {
                throw new IllegalArgumentException("目标仓单不存在: " + targetReceiptId);
            }
            // 增加仓单数量
            targetReceipt.setWeight(targetReceipt.getWeight().add(delegate.getTransportQuantity()));
            warehouseReceiptMapper.updateById(targetReceipt);
            logger.info("并入已有仓单: receiptId={}, 增加数量={}", targetReceiptId, delegate.getTransportQuantity());
        }

        delegate.setStatus(LogisticsDelegate.STATUS_DELIVERED);
        delegateMapper.updateById(delegate);
        logger.info("到货入库申请成功: voucherNo={}, actionType={}", voucherNo, actionType);

        // 尝试区块链上链
        try {
            if (actionType == LogisticsDelegate.ACTION_CREATE_NEW_RECEIPT) {
                // 到货创建新仓单
                String newReceiptId = "WR" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
                BigInteger weight = delegate.getTransportQuantity() != null
                    ? delegate.getTransportQuantity().toBigInteger()
                    : BigInteger.ZERO;
                byte[] ownerHash = delegate.getOwnerEntId() != null
                    ? String.valueOf(delegate.getOwnerEntId()).getBytes()
                    : new byte[0];
                byte[] warehouseHash = delegate.getTargetWhId() != null
                    ? String.valueOf(delegate.getTargetWhId()).getBytes()
                    : new byte[0];

                var receipt = logisticsContractService.arriveAndCreateReceipt(
                    voucherNo, newReceiptId, weight,
                    delegate.getUnit() != null ? delegate.getUnit() : "吨",
                    ownerHash, warehouseHash
                );
                if (receipt != null && receipt.getTransactionHash() != null) {
                    delegate.setChainTxHash(receipt.getTransactionHash());
                    delegateMapper.updateById(delegate);
                }
            } else if (actionType == LogisticsDelegate.ACTION_MERGE_EXISTING_RECEIPT) {
                // 到货增量入库
                String targetReceiptIdStr = String.valueOf(targetReceiptId);
                BigInteger quantity = delegate.getTransportQuantity() != null
                    ? delegate.getTransportQuantity().toBigInteger()
                    : BigInteger.ZERO;
                var receipt = logisticsContractService.arriveAndAddQuantity(
                    voucherNo, targetReceiptIdStr, quantity
                );
                if (receipt != null && receipt.getTransactionHash() != null) {
                    delegate.setChainTxHash(receipt.getTransactionHash());
                    delegateMapper.updateById(delegate);
                }
            }
            logger.info("到货入库上链成功: voucherNo={}", voucherNo);
        } catch (Exception e) {
            logger.warn("到货入库上链失败: voucherNo={}, error={}", voucherNo, e.getMessage());
        }

        return delegate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LogisticsDelegate updateStatus(String voucherNo, Integer status) {
        LogisticsDelegate delegate = delegateMapper.selectByVoucherNo(voucherNo);
        if (delegate == null) {
            throw new IllegalArgumentException("委派单不存在: " + voucherNo);
        }

        delegate.setStatus(status);
        delegateMapper.updateById(delegate);
        logger.info("更新物流状态: voucherNo={}, status={}", voucherNo, status);

        return delegate;
    }

    // ==================== 轨迹操作 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LogisticsTrack reportTrack(LogisticsTrack track) {
        track.setEventTime(LocalDateTime.now());

        // 计算位置哈希用于链上存证
        if (track.getLatitude() != null && track.getLongitude() != null) {
            String locationHash = calculateLocationHash(
                track.getLatitude().toString(),
                track.getLongitude().toString(),
                track.getEventTime() != null ? track.getEventTime().toString() : ""
            );
            // 使用 locationDesc 字段存储位置哈希信息
            String existingDesc = track.getLocationDesc() != null ? track.getLocationDesc() + "; " : "";
            track.setLocationDesc(existingDesc + "locationHash:" + locationHash);
            logger.info("位置哈希计算: voucherNo={}, locationHash={}", track.getVoucherNo(), locationHash);
        }

        trackMapper.insert(track);

        // 区块链上链存证（失败则抛出异常回滚本地事务）
        try {
            LogisticsDelegate delegate = delegateMapper.selectByVoucherNo(track.getVoucherNo());
            if (delegate != null && delegate.getStatus() == LogisticsDelegate.STATUS_IN_TRANSIT) {
                // 在途状态时更新链上状态（保持运输中状态）
                var receipt = logisticsContractService.updateStatus(
                    track.getVoucherNo(), LogisticsDelegate.STATUS_IN_TRANSIT
                );
                if (receipt != null && receipt.getTransactionHash() != null) {
                    logger.info("在途状态上链存证成功: voucherNo={}, txHash={}, lat={}, lon={}",
                        track.getVoucherNo(), receipt.getTransactionHash(),
                        track.getLatitude(), track.getLongitude());
                } else {
                    throw new RuntimeException("区块链交易回执无效");
                }
            }
        } catch (RuntimeException e) {
            logger.error("在途状态上链存证失败: voucherNo={}, error={}", track.getVoucherNo(), e.getMessage());
            throw e;
        }

        // 如果检测到偏航，触发信用扣分（这里仅记录，后续可调用信用模块）
        if (track.getIsDeviation() != null && track.getIsDeviation() == LogisticsTrack.DEVIATION_YES) {
            logger.warn("检测到物流偏航: voucherNo={}, deviationDistance={}",
                track.getVoucherNo(), track.getDeviationDistance());
        }

        return track;
    }

    /**
     * 计算位置哈希
     * 用于链上存证，确保位置不可篡改
     *
     * @param lat 纬度
     * @param lon 经度
     * @param timestamp 时间戳
     * @return SHA-256哈希值（十六进制字符串）
     */
    private String calculateLocationHash(String lat, String lon, String timestamp) {
        try {
            String data = lat + "|" + lon + "|" + timestamp;
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            logger.error("计算位置哈希失败", e);
            return "";
        }
    }

    @Override
    public List<LogisticsTrack> listTracks(String voucherNo) {
        return trackMapper.selectByVoucherNo(voucherNo);
    }

    @Override
    public LogisticsTrack getLatestTrack(String voucherNo) {
        return trackMapper.selectLatestByVoucherNo(voucherNo);
    }

    @Override
    public List<LogisticsTrack> listDeviations(String voucherNo) {
        return trackMapper.selectDeviationByVoucherNo(voucherNo);
    }

    // ==================== 物流追踪 ====================

    @Override
    public Map<String, Object> trackLogistics(String voucherNo) {
        LogisticsDelegate delegate = delegateMapper.selectByVoucherNo(voucherNo);
        if (delegate == null) {
            throw new IllegalArgumentException("委派单不存在: " + voucherNo);
        }

        // 查询本地轨迹
        List<LogisticsTrack> tracks = trackMapper.selectByVoucherNo(voucherNo);
        LogisticsTrack latestTrack = trackMapper.selectLatestByVoucherNo(voucherNo);

        // 返回追踪信息
        Map<String, Object> result = new HashMap<>();
        result.put("voucherNo", voucherNo);
        result.put("status", delegate.getStatus());
        result.put("statusDesc", delegate.getStatusDesc());
        result.put("businessScene", delegate.getBusinessScene());
        result.put("businessSceneDesc", delegate.getBusinessSceneDesc());
        result.put("ownerEntId", delegate.getOwnerEntId());
        result.put("carrierEntId", delegate.getCarrierEntId());
        result.put("sourceWhId", delegate.getSourceWhId());
        result.put("targetWhId", delegate.getTargetWhId());
        result.put("driverName", delegate.getDriverName());
        result.put("vehicleNo", delegate.getVehicleNo());
        result.put("transportQuantity", delegate.getTransportQuantity());
        result.put("unit", delegate.getUnit());
        result.put("latestTrack", latestTrack);
        result.put("trackCount", tracks.size());
        result.put("tracks", tracks);

        // 获取链上轨迹
        try {
            List<BigInteger> chainTrack = logisticsContractService.getLogisticsTrack(voucherNo);
            result.put("chainTrack", chainTrack);
            result.put("chainTrackCount", chainTrack != null ? chainTrack.size() : 0);
        } catch (Exception e) {
            logger.warn("获取链上轨迹失败: voucherNo={}, error={}", voucherNo, e.getMessage());
            result.put("chainTrack", null);
            result.put("chainTrackCount", 0);
        }

        // 获取区块链交易哈希
        result.put("chainTxHash", delegate.getChainTxHash());

        // 计算在途时长
        if (delegate.getStatus() == LogisticsDelegate.STATUS_IN_TRANSIT && latestTrack != null) {
            long minutes = java.time.Duration.between(latestTrack.getEventTime(), LocalDateTime.now()).toMinutes();
            result.put("transitDurationMinutes", minutes);
        }

        return result;
    }

    // ==================== 私有方法 ====================

    /**
     * 生成委派单编号
     */
    private String generateVoucherNo() {
        String date = LocalDateTime.now().format(VOUCHER_FORMAT);
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "DPDO" + date + uuid;
    }

    /**
     * 计算二维码哈希
     * 用于链上存证，确保二维码不可篡改
     *
     * @param qrCode 二维码内容
     * @return SHA-256哈希值（十六进制字符串）
     */
    private String calculateQrCodeHash(String qrCode) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(qrCode.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            logger.error("计算二维码哈希失败", e);
            return "";
        }
    }

    /**
     * 计算字节数组类型的哈希（用于智能合约 bytes32 参数）
     */
    private byte[] calculateQrCodeHashBytes(String data) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            return digest.digest(data.getBytes());
        } catch (Exception e) {
            logger.error("计算字节数组哈希失败", e);
            return new byte[32];
        }
    }

    /**
     * 生成授权码
     */
    private String generateAuthCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * 生成动态加密提货二维码
     * 二维码内容为JSON格式，包含：voucherNo, driverId, authCode, timestamp
     * 使用Base64编码确保安全传输
     */
    private String generatePickupQrCode(String voucherNo, String driverId, String authCode) {
        try {
            // 构建二维码内容
            Map<String, Object> qrData = new HashMap<>();
            qrData.put("voucherNo", voucherNo);
            qrData.put("driverId", driverId);
            qrData.put("authCode", authCode);
            qrData.put("timestamp", System.currentTimeMillis());
            qrData.put("expires", System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000); // 7天有效期

            // 转换为JSON并Base64编码
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(qrData);
            String base64 = java.util.Base64.getEncoder().encodeToString(json.getBytes("UTF-8"));

            logger.info("生成提货二维码: voucherNo={}, driverId={}", voucherNo, driverId);
            return base64;
        } catch (Exception e) {
            logger.error("生成二维码失败: voucherNo={}", voucherNo, e);
            return null;
        }
    }

    /**
     * 校验物流委派单状态值是否有效
     *
     * 状态值定义：
     * - 1: 待指派 (STATUS_PENDING) - 委派单刚创建，还未分配司机
     * - 2: 已调度 (STATUS_ASSIGNED) - 已分配司机，等待提货
     * - 3: 运输中 (STATUS_IN_TRANSIT) - 货物正在运输中
     * - 4: 已交付 (STATUS_DELIVERED) - 货物已送达目的地
     * - 5: 已失效 (STATUS_INVALID) - 委派单已失效/取消
     *
     * @param status 状态值
     * @throws IllegalArgumentException 如果状态值无效
     */
    public void validateStatus(Integer status) {
        if (status == null) {
            throw new IllegalArgumentException("物流单状态不能为空");
        }
        if (status < LogisticsDelegate.STATUS_PENDING || status > LogisticsDelegate.STATUS_INVALID) {
            throw new IllegalArgumentException(
                String.format("无效的物流单状态值: %d, 有效范围: %d-%d",
                    status, LogisticsDelegate.STATUS_PENDING, LogisticsDelegate.STATUS_INVALID));
        }
    }

    /**
     * 获取状态描述
     */
    public String getStatusDescription(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case LogisticsDelegate.STATUS_PENDING: return "待指派";
            case LogisticsDelegate.STATUS_ASSIGNED: return "已调度";
            case LogisticsDelegate.STATUS_IN_TRANSIT: return "运输中";
            case LogisticsDelegate.STATUS_DELIVERED: return "已交付";
            case LogisticsDelegate.STATUS_INVALID: return "已失效";
            default: return "未知";
        }
    }

    /**
     * 校验业务场景必填字段
     */
    private void validateBusinessScene(LogisticsDelegate delegate) {
        switch (delegate.getBusinessScene()) {
            case LogisticsDelegate.SCENE_DIRECT_TRANSFER:
                // 直接移库：必填 receipt_id, source_wh_id, target_wh_id
                if (delegate.getReceiptId() == null) {
                    throw new IllegalArgumentException("直接移库场景必须关联仓单");
                }
                if (delegate.getSourceWhId() == null) {
                    throw new IllegalArgumentException("必须指定起运地仓库");
                }
                if (delegate.getTargetWhId() == null) {
                    throw new IllegalArgumentException("必须指定目的地仓库");
                }
                break;
            case LogisticsDelegate.SCENE_TRANSFER_THEN_TRANSFER:
                // 转让后移库：必填 endorse_id, target_wh_id
                if (delegate.getEndorseId() == null) {
                    throw new IllegalArgumentException("转让后移库场景必须关联背书");
                }
                // 校验背书有效性
                ReceiptEndorsement endorsement = receiptEndorsementMapper.selectById(delegate.getEndorseId());
                if (endorsement == null) {
                    throw new IllegalArgumentException("背书不存在: " + delegate.getEndorseId());
                }
                // 校验背书状态（必须已完成转让）
                if (endorsement.getStatus() == null || endorsement.getStatus() != ReceiptEndorsement.STATUS_CONFIRMED) {
                    throw new IllegalArgumentException("背书转让未完成，无法创建物流委派单");
                }
                // 校验当前用户是否为背书受让人
                if (delegate.getOwnerEntId() != null && !delegate.getOwnerEntId().equals(endorsement.getTransfereeEntId())) {
                    throw new IllegalArgumentException("只有背书受让人才能发起转让后移库");
                }
                if (delegate.getTargetWhId() == null) {
                    throw new IllegalArgumentException("必须指定目的地仓库");
                }
                break;
            case LogisticsDelegate.SCENE_DELIVERY_TO_WAREHOUSE:
                // 发货入库：必填 target_wh_id
                if (delegate.getTargetWhId() == null) {
                    throw new IllegalArgumentException("发货入库场景必须指定入库仓库");
                }
                break;
            default:
                throw new IllegalArgumentException("无效的业务场景: " + delegate.getBusinessScene());
        }
    }

    // ==================== 区块链上链集成 ====================

    /**
     * 确认交付
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LogisticsDelegate confirmDelivery(String voucherNo, Integer action, String targetReceiptId) {
        LogisticsDelegate delegate = delegateMapper.selectByVoucherNo(voucherNo);
        if (delegate == null) {
            throw new IllegalArgumentException("委派单不存在: " + voucherNo);
        }

        if (delegate.getStatus() != LogisticsDelegate.STATUS_IN_TRANSIT) {
            throw new IllegalArgumentException("当前状态不允许确认交付: " + delegate.getStatusDesc());
        }

        delegate.setStatus(LogisticsDelegate.STATUS_DELIVERED);
        delegateMapper.updateById(delegate);

        logger.info("物流确认交付: voucherNo={}", voucherNo);

        // 区块链上链（失败则抛出异常回滚本地事务）
        try {
            var receipt = logisticsContractService.confirmDelivery(voucherNo, action != null ? action : 1, targetReceiptId);
            if (receipt != null && receipt.getTransactionHash() != null) {
                delegate.setChainTxHash(receipt.getTransactionHash());
                delegateMapper.updateById(delegate);
            } else {
                throw new RuntimeException("区块链交易回执无效");
            }
        } catch (RuntimeException e) {
            logger.error("确认交付上链失败: voucherNo={}, error={}", voucherNo, e.getMessage());
            throw e;
        }

        return delegate;
    }

    /**
     * 使委派单失效
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LogisticsDelegate invalidate(String voucherNo) {
        LogisticsDelegate delegate = delegateMapper.selectByVoucherNo(voucherNo);
        if (delegate == null) {
            throw new IllegalArgumentException("委派单不存在: " + voucherNo);
        }

        delegate.setStatus(LogisticsDelegate.STATUS_INVALID);
        delegateMapper.updateById(delegate);

        logger.info("委派单已失效: voucherNo={}", voucherNo);

        // 区块链上链（失败则抛出异常回滚本地事务）
        try {
            var receipt = logisticsContractService.invalidate(voucherNo);
            if (receipt != null && receipt.getTransactionHash() != null) {
                delegate.setChainTxHash(receipt.getTransactionHash());
                delegateMapper.updateById(delegate);
            } else {
                throw new RuntimeException("区块链交易回执无效");
            }
        } catch (RuntimeException e) {
            logger.error("失效上链失败: voucherNo={}, error={}", voucherNo, e.getMessage());
            throw e;
        }

        return delegate;
    }

    /**
     * 验证物流委派单
     */
    @Override
    public boolean validateDelegate(String voucherNo) {
        try {
            return logisticsContractService.validateLogisticsDelegate(voucherNo);
        } catch (Exception e) {
            logger.warn("链上验证失败，查询本地: voucherNo={}", voucherNo);
            LogisticsDelegate delegate = delegateMapper.selectByVoucherNo(voucherNo);
            return delegate != null && delegate.isValid();
        }
    }
}
