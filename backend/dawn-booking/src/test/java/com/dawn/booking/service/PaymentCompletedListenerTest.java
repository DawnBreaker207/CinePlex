package com.dawn.booking.service;

import com.dawn.booking.dto.response.ReservationDetailResponse;
import com.dawn.booking.dto.response.ReservationRedisDTO;
import com.dawn.common.core.constant.RabbitMQConstants;
import com.dawn.common.core.constant.ReservationStatus;
import com.dawn.common.core.dto.event.PaymentCompletedEvent;
import com.dawn.common.core.dto.event.PaymentFailedEvent;
import com.dawn.common.core.dto.event.RefundRequestedEvent;
import com.dawn.common.core.exception.ApiException;
import com.dawn.common.core.exception.wrapper.SeatUnavailableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentCompletedListener")
class PaymentCompletedListenerTest {

    @Mock
    ReservationService reservationService;

    @Mock
    ReservationRedisService reservationRedisService;

    @Mock
    RabbitTemplate rabbitTemplate;

    @InjectMocks
    PaymentCompletedListener listener;

    private static PaymentCompletedEvent completedEvent(String code) {
        return PaymentCompletedEvent.builder().eventId("e1").reservationCode(code).build();
    }

    private static PaymentCompletedEvent completedEvent(String code, List<Long> seatIds) {
        return PaymentCompletedEvent.builder().eventId("e1").reservationCode(code)
                .seatIds(seatIds).amount(BigDecimal.valueOf(200000)).build();
    }

    private static ReservationRedisDTO session(String code, List<Long> seatIds) {
        return ReservationRedisDTO.builder().id(code).userId(7L).showtimeId(1L)
                .theaterId(1L).seatsIds(seatIds).build();
    }

    private static PaymentFailedEvent failedEvent(String code) {
        return PaymentFailedEvent.builder().eventId("e1").reservationCode(code).reason("x").build();
    }

    private static ReservationDetailResponse detail(String code, ReservationStatus status) {
        return ReservationDetailResponse.builder().reservationCode(code).reservationStatus(status).build();
    }

    @Nested
    @DisplayName("onPaymentCompleted")
    class OnPaymentCompleted {

        @Test
        @DisplayName("EXPIRED row → skip without rethrow (no poison loop)")
        void expired_skips() {
            doThrow(new ApiException(HttpStatus.CONFLICT, "not confirmable"))
                    .when(reservationService).confirm("RES-1");
            when(reservationService.findDetailByCode("RES-1"))
                    .thenReturn(Optional.of(detail("RES-1", ReservationStatus.EXPIRED)));

            listener.onPaymentCompleted(completedEvent("RES-1"));

            verify(reservationService).confirm("RES-1");
        }

        @Test
        @DisplayName("missing row → skip without rethrow")
        void missing_skips() {
            doThrow(new ApiException(HttpStatus.CONFLICT, "not confirmable"))
                    .when(reservationService).confirm("RES-1");
            when(reservationService.findDetailByCode("RES-1")).thenReturn(Optional.empty());

            listener.onPaymentCompleted(completedEvent("RES-1"));

            verify(reservationService).confirm("RES-1");
        }

        @Test
        @DisplayName("PENDING row → rethrow for redelivery")
        void pending_rethrows() {
            doThrow(new ApiException(HttpStatus.CONFLICT, "not confirmable"))
                    .when(reservationService).confirm("RES-1");
            when(reservationService.findDetailByCode("RES-1"))
                    .thenReturn(Optional.of(detail("RES-1", ReservationStatus.PENDING)));

            assertThatThrownBy(() -> listener.onPaymentCompleted(completedEvent("RES-1")))
                    .isInstanceOf(ApiException.class);
        }

        @Test
        @DisplayName("FAILED row (C2) → settle: no rethrow, releases hold, requests refund")
        void failed_settlesWithRefund() {
            doThrow(new ApiException(HttpStatus.CONFLICT, "not confirmable"))
                    .when(reservationService).confirm("RES-1");
            when(reservationService.findDetailByCode("RES-1"))
                    .thenReturn(Optional.of(detail("RES-1", ReservationStatus.FAILED)));

            listener.onPaymentCompleted(completedEvent("RES-1"));

            verify(reservationService).fail("RES-1");
            ArgumentCaptor<RefundRequestedEvent> captor = ArgumentCaptor.forClass(RefundRequestedEvent.class);
            verify(rabbitTemplate).convertAndSend(
                    eq(RabbitMQConstants.EXCHANGE_PAYMENT),
                    eq(RabbitMQConstants.RK_PAYMENT_REFUND_REQUESTED),
                    captor.capture());
            assertThat(captor.getValue().reservationCode()).isEqualTo("RES-1");
        }

        @Test
        @DisplayName("PENDING + lock lost (S3) → settle, not hot-loop")
        void pending_lockLost_settles() {
            doThrow(new SeatUnavailableException("Seat B5 was held by another"))
                    .when(reservationService).confirm("RES-1");
            when(reservationService.findDetailByCode("RES-1"))
                    .thenReturn(Optional.of(detail("RES-1", ReservationStatus.PENDING)));

            listener.onPaymentCompleted(completedEvent("RES-1"));

            verify(reservationService).fail("RES-1");
            verify(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class));
        }

        @Test
        @DisplayName("PENDING + busy-processing → still transient, rethrows")
        void pending_processingBusy_rethrows() {
            doThrow(new ApiException(com.dawn.common.core.constant.ErrorCode.RESERVATION_PROCESSING,
                    HttpStatus.CONFLICT))
                    .when(reservationService).confirm("RES-1");
            when(reservationService.findDetailByCode("RES-1"))
                    .thenReturn(Optional.of(detail("RES-1", ReservationStatus.PENDING)));

            assertThatThrownBy(() -> listener.onPaymentCompleted(completedEvent("RES-1")))
                    .isInstanceOf(ApiException.class);
            verify(reservationService, never()).fail(any());
        }
    }

    @Nested
    @DisplayName("C4 seat reconciliation")
    class SeatReconciliation {

        @Test
        @DisplayName("seats changed after payment → fail + refund, confirm never runs")
        void seatsChanged_settlesWithRefund() {
            when(reservationRedisService.getReservationSession("RES-1"))
                    .thenReturn(session("RES-1", List.of(1L, 3L)));
            when(reservationService.findDetailByCode("RES-1"))
                    .thenReturn(Optional.of(detail("RES-1", ReservationStatus.PENDING)));

            listener.onPaymentCompleted(completedEvent("RES-1", List.of(1L, 2L)));

            verify(reservationService, never()).confirm(any());
            verify(reservationService).fail("RES-1");
            verify(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class));
        }

        @Test
        @DisplayName("same seats → confirms normally")
        void seatsMatch_confirms() {
            when(reservationRedisService.getReservationSession("RES-1"))
                    .thenReturn(session("RES-1", List.of(1L, 2L)));

            listener.onPaymentCompleted(completedEvent("RES-1", List.of(1L, 2L)));

            verify(reservationService).confirm("RES-1");
        }

        @Test
        @DisplayName("legacy event without snapshot → confirms as before")
        void noSnapshot_confirms() {
            listener.onPaymentCompleted(completedEvent("RES-1"));

            verify(reservationService).confirm("RES-1");
            verify(reservationRedisService, never()).getReservationSession(any());
        }
    }

    @Nested
    @DisplayName("onPaymentFailed")
    class OnPaymentFailed {

        @Test
        @DisplayName("CONFIRMED row → settle without rethrow (prompt release is a no-op + refund check)")
        void confirmed_skips() {
            doThrow(new ApiException(HttpStatus.CONFLICT, "cannot fail"))
                    .when(reservationService).fail("RES-1");
            when(reservationService.findDetailByCode("RES-1"))
                    .thenReturn(Optional.of(detail("RES-1", ReservationStatus.CONFIRMED)));

            listener.onPaymentFailed(failedEvent("RES-1"));

            verify(reservationService, org.mockito.Mockito.times(2)).fail("RES-1");
            verify(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class));
        }

        @Test
        @DisplayName("PENDING row → rethrow for redelivery")
        void pending_rethrows() {
            doThrow(new ApiException(HttpStatus.CONFLICT, "cannot fail"))
                    .when(reservationService).fail("RES-1");
            when(reservationService.findDetailByCode("RES-1"))
                    .thenReturn(Optional.of(detail("RES-1", ReservationStatus.PENDING)));

            assertThatThrownBy(() -> listener.onPaymentFailed(failedEvent("RES-1")))
                    .isInstanceOf(ApiException.class);
        }
    }
}
