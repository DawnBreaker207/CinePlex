package com.dawn.booking.utils;

import java.util.UUID;

public final class ReservationUtils {

    private ReservationUtils() {
    }

    public static String generateReservationId() {
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        return String.format("ORD-" + uuid);
    }
}