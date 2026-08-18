package com.dawn.cinema.service.impl;

import com.dawn.cinema.dto.request.SeatRequest;
import com.dawn.cinema.dto.response.SeatResponse;
import com.dawn.cinema.model.Room;
import com.dawn.cinema.model.SeatInstance;
import com.dawn.cinema.model.SeatTemplate;
import com.dawn.cinema.model.Showtime;
import com.dawn.cinema.repository.RoomRepository;
import com.dawn.cinema.repository.SeatInstanceRepository;
import com.dawn.cinema.repository.SeatTemplateRepository;
import com.dawn.cinema.repository.ShowtimeRepository;
import com.dawn.common.core.constant.SeatStatus;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import com.dawn.common.core.exception.wrapper.SeatUnavailableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SeatServiceImpl")
class SeatServiceImplTest {

    @Mock
    SeatInstanceRepository seatInstanceRepository;

    @Mock
    SeatTemplateRepository seatTemplateRepository;

    @Mock
    RoomRepository roomRepository;

    @Mock
    ShowtimeRepository showtimeRepository;

    @InjectMocks
    SeatServiceImpl service;

    private SeatTemplate template(Long id) {
        return SeatTemplate.builder().id(id).roomId(1L).rowLabel("A").seatNumber(1).seatType("STANDARD").build();
    }

    private SeatInstance seat(Long id, Long showtimeId, Long templateId, SeatStatus status) {
        return SeatInstance.builder()
                .id(id)
                .showtimeId(showtimeId)
                .seatTemplateId(templateId)
                .status(status.name())
                .price(new BigDecimal("100000"))
                .build();
    }

    // ----------------------------------------------------------------
    // bookSeats (CAS)
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("bookSeats")
    class BookSeats {

        @Test
        @DisplayName("all rows matched → return count, no exception")
        void allMatched_shouldReturnCount() {
            when(seatInstanceRepository.bookSeats(10L, List.of(1L, 2L), "BOOKED", "RES-001")).thenReturn(2);

            int booked = service.bookSeats(10L, List.of(1L, 2L), "RES-001");

            assertThat(booked).isEqualTo(2);
        }

        @Test
        @DisplayName("row count mismatch → throw SeatUnavailableException")
        void mismatch_shouldThrow() {
            when(seatInstanceRepository.bookSeats(10L, List.of(1L, 2L), "BOOKED", "RES-001")).thenReturn(1);

            assertThatThrownBy(() -> service.bookSeats(10L, List.of(1L, 2L), "RES-001"))
                    .isInstanceOf(SeatUnavailableException.class);
        }

        @Test
        @DisplayName("empty seat list → throw")
        void emptySeats_shouldThrow() {
            assertThatThrownBy(() -> service.bookSeats(10L, List.of(), "RES-001"))
                    .isInstanceOf(SeatUnavailableException.class);
        }
    }

    // ----------------------------------------------------------------
    // unbookSeats
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("unbookSeats")
    class UnbookSeats {

        @Test
        @DisplayName("delegates to repository and returns affected rows")
        void shouldReturnAffectedRows() {
            when(seatInstanceRepository.unbookSeats("RES-001", List.of(1L))).thenReturn(1);

            assertThat(service.unbookSeats("RES-001", List.of(1L))).isEqualTo(1);
        }

        @Test
        @DisplayName("empty seat list → 0, no repository call")
        void emptySeats_shouldReturnZero() {
            assertThat(service.unbookSeats("RES-001", List.of())).isZero();
            verify(seatInstanceRepository, never()).unbookSeats(anyString(), anyList());
        }
    }

    // ----------------------------------------------------------------
    // saveAllSeat
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("saveAllSeat")
    class SaveAllSeat {

        @Test
        @DisplayName("update status + reservationId on existing seats")
        void shouldUpdateSeats() {
            SeatInstance existing = seat(1L, 10L, 5L, SeatStatus.AVAILABLE);
            when(seatInstanceRepository.findAllById(List.of(1L))).thenReturn(List.of(existing));

            service.saveAllSeat(List.of(SeatRequest.builder().id(1L).status(SeatStatus.BOOKED).reservationId("RES-001").build()));

            assertThat(existing.getStatus()).isEqualTo("BOOKED");
            assertThat(existing.getReservationId()).isEqualTo("RES-001");
            verify(seatInstanceRepository).saveAll(List.of(existing));
        }

        @Test
        @DisplayName("empty request → no repository call")
        void emptyRequest_shouldNotCallRepository() {
            service.saveAllSeat(List.of());
            verify(seatInstanceRepository, never()).saveAll(any());
        }
    }

    // ----------------------------------------------------------------
    // getByShowtime
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("getByShowtime")
    class GetByShowtime {

        @Test
        @DisplayName("seats exist → return mapped list")
        void existingSeats_shouldReturn() {
            Showtime showtime = Showtime.builder().id(10L).roomId(1L).build();
            when(showtimeRepository.findById(10L)).thenReturn(Optional.of(showtime));
            when(seatInstanceRepository.findAllByShowtimeId(10L)).thenReturn(List.of(seat(1L, 10L, 5L, SeatStatus.AVAILABLE)));
            when(seatTemplateRepository.findByRoomId(1L)).thenReturn(List.of(template(5L)));

            List<SeatResponse> results = service.getByShowtime(10L);

            assertThat(results).hasSize(1);
            assertThat(results.getFirst().getSeatNumber()).isEqualTo("A1");
        }

        @Test
        @DisplayName("no seats yet → auto-create from templates")
        void noSeats_shouldAutoCreate() {
            Showtime showtime = Showtime.builder().id(10L).roomId(1L).price(new BigDecimal("100000")).build();
            when(showtimeRepository.findById(10L)).thenReturn(Optional.of(showtime));
            when(seatInstanceRepository.findAllByShowtimeId(10L)).thenReturn(List.of());
            when(roomRepository.findById(1L)).thenReturn(Optional.of(new Room()));
            when(seatTemplateRepository.findByRoomId(1L)).thenReturn(List.of(template(5L)));
            when(seatInstanceRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            List<SeatResponse> results = service.getByShowtime(10L);

            assertThat(results).isNotEmpty();
            verify(seatInstanceRepository).saveAll(anyList());
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