package com.dawn.notification.service;

import com.dawn.common.core.constant.Constants;
import com.dawn.notification.dto.SeatDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class ShowtimeSeatStateListener implements SseSubscriptionListener {

    private static final String SHOWTIME_CHANNEL_PREFIX = "channel:showtime:";

    private final ReservationNotifyService reservationNotifyService;
    private final SseConnectionRegistry registry;
    private final ObjectMapper objectMapper;

    @Override
    public void onSubscribed(SseEmitter emitter, String channel, String clientId) {
        if (!channel.startsWith(SHOWTIME_CHANNEL_PREFIX)) {
            return;
        }

        try {
            Long showtimeId = Long.valueOf(channel.substring(SHOWTIME_CHANNEL_PREFIX.length()));
            log.debug("Fetching snapshot for showtime {}", showtimeId);

            List<SeatDTO> lockedSeats = reservationNotifyService.getLockedSeats(showtimeId);

            Map<String, Object> initialState = Map.of(
                    Constants.SSE_FIELD_EVENT, Constants.SSE_SEAT_STATE_INIT,
                    Constants.SSE_FIELD_SHOWTIME_ID, showtimeId,
                    Constants.SSE_FIELD_SEAT_IDS, lockedSeats
            );

            emitter.send(SseEmitter.event()
                    .name(Constants.SSE_SEAT_STATE_INIT)
                    .data(objectMapper.writeValueAsString(initialState)));
            log.info("Sent initial seat state to new client, {} seats hold", lockedSeats);
        } catch (Exception e) {
            log.debug("Emitter disconnected before receiving initial state.");
            log.error("Failed to fetch/send initial seat state for channel {}", channel, e);
            registry.remove(channel, clientId, emitter);
            emitter.completeWithError(e);
        }
    }
}
