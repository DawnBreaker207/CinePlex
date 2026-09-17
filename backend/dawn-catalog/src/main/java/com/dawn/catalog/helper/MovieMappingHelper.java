package com.dawn.catalog.helper;


import com.dawn.catalog.dto.request.MovieRequest;
import com.dawn.catalog.dto.response.MovieResponse;
import com.dawn.catalog.model.Genre;
import com.dawn.catalog.model.Movie;

import java.util.stream.Collectors;

public interface MovieMappingHelper {
    static Movie map(final MovieRequest m) {
        return Movie.builder()
                .title(m.getTitle())
                .originalTitle(m.getOriginalTitle())
                .poster(m.getPoster())
                .backdrop(m.getBackdrop())
                .overview(m.getOverview())
                .duration(m.getDuration())
                .releaseDate(m.getReleaseDate())
                .imdbId(m.getImdbId())
                .filmId(m.getFilmId())
                .language(m.getLanguage())
                .country(m.getCountry())
                .build();
    }

    static MovieResponse map(final Movie m) {
        return MovieResponse.builder()
                .id(m.getId())
                .title(m.getTitle())
                .originalTitle(m.getOriginalTitle())
                .poster(m.getPoster())
                .backdrop(m.getBackdrop())
                .overview(m.getOverview())
                .duration(m.getDuration())
                .genres(m
                        .getGenres()
                        .stream()
                        .map(Genre::getName)
                        .collect(Collectors.toSet()))
                .releaseDate(m.getReleaseDate())
                .imdbId(m.getImdbId())
                .filmId(m.getFilmId())
                .language(m.getLanguage())
                .country(m.getCountry())
                .isActive(m.getIsActive())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }
}
