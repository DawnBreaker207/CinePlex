package com.dawn.booking.client;

import com.dawn.booking.dto.response.VoucherDiscountDTO;

import java.math.BigDecimal;

public interface VoucherClientService {
    public VoucherDiscountDTO calculateVoucher(String code, BigDecimal totalAmount);

    public void useVoucher(String code, Long userId, String reservationId);

    public void releaseVoucher(String code, Long userId);
}
