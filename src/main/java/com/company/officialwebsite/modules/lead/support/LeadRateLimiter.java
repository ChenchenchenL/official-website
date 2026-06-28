package com.company.officialwebsite.modules.lead.support;

import com.company.officialwebsite.modules.lead.service.LeadModuleConstants;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * LeadRateLimiter：基于 Redis 原子计数 + TTL 的固定窗口限流，拦截高频匿名提交。
 */
@Component
public class LeadRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(LeadRateLimiter.class);

    private final RedisTemplate<String, Object> redisTemplate;

    public LeadRateLimiter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 尝试消耗一次提交配额。返回 true 表示允许提交，false 表示已被限流。
     */
    public boolean tryAcquire(String clientIp) {
        String key = LeadModuleConstants.RATE_LIMIT_KEY_PREFIX + clientIp;
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, LeadModuleConstants.RATE_LIMIT_WINDOW_SECONDS, TimeUnit.SECONDS);
            }
            if (count != null && count > LeadModuleConstants.RATE_LIMIT_MAX_COUNT) {
                log.warn("lead submit rate limited ip={} count={}", clientIp, count);
                return false;
            }
            return true;
        } catch (Exception ex) {
            log.error("lead rate limiter failed ip={}", clientIp, ex);
            return true;
        }
    }
}
