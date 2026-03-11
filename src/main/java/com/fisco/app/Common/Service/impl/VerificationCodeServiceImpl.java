package com.fisco.app.Common.Service.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import com.fisco.app.Common.Service.VerificationCodeService;
import lombok.extern.slf4j.Slf4j;

/**
 * 验证码服务实现
 * 使用Caffeine本地缓存存储验证码（生产环境建议使用Redis）
 *
 * 验证码存储结构：
 * - key: entId:type
 * - value: {code, expireTime, attemptCount}
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Slf4j
@Service
public class VerificationCodeServiceImpl implements VerificationCodeService {

    /**
     * 验证码缓存
     * key: entId:type:code
     * value: 过期时间戳
     */
    private final Cache<String, Long> CODE_CACHE = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    /**
     * 发送频率限制缓存
     * key: entId:type
     * value: 上次发送时间戳
     */
    private final Cache<String, Long> RATE_LIMIT_CACHE = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .build();

    /**
     * 验证码长度
     */
    private static final int CODE_LENGTH = 6;

    /**
     * 发送间隔（秒）
     */
    private static final int SEND_INTERVAL = 60;

    /**
     * 随机数生成器
     */
    private final Random random = new Random();

    @Override
    public Map<String, Object> sendCode(String entId, String type, String target) {
        Map<String, Object> result = new HashMap<>();

        // 检查发送频率
        String rateKey = entId + ":" + type;
        Long lastSendTime = RATE_LIMIT_CACHE.getIfPresent(rateKey);
        if (lastSendTime != null) {
            long remainingTime = SEND_INTERVAL - (System.currentTimeMillis() - lastSendTime) / 1000;
            if (remainingTime > 0) {
                result.put("success", false);
                result.put("message", "发送过于频繁，请" + remainingTime + "秒后重试");
                return result;
            }
        }

        // 生成验证码
        String code = generateCode();

        // 存储验证码
        String codeKey = entId + ":" + type + ":" + code;
        CODE_CACHE.put(codeKey, System.currentTimeMillis() + 5 * 60 * 1000);

        // 更新发送时间
        RATE_LIMIT_CACHE.put(rateKey, System.currentTimeMillis());

        // TODO: 实际发送验证码（集成短信/邮箱服务）
        // 这里仅打印日志，生产环境需要集成实际发送服务
        log.info("发送验证码: 企业ID={}, 类型={}, 目标={}, 验证码={}",
                entId, type, target, code);

        result.put("success", true);
        result.put("message", "验证码已发送");
        result.put("expireSeconds", 300); // 5分钟有效

        return result;
    }

    @Override
    public boolean verifyCode(String entId, String type, String code) {
        if (entId == null || type == null || code == null) {
            log.warn("验证码校验失败: 参数为空");
            return false;
        }

        String codeKey = entId + ":" + type + ":" + code;
        Long expireTime = CODE_CACHE.getIfPresent(codeKey);

        if (expireTime == null) {
            log.warn("验证码校验失败: 验证码不存在或已过期, entId={}, type={}", entId, type);
            return false;
        }

        if (System.currentTimeMillis() > expireTime) {
            log.warn("验证码校验失败: 验证码已过期, entId={}, type={}", entId, type);
            CODE_CACHE.invalidate(codeKey);
            return false;
        }

        // 校验成功，删除验证码（一次性使用）
        CODE_CACHE.invalidate(codeKey);
        log.info("验证码校验成功: entId={}, type={}", entId, type);
        return true;
    }

    @Override
    public String createCode(String entId, String type) {
        String code = generateCode();
        String codeKey = entId + ":" + type + ":" + code;
        CODE_CACHE.put(codeKey, System.currentTimeMillis() + 5 * 60 * 1000);
        return code;
    }

    /**
     * 生成随机验证码
     *
     * @return 验证码字符串
     */
    private String generateCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }
}
