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

    static TheaterResponse map(final Theater theater, final List<Long> showtimeId) {
        return TheaterResponse
                .builder()
                .id(theater.getId())
                .name(theater.getName())
                .location(theater.getLocation())
                .showtime(showtimeId)
                .isDeleted(theater.getIsDeleted())
                .createdAt(theater.getCreatedAt())
                .updatedAt(theater.getUpdatedAt())
                .build();
    }
    static TheaterResponse map(final Theater theater){
        return map(theater, Collections.emptyList());
    }
}
