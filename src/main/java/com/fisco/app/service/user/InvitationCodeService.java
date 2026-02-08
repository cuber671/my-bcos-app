package com.fisco.app.service.user;

import com.fisco.app.entity.user.InvitationCode;
import com.fisco.app.entity.user.InvitationCode.InvitationCodeStatus;
import com.fisco.app.exception.BusinessException;
import com.fisco.app.repository.user.InvitationCodeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 邀请码服务
 * 负责邀请码校验与使用记录
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvitationCodeService {

    private final InvitationCodeRepository invitationCodeRepository;

    /**
     * 校验邀请码并返回实体
     */
    @Transactional(readOnly = true)
    public InvitationCode validateCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new BusinessException("邀请码不能为空");
        }

        InvitationCode invitationCode = invitationCodeRepository.findByCode(code)
            .orElseThrow(() -> new BusinessException("邀请码不存在"));

        if (!invitationCode.isValid()) {
            if (invitationCode.isExpired()) {
                throw new BusinessException("邀请码已过期");
            }
            if (invitationCode.isMaxUsesReached()) {
                throw new BusinessException("邀请码已用完");
            }
            if (invitationCode.getStatus() == InvitationCodeStatus.DISABLED) {
                throw new BusinessException("邀请码已禁用");
            }
            throw new BusinessException("邀请码无效");
        }

        return invitationCode;
    }

    /**
     * 使用邀请码
     */
    @Transactional
    public void useCode(String code) {
        InvitationCode invitationCode = validateCode(code);
        invitationCode.incrementUsedCount();

        if (invitationCode.isMaxUsesReached()) {
            invitationCode.setStatus(InvitationCodeStatus.EXPIRED);
        }

        invitationCodeRepository.save(invitationCode);
        log.info("邀请码使用成功: code={}, usedCount={}", code, invitationCode.getUsedCount());
    }
}
