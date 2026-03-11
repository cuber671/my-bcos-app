package com.fisco.app.Common.Config;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;

import com.fisco.app.Common.Utils.Result;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;

/**
 * 接口幂等性Filter
 * 基于Caffeine缓存实现接口幂等性校验
 *
 * 校验规则：
 * - 写入类接口入参必传transactionId（UUID）
 * - 通过transactionId+userId建立唯一索引
 * - transactionId有效期24小时
 * - 重复请求返回40005错误码
 *
 * 注意：此Filter在Spring DispatcherServlet之前执行，
 * 可以正确读取和缓存request body供后续Controller使用
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Slf4j
@Component
@Order(1) // 在Spring Security之前执行，确保可以读取body
public class IdempotentFilter implements Filter {

    /**
     * 幂等性缓存
     * key格式：idempotent_{userId}_{transactionId}
     */
    private final Cache<String, Long> idempotentCache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(24, TimeUnit.HOURS)
            .build();

    @Value("${idempotent.enabled:true}")
    private boolean enabled;

    @Value("${idempotent.expire-hours:24}")
    private int expireHours;

    /**
     * 需要进行幂等性校验的URL模式
     * 可以通过配置文件或数据库动态配置
     */
    /**
     * 需要进行幂等性校验的URL模式（可选配置）
     * 如果配置为空，则使用内置的业务端点识别逻辑
     * 示例：/api/**,/api/business/**
     */
    @Value("${idempotent.url-patterns:}")
    private String urlPatterns;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("IdempotentFilter 初始化完成, enabled={}, expireHours={}, urlPatterns={}",
                enabled, expireHours, urlPatterns);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 如果未启用幂等性校验，直接放行
        if (!enabled) {
            chain.doFilter(request, response);
            return;
        }

        // 检查URL是否需要幂等性校验
        String requestURI = httpRequest.getRequestURI();
        if (!shouldProcess(requestURI)) {
            chain.doFilter(request, response);
            return;
        }

        // 检查是否为写入类方法
        String method = httpRequest.getMethod();
        if (!isWriteMethod(method)) {
            chain.doFilter(request, response);
            return;
        }

        // 包装请求，使其可重复读取body
        CachingRequestWrapper wrappedRequest = new CachingRequestWrapper(httpRequest);

        // 获取transactionId
        String transactionId = getTransactionId(wrappedRequest);

        // 检查transactionId是否存在
        if (transactionId == null || transactionId.isEmpty()) {
            log.warn("幂等性校验失败 - 缺少transactionId参数, URI: {}", requestURI);
            sendErrorResponse(httpResponse, 40007, "缺少transactionId参数");
            return;
        }

        // 验证transactionId格式（UUID格式）
        if (!isValidUUID(transactionId)) {
            log.warn("幂等性校验失败 - transactionId格式无效: {}", transactionId);
            sendErrorResponse(httpResponse, 40007, "transactionId格式无效，请使用UUID格式");
            return;
        }

        // 构建幂等Key
        String userId = getUserId(httpRequest);
        String idempotentKey = buildIdempotentKey(userId, transactionId);

        // 检查是否已存在
        Long existingTime = idempotentCache.getIfPresent(idempotentKey);
        if (existingTime != null) {
            log.warn("幂等性校验失败 - 重复请求: key={}, userId={}, transactionId={}",
                    idempotentKey, userId, transactionId);
            sendErrorResponse(httpResponse, 40005, "重复请求，请勿重复提交");
            return;
        }

        // 首次请求，放入缓存
        idempotentCache.put(idempotentKey, System.currentTimeMillis());
        log.debug("幂等性校验通过 - key={}, userId={}, transactionId={}, URI={}",
                idempotentKey, userId, transactionId, requestURI);

        // 继续执行过滤器链
        chain.doFilter(wrappedRequest, response);
    }

    @Override
    public void destroy() {
        log.info("IdempotentFilter 销毁");
    }

    /**
     * 检查URL是否需要处理
     * 只有真正修改核心资源的写入操作才需要幂等性
     */
    private boolean shouldProcess(String requestURI) {
        // 排除不需要幂等性的接口
        if (shouldExcludeFromIdempotency(requestURI)) {
            return false;
        }

        // 只有明确配置的URL才进行幂等性校验（白名单模式）
        if (urlPatterns != null && !urlPatterns.isEmpty()) {
            String[] patterns = urlPatterns.split(",");
            for (String pattern : patterns) {
                pattern = pattern.trim();
                if (pattern.endsWith("/**")) {
                    String prefix = pattern.substring(0, pattern.length() - 2);
                    if (requestURI.startsWith(prefix)) {
                        return true;
                    }
                } else if (pattern.equals(requestURI)) {
                    return true;
                }
            }
            // 不在白名单中，不进行校验
            return false;
        }

        // 默认：只对已知需要幂等性的业务操作进行校验
        return isKnownIdempotentOperation(requestURI);
    }

    /**
     * 检查是否应排除幂等性校验
     * 纯查询、无状态变更、工具类接口不需要幂等性
     */
    private boolean shouldExcludeFromIdempotency(String requestURI) {
        // 1. 认证相关接口
        if (isAuthEndpoint(requestURI)) {
            return true;
        }

        // 2. 登出操作（无副作用）
        if (requestURI.endsWith("/logout")) {
            return true;
        }

        // 3. 查询类接口（天然幂等）
        if (containsQueryPattern(requestURI)) {
            return true;
        }

        // 4. 工具类/测试类接口（无核心资源修改）
        if (isUtilityEndpoint(requestURI)) {
            return true;
        }

        // 5. 企业注册接口（开放注册，不需要幂等校验）
        if (requestURI.contains("/enterprise/register")) {
            return true;
        }

        // 6. 用户注册接口（开放注册，不需要幂等校验）
        if (requestURI.contains("/user/register")) {
            return true;
        }

        return false;
    }

    /**
     * 检查是否为查询类模式
     */
    private boolean containsQueryPattern(String requestURI) {
        String[] queryPatterns = {"/query", "/list", "/get/", "/search", "/page", "/detail", "/info"};
        for (String pattern : queryPatterns) {
            if (requestURI.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查是否为工具类端点
     * 这类端点不修改核心资源，不需要幂等性
     */
    private boolean isUtilityEndpoint(String requestURI) {
        // 加密/解密操作
        if (requestURI.contains("/encryption") || requestURI.contains("/decrypt")) {
            return true;
        }
        // 验证类操作
        if (requestURI.contains("/validation") || requestURI.contains("/validate")) {
            return true;
        }
        // 限流测试
        if (requestURI.contains("/rate-limit")) {
            return true;
        }
        // 异步任务提交（任务可查询，不阻塞重试）
        if (requestURI.contains("/async/")) {
            return true;
        }
        // 区块链记录类（查询/记录，非核心资源修改）
        if (requestURI.contains("/blockchain/record")) {
            return true;
        }
        return false;
    }

    /**
     * 检查是否为已知需要幂等性的操作
     */
    private boolean isKnownIdempotentOperation(String requestURI) {
        // 只有明确的资源创建/修改操作才需要幂等性
        // 注意：/register 已在 shouldExcludeFromIdempotency() 中排除
        return requestURI.contains("/idempotent/create")
                || requestURI.contains("/create")
                || requestURI.contains("/submit")
                || requestURI.contains("/apply");
    }

    /**
     * 检查是否为认证端点
     * 认证端点不需要幂等性校验
     */
    private boolean isAuthEndpoint(String requestURI) {
        // 登录、刷新token、验证等认证接口不需要幂等性校验
        return requestURI.endsWith("/login")
                || requestURI.endsWith("/refresh")
                || requestURI.endsWith("/validate")
                || requestURI.contains("/auth/login")
                || requestURI.contains("/auth/refresh")
                || requestURI.contains("/auth/validate");
    }

    /**
     * 检查是否为写入类方法
     */
    private boolean isWriteMethod(String method) {
        return "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method);
    }

    /**
     * 从请求中获取transactionId
     * 支持JSON Body和Form表单两种方式
     */
    private String getTransactionId(HttpServletRequest request) {
        // 1. 尝试从URL参数获取
        String value = request.getParameter("transactionId");
        if (value != null && !value.isEmpty()) {
            return value;
        }

        // 2. 尝试从Header获取
        value = request.getHeader("X-Transaction-Id");
        if (value != null && !value.isEmpty()) {
            return value;
        }

        // 3. 尝试从JSON Body获取
        try {
            String body = getRequestBody(request);
            if (body != null && !body.isEmpty()) {
                // 简单解析JSON，查找transactionId字段
                value = extractJsonField(body, "transactionId");
                if (value != null) {
                    return value;
                }
            }
        } catch (Exception e) {
            log.debug("解析JSON Body失败: {}", e.getMessage());
        }

        return null;
    }

    /**
     * 获取请求Body内容
     */
    private String getRequestBody(HttpServletRequest request) throws IOException {
        // 从缓存中获取
        if (request instanceof CachingRequestWrapper) {
            CachingRequestWrapper wrapper = (CachingRequestWrapper) request;
            byte[] content = wrapper.getContentAsByteArray();
            if (content != null && content.length > 0) {
                return new String(content, request.getCharacterEncoding() != null
                        ? request.getCharacterEncoding() : "UTF-8");
            }
        }

        // 如果还没有缓存，读取并缓存
        StringBuilder body = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            body.append(line);
        }
        return body.toString();
    }

    /**
     * 从JSON字符串中提取字段值（简单实现）
     */
    private String extractJsonField(String json, String fieldName) {
        // 查找 "fieldName": "value" 或 "fieldName":value 格式
        String pattern = "\"" + fieldName + "\"\\s*:\\s*";
        int index = json.indexOf(pattern);
        if (index == -1) {
            return null;
        }

        int valueStart = index + pattern.length();
        if (valueStart >= json.length()) {
            return null;
        }

        // 判断是否为字符串值
        if (json.charAt(valueStart) == '\"') {
            valueStart++;
            int valueEnd = json.indexOf('\"', valueStart);
            if (valueEnd == -1) {
                return null;
            }
            return json.substring(valueStart, valueEnd);
        } else {
            // 非字符串值
            int valueEnd = valueStart;
            while (valueEnd < json.length()) {
                char c = json.charAt(valueEnd);
                if (c == ',' || c == '}' || c == ' ' || c == '\n' || c == '\r') {
                    break;
                }
                valueEnd++;
            }
            return json.substring(valueStart, valueEnd).trim();
        }
    }

    /**
     * 验证UUID格式
     */
    private boolean isValidUUID(String str) {
        if (str == null) {
            return false;
        }

        // 标准UUID格式: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
        // 简单UUID格式: 32位16进制字符串
        String uuidStr = str.replace("-", "");
        if (uuidStr.length() != 32) {
            return false;
        }

        // 检查是否全是16进制字符
        return uuidStr.matches("[0-9a-fA-F]+");
    }

    /**
     * 构建幂等Key
     */
    private String buildIdempotentKey(String userId, String transactionId) {
        StringBuilder sb = new StringBuilder();
        sb.append("idempotent_");
        if (userId != null && !userId.isEmpty()) {
            sb.append(userId).append("_");
        }
        sb.append(transactionId);
        return sb.toString();
    }

    /**
     * 获取用户ID（如果已登录）
     */
    private String getUserId(HttpServletRequest request) {
        // 尝试从header获取userId
        String userId = request.getHeader("X-User-Id");
        if (userId != null && !userId.isEmpty()) {
            return userId;
        }

        // 尝试从attribute获取
        Object userIdAttr = request.getAttribute("user_id");
        if (userIdAttr != null) {
            return userIdAttr.toString();
        }

        // 使用IP地址
        return request.getRemoteAddr();
    }

    /**
     * 发送错误响应
     */
    private void sendErrorResponse(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(code >= 400 && code < 600 ? code : 400);
        response.setContentType("application/json;charset=UTF-8");

        Result<?> result = Result.error(code, message);
        response.getWriter().write(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(result));
    }

    /**
     * 手动清理指定幂等Key（用于测试或管理）
     */
    public void invalidate(String userId, String transactionId) {
        String key = buildIdempotentKey(userId, transactionId);
        idempotentCache.invalidate(key);
        log.info("手动清理幂等Key: {}", key);
    }

    /**
     * 清理所有幂等缓存（用于测试或管理）
     */
    public void invalidateAll() {
        idempotentCache.invalidateAll();
        log.info("已清理所有幂等缓存");
    }

    /**
     * 自定义请求包装器，支持多次读取body
     * 使用Spring的ContentCachingRequestWrapper缓存body内容
     */
    private static class CachingRequestWrapper extends ContentCachingRequestWrapper {

        public CachingRequestWrapper(HttpServletRequest request) {
            super(request);
            // 触发缓存读取 - 在构造时立即缓存body
            // ContentCachingRequestWrapper会在第一次getInputStream时缓存
            try {
                getInputStream();
            } catch (IOException e) {
                // 忽略
            }
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            // 如果缓存已有内容，直接返回缓存的内容
            if (getContentAsByteArray().length > 0) {
                return new CachedServletInputStream(getContentAsByteArray());
            }
            return super.getInputStream();
        }

        @Override
        public BufferedReader getReader() throws IOException {
            // 如果缓存已有内容，直接返回
            if (getContentAsByteArray().length > 0) {
                return new BufferedReader(new InputStreamReader(
                        new ByteArrayInputStream(getContentAsByteArray()), getCharacterEncoding()));
            }
            return super.getReader();
        }
    }

    /**
     * ServletInputStream实现，用于从缓存读取
     */
    private static class CachedServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream inputStream;

        public CachedServletInputStream(byte[] cachedContent) {
            this.inputStream = new ByteArrayInputStream(cachedContent);
        }

        @Override
        public boolean isFinished() {
            return inputStream.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int read() {
            return inputStream.read();
        }
    }
}
