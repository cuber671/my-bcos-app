package com.fisco.app.security;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * JWT认证过滤器
 * 拦截请求，验证JWT令牌，设置认证信息到SecurityContext
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        // 从请求头获取Authorization
        String authHeader = request.getHeader("Authorization");

        // 提取JWT令牌
        String token = jwtTokenProvider.extractTokenFromHeader(authHeader);

        // 验证令牌并设置认证信息
        if (token != null && jwtTokenProvider.validateToken(token)) {
            try {
                // 尝试提取完整的用户信息（增强版token）
                String username = jwtTokenProvider.getUserAddressFromToken(token);
                String enterpriseId = jwtTokenProvider.getEnterpriseIdFromToken(token);
                String role = jwtTokenProvider.getRoleFromToken(token);
                String loginType = jwtTokenProvider.getLoginTypeFromToken(token);
                String enterpriseAddress = jwtTokenProvider.getEnterpriseAddressFromToken(token);

                // 创建完整的认证对象（包含区块链地址）
                UserAuthentication authentication = new UserAuthentication(
                    username, enterpriseId, role, loginType, enterpriseAddress
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("User authenticated successfully: username={}, enterpriseId={}, role={}, loginType={}, address={}",
                         username, enterpriseId, role, loginType, enterpriseAddress);
            } catch (Exception e) {
                log.error("Failed to set authentication: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        } else {
            if (token != null) {
                log.warn("Invalid JWT token provided");
            }
        }

        // 继续过滤器链
        filterChain.doFilter(request, response);
    }
}
