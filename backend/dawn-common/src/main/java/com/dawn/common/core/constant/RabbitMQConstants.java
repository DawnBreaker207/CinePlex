package com.dawn.common.core.constant;

public class RabbitMQConstants {

    // Notification Flow
    public static final String EXCHANGE_NOTIFICATION = "notification.exchange";
    public static final String RK_NOTIFICATION_RESERVATION_COMPLETED = "notification.booking.completed";
    public static final String QUEUE_NOTIFICATION_RESERVATION_COMPLETED = "notification.booking.completed";
    public static final String RK_NOTIFICATION_EMAIL_VERIFICATION = "notification.email.verification";
    public static final String QUEUE_NOTIFICATION_EMAIL_VERIFICATION = "notification.email.verification";
    public static final String RK_NOTIFICATION_PASSWORD_RESET = "notification.password.reset";
    public static final String QUEUE_NOTIFICATION_PASSWORD_RESET = "notification.password.reset";

    // Dashboard Flow
    public static final String RK_DASHBOARD_REFRESH = "dashboard.refresh";
    public static final String QUEUE_DASHBOARD = "dashboard.refresh";

    // Payment flow
    public static final String EXCHANGE_PAYMENT = "payment.exchange";
    public static final String EXCHANGE_PAYMENT_DLX = "payment.dlx";

    public static final String RK_PAYMENT_COMPLETED = "payment.completed";
    public static final String RK_PAYMENT_FAILED = "payment.failed";
    public static final String RK_PAYMENT_REFUND_REQUESTED = "payment.refund.requested";
    public static final String QUEUE_PAYMENT_REFUND_REQUESTED = "payment.refund.requested";
    public static final String QUEUE_PAYMENT_REFUND_REQUESTED_DLQ = "payment.refund.requested.dlq";

    // Reservation Flow
    public static final String QUEUE_BOOKING_PAYMENT_COMPLETED = "booking.payment.completed";
    public static final String QUEUE_BOOKING_PAYMENT_COMPLETED_DLQ = "booking.payment.completed.dlq";
    public static final String QUEUE_BOOKING_PAYMENT_FAILED = "booking.payment.failed";
    public static final String QUEUE_BOOKING_PAYMENT_FAILED_DLQ = "booking.payment.failed.dlq";

    // Cinema Flow
    public static final String EXCHANGE_CINEMA = "cinema.exchange";
    public static final String RK_CINEMA_SHOWTIME_CANCELED = "cinema.showtime.canceled";
    public static final String QUEUE_CINEMA_SHOWTIME_CANCELED = "cinema.showtime.canceled";

    // Notification Flow
    public static final String RK_NOTIFICATION_SHOWTIME_CANCELED = "notification.showtime.canceled";
    public static final String QUEUE_NOTIFICATION_SHOWTIME_CANCELED = "notification.showtime.canceled";
}
