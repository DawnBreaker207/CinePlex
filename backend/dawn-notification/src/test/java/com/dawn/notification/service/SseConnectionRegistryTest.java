package com.dawn.notification.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SseConnectionRegistryTest {

    private final SseConnectionRegistry registry = new SseConnectionRegistry();

    @Test
    void subscribe_registersEmitterPerChannelAndClient() {
        SseEmitter emitter = mock(SseEmitter.class);

        registry.subscribe("channel:showtime:1", "client-1", emitter);

        assertThat(registry.getAll("channel:showtime:1")).containsEntry("client-1", emitter);
    }

    @Test
    void subscribe_sameClientId_completesPreviousEmitter() {
        SseEmitter oldEmitter = mock(SseEmitter.class);
        SseEmitter newEmitter = mock(SseEmitter.class);
        registry.subscribe("channel:showtime:1", "client-1", oldEmitter);

        registry.subscribe("channel:showtime:1", "client-1", newEmitter);

        verify(oldEmitter).complete();
        assertThat(registry.getAll("channel:showtime:1")).containsEntry("client-1", newEmitter);
    }

    @Test
    void remove_onlyRemovesWhenEmitterMatches() {
        SseEmitter oldEmitter = mock(SseEmitter.class);
        SseEmitter newEmitter = mock(SseEmitter.class);
        registry.subscribe("channel:showtime:1", "client-1", oldEmitter);

        registry.remove("channel:showtime:1", "client-1", newEmitter);

        assertThat(registry.getAll("channel:showtime:1")).containsEntry("client-1", oldEmitter);

        registry.remove("channel:showtime:1", "client-1", oldEmitter);

        assertThat(registry.getAll("channel:showtime:1")).isNullOrEmpty();
    }

    @Test
    void remove_cleansUpEmptyChannel() {
        SseEmitter emitter = mock(SseEmitter.class);
        registry.subscribe("channel:showtime:1", "client-1", emitter);

        registry.remove("channel:showtime:1", "client-1", emitter);

        assertThat(registry.getAll("channel:showtime:1")).isNull();
    }

    @Test
    void remove_unknownChannel_isNoop() {
        registry.remove("channel:showtime:1", "client-1", mock(SseEmitter.class));

        assertThat(registry.getAll("channel:showtime:1")).isNull();
    }
}
