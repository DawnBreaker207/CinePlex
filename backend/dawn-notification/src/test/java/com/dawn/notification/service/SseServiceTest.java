package com.dawn.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SseServiceTest {

    private static final String CHANNEL_A = "channel:showtime:1";
    private static final String CHANNEL_B = "channel:showtime:2";

    private final SseConnectionRegistry registry = new SseConnectionRegistry();
    private final ObjectMapper objectMapper = mock(ObjectMapper.class);
    private final SseService sseService = new SseService(registry, List.of(), objectMapper);

    @AfterEach
    void tearDown() {
        sseService.shutdown();
    }

    @Test
    void subscribe_registersEmitterAndSendsConnected() {
        SseEmitter emitter = sseService.subscribe(CHANNEL_A, "client-1");

        assertThat(emitter).isNotNull();
        assertThat(registry.getAll(CHANNEL_A)).containsEntry("client-1", emitter);
    }

    @Test
    void subscribe_sameClientId_completesPreviousEmitter() {
        SseEmitter first = sseService.subscribe(CHANNEL_A, "client-1");
        SseEmitter second = sseService.subscribe(CHANNEL_A, "client-1");

        assertThat(registry.getAll(CHANNEL_A)).containsEntry("client-1", second);
    }

    @Test
    void subscribe_invokesSubscriptionListeners() {
        SseSubscriptionListener listener = mock(SseSubscriptionListener.class);
        SseService service = new SseService(registry, List.of(listener), objectMapper);

        SseEmitter emitter = service.subscribe(CHANNEL_A, "client-1");

        verify(listener, timeout(2000)).onSubscribed(emitter, CHANNEL_A, "client-1");
        service.shutdown();
    }

    @Test
    void broadcast_sendsToClientsInChannelOnly() throws Exception {
        SseEmitter a1 = mock(SseEmitter.class);
        SseEmitter a2 = mock(SseEmitter.class);
        SseEmitter b1 = mock(SseEmitter.class);
        registry.subscribe(CHANNEL_A, "a-1", a1);
        registry.subscribe(CHANNEL_A, "a-2", a2);
        registry.subscribe(CHANNEL_B, "b-1", b1);

        sseService.broadcastToChannel(CHANNEL_A, "SEAT_HOLD", "{\"event\":\"SEAT_HOLD\"}");

        verify(a1, timeout(2000)).send(any(SseEmitter.SseEventBuilder.class));
        verify(a2, timeout(2000)).send(any(SseEmitter.SseEventBuilder.class));
        verify(b1, never()).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void broadcast_serializesPayloadOnce() throws Exception {
        Object payload = Map.of("event", "SEAT_HOLD", "seatIds", List.of("A1"));
        when(objectMapper.writeValueAsString(payload)).thenReturn("{\"event\":\"SEAT_HOLD\"}");
        SseEmitter a1 = mock(SseEmitter.class);
        SseEmitter a2 = mock(SseEmitter.class);
        registry.subscribe(CHANNEL_A, "a-1", a1);
        registry.subscribe(CHANNEL_A, "a-2", a2);

        sseService.broadcastToChannel(CHANNEL_A, "SEAT_HOLD", payload);

        verify(objectMapper, timeout(2000)).writeValueAsString(payload);
        verify(a1, timeout(2000)).send(any(SseEmitter.SseEventBuilder.class));
        verify(a2, timeout(2000)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void broadcast_removesEmitterOnIOException() throws Exception {
        SseEmitter failing = mock(SseEmitter.class);
        doThrow(new IOException("gone")).when(failing).send(any(SseEmitter.SseEventBuilder.class));
        registry.subscribe(CHANNEL_A, "a-1", failing);

        sseService.broadcastToChannel(CHANNEL_A, "SEAT_HOLD", "{}");

        verify(failing, timeout(2000)).send(any(SseEmitter.SseEventBuilder.class));
        awaitRemoval(CHANNEL_A);
        assertThat(registry.getAll(CHANNEL_A)).isNullOrEmpty();
    }

    @Test
    void broadcast_unknownChannel_isNoop() {
        sseService.broadcastToChannel("channel:showtime:99", "SEAT_HOLD", "{}");

        assertThat(registry.getAll("channel:showtime:99")).isNull();
    }

    private void awaitRemoval(String channel) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        Map<String, SseEmitter> remaining = registry.getAll(channel);
        while ((remaining == null || remaining.isEmpty()) == false && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
            remaining = registry.getAll(channel);
        }
    }
}
