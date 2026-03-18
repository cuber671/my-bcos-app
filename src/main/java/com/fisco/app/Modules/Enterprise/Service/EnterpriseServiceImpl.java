package com.fisco.app.Modules.Enterprise.Service;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fisco.app.Common.Config.BlockchainConfig;
import com.fisco.app.Common.Service.EncryptionService;
import com.fisco.app.Modules.Enterprise.Entity.Enterprise;
import com.fisco.app.Modules.Enterprise.Entity.InvitationCode;
import com.fisco.app.Modules.Enterprise.Mapper.EnterpriseMapper;
import com.fisco.app.Modules.Enterprise.Mapper.InvitationCodeMapper;

import io.swagger.annotations.ApiOperation;

/**
 * 企业业务服务实现类
 *
 * 实现企业注册、登录、状态管理等业务功能
 * 集成区块链上链服务完成企业身份存证
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@ApiOperation("企业业务服务")
@Service
public class EnterpriseServiceImpl implements EnterpriseService {

    private static final Logger logger = LoggerFactory.getLogger(EnterpriseServiceImpl.class);

    @Autowired
    private EnterpriseMapper enterpriseMapper;

    @Autowired
    private InvitationCodeMapper invitationCodeMapper;

    @Autowired
    private EnterpriseContractService enterpriseContractService;

    @Autowired
    private EncryptionService encryptionService;

    @Autowired(required = false)
    private BlockchainConfig blockchainConfig;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ==================== 企业注册与登录 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long registerEnterprise(String username, String password, String payPassword,
            String enterpriseName, String orgCode, Integer entRole,
            String localAddress, String contactPhone) {

        // 参数校验
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("登录密码不能为空");
        }
        validatePassword(password);
        if (payPassword == null || payPassword.isEmpty()) {
            throw new IllegalArgumentException("交易密码不能为空");
        }
        if (enterpriseName == null || enterpriseName.isEmpty()) {
            throw new IllegalArgumentException("企业名称不能为空");
        }
        if (orgCode == null || orgCode.isEmpty()) {
            throw new IllegalArgumentException("统一社会信用代码不能为空");
        }

        // 检查用户名是否已存在
        Enterprise existUser = getEnterpriseByUsername(username);
        if (existUser != null) {
            throw new IllegalArgumentException("用户名已存在");
        }

        // 检查信用代码是否已存在
        Enterprise existOrg = getEnterpriseByOrgCode(orgCode);
        if (existOrg != null) {
            throw new IllegalArgumentException("统一社会信用代码已被注册");
        }

        // 生成企业ID（雪花算法由MyBatis-Plus自动处理）
        Enterprise enterprise = new Enterprise();
        enterprise.setEnterpriseName(enterpriseName);
        enterprise.setOrgCode(orgCode);
        enterprise.setLocalAddress(localAddress);
        enterprise.setContactPhone(contactPhone);
        enterprise.setUsername(username);
        // 密码加密存储
        enterprise.setPassword(passwordEncoder.encode(password));
        enterprise.setPayPassword(passwordEncoder.encode(payPassword));
        enterprise.setEntRole(entRole != null ? entRole : Enterprise.ROLE_SUPPLIER);
        enterprise.setStatus(Enterprise.STATUS_PENDING); // 待审核状态

        try {
            // 使用SDK生成密钥对（包含地址和私钥）
            if (blockchainConfig == null) {
                logger.warn("区块链配置不可用，使用空地址");
                enterprise.setBlockchainAddress(null);
                enterprise.setEncryptedPrivateKey(null);
            } else {
                BlockchainConfig.KeyPairInfo keyPairInfo = blockchainConfig.generateKeyPairWithPrivateKey();
                if (keyPairInfo == null) {
                    logger.warn("密钥对生成失败，使用空地址");
                    enterprise.setBlockchainAddress(null);
                    enterprise.setEncryptedPrivateKey(null);
                } else {
                    String blockchainAddress = keyPairInfo.getAddress();
                    String privateKey = keyPairInfo.getPrivateKey();

                    // 加密存储私钥
                    String encryptedPrivateKey = privateKey != null
                        ? encryptionService.encryptWithAes(privateKey)
                        : null;

                    enterprise.setBlockchainAddress(blockchainAddress);
                    enterprise.setEncryptedPrivateKey(encryptedPrivateKey);

                    logger.info("为企业生成区块链地址: {}", blockchainAddress);
                }
            }
        } catch (Exception e) {
            logger.warn("生成区块链地址失败，使用空地址: {}", e.getMessage());
            // 区块链地址可后续补充
            enterprise.setBlockchainAddress(null);
            enterprise.setEncryptedPrivateKey(null);
        }

        // 保存到数据库
        enterpriseMapper.insert(enterprise);


        return enterprise.getEntId();
    }

    @Override
    public Enterprise login(String username, String password) {
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }

        Enterprise enterprise = getEnterpriseByUsername(username);
        if (enterprise == null) {
            return null;
        }

        // 检查企业状态
        if (enterprise.getStatus() == Enterprise.STATUS_PENDING) {
            throw new IllegalStateException("账户待审核，请等待管理员审核通过后登录");
        }
        if (enterprise.getStatus() == Enterprise.STATUS_FROZEN) {
            throw new IllegalStateException("账户已被冻结");
        }
        if (enterprise.getStatus() == Enterprise.STATUS_CANCELLED) {
            throw new IllegalStateException("账户已注销");
        }
        if (enterprise.getStatus() == Enterprise.STATUS_CANCELLING) {
            throw new IllegalStateException("账户正在注销中，暂时无法登录");
        }

        // 验证密码
        if (!passwordEncoder.matches(password, enterprise.getPassword())) {
            return null;
        }

        return enterprise;
    }

    // ==================== 企业查询 ====================

    @Override
    public Enterprise getEnterpriseById(Long entId) {
        if (entId == null) {
            return null;
        }
        return enterpriseMapper.selectById(entId);
    }

    @Override
    public Enterprise getEnterpriseByUsername(String username) {
        if (username == null || username.isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<Enterprise> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Enterprise::getUsername, username);
        return enterpriseMapper.selectOne(wrapper);
    }

    @Override
    public Enterprise getEnterpriseByOrgCode(String orgCode) {
        if (orgCode == null || orgCode.isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<Enterprise> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Enterprise::getOrgCode, orgCode);
        return enterpriseMapper.selectOne(wrapper);
    }

    @Override
    public Enterprise getEnterpriseByBlockchainAddress(String blockchainAddress) {
        if (blockchainAddress == null || blockchainAddress.isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<Enterprise> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Enterprise::getBlockchainAddress, blockchainAddress);
        return enterpriseMapper.selectOne(wrapper);
    }

    @Override
    public List<Enterprise> listEnterprises(Integer status, Integer entRole) {
        LambdaQueryWrapper<Enterprise> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Enterprise::getStatus, status);
        }
        if (entRole != null) {
            wrapper.eq(Enterprise::getEntRole, entRole);
        }
        wrapper.orderByDesc(Enterprise::getCreateTime);
        return enterpriseMapper.selectList(wrapper);
    }

    // ==================== 企业状态管理 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateEnterpriseStatus(Long entId, Integer newStatus) {
        Enterprise enterprise = getEnterpriseById(entId);
        if (enterprise == null) {
            throw new IllegalArgumentException("企业不存在");
        }

        Integer oldStatus = enterprise.getStatus();
        enterprise.setStatus(newStatus);
        enterpriseMapper.updateById(enterprise);

        // 仅当企业已在区块链上注册时才更新链上状态
        try {
            if (enterprise.getBlockchainAddress() != null) {
                // 检查企业是否已在区块链上注册
                boolean isRegisteredOnChain = false;
                try {
                    var info = enterpriseContractService.getEnterprise(enterprise.getBlockchainAddress());
                    isRegisteredOnChain = info != null && !"0x0000000000000000000000000000000000000000".equals(info.getAddress());
                } catch (Exception e) {
                    logger.debug("查询企业链上信息失败，可能未注册: entId={}", entId);
                }

                if (isRegisteredOnChain) {
                    // 正常状态对应区块链1，冻结对应0
                    BigInteger chainStatus = newStatus == Enterprise.STATUS_NORMAL
                            ? BigInteger.ONE
                            : BigInteger.ZERO;
                    enterpriseContractService.updateEnterpriseStatus(
                            enterprise.getBlockchainAddress(),
                            chainStatus
                    );
                    logger.info("企业状态上链成功: entId={}, oldStatus={}, newStatus={}",
                            entId, oldStatus, newStatus);
                } else {
                    logger.info("企业未在区块链上注册，跳过链上状态更新: entId={}", entId);
                }
            }
        } catch (Exception e) {
            logger.error("企业状态上链失败: entId={}", entId, e);
            throw new RuntimeException("操作失败，请稍后重试");
        }

        return true;
    }

    @Override
    public boolean freezeEnterprise(Long entId) {
        return updateEnterpriseStatus(entId, Enterprise.STATUS_FROZEN);
    }

    @Override
    public boolean unfreezeEnterprise(Long entId) {
        return updateEnterpriseStatus(entId, Enterprise.STATUS_NORMAL);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateLoginPassword(Long entId, String oldPassword, String newPassword) {
        Enterprise enterprise = getEnterpriseById(entId);
        if (enterprise == null) {
            throw new IllegalArgumentException("企业不存在");
        }

        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, enterprise.getPassword())) {
            throw new IllegalArgumentException("原密码错误");
        }

        // 更新密码
        enterprise.setPassword(passwordEncoder.encode(newPassword));
        enterpriseMapper.updateById(enterprise);

        logger.info("企业登录密码已更新: entId={}", entId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updatePayPassword(Long entId, String oldPassword, String newPassword) {
        Enterprise enterprise = getEnterpriseById(entId);
        if (enterprise == null) {
            throw new IllegalArgumentException("企业不存在");
        }

        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, enterprise.getPayPassword())) {
            throw new IllegalArgumentException("原交易密码错误");
        }

        // 更新交易密码
        enterprise.setPayPassword(passwordEncoder.encode(newPassword));
        enterpriseMapper.updateById(enterprise);

        logger.info("企业交易密码已重置: entId={}", entId);
        return true;
    }

    // ==================== 邀请码管理 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String generateInvitationCode(Long entId, Integer maxUses, Integer expireDays, String remark) {
        Enterprise enterprise = getEnterpriseById(entId);
        if (enterprise == null) {
            throw new IllegalArgumentException("企业不存在");
        }

        // 生成唯一邀请码
        String code;
        do {
            code = generateUniqueCode();
        } while (getInvitationCodeByCode(code) != null);

        InvitationCode invitationCode = new InvitationCode();
        invitationCode.setInviterEntId(entId);
        invitationCode.setCode(code);
        invitationCode.setMaxUses(maxUses != null ? maxUses : 1);
        invitationCode.setUsedCount(0);
        if (expireDays != null && expireDays > 0) {
            invitationCode.setExpireTime(LocalDateTime.now().plusDays(expireDays));
        }
        invitationCode.setStatus(InvitationCode.STATUS_ENABLED);
        invitationCode.setRemark(remark);

        invitationCodeMapper.insert(invitationCode);

        logger.info("生成邀请码: entId={}, code={}", entId, code);
        return code;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long useInvitationCode(String code) {
        InvitationCode invitationCode = getInvitationCodeByCode(code);
        if (invitationCode == null) {
            throw new IllegalArgumentException("邀请码不存在");
        }

        // 验证邀请码有效性
        if (!invitationCode.isValid()) {
            if (invitationCode.isExpired()) {
                throw new IllegalArgumentException("邀请码已过期");
            }
            if (invitationCode.isExhausted()) {
                throw new IllegalArgumentException("邀请码已使用完毕");
            }
            if (invitationCode.getStatus() != InvitationCode.STATUS_ENABLED) {
                throw new IllegalArgumentException("邀请码已禁用");
            }
        }

        // 更新使用次数
        invitationCode.setUsedCount(invitationCode.getUsedCount() + 1);
        if (invitationCode.getUsedCount() >= invitationCode.getMaxUses()) {
            invitationCode.setStatus(InvitationCode.STATUS_EXHAUSTED);
        }
        invitationCodeMapper.updateById(invitationCode);

        logger.info("使用邀请码: code={}, inviterEntId={}", code, invitationCode.getInviterEntId());
        return invitationCode.getInviterEntId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteInvitationCode(Long codeId) {
        InvitationCode invitationCode = invitationCodeMapper.selectById(codeId);
        if (invitationCode == null) {
            throw new IllegalArgumentException("邀请码不存在");
        }

        invitationCode.setStatus(InvitationCode.STATUS_DISABLED);
        invitationCodeMapper.updateById(invitationCode);

        logger.info("删除邀请码: codeId={}", codeId);
        return true;
    }

    @Override
    public List<InvitationCode> listInvitationCodes(Long entId) {
        LambdaQueryWrapper<InvitationCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InvitationCode::getInviterEntId, entId);
        wrapper.orderByDesc(InvitationCode::getCreateTime);
        return invitationCodeMapper.selectList(wrapper);
    }

    @Override
    public boolean validateInvitationCode(String code) {
        InvitationCode invitationCode = getInvitationCodeByCode(code);
        return invitationCode != null && invitationCode.isValid();
    }

    // ==================== 企业注销管理 ====================

    @Override
    public CancellationResult applyCancellation(Long entId, String reason) {
        CancellationResult result = new CancellationResult();

        // 查询企业信息
        Enterprise enterprise = getEnterpriseById(entId);
        if (enterprise == null) {
            result.setSuccess(false);
            result.setMessage("企业不存在");
            return result;
        }

        // 检查企业状态是否为正常
        if (enterprise.getStatus() != Enterprise.STATUS_NORMAL) {
            result.setSuccess(false);
            result.setMessage("企业状态异常，无法申请注销。当前状态：" + getStatusName(enterprise.getStatus()));
            return result;
        }

        // 校验链上资产余额
        AssetBalance assetBalance = checkAssetBalance(entId);
        if (assetBalance.hasAssets()) {
            result.setSuccess(false);
            result.setMessage("企业存在未结清资产，无法申请注销。仓单：" + assetBalance.getWarehouseReceiptCount()
                    + "，票据：" + assetBalance.getBillCount()
                    + "，应收款：" + assetBalance.getReceivableCount());
            return result;
        }

        // 更新企业状态为注销待审核（等待管理员审核）
        enterprise.setStatus(Enterprise.STATUS_PENDING_CANCEL);
        enterpriseMapper.updateById(enterprise);

        // TODO: 同步更新区块链状态（可选，取决于业务需求）
        // 暂时不上链，等审核通过后再上链

        result.setSuccess(true);
        result.setMessage("注销申请已提交，等待管理员审核");
        result.setEntId(entId);
        result.setReason(reason);
        result.setApplyTime(LocalDateTime.now());

        logger.info("企业注销申请成功: entId={}, reason={}", entId, reason);
        return result;
    }

    @Override
    public boolean revokeCancellation(Long entId) {
        Enterprise enterprise = getEnterpriseById(entId);
        if (enterprise == null) {
            throw new IllegalArgumentException("企业不存在");
        }

        // 注销中或注销待审核状态都可以撤回
        if (enterprise.getStatus() != Enterprise.STATUS_CANCELLING
            && enterprise.getStatus() != Enterprise.STATUS_PENDING_CANCEL) {
            throw new IllegalArgumentException("只有注销中的企业才能撤回申请");
        }

        // 更新状态为正常
        enterprise.setStatus(Enterprise.STATUS_NORMAL);
        enterpriseMapper.updateById(enterprise);

        // 同步更新区块链状态
        try {
            if (enterprise.getBlockchainAddress() != null) {
                enterpriseContractService.updateEnterpriseStatus(
                        enterprise.getBlockchainAddress(),
                        BigInteger.ONE  // NORMAL状态
                );
            }
        } catch (Exception e) {
            logger.error("企业状态上链失败: entId={}", entId, e);
            throw new RuntimeException("操作失败，请稍后重试");
        }

        logger.info("企业注销申请已撤回: entId={}", entId);
        return true;
    }

    @Override
    public List<Enterprise> getPendingCancellationEnterprises() {
        LambdaQueryWrapper<Enterprise> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Enterprise::getStatus, Enterprise.STATUS_PENDING_CANCEL);
        return enterpriseMapper.selectList(wrapper);
    }

    @Override
    public boolean auditCancellation(Long entId, boolean approved) {
        Enterprise enterprise = getEnterpriseById(entId);
        if (enterprise == null) {
            throw new IllegalArgumentException("企业不存在");
        }
        if (enterprise.getStatus() != Enterprise.STATUS_PENDING_CANCEL) {
            throw new IllegalArgumentException("该企业不是注销待审核状态，无法审核");
        }

        // 审核通过设为已注销(4)，审核拒绝恢复正常(1)
        int newStatus = approved ? Enterprise.STATUS_CANCELLED : Enterprise.STATUS_NORMAL;
        enterprise.setStatus(newStatus);
        enterpriseMapper.updateById(enterprise);

        // 审核通过时同步更新区块链状态
        if (approved) {
            try {
                if (enterprise.getBlockchainAddress() != null) {
                    enterpriseContractService.removeEnterprise(enterprise.getBlockchainAddress(), "管理员审核通过");
                }
            } catch (Exception e) {
                logger.error("企业注销上链失败: entId={}", entId, e);
                throw new RuntimeException("操作失败，请稍后重试");
            }
        }

        logger.info("企业注销审核完成: entId={}, approved={}, newStatus={}", entId, approved, newStatus);
        return true;
    }

    @Override
    public AssetBalance checkAssetBalance(Long entId) {
        AssetBalance balance = new AssetBalance();
        balance.setEntId(entId);

        Enterprise enterprise = getEnterpriseById(entId);
        if (enterprise == null || enterprise.getBlockchainAddress() == null) {
            balance.setTotalAssets(0);
            return balance;
        }

        balance.setBlockchainAddress(enterprise.getBlockchainAddress());

        // TODO: 调用各模块的区块链服务查询资产数量
        // 目前返回0，实际需要查询：
        // 1. 仓单数量 - WarehouseReceiptContractService
        // 2. 票据数量 - BillContractService
        // 3. 应收款数量 - ReceivableContractService

        // 暂时设置为0，后续完善
        balance.setWarehouseReceiptCount(0);
        balance.setBillCount(0);
        balance.setReceivableCount(0);
        balance.setTotalAssets(0);

        return balance;
    }

    // ==================== 区块链操作 ====================

    @Override
    public EnterpriseContractService.EnterpriseInfo getEnterpriseFromChain(String blockchainAddress) {
        if (blockchainAddress == null || blockchainAddress.isEmpty()) {
            throw new IllegalArgumentException("区块链地址不能为空");
        }
        try {
            return enterpriseContractService.getEnterprise(blockchainAddress);
        } catch (Exception e) {
            logger.error("查询链上企业信息失败: address={}", blockchainAddress, e);
            throw new RuntimeException("查询失败，请稍后重试");
        }
    }

    @Override
    public String getEnterpriseAddressByOrgCode(String orgCode) {
        if (orgCode == null || orgCode.isEmpty()) {
            throw new IllegalArgumentException("统一社会信用代码不能为空");
        }
        try {
            return enterpriseContractService.getEnterpriseByCreditCode(orgCode);
        } catch (Exception e) {
            logger.error("查询链上企业地址失败: orgCode={}", orgCode, e);
            throw new RuntimeException("查询失败，请稍后重试");
        }
    }

    @Override
    public List<String> getEnterpriseListFromChain() {
        try {
            return enterpriseContractService.getEnterpriseList();
        } catch (Exception e) {
            logger.error("查询链上企业列表失败", e);
            throw new RuntimeException("查询失败，请稍后重试");
        }
    }
    @Override
    public String registerEnterpriseOnChain(Long entId) {
        Enterprise enterprise = getEnterpriseById(entId);
        if (enterprise == null) {
            throw new IllegalArgumentException("企业不存在");
        }
        if (enterprise.getBlockchainAddress() == null) {
            throw new IllegalArgumentException("企业无区块链地址");
        }
        try {
            org.fisco.bcos.sdk.v3.model.TransactionReceipt receipt = enterpriseContractService.registerEnterprise(
                    enterprise.getBlockchainAddress(),
                    enterprise.getOrgCode(),
                    mapToContractRole(enterprise.getEntRole()),
                    java.security.MessageDigest.getInstance("SHA-256").digest(enterprise.getOrgCode().getBytes())
            );
            if (receipt.isStatusOK()) {
                logger.info("企业上链成功: entId={}, txHash={}", entId, receipt.getTransactionHash());
                return receipt.getTransactionHash();
            } else {
                logger.error("企业上链失败 - status: {}, message: {}, output: {}", receipt.getStatus(), receipt.getMessage(), receipt.getOutput());
                throw new RuntimeException("操作失败，请稍后重试");
            }
        } catch (Exception e) {
            logger.error("企业上链失败: entId={}", entId, e);
            throw new RuntimeException("操作失败，请稍后重试");
        }
    }

    /**
     * 将Java应用角色映射为智能合约角色值
     * Java角色: 1=核心企业, 3=供应商, 6=金融机构, 9=仓储方
     * 合约角色: 1=核心企业, 0=供应商, 2=金融机构, 4=仓储方
     */
    private BigInteger mapToContractRole(Integer entRole) {
        if (entRole == null) {
            return BigInteger.valueOf(1);
        }
        switch (entRole) {
            case 1: return BigInteger.valueOf(1);  // 核心企业
            case 3: return BigInteger.valueOf(0);  // 供应商
            case 6: return BigInteger.valueOf(2);  // 金融机构
            case 9: return BigInteger.valueOf(4);  // 仓储方
            default: return BigInteger.valueOf(1); // 默认核心企业
        }
    }

    @Override
    public String updateEnterpriseStatusOnChain(Long entId, Integer status) {
        Enterprise enterprise = getEnterpriseById(entId);
        if (enterprise == null) {
            throw new IllegalArgumentException("企业不存在");
        }
        if (enterprise.getBlockchainAddress() == null) {
            throw new IllegalArgumentException("企业未上链，无区块链地址");
        }
        try {
            org.fisco.bcos.sdk.v3.model.TransactionReceipt receipt = enterpriseContractService.updateEnterpriseStatus(
                    enterprise.getBlockchainAddress(),
                    BigInteger.valueOf(status)
            );
            if (receipt.isStatusOK()) {
                logger.info("更新链上企业状态成功: entId={}, status={}, txHash={}",
                        entId, status, receipt.getTransactionHash());
                return receipt.getTransactionHash();
            } else {
                throw new RuntimeException("操作失败，请稍后重试");
            }
        } catch (Exception e) {
            logger.error("更新链上企业状态失败: entId={}", entId, e);
            throw new RuntimeException("操作失败，请稍后重试");
        }
    }

    @Override
    public String updateCreditRatingOnChain(Long entId, String rating) {
        Enterprise enterprise = getEnterpriseById(entId);
        if (enterprise == null) {
            throw new IllegalArgumentException("企业不存在");
        }
        if (enterprise.getBlockchainAddress() == null) {
            throw new IllegalArgumentException("企业未上链，无区块链地址");
        }
        // 将字符串评级转换为数字
        Integer ratingValue = convertRatingToNumber(rating);
        if (ratingValue == null) {
            throw new IllegalArgumentException("无效的信用评级格式: " + rating);
        }
        try {
            org.fisco.bcos.sdk.v3.model.TransactionReceipt receipt = enterpriseContractService.updateCreditRating(
                    enterprise.getBlockchainAddress(),
                    BigInteger.valueOf(ratingValue)
            );
            if (receipt.isStatusOK()) {
                logger.info("更新链上企业信用评级成功: entId={}, rating={}({}), txHash={}",
                        entId, rating, ratingValue, receipt.getTransactionHash());
                return receipt.getTransactionHash();
            } else {
                throw new RuntimeException("操作失败，请稍后重试");
            }
        } catch (Exception e) {
            logger.error("更新链上企业信用评级失败: entId={}", entId, e);
            throw new RuntimeException("操作失败，请稍后重试");
        }
    }

    /**
     * 将字符串评级转换为数字
     * 评级映射规则:
     * AAA -> 100, AA+ -> 95, AA -> 90, AA- -> 85
     * A+ -> 80, A -> 75, A- -> 70
     * BBB+ -> 65, BBB -> 60, BBB- -> 55
     * BB+ -> 50, BB -> 45, BB- -> 40
     * B+ -> 35, B -> 30, B- -> 25
     * CCC+ -> 20, CCC -> 15, CCC- -> 10
     * CC -> 5, C -> 3, D -> 0
     */
    private Integer convertRatingToNumber(String rating) {
        if (rating == null || rating.isEmpty()) {
            return null;
        }
        rating = rating.trim().toUpperCase();

        switch (rating) {
            case "AAA":
                return 100;
            case "AA+":
                return 95;
            case "AA":
                return 90;
            case "AA-":
                return 85;
            case "A+":
                return 80;
            case "A":
                return 75;
            case "A-":
                return 70;
            case "BBB+":
                return 65;
            case "BBB":
                return 60;
            case "BBB-":
                return 55;
            case "BB+":
                return 50;
            case "BB":
                return 45;
            case "BB-":
                return 40;
            case "B+":
                return 35;
            case "B":
                return 30;
            case "B-":
                return 25;
            case "CCC+":
                return 20;
            case "CCC":
                return 15;
            case "CCC-":
                return 10;
            case "CC":
                return 5;
            case "C":
                return 3;
            case "D":
                return 0;
            default:
                // 尝试直接解析数字
                try {
                    int value = Integer.parseInt(rating);
                    if (value >= 0 && value <= 100) {
                        return value;
                    }
                } catch (NumberFormatException e) {
                    // 忽略
                }
                return null;
        }
    }

    @Override
    public String setCreditLimitOnChain(Long entId, Long creditLimit) {
        Enterprise enterprise = getEnterpriseById(entId);
        if (enterprise == null) {
            throw new IllegalArgumentException("企业不存在");
        }
        if (enterprise.getBlockchainAddress() == null) {
            throw new IllegalArgumentException("企业未上链，无区块链地址");
        }
        try {
            org.fisco.bcos.sdk.v3.model.TransactionReceipt receipt = enterpriseContractService.setCreditLimit(
                    enterprise.getBlockchainAddress(),
                    BigInteger.valueOf(creditLimit)
            );
            if (receipt.isStatusOK()) {
                logger.info("设置链上企业授信额度成功: entId={}, creditLimit={}, txHash={}",
                        entId, creditLimit, receipt.getTransactionHash());
                return receipt.getTransactionHash();
            } else {
                throw new RuntimeException("操作失败，请稍后重试");
            }
        } catch (Exception e) {
            logger.error("设置链上企业授信额度失败: entId={}", entId, e);
            throw new RuntimeException("操作失败，请稍后重试");
        }
    }

    // ==================== 私有方法 ====================

    private InvitationCode getInvitationCodeByCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<InvitationCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InvitationCode::getCode, code);
        return invitationCodeMapper.selectOne(wrapper);
    }

    private String generateUniqueCode() {
        // 生成6位大写字母+数字组合
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            int index = (int) (Math.random() * chars.length());
            sb.append(chars.charAt(index));
        }
        return sb.toString();
    }

    /**
     * 获取企业状态名称
     */
    private String getStatusName(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case Enterprise.STATUS_PENDING:
                return "待审核";
            case Enterprise.STATUS_NORMAL:
                return "正常";
            case Enterprise.STATUS_FROZEN:
                return "冻结";
            case Enterprise.STATUS_CANCELLING:
                return "注销中";
            case Enterprise.STATUS_CANCELLED:
                return "已注销";
            default:
                return "未知";
        }
    }

    /**
     * 校验密码强度
     *
     * @param password 密码
     */
    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("密码长度不能少于8位");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("密码必须包含大写字母");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new IllegalArgumentException("密码必须包含小写字母");
        }
        if (!password.matches(".*\\d.*")) {
            throw new IllegalArgumentException("密码必须包含数字");
        }
    }
}
