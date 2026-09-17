package com.dawn.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class SseConnectionRegistry {

    private final Map<String, Map<String, SseEmitter>> emitters = new ConcurrentHashMap<>();

    public void subscribe(String channel, String clientId, SseEmitter emitter) {
        Map<String, SseEmitter> channelEmitters = emitters.computeIfAbsent(channel, c -> new ConcurrentHashMap<>());
        SseEmitter previous = channelEmitters.get(clientId);
        if (previous != null) {
            log.warn("Client [{}] reconnecting on [{}], completing previous emitter", clientId, channel);
            previous.complete();
        }
        channelEmitters.put(clientId, emitter);
    }

    public void remove(String channel, String clientId, SseEmitter emitter) {
        Map<String, SseEmitter> channelEmitters = emitters.get(channel);
        if (channelEmitters == null) {
            log.info("Client [{}] disconnected from [{}]", clientId, channel);
            return;
        }
        if (channelEmitters.get(clientId) != emitter) {
            return;
        }
        channelEmitters.remove(clientId);
        if (channelEmitters.isEmpty()) {
            emitters.remove(channel);
            log.info("All clients disconnected from [{}]", channel);
        }
    }

    public Map<String, SseEmitter> getAll(String channel) {
        return emitters.get(channel);
    }
}
