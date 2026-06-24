package com.company.officialwebsite.infrastructure.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * PortalCacheKeyBuilder：统一构造 Portal 缓存 key，避免业务代码散落硬编码前缀和分隔符。
 */
@Component
public class PortalCacheKeyBuilder {

    private static final String PORTAL_SEGMENT = "portal";

    private final String keyPrefix;

    public PortalCacheKeyBuilder(@Qualifier("officialCacheKeyPrefix") String keyPrefix) {
        this.keyPrefix = requireSegment(keyPrefix, "cacheKeyPrefix");
    }

    /**
     * Portal 缓存 key 固定带上 portal 段，避免后台私有缓存与公开缓存混用同一命名空间。
     */
    public String build(String module, String... segments) {
        List<String> parts = new ArrayList<>();
        parts.add(keyPrefix);
        parts.add(PORTAL_SEGMENT);
        parts.add(requireSegment(module, "module"));
        if (segments != null) {
            for (String segment : segments) {
                parts.add(requireSegment(segment, "segment"));
            }
        }
        return String.join(":", parts);
    }

    /**
     * 对批量段构建统一复用同一校验规则，避免空白段进入 Redis key。
     */
    public String build(String module, Collection<String> segments) {
        return build(module, segments == null ? new String[0] : segments.toArray(String[]::new));
    }

    private String requireSegment(String value, String name) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
