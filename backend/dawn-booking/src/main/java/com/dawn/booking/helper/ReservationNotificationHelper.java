package com.dawn.booking.helper;

import com.dawn.booking.dto.response.*;
import com.dawn.booking.model.Reservation;
import com.dawn.common.core.constant.Constants;
import com.dawn.booking.service.ReservationRedisService;
import com.dawn.booking.client.UserClientService;
import com.dawn.catalog.api.CatalogModuleApi;
import com.dawn.catalog.dto.response.MovieResponse;
import com.dawn.common.core.constant.RabbitMQConstants;
import com.dawn.common.core.dto.event.BookingCompleteEvent;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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

    RabbitTemplate rabbitTemplate;

    CatalogModuleApi catalogApi;

    UserClientService userService;

    ReservationRedisService reservationRedisService;

    public void handleNotification(Reservation reservation, ShowtimeDTO showtime, List<SeatDTO> seats) {
        try {

            UserDTO user = userService.findById(reservation.getUserId());
            log.info("Get user from reservation: {}", user);
            log.info("Get showtime from reservation: {}", showtime);
            MovieResponse movie = catalogApi.findMovieById(showtime.getMovieId());
            log.info("Get movie from reservation: {}", movie);
            String seatNumbers = seats.stream().map(SeatDTO::getSeatNumber).collect(Collectors.joining(","));

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
                    .reservationId(reservation.getId())
                    .movieName(movie.getTitle())
                    .theaterName(showtime.getTheaterName())
                    .showtimeSession(showtimeStr)
                    .seats(seatNumbers)
                    .paymentTime(paymentTimeStr)
                    .total(reservation.getTotalAmount().toString())
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.EXCHANGE_NOTIFICATION,
                    RabbitMQConstants.RK_NOTIFICATION_RESERVATION_COMPLETED,
                    event);

            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.EXCHANGE_NOTIFICATION,
                    RabbitMQConstants.RK_DASHBOARD_REFRESH,
                    Collections.singletonMap(Constants.SSE_FIELD_ACTION, "REFRESH"));
        } catch (Exception e) {
            log.error("Failed to send notification for reservation {} ", reservation.getId(), e);
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
        log.info("Get seat hold: {}", event);
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
        log.info("Get seat release: {}", event);
        reservationRedisService.publishSeatEvent(showtimeId, event);
    }

    public void sendSeatRelease(Long showtimeId, List<Long> seatIds, List<Long> allShowtimeSeatIds) {
        List<SseDTO> seatInfo = reservationRedisService.getLockedSeatsByShowtime(showtimeId, allShowtimeSeatIds);
        Map<String, Object> event = Map.of(
                Constants.SSE_FIELD_EVENT, Constants.SSE_SEAT_RELEASE,
                Constants.SSE_FIELD_SHOWTIME_ID, showtimeId,
                Constants.SSE_FIELD_SEAT_IDS, seatInfo
        );
        log.info("Publishing SEAT_RELEASE event: {}", event);
        reservationRedisService.publishSeatEvent(showtimeId, event);
    }
}
