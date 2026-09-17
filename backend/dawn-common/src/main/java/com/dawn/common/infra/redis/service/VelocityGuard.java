package com.dawn.common.infra.redis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

// Redis-backed velocity check shared by anti-abuse hooks; fail-open on Redis errors.
@Component
@RequiredArgsConstructor
@Slf4j
public class VelocityGuard {

    private final RedisService redisService;

    public boolean tryAcquire(String key, int maxHits, Duration window) {
        try {
            Long count = redisService.increment(key, 1L);
            if (count == null) {
                return true;
            }
            if (count == 1L) {
                redisService.expire(key, window);
            }
            return count <= maxHits;
        } catch (RuntimeException e) {
            log.warn("Velocity check fail-open for key {}", key, e);
            return true;
        }
    }
}
