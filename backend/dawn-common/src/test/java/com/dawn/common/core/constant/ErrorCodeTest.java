package com.dawn.common.core.constant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCodeTest {

    @Test
    void format_noArgs_returnsTemplateVerbatim() {
        assertThat(ErrorCode.USER_CANNOT_UPDATE_SELF.format()).isEqualTo("You can't update yourself");
        assertThat(ErrorCode.MOVIE_NOT_FOUND.format()).isEqualTo("Movie not found");
    }

    @Test
    void format_withArgs_replacesPlaceholders() {
        assertThat(ErrorCode.RESERVATION_NOT_ENOUGH_SEATS.format(2, 5))
                .isEqualTo("Not enough available seats. Request: 2, Available: 5");
        assertThat(ErrorCode.SEAT_WRONG_SHOWTIME.format("A1, A2"))
                .isEqualTo("Seats A1, A2 do not belong to the requested showtime");
    }

    @Test
    void code_returnsEnumName() {
        assertThat(ErrorCode.RESERVATION_NOT_FOUND.code()).isEqualTo("RESERVATION_NOT_FOUND");
    }
}