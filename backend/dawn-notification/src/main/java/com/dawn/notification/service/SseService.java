package com.dawn.notification.service;

import com.dawn.notification.dto.SeatDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SseService {

    private final Map<String, Map<String, SseEmitter>> emitters = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    private final ReservationNotifyService reservationNotifyService;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    public SseEmitter subscribe(String channel, String clientId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        emitter.onCompletion(() -> removeEmitter(channel, clientId));
        emitter.onTimeout(() -> removeEmitter(channel, clientId));
        emitter.onError((e) -> removeEmitter(channel, clientId));
        try {
            emitters.computeIfAbsent(channel, c -> new ConcurrentHashMap<>()).put(clientId, emitter);
            log.info("Client [{}] subscribed to [{}]", clientId, channel);

            if (channel.startsWith("channel:showtime:")) {
                executor.execute(() -> sendCurrentSeatState(emitter, channel));
            }
            log.info("Subscribe to channel {}", emitter);
            return emitter;

        } catch (Exception e) {
            log.error("Failed to subscribe client [{}] to [{}]", clientId, channel, e);
            removeEmitter(channel, clientId);
            emitter.completeWithError(e);
            throw e;
        }
    }

    private void removeEmitter(String channel, String clientId) {
        Map<String, SseEmitter> channelEmitters = emitters.get(channel);
        if (channelEmitters != null) {
            channelEmitters.remove(clientId);
            if (channelEmitters.isEmpty()) {
                emitters.remove(channel);
                log.info("All clients disconnected from [{}]", channel);
            }
        } else {
            log.info("Client [{}] disconnected from [{}]", clientId, channel);
        }
    }


    private void sendCurrentSeatState(SseEmitter emitter, String channel) {
        try {
            String[] parts = channel.split(":");
            Long showtimeId = Long.valueOf(parts[2]);
            log.debug("Fetching snapshot for showtime {}", showtimeId);

            List<SeatDTO> lockedSeats = reservationNotifyService.getLockedSeats(showtimeId);

            Map<String, Object> initialState = Map.of(
                    "event", "SEAT_STATE_INIT",
                    "showtimeId", showtimeId,
                    "seatIds", lockedSeats
            );

            String messageJson = objectMapper.writeValueAsString(initialState);
            emitter.send(SseEmitter.event().name("SEAT_STATE_INIT").data(messageJson));
            log.info("Sent initial seat state to new client, {} seats hold", lockedSeats);
        } catch (Exception e) {
            log.debug("Emitter disconnected before receiving initial state.");
            log.error("Failed to fetch/send initial seat state for channel {}", channel, e);
            removeEmitter(channel, "anonymous");
        }
    }

    public void broadcastToChannel(String channel, String message) {
        Map<String, SseEmitter> channelEmitters = emitters.get(channel);
        if (channelEmitters == null || channelEmitters.isEmpty()) return;
        log.info("Broadcasting to [{}] clients in channel [{}]", channelEmitters.size(), channel);
        log.debug("Message content [{}]", message);

        String eventName = extractEventName(message);
        log.info("Event name extracted: {}", eventName);
        channelEmitters.forEach((clientId, emitter) -> {
            try {
                emitter.send(SseEmitter
                        .event()
                        .name(eventName)
                        .data(message));
                log.info("Broadcast send event [{}] to client [{}] in channel [{}]", eventName, clientId, emitter);
            } catch (IOException e) {
                log.debug("Client [{}] disconnected during broadcast. Cleaning up.", clientId);
                removeEmitter(channel, clientId);
            }
        });
    }

    private String extractEventName(String messageJson) {
        try {
            log.info("Parsing event from: {}", messageJson);
            JsonNode node = objectMapper.readTree(messageJson);
            String eventName = node.has("event") ? node.get("event").asText() : "message";
            log.info("Extracted event name [{}]", eventName);
            return eventName;
        } catch (Exception e) {
            log.warn("Cannot parse event name from message", e);
            return "message";
        }
    }


}
