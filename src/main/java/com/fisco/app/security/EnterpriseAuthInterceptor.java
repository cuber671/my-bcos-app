package com.fisco.app.security;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fisco.app.entity.enterprise.Enterprise;
import com.fisco.app.service.enterprise.EnterpriseService;
import com.fisco.app.vo.Result;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 企业权限拦截器
 * 拦截带有@RequireEnterprise注解的请求，验证企业用户权限
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnterpriseAuthInterceptor implements HandlerInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final EnterpriseService enterpriseService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        // 只处理带有@RequireEnterprise注解的方法
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RequireEnterprise requireEnterprise = handlerMethod.getMethodAnnotation(RequireEnterprise.class);

        if (requireEnterprise == null) {
            // 没有注解，直接放行
            return true;
        }

        // 检查Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendErrorResponse(response, 401, "未提供认证令牌");
            return false;
        }

        // 提取并验证token
        String token = jwtTokenProvider.extractTokenFromHeader(authHeader);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            sendErrorResponse(response, 401, "令牌无效或已过期");
            return false;
        }

        // 获取企业信息：优先使用enterpriseId，其次使用address
        String enterpriseId = jwtTokenProvider.getEnterpriseIdFromToken(token);
        String address = jwtTokenProvider.getUserAddressFromToken(token);

        log.debug("EnterpriseAuthInterceptor: enterpriseId={}, address={}", enterpriseId, address);

        Enterprise enterprise;
        try {
            // 优先使用enterpriseId查询（更准确）
            if (enterpriseId != null && !enterpriseId.isEmpty()) {
                enterprise = enterpriseService.getEnterpriseById(enterpriseId);
                log.debug("Found enterprise by ID: {}", enterpriseId);
            } else if (address != null && !address.isEmpty()) {
                // 兼容旧版本：使用address查询
                enterprise = enterpriseService.getEnterprise(address);
                log.debug("Found enterprise by address: {}", address);
            } else {
                sendErrorResponse(response, 401, "令牌无效或已过期：缺少企业标识");
                return false;
            }

            // 检查企业状态（如果需要）
            if (requireEnterprise.requireActive() && enterprise.getStatus() != Enterprise.EnterpriseStatus.ACTIVE) {
                sendErrorResponse(response, 403, "企业账户未激活，请联系管理员");
                return false;
            }

            // 将企业信息存入request attribute，供Controller使用
            request.setAttribute("currentEnterprise", enterprise);
            log.debug("Enterprise authentication successful: id={}, name={}, username={}",
                     enterprise.getId(), enterprise.getName(), enterprise.getUsername());

            return true;

        } catch (Exception e) {
            log.error("企业权限验证失败: enterpriseId={}, address={}, error={}",
                     enterpriseId, address, e.getMessage());
            sendErrorResponse(response, 403, "权限验证失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 发送错误响应
     */
    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        Result<?> result = Result.error(message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
