package com.fisco.app.aspect;

import com.fisco.app.exception.BusinessException;
import com.fisco.app.security.PermissionChecker;
import com.fisco.app.security.UserAuthentication;
import com.fisco.app.security.annotations.RequireEnterpriseAccess;
import com.fisco.app.security.annotations.RequireEnterpriseAdmin;
import com.fisco.app.security.annotations.RequireRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;


/**
 * 权限验证切面
 * 处理方法级别的权限注解验证
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    private final PermissionChecker permissionChecker;

    /**
     * 处理@RequireEnterpriseAccess注解
     */
    @Before("@annotation(requireEnterpriseAccess)")
    public void checkEnterpriseAccess(JoinPoint joinPoint, RequireEnterpriseAccess requireEnterpriseAccess) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new BusinessException("未登录或认证已过期");
        }

        // 获取方法签名和参数
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        // 查找企业ID参数
        String enterpriseId = null;
        String paramName = requireEnterpriseAccess.param();

        if (!paramName.isEmpty()) {
            // 使用指定的参数名
            for (int i = 0; i < parameters.length; i++) {
                if (parameters[i].getName().equals(paramName)) {
                    enterpriseId = (String) args[i];
                    break;
                }
            }
        } else {
            // 自动查找参数（尝试常见的参数名）
            for (int i = 0; i < parameters.length; i++) {
                String param = parameters[i].getName();
                if ((param.equals("enterpriseId") || param.equals("enterprise_id") ||
                     param.equals("targetEnterpriseId")) && args[i] instanceof String) {
                    enterpriseId = (String) args[i];
                    break;
                }
            }
        }

        if (enterpriseId == null) {
            log.warn("无法从方法参数中提取企业ID: method={}", method.getName());
            throw new BusinessException(requireEnterpriseAccess.message());
        }

        // 获取HttpServletRequest（如果有的话）
        HttpServletRequest request = extractRequest(args);

        // 执行权限检查
        try {
            permissionChecker.checkEnterprisePermission(authentication, enterpriseId, request);
            log.debug("企业访问权限验证通过: method={}, enterpriseId={}, user={}",
                     method.getName(), enterpriseId, authentication.getName());
        } catch (BusinessException e) {
            log.warn("企业访问权限验证失败: method={}, enterpriseId={}, user={}, error={}",
                    method.getName(), enterpriseId, authentication.getName(), e.getMessage());
            throw new BusinessException(requireEnterpriseAccess.message());
        }
    }

    /**
     * 处理@RequireRole注解
     */
    @Before("@annotation(requireRole)")
    public void checkRole(JoinPoint joinPoint, RequireRole requireRole) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new BusinessException("未登录或认证已过期");
        }

        if (!(authentication instanceof UserAuthentication)) {
            throw new BusinessException("无效的认证信息");
        }

        UserAuthentication userAuth = (UserAuthentication) authentication;
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 检查角色
        String[] requiredRoles = requireRole.value();
        boolean hasRole = false;

        for (String role : requiredRoles) {
            // 检查用户角色
            if (userAuth.getRole() != null && userAuth.getRole().equals(role)) {
                hasRole = true;
                break;
            }
            // 检查登录类型
            if (userAuth.getLoginType() != null && userAuth.getLoginType().equals(role)) {
                hasRole = true;
                break;
            }
            // 检查系统管理员
            if (role.equals("ADMIN") && userAuth.isSystemAdmin()) {
                hasRole = true;
                break;
            }
        }

        if (!hasRole) {
            log.warn("角色权限验证失败: method={}, user={}, userRole={}, requiredRoles={}",
                    method.getName(), userAuth.getName(), userAuth.getRole(), java.util.Arrays.toString(requiredRoles));
            throw new BusinessException(requireRole.message());
        }

        log.debug("角色权限验证通过: method={}, user={}, roles={}",
                 method.getName(), userAuth.getName(), java.util.Arrays.toString(requiredRoles));
    }

    /**
     * 处理@RequireEnterpriseAdmin注解
     */
    @Before("@annotation(requireEnterpriseAdmin)")
    public void checkEnterpriseAdmin(JoinPoint joinPoint, RequireEnterpriseAdmin requireEnterpriseAdmin) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new BusinessException("未登录或认证已过期");
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        try {
            permissionChecker.checkEnterpriseAdminPermission(authentication);
            log.debug("企业管理员权限验证通过: method={}, user={}",
                     method.getName(), authentication.getName());
        } catch (BusinessException e) {
            log.warn("企业管理员权限验证失败: method={}, user={}, error={}",
                    method.getName(), authentication.getName(), e.getMessage());
            throw new BusinessException(requireEnterpriseAdmin.message());
        }
    }

    /**
     * 从参数中提取HttpServletRequest
     */
    private HttpServletRequest extractRequest(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof HttpServletRequest) {
                return (HttpServletRequest) arg;
            }
        }
        return null;
    }
}
