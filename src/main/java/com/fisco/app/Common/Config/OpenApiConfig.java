package com.fisco.app.Common.Config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Springdoc OpenAPI 配置
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FISCO BCOS Supply Chain Finance API")
                        .description("区块链供应链金融平台 API 文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("FISCO BCOS Team")
                                .email("")));
    }
}
