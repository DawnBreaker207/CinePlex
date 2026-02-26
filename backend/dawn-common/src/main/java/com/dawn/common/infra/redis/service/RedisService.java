package com.dawn.common.infra.redis.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    public Map<Object, Object> getHash(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    public void putHash(String key, Map<?, ?> data, Duration ttl) {
        redisTemplate.opsForHash().putAll(key, data);
        redisTemplate.expire(key, ttl);
    }

    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void set(String key, Object value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    public void put(String key, String field, Object value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    public Boolean setIfAbsent(String key, Object value, Duration ttl) {
        return redisTemplate.opsForValue().setIfAbsent(key, value, ttl);
    }

    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    public Long getExpired(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    public Boolean expire(String key, Duration ttl) {
        return redisTemplate.expire(key, ttl);
    }

    public Long decrement(String key, Long delta) {
        return redisTemplate.opsForValue().decrement(key, delta);
    }

    public Long increment(String key, Long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    public <T> T execute(RedisCallback<T> action) {
        return redisTemplate.execute(action);
    }
}
