package com.dawn.booking.service;

import com.dawn.booking.helper.ReservationNotificationHelper;
import com.dawn.booking.model.Reservation;
import com.dawn.booking.repository.ReservationRepository;
import com.dawn.catalog.api.CatalogModuleApi;
import com.dawn.catalog.dto.response.MovieResponse;
import com.dawn.cinema.api.CinemaModuleApi;
import com.dawn.cinema.dto.response.ShowtimeResponse;
import com.dawn.common.core.constant.RabbitMQConstants;
import com.dawn.common.core.constant.ReservationStatus;
import com.dawn.common.core.dto.event.RefundRequestedEvent;
import com.dawn.common.core.dto.event.ShowtimeCanceledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ShowtimeCanceledListener {

    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;
    private final ReservationNotificationHelper notificationHelper;
    private final RabbitTemplate rabbitTemplate;
    private final CinemaModuleApi cinemaApi;
    private final CatalogModuleApi catalogApi;

    @RabbitListener(queues = RabbitMQConstants.QUEUE_CINEMA_SHOWTIME_CANCELED)
    public void handleCanceledShowtime(ShowtimeCanceledEvent event) {
        List<Reservation> reservations = reservationRepository
                .findByShowtimeIdAndReservationStatusIn(event.showtimeId(),
                        List.of(ReservationStatus.CONFIRMED, ReservationStatus.PENDING));
        if (reservations.isEmpty()) {
            return;
        }
        String movieTitle = "";
        String showtimeInfo = "showtime " + event.showtimeId();
        try {
            ShowtimeResponse showtime = cinemaApi.findShowtimeById(event.showtimeId());
            MovieResponse movie = catalogApi.findMovieById(showtime.getMovieId());
            movieTitle = movie.getTitle();
            showtimeInfo = LocalDateTime.of(showtime.getShowDate(), showtime.getShowTime())
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        } catch (RuntimeException e) {
            log.warn("Showtime info unavailable for canceled showtime {}", event.showtimeId(), e);
        }
        for (Reservation reservation : reservations) {
            try {
                if (reservation.getReservationStatus() == ReservationStatus.CONFIRMED) {
                    reservationService.forceCancel(reservation.getReservationCode());
                    rabbitTemplate.convertAndSend(
                            RabbitMQConstants.EXCHANGE_PAYMENT,
                            RabbitMQConstants.RK_PAYMENT_REFUND_REQUESTED,
                            RefundRequestedEvent.builder()
                                    .reservationCode(reservation.getReservationCode())
                                    .build());
                } else {
                    reservationService.cancel(reservation.getReservationCode());
                }
                notificationHelper.handleShowtimeCanceled(reservation, movieTitle, showtimeInfo);
            } catch (Exception e) {
                log.error("Failed to release reservation {} of canceled showtime {}",
                        reservation.getReservationCode(), event.showtimeId(), e);
            }
        }
    }
}
