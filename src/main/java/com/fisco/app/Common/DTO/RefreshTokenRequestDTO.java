package com.fisco.app.Common.DTO;

import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 刷新令牌请求DTO
 * 用于使用Refresh Token获取新的Access Token
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
public class RefreshTokenRequestDTO {

    /**
     * Refresh Token（必填）
     */
    @NotBlank(message = "Refresh Token不能为空")
    @JsonProperty("refreshToken")
    private String refreshToken;

    /**
     * 授权类型（可选，默认refresh_token）
     */
    private String grantType;

    // ==================== 构造函数 ====================

    /**
     * 默认构造函数
     */
    public RefreshTokenRequestDTO() {
    }

    /**
     * 带参构造函数
     *
     * @param refreshToken Refresh Token
     */
    public RefreshTokenRequestDTO(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    // ==================== Getter & Setter ====================

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }

    /**
     * 验证请求参数是否有效
     *
     * @return true=有效
     */
    public boolean isValid() {
        return refreshToken != null && !refreshToken.trim().isEmpty();
    }
}
