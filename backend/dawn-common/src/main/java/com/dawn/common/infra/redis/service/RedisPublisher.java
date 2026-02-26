package com.dawn.common.infra.redis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisPublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    public void publish(String channel, Object message) {
        try {
            redisTemplate.convertAndSend(channel, message);
            log.info("Successfully publish event {} to channel {}", message.getClass().getSimpleName(), channel);
        } catch (Exception e) {
            log.error("Failed to publish message", e);
        }
    }
}
