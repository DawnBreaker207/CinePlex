package com.dawn.notification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.connection.Message;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RedisSubscriberTest {

    private final SseService sseService = mock(SseService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RedisSubscriber subscriber = new RedisSubscriber(sseService, objectMapper);

    @Test
    void onMessage_parsesEventNameAndBroadcastsPayload() throws Exception {
        Message message = message("channel:showtime:1", "{\"event\":\"SEAT_HOLD\",\"seatIds\":[\"A1\"]}");

        subscriber.onMessage(message, null);

        ArgumentCaptor<JsonNode> payloadCaptor = ArgumentCaptor.forClass(JsonNode.class);
        verify(sseService).broadcastToChannel(eq("channel:showtime:1"), eq("SEAT_HOLD"), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue().get("seatIds").get(0).asText()).isEqualTo("A1");
    }

    @Test
    void onMessage_withoutEventField_defaultsToMessage() {
        Message message = message("channel:showtime:1", "{\"seatIds\":[\"A1\"]}");

        subscriber.onMessage(message, null);

        verify(sseService).broadcastToChannel(eq("channel:showtime:1"), eq("message"), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void onMessage_broadcastFailure_isSwallowed() {
        Message message = message("channel:showtime:1", "{\"event\":\"SEAT_HOLD\"}");
        doThrow(new RuntimeException("boom")).when(sseService).broadcastToChannel(
                eq("channel:showtime:1"), eq("SEAT_HOLD"), org.mockito.ArgumentMatchers.any());

        assertThatCode(() -> subscriber.onMessage(message, null)).doesNotThrowAnyException();
    }

    private Message message(String channel, String body) {
        Message message = mock(Message.class);
        org.mockito.Mockito.when(message.getChannel()).thenReturn(channel.getBytes(StandardCharsets.UTF_8));
        org.mockito.Mockito.when(message.getBody()).thenReturn(body.getBytes(StandardCharsets.UTF_8));
        return message;
    }
}
