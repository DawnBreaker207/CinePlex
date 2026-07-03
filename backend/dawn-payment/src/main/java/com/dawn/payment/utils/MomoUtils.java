package com.dawn.payment.utils;

import com.dawn.payment.config.payment.MomoConfig;
import com.dawn.common.core.exception.wrapper.InternalServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@Slf4j
public class MomoUtils {

    public static String buildRawSignature(Map<String, String> params, String accessKey) {
        return String.format(
                "accessKey=%s&amount=%s&extraData=%s&ipnUrl=%s&orderId=%s&orderInfo=%s&partnerCode=%s&redirectUrl=%s&requestId=%s&requestType=%s",
                accessKey,
                params.get("amount"),
                params.get("extraData"),
                params.get("ipnUrl"),
                params.get("orderId"),
                params.get("orderInfo"),
                params.get("partnerCode"),
                params.get("redirectUrl"),
                params.get("requestId"),
                params.get("requestType")
        );
    }

    public static String hmacSHA256(final String key, final String data) {
        try {
            Mac hmacSha256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmacSha256.init(secretKey);
            byte[] hash = hmacSha256.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Failed to sign HMAC SHA256", e);
            throw new InternalServiceException(e.getMessage());
        }
    }

    public static String sign(Map<String, String> params, MomoConfig config) {
        String rawSignature = buildRawSignature(params, config.getMomo_AccessKey());
        log.info("Raw signature: {}", rawSignature);

        String signature = hmacSHA256(config.getMomo_SecretKey(), rawSignature);
        log.info("Signed signature: {}", signature);

        return signature;
    }
}
