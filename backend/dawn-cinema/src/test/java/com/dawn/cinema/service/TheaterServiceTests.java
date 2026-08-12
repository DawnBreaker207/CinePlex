package com.dawn.cinema.service;

import com.dawn.cinema.dto.request.TheaterRequest;
import com.dawn.cinema.dto.response.TheaterResponse;
import com.dawn.cinema.helper.TheaterMappingHelper;
import com.dawn.cinema.model.Theater;
import com.dawn.cinema.repository.TheaterRepository;
import com.dawn.cinema.service.impl.TheaterServiceImpl;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
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

    @InjectMocks
    private TheaterServiceImpl theaterService;

    private MockedStatic<TheaterMappingHelper> mappingHelperMock;

    private Theater theater;

    private TheaterRequest theaterRequest;

    private TheaterResponse theaterResponse;

    @BeforeEach
    void setUp() {
        theater = Theater.builder()
                .id(1L)
                .name("Grand Cinema")
                .location("HaNoi")
                .capacity(50)
                .build();

        theaterRequest = TheaterRequest
                .builder()
                .name("Grand Cinema")
                .location("HaNoi")
                .capacity(50)
                .build();

        theaterResponse = TheaterResponse
                .builder()
                .name("Grand Cinema")
                .location("HaNoi")
                .capacity(50)
                .build();
        mappingHelperMock = Mockito.mockStatic(TheaterMappingHelper.class);
    }

    @AfterEach
    void tearDown() {
        mappingHelperMock.close();
    }

    @Test
    void findAll_GivenTheaterExist_WhenCalled_ThenReturnTheaterList() {
        // Arrange
        when(theaterRepository
                .findAll())
                .thenReturn(List.of(theater));

        // Act
        List<TheaterResponse> result = theaterService
                .findAll(Pageable.unpaged())
                .getContent();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());

        verify(theaterRepository, times(1))
                .findAll();
    }

    @Test
    void findAll_GivenNoTheaterExist_WhenCalled_ThenReturnEmptyList() {
        // Arrange
        when(theaterRepository
                .findAll())
                .thenReturn(Collections.emptyList());

        // Act
        List<TheaterResponse> result = theaterService
                .findAll(Pageable.unpaged())
                .getContent();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(theaterRepository, times(1))
                .findAll();
    }

    @Test
    void findOne_GivenValidId_WhenCalled_ThenReturnTheaterDto() {
        // Arrange
        when(theaterRepository
                .findById(1L))
                .thenReturn(Optional.of(theater));

        // Act
        TheaterResponse result = theaterService
                .findOne(1L);

        // Assert
        assertNotNull(result);
        assertEquals(theaterResponse.getName(), result.getName());
        verify(theaterRepository, times(1))
                .findById(1L);
    }

    @Test
    void findOne_GivenNullId_WhenNotFound_ThenThrowResourceNotFoundException() {
        // Arrange
        when(theaterRepository
                .findById(null))
                .thenReturn(Optional.of(theater));

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> theaterService.findOne(null));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(theaterRepository, times(1)).findById(null);
    }

    @Test
    void findOne_GivenInvalidId_WhenNotFound_ThenThrowResourceNotFoundException() {
        // Arrange
        when(theaterRepository
                .findById(999L))
                .thenReturn(Optional.of(theater));

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> theaterService.findOne(999L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(theaterRepository, times(1))
                .findById(999L);
    }

    @Test
    void findByLocation_GivenLocation_WhenCalled_ThenReturnTheaterDto() {
        // Arrange
        when(theaterRepository
                .findByLocationContainingIgnoreCase("HaNoi", Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(theater)));

        // Act
        List<TheaterResponse> result = theaterService
                .findByLocation("HaNoi", Pageable.unpaged())
                .getContent();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(theaterResponse.getName(), result.getFirst().getName());
        verify(theaterRepository, times(1))
                .findByLocationContainingIgnoreCase("HaNoi", Pageable.unpaged());
    }

    @Test
    void findByLocation_GivenInvalidLocation_WhenNotFound_ThenThrowResourceNotFoundException() {
        // Arrange
        when(theaterRepository
                .findByLocationContainingIgnoreCase("Unknown", Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(theater)));

        // Act
        List<TheaterResponse> result = theaterService
                .findByLocation("Unknown", Pageable.unpaged())
                .getContent();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(theaterRepository, times(1))
                .findByLocationContainingIgnoreCase("Unknown", Pageable.unpaged())
                .getContent();
    }

    @Test
    void create_WhenSuccess_THenReturnTheater() {
        // Arrange
        when(theaterRepository
                .save(any(Theater.class)))
                .thenReturn(theater);

        // Act
        TheaterResponse result = theaterService
                .create(theaterRequest);

        // Assert
        assertNotNull(result);
        assertEquals(theaterResponse.getName(), result.getName());
        verify(theaterRepository, times(1))
                .save(any(Theater.class));
    }

    @Test
    void update_GivenValidId_WhenCalled_ThenReturnUpdatedTheater() {
        // Arrange
        when(theaterRepository
                .findById(1L))
                .thenReturn(Optional.of(theater));

        // Act
        TheaterResponse result = theaterService
                .update(1L, theaterRequest);

        // Assert
        assertNotNull(result);
        assertEquals(theaterResponse.getName(), result.getName());
        verify(theaterRepository, times(1))
                .findById(1L);
        verify(theaterRepository, times(1))
                .save(theater);
    }

    @Test
    void update_GivenInvalidId_WhenNotFound_ThenThrowResourceNotFoundException() {
        // Arrange
        when(theaterRepository
                .findById(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> theaterService.update(999L, theaterRequest));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(theaterRepository, times(1))
                .findById(999L);
        verify(theaterRepository, never())
                .save(any());
    }

    @Test
    void remove_GivenValidId_WhenCalled_ThenDeleteTheater() {
        // Arrange
        when(theaterRepository
                .findById(1L))
                .thenReturn(Optional.of(theater));

        // Act
        theaterService
                .remove(1L);

        // Assert
        verify(theaterRepository, times(1))
                .findById(1L);
        verify(theaterRepository, times(1))
                .deleteById(1L);
    }

    @Test
    void remove_GivenInvalidId_WhenNotFound_ThenThrowResourceNotFoundException() {
        // Arrange
        when(theaterRepository
                .findById(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> theaterService.remove(999L));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(theaterRepository, times(1))
                .findById(999L);
        verify(theaterRepository, never())
                .deleteById(any());
    }
}
