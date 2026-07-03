package com.dawn.payment.handler;

import com.dawn.payment.config.payment.MomoConfig;
import com.dawn.payment.utils.MomoUtils;
import com.dawn.common.core.exception.wrapper.InternalServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.springframework.core.ParameterizedTypeReference;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MomoHandler")
class MomoHandlerTest {

    @Mock
    RestClient restClient;
    @Mock
    RestClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock(answer = Answers.RETURNS_SELF)
    RestClient.RequestBodySpec requestBodySpec;
    @Mock
    RestClient.ResponseSpec responseSpec;

    MomoConfig momoConfig;
    MomoHandler handler;

    @BeforeEach
    void setUp() {
        momoConfig = new MomoConfig();
        ReflectionTestUtils.setField(momoConfig, "momo_PartnerCode", "MOMO_TEST");
        ReflectionTestUtils.setField(momoConfig, "momo_PayUrl", "https://test-payment.momo.vn");
        ReflectionTestUtils.setField(momoConfig, "momo_AccessKey", "test-access-key");
        ReflectionTestUtils.setField(momoConfig, "momo_SecretKey", "test-secret-key");
        ReflectionTestUtils.setField(momoConfig, "momo_RedirectUrl", "https://cineplex.com/payment/callback");
        ReflectionTestUtils.setField(momoConfig, "momo_OrderType", "captureWallet");
        ReflectionTestUtils.setField(momoConfig, "momo_IpnUrl", "https://cineplex.com/payment/ipn");

        handler = new MomoHandler(momoConfig, restClient);
    }

    @Nested
    @DisplayName("supports")
    class Supports {

        @Test
        @DisplayName("MOMO → true")
        void supports_momo_shouldReturnTrue() {
            assertThat(handler.supports("MOMO")).isTrue();
        }

        @Test
        @DisplayName("momo lowercase → true")
        void supports_momoLowercase_shouldReturnTrue() {
            assertThat(handler.supports("momo")).isTrue();
        }

        @Test
        @DisplayName("VNPAY → false")
        void supports_vnpay_shouldReturnFalse() {
            assertThat(handler.supports("VNPAY")).isFalse();
        }
    }

    @Nested
    @DisplayName("createPaymentUrl")
    class CreatePaymentUrl {

        @Test
        @DisplayName("success → returns payUrl from response")
        void createPaymentUrl_success_shouldReturnPayUrl() {
            when(restClient.post()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
            when(requestBodySpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(Map.of("payUrl", "https://momo.vn/pay/123"));

            String payUrl = handler.createPaymentUrl("RES-001", 100000, "127.0.0.1");

            assertThat(payUrl).isEqualTo("https://momo.vn/pay/123");
        }

        @Test
        @DisplayName("null response → throw InternalServiceException")
        void createPaymentUrl_nullResponse_shouldThrow() {
            when(restClient.post()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
            when(requestBodySpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(null);

            assertThatThrownBy(() -> handler.createPaymentUrl("RES-001", 100000, "127.0.0.1"))
                    .isInstanceOf(InternalServiceException.class);
        }

        @Test
        @DisplayName("payUrl null in response → throw InternalServiceException")
        void createPaymentUrl_missingPayUrl_shouldThrow() {
            when(restClient.post()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
            when(requestBodySpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(Map.of());

            assertThatThrownBy(() -> handler.createPaymentUrl("RES-001", 100000, "127.0.0.1"))
                    .isInstanceOf(InternalServiceException.class);
        }
    }

    @Nested
    @DisplayName("verifySignature")
    class VerifySignature {

        @Test
        @DisplayName("correct signature and resultCode 0 → true")
        void verifySignature_correct_shouldReturnTrue() {
            try (MockedStatic<MomoUtils> mocked = mockStatic(MomoUtils.class)) {
                mocked.when(() -> MomoUtils.sign(any(), any())).thenReturn("matching-signature");

                Map<String, String> params = Map.of(
                        "signature", "matching-signature",
                        "resultCode", "0"
                );

                assertThat(handler.verifySignature(params)).isTrue();
            }
        }

        @Test
        @DisplayName("non-zero resultCode → false even with correct signature")
        void verifySignature_nonZeroResultCode_shouldReturnFalse() {
            try (MockedStatic<MomoUtils> mocked = mockStatic(MomoUtils.class)) {
                mocked.when(() -> MomoUtils.sign(any(), any())).thenReturn("matching-signature");

                Map<String, String> params = Map.of(
                        "signature", "matching-signature",
                        "resultCode", "1"
                );

                assertThat(handler.verifySignature(params)).isFalse();
            }
        }
    }

    @Nested
    @DisplayName("getId")
    class GetId {

        @Test
        @DisplayName("returns orderId from params")
        void getId_shouldReturnOrderId() {
            Map<String, String> params = Map.of("orderId", "RES-001");

            String id = handler.getId(params);

            assertThat(id).isEqualTo("RES-001");
        }
    }
}
