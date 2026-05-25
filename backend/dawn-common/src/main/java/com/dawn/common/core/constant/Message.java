package com.dawn.common.core.constant;

public class Message {

    public static class Exception {
        //    Movie
        public static final String MOVIE_NOT_FOUND = "Movie not found";
        public static final String MOVIE_EXISTED = "Movie was existed";
        //    Reservation
        public static final String RESERVATION_NOT_FOUND = "Reservation not found";
        public static final String RESERVATION_EXISTED = "Reservation existed";
        public static final String RESERVATION_EXPIRED = "Reservation expired or not existed";
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
    }


}
