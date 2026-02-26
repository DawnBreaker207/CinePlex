package com.dawn.payment.handler;

import com.dawn.payment.config.payment.MomoConfig;
import com.dawn.payment.utils.MomoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MomoHandler implements PaymentHandler {

    private final MomoConfig momoConfig;

    private final RestClient restClient;

    @Override
    public Boolean supports(String provider) {
        return "momo".equalsIgnoreCase(provider);
    }

    @Override
    public String createPaymentUrl(String reservationId, Integer amount, String ipAddress) {
        Map<String, String> momoParamsMap = momoConfig.getMomoConfig();

        momoParamsMap.put("orderId", reservationId);
        momoParamsMap.put("orderInfo", "Thanh toan don hang: " + reservationId);
        momoParamsMap.put("requestId", UUID.randomUUID().toString());
        momoParamsMap.put("amount", String.valueOf(amount));
        momoParamsMap.put("extraData", "Khong co khuyen mai");
        momoParamsMap.put("signature", MomoUtils.sign(momoParamsMap, momoConfig));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String momoApiUrl = momoConfig.getMomo_PayUrl() + "/create";
        Map<String, Object> response = restClient.post()
                .uri(momoApiUrl)
                .body(momoParamsMap)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        if (response == null || response.get("payUrl") == null) {
            throw new RuntimeException("MoMo payment failed or payUrl is null");
        }
        return (String) response.get("payUrl");
    }

    @Override
    public Boolean verifySignature(Map<String, String> params) {
        String signature = params.get("signature");
        String calculateSign = MomoUtils.sign(params, momoConfig);
        return calculateSign.equalsIgnoreCase(signature) && "0".equals(params.get("resultCode"));
    }

    @Override
    public String getId(Map<String, String> params) {
        return params.get("orderId");
    }

    @Override
    public Boolean queryTransactions(String reservationId) {
        return true;
    }
}
