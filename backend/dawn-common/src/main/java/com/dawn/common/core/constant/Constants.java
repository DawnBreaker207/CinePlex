package com.dawn.common.core.constant;

public final class Constants {
    private Constants() {}

    // ==================== Redis Hash Field Names ====================
    public static final String REDIS_RESERVATION_ID = "reservationId";
    public static final String REDIS_USER_ID = "userId";
    public static final String REDIS_SHOWTIME_ID = "showtimeId";
    public static final String REDIS_THEATER_ID = "theaterId";
    public static final String REDIS_PRICE = "price";
    public static final String REDIS_SEAT_IDS = "seatIds";
    public static final String REDIS_VOUCHER_CODE = "voucherCode";
    public static final String REDIS_TEMP_FINAL_AMOUNT = "tempFinalAmount";

    // ==================== Cache Names ====================
    public static final String CACHE_DASHBOARD_METRICS = "dashboard:metrics";
    public static final String CACHE_DASHBOARD_REVENUE = "dashboard:revenue";
    public static final String CACHE_DASHBOARD_TOP_MOVIES = "dashboard:topMovies";
    public static final String CACHE_DASHBOARD_TOP_THEATERS = "dashboard:topTheaters";
    public static final String CACHE_DASHBOARD_PAYMENT_DISTRIBUTION = "dashboard:paymentDistribution";

    // ==================== Magic Numbers ====================
    public static final int RESERVATION_HOLD_MINUTES = 15;
    public static final int PROCESSING_LOCK_TTL_SECONDS = 5;
    public static final int DEFAULT_DASHBOARD_DAYS = 30;
    public static final int RANDOM_NUMBER_LENGTH = 8;
    public static final int BEARER_TOKEN_LENGTH = 7;
    public static final long SSE_EMITTER_TIMEOUT_MS = 300_000L;
    public static final int DEFAULT_CACHE_TTL_MINUTES = 10;
    public static final int INTERNAL_CONNECT_TIMEOUT_SECONDS = 3;
    public static final int INTERNAL_READ_TIMEOUT_SECONDS = 5;
    public static final int EXTERNAL_CONNECT_TIMEOUT_SECONDS = 5;
    public static final int EXTERNAL_READ_TIMEOUT_SECONDS = 10;

    // ==================== SSE Event Names ====================
    public static final String SSE_SEAT_STATE_INIT = "SEAT_STATE_INIT";
    public static final String SSE_SEAT_HOLD = "SEAT_HOLD";
    public static final String SSE_SEAT_RELEASE = "SEAT_RELEASE";
    public static final String SSE_CONNECTED = "CONNECTED";

    // ==================== SSE JSON Field Names ====================
    public static final String SSE_FIELD_EVENT = "event";
    public static final String SSE_FIELD_SHOWTIME_ID = "showtimeId";
    public static final String SSE_FIELD_SEAT_IDS = "seatIds";
    public static final String SSE_FIELD_USER_ID = "userId";
    public static final String SSE_FIELD_ACTION = "action";
}
