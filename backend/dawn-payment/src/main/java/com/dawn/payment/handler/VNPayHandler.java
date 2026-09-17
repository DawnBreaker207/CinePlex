package com.dawn.payment.handler;

import com.dawn.payment.config.payment.VNPayConfig;
import com.dawn.payment.utils.VNPayUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class VNPayHandler implements PaymentHandler {

    private static final long VND_MULTIPLIER = 100L;

    private final VNPayConfig VNPayConfig;

    @Override
    public Boolean supports(String provider) {
        return "vnpay".equalsIgnoreCase(provider);
    }

    @Override
    public String createPaymentUrl(String reservationId, Integer totalAmount, String ipAddress) {
        Map<String, String> vnpParamsMap = VNPayConfig.getVNPayConfig();
        long amount = totalAmount * VND_MULTIPLIER;

        vnpParamsMap.put("vnp_BankCode", "NCB");
        vnpParamsMap.put("vnp_TxnRef", reservationId);
        vnpParamsMap.put("vnp_Amount", String.valueOf(amount));
        vnpParamsMap.put("vnp_IpAddr", ipAddress);
        String queryUrl = VNPayUtils.getPaymentURL(vnpParamsMap, true);
        String hashData = VNPayUtils.getPaymentURL(vnpParamsMap, false);
        String vnpSecureHash = VNPayUtils.hmacSHA512(VNPayConfig.getVnp_SecretKey(), hashData);
        queryUrl += "&vnp_SecureHash=" + vnpSecureHash;
        return VNPayConfig.getVnp_PayUrl() + "?" + queryUrl;
    }

    @Override
    public Boolean verifySignature(Map<String, String> params) {
        String vnp_SecureHash = params.get("vnp_SecureHash");

        Map<String, String> vnp_Params = new HashMap<>(params);
        vnp_Params.remove("vnp_SecureHash");
        vnp_Params.remove("vnp_SecureHashType");
        String data = VNPayUtils.getPaymentURL(vnp_Params, false);
        String hash = VNPayUtils.hmacSHA512(VNPayConfig.getVnp_SecretKey(), data);
        boolean checkPaymentSuccess = hash.equalsIgnoreCase(vnp_SecureHash) && "00".equalsIgnoreCase(params.get("vnp_ResponseCode"));
        log.debug("Check payment success: {}", checkPaymentSuccess);
        return checkPaymentSuccess;
    }

    @Override
    public String getId(Map<String, String> params) {
        return params.get("vnp_TxnRef");
    }

    @Override
    public String getTxnRef(Map<String, String> params) {
        return params.get("vnp_TransactionNo");
    }

    @Override
    public BigDecimal getAmount(Map<String, String> params) {
        //  vnp_Amount is in VND * 100
        String amount = params.get("vnp_Amount");
        if (amount == null || amount.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(amount).divide(BigDecimal.valueOf(VND_MULTIPLIER));
        } catch (NumberFormatException e) {
            log.warn("Invalid vnp_Amount '{}'", amount);
            return null;
        }
    }
}
