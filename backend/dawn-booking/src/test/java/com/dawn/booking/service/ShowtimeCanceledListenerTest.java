package com.dawn.booking.service;

import com.dawn.booking.helper.ReservationNotificationHelper;
import com.dawn.booking.model.Reservation;
import com.dawn.booking.repository.ReservationRepository;
import com.dawn.catalog.api.CatalogModuleApi;
import com.dawn.catalog.dto.response.MovieResponse;
import com.dawn.cinema.api.CinemaModuleApi;
import com.dawn.cinema.dto.response.ShowtimeResponse;
import com.dawn.common.core.constant.ReservationStatus;
import com.dawn.common.core.dto.event.ShowtimeCanceledEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShowtimeCanceledListener")
class ShowtimeCanceledListenerTest {

    @Mock
    ReservationRepository reservationRepository;
    @Mock
    ReservationService reservationService;
    @Mock
    ReservationNotificationHelper notificationHelper;
    @Mock
    RabbitTemplate rabbitTemplate;
    @Mock
    CinemaModuleApi cinemaApi;
    @Mock
    CatalogModuleApi catalogApi;

    @InjectMocks
    ShowtimeCanceledListener listener;

    private static Reservation reservation(String code, ReservationStatus status) {
        Reservation r = Reservation.builder().reservationCode(code).userId(1L).showtimeId(10L).build();
        r.setReservationStatus(status);
        return r;
    }

    private void stubShowtimeInfo() {
        ShowtimeResponse showtime = ShowtimeResponse.builder().id(10L).movieId(1L)
                .showDate(LocalDate.now()).showTime(LocalTime.of(19, 0)).build();
        when(cinemaApi.findShowtimeById(10L)).thenReturn(showtime);
        when(catalogApi.findMovieById(1L)).thenReturn(MovieResponse.builder().id(1L).title("M").build());
    }

    @Nested
    @DisplayName("handleCanceledShowtime")
    class Handle {

        @Test
        @DisplayName("PENDING → cancel, no refund event")
        void pending_cancelNoRefund() {
            when(reservationRepository.findByShowtimeIdAndReservationStatusIn(10L,
                    List.of(ReservationStatus.CONFIRMED, ReservationStatus.PENDING)))
                    .thenReturn(List.of(reservation("CP-1", ReservationStatus.PENDING)));
            stubShowtimeInfo();

            listener.handleCanceledShowtime(ShowtimeCanceledEvent.builder().showtimeId(10L).build());

            verify(reservationService).cancel("CP-1");
            verify(reservationService, never()).forceCancel(anyString());
            verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), (Object) any());
            verify(notificationHelper).handleShowtimeCanceled(any(), any(), any());
        }

        @Test
        @DisplayName("CONFIRMED → forceCancel + refund request + mail")
        void confirmed_forceCancelRefundMail() {
            when(reservationRepository.findByShowtimeIdAndReservationStatusIn(10L,
                    List.of(ReservationStatus.CONFIRMED, ReservationStatus.PENDING)))
                    .thenReturn(List.of(reservation("CP-2", ReservationStatus.CONFIRMED)));
            stubShowtimeInfo();

            listener.handleCanceledShowtime(ShowtimeCanceledEvent.builder().showtimeId(10L).build());

            verify(reservationService).forceCancel("CP-2");
            verify(rabbitTemplate).convertAndSend(anyString(), anyString(), (Object) any());
            verify(notificationHelper).handleShowtimeCanceled(any(), any(), any());
        }

        @Test
        @DisplayName("no reservations → nothing happens")
        void empty_nothing() {
            when(reservationRepository.findByShowtimeIdAndReservationStatusIn(10L,
                    List.of(ReservationStatus.CONFIRMED, ReservationStatus.PENDING)))
                    .thenReturn(List.of());

            listener.handleCanceledShowtime(ShowtimeCanceledEvent.builder().showtimeId(10L).build());

            verify(reservationService, never()).cancel(anyString());
            verify(notificationHelper, never()).handleShowtimeCanceled(any(), any(), any());
        }
    }
}
