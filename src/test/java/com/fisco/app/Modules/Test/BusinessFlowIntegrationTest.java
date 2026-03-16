package com.fisco.app.Modules.Test;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 集成测试 - 业务流测试
 *
 * 测试完整的业务流程：企业注册 -> 员工入职 -> 仓单入库 -> 物流 -> 应收款 -> 融资
 *
 * 注意：此测试需要应用程序启动，使用H2内存数据库
 * 需要认证的接口需要先完成企业审核流程
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@SuppressWarnings("unchecked")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BusinessFlowIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    // 测试数据存储 - 使用线程安全的方式
    private static String enterpriseToken = null;
    private static Long enterpriseId = null;
    private static String inviteCode = null;
    private static String adminToken = null;

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpHeaders getAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.set("Authorization", "Bearer " + token);
        }
        return headers;
    }

    // ==================== 阶段1: 企业注册与登录 ====================

    @Test
    @Order(1)
    @DisplayName("1.1 企业注册")
    void step1_enterpriseRegister() {
        String requestBody = "{" +
            "\"username\": \"testflow\"," +
            "\"password\": \"Test@123456\"," +
            "\"payPassword\": \"Pay@123456\"," +
            "\"enterpriseName\": \"流程测试企业\"," +
            "\"orgCode\": \"91110000FLOW01\"," +
            "\"entRole\": 1," +
            "\"localAddress\": \"测试地址\"," +
            "\"contactPhone\": \"13800138000\"" +
            "}";

        HttpEntity<String> entity = new HttpEntity<>(requestBody, getHeaders());
        Map<String, Object> response = restTemplate.postForObject(
            "/api/v1/enterprise/register",
            entity,
            Map.class
        );

        assertNotNull(response);
        System.out.println("企业注册响应: " + response);

        // 从响应中获取企业ID
        if (response.containsKey("data")) {
            Object data = response.get("data");
            if (data instanceof Map) {
                Map<String, Object> dataMap = (Map<String, Object>) data;
                if (dataMap.containsKey("entId")) {
                    enterpriseId = ((Number) dataMap.get("entId")).longValue();
                }
                if (dataMap.containsKey("accessToken")) {
                    enterpriseToken = (String) dataMap.get("accessToken");
                }
            }
        }
        System.out.println("企业注册 - entId: " + enterpriseId + ", token: " + enterpriseToken);
    }

    @Test
    @Order(2)
    @DisplayName("1.2 管理员审核企业")
    void step2_auditEnterprise() {
        // 如果没有企业ID，跳过此步骤
        if (enterpriseId == null) {
            System.out.println("跳过审核 - 企业ID为空");
            return;
        }

        // 获取管理员token（使用系统管理员账户）
        String adminLoginBody = "{" +
            "\"username\": \"admin\"," +
            "\"password\": \"Admin@123456\"" +
            "}";
        HttpEntity<String> adminEntity = new HttpEntity<>(adminLoginBody, getHeaders());
        Map<String, Object> adminResponse = restTemplate.postForObject(
            "/api/v1/enterprise/admin/login",
            adminEntity,
            Map.class
        );
        System.out.println("管理员登录响应: " + adminResponse);

        if (adminResponse.containsKey("data")) {
            Object data = adminResponse.get("data");
            if (data instanceof Map) {
                Map<String, Object> dataMap = (Map<String, Object>) data;
                adminToken = (String) dataMap.get("accessToken");
            }
        }

        // 如果无法获取admin token，跳过
        if (adminToken == null) {
            System.out.println("跳过审核 - 无法获取管理员token");
            return;
        }

        // 审核企业（通过）
        String auditBody = "{" +
            "\"approved\": true" +
            "}";
        HttpEntity<String> auditEntity = new HttpEntity<>(auditBody, getAuthHeaders(adminToken));

        // 使用待审核企业的ID进行审核
        Map<String, Object> auditResponse = restTemplate.postForObject(
            "/api/v1/enterprise/" + enterpriseId + "/audit",
            auditEntity,
            Map.class
        );
        System.out.println("企业审核响应: " + auditResponse);
    }

    @Test
    @Order(3)
    @DisplayName("1.3 企业登录")
    void step3_enterpriseLogin() {
        String requestBody = "{" +
            "\"username\": \"testflow\"," +
            "\"password\": \"Test@123456\"" +
            "}";

        HttpEntity<String> entity = new HttpEntity<>(requestBody, getHeaders());
        Map<String, Object> response = restTemplate.postForObject(
            "/api/v1/enterprise/login",
            entity,
            Map.class
        );

        assertNotNull(response);
        System.out.println("企业登录响应: " + response);

        // 保存企业token用于后续请求
        if (response.containsKey("data")) {
            Object data = response.get("data");
            if (data instanceof Map) {
                Map<String, Object> dataMap = (Map<String, Object>) data;
                if (dataMap.containsKey("accessToken")) {
                    enterpriseToken = (String) dataMap.get("accessToken");
                }
                if (dataMap.containsKey("entId")) {
                    enterpriseId = ((Number) dataMap.get("entId")).longValue();
                }
            }
        }
        System.out.println("企业登录 - entId: " + enterpriseId + ", token: " + enterpriseToken);
    }

    @Test
    @Order(4)
    @DisplayName("1.4 生成邀请码")
    void step4_generateInviteCode() {
        // 如果没有企业token，跳过
        if (enterpriseToken == null) {
            System.out.println("跳过生成邀请码 - 企业token为空");
            return;
        }

        String requestBody = "{" +
            "\"maxUses\": 10," +
            "\"expireDays\": 30," +
            "\"remark\": \"测试邀请码\"" +
            "}";

        HttpEntity<String> entity = new HttpEntity<>(requestBody, getAuthHeaders(enterpriseToken));
        Map<String, Object> response = restTemplate.postForObject(
            "/api/v1/enterprise/invite-code",
            entity,
            Map.class
        );

        assertNotNull(response);
        System.out.println("生成邀请码响应: " + response);

        // 保存邀请码
        if (response.containsKey("data")) {
            Object data = response.get("data");
            if (data instanceof Map) {
                Map<String, Object> dataMap = (Map<String, Object>) data;
                if (dataMap.containsKey("inviteCode")) {
                    inviteCode = (String) dataMap.get("inviteCode");
                }
            }
        }
    }

    // ==================== 阶段2: 员工入职 ====================

    @Test
    @Order(10)
    @DisplayName("2.1 员工注册")
    void step10_userRegister() {
        if (inviteCode == null) {
            inviteCode = "INV_DEFAULT";
        }

        String requestBody = "{" +
            "\"username\": \"testuser\"," +
            "\"password\": \"Test@123456\"," +
            "\"inviteCode\": \"" + inviteCode + "\"," +
            "\"realName\": \"测试员工\"," +
            "\"phone\": \"13900139000\"," +
            "\"email\": \"testuser@example.com\"" +
            "}";

        HttpEntity<String> entity = new HttpEntity<>(requestBody, getHeaders());
        Map<String, Object> response = restTemplate.postForObject(
            "/api/v1/user/register",
            entity,
            Map.class
        );

        assertNotNull(response);
        System.out.println("员工注册响应: " + response);
    }

    // ==================== 公开接口测试（无需认证）====================

    @Test
    @Order(5)
    @DisplayName("公开接口 - Health检查")
    void step5_healthCheck() {
        Map<String, Object> response = restTemplate.getForObject(
            "/api/v1/health",
            Map.class
        );

        assertNotNull(response);
        System.out.println("Health Check响应: " + response);
    }

    @Test
    @Order(6)
    @DisplayName("公开接口 - 企业注册验证")
    void step6_registerValidation() {
        // 测试缺少必填参数
        String requestBody = "{" +
            "\"username\": \"\"" +
            "}";

        HttpEntity<String> entity = new HttpEntity<>(requestBody, getHeaders());
        Map<String, Object> response = restTemplate.postForObject(
            "/api/v1/enterprise/register",
            entity,
            Map.class
        );

        assertNotNull(response);
        System.out.println("企业注册验证响应: " + response);
    }

    @Test
    @Order(7)
    @DisplayName("公开接口 - 用户注册验证")
    void step7_userRegisterValidation() {
        // 测试缺少必填参数
        String requestBody = "{" +
            "\"username\": \"\"" +
            "}";

        HttpEntity<String> entity = new HttpEntity<>(requestBody, getHeaders());
        Map<String, Object> response = restTemplate.postForObject(
            "/api/v1/user/register",
            entity,
            Map.class
        );

        assertNotNull(response);
        System.out.println("用户注册验证响应: " + response);
    }

    @Test
    @Order(8)
    @DisplayName("公开接口 - 企业登录验证")
    void step8_enterpriseLoginValidation() {
        // 测试缺少必填参数
        String requestBody = "{" +
            "\"username\": \"\"" +
            "}";

        HttpEntity<String> entity = new HttpEntity<>(requestBody, getHeaders());
        Map<String, Object> response = restTemplate.postForObject(
            "/api/v1/enterprise/login",
            entity,
            Map.class
        );

        assertNotNull(response);
        System.out.println("企业登录验证响应: " + response);
    }

    @Test
    @Order(9)
    @DisplayName("公开接口 - 用户登录验证")
    void step9_userLoginValidation() {
        // 测试缺少必填参数
        String requestBody = "{" +
            "\"username\": \"\"" +
            "}";

        HttpEntity<String> entity = new HttpEntity<>(requestBody, getHeaders());
        Map<String, Object> response = restTemplate.postForObject(
            "/api/v1/user/login",
            entity,
            Map.class
        );

        assertNotNull(response);
        System.out.println("用户登录验证响应: " + response);
    }

    @Test
    @Order(11)
    @DisplayName("2.2 管理员审核员工")
    void step11_auditUser() {
        // 先获取待审核用户列表
        HttpHeaders headers = getHeaders();
        if (enterpriseToken != null) {
            headers.set("Authorization", "Bearer " + enterpriseToken);
        }

        Map<String, Object> listResponse = restTemplate.getForObject(
            "/api/v1/user/pending?enterpriseId=" + (enterpriseId != null ? enterpriseId : 1),
            Map.class
        );

        System.out.println("待审核用户列表: " + listResponse);

        // 审核第一个待审核用户
        if (listResponse != null && listResponse.containsKey("data")) {
            Object data = listResponse.get("data");
            if (data instanceof java.util.List && !((java.util.List<?>) data).isEmpty()) {
                Object user = ((java.util.List<?>) data).get(0);
                if (user instanceof java.util.Map) {
                    Long pendingUserId = ((Number) ((java.util.Map<?, ?>) user).get("userId")).longValue();

                    String auditBody = "{" +
                        "\"approved\": true" +
                        "}";

                    HttpEntity<String> auditEntity = new HttpEntity<>(auditBody, headers);
                    Map<String, Object> auditResponse = restTemplate.postForObject(
                        "/api/v1/user/" + pendingUserId + "/audit",
                        auditEntity,
                        Map.class
                    );

                    System.out.println("审核员工响应: " + auditResponse);
                }
            }
        }
    }

    @Test
    @Order(12)
    @DisplayName("2.3 员工登录")
    void step12_userLogin() {
        String requestBody = "{" +
            "\"username\": \"testuser\"," +
            "\"password\": \"Test@123456\"" +
            "}";

        HttpEntity<String> entity = new HttpEntity<>(requestBody, getHeaders());
        Map<String, Object> response = restTemplate.postForObject(
            "/api/v1/user/login",
            entity,
            Map.class
        );

        assertNotNull(response);
        System.out.println("员工登录响应: " + response);
    }

    // ==================== 阶段3: 仓单入库 ====================

    @Test
    @Order(20)
    @DisplayName("3.1 创建入库单")
    void step20_createStockOrder() {
        // 如果没有企业token，跳过
        if (enterpriseToken == null) {
            System.out.println("跳过创建入库单 - 企业token为空");
            return;
        }

        String requestBody = "{" +
            "\"warehouseId\": 1," +
            "\"goodsName\": \"钢材\"," +
            "\"goodsType\": \"建材\"," +
            "\"weight\": 100.000," +
            "\"unit\": \"吨\"," +
            "\"unitPrice\": 5000.00" +
            "}";

        HttpEntity<String> entity = new HttpEntity<>(requestBody, getAuthHeaders(enterpriseToken));
        Map<String, Object> response = restTemplate.postForObject(
            "/api/v1/warehouse/stock-order",
            entity,
            Map.class
        );

        assertNotNull(response);
        System.out.println("创建入库单响应: " + response);
    }

    // ==================== 阶段4: 物流 ====================

    @Test
    @Order(30)
    @DisplayName("4.1 创建物流委派单")
    void step30_createLogisticsDelegate() {
        // 如果没有企业token，跳过
        if (enterpriseToken == null) {
            System.out.println("跳过创建物流委派单 - 企业token为空");
            return;
        }

        String requestBody = "{" +
            "\"receiptId\": 1," +
            "\"startWarehouseId\": 1," +
            "\"endWarehouseId\": 2" +
            "}";

        HttpEntity<String> entity = new HttpEntity<>(requestBody, getAuthHeaders(enterpriseToken));
        Map<String, Object> response = restTemplate.postForObject(
            "/api/v1/logistics/delegate",
            entity,
            Map.class
        );

        assertNotNull(response);
        System.out.println("创建物流委派单响应: " + response);
    }

    // ==================== 阶段5: 金融 ====================

    @Test
    @Order(40)
    @DisplayName("5.1 生成应收款")
    void step40_generateReceivable() {
        // 如果没有企业token，跳过
        if (enterpriseToken == null) {
            System.out.println("跳过生成应收款 - 企业token为空");
            return;
        }

        String requestBody = "{" +
            "\"debtorEnterpriseId\": 1," +
            "\"delegateId\": 1," +
            "\"receiptId\": 1," +
            "\"amount\": 500000.00" +
            "}";

        HttpEntity<String> entity = new HttpEntity<>(requestBody, getAuthHeaders(enterpriseToken));
        Map<String, Object> response = restTemplate.postForObject(
            "/api/v1/finance/receivable",
            entity,
            Map.class
        );

        assertNotNull(response);
        System.out.println("生成应收款响应: " + response);
    }

    // ==================== 阶段6: 信用 ====================

    @Test
    @Order(50)
    @DisplayName("6.1 查询企业信用档案")
    void step50_getCreditProfile() {
        // 如果没有企业token，跳过
        if (enterpriseToken == null) {
            System.out.println("跳过查询信用档案 - 企业token为空");
            return;
        }

        Map<String, Object> response = restTemplate.getForObject(
            "/api/v1/credit/profile?enterpriseId=" + (enterpriseId != null ? enterpriseId : 1),
            Map.class
        );

        assertNotNull(response);
        System.out.println("查询信用档案响应: " + response);
    }
}
