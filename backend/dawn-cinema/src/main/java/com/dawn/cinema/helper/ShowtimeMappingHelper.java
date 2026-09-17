package com.dawn.cinema.helper;

import com.dawn.catalog.dto.response.MovieResponse;
import com.dawn.cinema.dto.request.ShowtimeRequest;
import com.dawn.cinema.dto.response.ShowtimeResponse;
import com.dawn.cinema.model.Showtime;
import com.dawn.cinema.model.Theater;

public interface ShowtimeMappingHelper {
    static Showtime map(final ShowtimeRequest showtime) {
        return Showtime
                .builder()
                .showDate(showtime.getShowDate())
                .showTime(showtime.getShowTime())
                .price(showtime.getPrice())
                .build();
    }

    static ShowtimeResponse map(final Showtime showtime, MovieResponse movie, Theater theater) {
        return ShowtimeResponse
                .builder()
                .id(showtime.getId())
                .movieId(movie.getId())
                .movieTitle(movie.getTitle())
                .moviePosterUrl(movie.getPoster())
                .theaterId(theater != null ? theater.getId() : null)
                .theaterName(theater != null ? theater.getName() : null)
                .theaterLocation(theater != null ? theater.getLocation() : null)
                .showDate(showtime.getShowDate())
                .showTime(showtime.getShowTime())
                .price(showtime.getPrice())
                .createdAt(showtime.getCreatedAt())
                .updatedAt(showtime.getUpdatedAt())
                .build();
    }
}
