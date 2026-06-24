package com.company.officialwebsite.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.officialwebsite.common.config.properties.OfficialProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * JacksonConfigurationTest：验证统一 JSON 配置对时间与 Long 输出的约束行为。
 */
class JacksonConfigurationTest {

    @Test
    void shouldSerializeLongAsStringWhenConfigured() throws Exception {
        OfficialProperties officialProperties = new OfficialProperties();
        officialProperties.getJson().setWriteLongAsString(true);

        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        new JacksonConfiguration().jackson2ObjectMapperBuilderCustomizer(officialProperties).customize(builder);
        ObjectMapper objectMapper = builder.build();

        String json = objectMapper.writeValueAsString(new SampleValue(9007199254740993L, LocalDateTime.of(2026, 6, 24, 10, 30, 0)));

        assertThat(json).contains("\"id\":\"9007199254740993\"");
        assertThat(json).contains("\"createdAt\":\"2026-06-24T10:30:00\"");
    }

    private record SampleValue(Long id, LocalDateTime createdAt) {
    }
}
