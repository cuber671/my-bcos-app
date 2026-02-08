package com.fisco.app.security;
import java.security.Key;
import java.util.Date;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT Token提供者
 * 负责生成和验证JWT令牌
 */
@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    private Key key;

    @PostConstruct
    public void init() {
        // 使用密钥生成安全的签名密钥
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /**
     * 生成JWT令牌（简单版，仅包含用户地址）
     *
     * @param userAddress 用户区块链地址
     * @return JWT令牌字符串
     */
    public String generateToken(String userAddress) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(userAddress)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * 生成增强的JWT令牌（包含用户名、企业ID和角色）
     *
     * @param username 用户名
     * @param enterpriseId 企业ID（可选）
     * @param role 角色（可选）
     * @param loginType 登录类型（USER, ADMIN, ENTERPRISE）
     * @return JWT令牌字符串
     */
    public String generateEnhancedToken(String username, String enterpriseId,
                                        String role, String loginType) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(username)
                .claim("enterpriseId", enterpriseId)
                .claim("role", role)
                .claim("loginType", loginType)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * 生成增强的JWT令牌（包含用户名、企业ID、角色和区块链地址）
     *
     * @param username 用户名
     * @param enterpriseId 企业ID（可选）
     * @param role 角色（可选）
     * @param loginType 登录类型（USER, ADMIN, ENTERPRISE）
     * @param enterpriseAddress 企业区块链地址（可选）
     * @return JWT令牌字符串
     */
    public String generateTokenWithAddress(String username, String enterpriseId,
                                          String role, String loginType,
                                          String enterpriseAddress) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(username)
                .claim("enterpriseId", enterpriseId)
                .claim("role", role)
                .claim("loginType", loginType)
                .claim("enterpriseAddress", enterpriseAddress)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * 从JWT令牌中获取用户地址
     *
     * @param token JWT令牌
     * @return 用户区块链地址
     */
    public String getUserAddressFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    /**
     * 从JWT令牌中获取企业ID
     *
     * @param token JWT令牌
     * @return 企业ID，如果没有则返回null
     */
    public String getEnterpriseIdFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.get("enterpriseId", String.class);
        } catch (Exception e) {
            log.debug("No enterpriseId in token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从JWT令牌中获取角色
     *
     * @param token JWT令牌
     * @return 角色，如果没有则返回null
     */
    public String getRoleFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.get("role", String.class);
        } catch (Exception e) {
            log.debug("No role in token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从JWT令牌中获取登录类型
     *
     * @param token JWT令牌
     * @return 登录类型（USER, ADMIN, ENTERPRISE），如果没有则返回null
     */
    public String getLoginTypeFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.get("loginType", String.class);
        } catch (Exception e) {
            log.debug("No loginType in token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从JWT令牌中获取企业区块链地址
     *
     * @param token JWT令牌
     * @return 企业区块链地址，如果没有则返回null
     */
    public String getEnterpriseAddressFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.get("enterpriseAddress", String.class);
        } catch (Exception e) {
            log.debug("No enterpriseAddress in token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 验证JWT令牌是否有效
     *
     * @param token JWT令牌
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (SecurityException ex) {
            log.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * 从Authorization header中提取JWT令牌
     *
     * @param authHeader Authorization header值
     * @return JWT令牌，如果没有则返回null
     */
    public String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
