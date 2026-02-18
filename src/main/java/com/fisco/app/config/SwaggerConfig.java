package com.fisco.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.oas.annotations.EnableOpenApi;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.Collections;
import java.util.List;

/**
 * Swagger配置类
 * 用于生成API文档
 */
@Configuration
@EnableOpenApi
public class SwaggerConfig {

    /**
     * 创建API文档
     */
    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.OAS_30)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.fisco.app.controller"))
                .paths(PathSelectors.any())
                .build()
                .securitySchemes(securitySchemes())
                .securityContexts(securityContexts());
    }

    /**
     * API信息
     */
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("供应链金融系统 API文档")
                .description("基于FISCO BCOS区块链的供应链金融系统接口文档")
                .contact(new Contact("开发团队", "", ""))
                .version("1.0.0")
                .build();
    }

    /**
     * 安全方案（JWT）
     */
    private List<SecurityScheme> securitySchemes() {
        // 使用Api Key认证
        ApiKey apiKey = new ApiKey("Authorization", "Authorization", "header");
        return Collections.singletonList(apiKey);
    }

    /**
     * 安全上下文
     */
    private List<SecurityContext> securityContexts() {
        return Collections.singletonList(
                SecurityContext.builder()
                        .securityReferences(defaultAuth())
                        .operationSelector(operationContext -> true)
                        .build()
        );
    }

    /**
     * 默认的安全引用
     */
    private List<SecurityReference> defaultAuth() {
        AuthorizationScope authorizationScope = new AuthorizationScope("global", "accessEverything");
        AuthorizationScope[] authorizationScopes = new AuthorizationScope[1];
        authorizationScopes[0] = authorizationScope;
        return Collections.singletonList(new SecurityReference("Authorization", authorizationScopes));
    }
}
