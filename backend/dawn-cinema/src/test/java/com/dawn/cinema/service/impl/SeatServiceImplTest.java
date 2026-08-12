package com.dawn.cinema.service.impl;

import com.dawn.cinema.dto.request.SeatRequest;
import com.dawn.cinema.dto.response.SeatResponse;
import com.dawn.cinema.helper.SeatMappingHelper;
import com.dawn.cinema.model.Seat;
import com.dawn.cinema.model.Showtime;
import com.dawn.cinema.model.Theater;
import com.dawn.cinema.repository.SeatRepository;
import com.dawn.cinema.repository.ShowtimeRepository;
import com.dawn.common.core.constant.SeatStatus;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SeatServiceImpl")
class SeatServiceImplTest {

    @Mock
    SeatRepository seatRepository;

    @Mock
    ShowtimeRepository showtimeRepository;

    @InjectMocks
    SeatServiceImpl service;

    private Showtime createShowtime(Long id, Long theaterId, int capacity) {
        Theater theater = Theater.builder().id(theaterId).capacity(capacity).build();
        return Showtime.builder().id(id).theater(theater).totalSeats(capacity).availableSeats(capacity).build();
    }

    private Seat createSeat(Long id, String number, SeatStatus status, Showtime showtime) {
        return Seat.builder().id(id).seatNumber(number).status(status).showtime(showtime).build();
    }

    // ----------------------------------------------------------------
    // findByIdWithLock
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("findByIdWithLock")
    class FindByIdWithLock {

        @Test
        @DisplayName("call repository with PESSIMISTIC_WRITE lock")
        void findByIdWithLock_shouldCallRepository() {
            Showtime showtime = createShowtime(10L, 1L, 100);
            Seat seat = createSeat(1L, "A1", SeatStatus.AVAILABLE, showtime);
            when(seatRepository.findByIdWithLock(List.of(1L))).thenReturn(List.of(seat));

            List<SeatResponse> results = service.findByIdWithLock(List.of(1L));

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getSeatNumber()).isEqualTo("A1");
        }
    }

    // ----------------------------------------------------------------
    // findAllByShowtimeId
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("findAllByShowtimeId")
    class FindAllByShowtimeId {

        @Test
        @DisplayName("return list of seats")
        void shouldReturnSeats() {
            Showtime showtime = createShowtime(10L, 1L, 100);
            Seat seat = createSeat(1L, "A1", SeatStatus.AVAILABLE, showtime);
            when(seatRepository.findAllByShowtimeId(10L)).thenReturn(List.of(seat));

            List<SeatResponse> results = service.findAllByShowtimeId(10L);

            assertThat(results).hasSize(1);
        }

        @Test
        @DisplayName("no seats → empty list")
        void noSeats_shouldReturnEmpty() {
            when(seatRepository.findAllByShowtimeId(10L)).thenReturn(List.of());

            List<SeatResponse> results = service.findAllByShowtimeId(10L);

            assertThat(results).isEmpty();
        }
    }

    // ----------------------------------------------------------------
    // saveAllSeat
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("saveAllSeat")
    class SaveAllSeat {

        @Test
        @DisplayName("update status to BOOKED")
        void shouldUpdateSeatStatus() {
            Showtime showtime = createShowtime(10L, 1L, 100);
            Seat existing = Seat.builder().id(1L).seatNumber("A1").status(SeatStatus.AVAILABLE).reservationId(null).showtime(showtime).build();
            when(seatRepository.findAllById(List.of(1L))).thenReturn(List.of(existing));

            SeatRequest req = SeatRequest.builder().id(1L).status(SeatStatus.BOOKED).reservationId("RES-001").build();
            service.saveAllSeat(List.of(req));

            assertThat(existing.getStatus()).isEqualTo(SeatStatus.BOOKED);
            assertThat(existing.getReservationId()).isEqualTo("RES-001");
            verify(seatRepository).saveAll(List.of(existing));
        }

        @Test
        @DisplayName("empty request → don't call repository")
        void emptyRequest_shouldNotCallRepository() {
            service.saveAllSeat(List.of());
            verify(seatRepository, never()).saveAll(any());
        }
    }

    // ----------------------------------------------------------------
    // getByShowtime
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("getByShowtime")
    class GetByShowtime {

        @Test
        @DisplayName("seats exist → return list")
        void existingSeats_shouldReturn() {
            Showtime showtime = createShowtime(10L, 1L, 100);
            Seat seat = createSeat(1L, "A1", SeatStatus.AVAILABLE, showtime);
            when(showtimeRepository.findById(10L)).thenReturn(Optional.of(showtime));
            when(seatRepository.findByShowtime(showtime)).thenReturn(List.of(seat));

            List<SeatResponse> results = service.getByShowtime(10L);

            assertThat(results).hasSize(1);
            verify(seatRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("no seats yet → auto-create")
        void noSeats_shouldAutoCreate() {
            Showtime showtime = createShowtime(10L, 1L, 100);
            when(showtimeRepository.findById(10L)).thenReturn(Optional.of(showtime));
            when(seatRepository.findByShowtime(showtime)).thenReturn(List.of());
            when(seatRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            List<SeatResponse> results = service.getByShowtime(10L);

            assertThat(results).isNotEmpty();
        }

        @Test
        @DisplayName("showtime not found → throw")
        void showtimeNotFound_shouldThrow() {
            when(showtimeRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getByShowtime(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
