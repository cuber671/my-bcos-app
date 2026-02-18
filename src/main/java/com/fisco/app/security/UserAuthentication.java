package com.fisco.app.security;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import lombok.Getter;

/**
 * 用户认证信息
 * 存储认证用户的完整信息，包括用户名、企业ID、角色、区块链地址等
 */
@Getter
public class UserAuthentication implements Authentication {

    private final String username;           // 用户名
    private final String enterpriseId;       // 企业ID（可为null）
    private final String role;               // 角色（可为null）
    private final String loginType;          // 登录类型（USER, ADMIN, ENTERPRISE）
    private final String enterpriseAddress;  // 企业区块链地址（可为null）
    private final boolean authenticated;
    private final Object principal;
    private final Collection<GrantedAuthority> authorities;

    /**
     * 简单构造函数（向后兼容）
     */
    public UserAuthentication(String userAddress) {
        this(userAddress, null, null, null, null);
    }

    /**
     * 完整构造函数（包含企业ID和角色）
     */
    public UserAuthentication(String username, String enterpriseId,
                             String role, String loginType) {
        this(username, enterpriseId, role, loginType, null);
    }

    /**
     * 完整构造函数（包含企业ID、角色和区块链地址）
     */
    public UserAuthentication(String username, String enterpriseId,
                             String role, String loginType, String enterpriseAddress) {
        this.username = username;
        this.enterpriseId = enterpriseId;
        this.role = role;
        this.loginType = loginType;
        this.enterpriseAddress = enterpriseAddress;
        this.authenticated = true;
        this.principal = username;
        this.authorities = buildAuthorities(role, loginType);
    }

    /**
     * 根据角色构建权限集合
     */
    private Collection<GrantedAuthority> buildAuthorities(String role, String loginType) {
        Set<GrantedAuthority> auths = new HashSet<>();

        // 添加登录类型作为角色
        if (loginType != null) {
            auths.add(new SimpleGrantedAuthority("ROLE_" + loginType));
        }

        // 添加具体角色
        if (role != null && !role.isEmpty()) {
            auths.add(new SimpleGrantedAuthority("ROLE_" + role));
        }

        return auths;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

    @Override
    public Object getCredentials() {
        // JWT令牌已验证，这里返回null
        return null;
    }

    @Override
    public Object getDetails() {
        // 可以在这里添加额外的详细信息
        return this;
    }

    @Override
    public Object getPrincipal() {
        return this.principal;
    }

    @Override
    public boolean isAuthenticated() {
        return this.authenticated;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        // 不允许修改认证状态
        throw new IllegalArgumentException("Cannot set authenticated status");
    }

    @Override
    public String getName() {
        return this.username;
    }

    /**
     * 检查是否有企业管理员权限
     * 企业登录（loginType="ENTERPRISE"）或用户角色为ENTERPRISE_ADMIN时拥有此权限
     */
    public boolean isEnterpriseAdmin() {
        return "ENTERPRISE_ADMIN".equals(this.role) ||
               "ADMIN".equals(this.loginType) ||
               "ENTERPRISE".equals(this.loginType);  // 企业登录拥有企业管理员权限
    }

    /**
     * 检查是否有系统管理员权限
     */
    public boolean isSystemAdmin() {
        return "SUPER_ADMIN".equals(this.role) || "ADMIN".equals(this.loginType);
    }

    /**
     * 检查是否属于指定企业
     */
    public boolean belongsToEnterprise(String enterpriseId) {
        return enterpriseId != null && enterpriseId.equals(this.enterpriseId);
    }

    /**
     * 检查是否为仓储企业
     */
    public boolean isWarehouseProvider() {
        return "WAREHOUSE_PROVIDER".equals(this.role);
    }

    /**
     * 检查用户是否是区块链地址的持有者
     * 用于仓单操作权限验证（如背书、冻结、拆分等）
     *
     * @param address 区块链地址
     * @return 如果用户的区块链地址与给定地址相同（忽略大小写），返回true
     */
    public boolean isHolder(String address) {
        return this.enterpriseAddress != null &&
               address != null &&
               this.enterpriseAddress.equalsIgnoreCase(address);
    }

    /**
     * 获取用户ID
     * 优先返回企业ID（对于企业用户），否则返回用户名
     *
     * @return 用户ID（企业ID或用户名）
     */
    public String getUserId() {
        // 优先返回企业ID，因为在供应链金融系统中，企业是主要用户类型
        if (this.enterpriseId != null && !this.enterpriseId.isEmpty()) {
            return this.enterpriseId;
        }
        // 如果没有企业ID，返回用户名
        return this.username;
    }
}
