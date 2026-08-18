package com.dawn.cinema.service;

import com.dawn.cinema.dto.request.RoomRequest;
import com.dawn.cinema.dto.response.RoomResponse;
import com.dawn.cinema.model.Room;
import com.dawn.cinema.model.SeatTemplate;
import com.dawn.cinema.repository.RoomRepository;
import com.dawn.cinema.repository.SeatTemplateRepository;
import com.dawn.cinema.repository.TheaterRepository;
import com.dawn.cinema.service.impl.RoomServiceImpl;
import com.dawn.common.core.constant.ErrorCode;
import com.dawn.common.core.exception.ApiException;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import com.dawn.cinema.model.Theater;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoomServiceImpl")
class RoomServiceImplTest {

    @Mock
    RoomRepository roomRepository;
    @Mock
    TheaterRepository theaterRepository;
    @Mock
    SeatTemplateRepository seatTemplateRepository;
    @Mock
    com.dawn.common.core.service.AuditLogService auditLogService;

    @InjectMocks
    RoomServiceImpl service;

    private Room buildRoom(Long id, String name) {
        return Room.builder().id(id).theaterId(1L).name(name).roomType("2D").totalSeats(0).isActive(true).build();
    }

    @Test
    @DisplayName("create → theater missing throws THEATER_NOT_FOUND")
    void create_theaterNotFound_shouldThrow() {
        when(theaterRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(99L, RoomRequest.builder().name("R1").build()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(ErrorCode.THEATER_NOT_FOUND.format());
    }

    @Test
    @DisplayName("create → duplicate name in same theater throws ROOM_NAME_EXISTS")
    void create_duplicateName_shouldThrow() {
        when(theaterRepository.findById(1L)).thenReturn(Optional.of(new Theater()));
        when(roomRepository.findByTheaterId(1L)).thenReturn(List.of(buildRoom(5L, "R1")));

        assertThatThrownBy(() -> service.create(1L, RoomRequest.builder().name("r1").build()))
                .isInstanceOf(ApiException.class)
                .hasMessage(ErrorCode.ROOM_NAME_EXISTS.format());
    }

    @Test
    @DisplayName("create → valid input saves room with defaults and returns response")
    void create_valid_shouldSave() {
        when(theaterRepository.findById(1L)).thenReturn(Optional.of(new Theater()));
        when(roomRepository.findByTheaterId(1L)).thenReturn(List.of());
        Room room = buildRoom(10L, "R1");
        when(roomRepository.save(any(Room.class))).thenReturn(room);
        when(seatTemplateRepository.findByRoomId(10L)).thenReturn(List.of());

        RoomResponse response = service.create(1L, RoomRequest.builder().name("  R1  ").build());

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getName()).isEqualTo("R1");
        assertThat(response.getRoomType()).isEqualTo("2D");
        assertThat(response.getTotalSeats()).isZero();
        assertThat(response.getSeatCount()).isZero();
    }

    @Test
    @DisplayName("update → rename to existing name in same theater throws")
    void update_duplicateName_shouldThrow() {
        Room room = buildRoom(10L, "R1");
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(roomRepository.findByTheaterId(1L))
                .thenReturn(List.of(buildRoom(10L, "R1"), buildRoom(11L, "R2")));

        assertThatThrownBy(() -> service.update(10L, RoomRequest.builder().name("R2").build()))
                .isInstanceOf(ApiException.class)
                .hasMessage(ErrorCode.ROOM_NAME_EXISTS.format());
    }

    @Test
    @DisplayName("update → same name keeps working (no self-conflict)")
    void update_sameName_shouldNotConflict() {
        Room room = buildRoom(10L, "R1");
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(roomRepository.save(any(Room.class))).thenReturn(room);
        when(seatTemplateRepository.findByRoomId(10L)).thenReturn(List.of());

        RoomResponse response = service.update(10L, RoomRequest.builder().name("R1").roomType("IMAX").build());

        assertThat(response.getName()).isEqualTo("R1");
        assertThat(response.getRoomType()).isEqualTo("IMAX");
    }

    @Test
    @DisplayName("remove → soft delete keeps row, sets isDeleted")
    void remove_shouldSoftDelete() {
        Room room = buildRoom(10L, "R1");
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));

        service.remove(10L);

        assertThat(room.getIsActive()).isFalse();
        verify(roomRepository).save(room);
    }

    @Test
    @DisplayName("findOne → soft-deleted room treated as not found")
    void findOne_deleted_shouldThrow() {
        Room room = buildRoom(10L, "R1");
        room.setIsActive(false);
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> service.findOne(10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(ErrorCode.ROOM_NOT_FOUND.format());
    }

    @Test
    @DisplayName("findByTheaterId → returns seatCount from active templates only")
    void findByTheaterId_shouldCountActiveTemplates() {
        Room room = buildRoom(10L, "R1");
        when(roomRepository.findByTheaterId(1L)).thenReturn(List.of(room));
        SeatTemplate active = SeatTemplate.builder().id(1L).isActive(true).build();
        SeatTemplate deleted = SeatTemplate.builder().id(2L).isActive(false).build();
        when(seatTemplateRepository.findByRoomId(10L)).thenReturn(List.of(active, deleted));

        List<RoomResponse> responses = service.findByTheaterId(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().getSeatCount()).isEqualTo(1);
    }
}