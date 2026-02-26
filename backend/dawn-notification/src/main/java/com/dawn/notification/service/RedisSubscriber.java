package com.dawn.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSubscriber implements MessageListener {

    private final SseService sseService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        log.info("Receive message from channel:{}, body:{}", channel, body);
        try {
            sseService.broadcastToChannel(channel, body);
        } catch (Exception e) {
            log.error("Failed to broadcast message to channel {}: {}", channel, e.getMessage());
        }
    }
}
