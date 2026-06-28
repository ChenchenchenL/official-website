package com.company.officialwebsite.infrastructure.cache;

import com.company.officialwebsite.common.config.properties.OfficialProperties;
import com.company.officialwebsite.common.trace.TraceContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Component;

/**
 * PortalCacheSupport：统一承接 Portal 缓存的读、写与 TTL 解析能力。
 *
 * <p>缓存读取失败（含反序列化异常）时，主动删除坏缓存并返回 null，让调用方回源数据库；
 * 缓存写入支持按模块覆盖 TTL 与空结果短 TTL，避免缓存穿透与长尾不一致。
 */
@Component
public class PortalCacheSupport {

    private static final Logger log = LoggerFactory.getLogger(PortalCacheSupport.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final PortalCacheKeyBuilder portalCacheKeyBuilder;
    private final PortalCacheInvalidationSupport portalCacheInvalidationSupport;
    private final OfficialProperties officialProperties;
    private final ObjectMapper objectMapper;

    public PortalCacheSupport(
            RedisTemplate<String, Object> redisTemplate,
            PortalCacheKeyBuilder portalCacheKeyBuilder,
            PortalCacheInvalidationSupport portalCacheInvalidationSupport,
            OfficialProperties officialProperties,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.portalCacheKeyBuilder = portalCacheKeyBuilder;
        this.portalCacheInvalidationSupport = portalCacheInvalidationSupport;
        this.officialProperties = officialProperties;
        this.objectMapper = objectMapper;
    }

    /**
     * 按模块段构建 Key 并触发事务后失效（含延迟二次删除）。
     */
    public void invalidatePortalKey(String module, String... segments) {
        portalCacheInvalidationSupport.invalidatePortalKey(module, segments);
    }

    /**
     * 直接传入已构造的 Key 触发事务后失效。
     */
    public void invalidate(String... keys) {
        portalCacheInvalidationSupport.invalidate(keys);
    }

    /**
     * 按模块与段构建完整的 Portal 缓存 key，避免业务代码硬编码前缀。
     */
    public String buildKey(String module, String... segments) {
        return portalCacheKeyBuilder.build(module, segments);
    }

    /**
     * 解析指定模块的 Portal 缓存 TTL：若配置了模块覆盖则使用覆盖值，否则使用默认 TTL。
     */
    public Duration resolveTtl(String module) {
        return officialProperties.getCache().resolvePortalTtl(module);
    }

    /**
     * 解析空结果缓存 TTL，默认短于正常 TTL，用于降低缓存穿透风险。
     */
    public Duration resolveEmptyResultTtl() {
        return officialProperties.getCache().getEmptyResultTtl();
    }

    /**
     * 尝试读取缓存，反序列化失败时删除坏缓存并返回 null，由调用方回源数据库。
     *
     * <p>返回 null 的两种语义：缓存未命中 / 缓存已损坏（后者已主动清理）。
     * 当 Redis 中字节损坏导致 get() 抛 SerializationException 时，同样视为坏缓存并主动删除。
     */
    public <T> T readCache(String cacheKey, Class<T> type, String module) {
        return readCacheInternal(cacheKey, module, cached -> objectMapper.convertValue(cached, type));
    }

    /**
     * 列表型 Portal 缓存读取，反序列化失败时删除坏缓存并返回 null。
     */
    public <T> List<T> readListCache(String cacheKey, Class<T> elementType, String module) {
        return readCacheInternal(cacheKey, module, cached -> objectMapper.convertValue(
                cached,
                objectMapper.getTypeFactory().constructCollectionType(List.class, elementType)));
    }

    /**
     * 缓存读取公共骨架：先读 Redis，命中后按 converter 转换为目标类型。
     *
     * <p>读取阶段抛 SerializationException（Redis 字节损坏）或转换阶段抛异常，均视为坏缓存，
     * 主动删除后返回 null；纯连接故障（非数据损坏）不触发删除，避免无意义重试。
     */
    private <T> T readCacheInternal(String cacheKey, String module, Function<Object, T> converter) {
        Object cached;
        try {
            cached = redisTemplate.opsForValue().get(cacheKey);
        } catch (Exception ex) {
            log.warn("portal cache read failed key={} module={} traceId={}",
                    cacheKey, module, TraceContext.getTraceId(), ex);
            if (isBadCacheException(ex)) {
                deleteBadCacheQuietly(cacheKey, module);
            }
            return null;
        }
        if (cached == null) {
            log.debug("portal cache miss key={} module={} traceId={}", cacheKey, module, TraceContext.getTraceId());
            return null;
        }
        try {
            T result = converter.apply(cached);
            log.debug("portal cache hit key={} module={} traceId={}", cacheKey, module, TraceContext.getTraceId());
            return result;
        } catch (Exception ex) {
            log.warn("portal cache deserialize failed, deleting bad cache key={} module={} traceId={}",
                    cacheKey, module, TraceContext.getTraceId(), ex);
            deleteBadCacheQuietly(cacheKey, module);
            return null;
        }
    }

    /**
     * 判断异常是否表明缓存数据已损坏（而非 Redis 连接故障），用于决定是否清理坏缓存。
     */
    private boolean isBadCacheException(Exception ex) {
        if (ex instanceof SerializationException) {
            return true;
        }
        Throwable cause = ex.getCause();
        return cause instanceof SerializationException || cause instanceof JsonProcessingException;
    }

    /**
     * 尽力删除坏缓存，删除失败仅记录 error，不影响回源主流程。
     */
    private void deleteBadCacheQuietly(String cacheKey, String module) {
        try {
            redisTemplate.delete(cacheKey);
        } catch (Exception deleteEx) {
            log.error("portal bad cache delete failed key={} module={} traceId={}",
                    cacheKey, module, TraceContext.getTraceId(), deleteEx);
        }
    }

    /**
     * 写入缓存；根据是否为空结果自动选择 TTL，写入失败仅记录 warn，不影响主流程。
     */
    public void writeCache(String cacheKey, Object value, boolean emptyResult, String module) {
        Duration ttl = emptyResult ? resolveEmptyResultTtl() : resolveTtl(module);
        if (ttl == null || ttl.isNegative()) {
            log.warn("portal cache write skipped due to invalid ttl key={} module={} ttl={} traceId={}",
                    cacheKey, module, ttl, TraceContext.getTraceId());
            return;
        }
        try {
            redisTemplate.opsForValue().set(cacheKey, value, ttl);
        } catch (Exception ex) {
            log.warn("portal cache write failed key={} module={} ttl={} traceId={}",
                    cacheKey, module, ttl, TraceContext.getTraceId(), ex);
        }
    }

    /**
     * 判断 Portal 查询结果是否为空：null、空字符串、空集合、空 Map 均视为空。
     */
    public boolean isEmptyResult(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String s) {
            return s.isEmpty();
        }
        if (value instanceof java.util.Collection<?> c) {
            return c.isEmpty();
        }
        if (value instanceof Map<?, ?> m) {
            return m.isEmpty();
        }
        return false;
    }
}
