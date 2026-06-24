package com.company.officialwebsite.common.config;

import com.company.officialwebsite.common.config.properties.OfficialProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * RedisConfiguration：统一定义 RedisTemplate 与缓存序列化规则，避免 Key 和 Value 格式失控。
 */
@Configuration
public class RedisConfiguration {

    /**
     * 统一 JSON 序列化器，确保 RedisTemplate 与 Spring Cache 读写格式一致。
     */
    @Bean
    public GenericJackson2JsonRedisSerializer officialRedisValueSerializer(ObjectMapper objectMapper) {
        return new GenericJackson2JsonRedisSerializer(objectMapper.copy());
    }

    /**
     * Portal 与后台共享统一的 RedisTemplate，后续缓存组件只需要遵守统一前缀和 TTL 约定。
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory redisConnectionFactory,
            GenericJackson2JsonRedisSerializer officialRedisValueSerializer) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        StringRedisSerializer keySerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(keySerializer);
        redisTemplate.setHashKeySerializer(keySerializer);
        redisTemplate.setValueSerializer(officialRedisValueSerializer);
        redisTemplate.setHashValueSerializer(officialRedisValueSerializer);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    /**
     * 暴露统一缓存前缀 Bean，便于后续缓存模块复用而不是散落硬编码。
     */
    @Bean
    public String officialCacheKeyPrefix(OfficialProperties officialProperties) {
        return officialProperties.getCache().getKeyPrefix();
    }
}
