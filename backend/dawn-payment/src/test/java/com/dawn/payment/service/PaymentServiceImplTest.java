package com.dawn.payment.service;

import com.dawn.common.core.constant.PaymentMethod;
import com.dawn.common.core.constant.PaymentStatus;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import com.dawn.common.core.service.AuditLogService;
import com.dawn.payment.dto.request.PaymentRequest;
import com.dawn.payment.dto.response.PaymentHandlerResponse;
import com.dawn.payment.dto.response.PaymentResponse;
import com.dawn.payment.handler.PaymentHandler;
import com.dawn.payment.model.Outbox;
import com.dawn.payment.model.Payment;
import com.dawn.payment.repository.OutboxRepository;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

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
    @Mock
    RabbitTemplate rabbitTemplate;
    @Mock
    OutboxRepository outboxRepository;
    @Mock
    AuditLogService auditLogService;

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

        service = new PaymentServiceImpl(
                List.of(vnpayHandler, momoHandler),
                paymentRepository,
                reservationClientService,
                rabbitTemplate,
                outboxRepository,
                objectMapper,
                auditLogService);
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
    }

    // ----------------------------------------------------------------
    // processCallback — idempotency
    // ----------------------------------------------------------------

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
            when(paymentRepository.findByReservationId(RES_ID)).thenReturn(Optional.of(paid));

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
            when(paymentRepository.findByReservationId(RES_ID)).thenReturn(Optional.of(paid));

            service.processCallback(VNPAY, params);
            service.processCallback(VNPAY, params);

            verify(paymentRepository, never()).saveAndFlush(any());
            verify(outboxRepository, never()).save(any());
        }
    }

    // ----------------------------------------------------------------
    // processCallback — happy path
    // ----------------------------------------------------------------

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
            when(paymentRepository.findByReservationId(RES_ID)).thenReturn(Optional.of(pending));
            when(paymentRepository.saveAndFlush(any())).thenReturn(pending);

            PaymentHandlerResponse response = service.processCallback(VNPAY, params);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getReservationId()).isEqualTo(RES_ID);

            // Verify payment được lưu với trạng thái PAID
            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).saveAndFlush(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(captor.getValue().getGatewayTxnRef()).isEqualTo("TXN-123");

            // Verify outbox được ghi, KHÔNG publish trực tiếp
            verify(outboxRepository).save(any(Outbox.class));
            verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
        }

        @Test
        @DisplayName("MOMO callback hợp lệ → dùng momoHandler.getId")
        void processCallback_momo_success_shouldUseMomoHandler() {
            Map<String, String> params = Map.of("orderId", RES_ID);
            when(momoHandler.getId(params)).thenReturn(RES_ID);
            when(momoHandler.verifySignature(params)).thenReturn(true);
            when(momoHandler.getTxnRef(params)).thenReturn("TRANS-999");

            Payment pending = buildPayment(RES_ID, PaymentStatus.PENDING, PaymentMethod.MOMO);
            when(paymentRepository.findByReservationId(RES_ID)).thenReturn(Optional.of(pending));
            when(paymentRepository.saveAndFlush(any())).thenReturn(pending);

            PaymentHandlerResponse response = service.processCallback(MOMO, params);

            assertThat(response.isSuccess()).isTrue();
            verify(vnpayHandler, never()).getId(any());
            verify(outboxRepository).save(any(Outbox.class));
        }
    }

    // ----------------------------------------------------------------
    // processCallback — failure path
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
        @DisplayName("signature sai → success=false, không đổi trạng thái")
        void processCallback_invalidSignature_shouldFailWithoutSave() {
            Map<String, String> params = Map.of("vnp_TxnRef", RES_ID);
            when(vnpayHandler.getId(params)).thenReturn(RES_ID);
            when(vnpayHandler.verifySignature(params)).thenReturn(false);

            Payment pending = buildPayment(RES_ID, PaymentStatus.PENDING, PaymentMethod.VNPAY);
            when(paymentRepository.findByReservationId(RES_ID)).thenReturn(Optional.of(pending));

            PaymentHandlerResponse response = service.processCallback(VNPAY, params);

            assertThat(response.isSuccess()).isFalse();
            verify(paymentRepository, never()).saveAndFlush(any());
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
            when(paymentRepository.findByReservationId(RES_ID)).thenReturn(Optional.of(pending));
            when(paymentRepository.saveAndFlush(any()))
                    .thenThrow(new RuntimeException("DB error"));

            PaymentHandlerResponse response = service.processCallback(VNPAY, params);

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getMessage()).isEqualTo("Internal Error");

            // Verify PaymentFailedEvent được publish
            verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
            verify(outboxRepository, never()).save(any());
        }
    }

    // ----------------------------------------------------------------
    // manualCheck
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("manualCheck")
    class ManualCheck {

        @Test
        @DisplayName("payment PAID → manualCheck trả về true")
        void manualCheck_paid_shouldReturnTrue() {
            Payment paid = buildPayment(RES_ID, PaymentStatus.PAID, PaymentMethod.VNPAY);
            when(paymentRepository.findByReservationId(RES_ID)).thenReturn(Optional.of(paid));

            Boolean result = service.manualCheck(VNPAY, RES_ID);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("payment PENDING → manualCheck trả về false")
        void manualCheck_pending_shouldReturnFalse() {
            Payment pending = buildPayment(RES_ID, PaymentStatus.PENDING, PaymentMethod.VNPAY);
            when(paymentRepository.findByReservationId(RES_ID)).thenReturn(Optional.of(pending));

            Boolean result = service.manualCheck(VNPAY, RES_ID);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("không có payment → manualCheck trả về false")
        void manualCheck_noPayment_shouldReturnFalse() {
            when(paymentRepository.findByReservationId(RES_ID)).thenReturn(Optional.empty());

            Boolean result = service.manualCheck(VNPAY, RES_ID);

            assertThat(result).isFalse();
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

            // FIX: truyền đủ 7 tham số
            PaymentServiceImpl svcWithWeird = new PaymentServiceImpl(
                    List.of(vnpayHandler, momoHandler, weirdHandler),
                    paymentRepository,
                    reservationClientService,
                    rabbitTemplate,
                    outboxRepository,
                    objectMapper,
                    auditLogService);

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
}