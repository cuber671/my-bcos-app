package com.fisco.app.Modules.User.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fisco.app.Common.Service.TokenService;
import com.fisco.app.Modules.User.Entity.User;
import com.fisco.app.Modules.User.Mapper.UserMapper;
import com.fisco.app.Modules.User.Service.UserService;
import com.fisco.app.Modules.Warehouse.Mapper.WarehouseReceiptMapper;
import com.fisco.app.Modules.Warehouse.Mapper.StockOrderMapper;
import com.fisco.app.Modules.Enterprise.Mapper.EnterpriseMapper;
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
 * UserController测试类
 *
 * 注意：由于项目使用Spring Security + JWT认证，需要认证的接口在测试环境下配置复杂。
 * 当前测试仅覆盖无需认证的接口（注册、登录）。
 * 需要认证的接口建议通过集成测试或E2E测试覆盖。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private WarehouseReceiptMapper warehouseReceiptMapper;

    @MockBean
    private StockOrderMapper stockOrderMapper;

    @MockBean
    private EnterpriseMapper enterpriseMapper;

    @MockBean
    private CreditProfileMapper creditProfileMapper;

    @MockBean
    private LogisticsDelegateMapper logisticsDelegateMapper;

    @MockBean
    private ReceivableMapper receivableMapper;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1001L);
        testUser.setUsername("testuser");
        testUser.setRealName("测试用户");
        testUser.setEnterpriseId(2001L);
        testUser.setUserRole("OPERATOR");
        testUser.setStatus(1);

        Mockito.reset(userService);
    }

    // ==================== 用户注册测试 ====================

    @Test
    void testRegisterUser_Success() throws Exception {
        when(userService.registerUser(anyString(), anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(1001L);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", "newuser");
        requestBody.put("password", "Test@123456");
        requestBody.put("inviteCode", "INV001");
        requestBody.put("realName", "新用户");

        mockMvc.perform(post("/api/v1/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").value(1001));

        verify(userService, times(1)).registerUser(anyString(), anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void testRegisterUser_MissingUsername() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("password", "Test@123456");
        requestBody.put("inviteCode", "INV001");
        requestBody.put("realName", "新用户");

        mockMvc.perform(post("/api/v1/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("用户名不能为空"));
    }

    @Test
    void testRegisterUser_MissingPassword() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", "newuser");
        requestBody.put("inviteCode", "INV001");
        requestBody.put("realName", "新用户");

        mockMvc.perform(post("/api/v1/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("密码不能为空"));
    }

    @Test
    void testRegisterUser_MissingInviteCode() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", "newuser");
        requestBody.put("password", "Test@123456");
        requestBody.put("realName", "新用户");

        mockMvc.perform(post("/api/v1/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("邀请码不能为空"));
    }

    @Test
    void testRegisterUser_MissingRealName() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", "newuser");
        requestBody.put("password", "Test@123456");
        requestBody.put("inviteCode", "INV001");

        mockMvc.perform(post("/api/v1/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("真实姓名不能为空"));
    }

    // ==================== 用户登录测试 ====================

    @Test
    void testLogin_Success() throws Exception {
        when(userService.login(anyString(), anyString())).thenReturn(testUser);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", "testuser");
        requestBody.put("password", "Test@123456");

        mockMvc.perform(post("/api/v1/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").value(1001))
                .andExpect(jsonPath("$.data.username").value("testuser"));

        verify(userService, times(1)).login(anyString(), anyString());
    }

    @Test
    void testLogin_MissingUsername() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("password", "Test@123456");

        mockMvc.perform(post("/api/v1/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("用户名不能为空"));
    }

    @Test
    void testLogin_MissingPassword() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", "testuser");

        mockMvc.perform(post("/api/v1/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("密码不能为空"));
    }

    @Test
    void testLogin_InvalidCredentials() throws Exception {
        when(userService.login(anyString(), anyString())).thenReturn(null);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", "wronguser");
        requestBody.put("password", "wrongpass");

        mockMvc.perform(post("/api/v1/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.msg").value("用户名或密码错误"));
    }

    // ==================== 响应格式测试 ====================

    @Test
    void testResponseFormat_Success() throws Exception {
        when(userService.registerUser(anyString(), anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(1001L);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", "newuser");
        requestBody.put("password", "Test@123456");
        requestBody.put("inviteCode", "INV001");
        requestBody.put("realName", "新用户");

        mockMvc.perform(post("/api/v1/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.msg").exists())
                .andExpect(jsonPath("$.data").exists());
    }
}
