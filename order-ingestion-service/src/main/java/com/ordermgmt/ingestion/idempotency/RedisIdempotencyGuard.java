package com.ordermgmt.ingestion.idempotency;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisIdempotencyGuard implements IdempotencyGuard {

    private static final String PROCESSING_MARKER = "PROCESSING";

    private final StringRedisTemplate redisTemplate;
    private final Duration keyTtl;

    public RedisIdempotencyGuard(
            StringRedisTemplate redisTemplate,
            @Value("${ordermgmt.idempotency.key-ttl:PT24H}") Duration keyTtl) {
        this.redisTemplate = redisTemplate;
        this.keyTtl = keyTtl;
    }

    @Override
    public boolean tryAcquire(String key) {
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(redisKey(key), PROCESSING_MARKER, keyTtl);
        return Boolean.TRUE.equals(acquired);
    }

    private String redisKey(String key) {
        return "idempotency:order:" + key;
    }
}
