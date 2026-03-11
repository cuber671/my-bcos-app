package com.fisco.app.Modules.Blockchain.Service;

import java.math.BigInteger;
import java.util.List;

import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.fisco.bcos.sdk.v3.transaction.model.dto.CallResponse;

/**
 * 区块链基础服务接口
 *
 * 提供区块链网络的基础操作能力，包括：
 * - 区块信息查询
 * - 交易操作
 * - 账户管理
 * - 合约调用
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
public interface BlockchainService {

    // ==================== 基础查询 ====================

    /**
     * 获取当前块高
     *
     * @return 当前区块高度
     */
    BigInteger getBlockNumber();

    /**
     * 根据块号获取区块信息
     *
     * @param blockNumber 块号
     * @return 区块信息（JSON格式字符串）
     */
    String getBlockByNumber(BigInteger blockNumber);

    /**
     * 根据块号获取区块哈希
     *
     * @param blockNumber 块号
     * @return 区块哈希
     */
    String getBlockHashByNumber(BigInteger blockNumber);

    /**
     * 获取链ID
     *
     * @return 链ID
     */
    String getChainId();

    // ==================== 交易操作 ====================

    /**
     * 根据交易哈希获取交易收据
     *
     * @param txHash 交易哈希
     * @return 交易收据
     */
    TransactionReceipt getTransactionReceipt(String txHash);

    /**
     * 发送原始交易
     *
     * @param to   目标地址
     * @param data 交易数据（编码后的字节数组字符串）
     * @return 交易收据
     */
    TransactionReceipt sendRawTransaction(String to, String data);

    // ==================== 账户操作 ====================

    /**
     * 获取当前系统账户地址
     *
     * @return 当前账户地址（0x开头）
     */
    String getCurrentAccountAddress();

    /**
     * 查询账户余额
     *
     * @param address 账户地址
     * @return 余额（单位：wei）
     */
    String getBalance(String address);

    // ==================== 合约操作 ====================

    /**
     * 调用合约只读方法
     *
     * @param contractAddress 合约地址
     * @param abi             合约ABI
     * @param method          方法名
     * @param params          参数列表
     * @return 合约调用响应
     */
    CallResponse callContract(String contractAddress, String abi, String method, List<Object> params);

    /**
     * 发送交易调用合约写方法
     *
     * @param contractAddress 合约地址
     * @param abi             合约ABI
     * @param method          方法名
     * @param params          参数列表
     * @return 交易响应
     */
    Object sendContractTransaction(String contractAddress, String abi, String method, List<Object> params);

    // ==================== 群组操作 ====================

    /**
     * 获取节点上的群组列表
     *
     * @return 群组ID列表
     */
    List<String> getGroupList();

    /**
     * 获取当前群组信息
     *
     * @return 群组信息（JSON格式字符串）
     */
    String getGroupInfo();

    // ==================== 状态检查 ====================

    /**
     * 检查区块链连接状态
     *
     * @return true 表示已连接
     */
    boolean isConnected();
}
