package com.dawn.cinema.service;

import com.dawn.cinema.dto.request.SeatTemplateRequest;
import com.dawn.cinema.dto.response.SeatTemplateResponse;
import com.dawn.cinema.model.Room;
import com.dawn.cinema.model.SeatTemplate;
import com.dawn.cinema.repository.RoomRepository;
import com.dawn.cinema.repository.SeatTemplateRepository;
import com.dawn.cinema.service.impl.SeatLayoutServiceImpl;
import com.dawn.common.core.constant.ErrorCode;
import com.dawn.common.core.exception.ApiException;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SeatLayoutServiceImpl")
class SeatLayoutServiceImplTest {

    @Mock
    RoomRepository roomRepository;
    @Mock
    SeatTemplateRepository seatTemplateRepository;
    @Mock
    com.dawn.common.core.service.AuditLogService auditLogService;

    @InjectMocks
    SeatLayoutServiceImpl service;

    private Room buildRoom(Long id) {
        return Room.builder().id(id).theaterId(1L).name("R1").roomType("2D").totalSeats(0).isActive(true).build();
    }

    private SeatTemplate buildTemplate(Long id, String row, int num, String type) {
        return SeatTemplate.builder()
                .id(id).roomId(1L).rowLabel(row).seatNumber(num).seatType(type)
                .posX(0).posY(0).isActive(true).build();
    }

    @Test
    @DisplayName("findByRoomId → room missing throws ROOM_NOT_FOUND")
    void findByRoomId_roomNotFound_shouldThrow() {
        when(roomRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByRoomId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(ErrorCode.ROOM_NOT_FOUND.format());
    }

    @Test
    @DisplayName("addSeats → duplicate within request batch throws SEAT_TEMPLATE_DUPLICATE")
    void addSeats_duplicateInBatch_shouldThrow() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(buildRoom(1L)));
        when(seatTemplateRepository.findByRoomId(1L)).thenReturn(List.of());

        List<SeatTemplateRequest> requests = List.of(
                SeatTemplateRequest.builder().rowLabel("A").seatNumber(1).build(),
                SeatTemplateRequest.builder().rowLabel("a").seatNumber(1).build());

        assertThatThrownBy(() -> service.addSeats(1L, requests))
                .isInstanceOf(ApiException.class)
                .hasMessage(ErrorCode.SEAT_TEMPLATE_DUPLICATE.format("a", 1));
    }

    @Test
    @DisplayName("addSeats → duplicate against existing layout throws")
    void addSeats_duplicateWithExisting_shouldThrow() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(buildRoom(1L)));
        when(seatTemplateRepository.findByRoomId(1L))
                .thenReturn(List.of(buildTemplate(7L, "A", 1, "NORMAL")));

        List<SeatTemplateRequest> requests = List.of(
                SeatTemplateRequest.builder().rowLabel("A").seatNumber(1).build());

        assertThatThrownBy(() -> service.addSeats(1L, requests))
                .isInstanceOf(ApiException.class)
                .hasMessage(ErrorCode.SEAT_TEMPLATE_DUPLICATE.format("A", 1));
    }

    @Test
    @DisplayName("addSeats → saves seats, defaults NORMAL/0, syncs room.totalSeats")
    void addSeats_valid_shouldSaveAndSyncTotalSeats() {
        Room room = buildRoom(1L);
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(seatTemplateRepository.findByRoomId(1L)).thenReturn(List.of(), List.of(
                buildTemplate(101L, "A", 1, "NORMAL"), buildTemplate(102L, "A", 2, "VIP")));
        when(seatTemplateRepository.saveAll(anyList())).thenAnswer(inv ->
                inv.<List<SeatTemplate>>getArgument(0).stream()
                        .map(t -> SeatTemplate.builder()
                                .id(100L + t.getSeatNumber()).roomId(t.getRoomId())
                                .rowLabel(t.getRowLabel()).seatNumber(t.getSeatNumber())
                                .seatType(t.getSeatType()).posX(t.getPosX()).posY(t.getPosY())
                                .isActive(true).build())
                        .toList());

        List<SeatTemplateResponse> responses = service.addSeats(1L, List.of(
                SeatTemplateRequest.builder().rowLabel("A").seatNumber(1).build(),
                SeatTemplateRequest.builder().rowLabel("A").seatNumber(2).seatType("VIP").posX(3).posY(4).build()));

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getSeatType()).isEqualTo("NORMAL");
        assertThat(responses.get(1).getSeatType()).isEqualTo("VIP");
        assertThat(responses.get(1).getPosX()).isEqualTo(3);
        assertThat(room.getTotalSeats()).isEqualTo(2);
        verify(roomRepository).save(room);
    }

    @Test
    @DisplayName("addSeats → invalid seat type throws SEAT_TYPE_INVALID")
    void addSeats_invalidSeatType_shouldThrow() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(buildRoom(1L)));
        when(seatTemplateRepository.findByRoomId(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.addSeats(1L, List.of(
                SeatTemplateRequest.builder().rowLabel("A").seatNumber(1).seatType("LOUNGE").build())))
                .isInstanceOf(ApiException.class)
                .hasMessage(ErrorCode.SEAT_TYPE_INVALID.format("LOUNGE"));
    }

    @Test
    @DisplayName("update → moving to occupied position throws SEAT_TEMPLATE_DUPLICATE")
    void update_occupiedPosition_shouldThrow() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(buildRoom(1L)));
        when(seatTemplateRepository.findById(5L)).thenReturn(Optional.of(buildTemplate(5L, "A", 1, "NORMAL")));
        when(seatTemplateRepository.findByRoomId(1L))
                .thenReturn(List.of(buildTemplate(5L, "A", 1, "NORMAL"), buildTemplate(6L, "B", 1, "NORMAL")));

        assertThatThrownBy(() -> service.update(1L, 5L,
                SeatTemplateRequest.builder().rowLabel("B").seatNumber(1).build()))
                .isInstanceOf(ApiException.class)
                .hasMessage(ErrorCode.SEAT_TEMPLATE_DUPLICATE.format("B", 1));
    }

    @Test
    @DisplayName("update → same position is allowed, updates type/pos")
    void update_samePosition_shouldUpdate() {
        Room room = buildRoom(1L);
        SeatTemplate template = buildTemplate(5L, "A", 1, "NORMAL");
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(seatTemplateRepository.findById(5L)).thenReturn(Optional.of(template));
        when(seatTemplateRepository.save(any(SeatTemplate.class))).thenReturn(template);

        SeatTemplateResponse response = service.update(1L, 5L,
                SeatTemplateRequest.builder().rowLabel("A").seatNumber(1).seatType("VIP").posX(9).posY(9).build());

        assertThat(response.getSeatType()).isEqualTo("VIP");
        assertThat(response.getPosX()).isEqualTo(9);
        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    @DisplayName("remove → soft delete + sync totalSeats")
    void remove_shouldSoftDeleteAndSync() {
        Room room = buildRoom(1L);
        SeatTemplate template = buildTemplate(5L, "A", 1, "NORMAL");
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(seatTemplateRepository.findById(5L)).thenReturn(Optional.of(template));
        when(seatTemplateRepository.findByRoomId(1L)).thenReturn(List.of(template));

        service.remove(1L, 5L);

        assertThat(template.getIsActive()).isFalse();
        assertThat(room.getTotalSeats()).isZero();
        verify(seatTemplateRepository).save(template);
    }

    @Test
    @DisplayName("replaceLayout → soft-deletes old, inserts new, syncs totalSeats")
    void replaceLayout_shouldReplaceAndSync() {
        Room room = buildRoom(1L);
        SeatTemplate old1 = buildTemplate(5L, "A", 1, "NORMAL");
        SeatTemplate old2 = buildTemplate(6L, "A", 2, "NORMAL");
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(seatTemplateRepository.findByRoomId(1L)).thenReturn(
                List.of(old1, old2), List.of(SeatTemplate.builder().id(201L).roomId(1L).rowLabel("B").seatNumber(1).build()));
        when(seatTemplateRepository.saveAll(anyList())).thenAnswer(inv ->
                inv.<List<SeatTemplate>>getArgument(0).stream()
                        .map(t -> SeatTemplate.builder()
                                .id(200L + t.getSeatNumber()).roomId(t.getRoomId())
                                .rowLabel(t.getRowLabel()).seatNumber(t.getSeatNumber())
                                .seatType(t.getSeatType()).posX(t.getPosX()).posY(t.getPosY())
                                .isActive(true).build())
                        .toList());

        List<SeatTemplateResponse> responses = service.replaceLayout(1L, List.of(
                SeatTemplateRequest.builder().rowLabel("B").seatNumber(1).build()));

        assertThat(old1.getIsActive()).isFalse();
        assertThat(old2.getIsActive()).isFalse();
        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().getRowLabel()).isEqualTo("B");
        assertThat(room.getTotalSeats()).isEqualTo(1);
    }
}