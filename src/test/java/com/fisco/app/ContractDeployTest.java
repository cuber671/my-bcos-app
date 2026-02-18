package com.fisco.app;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.fisco.app.contract.bill.BillV2;
import com.fisco.app.deployment.ContractManager;

@SpringBootTest
public class ContractDeployTest {

    @Autowired
    private ContractManager contractManager;

    @Test
    @DisplayName("验证合约一键部署逻辑与地址持久化")
    public void testDeployAndLoad() {
        System.out.println("\n=== 开始合约部署单元测试 ===");

        // 1. 调用部署逻辑 (第一次运行会部署，第二次会自动跳过)
        contractManager.deployAll();

        // 2. 验证加载逻辑
        BillV2 billService = contractManager.getBillService();
        
        // 3. 断言校验
        assertNotNull(billService, "加载失败：BillV2 合约实例不应为空");
        String address = billService.getContractAddress();
        System.out.println("成功关联到链上合约地址: " + address);
        
        assertTrue(address.startsWith("0x"), "地址格式错误");

        // 4. 模拟二次部署测试幂等性
        System.out.println("测试幂等性（再次调用 deployAll）:");
        contractManager.deployAll(); 

        System.out.println("=== 测试完成 ===\n");
    }
}
