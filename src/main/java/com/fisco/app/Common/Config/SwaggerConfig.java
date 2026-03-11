package com.fisco.app.Common.Config;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;

import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.spring.web.plugins.WebFluxRequestHandlerProvider;
import springfox.documentation.spring.web.plugins.WebMvcRequestHandlerProvider;

/**
 * Springfox (Swagger) 配置
 *
 * 修复 Springfox 3.0.0 与 Spring Boot 2.6+ 的兼容性问题
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.OAS_30)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.fisco.app"))
                .paths(PathSelectors.any())
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfo(
                "FISCO BCOS Supply Chain Finance API",
                "区块链供应链金融平台 API 文档",
                "1.0.0",
                "",
                new Contact("FISCO BCOS Team", "", ""),
                "",
                "",
                Collections.emptyList()
        );
    }

    /**
     * 修复 Springfox 与 Spring Boot 2.6+ 的兼容性问题
     * 解决 NullPointerException 错误
     */
    @Bean
    public static BeanPostProcessor springfoxHandlerProviderBeanPostProcessor() {
        return new BeanPostProcessor() {

            @Override
            public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
                if (bean instanceof WebMvcRequestHandlerProvider || bean instanceof WebFluxRequestHandlerProvider) {
                    var handlerMappingsField = ReflectionUtils.findField(
                            bean.getClass(), "handlerMappings");
                    if (handlerMappingsField != null) {
                        handlerMappingsField.setAccessible(true);
                        try {
                            @SuppressWarnings("unchecked")
                            List<RequestMappingInfoHandlerMapping> mappings =
                                    (List<RequestMappingInfoHandlerMapping>) handlerMappingsField.get(bean);
                            if (mappings != null) {
                                mappings.clear();
                            }
                        } catch (IllegalAccessException e) {
                            // ignore
                        }
                    }
                }
                return bean;
            }
        };
    }
}
