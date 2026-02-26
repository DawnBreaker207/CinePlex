package com.dawn.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherDiscountDTO {

    private String code;

    private BigDecimal originalAmount;

    private BigDecimal discountAmount;

    private BigDecimal finalAmount;
}
