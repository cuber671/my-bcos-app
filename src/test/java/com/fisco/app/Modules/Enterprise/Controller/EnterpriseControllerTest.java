package com.fisco.app.Modules.Enterprise.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fisco.app.Common.Service.TokenService;
import com.fisco.app.Modules.Enterprise.Entity.Enterprise;
import com.fisco.app.Modules.Enterprise.Mapper.EnterpriseMapper;
import com.fisco.app.Modules.Enterprise.Service.EnterpriseService;
import com.fisco.app.Modules.User.Mapper.UserMapper;
import com.fisco.app.Modules.User.Service.UserService;
import com.fisco.app.Modules.Warehouse.Mapper.WarehouseReceiptMapper;
import com.fisco.app.Modules.Warehouse.Mapper.StockOrderMapper;
import com.fisco.app.Modules.Credit.Mapper.CreditProfileMapper;
import com.fisco.app.Modules.Logistics.Mapper.LogisticsDelegateMapper;
import com.fisco.app.Modules.Finance.Mapper.ReceivableMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * EnterpriseController测试类
 *
 * 注意：仅覆盖无需认证的接口（注册、登录）
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class EnterpriseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EnterpriseService enterpriseService;

    @MockBean
    private UserService userService;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private EnterpriseMapper enterpriseMapper;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private WarehouseReceiptMapper warehouseReceiptMapper;

    @MockBean
    private StockOrderMapper stockOrderMapper;

    @MockBean
    private CreditProfileMapper creditProfileMapper;

    @MockBean
    private LogisticsDelegateMapper logisticsDelegateMapper;

    @MockBean
    private ReceivableMapper receivableMapper;

    @Autowired
    private ObjectMapper objectMapper;

    private Enterprise testEnterprise;

    @BeforeEach
    void setUp() {
        testEnterprise = new Enterprise();
        testEnterprise.setEntId(2001L);
        testEnterprise.setUsername("testent");
        testEnterprise.setEnterpriseName("测试企业");
        testEnterprise.setOrgCode("91110000TEST001");
        testEnterprise.setStatus(1);

        Mockito.reset(enterpriseService, userService, tokenService);
    }

    // ==================== 企业注册测试 ====================

    @Test
    void testEnterpriseRegister_MissingUsername() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("password", "Test@123456");
        requestBody.put("enterpriseName", "测试企业");
        requestBody.put("orgCode", "91110000TEST001");

        mockMvc.perform(post("/api/v1/enterprise/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void testEnterpriseRegister_MissingPassword() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", "testent");
        requestBody.put("enterpriseName", "测试企业");
        requestBody.put("orgCode", "91110000TEST001");

        mockMvc.perform(post("/api/v1/enterprise/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void testEnterpriseRegister_MissingEnterpriseName() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", "testent");
        requestBody.put("password", "Test@123456");
        requestBody.put("orgCode", "91110000TEST001");

        mockMvc.perform(post("/api/v1/enterprise/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void testEnterpriseRegister_MissingOrgCode() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", "testent");
        requestBody.put("password", "Test@123456");
        requestBody.put("enterpriseName", "测试企业");

        mockMvc.perform(post("/api/v1/enterprise/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    // ==================== 企业登录测试 ====================

    @Test
    void testEnterpriseLogin_Success() throws Exception {
        when(enterpriseService.login(anyString(), anyString())).thenReturn(testEnterprise);

        Map<String, String> tokenPair = new HashMap<>();
        tokenPair.put("accessToken", "mock-access-token");
        tokenPair.put("refreshToken", "mock-refresh-token");
        when(tokenService.generateTokenPair(any(), any(), any(), any())).thenReturn(tokenPair);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", "testent");
        requestBody.put("password", "Test@123456");

        mockMvc.perform(post("/api/v1/enterprise/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").exists());

        verify(enterpriseService, times(1)).login(anyString(), anyString());
    }

    @Test
    void testEnterpriseLogin_MissingUsername() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("password", "Test@123456");

        mockMvc.perform(post("/api/v1/enterprise/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void testEnterpriseLogin_MissingPassword() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", "testent");

        mockMvc.perform(post("/api/v1/enterprise/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void testEnterpriseLogin_InvalidCredentials() throws Exception {
        when(enterpriseService.login(anyString(), anyString())).thenReturn(null);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", "wrongent");
        requestBody.put("password", "wrongpass");

        mockMvc.perform(post("/api/v1/enterprise/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    // ==================== 响应格式测试 ====================

    @Test
    void testResponseFormat_Success() throws Exception {
        when(enterpriseService.login(anyString(), anyString())).thenReturn(testEnterprise);

        Map<String, String> tokenPair = new HashMap<>();
        tokenPair.put("accessToken", "mock-access-token");
        tokenPair.put("refreshToken", "mock-refresh-token");
        when(tokenService.generateTokenPair(any(), any(), any(), any())).thenReturn(tokenPair);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", "testent");
        requestBody.put("password", "Test@123456");

        mockMvc.perform(post("/api/v1/enterprise/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.msg").exists())
                .andExpect(jsonPath("$.data").exists());
    }
}
