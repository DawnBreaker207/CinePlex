package com.dawn.payment.config.payment;

import com.dawn.common.core.constant.Constants;
import com.dawn.payment.utils.VNPayUtils;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

@Configuration
@Getter
public class VNPayConfig {

    @Value("${payment.VNPay.url}")
    private String vnp_PayUrl;

    @Value("${payment.VNPay.returnUrl}")
    private String vnp_ReturnUrl;

    @Value("${payment.VNPay.tmnCode}")
    private String vnp_TmnCode;

    @Value("${payment.VNPay.secretKey}")
    private String vnp_SecretKey;

    @Value("${payment.VNPay.version}")
    private String vnp_Version;

    @Value("${payment.VNPay.command}")
    private String vnp_Command;

    @Value("${payment.VNPay.orderType}")
    private String vnp_OrderType;

    public Map<String, String> getVNPayConfig() {
        Map<String, String> vnpParamsMap = new HashMap<>();
        vnpParamsMap.put("vnp_Version", this.vnp_Version);
        vnpParamsMap.put("vnp_Command", this.vnp_Command);
        vnpParamsMap.put("vnp_TmnCode", this.vnp_TmnCode);
        vnpParamsMap.put("vnp_CurrCode", "VND");
        vnpParamsMap.put("vnp_OrderInfo", "Thanh toan don hang:" + VNPayUtils.getRandomNumber(Constants.RANDOM_NUMBER_LENGTH));
        vnpParamsMap.put("vnp_OrderType", this.vnp_OrderType);
        vnpParamsMap.put("vnp_Locale", "vn");
        vnpParamsMap.put("vnp_ReturnUrl", this.vnp_ReturnUrl);
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        String vnpCreateDate = formatter.format(calendar.getTime());
        vnpParamsMap.put("vnp_CreateDate", vnpCreateDate);
        calendar.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(calendar.getTime());
        vnpParamsMap.put("vnp_ExpireDate", vnp_ExpireDate);

        return vnpParamsMap;
    }

}
