package com.dawn.ai.service;

import com.dawn.booking.dto.response.UserReservationResponse;
import com.dawn.booking.service.ReservationService;
import com.dawn.catalog.dto.response.MovieResponse;
import com.dawn.catalog.internal.MovieService;
import com.dawn.common.core.dto.response.ResponsePage;
import com.dawn.report.dto.response.TopMovieResponse;
import com.dawn.report.service.DashboardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationService")
class RecommendationServiceTest {

    @Mock
    MovieService movieService;
    @Mock
    DashboardService dashboardService;
    @Mock
    ReservationService reservationService;

    @InjectMocks
    RecommendationService service;

    private static MovieResponse movie(Long id, String title, Set<String> genres, LocalDate releaseDate) {
        return MovieResponse.builder()
                .id(id).title(title).genres(genres).releaseDate(releaseDate).isActive(true)
                .build();
    }

    private void stubCatalog(MovieResponse... movies) {
        List<MovieResponse> list = List.of(movies);
        when(movieService.findAll(any(), any()))
                .thenReturn(new ResponsePage<>(new PageImpl<>(list, PageRequest.of(0, 200), list.size())));
    }

    private void stubHot(String... names) {
        List<TopMovieResponse> top = java.util.Arrays.stream(names)
                .map(n -> TopMovieResponse.builder().movieName(n).ticketSold(10L).build())
                .toList();
        when(dashboardService.getTopMovies(any())).thenReturn(top);
    }

    @Nested
    @DisplayName("recommend")
    class Recommend {

        @Test
        @DisplayName("anonymous: hot titles first")
        void anonymous_hotFirst() {
            stubCatalog(
                    movie(1L, "Quiet Drama", Set.of("Drama"), LocalDate.now().minusDays(200)),
                    movie(2L, "Loud Action", Set.of("Action"), LocalDate.now().minusDays(200)));
            stubHot("Loud Action");

            List<MovieResponse> result = service.recommend(null, 5);

            assertThat(result).extracting(MovieResponse::getTitle)
                    .containsExactly("Loud Action", "Quiet Drama");
        }

        @Test
        @DisplayName("known user: excludes watched, boosts same genre")
        void knownUser_excludesWatchedBoostsGenre() {
            stubCatalog(
                    movie(1L, "Watched Action", Set.of("Action"), LocalDate.now().minusDays(200)),
                    movie(2L, "New Action", Set.of("Action"), LocalDate.now().minusDays(200)),
                    movie(3L, "New Drama", Set.of("Drama"), LocalDate.now().minusDays(200)));
            stubHot();
            when(reservationService.findByUser(any(), any())).thenReturn(
                    new ResponsePage<>(new PageImpl<>(
                            List.of(UserReservationResponse.builder().movieTitle("Watched Action").build()),
                            PageRequest.of(0, 50), 1)));

            List<MovieResponse> result = service.recommend(7L, 5);

            assertThat(result).extracting(MovieResponse::getTitle)
                    .containsExactly("New Action", "New Drama");
        }

        @Test
        @DisplayName("reporting down: falls back to catalog order")
        void reportingDown_fallback() {
            stubCatalog(
                    movie(1L, "Only Movie", Set.of("Drama"), LocalDate.now().minusDays(10)));
            when(dashboardService.getTopMovies(any())).thenThrow(new RuntimeException("db down"));

            List<MovieResponse> result = service.recommend(null, 5);

            assertThat(result).extracting(MovieResponse::getTitle)
                    .containsExactly("Only Movie");
        }

        @Test
        @DisplayName("empty catalog: empty result")
        void emptyCatalog_empty() {
            stubCatalog();

            assertThat(service.recommend(null, 5)).isEmpty();
        }

        @Test
        @DisplayName("limit is capped")
        void limitCapped() {
            stubCatalog(movie(1L, "A", Set.of(), LocalDate.now()));
            stubHot();

            assertThat(service.recommend(null, 500)).hasSize(1);
            assertThat(service.recommend(null, 0)).hasSize(1);
        }
    }
}
