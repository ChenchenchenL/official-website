package com.company.officialwebsite.common.config;

import com.company.officialwebsite.common.config.properties.OfficialProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JacksonConfiguration：统一约束接口 JSON 序列化行为，避免时间和数字输出风格在各模块漂移。
 */
@Configuration
public class JacksonConfiguration {

    /**
     * 统一关闭时间戳时间格式，并按配置决定是否将 Long 输出为字符串。
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer(
            OfficialProperties officialProperties) {
        return builder -> {
            builder.serializationInclusion(JsonInclude.Include.NON_NULL);
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            builder.modules(new JavaTimeModule(), longSerializationModule(officialProperties));
        };
    }

    private SimpleModule longSerializationModule(OfficialProperties officialProperties) {
        SimpleModule module = new SimpleModule();
        if (officialProperties.getJson().isWriteLongAsString()) {
            JsonSerializer<Long> serializer = new LongToStringSerializer();
            module.addSerializer(Long.class, serializer);
            module.addSerializer(Long.TYPE, serializer);
        }
        return module;
    }

    /**
     * LongToStringSerializer：在前端存在 Long 精度风险时，将 Long 序列化为字符串。
     */
    private static final class LongToStringSerializer extends JsonSerializer<Long> {

        @Override
        public void serialize(Long value, JsonGenerator jsonGenerator, SerializerProvider serializers)
                throws IOException {
            if (value == null) {
                jsonGenerator.writeNull();
                return;
            }
            jsonGenerator.writeString(String.valueOf(value));
        }
    }
}
