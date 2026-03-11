package com.fisco.app.Common.Service.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.fisco.app.Common.Service.TokenService;
import com.fisco.app.Common.Utils.JwtUtil;

import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;

/**
 * Token服务实现类 - 双令牌策略
 * 使用Caffeine作为令牌黑名单缓存
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Slf4j
@Service
public class TokenServiceImpl implements TokenService {

    /**
     * 令牌黑名单缓存
     * Key: JTI (JWT唯一标识符)
     * Value: 过期时间戳
     * 最大容量: 10000条
     * 过期时间: 7天（与Refresh Token一致）
     */
    private static final Cache<String, Long> TOKEN_BLACKLIST = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(7, TimeUnit.DAYS)
            .build();

    @Override
    public Map<String, String> generateTokenPair(Long userId, Long entId, String role, Integer scope) {
        try {
            // 校验必填参数
            if (userId == null) {
                throw new IllegalArgumentException("User ID cannot be null");
            }

            // 调用JwtUtil生成令牌对
            Map<String, String> tokenPair = JwtUtil.createTokenPair(userId, entId, role, scope);
            log.info("生成令牌对成功，用户ID: {}, 企业ID: {}, 角色: {}", userId, entId, role);
            return tokenPair;
        } catch (Exception e) {
            log.error("生成令牌对失败: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate token pair: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, String> refreshToken(String refreshToken) {
        try {
            // 1. 解析Refresh Token
            Claims claims = JwtUtil.parseToken(refreshToken);
            if (claims == null) {
                log.warn("Refresh Token解析失败");
                return null;
            }

            // 2. 验证Token类型
            if (!JwtUtil.isRefreshToken(claims)) {
                log.warn("Token类型错误，不是Refresh Token");
                return null;
            }

            // 3. 检查是否在黑名单中
            String jti = JwtUtil.getJti(claims);
            if (jti != null && isTokenBlacklisted(jti)) {
                log.warn("Refresh Token已被吊销，JTI: {}", jti);
                return null;
            }

            // 4. 验证Token是否过期
            if (!JwtUtil.validateToken(refreshToken)) {
                log.warn("Refresh Token已过期");
                return null;
            }

            // 5. 提取用户信息生成新令牌
            Long userId = JwtUtil.getSubId(claims);
            Long entId = JwtUtil.getEntId(claims);
            String role = JwtUtil.getRole(claims);
            Integer scope = JwtUtil.getScope(claims);

            // 生成新的令牌对
            Map<String, String> newTokenPair = JwtUtil.createTokenPair(userId, entId, role, scope);
            log.info("刷新令牌成功，用户ID: {}", userId);
            return newTokenPair;
        } catch (Exception e) {
            log.error("刷新令牌失败: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public boolean validateAccessToken(String accessToken) {
        try {
            // 1. 解析Token
            Claims claims = JwtUtil.parseToken(accessToken);
            if (claims == null) {
                log.warn("Access Token解析失败");
                return false;
            }

            // 2. 验证Token类型
            if (!JwtUtil.isAccessToken(claims)) {
                log.warn("Token类型错误，不是Access Token");
                return false;
            }

            // 3. 检查是否在黑名单中
            String jti = JwtUtil.getJti(claims);
            if (jti != null && isTokenBlacklisted(jti)) {
                log.warn("Access Token已被吊销，JTI: {}", jti);
                return false;
            }

            // 4. 验证Token是否过期
            return JwtUtil.validateToken(accessToken);
        } catch (Exception e) {
            log.error("验证Access Token失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public Map<String, Object> parseAccessToken(String accessToken) {
        Map<String, Object> userInfo = new HashMap<>();
        try {
            // 解析Token
            Claims claims = JwtUtil.parseToken(accessToken);
            if (claims == null) {
                log.warn("Access Token解析失败");
                return null;
            }

            // 验证Token类型
            if (!JwtUtil.isAccessToken(claims)) {
                log.warn("Token类型错误");
                return null;
            }

            // 提取用户信息
            userInfo.put("userId", JwtUtil.getSubId(claims));
            userInfo.put("entId", JwtUtil.getEntId(claims));
            userInfo.put("role", JwtUtil.getRole(claims));
            userInfo.put("scope", JwtUtil.getScope(claims));
            userInfo.put("jti", JwtUtil.getJti(claims));

            // 计算过期剩余时间
            long expireIn = JwtUtil.getExpireIn(accessToken);
            userInfo.put("expireIn", expireIn);

            return userInfo;
        } catch (Exception e) {
            log.error("解析Access Token失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public boolean revokeToken(String token) {
        try {
            // 解析Token获取JTI
            Claims claims = JwtUtil.parseToken(token);
            if (claims == null) {
                log.warn("Token解析失败，无法吊销");
                return false;
            }

            String jti = JwtUtil.getJti(claims);
            if (jti == null) {
                log.warn("Token无JTI标识，无法吊销");
                return false;
            }

            // 计算过期时间
            long expirationTime = claims.getExpiration().getTime();

            // 加入黑名单
            log.debug("添加JTI到黑名单: {}, 过期时间: {}", jti, expirationTime);
            TOKEN_BLACKLIST.put(jti, expirationTime);
            log.info("令牌吊销成功，JTI: {}", jti);
            return true;
        } catch (Exception e) {
            log.error("吊销令牌失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isTokenBlacklisted(String jti) {
        if (jti == null) {
            return false;
        }
        Long expirationTime = TOKEN_BLACKLIST.getIfPresent(jti);
        log.debug("查询黑名单 - JTI: {}, 存储的过期时间: {}, 当前时间: {}", jti, expirationTime, System.currentTimeMillis());
        if (expirationTime == null) {
            log.debug("JTI不在黑名单中: {}", jti);
            return false;
        }
        // 如果已过期，从黑名单移除
        if (System.currentTimeMillis() > expirationTime) {
            TOKEN_BLACKLIST.invalidate(jti);
            log.debug("JTI已过期，从黑名单移除: {}", jti);
            return false;
        }
        log.debug("JTI在黑名单中: {}", jti);
        return true;
    }
}
