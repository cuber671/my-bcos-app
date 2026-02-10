package com.fisco.app.service.enterprise;
import java.util.List;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fisco.app.entity.enterprise.Enterprise;
import com.fisco.app.entity.enterprise.EnterpriseAuditLog;
import com.fisco.app.entity.user.User;
import com.fisco.app.repository.enterprise.EnterpriseAuditLogRepository;
import com.fisco.app.repository.enterprise.EnterpriseRepository;
import com.fisco.app.repository.user.UserRepository;
import com.fisco.app.service.blockchain.ContractService;

import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 企业Service
 */
@Slf4j
@ConditionalOnProperty(name = "fisco.enabled", havingValue = "true", matchIfMissing = true)
@Service
@Api(tags = "企业服务")
@RequiredArgsConstructor
public class EnterpriseService {

    private final EnterpriseRepository enterpriseRepository;
    private final UserRepository userRepository;
    private final EnterpriseAuditLogRepository auditLogRepository;
    private final ContractService contractService;
    private final com.fisco.app.repository.enterprise.CreditRatingHistoryRepository creditRatingHistoryRepository;
    private final com.fisco.app.repository.bill.BillRepository billRepository;
    private final com.fisco.app.repository.receivable.ReceivableRepository receivableRepository;
    private final com.fisco.app.repository.warehouse.ElectronicWarehouseReceiptRepository warehouseReceiptRepository;

    /**
     * 注册企业（完整版本，包含IP地址）
     */
    @Transactional
    public Enterprise registerEnterprise(Enterprise enterprise, String initialPassword,
                                         String createdBy, String ipAddress) {
        log.info("==================== 企业注册开始 ====================");
        log.info("企业基本信息: name={}, creditCode={}, role={}, address={}",
                 enterprise.getName(), enterprise.getCreditCode(), enterprise.getRole(), enterprise.getAddress());
        log.info("创建者: {}, IP: {}", createdBy, ipAddress);
        log.debug("初始密码: {}", initialPassword != null && !initialPassword.isEmpty() ? "已提供" : "未提供");

        long startTime = System.currentTimeMillis();

        try {
            // 检查信用代码是否已存在
            log.debug("检查信用代码唯一性: creditCode={}", enterprise.getCreditCode());
            if (enterpriseRepository.existsByCreditCode(enterprise.getCreditCode())) {
                log.warn("信用代码已存在: creditCode={}", enterprise.getCreditCode());
                throw new com.fisco.app.exception.BusinessException("信用代码已存在");
            }
            log.debug("信用代码唯一性检查通过");

            // 检查用户名是否已存在
            log.debug("检查用户名唯一性: username={}", enterprise.getUsername());
            if (enterpriseRepository.existsByUsername(enterprise.getUsername())) {
                log.warn("用户名已存在: username={}", enterprise.getUsername());
                throw new com.fisco.app.exception.BusinessException("用户名已存在");
            }
            log.debug("用户名唯一性检查通过");

            // 检查邮箱是否已存在（如果提供了）
            if (enterprise.getEmail() != null && !enterprise.getEmail().trim().isEmpty()) {
                log.debug("检查邮箱唯一性: email={}", enterprise.getEmail());
                if (enterpriseRepository.existsByEmail(enterprise.getEmail())) {
                    log.warn("邮箱已存在: email={}", enterprise.getEmail());
                    throw new com.fisco.app.exception.BusinessException("邮箱已被使用");
                }
                log.debug("邮箱唯一性检查通过");
            }

            // 检查手机号是否已存在（如果提供了）
            if (enterprise.getPhone() != null && !enterprise.getPhone().trim().isEmpty()) {
                log.debug("检查手机号唯一性: phone={}", enterprise.getPhone());
                if (enterpriseRepository.existsByPhone(enterprise.getPhone())) {
                    log.warn("手机号已存在: phone={}", enterprise.getPhone());
                    throw new com.fisco.app.exception.BusinessException("手机号已被使用");
                }
                log.debug("手机号唯一性检查通过");
            }

            // 设置默认角色（如果未指定）
            if (enterprise.getRole() == null) {
                log.debug("设置默认角色: SUPPLIER");
                enterprise.setRole(Enterprise.EnterpriseRole.SUPPLIER);
            }

            // 设置初始状态
            if (enterprise.getStatus() == null) {
                log.debug("设置默认状态: PENDING");
                enterprise.setStatus(Enterprise.EnterpriseStatus.PENDING);
            }

            // 自动生成区块链地址（如果未提供）
            if (enterprise.getAddress() == null || enterprise.getAddress().trim().isEmpty()) {
                log.debug("区块链地址未提供，自动生成随机地址");
                String address = generateRandomAddress();
                enterprise.setAddress(address);
                log.info("✓ 自动生成区块链地址: {}", address);
            } else {
                log.debug("使用提供的区块链地址: {}", enterprise.getAddress());
            }

            // 设置审计字段
            enterprise.setCreatedBy(createdBy);
            enterprise.setUpdatedBy(createdBy);
            log.debug("设置审计字段: createdBy={}, updatedBy={}", createdBy, createdBy);

            // 加密密码并设置
            if (initialPassword != null && !initialPassword.isEmpty()) {
                log.debug("加密用户密码");
                String encodedPassword = com.fisco.app.security.PasswordUtil.encode(initialPassword);
                enterprise.setPassword(encodedPassword);
                log.debug("密码加密完成");
            }

            // 生成API密钥
            log.debug("生成API密钥");
            String apiKey = com.fisco.app.security.PasswordUtil.generateApiKey();
            enterprise.setApiKey(apiKey);
            log.debug("API密钥生成完成: {}...", maskApiKey(apiKey));

            // 保存到数据库
            log.debug("保存企业信息到数据库");
            Enterprise saved = enterpriseRepository.save(enterprise);
            log.info("✓ 企业信息已保存到数据库: id={}, address={}", saved.getId(), saved.getAddress());

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 企业注册成功: name={}, address={}, 耗时={}ms", saved.getName(), saved.getAddress(), duration);
            log.info("==================== 企业注册结束 ====================");

            return saved;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 企业注册失败: name={}, creditCode={}, 耗时={}ms, error={}",
                     enterprise.getName(), enterprise.getCreditCode(), duration, e.getMessage(), e);
            log.info("==================== 企业注册失败（结束） ====================");
            throw e;
        }
    }

    /**
     * 注册企业（兼容旧版本，3参数）
     */
    @Transactional
    public Enterprise registerEnterprise(Enterprise enterprise, String initialPassword, String operator) {
        // 调用新版本，IP地址设为 null
        return registerEnterprise(enterprise, initialPassword, operator, null);
    }

    /**
     * 注册企业（重载方法，兼容旧版本）
     */
    @Transactional
    public Enterprise registerEnterprise(Enterprise enterprise, String initialPassword) {
        // 默认操作人为 system
        return registerEnterprise(enterprise, initialPassword, "system", null);
    }

    /**
     * 设置企业密码
     */
    @Transactional
    public void setEnterprisePassword(String address, String newPassword, String updatedBy) {
        log.info("设置企业密码: address={}, updatedBy={}", address, updatedBy);

        Enterprise enterprise = enterpriseRepository.findByAddress(address)
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException.EnterpriseNotFoundException(address));

        String encodedPassword = com.fisco.app.security.PasswordUtil.encode(newPassword);
        enterprise.setPassword(encodedPassword);
        enterprise.setUpdatedBy(updatedBy);
        enterpriseRepository.save(enterprise);

        log.info("企业密码设置成功: address={}, updatedBy={}", address, updatedBy);
    }

    /**
     * @deprecated 使用 {@link #setEnterprisePassword(String, String, String)} 代替
     */
    @Deprecated
    public void setEnterprisePassword(String address, String newPassword) {
        setEnterprisePassword(address, newPassword, "SYSTEM");
    }

    /**
     * 重置API密钥
     */
    @Transactional
    public String resetApiKey(String address, String updatedBy) {
        log.info("重置API密钥: address={}, updatedBy={}", address, updatedBy);

        Enterprise enterprise = enterpriseRepository.findByAddress(address)
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException.EnterpriseNotFoundException(address));

        String newApiKey = com.fisco.app.security.PasswordUtil.generateApiKey();
        enterprise.setApiKey(newApiKey);
        enterprise.setUpdatedBy(updatedBy);
        enterpriseRepository.save(enterprise);

        log.info("API密钥重置成功: address={}, updatedBy={}", address, updatedBy);
        return newApiKey;
    }

    /**
     * @deprecated 使用 {@link #resetApiKey(String, String)} 代替
     */
    @Deprecated
    public String resetApiKey(String address) {
        return resetApiKey(address, "SYSTEM");
    }

    /**
     * 掩码API密钥
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) {
            return "***";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }

    /**
     * 更新企业状态
     */
    @Transactional
    public void updateEnterpriseStatus(String address, Enterprise.EnterpriseStatus status, String updatedBy) {
        log.info("更新企业状态: address={}, status={}, updatedBy={}", address, status, updatedBy);

        Enterprise enterprise = enterpriseRepository.findByAddress(address)
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException.EnterpriseNotFoundException(address));

        enterprise.setStatus(status);
        enterprise.setUpdatedBy(updatedBy);
        enterpriseRepository.save(enterprise);

        // 调用区块链合约更新状态
        try {
            String txHash = contractService.updateEnterpriseStatusOnChain(address, status);
            log.info("企业状态已上链更新: address={}, status={}, txHash={}", address, status, txHash);
        } catch (Exception e) {
            log.warn("企业状态上链更新失败，但数据库更新成功: address={}, status={}, error={}",
                address, status, e.getMessage());
        }

        log.info("企业状态更新成功: address={}, status={}", address, status);
    }

    /**
     * @deprecated 使用 {@link #updateEnterpriseStatus(String, Enterprise.EnterpriseStatus, String)} 代替
     */
    @Deprecated
    public void updateEnterpriseStatus(String address, Enterprise.EnterpriseStatus status) {
        updateEnterpriseStatus(address, status, "SYSTEM");
    }

    /**
     * 更新信用评级（增强版，包含原因和历史记录）
     */
    @Transactional
    public void updateCreditRating(String address, Integer creditRating, String reason, String updatedBy) {
        log.info("更新信用评级: address={}, rating={}, reason={}, updatedBy={}", address, creditRating, reason, updatedBy);

        if (creditRating < 0 || creditRating > 100) {
            throw new com.fisco.app.exception.BusinessException("信用评级必须在0-100之间");
        }

        Enterprise enterprise = enterpriseRepository.findByAddress(address)
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException.EnterpriseNotFoundException(address));

        Integer oldRating = enterprise.getCreditRating();
        enterprise.setCreditRating(creditRating);
        enterprise.setUpdatedBy(updatedBy);
        enterpriseRepository.save(enterprise);

        // 记录评级变更历史
        com.fisco.app.entity.enterprise.CreditRatingHistory history = new com.fisco.app.entity.enterprise.CreditRatingHistory();
        history.setEnterpriseAddress(address);
        history.setEnterpriseName(enterprise.getName());
        history.setOldRating(oldRating);
        history.setNewRating(creditRating);
        history.setChangeReason(reason);
        history.setChangedBy(updatedBy);
        history.setChangedAt(java.time.LocalDateTime.now());
        creditRatingHistoryRepository.save(history);

        // 调用区块链合约更新信用评级
        try {
            String txHash = contractService.updateCreditRatingOnChain(
                address, creditRating, reason != null && !reason.isEmpty() ? reason : "管理员更新评级");
            log.info("企业信用评级已上链更新: address={}, oldRating={}, newRating={}, txHash={}",
                address, oldRating, creditRating, txHash);

            // 更新历史的txHash
            history.setTxHash(txHash);
            creditRatingHistoryRepository.save(history);
        } catch (Exception e) {
            log.warn("企业信用评级上链更新失败，但数据库更新成功: address={}, rating={}, error={}",
                address, creditRating, e.getMessage());
            // 注意：这里不抛出异常，允许数据库操作成功
        }

        log.info("信用评级更新成功: address={}, oldRating={}, newRating={}", address, oldRating, creditRating);
    }

    /**
     * 更新信用评级（简化版，兼容旧代码）
     * @deprecated 使用 {@link #updateCreditRating(String, Integer, String, String)} 代替
     */
    @Deprecated
    public void updateCreditRating(String address, Integer creditRating, String updatedBy) {
        updateCreditRating(address, creditRating, null, updatedBy);
    }

    /**
     * @deprecated 使用 {@link #updateCreditRating(String, Integer, String, String)} 代替
     */
    @Deprecated
    public void updateCreditRating(String address, Integer creditRating) {
        updateCreditRating(address, creditRating, null, "SYSTEM");
    }

    /**
     * 设置授信额度
     */
    @Transactional
    public void setCreditLimit(String address, java.math.BigDecimal creditLimit, String updatedBy) {
        log.info("设置授信额度: address={}, limit={}, updatedBy={}", address, creditLimit, updatedBy);

        Enterprise enterprise = enterpriseRepository.findByAddress(address)
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException.EnterpriseNotFoundException(address));

        enterprise.setCreditLimit(creditLimit);
        enterprise.setUpdatedBy(updatedBy);
        enterpriseRepository.save(enterprise);

        // 调用区块链合约设置授信额度
        try {
            String txHash = contractService.setCreditLimitOnChain(address, creditLimit);
            log.info("企业授信额度已上链设置: address={}, limit={}, txHash={}",
                address, creditLimit, txHash);
        } catch (Exception e) {
            log.warn("企业授信额度上链设置失败，但数据库更新成功: address={}, limit={}, error={}",
                address, creditLimit, e.getMessage());
            // 注意：这里不抛出异常，允许数据库操作成功
        }

        log.info("授信额度设置成功: address={}, limit={}, updatedBy={}", address, creditLimit, updatedBy);
    }

    /**
     * @deprecated 使用 {@link #setCreditLimit(String, java.math.BigDecimal, String)} 代替
     */
    @Deprecated
    public void setCreditLimit(String address, java.math.BigDecimal creditLimit) {
        setCreditLimit(address, creditLimit, "SYSTEM");
    }

    /**
     * 获取企业信息
     */
    public Enterprise getEnterprise(String address) {
        return enterpriseRepository.findByAddress(address)
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException.EnterpriseNotFoundException(address));
    }

    /**
     * 根据用户名获取企业信息
     * @param username 用户名
     * @return 企业信息
     * @throws com.fisco.app.exception.BusinessException.EnterpriseNotFoundException 如果企业不存在
     */
    public Enterprise getEnterpriseByUsername(String username) {
        log.debug("根据用户名查找企业: username={}", username);
        return enterpriseRepository.findByUsername(username)
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException.EnterpriseNotFoundException("用户名: " + username));
    }

    /**
     * 根据邮箱获取企业信息
     * @param email 邮箱
     * @return 企业信息
     * @throws com.fisco.app.exception.BusinessException.EnterpriseNotFoundException 如果企业不存在
     */
    public Enterprise getEnterpriseByEmail(String email) {
        log.debug("根据邮箱查找企业: email={}", email);
        return enterpriseRepository.findByEmail(email)
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException.EnterpriseNotFoundException("邮箱: " + email));
    }

    /**
     * 根据手机号获取企业信息
     * @param phone 手机号
     * @return 企业信息
     * @throws com.fisco.app.exception.BusinessException.EnterpriseNotFoundException 如果企业不存在
     */
    public Enterprise getEnterpriseByPhone(String phone) {
        log.debug("根据手机号查找企业: phone={}", phone);
        return enterpriseRepository.findByPhone(phone)
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException.EnterpriseNotFoundException("手机号: " + phone));
    }

    /**
     * 根据ID获取企业信息
     */
    public Enterprise getEnterpriseById(@NonNull String enterpriseId) {
        return enterpriseRepository.findById(enterpriseId)
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException.EnterpriseNotFoundException(enterpriseId));
    }

    /**
     * 根据API密钥获取企业信息
     * @param apiKey API密钥
     * @return 企业信息
     * @throws com.fisco.app.exception.BusinessException.EnterpriseNotFoundException 如果企业不存在
     */
    public Enterprise getEnterpriseByApiKey(String apiKey) {
        log.debug("根据API密钥查找企业: apiKey={}", apiKey != null ? apiKey.substring(0, Math.min(4, apiKey.length())) + "****" : null);
        return enterpriseRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException.EnterpriseNotFoundException("API密钥: " + apiKey));
    }

    /**
     * 根据企业ID（UUID）获取企业及其所有用户
     * @param enterpriseId 企业ID（UUID）
     * @return 企业及其用户信息
     * @throws com.fisco.app.exception.BusinessException.EnterpriseNotFoundException 如果企业不存在
     */
    public com.fisco.app.dto.enterprise.EnterpriseWithUsersDTO getEnterpriseWithUsers(String enterpriseId) {
        Enterprise enterprise = enterpriseRepository.findByIdWithUsers(enterpriseId)
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException.EnterpriseNotFoundException(enterpriseId));

        return com.fisco.app.dto.enterprise.EnterpriseWithUsersDTO.fromEntity(enterprise);
    }
    public com.fisco.app.dto.enterprise.EnterpriseWithUsersDTO getEnterpriseWithUsersByAddress(String address) {
        Enterprise enterprise = enterpriseRepository.findByAddressWithUsers(address)
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException.EnterpriseNotFoundException(address));

        return com.fisco.app.dto.enterprise.EnterpriseWithUsersDTO.fromEntity(enterprise);
    }

    /**
     * 根据用户名获取企业及其所有用户
     */
    public com.fisco.app.dto.enterprise.EnterpriseWithUsersDTO getEnterpriseWithUsersByUsername(String username) {
        Enterprise enterprise = enterpriseRepository.findByUsernameWithUsers(username)
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException.EnterpriseNotFoundException(username));

        return com.fisco.app.dto.enterprise.EnterpriseWithUsersDTO.fromEntity(enterprise);
    }

    /**
     * 获取企业及其用户数量统计
     */
    public com.fisco.app.dto.enterprise.EnterpriseWithUsersDTO getEnterpriseWithUserCount(String enterpriseId) {
        if (enterpriseId == null) {
            throw new com.fisco.app.exception.BusinessException("企业ID不能为空");
        }

        Enterprise enterprise = enterpriseRepository.findById(enterpriseId)
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException.EnterpriseNotFoundException(String.valueOf(enterpriseId)));

        java.util.List<com.fisco.app.entity.user.User> users = userRepository.findByEnterpriseId(enterpriseId);

        return com.fisco.app.dto.enterprise.EnterpriseWithUsersDTO.fromEntities(enterprise, users);
    }

    // ==================== 审核相关方法 ====================

    /**
     * 根据企业ID审核企业（推荐使用此方法）
     * 通过ID查询企业后，使用其地址进行审核
     */
    @Transactional
    public void approveEnterpriseById(@NonNull String id, String auditor, String reason, String ipAddress) {
        log.info("通过ID审核企业: id={}, auditor={}", id, auditor);

        // 通过ID查询企业
        Enterprise enterprise = enterpriseRepository.findById(id)
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException.EnterpriseNotFoundException(id));

        // 获取企业地址后调用原有的审核方法
        approveEnterprise(enterprise.getAddress(), auditor, reason, ipAddress);
    }

    /**
     * 根据企业ID审核企业（简化版）
     */
    @Transactional
    public void approveEnterpriseById(@NonNull String id, String auditor) {
        approveEnterpriseById(id, auditor, null, null);
    }

    /**
     * 根据企业ID拒绝企业（推荐使用此方法）
     * 通过ID查询企业后，使用其地址进行拒绝
     */
    @Transactional
    public void rejectEnterpriseById(@NonNull String id, String auditor, String reason, String ipAddress) {
        log.info("通过ID拒绝企业: id={}, auditor={}, reason={}", id, auditor, reason);

        // 通过ID查询企业
        Enterprise enterprise = enterpriseRepository.findById(id)
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException.EnterpriseNotFoundException(id));

        // 获取企业地址后调用原有的拒绝方法
        rejectEnterprise(enterprise.getAddress(), auditor, reason, ipAddress);
    }

    /**
     * 根据企业ID拒绝企业（简化版）
     */
    @Transactional
    public void rejectEnterpriseById(@NonNull String id, String auditor) {
        rejectEnterpriseById(id, auditor, null, null);
    }

    /**
     * 审核企业（修改版，记录审核日志）
     * @param address 企业地址（区块链地址，0x开头）
     */
    @Transactional
    public void approveEnterprise(String address, String auditor) {
        log.info("审核企业通过: address={}, auditor={}", address, auditor);
        approveEnterprise(address, auditor, null, null);
    }

    /**
     * 审核企业（完整版，带审核理由和IP）
     */
    @Transactional
    public void approveEnterprise(String address, String auditor, String reason, String ipAddress) {
        log.info("==================== 企业审核开始 ====================");
        log.info("审核信息: address={}, auditor={}, reason={}, ip={}",
                 address, auditor, reason, ipAddress);

        long startTime = System.currentTimeMillis();

        try {
            log.debug("查询企业信息: address={}", address);
            Enterprise enterprise = enterpriseRepository.findByAddress(address)
                    .orElseThrow(() -> new com.fisco.app.exception.BusinessException.EnterpriseNotFoundException(address));
            log.debug("企业信息查询成功: name={}, currentStatus={}", enterprise.getName(), enterprise.getStatus());

            // 步骤1: 将企业注册到区块链（审核通过后才上链）
            log.debug("准备上链注册企业");
            String txHash = null;
            try {
                txHash = contractService.registerEnterpriseOnChain(enterprise);
                log.info("✓ 企业已上链注册: address={}, txHash={}", address, txHash);
            } catch (Exception e) {
                log.error("✗ 企业上链注册失败: address={}, error={}", address, e.getMessage(), e);
                // 上链失败，拒绝审核
                throw new com.fisco.app.exception.BusinessException(
                    500, "企业上链注册失败，无法通过审核: " + e.getMessage(), e);
            }

            // 步骤2: 更新数据库状态为 ACTIVE
            log.debug("更新企业状态: {} -> ACTIVE", enterprise.getStatus());
            Enterprise.EnterpriseStatus oldStatus = enterprise.getStatus();
            enterprise.setStatus(Enterprise.EnterpriseStatus.ACTIVE);

            Enterprise saved = enterpriseRepository.save(enterprise);
            log.info("✓ 数据库更新成功: status={} -> ACTIVE", oldStatus);

            // 记录审核日志
            log.debug("记录审核日志到数据库");
            recordAuditLog(saved, auditor, EnterpriseAuditLog.AuditAction.APPROVE,
                           reason, ipAddress, txHash, "审核通过，企业已上链注册，状态从 " + oldStatus + " 变更为 ACTIVE");
            log.info("✓ 审核日志记录成功");

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 企业审核通过: address={}, name={}, auditor={}, txHash={}, 耗时={}ms",
                     address, saved.getName(), auditor, txHash, duration);
            log.info("==================== 企业审核结束 ====================");
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 企业审核失败: address={}, auditor={}, 耗时={}ms, error={}",
                     address, auditor, duration, e.getMessage(), e);
            log.info("==================== 企业审核失败（结束） ====================");
            throw e;
        }
    }

    /**
     * 拒绝企业注册
     */
    @Transactional
    public void rejectEnterprise(String address, String auditor) {
        rejectEnterprise(address, auditor, null, null);
    }

    /**
     * 拒绝企业注册（完整版，带拒绝理由和IP）
     */
    @Transactional
    public void rejectEnterprise(String address, String auditor, String reason, String ipAddress) {
        log.info("拒绝企业注册（删除数据）: address={}, auditor={}, reason={}", address, auditor, reason);

        Enterprise enterprise = enterpriseRepository.findByAddress(address)
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException.EnterpriseNotFoundException(address));

        // 记录企业信息（用于日志）
        String enterpriseId = enterprise.getId();
        String enterpriseName = enterprise.getName();
        String enterpriseAddress = enterprise.getAddress();
        Enterprise.EnterpriseStatus oldStatus = enterprise.getStatus();

        // 验证 enterpriseId 不为空
        if (enterpriseId == null) {
            throw new IllegalStateException("企业ID不能为空: address=" + address);
        }

        // 先记录审核日志（保留审计痕迹）
        recordAuditLog(enterprise, auditor, EnterpriseAuditLog.AuditAction.REJECT,
                       reason, ipAddress, null, "企业注册申请被拒绝，已删除企业数据。原状态: " + oldStatus);

        // 删除该企业的所有关联用户
        List<User> users = userRepository.findByEnterpriseId(enterpriseId);
        if (!users.isEmpty()) {
            log.info("删除企业关联的用户: enterpriseId={}, userCount={}", enterpriseId, users.size());
            userRepository.deleteAll(users);
            log.info("✓ 用户已删除: count={}", users.size());
        }

        // 删除企业记录
        enterpriseRepository.deleteById(enterpriseId);
        log.info("✓ 企业已删除: id={}, name={}, address={}", enterpriseId, enterpriseName, enterpriseAddress);

        log.info("✓✓✓ 企业拒绝完成: address={}, auditor={}, reason={}, deletedUsers={}",
                 address, auditor, reason, users.size());
    }

    /**
     * 批量审核企业
     */
    @Transactional
    public com.fisco.app.dto.audit.AuditBatchResult batchApproveEnterprises(
            java.util.List<String> addresses, String auditor, String ipAddress) {

        log.info("批量审核企业: count={}, auditor={}", addresses.size(), auditor);

        int successCount = 0;
        int failCount = 0;
        java.util.List<String> failedAddresses = new java.util.ArrayList<>();
        java.util.Map<String, String> errorMessages = new java.util.HashMap<>();

        for (String address : addresses) {
            try {
                approveEnterprise(address, auditor, "批量审核通过", ipAddress);
                successCount++;
            } catch (Exception e) {
                failCount++;
                failedAddresses.add(address);
                errorMessages.put(address, e.getMessage());
                log.error("批量审核失败: address={}, error={}", address, e.getMessage());
            }
        }

        com.fisco.app.dto.audit.AuditBatchResult result = new com.fisco.app.dto.audit.AuditBatchResult();
        result.setTotalCount(addresses.size());
        result.setSuccessCount(successCount);
        result.setFailCount(failCount);
        result.setFailedAddresses(failedAddresses);
        result.setErrorMessages(errorMessages);
        result.setAuditor(auditor);
        result.setAuditTime(java.time.LocalDateTime.now());

        log.info("批量审核完成: total={}, success={}, fail={}",
                 addresses.size(), successCount, failCount);

        return result;
    }

    /**
     * 批量拒绝企业
     */
    @Transactional
    public com.fisco.app.dto.audit.AuditBatchResult batchRejectEnterprises(
            java.util.List<String> addresses, String auditor, String reason, String ipAddress) {

        log.info("批量拒绝企业: count={}, auditor={}, reason={}",
                 addresses.size(), auditor, reason);

        int successCount = 0;
        int failCount = 0;
        java.util.List<String> failedAddresses = new java.util.ArrayList<>();
        java.util.Map<String, String> errorMessages = new java.util.HashMap<>();

        for (String address : addresses) {
            try {
                rejectEnterprise(address, auditor, reason, ipAddress);
                successCount++;
            } catch (Exception e) {
                failCount++;
                failedAddresses.add(address);
                errorMessages.put(address, e.getMessage());
                log.error("批量拒绝失败: address={}, error={}", address, e.getMessage());
            }
        }

        com.fisco.app.dto.audit.AuditBatchResult result = new com.fisco.app.dto.audit.AuditBatchResult();
        result.setTotalCount(addresses.size());
        result.setSuccessCount(successCount);
        result.setFailCount(failCount);
        result.setFailedAddresses(failedAddresses);
        result.setErrorMessages(errorMessages);
        result.setAuditor(auditor);
        result.setAuditTime(java.time.LocalDateTime.now());

        log.info("批量拒绝完成: total={}, success={}, fail={}",
                 addresses.size(), successCount, failCount);

        return result;
    }

    /**
     * 记录审核日志
     */
    private void recordAuditLog(Enterprise enterprise, String auditor,
                                EnterpriseAuditLog.AuditAction action,
                                String reason, String ipAddress, String txHash,
                                String remarks) {
        EnterpriseAuditLog auditLog = new EnterpriseAuditLog();
        auditLog.setEnterpriseAddress(enterprise.getAddress());
        auditLog.setEnterpriseName(enterprise.getName());
        auditLog.setAuditor(auditor);
        auditLog.setAction(action);
        auditLog.setReason(reason);
        auditLog.setIpAddress(ipAddress);
        auditLog.setTxHash(txHash);
        auditLog.setRemarks(remarks);
        auditLog.setAuditTime(java.time.LocalDateTime.now());

        auditLogRepository.save(auditLog);

        log.debug("审核日志已记录: enterprise={}, action={}, auditor={}",
                  enterprise.getAddress(), action, auditor);
    }

    /**
     * 获取企业的审核历史
     */
    public java.util.List<EnterpriseAuditLog> getEnterpriseAuditHistory(String address) {
        return auditLogRepository.findByEnterpriseAddressOrderByAuditTimeDesc(address);
    }

    /**
     * 获取审核人的审核历史
     */
    public java.util.List<EnterpriseAuditLog> getAuditorHistory(String auditor) {
        return auditLogRepository.findByAuditorOrderByAuditTimeDesc(auditor);
    }

    /**
     * 获取所有审核日志（分页）
     */
    public org.springframework.data.domain.Page<EnterpriseAuditLog> getAllAuditLogs(
            org.springframework.data.domain.Pageable pageable) {
        // 防御性编程：如果pageable为null，提供默认值
        if (pageable == null) {
            pageable = org.springframework.data.domain.PageRequest.of(0, 20);
        }
        return auditLogRepository.findAll(pageable);
    }

    /**
     * 根据审核动作查询日志
     */
    public java.util.List<EnterpriseAuditLog> getAuditLogsByAction(
            EnterpriseAuditLog.AuditAction action) {
        return auditLogRepository.findByActionOrderByAuditTimeDesc(action);
    }

    /**
     * 统计审核人的审核次数
     */
    public java.util.Map<String, Long> getAuditorStatistics() {
        java.util.Map<String, Long> stats = new java.util.HashMap<>();
        List<Object[]> results = auditLogRepository.countByAuditor();
        for (Object[] result : results) {
            String auditor = (String) result[0];
            Long count = (Long) result[1];
            stats.put(auditor, count);
        }
        return stats;
    }

    /**
     * 请求企业注销
     * 企业注销需要审核，审核通过后才会真正删除
     */
    @Transactional
    public void requestEnterpriseDeletion(String address, String reason, String requester, String ipAddress) {
        log.info("==================== 企业注销请求开始 ====================");
        log.info("注销请求信息: address={}, requester={}, reason={}", address, requester, reason);

        long startTime = System.currentTimeMillis();

        try {
            // 查找企业
            Enterprise enterprise = enterpriseRepository.findByAddress(address)
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException.EnterpriseNotFoundException(address));

            // 检查企业状态：只有ACTIVE状态的企业可以申请注销
            if (enterprise.getStatus() != Enterprise.EnterpriseStatus.ACTIVE) {
                log.warn("企业状态不允许注销: address={}, status={}", address, enterprise.getStatus());
                throw new com.fisco.app.exception.BusinessException(
                    400, "只有已激活的企业才能申请注销，当前状态：" + enterprise.getStatus());
            }

            // 检查是否已有待删除的请求
            if (enterprise.getStatus() == Enterprise.EnterpriseStatus.PENDING_DELETION) {
                log.warn("企业已在注销审核流程中: address={}", address);
                throw new com.fisco.app.exception.BusinessException(400, "企业已在注销审核流程中，请勿重复提交");
            }

            // 更新企业状态为待删除
            Enterprise.EnterpriseStatus oldStatus = enterprise.getStatus();
            enterprise.setStatus(Enterprise.EnterpriseStatus.PENDING_DELETION);
            enterprise.setUpdatedBy(requester);
            enterpriseRepository.save(enterprise);

            // 记录审核日志
            recordAuditLog(enterprise, requester, EnterpriseAuditLog.AuditAction.REQUEST_DELETE,
                reason, ipAddress, null, "申请注销企业，状态从 " + oldStatus + " 变更为 PENDING_DELETION");

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 企业注销请求已提交: address={}, 耗时={}ms", address, duration);
            log.info("==================== 企业注销请求结束 ====================");

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 企业注销请求失败: address={}, 耗时={}ms, error={}", address, duration, e.getMessage(), e);
            log.info("==================== 企业注销请求失败（结束） ====================");
            throw e;
        }
    }

    /**
     * 审核通过企业注销
     * 删除区块链记录、数据库企业记录和关联的员工记录
     */
    @Transactional
    public void approveEnterpriseDeletion(String address, String auditor, String reason, String ipAddress) {
        log.info("==================== 企业注销审核通过开始 ====================");
        log.info("审核信息: address={}, auditor={}, reason={}", address, auditor, reason);

        long startTime = System.currentTimeMillis();

        try {
            // 查找企业
            Enterprise enterprise = enterpriseRepository.findByAddress(address)
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException.EnterpriseNotFoundException(address));

            // 检查企业状态
            if (enterprise.getStatus() != Enterprise.EnterpriseStatus.PENDING_DELETION) {
                log.warn("企业状态不允许注销: address={}, status={}", address, enterprise.getStatus());
                throw new com.fisco.app.exception.BusinessException(
                    400, "只有待删除状态的企业才能通过注销审核，当前状态：" + enterprise.getStatus());
            }

            String txHash = null;

            // 步骤1: 如果企业已上链，先从区块链删除
            try {
                log.debug("准备从区块链删除企业: address={}", address);
                txHash = contractService.removeEnterpriseFromChain(address);
                log.info("✓ 企业已从区块链删除: address={}, txHash={}", address, txHash);
            } catch (Exception e) {
                log.error("✗ 从区块链删除企业失败: address={}, error={}", address, e.getMessage(), e);
                // 区块链删除失败，拒绝审核
                throw new com.fisco.app.exception.BusinessException(
                    500, "从区块链删除企业失败，无法通过注销审核: " + e.getMessage(), e);
            }

            // 步骤2: 统计该企业的员工数量
            Long userCount = userRepository.countByEnterpriseId(enterprise.getId());
            log.info("企业员工数量: address={}, userCount={}", address, userCount);

            // 步骤3: 删除企业的所有员工
            if (userCount > 0) {
                log.debug("准备删除企业员工: enterpriseId={}, count={}", enterprise.getId(), userCount);
                userRepository.deleteByEnterpriseId(enterprise.getId());
                log.info("✓ 已删除企业员工: count={}", userCount);
            }

            // 步骤4: 记录审核日志（在删除企业之前）
            recordAuditLog(enterprise, auditor, EnterpriseAuditLog.AuditAction.APPROVE_DELETE,
                reason, ipAddress, txHash, "审核通过注销企业，已从区块链删除，删除员工 " + userCount + " 人");

            // 步骤5: 删除企业记录
            log.debug("准备删除企业记录: address={}", address);
            enterpriseRepository.delete(enterprise);
            log.info("✓ 已删除企业记录: address={}", address);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 企业注销审核通过完成: address={}, txHash={}, deletedUsers={}, 耗时={}ms",
                address, txHash, userCount, duration);
            log.info("==================== 企业注销审核通过结束 ====================");

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 企业注销审核失败: address={}, 耗时={}ms, error={}", address, duration, e.getMessage(), e);
            log.info("==================== 企业注销审核失败（结束） ====================");
            throw e;
        }
    }

    /**
     * 拒绝企业注销请求
     * 恢复企业为原状态（ACTIVE）
     */
    @Transactional
    public void rejectEnterpriseDeletion(String address, String auditor, String reason, String ipAddress) {
        log.info("==================== 拒绝企业注销开始 ====================");
        log.info("拒绝信息: address={}, auditor={}, reason={}", address, auditor, reason);

        long startTime = System.currentTimeMillis();

        try {
            // 查找企业
            Enterprise enterprise = enterpriseRepository.findByAddress(address)
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException.EnterpriseNotFoundException(address));

            // 检查企业状态
            if (enterprise.getStatus() != Enterprise.EnterpriseStatus.PENDING_DELETION) {
                log.warn("企业状态不允许拒绝注销: address={}, status={}", address, enterprise.getStatus());
                throw new com.fisco.app.exception.BusinessException(
                    400, "只有待删除状态的企业才能拒绝注销，当前状态：" + enterprise.getStatus());
            }

            // 恢复企业状态为 ACTIVE
            Enterprise.EnterpriseStatus oldStatus = enterprise.getStatus();
            enterprise.setStatus(Enterprise.EnterpriseStatus.ACTIVE);
            enterprise.setUpdatedBy(auditor);
            enterpriseRepository.save(enterprise);

            // 记录审核日志
            recordAuditLog(enterprise, auditor, EnterpriseAuditLog.AuditAction.REJECT_DELETE,
                reason, ipAddress, null, "拒绝注销企业，状态从 " + oldStatus + " 恢复为 ACTIVE");

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 拒绝企业注销完成: address={}, 耗时={}ms", address, duration);
            log.info("==================== 拒绝企业注销结束 ====================");

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 拒绝企业注销失败: address={}, 耗时={}ms, error={}", address, duration, e.getMessage(), e);
            log.info("==================== 拒绝企业注销失败（结束） ====================");
            throw e;
        }
    }

    // ==================== 查询方法 ====================

    /**
     * 获取待审核企业列表（分页）
     */
    public org.springframework.data.domain.Page<Enterprise> getPendingEnterprises(
            org.springframework.data.domain.Pageable pageable) {
        return enterpriseRepository.findByStatus(Enterprise.EnterpriseStatus.PENDING, pageable);
    }

    /**
     * 获取所有活跃企业
     */
    public List<Enterprise> getActiveEnterprises() {
        return enterpriseRepository.findAllActiveEnterprises();
    }

    /**
     * 根据角色获取企业列表
     */
    public List<Enterprise> getEnterprisesByRole(Enterprise.EnterpriseRole role) {
        return enterpriseRepository.findByRole(role);
    }

    /**
     * 根据状态获取企业列表
     */
    public List<Enterprise> getEnterprisesByStatus(Enterprise.EnterpriseStatus status) {
        return enterpriseRepository.findByStatus(status);
    }

    /**
     * 根据状态获取企业列表（分页）
     */
    public org.springframework.data.domain.Page<Enterprise> getEnterprisesByStatus(
            Enterprise.EnterpriseStatus status, org.springframework.data.domain.Pageable pageable) {
        return enterpriseRepository.findByStatus(status, pageable);
    }

    /**
     * 获取所有企业（分页）
     */
    @org.springframework.lang.NonNull
    public org.springframework.data.domain.Page<Enterprise> getAllEnterprises(
            @org.springframework.lang.NonNull org.springframework.data.domain.Pageable pageable) {
        return enterpriseRepository.findAll(pageable);
    }

    /**
     * 根据信用评级范围获取企业列表
     */
    public List<Enterprise> getEnterprisesByRatingRange(Integer minRating, Integer maxRating) {
        return enterpriseRepository.findByCreditRatingRange(minRating, maxRating);
    }

    /**
     * 验证企业是否有效（地址存在且状态为ACTIVE）
     */
    public boolean isEnterpriseValid(String address) {
        Optional<Enterprise> enterprise = enterpriseRepository.findByAddress(address);
        return enterprise.isPresent() && enterprise.get().getStatus() == Enterprise.EnterpriseStatus.ACTIVE;
    }

    /**
     * 搜索企业
     * 按名称、信用代码或用户名搜索企业
     */
    public List<Enterprise> searchEnterprises(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }

        String likeKeyword = "%" + keyword.trim() + "%";

        // 使用Repository查询方法或自定义查询
        // 这里使用Specification进行灵活查询
        return enterpriseRepository.findAll((root, query, cb) -> {
            List<javax.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();

            predicates.add(cb.or(
                    cb.like(root.get("name"), likeKeyword),
                    cb.like(root.get("creditCode"), likeKeyword),
                    cb.like(root.get("username"), likeKeyword),
                    cb.like(root.get("email"), likeKeyword),
                    cb.like(root.get("phone"), likeKeyword)
            ));

            return cb.and(predicates.toArray(new javax.persistence.criteria.Predicate[0]));
        });
    }

    /**
     * 更新企业信息（企业自我管理）
     * 允许企业更新部分基本信息
     */
    @SuppressWarnings("unused")
    @org.springframework.lang.NonNull
    /**
     * 更新企业信息（企业自我管理）
     * 支持使用企业ID或地址查询
     */
    @Transactional
    public Enterprise updateEnterpriseInfo(String enterpriseIdentifier, String enterpriseAddress,
                                         String email, String phone,
                                         String remarks, String updatedBy) {
        log.info("更新企业信息: identifier={}, enterpriseAddress={}, email={}, phone={}, updatedBy={}",
                 enterpriseIdentifier, enterpriseAddress, email, phone, updatedBy);

        // 优先按ID查找，否则按地址查找
        Enterprise enterprise;
        try {
            // 尝试按ID查找（UUID格式）
            if (enterpriseIdentifier != null && enterpriseIdentifier.matches("^[a-f0-9\\-]{36}$")) {
                enterprise = getEnterpriseById(enterpriseIdentifier);
                log.debug("Found enterprise by ID: {}", enterpriseIdentifier);
            } else {
                // 按地址查找
                enterprise = getEnterprise(enterpriseIdentifier);
                log.debug("Found enterprise by address: {}", enterpriseIdentifier);
            }
        } catch (Exception e) {
            log.error("获取企业失败: identifier={}, error={}", enterpriseIdentifier, e.getMessage());
            throw new com.fisco.app.exception.BusinessException("企业不存在: " + enterpriseIdentifier);
        }

        // 只更新允许企业自己修改的字段
        if (enterpriseAddress != null && !enterpriseAddress.trim().isEmpty()) {
            enterprise.setEnterpriseAddress(enterpriseAddress);
        }
        if (email != null && !email.trim().isEmpty() && !email.equals(enterprise.getEmail())) {
            // 检查邮箱是否被其他企业使用
            if (enterpriseRepository.existsByEmail(email)) {
                throw new com.fisco.app.exception.BusinessException("邮箱已被其他企业使用");
            }
            enterprise.setEmail(email);
        }
        if (phone != null && !phone.trim().isEmpty() && !phone.equals(enterprise.getPhone())) {
            // 检查手机号是否被其他企业使用
            if (enterpriseRepository.existsByPhone(phone)) {
                throw new com.fisco.app.exception.BusinessException("手机号已被其他企业使用");
            }
            enterprise.setPhone(phone);
        }
        if (remarks != null && !remarks.trim().isEmpty()) {
            enterprise.setRemarks(remarks);
        }

        enterprise.setUpdatedBy(updatedBy);
        Enterprise saved = enterpriseRepository.save(enterprise);
        if (saved == null) {
            throw new com.fisco.app.exception.BusinessException("保存企业信息失败");
        }
        log.info("企业信息更新成功: enterpriseId={}, updatedBy={}", saved.getId(), updatedBy);
        return saved;
    }

    /**
     * 修改企业密码（企业自我管理）
     * 验证旧密码后设置新密码
     */
    @Transactional
    public void changeEnterprisePassword(String address, String oldPassword, String newPassword, String updatedBy) {
        log.info("企业请求修改密码: address={}, updatedBy={}", address, updatedBy);

        Enterprise enterprise = getEnterprise(address);

        // 验证旧密码
        if (enterprise.getPassword() == null || enterprise.getPassword().isEmpty()) {
            throw new com.fisco.app.exception.BusinessException("账户未设置密码，请联系管理员初始化");
        }

        if (!com.fisco.app.security.PasswordUtil.matches(oldPassword, enterprise.getPassword())) {
            throw new com.fisco.app.exception.BusinessException("原密码错误");
        }

        // 设置新密码
        enterprise.setPassword(com.fisco.app.security.PasswordUtil.encode(newPassword));
        enterprise.setUpdatedBy(updatedBy);
        enterpriseRepository.save(enterprise);

        log.info("企业密码修改成功: address={}, updatedBy={}", address, updatedBy);
    }

    /**
     * @deprecated 使用 {@link #changeEnterprisePassword(String, String, String, String)} 代替
     */
    @Deprecated
    public void changeEnterprisePassword(String address, String oldPassword, String newPassword) {
        changeEnterprisePassword(address, oldPassword, newPassword, "SELF");
    }

    // ==================== 区块链管理相关辅助方法 ====================

    /**
     * 获取所有已激活的企业（不分页）
     * 用于区块链管理接口查询所有已上链企业
     *
     * @return 所有ACTIVE状态的企业列表
     */
    public List<Enterprise> getAllEnterprisesWithoutPagination() {
        log.debug("查询所有ACTIVE状态的企业（不分页）");
        return enterpriseRepository.findByStatus(Enterprise.EnterpriseStatus.ACTIVE);
    }

    /**
     * 通过地址获取企业
     *
     * @param address 企业区块链地址
     * @return 企业信息
     */
    public Enterprise getEnterpriseByAddress(String address) {
        log.debug("通过地址查询企业: address={}", address);
        return enterpriseRepository.findByAddress(address)
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException.EnterpriseNotFoundException(address));
    }

    /**
     * 统计指定状态的企业数量
     *
     * @param status 企业状态
     * @return 企业数量
     */
    public long countByStatus(Enterprise.EnterpriseStatus status) {
        log.debug("统计企业数量: status={}", status);
        return enterpriseRepository.countByStatus(status);
    }

    /**
     * 统计企业总数
     *
     * @return 企业总数
     */
    public long countTotal() {
        log.debug("统计企业总数");
        return enterpriseRepository.count();
    }

    // ==================== 辅助方法 ====================

    /**
     * 生成随机的区块链地址
     * 格式：0x + 40位十六进制字符（共42位）
     *
     * @return 随机生成的区块链地址
     */
    private String generateRandomAddress() {
        java.security.SecureRandom random = new java.security.SecureRandom();
        byte[] bytes = new byte[20]; // 20字节 = 40位十六进制
        random.nextBytes(bytes);

        // 转换为十六进制字符串
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02x", b));
        }

        // 添加 0x 前缀，确保是42位
        return "0x" + hexString.toString();
    }

    // ==================== 企业画像和信用评分相关方法 ====================

    /**
     * 获取企业画像（聚合多维度数据）
     *
     * @param enterpriseId 企业ID
     * @return 企业画像DTO
     */
    public com.fisco.app.dto.enterprise.EnterpriseProfileDTO getEnterpriseProfile(@NonNull String enterpriseId) {
        log.debug("获取企业画像: enterpriseId={}", enterpriseId);
        Enterprise enterprise = getEnterpriseById(enterpriseId);

        com.fisco.app.dto.enterprise.EnterpriseProfileDTO profile = new com.fisco.app.dto.enterprise.EnterpriseProfileDTO();

        // 基本信息
        profile.setEnterpriseId(enterprise.getId());
        profile.setName(enterprise.getName());
        profile.setCreditCode(enterprise.getCreditCode());
        profile.setRole(enterprise.getRole().name());
        profile.setStatus(enterprise.getStatus().name());
        profile.setCreditRating(enterprise.getCreditRating());
        profile.setCreditLimit(enterprise.getCreditLimit());
        profile.setRegisteredAt(enterprise.getRegisteredAt());

        // 交易习惯
        com.fisco.app.dto.enterprise.EnterpriseProfileDTO.TransactionHabitsDTO habits =
            calculateTransactionHabits(enterprise.getAddress());
        profile.setTransactionHabits(habits);

        // 经营状况
        com.fisco.app.dto.enterprise.EnterpriseProfileDTO.OperatingStatusDTO status =
            calculateOperatingStatus(enterprise.getAddress());
        profile.setOperatingStatus(status);

        // 风险指标
        com.fisco.app.dto.enterprise.EnterpriseProfileDTO.RiskMetricsDTO metrics =
            getRiskMetrics(enterprise.getAddress());
        profile.setRiskMetrics(metrics);

        return profile;
    }

    /**
     * 获取信用评分和历史
     *
     * @param enterpriseId 企业ID
     * @return 信用评分DTO
     */
    public com.fisco.app.dto.enterprise.EnterpriseCreditScoreDTO getCreditScore(@NonNull String enterpriseId) {
        log.debug("获取企业信用评分: enterpriseId={}", enterpriseId);
        Enterprise enterprise = getEnterpriseById(enterpriseId);

        com.fisco.app.dto.enterprise.EnterpriseCreditScoreDTO dto = new com.fisco.app.dto.enterprise.EnterpriseCreditScoreDTO();
        dto.setCurrentRating(enterprise.getCreditRating());
        dto.setRatingLevel(calculateRatingLevel(enterprise.getCreditRating()));

        // 查询历史记录（最近20条）
        java.util.List<com.fisco.app.entity.enterprise.CreditRatingHistory> historyList =
            creditRatingHistoryRepository.findByEnterpriseAddressOrderByChangedAtDesc(enterprise.getAddress());

        // 转换为DTO并限制数量
        java.util.List<com.fisco.app.dto.enterprise.EnterpriseCreditScoreDTO.CreditRatingHistoryRecord> historyRecords =
            historyList.stream()
                .limit(20)
                .map(com.fisco.app.dto.enterprise.EnterpriseCreditScoreDTO.CreditRatingHistoryRecord::fromEntity)
                .collect(java.util.stream.Collectors.toList());

        dto.setHistory(historyRecords);

        // 生成趋势数据
        java.util.List<com.fisco.app.dto.enterprise.EnterpriseCreditScoreDTO.TrendData> trend =
            generateTrendData(historyList);
        dto.setTrend(trend);

        return dto;
    }

    /**
     * 计算交易习惯
     */
    private com.fisco.app.dto.enterprise.EnterpriseProfileDTO.TransactionHabitsDTO calculateTransactionHabits(String address) {
        com.fisco.app.dto.enterprise.EnterpriseProfileDTO.TransactionHabitsDTO habits =
            new com.fisco.app.dto.enterprise.EnterpriseProfileDTO.TransactionHabitsDTO();

        // 统计票据
        Long billCount = billRepository.countBillsByHolder(address);
        java.math.BigDecimal billAmount = billRepository.totalAmountByHolder(address);
        habits.setBillCount(billCount != null ? billCount : 0L);

        // 统计应收账款
        java.util.List<com.fisco.app.entity.receivable.Receivable> receivables =
            receivableRepository.findByCurrentHolder(address);
        habits.setReceivableCount((long) receivables.size());

        // 统计仓单
        java.util.List<com.fisco.app.entity.warehouse.ElectronicWarehouseReceipt> receipts =
            warehouseReceiptRepository.findByHolderAddress(address);
        habits.setWarehouseReceiptCount((long) receipts.size());

        // 计算总数
        long totalTransactions = habits.getBillCount() + habits.getReceivableCount() + habits.getWarehouseReceiptCount();
        habits.setTotalTransactions(totalTransactions);

        // 计算总金额（分）- 票据金额 + 应收账款金额
        long totalAmount = 0L;
        if (billAmount != null) {
            totalAmount += billAmount.multiply(new java.math.BigDecimal("100")).longValue(); // 转换为分
        }
        for (com.fisco.app.entity.receivable.Receivable r : receivables) {
            if (r.getAmount() != null) {
                totalAmount += r.getAmount().multiply(new java.math.BigDecimal("100")).longValue();
            }
        }
        habits.setTotalAmount(totalAmount);

        // 计算平均交易额
        if (totalTransactions > 0) {
            habits.setAverageTransactionAmount(totalAmount / totalTransactions);
        }

        // 计算月均交易频次（假设数据最早从6个月前开始）
        long monthsSinceRegistration = 6; // 默认6个月
        if (totalTransactions > 0) {
            habits.setMonthlyTransactionFrequency((double) totalTransactions / monthsSinceRegistration);
        }

        // 交易成功率（简化版：基于状态统计）
        // 这里可以根据实际业务需求计算
        habits.setTransactionSuccessRate(98.0); // 默认值

        // 最近交易时间
        java.time.LocalDateTime lastTransactionTime = findLastTransactionTime(address);
        habits.setLastTransactionTime(lastTransactionTime);

        return habits;
    }

    /**
     * 计算经营状况
     */
    private com.fisco.app.dto.enterprise.EnterpriseProfileDTO.OperatingStatusDTO calculateOperatingStatus(String address) {
        com.fisco.app.dto.enterprise.EnterpriseProfileDTO.OperatingStatusDTO status =
            new com.fisco.app.dto.enterprise.EnterpriseProfileDTO.OperatingStatusDTO();

        // 这里需要集成实际的融资、还款等服务
        // 暂时返回默认值
        status.setTotalFinancingAmount(0L);
        status.setFinancingCount(0);
        status.setRepaidAmount(0L);
        status.setPendingRepaymentAmount(0L);
        status.setOverdueCount(0);
        status.setOverdueAmount(0L);

        // 资产数量统计
        com.fisco.app.dto.enterprise.EnterpriseProfileDTO.AssetCountDTO assetCounts =
            new com.fisco.app.dto.enterprise.EnterpriseProfileDTO.AssetCountDTO();
        assetCounts.setBills(billRepository.countBillsByHolder(address));
        assetCounts.setReceivables((long) receivableRepository.findByCurrentHolder(address).size());
        assetCounts.setWarehouseReceipts((long) warehouseReceiptRepository.findByHolderAddress(address).size());
        status.setAssetCounts(assetCounts);

        return status;
    }

    /**
     * 获取风险指标
     */
    private com.fisco.app.dto.enterprise.EnterpriseProfileDTO.RiskMetricsDTO getRiskMetrics(String address) {
        com.fisco.app.dto.enterprise.EnterpriseProfileDTO.RiskMetricsDTO metrics =
            new com.fisco.app.dto.enterprise.EnterpriseProfileDTO.RiskMetricsDTO();

        // 基于信用评级计算风险等级
        try {
            Enterprise enterprise = enterpriseRepository.findByAddress(address).orElse(null);
            if (enterprise != null) {
                Integer rating = enterprise.getCreditRating();
                metrics.setRiskScore(rating);

                // 根据评级设置风险等级
                if (rating >= 90) {
                    metrics.setRiskLevel("LOW");
                } else if (rating >= 75) {
                    metrics.setRiskLevel("MEDIUM");
                } else if (rating >= 60) {
                    metrics.setRiskLevel("MEDIUM_HIGH");
                } else {
                    metrics.setRiskLevel("HIGH");
                }
            }
        } catch (Exception e) {
            log.warn("获取风险指标失败，使用默认值: address={}, error={}", address, e.getMessage());
            metrics.setRiskLevel("UNKNOWN");
            metrics.setRiskScore(60);
        }

        // 预警数量（默认为0，后续可以集成实际的风险预警服务）
        metrics.setWarningCount(0);

        return metrics;
    }

    /**
     * 查找最后交易时间
     */
    private java.time.LocalDateTime findLastTransactionTime(String address) {
        java.time.LocalDateTime lastTime = null;

        // 查找最后票据交易时间
        java.util.List<com.fisco.app.entity.bill.Bill> bills =
            billRepository.findTransferableBillsByHolder(address);
        for (com.fisco.app.entity.bill.Bill bill : bills) {
            if (lastTime == null || (bill.getCreatedAt() != null && bill.getCreatedAt().isAfter(lastTime))) {
                lastTime = bill.getCreatedAt();
            }
        }

        // 查找最后应收账款时间
        java.util.List<com.fisco.app.entity.receivable.Receivable> receivables =
            receivableRepository.findByCurrentHolder(address);
        for (com.fisco.app.entity.receivable.Receivable r : receivables) {
            if (lastTime == null || (r.getCreatedAt() != null && r.getCreatedAt().isAfter(lastTime))) {
                lastTime = r.getCreatedAt();
            }
        }

        return lastTime;
    }

    /**
     * 计算评分等级
     */
    private String calculateRatingLevel(Integer rating) {
        if (rating == null) {
            return "未知";
        }
        if (rating >= 90) {
            return "优秀";
        } else if (rating >= 75) {
            return "良好";
        } else if (rating >= 60) {
            return "一般";
        } else if (rating >= 40) {
            return "较差";
        } else {
            return "差";
        }
    }

    /**
     * 生成趋势数据
     */
    private java.util.List<com.fisco.app.dto.enterprise.EnterpriseCreditScoreDTO.TrendData> generateTrendData(
            java.util.List<com.fisco.app.entity.enterprise.CreditRatingHistory> history) {

        return history.stream()
            .limit(10) // 最近10条
            .map(h -> {
                com.fisco.app.dto.enterprise.EnterpriseCreditScoreDTO.TrendData trend =
                    new com.fisco.app.dto.enterprise.EnterpriseCreditScoreDTO.TrendData();
                trend.setDate(h.getChangedAt().toLocalDate().toString());
                trend.setRating(h.getNewRating());
                return trend;
            })
            .collect(java.util.stream.Collectors.toList());
    }
}
