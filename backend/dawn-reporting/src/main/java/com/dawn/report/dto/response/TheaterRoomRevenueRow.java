package com.dawn.report.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// One theater -> room hierarchy row; field names match DashboardRepository aliases and report.jrxml <field> entries.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TheaterRoomRevenueRow {
    private String theaterName;
    private String roomName;
    private Long ticketsSold;
    private BigDecimal bookedValue;
    private BigDecimal paidRevenue;
}
