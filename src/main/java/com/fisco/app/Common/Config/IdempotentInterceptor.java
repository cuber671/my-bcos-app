package com.fisco.app.Common.Config;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.servlet.HandlerInterceptor;

import com.fisco.app.Common.Annotation.Idempotent;
import com.fisco.app.Common.Utils.Result;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;

/**
 * 接口幂等性拦截器
 * 基于Caffeine缓存实现接口幂等性校验
 *
 * 校验规则：
 * - 写入类接口入参必传transactionId（UUID）
 * - 通过transactionId+userId建立唯一索引
 * - transactionId有效期24小时
 * - 重复请求返回40005错误码
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Slf4j
@Component
public class IdempotentInterceptor implements HandlerInterceptor {

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

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, Object handler)
            throws Exception {

        // 包装请求，使其可重复读取body
        HttpServletRequest wrappedRequest = wrapRequest(request);

        // 如果未启用幂等性校验，直接放行
        if (!enabled) {
            return true;
        }

        // 只对HandlerMethod进行拦截（Controller方法）
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;

        // 检查方法是否有@Idempotent注解
        Idempotent idempotent = handlerMethod.getMethodAnnotation(Idempotent.class);
        if (idempotent == null) {
            idempotent = handlerMethod.getBeanType().getAnnotation(Idempotent.class);
        }

        // 如果没有幂等性注解，放行
        if (idempotent == null) {
            return true;
        }

        // 获取transactionId
        String transactionId = getTransactionId(wrappedRequest, idempotent.transactionIdParam());

        // 检查transactionId是否存在
        if (transactionId == null || transactionId.isEmpty()) {
            if (idempotent.required()) {
                log.warn("幂等性校验失败 - 缺少transactionId参数: {}", idempotent.transactionIdParam());
                sendErrorResponse(response, 40007, "缺少transactionId参数: " + idempotent.transactionIdParam());
                return false;
            } else {
                // required=false时，放行不进行幂等校验
                log.debug("幂等性校验跳过 - transactionId为空且required=false");
                return true;
            }
        }

        // 验证transactionId格式（UUID格式）
        if (!isValidUUID(transactionId)) {
            log.warn("幂等性校验失败 - transactionId格式无效: {}", transactionId);
            sendErrorResponse(response, 40007, "transactionId格式无效，请使用UUID格式");
            return false;
        }

        // 构建幂等Key
        String userId = getUserId(request);
        String idempotentKey = buildIdempotentKey(userId, transactionId);

        // 检查是否已存在
        Long existingTime = idempotentCache.getIfPresent(idempotentKey);
        if (existingTime != null) {
            log.warn("幂等性校验失败 - 重复请求: key={}, userId={}, transactionId={}",
                    idempotentKey, userId, transactionId);
            sendErrorResponse(response, 40005, idempotent.message());
            return false;
        }

        // 首次请求，放入缓存
        idempotentCache.put(idempotentKey, System.currentTimeMillis());
        log.debug("幂等性校验通过 - key={}, userId={}, transactionId={}",
                idempotentKey, userId, transactionId);

        return true;
    }

    /**
     * 从请求中获取transactionId
     * 支持JSON Body和Form表单两种方式
     */
    private String getTransactionId(HttpServletRequest request, String paramName) {
        // 1. 尝试从URL参数获取
        String value = request.getParameter(paramName);
        if (value != null && !value.isEmpty()) {
            return value;
        }

        // 2. 尝试从JSON Body获取
        try {
            String body = getRequestBody(request);
            if (body != null && !body.isEmpty()) {
                // 简单解析JSON，查找transactionId字段
                value = extractJsonField(body, paramName);
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
        if (json.charAt(valueStart) == '"') {
            valueStart++;
            int valueEnd = json.indexOf('"', valueStart);
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
        // 标准UUID格式: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
        // 简单UUID格式: 32位16进制字符串
        if (str == null) {
            return false;
        }

        // 去除连字符后检查是否为32位16进制
        String uuidStr = str.replace("-", "");
        if (uuidStr.length() != 32) {
            return false;
        }

        // 检查是否全是16进制字符
        return uuidStr.matches("[0-9a-fA-F]+");
    }

    /**
     * 构建幂等Key
     * 格式：idempotent_{userId}_{transactionId}
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
        Object userId = request.getAttribute("user_id");
        return userId != null ? userId.toString() : request.getRemoteAddr();
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
     * 包装请求，使其可重复读取body
     */
    private HttpServletRequest wrapRequest(HttpServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper) {
            return request;
        }
        CachingRequestWrapper wrapper = new CachingRequestWrapper(request);
        // 触发缓存读取
        try {
            wrapper.getInputStream();
        } catch (IOException e) {
            log.warn("请求body缓存失败: {}", e.getMessage());
        }
        return wrapper;
    }

    /**
     * 自定义请求包装器，支持多次读取body
     */
    private class CachingRequestWrapper extends ContentCachingRequestWrapper {
        public CachingRequestWrapper(HttpServletRequest request) {
            super(request);
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
