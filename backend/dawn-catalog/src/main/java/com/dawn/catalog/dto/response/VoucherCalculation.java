package com.dawn.catalog.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class VoucherCalculation {

    private String code;

    private BigDecimal originalAmount;

    private BigDecimal discountAmount;

    private BigDecimal finalAmount;
}
