package com.fisco.app.Common.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Jackson 配置类
 *
 * 配置说明：
 * - 响应序列化：驼峰命名 (userName -> userName)
 * - 请求解析：默认不做转换，需要在 DTO 类上使用 @JsonDeserialize 注解
 *
 * 如需全局启用请求下划线转驼峰，请在对应的 DTO 类上添加：
 * @JsonDeserialize(propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE)
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // 注册 Java 8 日期时间模块
        mapper.registerModule(new JavaTimeModule());
        // 配置反序列化：忽略未知字段
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 配置序列化：日期格式
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setDateFormat(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        return mapper;
    }
}
