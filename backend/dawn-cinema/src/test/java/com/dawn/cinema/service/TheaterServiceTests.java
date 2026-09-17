package com.dawn.cinema.service;

import com.dawn.cinema.dto.request.TheaterRequest;
import com.dawn.cinema.dto.response.TheaterResponse;
import com.dawn.cinema.model.Theater;
import com.dawn.cinema.repository.ShowtimeRepository;
import com.dawn.cinema.repository.TheaterRepository;
import com.dawn.cinema.service.impl.TheaterServiceImpl;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TheaterService Unit Tests")
public class TheaterServiceTests {

    @Mock
    private TheaterRepository theaterRepository;

    @Mock
    private ShowtimeRepository showtimeRepository;

    @InjectMocks
    private TheaterServiceImpl theaterService;

    private Theater theater;

    private TheaterRequest theaterRequest;

    @BeforeEach
    void setUp() {
        theater = new Theater();
        theater.setId(1L);
        theater.setName("Grand Cinema");
        theater.setLocation("HaNoi");

        theaterRequest = TheaterRequest
                .builder()
                .name("Grand Cinema")
                .location("HaNoi")
                .build();
    }

    @Test
    void findAll_GivenTheaterExist_WhenCalled_ThenReturnTheaterList() {
        Pageable pageable = PageRequest.of(0, 10);
        when(theaterRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(theater), pageable, 1));
        when(showtimeRepository.findShowtimeByTheaterIds(List.of(1L)))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 10L}));

        List<TheaterResponse> result = theaterService
                .findAll(pageable)
                .getContent();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(List.of(10L), result.getFirst().getShowtime());
        verify(showtimeRepository, times(1)).findShowtimeByTheaterIds(List.of(1L));
    }

    @Test
    void findAll_GivenNoTheaterExist_WhenCalled_ThenReturnEmptyList() {
        Pageable pageable = PageRequest.of(0, 10);
        when(theaterRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));

        List<TheaterResponse> result = theaterService
                .findAll(pageable)
                .getContent();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(showtimeRepository, never()).findShowtimeByTheaterIds(anyList());
    }

    @Test
    void findOne_GivenValidId_WhenCalled_ThenReturnTheaterDto() {
        when(theaterRepository.findById(1L)).thenReturn(Optional.of(theater));
        when(showtimeRepository.findShowtimeByTheaterId(1L)).thenReturn(List.of(10L));

        TheaterResponse result = theaterService.findOne(1L);

        assertNotNull(result);
        assertEquals("Grand Cinema", result.getName());
        assertEquals(List.of(10L), result.getShowtime());
        verify(theaterRepository, times(1)).findById(1L);
    }

    @Test
    void findOne_GivenInvalidId_WhenNotFound_ThenThrowResourceNotFoundException() {
        when(theaterRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> theaterService.findOne(999L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void findByLocation_GivenLocation_WhenCalled_ThenReturnTheaterDto() {
        Pageable pageable = PageRequest.of(0, 10);
        when(theaterRepository.findByLocationContainingIgnoreCase("HaNoi", pageable))
                .thenReturn(new PageImpl<>(List.of(theater), pageable, 1));
        when(showtimeRepository.findShowtimeByTheaterIds(List.of(1L)))
                .thenReturn(List.of());

        List<TheaterResponse> result = theaterService
                .findByLocation("HaNoi", pageable)
                .getContent();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Grand Cinema", result.getFirst().getName());
        verify(theaterRepository, times(1))
                .findByLocationContainingIgnoreCase("HaNoi", pageable);
    }

    @Test
    void create_WhenSuccess_ThenReturnTheater() {
        when(theaterRepository.save(any(Theater.class))).thenReturn(theater);

        TheaterResponse result = theaterService.create(theaterRequest);

        assertNotNull(result);
        assertEquals("Grand Cinema", result.getName());
        verify(theaterRepository, times(1)).save(any(Theater.class));
    }

    @Test
    void update_GivenValidId_WhenCalled_ThenReturnUpdatedTheater() {
        when(theaterRepository.findById(1L)).thenReturn(Optional.of(theater));
        when(theaterRepository.save(any(Theater.class))).thenReturn(theater);

        TheaterResponse result = theaterService.update(1L, theaterRequest);

        assertNotNull(result);
        assertEquals("Grand Cinema", result.getName());
        verify(theaterRepository, times(1)).findById(1L);
        verify(theaterRepository, times(1)).save(theater);
    }

    @Test
    void update_GivenInvalidId_WhenNotFound_ThenThrowResourceNotFoundException() {
        when(theaterRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> theaterService.update(999L, theaterRequest));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(theaterRepository, never()).save(any());
    }

    @Test
    void remove_GivenValidId_WhenCalled_ThenDeleteTheater() {
        when(theaterRepository.findById(1L)).thenReturn(Optional.of(theater));

        theaterService.remove(1L);

        verify(theaterRepository, times(1)).findById(1L);
        verify(theaterRepository, times(1)).deleteById(1L);
    }

    @Test
    void remove_GivenInvalidId_WhenNotFound_ThenThrowResourceNotFoundException() {
        when(theaterRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> theaterService.remove(999L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(theaterRepository, never()).deleteById(any());
    }
}