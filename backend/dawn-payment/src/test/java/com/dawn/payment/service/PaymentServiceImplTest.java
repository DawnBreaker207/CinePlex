package com.dawn.payment.service;

import com.dawn.common.core.constant.PaymentMethod;
import com.dawn.common.core.constant.PaymentStatus;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import com.dawn.payment.dto.request.PaymentRequest;
import com.dawn.payment.dto.request.PaymentUpdateRequest;
import com.dawn.payment.dto.response.PaymentHandlerResponse;
import com.dawn.payment.dto.response.PaymentResponse;
import com.dawn.payment.dto.response.ReservationDTO;
import com.dawn.payment.handler.PaymentHandler;
import com.dawn.payment.model.Payment;
import com.dawn.payment.repository.PaymentRepository;
import com.dawn.payment.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentServiceImpl")
class PaymentServiceImplTest {

    @Mock
    PaymentRepository paymentRepository;
    @Mock
    ReservationClientService reservationClientService;
    @Mock
    PaymentHandler vnpayHandler;
    @Mock
    PaymentHandler momoHandler;

    PaymentServiceImpl service;

    private static final String RES_ID = "RES-001";
    private static final String VNPAY = "VNPAY";
    private static final String MOMO = "MOMO";

    @BeforeEach
    void setUp() {
        // VNPAY handler supports "VNPAY", Momo supports "MOMO"
        when(vnpayHandler.supports(VNPAY)).thenReturn(true);
        when(vnpayHandler.supports(MOMO)).thenReturn(false);
        when(momoHandler.supports(MOMO)).thenReturn(true);
        when(momoHandler.supports(VNPAY)).thenReturn(false);

        service = new PaymentServiceImpl(
                List.of(vnpayHandler, momoHandler),
                paymentRepository,
                reservationClientService);
    }

    // ----------------------------------------------------------------
    // createPayment
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("createPayment")
    class CreatePayment {

        @Test
        @DisplayName("VNPAY — tạo payment PENDING, trả về URL")
        void createPayment_vnpay_shouldSavePendingAndReturnUrl() {
            when(vnpayHandler.createPaymentUrl(eq(RES_ID), anyInt(), anyString()))
                    .thenReturn("https://vnpay.test/pay?token=abc");

            PaymentRequest req = buildRequest(VNPAY, 100_000);
            PaymentResponse response = service.createPayment(req, "127.0.0.1");

            // Verify URL trả về đúng
            assertThat(response.getCode()).isEqualTo("ok");
            assertThat(response.getPaymentUrl()).isEqualTo("https://vnpay.test/pay?token=abc");

            // Verify Payment được lưu đúng trạng thái
            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(captor.capture());
            Payment saved = captor.getValue();
            assertThat(saved.getReservationId()).isEqualTo(RES_ID);
            assertThat(saved.getStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(saved.getMethod()).isEqualTo(PaymentMethod.VNPAY);
            assertThat(saved.getAmount()).isEqualByComparingTo("100000");
        }

        @Test
        @DisplayName("MOMO — handler đúng được chọn")
        void createPayment_momo_shouldUseMomoHandler() {
            when(momoHandler.createPaymentUrl(eq(RES_ID), anyInt(), anyString()))
                    .thenReturn("https://momo.test/pay");

            PaymentRequest req = buildRequest(MOMO, 50_000);
            PaymentResponse response = service.createPayment(req, "127.0.0.1");

            assertThat(response.getPaymentUrl()).isEqualTo("https://momo.test/pay");
            verify(vnpayHandler, never()).createPaymentUrl(any(), anyInt(), any());
            verify(momoHandler).createPaymentUrl(eq(RES_ID), eq(50_000), eq("127.0.0.1"));
        }

        @Test
        @DisplayName("provider không hợp lệ → throw ResourceNotFoundException")
        void createPayment_unknownProvider_shouldThrow() {
            PaymentRequest req = buildRequest("UNKNOWN_PAY", 100_000);

            assertThatThrownBy(() -> service.createPayment(req, "127.0.0.1"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Provider not supported");
        }

        @Test
        @DisplayName("provider null → method = UNKNOWN, vẫn throw vì không có handler")
        void createPayment_nullProvider_shouldThrow() {
            PaymentRequest req = buildRequest(null, 100_000);

            assertThatThrownBy(() -> service.createPayment(req, "127.0.0.1"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ----------------------------------------------------------------
    // processCallback — idempotency
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("processCallback — idempotency")
    class ProcessCallbackIdempotency {

        @Test
        @DisplayName("callback lần 2 với payment đã PAID → trả về success=true, KHÔNG gọi confirm lại")
        void processCallback_alreadyPaid_shouldReturnSuccessWithoutConfirm() {
            Map<String, String> params = Map.of("vnp_TxnRef", RES_ID);
            when(vnpayHandler.getId(params)).thenReturn(RES_ID);

            Payment paid = buildPayment(RES_ID, PaymentStatus.PAID, PaymentMethod.VNPAY);
            when(paymentRepository.findByReservationId(RES_ID)).thenReturn(Optional.of(paid));

            PaymentHandlerResponse response = service.processCallback(VNPAY, params);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getReservationId()).isEqualTo(RES_ID);

            // Idempotency: confirm KHÔNG được gọi lần 2
            verify(reservationClientService, never()).confirm(any());
            verify(paymentRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("duplicate callback đồng thời — payment đã PAID → không double-confirm")
        void processCallback_duplicateCallback_shouldNotDoubleConfirm() {
            Map<String, String> params = Map.of("vnp_TxnRef", RES_ID);
            when(vnpayHandler.getId(params)).thenReturn(RES_ID);

            // Lần đầu PENDING, sau khi check idempotency thì gọi tiếp
            // Giả lập: lần gọi thứ nhất thấy PAID (đã được xử lý bởi thread khác)
            Payment paid = buildPayment(RES_ID, PaymentStatus.PAID, PaymentMethod.VNPAY);
            when(paymentRepository.findByReservationId(RES_ID)).thenReturn(Optional.of(paid));

            service.processCallback(VNPAY, params);
            service.processCallback(VNPAY, params); // gọi lần 2

            // confirm chỉ 0 lần (cả 2 lần đều thấy PAID)
            verify(reservationClientService, never()).confirm(any());
        }
    }

    // ----------------------------------------------------------------
    // processCallback — happy path
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("processCallback — happy path")
    class ProcessCallbackHappyPath {

        @Test
        @DisplayName("VNPAY callback hợp lệ → updatePayment PAID, confirm reservation, update amount")
        void processCallback_vnpay_success_shouldUpdateAndConfirm() {
            Map<String, String> params = Map.of("vnp_TxnRef", RES_ID);
            when(vnpayHandler.getId(params)).thenReturn(RES_ID);

            Payment pending = buildPayment(RES_ID, PaymentStatus.PENDING, PaymentMethod.VNPAY);
            // findByReservationId được gọi 2 lần: lần 1 check idempotency, lần 2 update amount
            when(paymentRepository.findByReservationId(RES_ID))
                    .thenReturn(Optional.of(pending))  // lần 1: idempotency check → PENDING
                    .thenReturn(Optional.of(pending)); // lần 2: sau updatePayment

            ReservationDTO reservation = buildReservationDTO(RES_ID, new BigDecimal("95000"));
            when(reservationClientService.confirm(RES_ID)).thenReturn(reservation);

            // updatePayment gọi findByReservationId nội bộ
            Payment updatingPayment = buildPayment(RES_ID, PaymentStatus.PENDING, PaymentMethod.VNPAY);
            // saveAndFlush trong updatePayment
            when(paymentRepository.saveAndFlush(any())).thenReturn(updatingPayment);

            PaymentHandlerResponse response = service.processCallback(VNPAY, params);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getReservationId()).isEqualTo(RES_ID);

            // Verify confirm được gọi
            verify(reservationClientService).confirm(RES_ID);

            // Verify payment amount được update từ reservation
            verify(paymentRepository).save(argThat(p ->
                    p.getAmount().compareTo(new BigDecimal("95000")) == 0));
        }

        @Test
        @DisplayName("MOMO callback hợp lệ → dùng momoHandler.getId")
        void processCallback_momo_success_shouldUseMomoHandler() {
            Map<String, String> params = Map.of("orderId", RES_ID);
            when(momoHandler.getId(params)).thenReturn(RES_ID);

            Payment pending = buildPayment(RES_ID, PaymentStatus.PENDING, PaymentMethod.MOMO);
            when(paymentRepository.findByReservationId(RES_ID))
                    .thenReturn(Optional.of(pending))
                    .thenReturn(Optional.of(pending));

            ReservationDTO reservation = buildReservationDTO(RES_ID, new BigDecimal("100000"));
            when(reservationClientService.confirm(RES_ID)).thenReturn(reservation);
            when(paymentRepository.saveAndFlush(any())).thenReturn(pending);

            PaymentHandlerResponse response = service.processCallback(MOMO, params);

            assertThat(response.isSuccess()).isTrue();
            verify(vnpayHandler, never()).getId(any());
        }
    }

    // ----------------------------------------------------------------
    // processCallback — failure / exception path
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("processCallback — failure path")
    class ProcessCallbackFailure {

        @Test
        @DisplayName("reservationId không tồn tại trong DB → throw ResourceNotFoundException")
        void processCallback_paymentNotFound_shouldThrow() {
            Map<String, String> params = Map.of("vnp_TxnRef", RES_ID);
            when(vnpayHandler.getId(params)).thenReturn(RES_ID);
            when(paymentRepository.findByReservationId(RES_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.processCallback(VNPAY, params))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("confirm reservation throw → cancel được gọi, trả về success=false")
        void processCallback_confirmThrows_shouldCancelAndReturnFail() {
            Map<String, String> params = Map.of("vnp_TxnRef", RES_ID);
            when(vnpayHandler.getId(params)).thenReturn(RES_ID);

            Payment pending = buildPayment(RES_ID, PaymentStatus.PENDING, PaymentMethod.VNPAY);
            when(paymentRepository.findByReservationId(RES_ID))
                    .thenReturn(Optional.of(pending))
                    .thenReturn(Optional.of(pending));
            when(paymentRepository.saveAndFlush(any())).thenReturn(pending);

            // confirm throw
            when(reservationClientService.confirm(RES_ID))
                    .thenThrow(new RuntimeException("Booking service down"));

            PaymentHandlerResponse response = service.processCallback(VNPAY, params);

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getMessage()).isEqualTo("Internal Error");
            // cancel phải được gọi khi confirm fail
            verify(reservationClientService).cancel(RES_ID);
        }

        @Test
        @DisplayName("updatePayment throw IllegalStateException (PAID race) → cancel được gọi")
        void processCallback_updatePaymentRace_shouldCancelAndReturnFail() {
            Map<String, String> params = Map.of("vnp_TxnRef", RES_ID);
            when(vnpayHandler.getId(params)).thenReturn(RES_ID);

            Payment pending = buildPayment(RES_ID, PaymentStatus.PENDING, PaymentMethod.VNPAY);
            // Lần 1: idempotency check thấy PENDING
            // Lần 2 (trong updatePayment): thấy PAID → throw IllegalStateException
            Payment paid = buildPayment(RES_ID, PaymentStatus.PAID, PaymentMethod.VNPAY);
            when(paymentRepository.findByReservationId(RES_ID))
                    .thenReturn(Optional.of(pending))  // idempotency check
                    .thenReturn(Optional.of(paid));    // updatePayment thấy đã PAID

            PaymentHandlerResponse response = service.processCallback(VNPAY, params);

            assertThat(response.isSuccess()).isFalse();
            verify(reservationClientService).cancel(RES_ID);
            verify(reservationClientService, never()).confirm(any());
        }
    }

    // ----------------------------------------------------------------
    // updatePayment
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("updatePayment")
    class UpdatePayment {

        @Test
        @DisplayName("PENDING → PAID: status và method được update, saveAndFlush được gọi")
        void updatePayment_pendingToPaid_shouldUpdate() {
            Payment pending = buildPayment(RES_ID, PaymentStatus.PENDING, PaymentMethod.VNPAY);
            when(paymentRepository.findByReservationId(RES_ID)).thenReturn(Optional.of(pending));
            when(paymentRepository.saveAndFlush(any())).thenReturn(pending);

            PaymentUpdateRequest req = PaymentUpdateRequest.builder()
                    .reservationId(RES_ID)
                    .method(PaymentMethod.VNPAY)
                    .isSuccess(true)
                    .build();

            service.updatePayment(req);

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).saveAndFlush(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(captor.getValue().getMethod()).isEqualTo(PaymentMethod.VNPAY);
        }

        @Test
        @DisplayName("đã PAID → throw IllegalStateException (guard double-update)")
        void updatePayment_alreadyPaid_shouldThrow() {
            Payment paid = buildPayment(RES_ID, PaymentStatus.PAID, PaymentMethod.VNPAY);
            when(paymentRepository.findByReservationId(RES_ID)).thenReturn(Optional.of(paid));

            PaymentUpdateRequest req = PaymentUpdateRequest.builder()
                    .reservationId(RES_ID)
                    .method(PaymentMethod.VNPAY)
                    .isSuccess(true)
                    .build();

            assertThatThrownBy(() -> service.updatePayment(req))
                    .isInstanceOf(IllegalStateException.class);

            verify(paymentRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("reservationId không tồn tại → throw ResourceNotFoundException")
        void updatePayment_notFound_shouldThrow() {
            when(paymentRepository.findByReservationId(RES_ID)).thenReturn(Optional.empty());

            PaymentUpdateRequest req = PaymentUpdateRequest.builder()
                    .reservationId(RES_ID)
                    .method(PaymentMethod.VNPAY)
                    .isSuccess(true)
                    .build();

            assertThatThrownBy(() -> service.updatePayment(req))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }


    // ----------------------------------------------------------------
    // manualCheck
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("manualCheck")
    class ManualCheck {

        @Test
        @DisplayName("queryTransactions trả về true → manualCheck trả về true")
        void manualCheck_transactionExists_shouldReturnTrue() {
            when(vnpayHandler.queryTransactions(RES_ID)).thenReturn(true);

            Boolean result = service.manualCheck(VNPAY, RES_ID);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("queryTransactions trả về false → manualCheck trả về false")
        void manualCheck_transactionNotFound_shouldReturnFalse() {
            when(vnpayHandler.queryTransactions(RES_ID)).thenReturn(false);

            Boolean result = service.manualCheck(VNPAY, RES_ID);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("provider không hợp lệ → throw ResourceNotFoundException")
        void manualCheck_unknownProvider_shouldThrow() {
            assertThatThrownBy(() -> service.manualCheck("UNKNOWN", RES_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ----------------------------------------------------------------
    // checkPaymentMethod (via createPayment)
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("checkPaymentMethod")
    class CheckPaymentMethod {

        @Test
        @DisplayName("provider lowercase → vẫn map đúng PaymentMethod")
        void checkPaymentMethod_lowercaseProvider_shouldMap() {
            // supports() mock phải trả true cho lowercase
            when(vnpayHandler.supports("vnpay")).thenReturn(true);
            when(vnpayHandler.createPaymentUrl(any(), anyInt(), any()))
                    .thenReturn("https://vnpay.test");

            PaymentRequest req = buildRequest("vnpay", 100_000);
            service.createPayment(req, "127.0.0.1");

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(captor.capture());
            assertThat(captor.getValue().getMethod()).isEqualTo(PaymentMethod.VNPAY);
        }

        @Test
        @DisplayName("provider không map được → method = UNKNOWN")
        void checkPaymentMethod_unknownProvider_shouldReturnUnknown() {
            // Tạo handler giả hỗ trợ "WEIRD"
            PaymentHandler weirdHandler = mock(PaymentHandler.class);
            when(weirdHandler.supports("WEIRD")).thenReturn(true);
            when(weirdHandler.supports(VNPAY)).thenReturn(false);
            when(weirdHandler.supports(MOMO)).thenReturn(false);
            when(weirdHandler.createPaymentUrl(any(), anyInt(), any()))
                    .thenReturn("https://weird.test");

            PaymentServiceImpl svcWithWeird = new PaymentServiceImpl(
                    List.of(vnpayHandler, momoHandler, weirdHandler),
                    paymentRepository,
                    reservationClientService);

            PaymentRequest req = buildRequest("WEIRD", 100_000);
            svcWithWeird.createPayment(req, "127.0.0.1");

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(captor.capture());
            assertThat(captor.getValue().getMethod()).isEqualTo(PaymentMethod.UNKNOWN);
        }
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private PaymentRequest buildRequest(String type, int amount) {
        PaymentRequest req = new PaymentRequest();
        req.setReservationId(RES_ID);
        req.setPaymentType(type);
        req.setAmount(amount);
        return req;
    }

    private Payment buildPayment(String reservationId, PaymentStatus status, PaymentMethod method) {
        return Payment.builder()
                .id(1L)
                .reservationId(reservationId)
                .paymentIntentId(reservationId)
                .amount(new BigDecimal("100000"))
                .method(method)
                .status(status)
                .createdAt(Instant.now())
                .build();
    }

    private ReservationDTO buildReservationDTO(String id, BigDecimal total) {
        return ReservationDTO.builder()
                .id(id)
                .totalAmount(total)
                .build();
    }
}