package com.fisco.app.aspect;
import java.util.Arrays;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.fisco.app.entity.warehouse.ElectronicWarehouseReceipt;
import com.fisco.app.exception.BusinessException;
import com.fisco.app.repository.warehouse.ElectronicWarehouseReceiptRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 区块链状态检查切面
 * 拦截标记了 @RequireOnChain 注解的方法，确保仓单已上链
 */
@Slf4j
@Aspect
@Component
@Order(1) // 优先级高于事务切面
@RequiredArgsConstructor
public class OnChainStatusAspect {

    private final ElectronicWarehouseReceiptRepository repository;

    @Around("@annotation(com.fisco.app.aspect.RequireOnChain)")
    public Object checkOnChainStatus(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        RequireOnChain annotation = signature.getMethod().getAnnotation(RequireOnChain.class);

        // 获取方法参数
        Object[] args = joinPoint.getArgs();
        if (args.length == 0) {
            log.warn("@RequireOnChain 注解的方法没有参数，无法获取仓单ID");
            throw new BusinessException("方法参数错误");
        }

        // 假设第一个参数是仓单ID
        String receiptId = extractReceiptId(args[0]);
        if (receiptId == null) {
            log.warn("无法从方法参数中提取仓单ID: {}",
                    Arrays.toString(args));
            throw new BusinessException("无法获取仓单ID");
        }

        // 查询仓单
        ElectronicWarehouseReceipt receipt = repository.findById(receiptId)
                .orElseThrow(() -> new BusinessException("仓单不存在: " + receiptId));

        // 检查区块链状态
        ElectronicWarehouseReceipt.BlockchainStatus blockchainStatus = receipt.getBlockchainStatus();
        if (blockchainStatus == null) {
            blockchainStatus = ElectronicWarehouseReceipt.BlockchainStatus.PENDING;
        }

        String operation = annotation.value().isEmpty()
                ? signature.getName()
                : annotation.value();

        // 如果不允许 FAILED 状态，且当前状态不是 SYNCED，则抛出异常
        if (!annotation.allowFailed() && blockchainStatus != ElectronicWarehouseReceipt.BlockchainStatus.SYNCED) {
            String message = buildErrorMessage(receipt, blockchainStatus, operation);
            log.warn("区块链状态检查失败: receiptId={}, status={}, operation={}",
                    receiptId, blockchainStatus, operation);
            throw new BusinessException(message);
        }

        // 允许执行
        log.debug("区块链状态检查通过: receiptId={}, status={}, operation={}",
                receiptId, blockchainStatus, operation);

        return joinPoint.proceed();
    }

    /**
     * 从参数中提取仓单ID
     * 支持直接传入 String ID 或从 DTO 中提取
     */
    private String extractReceiptId(Object arg) {
        if (arg instanceof String) {
            return (String) arg;
        }

        // 使用反射尝试从 DTO 中提取 id 或 receiptId 字段
        try {
            java.lang.reflect.Field idField = arg.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            Object idValue = idField.get(arg);
            if (idValue instanceof String) {
                return (String) idValue;
            }

            java.lang.reflect.Field receiptIdField = arg.getClass().getDeclaredField("receiptId");
            receiptIdField.setAccessible(true);
            Object receiptIdValue = receiptIdField.get(arg);
            if (receiptIdValue instanceof String) {
                return (String) receiptIdValue;
            }
        } catch (Exception e) {
            log.debug("无法从参数中提取仓单ID: {}", arg.getClass().getSimpleName());
        }

        return null;
    }

    /**
     * 构建错误提示信息
     */
    private String buildErrorMessage(ElectronicWarehouseReceipt receipt,
                                     ElectronicWarehouseReceipt.BlockchainStatus status,
                                     String operation) {
        StringBuilder message = new StringBuilder();
        message.append("仓单无法进行").append(operation).append("操作。");

        switch (status) {
            case PENDING:
                message.append("仓单正在上链中，请稍后再试。");
                break;
            case FAILED:
                message.append("仓单上链失败，请先重试上链操作。");
                message.append("（仓单编号：").append(receipt.getReceiptNo()).append("）");
                break;
            default:
                message.append("仓单区块链状态异常：").append(status);
                break;
        }

        return message.toString();
    }
}
