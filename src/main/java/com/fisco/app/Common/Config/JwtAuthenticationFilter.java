package com.fisco.app.Common.Config;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fisco.app.Common.Service.TokenService;
import com.fisco.app.Common.Utils.AuditContext;
import com.fisco.app.Common.Utils.JwtUtil;

import org.slf4j.MDC;

import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT认证过滤器
 * 拦截请求，验证JWT Token，提取用户信息并存入请求属性
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /**
     * 请求头中的Token前缀
     */
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * 请求头名称
     */
    private static final String AUTHORIZATION_HEADER = "Authorization";

    /**
     * 用户信息属性名
     */
    public static final String ATTR_CLAIMS = "jwt_claims";

    /**
     * 用户ID属性名
     */
    public static final String ATTR_USER_ID = "user_id";

    /**
     * 企业ID属性名
     */
    public static final String ATTR_ENT_ID = "ent_id";

    /**
     * 角色属性名
     */
    public static final String ATTR_ROLE = "role";

    /**
     * 权限范围属性名
     */
    public static final String ATTR_SCOPE = "scope";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final TokenService tokenService;

    /**
     * 构造函数注入TokenService
     */
    public JwtAuthenticationFilter(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    /**
     * 不需要拦截的URL模式
     */
    private static final String[] EXCLUDE_PATTERNS = {
            "/api/auth/**",
            "/api/v1/auth/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/webjars/**",
            "/actuator/**",
            "/api/v1/enterprise/register",
            "/api/v1/enterprise/login",
            "/api/v1/enterprise/admin/login",
            "/api/v1/user/register",
            "/api/v1/user/login",
            "/error"
    };

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            // 1. 检查是否需要跳过认证
            if (shouldSkipAuthentication(request)) {
                filterChain.doFilter(request, response);
                return;
            }

            // 2. 从请求头获取Token
            String authHeader = request.getHeader(AUTHORIZATION_HEADER);
            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                log.warn("请求未携带有效的Authorization头: {}", request.getRequestURI());
                sendUnauthorizedResponse(response, "Missing or invalid Authorization header");
                return;
            }

            // 3. 提取Token
            String token = authHeader.substring(BEARER_PREFIX.length());

            // 4. 验证Token
            Claims claims = JwtUtil.parseToken(token);
            if (claims == null) {
                log.warn("Token解析失败: {}", request.getRequestURI());
                sendUnauthorizedResponse(response, "Invalid token");
                return;
            }

            // 5. 验证Token类型（必须是Access Token）
            if (!JwtUtil.isAccessToken(claims)) {
                log.warn("非Access Token访问: {}", request.getRequestURI());
                sendUnauthorizedResponse(response, "Invalid token type, expected access token");
                return;
            }

            // 6. 验证Token是否过期
            if (!JwtUtil.validateToken(token)) {
                log.warn("Token已过期: {}", request.getRequestURI());
                sendUnauthorizedResponse(response, "Token has expired");
                return;
            }

            // 6.1 验证Token是否在黑名单中
            String jti = JwtUtil.getJti(claims);
            log.debug("黑名单检查 - JTI: {}, 黑名单中是否存在: {}", jti, tokenService.isTokenBlacklisted(jti));
            if (tokenService.isTokenBlacklisted(jti)) {
                log.warn("Token已被吊销: {}, JTI: {}", request.getRequestURI(), jti);
                sendUnauthorizedResponse(response, "Token has been revoked");
                return;
            }

            // 7. 将用户信息存入请求属性，供后续使用
            request.setAttribute(ATTR_CLAIMS, claims);
            Long userId = JwtUtil.getSubId(claims);
            request.setAttribute(ATTR_USER_ID, userId);
            request.setAttribute(ATTR_ENT_ID, JwtUtil.getEntId(claims));
            request.setAttribute(ATTR_ROLE, JwtUtil.getRole(claims));
            request.setAttribute(ATTR_SCOPE, JwtUtil.getScope(claims));

            // 8. 设置审计上下文（用于自动填充operator_id）
            AuditContext.set(
                    userId,
                    JwtUtil.getEntId(claims),
                    JwtUtil.getRole(claims),
                    JwtUtil.getScope(claims),
                    JwtUtil.getJti(claims)
            );

            // 设置Spring Security上下文
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userId, null, java.util.Collections.emptyList());
            authentication.setDetails(claims);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 设置MDC上下文，用于日志追踪
            MDC.put("userId", String.valueOf(userId));

            log.debug("JWT认证成功，用户ID: {}, 角色: {}, URI: {}",
                    userId, JwtUtil.getRole(claims), request.getRequestURI());

            // 9. 继续过滤链
            filterChain.doFilter(request, response);
        } finally {
            // 10. 请求结束后清理审计上下文，防止内存泄漏
            AuditContext.clear();
            // 清理MDC上下文
            MDC.remove("userId");
        }
    }

    /**
     * 判断是否需要跳过认证
     *
     * @param request HTTP请求
     * @return true=跳过认证
     */
    private boolean shouldSkipAuthentication(HttpServletRequest request) {
        String uri = request.getRequestURI();
        for (String pattern : EXCLUDE_PATTERNS) {
            if (matchPattern(uri, pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 简单的URL模式匹配
     *
     * @param uri     请求URI
     * @param pattern 模式（支持*通配符）
     * @return true=匹配
     */
    private boolean matchPattern(String uri, String pattern) {
        if (pattern.contains("**")) {
            // 处理Ant风格的通配符
            String prefix = pattern.replace("/**", "");
            return uri.startsWith(prefix);
        }
        return uri.equals(pattern);
    }

    /**
     * 发送未授权响应
     *
     * @param response HTTP响应
     * @param message  错误消息
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> error = new HashMap<>();
        error.put("code", 401);
        error.put("message", message);

        response.getWriter().write(objectMapper.writeValueAsString(error));
    }

    /**
     * 从请求中获取用户ID
     *
     * @param request HTTP请求
     * @return 用户ID，未登录返回null
     */
    public static Long getUserId(HttpServletRequest request) {
        Object userId = request.getAttribute(ATTR_USER_ID);
        return userId != null ? (Long) userId : null;
    }

    /**
     * 从请求中获取企业ID
     *
     * @param request HTTP请求
     * @return 企业ID，未登录返回null
     */
    public static Long getEntId(HttpServletRequest request) {
        Object entId = request.getAttribute(ATTR_ENT_ID);
        return entId != null ? (Long) entId : null;
    }

    /**
     * 从请求中获取角色
     *
     * @param request HTTP请求
     * @return 角色，未登录返回null
     */
    public static String getRole(HttpServletRequest request) {
        return (String) request.getAttribute(ATTR_ROLE);
    }

    /**
     * 从请求中获取权限范围
     *
     * @param request HTTP请求
     * @return 权限范围，未登录返回null
     */
    public static Integer getScope(HttpServletRequest request) {
        Object scope = request.getAttribute(ATTR_SCOPE);
        return scope != null ? (Integer) scope : null;
    }

    /**
     * 从请求中获取完整Claims
     *
     * @param request HTTP请求
     * @return Claims，未登录返回null
     */
    public static Claims getClaims(HttpServletRequest request) {
        return (Claims) request.getAttribute(ATTR_CLAIMS);
    }
}
