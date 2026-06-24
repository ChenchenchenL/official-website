package com.company.officialwebsite.common.config;

import com.company.officialwebsite.common.config.properties.OfficialProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * CacheConfiguration：统一配置 Spring Cache 与 Redis 缓存规则，收口 TTL、前缀和序列化策略。
 */
@Configuration
@EnableCaching
public class CacheConfiguration {

    /**
     * 使用统一前缀和 TTL，避免不同缓存入口生成互不兼容的 Redis key。
     */
    @Bean
    public RedisCacheConfiguration officialRedisCacheConfiguration(
            OfficialProperties officialProperties,
            GenericJackson2JsonRedisSerializer officialRedisValueSerializer) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(officialProperties.getCache().getDefaultTtl())
                .disableCachingNullValues()
                .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(SerializationPair.fromSerializer(officialRedisValueSerializer))
                .computePrefixWith(cacheName -> officialProperties.getCache().getKeyPrefix() + ":" + cacheName + ":");
    }

    /**
     * 统一暴露 RedisCacheManager，后续业务模块可直接基于 cacheName 约定接入 Spring Cache。
     */
    @Bean
    public RedisCacheManager redisCacheManager(
            RedisConnectionFactory redisConnectionFactory,
            RedisCacheConfiguration officialRedisCacheConfiguration) {
        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(officialRedisCacheConfiguration)
                .transactionAware()
                .build();
    }
}
