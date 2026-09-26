package com.dawn.payment.service;

import com.dawn.common.core.constant.PaymentMethod;
import com.dawn.common.core.constant.PaymentStatus;
import com.dawn.common.core.constant.PermissionKey;
import com.dawn.common.core.exception.wrapper.InvalidRequestException;
import com.dawn.common.core.exception.wrapper.PermissionDeniedException;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import com.dawn.common.core.outbox.Outbox;
import com.dawn.common.core.outbox.OutboxRepository;
import com.dawn.common.core.service.AuditLogService;
import com.dawn.common.core.utils.SecurityUtils;
import com.dawn.identity.api.IdentityModuleApi;
import com.dawn.payment.dto.request.PaymentRequest;
import com.dawn.payment.dto.response.PaymentHandlerResponse;
import com.dawn.payment.dto.response.PaymentRefundDTO;
import com.dawn.payment.dto.response.PaymentResponse;
import com.dawn.payment.handler.PaymentHandler;
import com.dawn.payment.model.Payment;
import com.dawn.payment.repository.PaymentRepository;
import com.dawn.payment.service.impl.PaymentServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
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
    PaymentHandler vnpayHandler;
    @Mock
    PaymentHandler momoHandler;
    @Mock
    OutboxRepository outboxRepository;
    @Mock
    AuditLogService auditLogService;

    @Mock
    IdentityModuleApi identityModuleApi;
    @Mock
    com.dawn.booking.api.BookingModuleApi bookingApi;

    PaymentServiceImpl service;

    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private static final String RES_ID = "RES-001";
    private static final String VNPAY = "VNPAY";
    private static final String MOMO = "MOMO";

    @BeforeEach
    void setUp() {
        lenient().when(vnpayHandler.supports(VNPAY)).thenReturn(true);
        lenient().when(vnpayHandler.supports(MOMO)).thenReturn(false);
        lenient().when(momoHandler.supports(MOMO)).thenReturn(true);
        lenient().when(momoHandler.supports(VNPAY)).thenReturn(false);

        lenient().when(bookingApi.getHoldTtlSeconds(anyString())).thenReturn(900L);

        service = new PaymentServiceImpl(
                List.of(vnpayHandler, momoHandler),
                paymentRepository,
                outboxRepository,
                objectMapper,
                identityModuleApi,
                auditLogService,
                bookingApi);
    }


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

            assertThat(response.getCode()).isEqualTo("ok");
            assertThat(response.getPaymentUrl()).isEqualTo("https://vnpay.test/pay?token=abc");

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
        @DisplayName("txnRef được sinh unique dạng CP{ts}-{hex}, KHÔNG dùng reservationId")
        void createPayment_shouldGenerateUniqueTxnRef() {
            when(vnpayHandler.createPaymentUrl(any(), anyInt(), anyString()))
                    .thenReturn("https://vnpay.test/pay");

            service.createPayment(buildRequest(VNPAY, 100_000), "127.0.0.1");
            service.createPayment(buildRequest(VNPAY, 100_000), "127.0.0.1");

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository, times(2)).save(captor.capture());

            Payment first = captor.getAllValues().get(0);
            Payment second = captor.getAllValues().get(1);
            assertThat(first.getGatewayTxnRef()).startsWith("CP").isNotEqualTo(RES_ID);
            assertThat(second.getGatewayTxnRef()).startsWith("CP").isNotEqualTo(first.getGatewayTxnRef());
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
        @DisplayName("provider null → throw vì không có handler")
        void createPayment_nullProvider_shouldThrow() {
            PaymentRequest req = buildRequest(null, 100_000);

            assertThatThrownBy(() -> service.createPayment(req, "127.0.0.1"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("C0: row PENDING đã có → tái dùng, KHÔNG chèn mới")
        void createPayment_openPending_shouldReuse() {
            Payment open = buildPayment(RES_ID, PaymentStatus.PENDING, PaymentMethod.VNPAY);
            when(paymentRepository.findFirstByReservationIdOrderByCreatedAtDesc(RES_ID))
                    .thenReturn(Optional.of(open));
            when(vnpayHandler.createPaymentUrl(eq(RES_ID), anyInt(), anyString()))
                    .thenReturn("https://vnpay.test/pay?reuse=1");

            PaymentResponse response = service.createPayment(buildRequest(VNPAY, 100_000), "127.0.0.1");

            assertThat(response.getPaymentUrl()).isEqualTo("https://vnpay.test/pay?reuse=1");
            verify(paymentRepository, never()).save(any(Payment.class));
        }

        @Test
        @DisplayName("C0: row PAID đã có → 409 PAYMENT_COMPLETE, không mở gateway lần 2")
        void createPayment_alreadyPaid_shouldConflict() {
            Payment paid = buildPayment(RES_ID, PaymentStatus.PAID, PaymentMethod.VNPAY);
            when(paymentRepository.findFirstByReservationIdOrderByCreatedAtDesc(RES_ID))
                    .thenReturn(Optional.of(paid));

            assertThatThrownBy(() -> service.createPayment(buildRequest(VNPAY, 100_000), "127.0.0.1"))
                    .isInstanceOf(com.dawn.common.core.exception.ApiException.class)
                    .hasMessageContaining("Payment already");
            verify(paymentRepository, never()).save(any(Payment.class));
        }

        @Test
        @DisplayName("S4: hold đã chết → 409 RESERVATION_EXPIRED, chặn gửi gateway")
        void createPayment_deadHold_shouldRefuse() {
            when(bookingApi.getHoldTtlSeconds(RES_ID)).thenReturn(0L);

            assertThatThrownBy(() -> service.createPayment(buildRequest(VNPAY, 100_000), "127.0.0.1"))
                    .isInstanceOf(com.dawn.common.core.exception.ApiException.class)
                    .hasMessageContaining("expired");
            verify(paymentRepository, never()).save(any(Payment.class));
        }

        @Test
        @DisplayName("C4: snapshot seatIds được lưu vào row")
        void createPayment_withSeats_shouldSnapshot() {
            when(vnpayHandler.createPaymentUrl(eq(RES_ID), anyInt(), anyString()))
                    .thenReturn("https://vnpay.test/pay");

            PaymentRequest req = buildRequest(VNPAY, 100_000);
            req.setSeatIds(java.util.List.of(11L, 12L));
            service.createPayment(req, "127.0.0.1");

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(captor.capture());
            assertThat(captor.getValue().getSeatSnapshot()).isEqualTo("[11,12]");
        }
    }

    // processCallback — idempotency

    @Nested
    @DisplayName("processCallback — idempotency")
    class ProcessCallbackIdempotency {

        @Test
        @DisplayName("callback lần 2 với payment đã PAID → trả về success=true, KHÔNG lưu lại")
        void processCallback_alreadyPaid_shouldReturnSuccessWithoutConfirm() {
            Map<String, String> params = Map.of("vnp_TxnRef", RES_ID);
            when(vnpayHandler.getId(params)).thenReturn(RES_ID);
            when(vnpayHandler.verifySignature(params)).thenReturn(true);

            Payment paid = buildPayment(RES_ID, PaymentStatus.PAID, PaymentMethod.VNPAY);
            when(paymentRepository.findFirstByReservationIdOrderByCreatedAtDesc(RES_ID)).thenReturn(Optional.of(paid));

            PaymentHandlerResponse response = service.processCallback(VNPAY, params);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getReservationId()).isEqualTo(RES_ID);

            verify(paymentRepository, never()).saveAndFlush(any());
            verify(outboxRepository, never()).save(any());
        }

        @Test
        @DisplayName("duplicate callback — payment đã PAID → không double-process")
        void processCallback_duplicateCallback_shouldNotDoubleProcess() {
            Map<String, String> params = Map.of("vnp_TxnRef", RES_ID);
            when(vnpayHandler.getId(params)).thenReturn(RES_ID);
            when(vnpayHandler.verifySignature(params)).thenReturn(true);

            Payment paid = buildPayment(RES_ID, PaymentStatus.PAID, PaymentMethod.VNPAY);
            when(paymentRepository.findFirstByReservationIdOrderByCreatedAtDesc(RES_ID)).thenReturn(Optional.of(paid));

            service.processCallback(VNPAY, params);
            service.processCallback(VNPAY, params);

            verify(paymentRepository, never()).saveAndFlush(any());
            verify(outboxRepository, never()).save(any());
        }

        @Test
        @DisplayName("PAID rồi nhưng callback khác amount → vẫn success + audit CONFLICT")
        void processCallback_contradictoryAmount_shouldAuditConflict() {
            Map<String, String> params = Map.of("vnp_TxnRef", RES_ID);
            when(vnpayHandler.getId(params)).thenReturn(RES_ID);
            when(vnpayHandler.verifySignature(params)).thenReturn(true);
            when(vnpayHandler.getAmount(params)).thenReturn(new BigDecimal("200000"));
            when(vnpayHandler.getTxnRef(params)).thenReturn("TXN-999");

            Payment paid = buildPayment(RES_ID, PaymentStatus.PAID, PaymentMethod.VNPAY);
            when(paymentRepository.findFirstByReservationIdOrderByCreatedAtDesc(RES_ID)).thenReturn(Optional.of(paid));

            PaymentHandlerResponse response = service.processCallback(VNPAY, params);

            assertThat(response.isSuccess()).isTrue();
            verify(auditLogService).record(eq("PAYMENT_CALLBACK_CONFLICT"),
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("PAID + callback khớp số liệu → success, không audit CONFLICT")
        void processCallback_consistentDuplicate_shouldNotAudit() {
            Map<String, String> params = Map.of("vnp_TxnRef", RES_ID);
            when(vnpayHandler.getId(params)).thenReturn(RES_ID);
            when(vnpayHandler.verifySignature(params)).thenReturn(true);
            when(vnpayHandler.getAmount(params)).thenReturn(new BigDecimal("100000"));
            when(vnpayHandler.getTxnRef(params)).thenReturn(null);

            Payment paid = buildPayment(RES_ID, PaymentStatus.PAID, PaymentMethod.VNPAY);
            when(paymentRepository.findFirstByReservationIdOrderByCreatedAtDesc(RES_ID)).thenReturn(Optional.of(paid));

            PaymentHandlerResponse response = service.processCallback(VNPAY, params);

            assertThat(response.isSuccess()).isTrue();
            verify(auditLogService, never()).record(eq("PAYMENT_CALLBACK_CONFLICT"),
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("outbox row da ton tai - payment PENDING nhung event da enqueue -> khong save lai")
        void processCallback_outboxAlreadyEnqueued_shouldSkipOutboxSave() {
            Map<String, String> params = Map.of("vnp_TxnRef", RES_ID);
            when(vnpayHandler.getId(params)).thenReturn(RES_ID);
            when(vnpayHandler.verifySignature(params)).thenReturn(true);
            when(vnpayHandler.getTxnRef(params)).thenReturn("TXN-123");

            Payment pending = buildPayment(RES_ID, PaymentStatus.PENDING, PaymentMethod.VNPAY);
            when(paymentRepository.findFirstByReservationIdOrderByCreatedAtDesc(RES_ID)).thenReturn(Optional.of(pending));
            when(paymentRepository.saveAndFlush(any())).thenReturn(pending);
            when(outboxRepository.existsByEventTypeAndAggregateId(anyString(), eq(RES_ID))).thenReturn(true);

            PaymentHandlerResponse response = service.processCallback(VNPAY, params);

            assertThat(response.isSuccess()).isTrue();
            verify(paymentRepository).saveAndFlush(any());
            verify(outboxRepository, never()).save(any(Outbox.class));
        }
    }

    // processCallback — happy path

    @Nested
    @DisplayName("processCallback — happy path")
    class ProcessCallbackHappyPath {

        @Test
        @DisplayName("VNPAY callback hợp lệ → update PAID, enqueue outbox")
        void processCallback_vnpay_success_shouldUpdateAndPublish() {
            Map<String, String> params = Map.of("vnp_TxnRef", RES_ID);
            when(vnpayHandler.getId(params)).thenReturn(RES_ID);
            when(vnpayHandler.verifySignature(params)).thenReturn(true);
            when(vnpayHandler.getTxnRef(params)).thenReturn("TXN-123");

            Payment pending = buildPayment(RES_ID, PaymentStatus.PENDING, PaymentMethod.VNPAY);
            when(paymentRepository.findFirstByReservationIdOrderByCreatedAtDesc(RES_ID)).thenReturn(Optional.of(pending));
            when(paymentRepository.saveAndFlush(any())).thenReturn(pending);

            PaymentHandlerResponse response = service.processCallback(VNPAY, params);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getReservationId()).isEqualTo(RES_ID);

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).saveAndFlush(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(captor.getValue().getGatewayTxnRef()).isEqualTo("TXN-123");

            verify(outboxRepository).save(any(Outbox.class));
        }

        @Test
        @DisplayName("MOMO callback hợp lệ → dùng momoHandler.getId")
        void processCallback_momo_success_shouldUseMomoHandler() {
            Map<String, String> params = Map.of("orderId", RES_ID);
            when(momoHandler.getId(params)).thenReturn(RES_ID);
            when(momoHandler.verifySignature(params)).thenReturn(true);
            when(momoHandler.getTxnRef(params)).thenReturn("TRANS-999");

            Payment pending = buildPayment(RES_ID, PaymentStatus.PENDING, PaymentMethod.MOMO);
            when(paymentRepository.findFirstByReservationIdOrderByCreatedAtDesc(RES_ID)).thenReturn(Optional.of(pending));
            when(paymentRepository.saveAndFlush(any())).thenReturn(pending);

            PaymentHandlerResponse response = service.processCallback(MOMO, params);

            assertThat(response.isSuccess()).isTrue();
            verify(vnpayHandler, never()).getId(any());
            verify(outboxRepository).save(any(Outbox.class));
        }
    }

    // processCallback — failure path

    @Nested
    @DisplayName("processCallback — failure path")
    class ProcessCallbackFailure {

        @Test
        @DisplayName("reservationId không tồn tại trong DB → throw ResourceNotFoundException")
        void processCallback_paymentNotFound_shouldThrow() {
            Map<String, String> params = Map.of("vnp_TxnRef", RES_ID);
            when(vnpayHandler.getId(params)).thenReturn(RES_ID);
            when(paymentRepository.findFirstByReservationIdOrderByCreatedAtDesc(RES_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.processCallback(VNPAY, params))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("signature sai → success=false, không đổi trạng thái")
        void processCallback_invalidSignature_shouldFailWithoutSave() {
            Map<String, String> params = Map.of("vnp_TxnRef", RES_ID);
            when(vnpayHandler.getId(params)).thenReturn(RES_ID);
            when(vnpayHandler.verifySignature(params)).thenReturn(false);

            Payment pending = buildPayment(RES_ID, PaymentStatus.PENDING, PaymentMethod.VNPAY);
            when(paymentRepository.findFirstByReservationIdOrderByCreatedAtDesc(RES_ID)).thenReturn(Optional.of(pending));

            PaymentHandlerResponse response = service.processCallback(VNPAY, params);

            assertThat(response.isSuccess()).isFalse();
            verify(paymentRepository, never()).saveAndFlush(any());
            verify(outboxRepository, never()).save(any());
        }

        @Test
        @DisplayName("amount từ gateway lệch → FAILED/AMOUNT_MISMATCH, success=false")
        void processCallback_amountMismatch_shouldFailPayment() {
            Map<String, String> params = Map.of("vnp_TxnRef", RES_ID);
            when(vnpayHandler.getId(params)).thenReturn(RES_ID);
            when(vnpayHandler.verifySignature(params)).thenReturn(true);
            when(vnpayHandler.getAmount(params)).thenReturn(new BigDecimal("99999"));

            Payment pending = buildPayment(RES_ID, PaymentStatus.PENDING, PaymentMethod.VNPAY);
            when(paymentRepository.findFirstByReservationIdOrderByCreatedAtDesc(RES_ID)).thenReturn(Optional.of(pending));

            PaymentHandlerResponse response = service.processCallback(VNPAY, params);

            assertThat(response.isSuccess()).isFalse();
            assertThat(pending.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(pending.getStatusReason()).isEqualTo("AMOUNT_MISMATCH");
            verify(paymentRepository).save(pending);
            verify(outboxRepository, never()).save(any());
        }

        @Test
        @DisplayName("saveAndFlush throw → publish PaymentFailedEvent, trả về success=false")
        void processCallback_saveThrows_shouldPublishFailedEvent() {
            Map<String, String> params = Map.of("vnp_TxnRef", RES_ID);
            when(vnpayHandler.getId(params)).thenReturn(RES_ID);
            when(vnpayHandler.verifySignature(params)).thenReturn(true);
            when(vnpayHandler.getTxnRef(params)).thenReturn("TXN-123");

            Payment pending = buildPayment(RES_ID, PaymentStatus.PENDING, PaymentMethod.VNPAY);
            when(paymentRepository.findFirstByReservationIdOrderByCreatedAtDesc(RES_ID)).thenReturn(Optional.of(pending));
            when(paymentRepository.saveAndFlush(any()))
                    .thenThrow(new RuntimeException("DB error"));

            PaymentHandlerResponse response = service.processCallback(VNPAY, params);

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getMessage()).isEqualTo("Internal Error");

            verify(outboxRepository).save(any(Outbox.class));
        }
    }


    @Nested
    @DisplayName("manualCheck")
    class ManualCheck {

        @Test
        @DisplayName("payment PAID → manualCheck trả về true")
        void manualCheck_paid_shouldReturnTrue() {
            Payment paid = buildPayment(RES_ID, PaymentStatus.PAID, PaymentMethod.VNPAY);
            when(paymentRepository.findFirstByReservationIdOrderByCreatedAtDesc(RES_ID)).thenReturn(Optional.of(paid));

            Boolean result = service.manualCheck(VNPAY, RES_ID);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("payment PENDING → manualCheck trả về false")
        void manualCheck_pending_shouldReturnFalse() {
            Payment pending = buildPayment(RES_ID, PaymentStatus.PENDING, PaymentMethod.VNPAY);
            when(paymentRepository.findFirstByReservationIdOrderByCreatedAtDesc(RES_ID)).thenReturn(Optional.of(pending));

            Boolean result = service.manualCheck(VNPAY, RES_ID);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("không có payment → manualCheck trả về false")
        void manualCheck_noPayment_shouldReturnFalse() {
            when(paymentRepository.findFirstByReservationIdOrderByCreatedAtDesc(RES_ID)).thenReturn(Optional.empty());

            Boolean result = service.manualCheck(VNPAY, RES_ID);

            assertThat(result).isFalse();
        }
    }


    @Nested
    @DisplayName("checkPaymentMethod")
    class CheckPaymentMethod {

        @Test
        @DisplayName("provider lowercase → vẫn map đúng PaymentMethod")
        void checkPaymentMethod_lowercaseProvider_shouldMap() {
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
            PaymentHandler weirdHandler = mock(PaymentHandler.class);
            when(weirdHandler.supports("WEIRD")).thenReturn(true);
            when(weirdHandler.createPaymentUrl(any(), anyInt(), any()))
                    .thenReturn("https://weird.test");

            PaymentServiceImpl svcWithWeird = new PaymentServiceImpl(
List.of(vnpayHandler, momoHandler, weirdHandler),
                paymentRepository,
                outboxRepository,
                objectMapper,
                identityModuleApi,
                auditLogService,
                bookingApi);

            PaymentRequest req = buildRequest("WEIRD", 100_000);
            svcWithWeird.createPayment(req, "127.0.0.1");

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(captor.capture());
            assertThat(captor.getValue().getMethod()).isEqualTo(PaymentMethod.UNKNOWN);
        }
    }

    @Nested
    @DisplayName("markRefundPending")
    class MarkRefundPending {

        @Test
        @DisplayName("PAID → REFUND_PENDING")
        void paid_markedRefundPending() {
            Payment paid = buildPayment(RES_ID, PaymentStatus.PAID, PaymentMethod.VNPAY);
            when(paymentRepository.findFirstByReservationIdOrderByCreatedAtDesc(RES_ID)).thenReturn(Optional.of(paid));

            service.markRefundPending(RES_ID);

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.REFUND_PENDING);
        }

        @Test
        @DisplayName("not PAID → untouched")
        void notPaid_untouched() {
            Payment pending = buildPayment(RES_ID, PaymentStatus.PENDING, PaymentMethod.VNPAY);
            when(paymentRepository.findFirstByReservationIdOrderByCreatedAtDesc(RES_ID)).thenReturn(Optional.of(pending));

            service.markRefundPending(RES_ID);

            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("missing payment → no-op")
        void missing_noop() {
            when(paymentRepository.findFirstByReservationIdOrderByCreatedAtDesc(RES_ID)).thenReturn(Optional.empty());

            service.markRefundPending(RES_ID);

            verify(paymentRepository, never()).save(any());
        }
    }


    @Nested
    @DisplayName("listRefundRequests")
    class ListRefundRequests {

        @Test
        @DisplayName("without FINANCE → denied, no query")
        void noFinance_denied() {
            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getCurrentUserId).thenReturn(7L);
                when(identityModuleApi.hasPermission(7L, PermissionKey.FINANCE)).thenReturn(false);

                assertThatThrownBy(() -> service.listRefundRequests())
                        .isInstanceOf(PermissionDeniedException.class);
                verify(paymentRepository, never()).findAllByStatusOrderByCreatedAtDesc(any());
            }
        }

        @Test
        @DisplayName("with FINANCE → mapped DTOs in repo order")
        void finance_returnsMapped() {
            Payment pending = buildPayment(RES_ID, PaymentStatus.REFUND_PENDING, PaymentMethod.VNPAY);
            pending.setPaidAt(Instant.parse("2026-09-01T10:00:00Z"));
            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getCurrentUserId).thenReturn(7L);
                when(identityModuleApi.hasPermission(7L, PermissionKey.FINANCE)).thenReturn(true);
                when(paymentRepository.findAllByStatusOrderByCreatedAtDesc(PaymentStatus.REFUND_PENDING))
                        .thenReturn(List.of(pending));

                List<PaymentRefundDTO> result = service.listRefundRequests();

                assertThat(result).hasSize(1);
                assertThat(result.get(0).getReservationId()).isEqualTo(RES_ID);
                assertThat(result.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("100000"));
                assertThat(result.get(0).getMethod()).isEqualTo(PaymentMethod.VNPAY);
                assertThat(result.get(0).getStatus()).isEqualTo(PaymentStatus.REFUND_PENDING);
                assertThat(result.get(0).getPaidAt()).isEqualTo(Instant.parse("2026-09-01T10:00:00Z"));
            }
        }

        @Test
        @DisplayName("none pending → empty list")
        void none_empty() {
            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getCurrentUserId).thenReturn(7L);
                when(identityModuleApi.hasPermission(7L, PermissionKey.FINANCE)).thenReturn(true);
                when(paymentRepository.findAllByStatusOrderByCreatedAtDesc(PaymentStatus.REFUND_PENDING))
                        .thenReturn(List.of());

                assertThat(service.listRefundRequests()).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("approveRefund")
    class ApproveRefund {

        @Test
        @DisplayName("without FINANCE → denied")
        void noFinance_denied() {
            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getCurrentUserId).thenReturn(7L);
                when(identityModuleApi.hasPermission(7L, PermissionKey.FINANCE)).thenReturn(false);

                assertThatThrownBy(() -> service.approveRefund(RES_ID))
                        .isInstanceOf(PermissionDeniedException.class);
                verify(paymentRepository, never()).save(any());
            }
        }

        @Test
        @DisplayName("missing payment → not found")
        void missing_notFound() {
            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getCurrentUserId).thenReturn(7L);
                when(identityModuleApi.hasPermission(7L, PermissionKey.FINANCE)).thenReturn(true);
                when(paymentRepository.findFirstByReservationIdOrderByCreatedAtDesc(RES_ID)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.approveRefund(RES_ID))
                        .isInstanceOf(ResourceNotFoundException.class);
            }
        }

        @Test
        @DisplayName("not REFUND_PENDING → invalid transition")
        void wrongStatus_rejected() {
            Payment paid = buildPayment(RES_ID, PaymentStatus.PAID, PaymentMethod.VNPAY);
            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getCurrentUserId).thenReturn(7L);
                when(identityModuleApi.hasPermission(7L, PermissionKey.FINANCE)).thenReturn(true);
                when(paymentRepository.findFirstByReservationIdOrderByCreatedAtDesc(RES_ID)).thenReturn(Optional.of(paid));

                assertThatThrownBy(() -> service.approveRefund(RES_ID))
                        .isInstanceOf(InvalidRequestException.class);
                verify(paymentRepository, never()).save(any());
            }
        }

        @Test
        @DisplayName("REFUND_PENDING → REFUNDED + audit")
        void pending_approved() {
            Payment pending = buildPayment(RES_ID, PaymentStatus.REFUND_PENDING, PaymentMethod.VNPAY);
            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getCurrentUserId).thenReturn(7L);
                when(identityModuleApi.hasPermission(7L, PermissionKey.FINANCE)).thenReturn(true);
                when(paymentRepository.findFirstByReservationIdOrderByCreatedAtDesc(RES_ID)).thenReturn(Optional.of(pending));

                service.approveRefund(RES_ID);

                ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
                verify(paymentRepository).save(captor.capture());
                assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.REFUNDED);
                verify(auditLogService).record(eq("REFUND_APPROVED"), any(), any(), any(),
                        any(), any(), any(), any(), any(), any(), any());
            }
        }
    }


    @Nested
    @DisplayName("rejectRefund")
    class RejectRefund {

        @Test
        @DisplayName("without FINANCE → denied")
        void noFinance_denied() {
            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getCurrentUserId).thenReturn(7L);
                when(identityModuleApi.hasPermission(7L, PermissionKey.FINANCE)).thenReturn(false);

                assertThatThrownBy(() -> service.rejectRefund(RES_ID))
                        .isInstanceOf(PermissionDeniedException.class);
                verify(paymentRepository, never()).save(any());
            }
        }

        @Test
        @DisplayName("not REFUND_PENDING → invalid transition")
        void wrongStatus_rejected() {
            Payment paid = buildPayment(RES_ID, PaymentStatus.PAID, PaymentMethod.VNPAY);
            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getCurrentUserId).thenReturn(7L);
                when(identityModuleApi.hasPermission(7L, PermissionKey.FINANCE)).thenReturn(true);
                when(paymentRepository.findFirstByReservationIdOrderByCreatedAtDesc(RES_ID)).thenReturn(Optional.of(paid));

                assertThatThrownBy(() -> service.rejectRefund(RES_ID))
                        .isInstanceOf(InvalidRequestException.class);
                verify(paymentRepository, never()).save(any());
            }
        }

        @Test
        @DisplayName("REFUND_PENDING → back to PAID + audit")
        void pending_rejected() {
            Payment pending = buildPayment(RES_ID, PaymentStatus.REFUND_PENDING, PaymentMethod.VNPAY);
            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getCurrentUserId).thenReturn(7L);
                when(identityModuleApi.hasPermission(7L, PermissionKey.FINANCE)).thenReturn(true);
                when(paymentRepository.findFirstByReservationIdOrderByCreatedAtDesc(RES_ID)).thenReturn(Optional.of(pending));

                service.rejectRefund(RES_ID);

                ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
                verify(paymentRepository).save(captor.capture());
                assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.PAID);
                verify(auditLogService).record(eq("REFUND_REJECTED"), any(), any(), any(),
                        any(), any(), any(), any(), any(), any(), any());
            }
        }
    }

    @Nested
    @DisplayName("listFailedPayments")
    class ListFailedPayments {

        @Test
        @DisplayName("without FINANCE → denied, no query")
        void noFinance_denied() {
            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getCurrentUserId).thenReturn(7L);
                when(identityModuleApi.hasPermission(7L, PermissionKey.FINANCE)).thenReturn(false);

                assertThatThrownBy(() -> service.listFailedPayments())
                        .isInstanceOf(PermissionDeniedException.class);
                verify(paymentRepository, never()).findAllByStatusOrderByCreatedAtDesc(any());
            }
        }

        @Test
        @DisplayName("with FINANCE → mapped FAILED DTOs")
        void finance_returnsMapped() {
            Payment failed = buildPayment(RES_ID, PaymentStatus.FAILED, PaymentMethod.MOMO);
            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getCurrentUserId).thenReturn(7L);
                when(identityModuleApi.hasPermission(7L, PermissionKey.FINANCE)).thenReturn(true);
                when(paymentRepository.findAllByStatusOrderByCreatedAtDesc(PaymentStatus.FAILED))
                        .thenReturn(List.of(failed));

                List<PaymentRefundDTO> result = service.listFailedPayments();

                assertThat(result).hasSize(1);
                assertThat(result.get(0).getReservationId()).isEqualTo(RES_ID);
                assertThat(result.get(0).getStatus()).isEqualTo(PaymentStatus.FAILED);
                verify(paymentRepository).findAllByStatusOrderByCreatedAtDesc(PaymentStatus.FAILED);
            }
        }
    }


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
}