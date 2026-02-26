package com.dawn.payment.handler;

import com.dawn.payment.config.payment.VNPayConfig;
import com.dawn.payment.utils.VNPayUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class VNPayHandler implements PaymentHandler {

    private final VNPayConfig VNPayConfig;

    @Override
    public Boolean supports(String provider) {
        return "vnpay".equalsIgnoreCase(provider);
    }

    @Override
    public String createPaymentUrl(String reservationId, Integer totalAmount, String ipAddress) {
        Map<String, String> vnpParamsMap = VNPayConfig.getVNPayConfig();
        long amount = totalAmount * 100L;

        //  Config default bankCode
        //  This bank code is fixed, you can change it
        vnpParamsMap.put("vnp_BankCode", "NCB");
        vnpParamsMap.put("vnp_TxnRef", reservationId);
        vnpParamsMap.put("vnp_Amount", String.valueOf(amount));
        //  Update Ip Address
        // You can set default IP to "127.0.0.1" or "0.0.0.0" without get IP utils
        vnpParamsMap.put("vnp_IpAddr", ipAddress);
        //	Build Query URL
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
        log.info("Check payment success: {}", checkPaymentSuccess);
        return checkPaymentSuccess;
    }

    @Override
    public String getId(Map<String, String> params) {
        return params.get("vnp_TxnRef");
    }

    @Override
    public Boolean queryTransactions(String reservationId) {
        return true;
    }
}
