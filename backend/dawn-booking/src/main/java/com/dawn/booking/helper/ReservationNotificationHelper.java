package com.dawn.booking.helper;

import com.dawn.booking.dto.response.*;
import com.dawn.booking.model.Reservation;
import com.dawn.common.core.constant.Constants;
import com.dawn.booking.service.ReservationRedisService;
import com.dawn.catalog.api.CatalogModuleApi;
import com.dawn.catalog.dto.response.MovieResponse;
import com.dawn.cinema.dto.response.SeatResponse;
import com.dawn.cinema.dto.response.ShowtimeResponse;
import com.dawn.identity.api.IdentityModuleApi;
import com.dawn.identity.dto.response.UserResponse;
import com.dawn.common.core.constant.RabbitMQConstants;
import com.dawn.common.core.dto.event.BookingCompleteEvent;
import com.dawn.common.core.outbox.Outbox;
import com.dawn.common.core.outbox.OutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Component
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ReservationNotificationHelper {

    private static final String OUTBOX_EVENT_BOOKING_COMPLETED = "BOOKING_COMPLETED";

    private static final String OUTBOX_EVENT_DASHBOARD_REFRESH = "DASHBOARD_REFRESH";

    private static final String AGGREGATE_BOOKING = "booking";

    OutboxRepository outboxRepository;

    ObjectMapper objectMapper;

    CatalogModuleApi catalogApi;

    IdentityModuleApi userService;

    ReservationRedisService reservationRedisService;

    public void handleNotification(Reservation reservation, ShowtimeResponse showtime, List<SeatResponse> seats) {
        try {

            UserResponse user = userService.findUserById(reservation.getUserId());
            MovieResponse movie = catalogApi.findMovieById(showtime.getMovieId());
            String seatNumbers = seats.stream().map(SeatResponse::getSeatNumber).collect(Collectors.joining(","));

            String paymentTimeStr = LocalDateTime
                    .ofInstant(reservation.getCreatedAt(), ZoneId.of("Asia/Ho_Chi_Minh"))
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

            String showtimeStr = LocalDateTime
                    .of(showtime.getShowDate(), showtime.getShowTime())
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

            BookingCompleteEvent event = BookingCompleteEvent
                    .builder()
                    .to(user.getEmail())
                    .name(user.getUsername())
                    .reservationCode(reservation.getReservationCode())
                    .movieName(movie.getTitle())
                    .theaterName(showtime.getTheaterName())
                    .showtimeSession(showtimeStr)
                    .seats(seatNumbers)
                    .paymentTime(paymentTimeStr)
                    .total(reservation.getTotalAmount().toString())
                    .build();

            enqueue(RabbitMQConstants.RK_NOTIFICATION_RESERVATION_COMPLETED, OUTBOX_EVENT_BOOKING_COMPLETED,
                    reservation.getReservationCode(), event);
            enqueue(RabbitMQConstants.RK_DASHBOARD_REFRESH, OUTBOX_EVENT_DASHBOARD_REFRESH,
                    reservation.getReservationCode(),
                    Collections.singletonMap(Constants.SSE_FIELD_ACTION, "REFRESH"));
        } catch (Exception e) {
            log.error("Failed to send notification for reservation {} ", reservation.getId(), e);
        }
    }

    private void enqueue(String routingKey, String eventType, String aggregateId, Object payload) {
        try {
            outboxRepository.save(Outbox.builder()
                    .aggregateType(AGGREGATE_BOOKING)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .exchange(RabbitMQConstants.EXCHANGE_NOTIFICATION)
                    .routingKey(routingKey)
                    .payload(objectMapper.writeValueAsString(payload))
                    .build());
            log.info("Enqueued outbox event {} for {}", eventType, aggregateId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize {} event for reservation {}", eventType, aggregateId, e);
        }
    }

    public void sendSeatHold(Long showtimeId, Long userId, List<Long> allShowtimeSeatIds) {
        List<SseDTO> seatInfo = reservationRedisService.getLockedSeatsByShowtime(showtimeId, allShowtimeSeatIds);
        Map<String, Object> event = Map.of(
                Constants.SSE_FIELD_EVENT, Constants.SSE_SEAT_HOLD,
                Constants.SSE_FIELD_SHOWTIME_ID, showtimeId,
                Constants.SSE_FIELD_USER_ID, userId,
                Constants.SSE_FIELD_SEAT_IDS, seatInfo
        );
        reservationRedisService.publishSeatEvent(showtimeId, event);
    }

    public void getSeatRelease(Long showtimeId, Long userId, List<Long> allShowtimeSeatIds) {
        List<SseDTO> seatInfo = reservationRedisService.getLockedSeatsByShowtime(showtimeId, allShowtimeSeatIds);
        Map<String, Object> event = Map.of(
                Constants.SSE_FIELD_EVENT, Constants.SSE_SEAT_RELEASE,
                Constants.SSE_FIELD_SHOWTIME_ID, showtimeId,
                Constants.SSE_FIELD_USER_ID, userId,
                Constants.SSE_FIELD_SEAT_IDS, seatInfo
        );
        reservationRedisService.publishSeatEvent(showtimeId, event);
    }

    public void sendSeatRelease(Long showtimeId, List<Long> seatIds, List<Long> allShowtimeSeatIds) {
        List<SseDTO> seatInfo = reservationRedisService.getLockedSeatsByShowtime(showtimeId, allShowtimeSeatIds);
        Map<String, Object> event = Map.of(
                Constants.SSE_FIELD_EVENT, Constants.SSE_SEAT_RELEASE,
                Constants.SSE_FIELD_SHOWTIME_ID, showtimeId,
                Constants.SSE_FIELD_SEAT_IDS, seatInfo
        );
        reservationRedisService.publishSeatEvent(showtimeId, event);
    }
}
