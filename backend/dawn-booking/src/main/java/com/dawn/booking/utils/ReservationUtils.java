package com.dawn.booking.utils;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class ReservationUtils {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789".toCharArray();

    private ReservationUtils() {
    }

    public static String generateReservationCode() {
        StringBuilder suffix = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            suffix.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
        }
        return "CP-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd")) + "-" + suffix;
    }
}