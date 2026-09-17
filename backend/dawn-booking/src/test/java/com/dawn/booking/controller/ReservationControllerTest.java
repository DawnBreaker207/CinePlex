package com.dawn.booking.controller;

import com.dawn.booking.dto.request.ReservationUserRequest;
import com.dawn.booking.service.ReservationService;
import com.dawn.booking.service.SeatHoldService;
import com.dawn.booking.service.VoucherApplicationService;
import com.dawn.booking.api.BookingModuleApi;
import com.dawn.common.core.model.AuthenticatedUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationController")
class ReservationControllerTest {

    @Mock
    ReservationService reservationService;
    @Mock
    SeatHoldService seatHoldService;
    @Mock
    VoucherApplicationService voucherApplicationService;
    @Mock
    BookingModuleApi bookingApi;

    @InjectMocks
    ReservationController controller;

    @Nested
    @DisplayName("getAllByUser")
    class GetAllByUser {

        @Test
        @DisplayName("userId param overridden by authenticated user")
        void userIdOverriddenByPrincipal() {
            ReservationUserRequest request = new ReservationUserRequest();
            request.setUserId(999L);
            AuthenticatedUser principal = mock(AuthenticatedUser.class);
            when(principal.getId()).thenReturn(7L);
            Authentication authentication = mock(Authentication.class);
            when(authentication.getPrincipal()).thenReturn(principal);

            controller.getAllByUser(request, Pageable.unpaged(), authentication);

            org.assertj.core.api.Assertions.assertThat(request.getUserId()).isEqualTo(7L);
            verify(reservationService).findByUser(any(), any());
        }
    }
}
