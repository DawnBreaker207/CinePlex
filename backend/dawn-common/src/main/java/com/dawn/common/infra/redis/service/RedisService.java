package com.dawn.common.infra.redis.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private DefaultRedisScript<List> scriptLockMulti;

    private DefaultRedisScript<Long> scriptDeleteIfEquals;

    @PostConstruct
    public void initScript() {
        this.scriptLockMulti = new DefaultRedisScript<>();
        this.scriptLockMulti.setLocation(new ClassPathResource("script/lock_seat.lua"));
        this.scriptLockMulti.setResultType(List.class);

        this.scriptDeleteIfEquals = new DefaultRedisScript<>();
        this.scriptDeleteIfEquals.setLocation(new ClassPathResource("script/unlock_seat.lua"));
        this.scriptDeleteIfEquals.setResultType(Long.class);
    }


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

    public <T> T executeScript(RedisScript<T> script, List<String> keys, Object... args) {
        return redisTemplate.execute(script, keys, args);
    }

    public List lockMulti(List<String> keys, String owner, Duration ttl) {
        if (keys == null || keys.isEmpty()) return Collections.singletonList(1L);
        try {
            return stringRedisTemplate.execute(
                    scriptLockMulti,
                    keys,
                    owner,
                    String.valueOf(ttl.getSeconds()));
        } catch (Exception e) {
            log.error("Redis error: {}", e.getMessage());
            return Arrays.asList(0L, keys.getFirst(), "System_error");
        }
    }

    public boolean releaseLock(String key, String expectedOwner) {
        if (key == null || expectedOwner == null) return false;

        try {
            Long result = stringRedisTemplate.execute(
                    scriptDeleteIfEquals,
                    Collections.singletonList(key),
                    expectedOwner);
            return Long.valueOf(1L).equals(result);
        } catch (Exception e) {
            log.error("Redis error: {}", e.getMessage());
            return false;
        }
    }

    public List<Object> multiGet(List<String> keys) {
        if (keys == null || keys.isEmpty()) return Collections.emptyList();
        try {
            List<String> values = stringRedisTemplate.opsForValue().multiGet(keys);
            if (values == null) return List.of();
            return values
                    .stream()
                    .map(v -> (Object) v)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Redis multiGet error for key {}: {}", keys, e.getMessage());
            return Collections.nCopies(keys.size(), null);
        }
    }
}
