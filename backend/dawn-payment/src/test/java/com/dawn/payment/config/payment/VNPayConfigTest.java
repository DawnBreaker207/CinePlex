package com.dawn.payment.config.payment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("VNPayConfig")
class VNPayConfigTest {

    VNPayConfig config;

    @BeforeEach
    void setUp() {
        config = new VNPayConfig();
        ReflectionTestUtils.setField(config, "vnp_PayUrl", "https://pay.vnpay.vn");
        ReflectionTestUtils.setField(config, "vnp_ReturnUrl", "https://cineplex.com/payment/callback");
        ReflectionTestUtils.setField(config, "vnp_TmnCode", "TESTCODE");
        ReflectionTestUtils.setField(config, "vnp_SecretKey", "test-secret-key");
        ReflectionTestUtils.setField(config, "vnp_Version", "2.1.0");
        ReflectionTestUtils.setField(config, "vnp_Command", "pay");
        ReflectionTestUtils.setField(config, "vnp_OrderType", "other");
    }

    @Test
    @DisplayName("getVNPayConfig → contains all required keys")
    void getVNPayConfig_shouldContainAllRequiredKeys() {
        Map<String, String> params = config.getVNPayConfig();

        assertThat(params)
                .containsKey("vnp_Version")
                .containsKey("vnp_Command")
                .containsKey("vnp_TmnCode")
                .containsKey("vnp_CurrCode")
                .containsKey("vnp_TxnRef")
                .containsKey("vnp_OrderInfo")
                .containsKey("vnp_OrderType")
                .containsKey("vnp_Locale")
                .containsKey("vnp_ReturnUrl")
                .containsKey("vnp_CreateDate")
                .containsKey("vnp_ExpireDate");
    }

    @Test
    @DisplayName("getVNPayConfig → vnp_TxnRef is 8-digit number")
    void getVNPayConfig_txnRef_shouldBe8Digits() {
        Map<String, String> params = config.getVNPayConfig();

        assertThat(params.get("vnp_TxnRef")).matches("\\d{8}");
    }

    @Test
    @DisplayName("getVNPayConfig → vnp_OrderInfo has correct prefix")
    void getVNPayConfig_orderInfo_shouldHaveCorrectPrefix() {
        Map<String, String> params = config.getVNPayConfig();

        assertThat(params.get("vnp_OrderInfo")).startsWith("Thanh toan don hang:");
    }

    @Test
    @DisplayName("getVNPayConfig → vnp_CreateDate and vnp_ExpireDate are valid timestamps")
    void getVNPayConfig_dates_shouldBeValidFormat() {
        Map<String, String> params = config.getVNPayConfig();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        LocalDateTime createDate = LocalDateTime.parse(params.get("vnp_CreateDate"), formatter);
        LocalDateTime expireDate = LocalDateTime.parse(params.get("vnp_ExpireDate"), formatter);

        assertThat(expireDate).isAfter(createDate);
        assertThat(expireDate).isEqualTo(createDate.plusMinutes(15));
    }

    @Test
    @DisplayName("getVNPayConfig → static values are correct")
    void getVNPayConfig_staticValues_shouldBeCorrect() {
        Map<String, String> params = config.getVNPayConfig();

        assertThat(params.get("vnp_CurrCode")).isEqualTo("VND");
        assertThat(params.get("vnp_Locale")).isEqualTo("vn");
        assertThat(params.get("vnp_ReturnUrl")).isEqualTo("https://cineplex.com/payment/callback");
    }
}
