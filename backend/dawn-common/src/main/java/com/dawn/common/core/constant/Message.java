package com.dawn.common.core.constant;

import java.text.MessageFormat;

public class Message {

    private Message() {
    }

    public static String format(String template, Object... args) {
        return MessageFormat.format(template, args);
    }

    public static class Exception {
        //    Movie
        public static final String MOVIE_NOT_FOUND = "Movie not found";
        public static final String MOVIE_EXISTED = "Movie was existed";
        //    Reservation
        public static final String RESERVATION_NOT_FOUND = "Reservation not found";
        public static final String RESERVATION_EXISTED = "Reservation existed";
        public static final String RESERVATION_EXPIRED = "Reservation expired or not existed";
        public static final String RESERVATION_PAST_SHOWTIME = "Cannot reserve seats for past showtime";
        public static final String RESERVATION_NOT_ENOUGH_SEATS = "Not enough available seats. Request: {0}, Available: {1}";
        public static final String RESERVATION_INVALID_DATA = "Invalid reservation data";
        public static final String RESERVATION_SEATS_NOT_FOUND_DB = "Some seats not found in database";
        public static final String SEAT_WRONG_SHOWTIME = "Seats {0} do not belong to the requested showtime";
        public static final String FAILED_STORE_SEAT = "Failed to store seat information. Please try again";
        public static final String FAILED_SEAT_LOCK = "Failed to acquire seat lock, please try again";
        public static final String SEAT_HELD_BY_ANOTHER = "Seat {0} was held by another";
        public static final String INVALID_REDIS_FORMAT = "Invalid {0} format in Redis";
        public static final String INVALID_REDIS_DATA = "Info in redis not exists or error when getting that";
        //    Showtime
        public static final String SHOWTIME_NOT_FOUND = "Showtime not found";
        //    Theater
        public static final String THEATER_NOT_FOUND = "Theater not found";
        //    Seat
        public static final String NO_SEAT_SELECTED = "No seats selected in this reservation";
        public static final String SEAT_NOT_FOUND = "Seat not found";
        public static final String SEAT_UNAVAILABLE = "Seat already booked or no longer unavailable";

        //Refresh Token
        public static final String REFRESH_TOKEN_NOT_FOUND = "Refresh token not found";
        public static final String REFRESH_TOKEN_EXPIRED = "Refresh token was expired, Please make a new log in request";
        //    User
        public static final String USER_NOT_FOUND = "User not found";
        public static final String USERNAME_EXISTED = "Username already exists";
        public static final String USERNAME_NOT_FOUND = "Username not found";
        public static final String EMAIL_NOT_FOUND = "Email not found";
        public static final String EMAIL_EXISTED = "Email already exists";
        public static final String PASSWORD_NOT_MATCH = "Password not match";
        //    Role
        public static final String ROLE_NOT_FOUND = "Role not found";
        public static final String PERMISSION_FORBIDDEN = "You don't have permission";
        //    Payment
        public static final String PAYMENT_COMPLETE = "Payment already for this reservation";
        public static final String PAYMENT_NOT_FOUND = "Payment not found";
        // Service
        public static final String INTERNAL_SERVICE_ERROR = "Internal Service Error";
        // Voucher
        public static final String VOUCHER_NOT_FOUND = "Voucher not found";
        public static final String VOUCHER_EXPIRED = "Voucher has expired";
        public static final String VOUCHER_OUT_OF_STOCK = "Voucher already taken, please try again";
        public static final String VOUCHER_CONFLICT = "Voucher conflict";
        public static final String VOUCHER_INACTIVE = "Voucher is inactive";
        public static final String VOUCHER_ALREADY_CLAIMED = "Voucher already claimed";
        public static final String VOUCHER_MAX_PER_USER = "Voucher usage limit reached";
        public static final String VOUCHER_NOT_STARTED = "Voucher has not started yet";
        public static final String VOUCHER_INVALID = "Voucher is invalid or not eligible for application";
        public static final String VOUCHER_NO_RESPONSE = "No response received from Voucher system";
    }


}
