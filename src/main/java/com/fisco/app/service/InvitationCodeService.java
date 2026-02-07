package com.fisco.app.service;

import com.fisco.app.entity.Enterprise;
import com.fisco.app.entity.InvitationCode;
import com.fisco.app.exception.BusinessException;
import com.fisco.app.repository.EnterpriseRepository;
import com.fisco.app.repository.InvitationCodeRepository;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 邀请码Service
 */
@Slf4j
@Service
@Api(tags = "邀请码服务")
@RequiredArgsConstructor
public class InvitationCodeService {

    private final InvitationCodeRepository invitationCodeRepository;
    private final EnterpriseRepository enterpriseRepository;

    /**
     * 生成邀请码
     * 业务规则：
     * 1. 一个企业只能有一个有效的邀请码
     * 2. 只有激活状态的企业才能申请邀请码
     * 3. 如果企业已有邀请码，返回现有邀请码
     */
    @Transactional
    public InvitationCode generateCode(String enterpriseId, String createdBy, Integer maxUses, Integer daysValid) {
        log.info("生成邀请码请求: enterpriseId={}, createdBy={}, maxUses={}, daysValid={}",
                 enterpriseId, createdBy, maxUses, daysValid);

        // 验证企业存在
        Enterprise enterprise = enterpriseRepository.findById(java.util.Objects.requireNonNull(enterpriseId))
                .orElseThrow(() -> new BusinessException.EnterpriseNotFoundException(String.valueOf(enterpriseId)));

        // 验证企业状态：只有激活的企业才能申请邀请码
        if (enterprise.getStatus() != Enterprise.EnterpriseStatus.ACTIVE) {
            log.warn("企业状态不允许生成邀请码: enterpriseId={}, status={}",
                     enterpriseId, enterprise.getStatus());
            throw new BusinessException("只有激活状态的企业才能申请邀请码，当前状态：" + enterprise.getStatus().name());
        }

        // 检查是否已有邀请码
        InvitationCode existingCode = invitationCodeRepository
                .findActiveCodeByEnterprise(enterpriseId, LocalDateTime.now())
                .orElse(null);

        if (existingCode != null) {
            log.info("企业已存在邀请码，返回现有邀请码: enterpriseId={}, code={}",
                     enterpriseId, existingCode.getCode());
            return existingCode;
        }

        // 生成新的邀请码
        String code = generateUniqueCode();

        // 创建邀请码对象
        InvitationCode invitationCode = new InvitationCode();
        invitationCode.setCode(code);
        invitationCode.setEnterpriseId(enterpriseId);
        invitationCode.setEnterpriseName(enterprise.getName());
        invitationCode.setCreatedBy(createdBy);
        invitationCode.setMaxUses(maxUses);
        invitationCode.setUsedCount(0);
        invitationCode.setStatus(InvitationCode.InvitationCodeStatus.ACTIVE);

        // 设置过期时间
        if (daysValid != null && daysValid > 0) {
            invitationCode.setExpiresAt(LocalDateTime.now().plusDays(daysValid));
        }

        InvitationCode saved = invitationCodeRepository.save(invitationCode);

        log.info("邀请码生成成功: code={}, enterpriseId={}, maxUses={}, expiresAt={}",
                 code, enterpriseId, maxUses, saved.getExpiresAt());

        return saved;
    }

    /**
     * 验证邀请码
     */
    public InvitationCode validateCode(String code) {
        log.info("验证邀请码: code={}", code);

        if (code == null || code.trim().isEmpty()) {
            throw new BusinessException("邀请码不能为空");
        }

        InvitationCode invitationCode = invitationCodeRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException("邀请码不存在"));

        // 检查邀请码状态
        if (invitationCode.getStatus() != InvitationCode.InvitationCodeStatus.ACTIVE) {
            throw new BusinessException("邀请码已失效");
        }

        // 检查是否过期
        if (invitationCode.isExpired()) {
            // 自动标记为过期
            invitationCode.setStatus(InvitationCode.InvitationCodeStatus.EXPIRED);
            invitationCodeRepository.save(invitationCode);
            throw new BusinessException("邀请码已过期");
        }

        // 检查是否达到使用上限
        if (invitationCode.isMaxUsesReached()) {
            throw new BusinessException("邀请码使用次数已达上限");
        }

        log.info("邀请码验证成功: code={}, enterpriseId={}", code, invitationCode.getEnterpriseId());

        return invitationCode;
    }

    /**
     * 使用邀请码
     */
    @Transactional
    public void useCode(String code) {
        log.info("使用邀请码: code={}", code);

        InvitationCode invitationCode = validateCode(code);

        // 增加使用次数
        invitationCode.incrementUsedCount();

        // 如果达到使用上限，标记为已禁用
        if (invitationCode.isMaxUsesReached()) {
            invitationCode.setStatus(InvitationCode.InvitationCodeStatus.DISABLED);
            log.info("邀请码已达使用上限，自动禁用: code={}", code);
        }

        invitationCodeRepository.save(invitationCode);

        log.info("邀请码使用成功: code={}, usedCount={}", code, invitationCode.getUsedCount());
    }

    /**
     * 禁用邀请码
     */
    @Transactional
    public void disableCode(Long codeId) {
        log.info("禁用邀请码: codeId={}", codeId);

        InvitationCode invitationCode = invitationCodeRepository.findById(java.util.Objects.requireNonNull(codeId))
                .orElseThrow(() -> new BusinessException("邀请码不存在"));

        invitationCode.setStatus(InvitationCode.InvitationCodeStatus.DISABLED);
        invitationCodeRepository.save(invitationCode);

        log.info("邀请码已禁用: codeId={}", codeId);
    }

    /**
     * 启用邀请码
     */
    @Transactional
    public void enableCode(Long codeId) {
        log.info("启用邀请码: codeId={}", codeId);

        InvitationCode invitationCode = invitationCodeRepository.findById(java.util.Objects.requireNonNull(codeId))
                .orElseThrow(() -> new BusinessException("邀请码不存在"));

        // 检查是否已过期
        if (invitationCode.isExpired()) {
            throw new BusinessException("邀请码已过期，无法启用");
        }

        invitationCode.setStatus(InvitationCode.InvitationCodeStatus.ACTIVE);
        invitationCodeRepository.save(invitationCode);

        log.info("邀请码已启用: codeId={}", codeId);
    }

    /**
     * 删除邀请码
     */
    @Transactional
    public void deleteCode(Long codeId) {
        log.info("删除邀请码: codeId={}", codeId);

        if (!invitationCodeRepository.existsById(java.util.Objects.requireNonNull(codeId))) {
            throw new BusinessException("邀请码不存在");
        }

        invitationCodeRepository.deleteById(java.util.Objects.requireNonNull(codeId));

        log.info("邀请码已删除: codeId={}", codeId);
    }

    /**
     * 获取企业的所有邀请码
     */
    public List<InvitationCode> getCodesByEnterprise(String enterpriseId) {
        return invitationCodeRepository.findByEnterpriseIdOrderByCreatedAtDesc(enterpriseId);
    }

    /**
     * 获取企业的所有有效邀请码
     */
    public List<InvitationCode> getActiveCodesByEnterprise(String enterpriseId) {
        return invitationCodeRepository.findActiveCodesByEnterprise(enterpriseId, LocalDateTime.now());
    }

    /**
     * 获取邀请码详情
     */
    public InvitationCode getCodeById(Long codeId) {
        return invitationCodeRepository.findById(java.util.Objects.requireNonNull(codeId))
                .orElseThrow(() -> new BusinessException("邀请码不存在"));
    }

    /**
     * 检查邀请码是否可用
     */
    public boolean isCodeAvailable(String code) {
        try {
            InvitationCode invitationCode = validateCode(code);
            return invitationCode != null && invitationCode.isValid();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 生成唯一邀请码
     */
    private String generateUniqueCode() {
        String code;
        do {
            // 生成8位随机码
            code = "INV" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (invitationCodeRepository.existsByCode(code));
        return code;
    }

    /**
     * 清理过期的邀请码
     */
    @Transactional
    public void cleanupExpiredCodes(int daysBefore) {
        log.info("清理过期邀请码: daysBefore={}", daysBefore);

        LocalDateTime expireBefore = LocalDateTime.now().minusDays(daysBefore);
        invitationCodeRepository.deleteExpiredCodes(expireBefore);

        log.info("过期邀请码清理完成: daysBefore={}", daysBefore);
    }
}
