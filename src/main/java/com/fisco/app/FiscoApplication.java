package com.fisco.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import io.github.resilience4j.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration;

@SpringBootApplication(exclude = {CircuitBreakerAutoConfiguration.class})
@EnableTransactionManagement
@MapperScan("com.fisco.app.Modules.**.Mapper")
public class FiscoApplication {
    public static void main(String[] args) {
        SpringApplication.run(FiscoApplication.class, args);
        System.out.println("=================================================");
        System.out.println("        供应链金融区块链平台 启动成功           ");
        System.out.println("            (Fisco BCOS System)                 ");
        System.out.println("=================================================");
    }
}
