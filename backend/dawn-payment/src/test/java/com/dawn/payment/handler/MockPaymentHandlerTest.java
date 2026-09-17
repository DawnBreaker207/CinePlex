package com.dawn.payment.handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MockPaymentHandler")
class MockPaymentHandlerTest {

    private final MockPaymentHandler handler = handler();

    private static MockPaymentHandler handler() {
        MockPaymentHandler h = new MockPaymentHandler();
        ReflectionTestUtils.setField(h, "backendBaseUrl", "http://localhost:8888");
        return h;
    }

    @Nested
    @DisplayName("supports")
    class Supports {

        @Test
        @DisplayName("matches mock case-insensitively only")
        void matchesMockOnly() {
            assertThat(handler.supports("mock")).isTrue();
            assertThat(handler.supports("MOCK")).isTrue();
            assertThat(handler.supports("vnpay")).isFalse();
        }
    }

    @Nested
    @DisplayName("createPaymentUrl")
    class CreateUrl {

        @Test
        @DisplayName("points back at the real mock callback with params")
        void pointsAtCallback() {
            String url = handler.createPaymentUrl("CP-123", 150000, "127.0.0.1");

            assertThat(url).isEqualTo("http://localhost:8888/api/v1/payment/callback/mock"
                    + "?reservationId=CP-123&amount=150000&status=SUCCESS");
        }
    }

    @Nested
    @DisplayName("callback params")
    class CallbackParams {

        @Test
        @DisplayName("SUCCESS passes, anything else fails")
        void signatureByStatus() {
            assertThat(handler.verifySignature(Map.of("status", "SUCCESS"))).isTrue();
            assertThat(handler.verifySignature(Map.of("status", "FAILED"))).isFalse();
            assertThat(handler.verifySignature(Map.of())).isFalse();
        }

        @Test
        @DisplayName("id, amount and unique txn ref resolve")
        void paramsResolve() {
            Map<String, String> params = Map.of("reservationId", "CP-123", "amount", "150000");

            assertThat(handler.getId(params)).isEqualTo("CP-123");
            assertThat(handler.getAmount(params)).isEqualByComparingTo(new BigDecimal("150000"));
            assertThat(handler.getTxnRef(params)).matches("MOCK-[0-9A-F]{12}");
            assertThat(handler.getTxnRef(params)).isNotEqualTo(handler.getTxnRef(params));
        }

        @Test
        @DisplayName("missing amount resolves to null")
        void missingAmountNull() {
            assertThat(handler.getAmount(Map.of())).isNull();
        }
    }
}
