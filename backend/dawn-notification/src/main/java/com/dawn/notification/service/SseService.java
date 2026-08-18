package com.dawn.notification.service;

import com.dawn.common.core.constant.Constants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SseService {

    private static final int BROADCAST_THREADS = 8;

    private final SseConnectionRegistry registry;

    private final List<SseSubscriptionListener> subscriptionListeners;

    private final ObjectMapper objectMapper;

    private final ExecutorService executor = Executors.newFixedThreadPool(BROADCAST_THREADS);

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
        log.info("SSE executor shut down");
    }

    public SseEmitter subscribe(String channel, String clientId) {
        SseEmitter emitter = new SseEmitter(Constants.SSE_EMITTER_TIMEOUT_MS);

        emitter.onCompletion(() -> registry.remove(channel, clientId, emitter));
        emitter.onTimeout(() -> registry.remove(channel, clientId, emitter));
        emitter.onError((e) -> registry.remove(channel, clientId, emitter));

        registry.subscribe(channel, clientId, emitter);
        log.info("Client [{}] subscribed to [{}]", clientId, channel);

        try {
            emitter.send(SseEmitter.event().name(Constants.SSE_CONNECTED).data("connected"));
        } catch (IOException e) {
            log.error("Failed to subscribe client [{}] to [{}]", clientId, channel, e);
            registry.remove(channel, clientId, emitter);
            emitter.completeWithError(e);
            return emitter;
        }

        subscriptionListeners.forEach(listener ->
                executor.execute(() -> listener.onSubscribed(emitter, channel, clientId)));

        return emitter;
    }

    public void broadcastToChannel(String channel, String eventName, Object payload) {
        Map<String, SseEmitter> channelEmitters = registry.getAll(channel);
        if (channelEmitters == null || channelEmitters.isEmpty()) return;

        log.info("Broadcasting event [{}] to [{}] clients in channel [{}]", eventName, channelEmitters.size(), channel);

        executor.execute(() -> {
            String payloadJson = serialize(payload);
            if (payloadJson == null) return;

            channelEmitters.forEach((clientId, emitter) -> {
                try {
                    emitter.send(SseEmitter.event().name(eventName).data(payloadJson));
                    log.info("Broadcast send event [{}] to client [{}] in channel [{}]", eventName, clientId, emitter);
                } catch (IOException e) {
                    log.debug("Client [{}] disconnected during broadcast. Cleaning up.", clientId);
                    registry.remove(channel, clientId, emitter);
                }
            });
        });
    }

    private String serialize(Object payload) {
        if (payload instanceof String s) return s;
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize broadcast payload", e);
            return null;
        }
    }
}
