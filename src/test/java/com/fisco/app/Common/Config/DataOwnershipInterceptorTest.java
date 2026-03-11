package com.fisco.app.Common.Config;

import com.fisco.app.Common.Annotation.DataOwnership;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DataOwnershipInterceptor单元测试
 */
class DataOwnershipInterceptorTest {

    private DataOwnershipInterceptor interceptor;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        interceptor = new DataOwnershipInterceptor();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    /**
     * 测试无@DataOwnership注解的方法 - 应放行
     */
    @Test
    void testNoDataOwnershipAnnotation() throws Exception {
        HandlerMethod handler = createHandlerMethod(TestController.class, "noOwnership");

        boolean result = interceptor.preHandle(request, response, handler);

        assertTrue(result, "无注解的方法应该放行");
    }

    /**
     * 测试数据归属匹配 - 应放行
     */
    @Test
    void testDataOwnershipMatch() throws Exception {
        setJwtClaims(100L, "USER", 0);  // 用户entId=100
        request.addParameter("entId", "100");  // 目标entId=100

        HandlerMethod handler = createHandlerMethod(TestController.class, "ownershipEnterprise");

        boolean result = interceptor.preHandle(request, response, handler);

        assertTrue(result, "数据归属匹配应该放行");
    }

    /**
     * 测试数据归属不匹配 - 应返回403
     */
    @Test
    void testDataOwnershipNotMatch() throws Exception {
        setJwtClaims(100L, "USER", 0);  // 用户entId=100
        request.addParameter("entId", "200");  // 目标entId=200

        HandlerMethod handler = createHandlerMethod(TestController.class, "ownershipEnterprise");

        boolean result = interceptor.preHandle(request, response, handler);

        assertFalse(result, "数据归属不匹配应该拒绝");
        assertEquals(403, response.getStatus(), "应返回403状态码");
    }

    /**
     * 测试管理员绕过 - scope=1应放行
     */
    @Test
    void testAdminBypass() throws Exception {
        setJwtClaims(100L, "ADMIN", 1);  // scope=1 管理员
        request.addParameter("entId", "200");  // 目标entId=200（不匹配）

        HandlerMethod handler = createHandlerMethod(TestController.class, "ownershipEnterprise");

        boolean result = interceptor.preHandle(request, response, handler);

        assertTrue(result, "系统管理员应该绕过数据归属校验");
    }

    /**
     * 测试adminBypass=false不允许绕过
     */
    @Test
    void testAdminBypassFalse() throws Exception {
        setJwtClaims(100L, "ADMIN", 1);  // scope=1 管理员
        request.addParameter("entId", "200");  // 不匹配

        HandlerMethod handler = createHandlerMethod(TestController.class, "ownershipStrict");

        boolean result = interceptor.preHandle(request, response, handler);

        assertFalse(result, "adminBypass=false时管理员不能绕过");
        assertEquals(403, response.getStatus(), "应返回403状态码");
    }

    /**
     * 测试无JWT Claims - 应返回403
     */
    @Test
    void testNoJwtClaims() throws Exception {
        request.removeAttribute(JwtAuthenticationFilter.ATTR_CLAIMS);
        request.addParameter("entId", "100");

        HandlerMethod handler = createHandlerMethod(TestController.class, "ownershipEnterprise");

        boolean result = interceptor.preHandle(request, response, handler);

        assertFalse(result, "无JWT Claims应该拒绝");
        assertEquals(403, response.getStatus(), "应返回403状态码");
    }

    /**
     * 测试用户无entId - 应放行
     */
    @Test
    void testUserWithoutEntId() throws Exception {
        setJwtClaims(null, "USER", 0);  // 无entId
        request.addParameter("entId", "100");

        HandlerMethod handler = createHandlerMethod(TestController.class, "ownershipEnterprise");

        boolean result = interceptor.preHandle(request, response, handler);

        assertTrue(result, "用户无entId应该放行");
    }

    /**
     * 测试参数不存在 - 应放行
     */
    @Test
    void testParameterNotFound() throws Exception {
        setJwtClaims(100L, "USER", 0);
        // 不设置entId参数

        HandlerMethod handler = createHandlerMethod(TestController.class, "ownershipEnterprise");

        boolean result = interceptor.preHandle(request, response, handler);

        assertTrue(result, "参数不存在应该放行");
    }

    /**
     * 测试自定义参数名(ownerId)
     */
    @Test
    void testCustomParamNameMatch() throws Exception {
        setJwtClaims(100L, "USER", 0);  // 用户entId=100
        request.addParameter("ownerId", "100");  // 目标ownerId=100

        HandlerMethod handler = createHandlerMethod(TestController.class, "ownershipAsset");

        boolean result = interceptor.preHandle(request, response, handler);

        assertTrue(result, "自定义参数匹配应该放行");
    }

    /**
     * 测试自定义参数名不匹配
     */
    @Test
    void testCustomParamNameNotMatch() throws Exception {
        setJwtClaims(100L, "USER", 0);  // 用户entId=100
        request.addParameter("ownerId", "200");  // 目标ownerId=200

        HandlerMethod handler = createHandlerMethod(TestController.class, "ownershipAsset");

        boolean result = interceptor.preHandle(request, response, handler);

        assertFalse(result, "自定义参数不匹配应该拒绝");
        assertEquals(403, response.getStatus(), "应返回403状态码");
    }

    /**
     * 创建HandlerMethod辅助方法
     */
    private HandlerMethod createHandlerMethod(Class<?> controllerClass, String methodName) throws NoSuchMethodException {
        Method method = controllerClass.getMethod(methodName);
        return new HandlerMethod(new TestController(), method);
    }

    /**
     * 设置模拟JWT Claims
     */
    private void setJwtClaims(Long entId, String role, Integer scope) {
        io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.claims().build();
        if (entId != null) {
            claims.put("entId", entId);
        }
        if (role != null) {
            claims.put("role", role);
        }
        if (scope != null) {
            claims.put("scope", scope);
        }
        request.setAttribute(JwtAuthenticationFilter.ATTR_CLAIMS, claims);
    }
}

/**
 * 测试用的Controller
 */
class TestController {
    // 无@DataOwnership注解
    public String noOwnership() { return "noOwnership"; }

    // @DataOwnership(paramName = "entId")
    @DataOwnership(paramName = "entId")
    public String ownershipEnterprise() { return "ownershipEnterprise"; }

    // @DataOwnership(paramName = "entId", adminBypass = false)
    @DataOwnership(paramName = "entId", adminBypass = false)
    public String ownershipStrict() { return "ownershipStrict"; }

    // @DataOwnership(paramName = "ownerId")
    @DataOwnership(paramName = "ownerId")
    public String ownershipAsset() { return "ownershipAsset"; }
}
