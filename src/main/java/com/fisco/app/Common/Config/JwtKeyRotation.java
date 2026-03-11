package com.fisco.app.Common.Config;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT密钥轮换管理器
 * 支持多版本密钥，新令牌使用当前版本，验证时支持当前和上一版本
 * 定期轮换密钥，每3个月自动生成新密钥
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Slf4j
@Component
public class JwtKeyRotation implements InitializingBean {

    /**
     * 密钥版本号
     */
    @Value("${jwt.key.version:1}")
    private int currentVersion;

    /**
     * 密钥轮换周期（天）
     */
    @Value("${jwt.key.rotation.days:90}")
    private int rotationDays;

    /**
     * JWT签名密钥
     */
    @Value("${jwt.secret:}")
    private String jwtSecret;

    /**
     * 密钥版本映射：版本号 -> SecretKey
     */
    private static final Map<Integer, SecretKey> KEY_CACHE = new ConcurrentHashMap<>();

    /**
     * 上一版本密钥（用于平滑过渡）
     */
    private static SecretKey previousKey;

    /**
     * 当前密钥
     */
    private static SecretKey currentKey;

    /**
     * 当前密钥版本
     */
    private static int currentKeyVersion = 1;

    /**
     * 定时调度器
     */
    private static ScheduledExecutorService SCHEDULER;

    /**
     * 构造函数 - 初始化密钥
     */
    public JwtKeyRotation() {
    }

    /**
     * 初始化方法 - 在构造函数和字段注入后执行
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        // 初始化调度器
        SCHEDULER = Executors.newSingleThreadScheduledExecutor();

        // 初始化当前密钥
        String secret = (jwtSecret == null || jwtSecret.trim().isEmpty())
                ? "FiscoBcos_Platform_Secret_Key_2026"
                : jwtSecret;

        if (secret.length() < 32) {
            log.warn("JWT密钥长度不足32字节，使用默认密钥");
            secret = "FiscoBcos_Platform_Secret_Key_2026";
        }

        currentKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        currentKeyVersion = currentVersion;
        KEY_CACHE.put(currentKeyVersion, currentKey);

        log.info("JWT密钥初始化完成，当前版本: {}, 轮换周期: {} 天", currentKeyVersion, rotationDays);

        // 启动定时轮换任务
        scheduleKeyRotation(rotationDays);
    }

    /**
     * 定时密钥轮换
     *
     * @param days 轮换周期（天）
     */
    private void scheduleKeyRotation(int days) {
        SCHEDULER.scheduleAtFixedRate(() -> {
            try {
                rotateKey();
            } catch (Exception e) {
                log.error("密钥轮换失败: {}", e.getMessage(), e);
            }
        }, days, days, TimeUnit.DAYS);

        log.info("密钥轮换任务已启动，每 {} 天执行一次", days);
    }

    /**
     * 手动触发密钥轮换（管理员接口调用）
     *
     * @param newSecret 新密钥（可选，为空则自动生成）
     * @return 新密钥版本号
     */
    public synchronized int rotateKey(String newSecret) {
        // 保存当前密钥为上一版本
        previousKey = currentKey;

        // 生成新密钥
        String secret = (newSecret != null && !newSecret.trim().isEmpty())
                ? newSecret
                : generateNewSecret();

        if (secret.length() < 32) {
            secret = "FiscoBcos_Platform_Secret_Key_2026_" + System.currentTimeMillis();
        }

        // 创建新密钥
        currentKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        currentKeyVersion++;

        // 缓存新密钥
        KEY_CACHE.put(currentKeyVersion, currentKey);

        // 保留最近两个版本
        if (currentKeyVersion > 2) {
            KEY_CACHE.remove(currentKeyVersion - 2);
        }

        log.info("密钥轮换完成，新版本: {}, 旧版本保留用于验证", currentKeyVersion);
        return currentKeyVersion;
    }

    /**
     * 使用默认方式轮换密钥
     *
     * @return 新密钥版本号
     */
    public int rotateKey() {
        return rotateKey(null);
    }

    /**
     * 生成新密钥
     *
     * @return 新密钥字符串
     */
    private String generateNewSecret() {
        return "FiscoBcos_Platform_Key_" + System.currentTimeMillis();
    }

    /**
     * 获取当前密钥
     *
     * @return 当前SecretKey
     */
    public static SecretKey getCurrentKey() {
        return currentKey;
    }

    /**
     * 获取上一版本密钥
     *
     * @return 上一版本SecretKey
     */
    public static SecretKey getPreviousKey() {
        return previousKey;
    }

    /**
     * 获取当前密钥版本
     *
     * @return 版本号
     */
    public static int getCurrentKeyVersion() {
        return currentKeyVersion;
    }

    /**
     * 解析Token（支持多版本密钥）
     * 先尝试当前密钥，失败则尝试上一版本密钥
     *
     * @param token JWT令牌
     * @param parser JWT解析器
     * @return 解析结果
     */
    public static io.jsonwebtoken.Claims parseWithKeyRotation(String token,
            io.jsonwebtoken.JwtParserBuilder parser) {
        try {
            // 尝试当前密钥
            return parser.verifyWith(currentKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            // 尝试上一版本密钥
            if (previousKey != null) {
                try {
                    return parser.verifyWith(previousKey)
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();
                } catch (Exception ex) {
                    log.debug("上一版本密钥验证也失败");
                }
            }
            log.warn("Token验证失败，所有密钥版本均无效");
            return null;
        }
    }

    /**
     * 获取密钥信息（用于监控）
     *
     * @return 密钥状态信息
     */
    public Map<String, Object> getKeyStatus() {
        Map<String, Object> status = new ConcurrentHashMap<>();
        status.put("currentVersion", currentKeyVersion);
        status.put("previousKeyExists", previousKey != null);
        status.put("cachedVersions", KEY_CACHE.keySet());
        status.put("rotationDays", rotationDays);
        return status;
    }
}
