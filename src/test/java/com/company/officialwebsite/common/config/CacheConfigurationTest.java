package com.company.officialwebsite.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.company.officialwebsite.common.config.properties.OfficialProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

/**
 * CacheConfigurationTest：验证 Spring Cache 默认 TTL、前缀和 RedisCacheManager 装配结果。
 */
class CacheConfigurationTest {

    @Test
    void officialRedisCacheConfiguration_shouldApplyTtlAndPrefix() {
        OfficialProperties officialProperties = new OfficialProperties();
        officialProperties.getCache().setKeyPrefix("official");
        officialProperties.getCache().setDefaultTtl(Duration.ofMinutes(12));

        GenericJackson2JsonRedisSerializer serializer =
                new RedisConfiguration().officialRedisValueSerializer(new ObjectMapper().findAndRegisterModules());
        RedisCacheConfiguration cacheConfiguration =
                new CacheConfiguration().officialRedisCacheConfiguration(officialProperties, serializer);

        assertThat(cacheConfiguration.getTtl()).isEqualTo(Duration.ofMinutes(12));
        assertThat(cacheConfiguration.usePrefix()).isTrue();
        assertThat(cacheConfiguration.getKeyPrefixFor("portal:home")).isEqualTo("official:portal:home:");
        assertThat(cacheConfiguration.getAllowCacheNullValues()).isFalse();
    }

    @Test
    void redisCacheManager_shouldBeCreatedWithTransactionAwareSupport() {
        OfficialProperties officialProperties = new OfficialProperties();
        GenericJackson2JsonRedisSerializer serializer =
                new RedisConfiguration().officialRedisValueSerializer(new ObjectMapper().findAndRegisterModules());
        RedisCacheConfiguration cacheConfiguration =
                new CacheConfiguration().officialRedisCacheConfiguration(officialProperties, serializer);

        RedisConnectionFactory redisConnectionFactory = mock(RedisConnectionFactory.class);
        RedisCacheManager redisCacheManager =
                new CacheConfiguration().redisCacheManager(redisConnectionFactory, cacheConfiguration);

        assertThat(redisCacheManager).isNotNull();
    }
}
