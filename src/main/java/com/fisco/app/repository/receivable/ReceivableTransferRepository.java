package com.fisco.app.repository.receivable;

import com.fisco.app.entity.receivable.ReceivableTransfer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 应收账款转让记录Repository接口
 *
 * 提供转让记录的查询方法
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-09
 * @version 1.0
 */
public interface ReceivableTransferRepository extends JpaRepository<ReceivableTransfer, Long> {

    /**
     * 查询应收账款的所有转让记录（按时间倒序）
     *
     * @param receivableId 应收账款ID
     * @return 转让记录列表
     */
    List<ReceivableTransfer> findByReceivableIdOrderByTimestampDesc(String receivableId);

    /**
     * 查询转出方的转让记录（按时间倒序）
     *
     * @param fromAddress 转出方地址
     * @return 转让记录列表
     */
    List<ReceivableTransfer> findByFromAddressOrderByTimestampDesc(String fromAddress);

    /**
     * 查询转入方的转让记录（按时间倒序）
     *
     * @param toAddress 转入方地址
     * @return 转让记录列表
     */
    List<ReceivableTransfer> findByToAddressOrderByTimestampDesc(String toAddress);

    /**
     * 查询指定类型的转让记录
     *
     * @param transferType 转让类型
     * @return 转让记录列表
     */
    List<ReceivableTransfer> findByTransferTypeOrderByTimestampDesc(String transferType);
}
