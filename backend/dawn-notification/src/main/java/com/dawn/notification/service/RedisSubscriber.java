package com.dawn.notification.service;

import com.dawn.common.core.constant.Constants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private static final String DEFAULT_EVENT_NAME = "message";

    private final SseService sseService;

    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        log.info("Receive message from channel:{}, body:{}", channel, body);
        try {
            JsonNode node = objectMapper.readTree(body);
            String eventName = node.has(Constants.SSE_FIELD_EVENT)
                    ? node.get(Constants.SSE_FIELD_EVENT).asText()
                    : DEFAULT_EVENT_NAME;
            sseService.broadcastToChannel(channel, eventName, node);
        } catch (Exception e) {
            log.error("Failed to broadcast message to channel {}: {}", channel, e.getMessage(), e);
        }
    }
}
