package com.dawn.booking.helper;

import com.dawn.booking.dto.response.*;
import com.dawn.booking.model.Reservation;
import com.dawn.booking.service.MovieClientBookingService;
import com.dawn.booking.service.ReservationRedisService;
import com.dawn.booking.service.UserClientService;
import com.dawn.common.core.constant.RabbitMQConstants;
import com.dawn.common.core.dto.request.BookingNotificationEvent;
import com.dawn.notification.service.SseService;
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

    MovieClientBookingService movieService;

    UserClientService userService;

    ReservationRedisService reservationRedisService;

    public void handleNotification(Reservation reservation, ShowtimeDTO showtime, List<SeatDTO> seats) {
        try {

            UserDTO user = userService.findById(reservation.getUserId());
            log.info("Get user from reservation: {}", user);
            log.info("Get showtime from reservation: {}", showtime);
            MovieDTO movie = movieService.findOne(showtime.getMovieId());
            log.info("Get movie from reservation: {}", movie);
            String seatNumbers = seats.stream().map(SeatDTO::getSeatNumber).collect(Collectors.joining(","));

            String paymentTimeStr = LocalDateTime
                    .ofInstant(reservation.getCreatedAt(), ZoneId.of("Asia/Ho_Chi_Minh"))
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

            String showtimeStr = LocalDateTime
                    .of(showtime.getShowDate(), showtime.getShowTime())
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

            BookingNotificationEvent event = BookingNotificationEvent
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
                    RabbitMQConstants.EXCHANGE_NOTIFY,
                    RabbitMQConstants.ROUTING_KEY_NOTIFY,
                    event);

            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.EXCHANGE_NOTIFY,
                    RabbitMQConstants.ROUTING_KEY_DASHBOARD,
                    Collections.singletonMap("action", "REFRESH"));
        } catch (Exception e) {
            log.error("Failed to send notification for reservation {} ", reservation.getId(), e);
        }
    }

    public void sendSeatHold(Long showtimeId, Long userId) {
        List<SseDTO> seatInfo = reservationRedisService.getLockedSeatsByShowtime(showtimeId);
        Map<String, Object> event = Map.of(
                "event", "SEAT_HOLD",
                "showtimeId", showtimeId,
                "userId", userId,
                "seatIds", seatInfo
        );
        log.info("Get seat hold: {}", event);
        reservationRedisService.publishSeatEvent(showtimeId, event);
    }

    public void getSeatRelease(Long showtimeId, Long userId) {
        List<SseDTO> seatInfo = reservationRedisService.getLockedSeatsByShowtime(showtimeId);
        Map<String, Object> event = Map.of(
                "event", "SEAT_RELEASE",
                "showtimeId", showtimeId,
                "userId", userId,
                "seatIds", seatInfo
        );
        log.info("Get seat release: {}", event);
        reservationRedisService.publishSeatEvent(showtimeId, event);
    }

    public void sendSeatRelease(Long showtimeId, List<Long> seatIds) {
        Map<String, Object> event = Map.of(
                "event", "SEAT_RELEASE",
                "showtimeId", showtimeId,
                "seatIds", seatIds
        );
        log.info("Publishing SEAT_RELEASE event: {}", event);
        reservationRedisService.publishSeatEvent(showtimeId, event);
    }
}
