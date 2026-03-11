package com.fisco.app.Common.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Token令牌响应DTO
 * 用于登录成功后返回令牌信息
 *
 * 注意：添加自定义反序列化器用于接收 snake_case 请求参数
 * 例如：access_token -> accessToken, user_id -> userId
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@JsonDeserialize(using = TokenResponseDTO.SnakeToCamelDeserializer.class)
public class TokenResponseDTO {

    /**
     * 自定义反序列化器：将 snake_case 转换为 camelCase
     */
    public static class SnakeToCamelDeserializer extends JsonDeserializer<TokenResponseDTO> {
        @Override
        public TokenResponseDTO deserialize(JsonParser p, DeserializationContext ctxt) {
            try {
                TokenResponseDTO dto = new TokenResponseDTO();
                JsonNode tree = p.readValueAsTree();

                dto.setAccessToken(getTextValue(tree, "access_token", "accessToken"));
                dto.setRefreshToken(getTextValue(tree, "refresh_token", "refreshToken"));
                dto.setExpiresIn(getLongValue(tree, "expires_in", "expiresIn"));
                dto.setTokenType(getTextValue(tree, "token_type", "tokenType"));
                dto.setScope(getTextValue(tree, "scope"));
                dto.setUserId(getLongValue(tree, "user_id", "userId"));
                dto.setEntId(getLongValue(tree, "ent_id", "entId"));

                return dto;
            } catch (Exception e) {
                throw new RuntimeException("Failed to deserialize TokenResponseDTO", e);
            }
        }

        private String getTextValue(JsonNode tree, String... fieldNames) {
            for (String name : fieldNames) {
                JsonNode node = tree.get(name);
                if (node != null && !node.isNull()) {
                    return node.asText();
                }
            }
            return null;
        }

        private Long getLongValue(JsonNode tree, String... fieldNames) {
            for (String name : fieldNames) {
                JsonNode node = tree.get(name);
                if (node != null && !node.isNull()) {
                    return node.asLong();
                }
            }
            return null;
        }
    }

    /**
     * Access Token（短期令牌，有效期2小时）
     */
    @JsonProperty("accessToken")
    private String accessToken;

    /**
     * Refresh Token（长期令牌，有效期7天）
     */
    @JsonProperty("refreshToken")
    private String refreshToken;

    /**
     * Access Token 剩余过期时间（秒）
     */
    @JsonProperty("expiresIn")
    private Long expiresIn;

    /**
     * Token 类型（Bearer）
     */
    @JsonProperty("tokenType")
    private String tokenType;

    /**
     * 授权范围
     */
    private String scope;

    /**
     * 用户ID
     */
    @JsonProperty("userId")
    private Long userId;

    /**
     * 企业ID
     */
    @JsonProperty("entId")
    private Long entId;

    // ==================== 构造函数 ====================

    /**
     * 默认构造函数
     */
    public TokenResponseDTO() {
    }

    /**
     * 全参数构造函数
     */
    public TokenResponseDTO(String accessToken, String refreshToken, Long expiresIn,
                           String tokenType, String scope, Long userId, Long entId) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.tokenType = tokenType;
        this.scope = scope;
        this.userId = userId;
        this.entId = entId;
    }

    // ==================== Getter & Setter ====================

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getEntId() {
        return entId;
    }

    public void setEntId(Long entId) {
        this.entId = entId;
    }

    /**
     * 静态工厂方法：构建令牌响应
     *
     * @param accessToken  Access Token
     * @param refreshToken Refresh Token
     * @param expiresIn    过期秒数
     * @param userId       用户ID
     * @param entId        企业ID
     * @return TokenResponseDTO实例
     */
    public static TokenResponseDTO of(String accessToken, String refreshToken,
                                     Long expiresIn, Long userId, Long entId) {
        return new TokenResponseDTO(
                accessToken,
                refreshToken,
                expiresIn,
                "Bearer",
                null,
                userId,
                entId
        );
    }
}
