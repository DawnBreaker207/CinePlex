package com.dawn.catalog.internal;

import com.dawn.catalog.dto.request.MovieRequest;
import com.dawn.catalog.dto.response.MovieResponse;
import com.dawn.catalog.helper.MovieMappingHelper;
import com.dawn.catalog.model.Genre;
import com.dawn.catalog.model.Movie;
import com.dawn.catalog.repository.GenreRepository;
import com.dawn.catalog.repository.MovieRepository;
import com.dawn.catalog.internal.impl.MovieServiceImpl;
import com.dawn.common.core.exception.wrapper.ResourceAlreadyExistedException;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.time.ZoneId;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MovieService Unit Tests")
public class MovieServiceTests {

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private GenreRepository genreRepository;

    @InjectMocks
    private MovieServiceImpl movieService;

    MockedStatic<MovieMappingHelper> mappingHelperMock;

    private Movie movie;

    private MovieRequest movieRequest;

    private Genre actionGenre;

    private Genre dramaGenre;

    @BeforeEach
    void setUp() {
        actionGenre = Genre.builder()
                .id(1L)
                .name("Action")
                .build();

        dramaGenre = Genre.builder()
                .id(2L)
                .name("Drama")
                .build();

        movie = Movie.builder()
                .id(1L)
                .filmId("12345")
                .title("Inception")
                .overview("A mind-bending thriller")
                .poster("inception.jpg")
                .duration(148)
                .releaseDate(new Date().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
                .language("en")
                .imdbId("tt1375666")
                .genres(Set.of(actionGenre, dramaGenre))
                .build();

        movieRequest = MovieRequest.builder()
                .filmId("12345")
                .title("Inception")
                .overview("A mind-bending thriller")
                .poster("inception.jpg")
                .duration(148)
                .releaseDate(new Date().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
                .language("en")
                .imdbId("tt1375666")
                .genres(Set.of("Action", "Drama"))
                .build();
        mappingHelperMock = Mockito.mockStatic(MovieMappingHelper.class);
    }

    @AfterEach
    void tearDown() {
        mappingHelperMock.close();
    }

    @Test
    void findAll_GivenFilteredMovie_WhenCalled_ThenReturnMovieList() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        MovieRequest filter = MovieRequest
                .builder()
                .title("Inception")
                .build();
        MovieResponse dto = MovieResponse
                .builder()
                .id(1L)
                .title("Inception")
                .build();
        mappingHelperMock
                .when(() -> MovieMappingHelper.map(movie))
                .thenReturn(dto);

        when(movieRepository
                .findAllWithFilter(filter, pageable))
                .thenReturn(new PageImpl<>(List.of(movie), pageable, 1));

        // Act
        List<MovieResponse> result = movieService
                .findAll(filter, pageable)
                .getContent();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(movie.getId(), result.getFirst().getId());
        assertEquals(filter.getTitle(), result.getFirst().getTitle());

        verify(movieRepository, times(1)).findAllWithFilter(filter, pageable);
    }

    @Test
    void findAll_GivenInvalidFilteredMovie_WhenCalled_ThenReturnEmptyList() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        MovieRequest filter = MovieRequest
                .builder()
                .build();
        when(movieRepository
                .findAllWithFilter(filter, pageable))
                .thenReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));

        // Act
        List<MovieResponse> result = movieService
                .findAll(filter, pageable)
                .getContent();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(movieRepository, times(1))
                .findAllWithFilter(filter, pageable);
    }

    @Test
    void findOne_GivenValidId_WhenFound_ThenReturnMovieDto() {
        // Arrange
        when(movieRepository
                .findById(1L))
                .thenReturn(Optional.of(movie));
        MovieResponse dto = MovieResponse
                .builder()
                .id(1L)
                .title("Inception")
                .filmId("12345")
                .build();
        mappingHelperMock
                .when(() -> MovieMappingHelper.map(movie))
                .thenReturn(dto);

        // Act
        MovieResponse result = movieService
                .findById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("Inception", result.getTitle());
        assertEquals("12345", result.getFilmId());

        verify(movieRepository, times(1))
                .findById(1L);
    }

    @Test
    void findOne_GivenNullId_WhenNotFound_ThenThrowResourceNotFoundException() {
        // Arrange
        when(movieRepository
                .findById(null))
                .thenReturn(Optional.of(movie));

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> movieService.findById(null));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(movieRepository, times(1))
                .findById(null);
    }

    @Test
    void findOne_GivenInvalidId_WhenNotFound_ThenThrowResourceNotFoundException() {
        // Arrange
        when(movieRepository
                .findById(999L))
                .thenReturn(Optional.of(movie));

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> movieService.findById(999L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(movieRepository, times(1))
                .findById(999L);
    }

    @Test
    void findByMovieId_GivenValidId_WhenFound_ThenReturnMovieDto() {
        // Arrange
        when(movieRepository
                .findByFilmId("12345"))
                .thenReturn(Optional.of(movie));
        MovieResponse dto = MovieResponse
                .builder()
                .id(1L)
                .filmId("12345")
                .build();
        mappingHelperMock
                .when(() -> MovieMappingHelper.map(movie))
                .thenReturn(dto);

        // Act
        MovieResponse result = movieService
                .findByMovieId("12345");

        // Assert
        assertNotNull(result);
        assertEquals("12345", result.getFilmId());
        verify(movieRepository, times(1))
                .findByFilmId("12345");
    }

    @Test
    void findByMovieId_GivenInvalidId_WhenNotFound_ThenThrowResourceNotFoundException() {
        // Arrange
        when(movieRepository
                .findByFilmId("99999"))
                .thenReturn(Optional.of(movie));

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> movieService.findByMovieId("99999"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(movieRepository, times(1))
                .findByFilmId("99999");
    }

    @Test
    void create_GivenNewGenres_WhenSuccess_ThenReturnMovieWithGenresSaved() {
        // Arrange
        when(movieRepository
                .findByFilmId("12345"))
                .thenReturn(Optional.empty());
        when(genreRepository
                .findAll())
                .thenReturn(List.of(actionGenre));
        when(genreRepository
                .save(any()))
                .thenReturn(dramaGenre);

        Movie mappedMovie = Movie
                .builder()
                .filmId("12345")
                .build();
        when(MovieMappingHelper
                .map(movieRequest))
                .thenReturn(mappedMovie);

        Movie savedMovie = Movie
                .builder()
                .id(1L)
                .filmId("12345")
                .build();
        when(movieRepository
                .save(mappedMovie))
                .thenReturn(savedMovie);

        MovieResponse dto = MovieResponse
                .builder()
                .id(1L)
                .filmId("12345")
                .build();
        mappingHelperMock
                .when(() -> MovieMappingHelper.map(savedMovie))
                .thenReturn(dto);

        // Act
        MovieResponse result = movieService
                .create(movieRequest);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(movieRepository, times(1))
                .findByFilmId("12345");
        verify(movieRepository, times(1))
                .save(mappedMovie);
        verify(genreRepository, times(1))
                .save(any());
    }

    @Test
    void create_GivenDuplicatedFilmId_WhenExists_ThenThrowResourceAlreadyExistedException() {
        // Arrange
        when(movieRepository
                .findByFilmId("12345"))
                .thenReturn(Optional.of(movie));

        // Act & Assert
        assertThrows(
                ResourceAlreadyExistedException.class,
                () -> movieService.create(movieRequest));

        verify(movieRepository, times(1))
                .findByFilmId("12345");
        verify(movieRepository, never())
                .save(any());
        verify(genreRepository, never())
                .save(any());
    }

    @Test
    void update_GivenValidIdAndNewGenres_WhenSuccess_ThenMovieUpdatedWithGenres() {
        // Arrange
        MovieRequest updateDto = MovieRequest
                .builder()
                .title("Update title")
                .genres(Set.of("Action", "Comedy"))
                .build();
        Genre comedy = Genre
                .builder()
                .id(1L)
                .name("Comedy")
                .build();

        when(movieRepository.findById(1L))
                .thenReturn(Optional.of(movie));
        when(genreRepository.findAll())
                .thenReturn(List.of(actionGenre));
        when(genreRepository.save(any()))
                .thenReturn(comedy);

        Movie savedMovie = Movie
                .builder()
                .id(1L)
                .filmId("Update title")
                .build();
        when(movieRepository.save(movie))
                .thenReturn(savedMovie);

        MovieResponse dto = MovieResponse
                .builder()
                .id(1L)
                .title("Update title")
                .build();
        mappingHelperMock
                .when(() -> MovieMappingHelper.map(savedMovie))
                .thenReturn(dto);

        // Act
        MovieResponse result = movieService
                .update(1L, updateDto);

        // Assert
        assertNotNull(result);
        assertEquals("Update title", result.getTitle());
        verify(movieRepository, times(1))
                .findById(1L);
        verify(movieRepository, times(1))
                .save(movie);
        verify(genreRepository, times(1))
                .save(any());
    }

    @Test
    void update_GivenInvalidId_WhenMovieNotFound_ThenThrowResourceNotFoundException() {
        // Arrange
        when(movieRepository
                .findById(2L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                ResourceNotFoundException.class,
                () -> movieService.update(2L, movieRequest));
        verify(movieRepository, times(1))
                .findById(2L);
        verify(movieRepository, never())
                .save(any());
        verify(genreRepository, never())
                .save(any());
    }

    @Test
    void delete_GivenValidId_WhenSuccess_ThenMovieDeleted() {
        // Arrange
        when(movieRepository
                .findById(1L))
                .thenReturn(Optional.of(movie));

        // Act
        movieService
                .delete(1L);

        // Assert
        verify(movieRepository, times(1))
                .findById(1L);
        verify(movieRepository, times(1))
                .deleteById(1L);
    }

    @Test
    void delete_GivenInvalidId_WhenMovieNotFound_ThenThrowResourceNotFoundException() {
        // Arrange
        when(movieRepository
                .findById(1L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                ResourceNotFoundException.class,
                () -> movieService.delete(1L));
        verify(movieRepository, times(1))
                .findById(1L);
        verify(movieRepository, never())
                .deleteById(any());
    }
}
