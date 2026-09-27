package com.dawn.cinema.helper;

import com.dawn.cinema.dto.request.TheaterRequest;
import com.dawn.cinema.dto.response.TheaterResponse;
import com.dawn.cinema.model.Theater;

import java.util.Collections;
import java.util.List;

public interface TheaterMappingHelper {
    static Theater map(final TheaterRequest theater) {
        return Theater
                .builder()
                .name(theater.getName())
                .location(theater.getLocation())
                .build();
    }

    static TheaterResponse map(final Theater theater, final List<Long> showtimeId, final Integer capacity) {
        return TheaterResponse
                .builder()
                .id(theater.getId())
                .name(theater.getName())
                .location(theater.getLocation())
                .capacity(capacity)
                .showtime(showtimeId)
                .isActive(theater.getIsActive())
                .createdAt(theater.getCreatedAt())
                .updatedAt(theater.getUpdatedAt())
                .build();
    }
    static TheaterResponse map(final Theater theater, final List<Long> showtimeId) {
        return map(theater, showtimeId, 0);
    }
    static TheaterResponse map(final Theater theater){
        return map(theater, Collections.emptyList(), 0);
    }
}
