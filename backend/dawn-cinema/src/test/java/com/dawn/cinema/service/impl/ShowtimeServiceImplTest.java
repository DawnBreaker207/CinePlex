package com.dawn.cinema.service.impl;

import com.dawn.catalog.api.CatalogModuleApi;
import com.dawn.catalog.dto.response.MovieResponse;
import com.dawn.cinema.dto.request.ShowtimeRequest;
import com.dawn.cinema.model.Room;
import com.dawn.cinema.model.Showtime;
import com.dawn.cinema.model.Theater;
import com.dawn.cinema.repository.RoomRepository;
import com.dawn.cinema.repository.SeatInstanceRepository;
import com.dawn.cinema.repository.SeatTemplateRepository;
import com.dawn.cinema.repository.ShowtimeRepository;
import com.dawn.cinema.repository.TheaterRepository;
import com.dawn.common.core.exception.wrapper.InvalidRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShowtimeServiceImpl overlap")
class ShowtimeServiceImplTest {

    @Mock
    ShowtimeRepository showtimeRepository;
    @Mock
    CatalogModuleApi catalogModuleApi;
    @Mock
    TheaterRepository theaterRepository;
    @Mock
    RoomRepository roomRepository;
    @Mock
    SeatTemplateRepository seatTemplateRepository;
    @Mock
    SeatInstanceRepository seatInstanceRepository;
    @Mock
    RabbitTemplate rabbitTemplate;

    @InjectMocks
    ShowtimeServiceImpl service;

    private static final LocalDate DAY = LocalDate.of(2026, 10, 1);

    private static MovieResponse movie(Long id, Integer duration) {
        return MovieResponse.builder().id(id).title("M" + id).duration(duration).build();
    }

    private static Showtime show(Long id, Long roomId, Long movieId, LocalTime start) {
        return Showtime.builder().id(id).roomId(roomId).movieId(movieId)
                .showDate(DAY).showTime(start).price(BigDecimal.valueOf(100000)).build();
    }

    private void stubCatalog() {
        when(theaterRepository.findById(5L))
                .thenReturn(Optional.of(Theater.builder().id(5L).name("T").build()));
        when(roomRepository.findByTheaterId(5L))
                .thenReturn(List.of(Room.builder().id(9L).theaterId(5L).name("R1").build()));
        when(catalogModuleApi.findMovieById(1L)).thenReturn(movie(1L, 120));
        when(catalogModuleApi.findMoviesByIds(List.of(2L))).thenReturn(List.of(movie(2L, 120)));
    }

    private static ShowtimeRequest request(LocalTime start) {
        return ShowtimeRequest.builder()
                .movieId(1L).theaterId(5L).showDate(DAY).showTime(start)
                .price(BigDecimal.valueOf(100000)).build();
    }

    @Nested
    @DisplayName("add")
    class Add {

        @Test
        @DisplayName("overlapping showtime in same room → throw")
        void overlapping_throws() {
            stubCatalog();
            when(showtimeRepository.findByRoomIdAndShowDate(9L, DAY))
                    .thenReturn(List.of(show(55L, 9L, 2L, LocalTime.of(10, 0))));

            assertThatThrownBy(() -> service.add(request(LocalTime.of(11, 0))))
                    .isInstanceOf(InvalidRequestException.class);
            verify(showtimeRepository, never()).save(any());
        }

        @Test
        @DisplayName("back-to-back (end == start) → allowed")
        void backToBack_allowed() {
            stubCatalog();
            when(showtimeRepository.findByRoomIdAndShowDate(9L, DAY))
                    .thenReturn(List.of(show(55L, 9L, 2L, LocalTime.of(10, 0))));
            when(showtimeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(seatTemplateRepository.findByRoomId(9L)).thenReturn(List.of());

            service.add(request(LocalTime.of(12, 0)));

            verify(showtimeRepository).save(any());
        }
    }

    @Nested
    @DisplayName("cancelShowtime")
    class CancelShowtime {

        @Test
        @DisplayName("active → CANCELED + event published")
        void active_cancelsAndPublishes() {
            Showtime show = show(100L, 9L, 1L, LocalTime.of(10, 0));
            show.setStatus("ACTIVE");
            when(showtimeRepository.findById(100L)).thenReturn(Optional.of(show));
            when(catalogModuleApi.findMovieById(1L)).thenReturn(movie(1L, 120));
            when(showtimeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var res = service.cancelShowtime(100L);

            assertThat(res).isNotNull();
            assertThat(show.getStatus()).isEqualTo("CANCELED");
            verify(rabbitTemplate).convertAndSend(
                    eq("cinema.exchange"), eq("cinema.showtime.canceled"), (Object) any());
        }

        @Test
        @DisplayName("already CANCELED → idempotent, no republish")
        void alreadyCanceled_idempotent() {
            Showtime show = show(100L, 9L, 1L, LocalTime.of(10, 0));
            show.setStatus("CANCELED");
            when(showtimeRepository.findById(100L)).thenReturn(Optional.of(show));
            when(catalogModuleApi.findMovieById(1L)).thenReturn(movie(1L, 120));

            service.cancelShowtime(100L);

            verify(showtimeRepository, never()).save(any());
            verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), (Object) any());
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("own row excluded → no false overlap")
        void selfExcluded_ok() {
            Showtime self = show(100L, 9L, 1L, LocalTime.of(10, 0));
            when(showtimeRepository.findById(100L)).thenReturn(Optional.of(self));
            when(catalogModuleApi.findMovieById(1L)).thenReturn(movie(1L, 120));
            when(showtimeRepository.findByRoomIdAndShowDate(9L, DAY)).thenReturn(List.of(self));
            when(showtimeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var res = service.update(100L, ShowtimeRequest.builder().movieId(1L).build());

            assertThat(res).isNotNull();
            verify(showtimeRepository).save(any());
        }
    }
}
