package com.dawn.ai.service;

import com.dawn.booking.dto.request.ReservationUserRequest;
import com.dawn.booking.service.ReservationService;
import com.dawn.catalog.dto.request.MovieRequest;
import com.dawn.catalog.dto.response.MovieResponse;
import com.dawn.catalog.internal.MovieService;
import com.dawn.report.dto.request.DashboardFilterRequest;
import com.dawn.report.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// Rule-based recommendations (no LLM): hot titles + genre affinity + freshness.
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private static final int CATALOG_PAGE_SIZE = 200;
    private static final int HISTORY_PAGE_SIZE = 50;
    private static final int HOT_LIST_DAYS = 30;
    private static final int FRESH_DAYS = 60;
    private static final int HOT_BASE_SCORE = 50;
    private static final int HOT_STEP = 10;
    private static final int GENRE_WEIGHT = 5;
    private static final int FRESH_BONUS = 10;
    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 20;

    private final MovieService movieService;
    private final DashboardService dashboardService;
    private final ReservationService reservationService;

    public List<MovieResponse> recommend(Long userId, Integer limit) {
        int take = (limit == null || limit < 1) ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        List<MovieResponse> catalog = movieService.findAll(new MovieRequest(), Pageable.ofSize(CATALOG_PAGE_SIZE))
                .getContent();
        if (catalog.isEmpty()) {
            return List.of();
        }
        Map<String, Integer> hotRank = hotRank();
        History history = userId == null ? History.empty() : loadHistory(userId, catalog);
        return catalog.stream()
                .filter(m -> m.getIsActive() != Boolean.FALSE)
                .filter(m -> !history.watched().contains(norm(m.getTitle())))
                .map(m -> Map.entry(m, score(m, hotRank, history.genres())))
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(take)
                .map(Map.Entry::getKey)
                .toList();
    }

    private int score(MovieResponse m, Map<String, Integer> hotRank, Map<String, Long> genres) {
        int score = 0;
        Integer rank = hotRank.get(norm(m.getTitle()));
        if (rank != null) {
            score += Math.max(0, HOT_BASE_SCORE - rank * HOT_STEP);
        }
        if (m.getGenres() != null) {
            for (String g : m.getGenres()) {
                score += genres.getOrDefault(g, 0L) * GENRE_WEIGHT;
            }
        }
        if (m.getReleaseDate() != null && !m.getReleaseDate().isBefore(LocalDate.now().minusDays(FRESH_DAYS))) {
            score += FRESH_BONUS;
        }
        return score;
    }

    private Map<String, Integer> hotRank() {
        try {
            List<String> names = dashboardService.getTopMovies(DashboardFilterRequest.builder()
                            .startDate(LocalDate.now().minusDays(HOT_LIST_DAYS))
                            .endDate(LocalDate.now())
                            .build()).stream()
                    .map(t -> norm(t.getMovieName()))
                    .toList();
            Map<String, Integer> rank = new HashMap<>();
            for (int i = 0; i < names.size(); i++) {
                rank.putIfAbsent(names.get(i), i);
            }
            return rank;
        } catch (RuntimeException e) {
            log.warn("Top-movies unavailable, recommending without hot score", e);
            return Map.of();
        }
    }

    private History loadHistory(Long userId, List<MovieResponse> catalog) {
        try {
            Map<String, MovieResponse> byTitle = new HashMap<>();
            for (MovieResponse m : catalog) {
                byTitle.putIfAbsent(norm(m.getTitle()), m);
            }
            ReservationUserRequest req = new ReservationUserRequest();
            req.setUserId(userId);
            Set<String> watched = new HashSet<>();
            Map<String, Long> genres = new HashMap<>();
            reservationService.findByUser(req, Pageable.ofSize(HISTORY_PAGE_SIZE)).getContent().stream()
                    .map(h -> byTitle.get(norm(h.getMovieTitle())))
                    .filter(m -> m != null)
                    .forEach(m -> {
                        watched.add(norm(m.getTitle()));
                        if (m.getGenres() != null) {
                            for (String g : m.getGenres()) {
                                genres.merge(g, 1L, Long::sum);
                            }
                        }
                    });
            return new History(watched, genres);
        } catch (RuntimeException e) {
            log.warn("Booking history unavailable for user {}, recommending without affinity", userId, e);
            return History.empty();
        }
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    private record History(Set<String> watched, Map<String, Long> genres) {
        static History empty() {
            return new History(Set.of(), Map.of());
        }
    }
}
