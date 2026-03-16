package com.fisco.app.Common.Utils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

/**
 * 增强型 JWT 工具类 - 双令牌策略
 * 功能：多级权限隔离、财务职能校验、数据归属校验
 *
 * 双令牌策略说明：
 * - Access Token: 短期令牌，有效期2小时，用于业务请求
 * - Refresh Token: 长期令牌，有效期7天，用于自动续期
 *
 * 配置说明：
 * - jwt.secret: JWT签名密钥（必需，长度>=32字节）
 * - jwt.key.version: 密钥版本号
 * - jwt.key.rotation.days: 密钥轮换周期（默认90天=3个月）
 * - jwt.refresh-expiration: Refresh Token过期时间（毫秒，默认604800000=7天）
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Slf4j
@Component
public class JwtUtil {

    // ==================== 静态配置持有类 ====================

    /**
     * JWT配置持有类
     * 用于在静态方法中访问Spring配置的密钥
     */
    private static class JwtConfigHolder {
        private static String SECRET;
        private static javax.crypto.SecretKey KEY;
        private static long ACCESS_TOKEN_EXPIRATION;
        private static long REFRESH_TOKEN_EXPIRATION;

        /**
         * 初始化配置
         */
        public static void init(String secret, long accessExpiration, long refreshExpiration) {
            SECRET = secret;
            // 密钥长度必须足够（HS256需要至少32字节）
            if (SECRET == null || SECRET.length() < 32) {
                SECRET = "FiscoBcos_Platform_Secret_Key_2026";
                log.warn("JWT密钥长度不足64字节，使用默认密钥（生产环境请配置安全的密钥）");
            }
            KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
            ACCESS_TOKEN_EXPIRATION = accessExpiration;
            REFRESH_TOKEN_EXPIRATION = refreshExpiration;
            log.info("JWT工具类初始化完成，AccessToken过期时间: {}ms, RefreshToken过期时间: {}ms",
                    ACCESS_TOKEN_EXPIRATION, REFRESH_TOKEN_EXPIRATION);
        }
    }

    // ==================== 过期时间常量（供外部获取）====================

    /**
     * Access Token 过期时间（毫秒）
     */
    public static long ACCESS_TOKEN_EXPIRATION = 2 * 60 * 60 * 1000L;

    /**
     * Refresh Token 过期时间（毫秒）
     */
    public static long REFRESH_TOKEN_EXPIRATION = 7 * 24 * 60 * 60 * 1000L;

    // ==================== Claims 常量定义 ====================
    public static final String CLAIM_ENT_ID = "entId";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_SCOPE = "scope";
    /**
     * 企业角色（整数类型），用于WarehouseABACInterceptor等需要整数角色的场景
     */
    public static final String CLAIM_ENT_ROLE = "entRole";
    /**
     * Token类型标识：access 或 refresh
     */
    public static final String CLAIM_TOKEN_TYPE = "tokenType";
    /**
     * JWT唯一标识符，用于黑名单机制
     */
    public static final String CLAIM_JTI = "jti";

    // Token类型枚举
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    // 职能定义：拥有金融操作权限的角色
    private static final List<String> FINANCE_ROLES = Arrays.asList("ADMIN", "FINANCE");

    // 角色权限等级映射
    // 10: 管理员 (ADMIN)
    // 5:  财务   (FINANCE)
    // 1:  普通用户 (USER)
    private static final Map<String, Integer> ROLE_LEVEL_MAP = new HashMap<>();
    static {
        ROLE_LEVEL_MAP.put("ADMIN", 10);
        ROLE_LEVEL_MAP.put("FINANCE", 5);
        ROLE_LEVEL_MAP.put("USER", 1);
    }

    // ==================== 构造函数 ====================

    /**
     * 构造函数 - 初始化JWT配置
     *
     * @param jwtSecret JWT签名密钥（从环境变量注入）
     * @param accessExpiration Access Token过期时间
     * @param refreshExpiration Refresh Token过期时间
     */
    public JwtUtil(
            @Value("${jwt.secret:}") String jwtSecret,
            @Value("${jwt.expiration:7200000}") long accessExpiration,
            @Value("${jwt.refresh-expiration:604800000}") long refreshExpiration) {
        // 使用默认值覆盖（如果环境变量为空）
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            jwtSecret = "FiscoBcos_Platform_Secret_Key_2026";
        }
        init(jwtSecret, accessExpiration, refreshExpiration);
    }

    /**
     * 静态初始化方法 - 供测试代码调用
     *
     * @param secret JWT签名密钥
     * @param accessExpiration Access Token过期时间（毫秒）
     * @param refreshExpiration Refresh Token过期时间（毫秒）
     */
    public static void init(String secret, long accessExpiration, long refreshExpiration) {
        JwtConfigHolder.init(secret, accessExpiration, refreshExpiration);
        ACCESS_TOKEN_EXPIRATION = accessExpiration;
        REFRESH_TOKEN_EXPIRATION = refreshExpiration;
    }

    // ==================== 双令牌生成方法 ====================

    /**
     * 生成双令牌（Access Token + Refresh Token）
     *
     * @param sub   用户ID（必填）
     * @param entId 企业ID（可选，用于数据隔离）
     * @param role  角色（可选）
     * @param scope 权限范围（可选，1=系统管理员）
     * @return 包含accessToken和refreshToken的Map
     */
    public static Map<String, String> createTokenPair(Long sub, Long entId, String role, Integer scope) {
        return createTokenPair(sub, entId, role, scope, null);
    }

    /**
     * 生成双令牌对（支持企业角色）
     *
     * @param sub     用户ID
     * @param entId  企业ID
     * @param role   角色（字符串）
     * @param scope  权限范围
     * @param entRole 企业角色（整数类型），用于权限校验
     * @return 令牌对Map
     */
    public static Map<String, String> createTokenPair(Long sub, Long entId, String role, Integer scope, Integer entRole) {
        Map<String, String> tokenPair = new HashMap<>();

        // 生成唯一标识符（JTI）
        String jti = UUID.randomUUID().toString();

        // 1. 生成 Access Token（短期，2小时）
        Map<String, Object> accessClaims = new HashMap<>();
        accessClaims.put(CLAIM_ENT_ID, entId);
        accessClaims.put(CLAIM_ROLE, role);
        accessClaims.put(CLAIM_SCOPE, scope);
        accessClaims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS);
        accessClaims.put(CLAIM_JTI, jti);
        // 添加企业角色（整数类型）
        if (entRole != null) {
            accessClaims.put(CLAIM_ENT_ROLE, entRole);
        }

        String accessToken = Jwts.builder()
                .claims(accessClaims)
                .subject(sub.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + JwtConfigHolder.ACCESS_TOKEN_EXPIRATION))
                .signWith(JwtConfigHolder.KEY)
                .compact();

        // 2. 生成 Refresh Token（长期，7天）
        Map<String, Object> refreshClaims = new HashMap<>();
        refreshClaims.put(CLAIM_ENT_ID, entId);
        refreshClaims.put(CLAIM_ROLE, role);
        refreshClaims.put(CLAIM_SCOPE, scope);
        refreshClaims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH);
        refreshClaims.put(CLAIM_JTI, jti);
        // 添加企业角色（整数类型）
        if (entRole != null) {
            refreshClaims.put(CLAIM_ENT_ROLE, entRole);
        }

        String refreshToken = Jwts.builder()
                .claims(refreshClaims)
                .subject(sub.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + JwtConfigHolder.REFRESH_TOKEN_EXPIRATION))
                .signWith(JwtConfigHolder.KEY)
                .compact();

        tokenPair.put("accessToken", accessToken);
        tokenPair.put("refreshToken", refreshToken);
        tokenPair.put("expiresIn", String.valueOf(JwtConfigHolder.ACCESS_TOKEN_EXPIRATION / 1000)); // 秒

        log.info("生成双令牌成功，用户ID: {}, JTI: {}", sub, jti);
        return tokenPair;
    }

    /**
     * 生成单个 Access Token（短期）
     *
     * @param sub   用户ID
     * @param entId 企业ID
     * @param role  角色
     * @param scope 权限范围
     * @return Access Token字符串
     */
    public static String createAccessToken(Long sub, Long entId, String role, Integer scope) {
        return createAccessToken(sub, entId, role, scope, null);
    }

    /**
     * 生成单个 Access Token（短期，支持企业角色）
     *
     * @param sub     用户ID
     * @param entId   企业ID
     * @param role    角色
     * @param scope   权限范围
     * @param entRole 企业角色（整数）
     * @return Access Token字符串
     */
    public static String createAccessToken(Long sub, Long entId, String role, Integer scope, Integer entRole) {
        String jti = UUID.randomUUID().toString();

        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_ENT_ID, entId);
        claims.put(CLAIM_ROLE, role);
        claims.put(CLAIM_SCOPE, scope);
        claims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS);
        claims.put(CLAIM_JTI, jti);
        if (entRole != null) {
            claims.put(CLAIM_ENT_ROLE, entRole);
        }

        return Jwts.builder()
                .claims(claims)
                .subject(sub.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + JwtConfigHolder.ACCESS_TOKEN_EXPIRATION))
                .signWith(JwtConfigHolder.KEY)
                .compact();
    }

    /**
     * 生成单个 Refresh Token（长期）
     *
     * @param sub   用户ID
     * @param entId 企业ID
     * @param role  角色
     * @param scope 权限范围
     * @return Refresh Token字符串
     */
    public static String createRefreshToken(Long sub, Long entId, String role, Integer scope) {
        return createRefreshToken(sub, entId, role, scope, null);
    }

    /**
     * 生成单个 Refresh Token（长期，支持企业角色）
     *
     * @param sub     用户ID
     * @param entId   企业ID
     * @param role    角色
     * @param scope   权限范围
     * @param entRole 企业角色（整数）
     * @return Refresh Token字符串
     */
    public static String createRefreshToken(Long sub, Long entId, String role, Integer scope, Integer entRole) {
        String jti = UUID.randomUUID().toString();

        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_ENT_ID, entId);
        claims.put(CLAIM_ROLE, role);
        claims.put(CLAIM_SCOPE, scope);
        claims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH);
        claims.put(CLAIM_JTI, jti);
        if (entRole != null) {
            claims.put(CLAIM_ENT_ROLE, entRole);
        }

        return Jwts.builder()
                .claims(claims)
                .subject(sub.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + JwtConfigHolder.REFRESH_TOKEN_EXPIRATION))
                .signWith(JwtConfigHolder.KEY)
                .compact();
    }

    /**
     * 解析 Token
     *
     * @param token JWT令牌字符串
     * @return Claims解析结果，解析失败返回null
     */
    public static Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(JwtConfigHolder.KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.error("JWT 解析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 验证 Token 是否有效
     *
     * @param token JWT令牌字符串
     * @return true=有效, false=无效
     */
    public static boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims != null && !isTokenExpired(claims);
        } catch (Exception e) {
            log.error("JWT 验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查 Token 是否过期
     *
     * @param claims Token的Claims
     * @return true=已过期, false=未过期
     */
    private static boolean isTokenExpired(Claims claims) {
        Date expiration = claims.getExpiration();
        return expiration != null && expiration.before(new Date());
    }

    /**
     * 获取 Token 类型（access 或 refresh）
     *
     * @param claims Token的Claims
     * @return Token类型
     */
    public static String getTokenType(Claims claims) {
        return claims.get(CLAIM_TOKEN_TYPE, String.class);
    }

    /**
     * 判断是否为 Access Token
     *
     * @param claims Token的Claims
     * @return true=是Access Token
     */
    public static boolean isAccessToken(Claims claims) {
        return TOKEN_TYPE_ACCESS.equals(getTokenType(claims));
    }

    /**
     * 判断是否为 Refresh Token
     *
     * @param claims Token的Claims
     * @return true=是Refresh Token
     */
    public static boolean isRefreshToken(Claims claims) {
        return TOKEN_TYPE_REFRESH.equals(getTokenType(claims));
    }

    // ==================== 属性提取方法 ====================

    /**
     * 获取用户ID（sub）
     *
     * @param claims Token的Claims
     * @return 用户ID
     */
    public static Long getSubId(Claims claims) {
        return Long.parseLong(claims.getSubject());
    }

    /**
     * 获取企业ID
     *
     * @param claims Token的Claims
     * @return 企业ID，可能为null
     */
    public static Long getEntId(Claims claims) {
        Object entId = claims.get(CLAIM_ENT_ID);
        if (entId == null) return null;
        if (entId instanceof Integer) return ((Integer) entId).longValue();
        return (Long) entId;
    }

    /**
     * 获取角色
     *
     * @param claims Token的Claims
     * @return 角色字符串
     */
    public static String getRole(Claims claims) {
        return claims.get(CLAIM_ROLE, String.class);
    }

    /**
     * 获取权限范围
     *
     * @param claims Token的Claims
     * @return 权限范围（1=系统管理员）
     */
    public static Integer getScope(Claims claims) {
        return claims.get(CLAIM_SCOPE, Integer.class);
    }

    /**
     * 获取企业角色（整数类型）
     *
     * @param claims Token的Claims
     * @return 企业角色（整数），如果没有返回null
     */
    public static Integer getEntRole(Claims claims) {
        return claims.get(CLAIM_ENT_ROLE, Integer.class);
    }

    /**
     * 获取用户权限等级
     * 根据角色映射到权限等级：ADMIN=10, FINANCE=5, USER=1
     *
     * @param claims Token的Claims
     * @return 权限等级，如果角色未映射返回0
     */
    public static int getPermissionLevel(Claims claims) {
        String role = getRole(claims);
        if (role == null) {
            return 0;
        }
        Integer level = ROLE_LEVEL_MAP.get(role);
        return level != null ? level : 0;
    }

    /**
     * 校验用户权限等级是否满足要求
     *
     * @param claims     Token的Claims
     * @param requiredLevel 所需权限等级
     * @return true=权限足够
     */
    public static boolean hasPermissionLevel(Claims claims, int requiredLevel) {
        // 系统管理员(scope=1)拥有最高权限
        if (Integer.valueOf(1).equals(getScope(claims))) {
            return true;
        }
        int userLevel = getPermissionLevel(claims);
        return userLevel >= requiredLevel;
    }

    /**
     * 获取JWT唯一标识符（JTI）
     * 用于黑名单机制
     *
     * @param claims Token的Claims
     * @return JTI字符串
     */
    public static String getJti(Claims claims) {
        return claims.get(CLAIM_JTI, String.class);
    }

    // ==================== 权限校验核心逻辑 ====================

    /**
     * 基础校验：是否有权操作该企业的数据（数据隔离）
     *
     * @param claims      Token的Claims
     * @param targetEntId 目标企业ID
     * @return true=有权限
     */
    public static boolean canAccess(Claims claims, Long targetEntId) {
        if (claims == null) return false;
        // 系统管理员(Scope=1) 拥有全局穿透权限
        if (Integer.valueOf(1).equals(getScope(claims))) return true;
        // 企业级用户必须 ID 匹配
        Long entId = getEntId(claims);
        return entId != null && entId.equals(targetEntId);
    }

    /**
     * 职能校验：判断是否具备金融操作职能
     * 只有管理员(ADMIN)和财务(FINANCE)可以操作金融内容
     *
     * @param claims Token的Claims
     * @return true=有金融权限
     */
    public static boolean isFinanceRole(Claims claims) {
        if (claims == null) return false;
        // 系统管理员默认拥有所有职能
        if (Integer.valueOf(1).equals(getScope(claims))) return true;

        String role = getRole(claims);
        return role != null && FINANCE_ROLES.contains(role);
    }

    /**
     * 组合校验：必须是本企业人员 且 具备财务权限
     * 适用场景：签发票据、审核贷款、资金流转
     *
     * @param claims      Token的Claims
     * @param targetEntId 目标企业ID
     * @return true=可以执行金融操作
     */
    public static boolean canExecuteFinance(Claims claims, Long targetEntId) {
        return canAccess(claims, targetEntId) && isFinanceRole(claims);
    }

    /**
     * 获取令牌过期剩余时间（秒）
     *
     * @param token JWT令牌
     * @return 剩余过期秒数，-1表示已过期或解析失败
     */
    public static long getExpireIn(String token) {
        try {
            Claims claims = parseToken(token);
            if (claims == null) return -1;
            Date expiration = claims.getExpiration();
            if (expiration == null) return -1;
            return (expiration.getTime() - System.currentTimeMillis()) / 1000;
        } catch (Exception e) {
            log.error("获取令牌过期时间失败: {}", e.getMessage());
            return -1;
        }
    }
}
