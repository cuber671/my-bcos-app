package com.fisco.app.Common.Config;

import com.fisco.app.Common.Annotation.RequirePermission;
import com.fisco.app.Common.Annotation.RequireRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RoleAuthorizationInterceptor单元测试
 */
class RoleAuthorizationInterceptorTest {

    private RoleAuthorizationInterceptor interceptor;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        interceptor = new RoleAuthorizationInterceptor();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    /**
     * 测试无@RequireRole注解的方法 - 应放行
     */
    @Test
    void testNoRequireRoleAnnotation() throws Exception {
        // 使用HelloController的hello方法（无@RequireRole注解）
        HandlerMethod handler = createHandlerMethod(HelloController.class, "hello");

        boolean result = interceptor.preHandle(request, response, handler);

        assertTrue(result, "无注解的方法应该放行");
    }

    /**
     * 测试有@RequireRole注解且角色匹配
     */
    @Test
    void testRoleMatch() throws Exception {
        // 设置JWT Claims（模拟已登录的ADMIN用户）
        setJwtClaims("ADMIN", 1);

        // 使用admin方法（有@RequireRole({"ADMIN"})注解）
        HandlerMethod handler = createHandlerMethod(HelloController.class, "adminEndpoint");

        boolean result = interceptor.preHandle(request, response, handler);

        assertTrue(result, "角色匹配应该放行");
    }

    /**
     * 测试有@RequireRole注解但角色不匹配 - 应返回403
     */
    @Test
    void testRoleNotMatch() throws Exception {
        // 设置JWT Claims（模拟普通USER用户）
        setJwtClaims("USER", 0);

        // 使用admin方法（有@RequireRole({"ADMIN"})注解）
        HandlerMethod handler = createHandlerMethod(HelloController.class, "adminEndpoint");

        boolean result = interceptor.preHandle(request, response, handler);

        assertFalse(result, "角色不匹配应该拒绝");
        assertEquals(403, response.getStatus(), "应返回403状态码");
    }

    /**
     * 测试系统管理员(scope=1)绕过角色校验
     */
    @Test
    void testAdminBypass() throws Exception {
        // 设置JWT Claims（模拟scope=1的系统管理员）
        setJwtClaims("USER", 1);

        // 使用admin方法（有@RequireRole({"ADMIN"})注解，adminBypass=true）
        HandlerMethod handler = createHandlerMethod(HelloController.class, "adminEndpoint");

        boolean result = interceptor.preHandle(request, response, handler);

        assertTrue(result, "系统管理员应该绕过角色校验");
    }

    /**
     * 测试adminBypass=false时管理员不能绕过
     */
    @Test
    void testAdminBypassFalse() throws Exception {
        // 设置JWT Claims（模拟scope=1的系统管理员，但角色是ADMIN）
        setJwtClaims("ADMIN", 1);

        // 使用userOnly方法（有@RequireRole(value={"USER"}, adminBypass=false)注解）
        // 只有USER角色能访问，且adminBypass=false禁止绕过
        HandlerMethod handler = createHandlerMethod(HelloController.class, "userOnlyEndpoint");

        boolean result = interceptor.preHandle(request, response, handler);

        // ADMIN不在允许列表{"USER"}中，且adminBypass=false禁止绕过，应拒绝
        assertFalse(result, "ADMIN不在允许列表中，应拒绝");
        assertEquals(403, response.getStatus(), "应返回403状态码");
    }

    /**
     * 测试多角色匹配 - 允许列表中的任一角色
     */
    @Test
    void testMultipleRolesMatch() throws Exception {
        // 设置JWT Claims（模拟FINANCE用户）
        setJwtClaims("FINANCE", 0);

        // 使用finance方法（有@RequireRole({"ADMIN", "FINANCE"})注解）
        HandlerMethod handler = createHandlerMethod(HelloController.class, "financeEndpoint");

        boolean result = interceptor.preHandle(request, response, handler);

        assertTrue(result, "FINANCE角色在允许列表中应该放行");
    }

    /**
     * 测试JWT Claims为空
     */
    @Test
    void testNoJwtClaims() throws Exception {
        // 不设置JWT Claims
        request.removeAttribute(JwtAuthenticationFilter.ATTR_CLAIMS);

        // 使用admin方法
        HandlerMethod handler = createHandlerMethod(HelloController.class, "adminEndpoint");

        boolean result = interceptor.preHandle(request, response, handler);

        assertFalse(result, "无JWT Claims应该拒绝");
        assertEquals(403, response.getStatus(), "应返回403状态码");
    }

    /**
     * 测试role为null的情况
     */
    @Test
    void testNullRole() throws Exception {
        // 设置JWT Claims但不设置role
        io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.claims().build();
        claims.put("scope", 0);
        request.setAttribute(JwtAuthenticationFilter.ATTR_CLAIMS, claims);

        // 使用admin方法
        HandlerMethod handler = createHandlerMethod(HelloController.class, "adminEndpoint");

        boolean result = interceptor.preHandle(request, response, handler);

        assertFalse(result, "role为null应该拒绝");
        assertEquals(403, response.getStatus(), "应返回403状态码");
    }

    // ==================== 权限等级测试用例 ====================

    /**
     * 测试@RequirePermission - 权限等级匹配
     * ADMIN (level=10) 访问需要 level=5 的接口
     */
    @Test
    void testPermissionLevelMatch() throws Exception {
        // ADMIN token (level=10) 访问需要 level=5 的接口
        setJwtClaims("ADMIN", 1);
        HandlerMethod handler = createPermissionHandler(5);

        boolean result = interceptor.preHandle(request, response, handler);

        assertTrue(result, "权限等级足够应该放行");
    }

    /**
     * 测试@RequirePermission - 权限等级不足
     * USER (level=1) 访问需要 level=5 的接口
     */
    @Test
    void testPermissionLevelNotMatch() throws Exception {
        // USER token (level=1) 访问需要 level=5 的接口
        setJwtClaims("USER", 0);
        HandlerMethod handler = createPermissionHandler(5);

        boolean result = interceptor.preHandle(request, response, handler);

        assertFalse(result, "权限等级不足应该拒绝");
        assertEquals(403, response.getStatus(), "应返回403状态码");
    }

    /**
     * 测试@RequirePermission - 管理员绕过权限等级校验
     * scope=1 管理员访问需要 level=10 的接口（绕过）
     */
    @Test
    void testAdminBypassPermissionLevel() throws Exception {
        // scope=1 管理员访问需要 level=10 的接口（绕过）
        setJwtClaims("USER", 1);
        HandlerMethod handler = createPermissionHandler(10);

        boolean result = interceptor.preHandle(request, response, handler);

        assertTrue(result, "系统管理员应该绕过权限等级校验");
    }

    /**
     * 测试@RequirePermission - adminBypass=false不允许绕过
     * ADMIN (level=10) 访问需要 level=10 的接口（level=10匹配）
     */
    @Test
    void testPermissionLevelExactMatch() throws Exception {
        // ADMIN (level=10) 访问需要 level=10 的接口
        setJwtClaims("ADMIN", 1);
        HandlerMethod handler = createPermissionHandlerNoBypass(10);

        boolean result = interceptor.preHandle(request, response, handler);

        assertTrue(result, "权限等级匹配应该放行");
    }

    /**
     * 测试@RequirePermission - 权限等级不足的错误消息
     */
    @Test
    void testPermissionLevelErrorMessage() throws Exception {
        setJwtClaims("USER", 0);
        HandlerMethod handler = createPermissionHandler(10);

        interceptor.preHandle(request, response, handler);

        String responseBody = response.getContentAsString();
        assertTrue(responseBody.contains("insufficient permission level"), "应包含权限不足错误消息");
    }

    /**
     * 创建HandlerMethod辅助方法
     */
    private HandlerMethod createHandlerMethod(Class<?> controllerClass, String methodName) throws NoSuchMethodException {
        Method method = controllerClass.getMethod(methodName);
        return new HandlerMethod(new HelloController(), method);
    }

    /**
     * 创建带@RequirePermission注解的HandlerMethod (adminBypass=true)
     */
    private HandlerMethod createPermissionHandler(int level) throws NoSuchMethodException {
        Method method = PermissionTestController.class.getMethod("perm" + level);
        return new HandlerMethod(new PermissionTestController(), method);
    }

    /**
     * 创建带@RequirePermission注解的HandlerMethod (adminBypass=false)
     */
    private HandlerMethod createPermissionHandlerNoBypass(int level) throws NoSuchMethodException {
        Method method = PermissionTestController.class.getMethod("permNoBypass" + level);
        return new HandlerMethod(new PermissionTestController(), method);
    }

    /**
     * 设置模拟JWT Claims
     */
    private void setJwtClaims(String role, Integer scope) {
        io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.claims().build();
        claims.put("role", role);
        if (scope != null) {
            claims.put("scope", scope);
        }
        request.setAttribute(JwtAuthenticationFilter.ATTR_CLAIMS, claims);
    }
}

/**
 * 测试用的HelloController
 */
class HelloController {
    public String hello() { return "hello"; }
    public String publicEndpoint() { return "public"; }

    @RequireRole({"ADMIN"})
    public String adminEndpoint() { return "admin"; }

    @RequireRole({"ADMIN", "FINANCE"})
    public String financeEndpoint() { return "finance"; }

    @RequireRole(value = {"USER"}, adminBypass = false)
    public String userOnlyEndpoint() { return "user-only"; }
}

/**
 * 测试用的Permission Controller
 */
class PermissionTestController {
    // level=5 (FINANCE)
    @RequirePermission(level = 5)
    public String perm5() { return "perm5"; }

    // level=10 (ADMIN)
    @RequirePermission(level = 10)
    public String perm10() { return "perm10"; }

    // level=5, adminBypass=false
    @RequirePermission(level = 5, adminBypass = false)
    public String permNoBypass5() { return "permNoBypass5"; }

    // level=10, adminBypass=false
    @RequirePermission(level = 10, adminBypass = false)
    public String permNoBypass10() { return "permNoBypass10"; }
}
