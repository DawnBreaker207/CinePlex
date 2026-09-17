package com.dawn.report.repository;


import com.dawn.report.dto.response.PaymentDistribution;
import com.dawn.report.dto.response.RevenuePointResponse;
import com.dawn.report.dto.response.TheaterRoomRevenueRow;
import com.dawn.report.dto.response.TopMovieResponse;
import com.dawn.report.dto.response.TopTheaterResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

// Queries checked against the real schema (Flyway V1-V10); aggregate-then-join per metric to avoid fan-out double counting.
@Repository
@RequiredArgsConstructor
public class DashboardRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public Double getTotalRevenue(LocalDate from, LocalDate to, Long movieId, Long theaterId) {
        String sql = """
                SELECT COALESCE(SUM(p.amount), 0) AS totalRevenue
                FROM payment p
                JOIN reservation r ON r.reservation_code = p.reservation_code
                JOIN showtime s ON s.id = r.showtime_id
                LEFT JOIN room rm ON rm.id = s.room_id
                WHERE p.status = 'PAID'
                    AND (:from IS NULL OR p.paid_at >= :from)
                    AND (:to IS NULL OR p.paid_at <= :to)
                    AND (:movieId IS NULL OR s.movie_id = :movieId)
                    AND (:theaterId IS NULL OR rm.theater_id = :theaterId)
                """;

        return jdbcTemplate.queryForObject(
                sql,
                getParams(from, to, movieId, theaterId),
                Double.class
        );
    }

    public Long getTicketsSold(LocalDate from, LocalDate to, Long movieId, Long theaterId) {
        String sql = """
                SELECT COUNT(ti.id) AS ticketsSold
                FROM ticket ti
                JOIN reservation r ON r.id = ti.reservation_id
                    AND r.status = 'CONFIRMED'
                    AND r.is_deleted = false
                JOIN showtime s ON s.id = r.showtime_id
                LEFT JOIN room rm ON rm.id = s.room_id
                WHERE (:from IS NULL OR r.created_at >= :from)
                    AND (:to IS NULL OR r.created_at <= :to)
                	AND (:movieId IS NULL OR s.movie_id = :movieId)
                	AND (:theaterId IS NULL OR rm.theater_id = :theaterId)
                """;
        return jdbcTemplate.queryForObject(
                sql,
                getParams(from, to, movieId, theaterId),
                Long.class
        );
    }

    public Long getActiveTheaters(LocalDate from, LocalDate to, Long movieId, Long theaterId) {
        String sql = """
                SELECT COUNT(DISTINCT rm.theater_id) AS activeTheaters
                FROM reservation r
                JOIN showtime s ON s.id = r.showtime_id
                LEFT JOIN room rm ON rm.id = s.room_id
                WHERE r.status = 'CONFIRMED'
                    AND r.is_deleted = false
                    AND (:from IS NULL OR r.created_at >= :from)
                    AND (:to IS NULL OR r.created_at <= :to)
                	AND (:movieId IS NULL OR s.movie_id = :movieId)
                	AND (:theaterId IS NULL OR rm.theater_id = :theaterId)
                """;
        return jdbcTemplate.queryForObject(
                sql,
                getParams(from, to, movieId, theaterId),
                Long.class
        );
    }

    public Double getSeatUtilization(LocalDate from, LocalDate to, Long theaterId) {
        String sql = """
                SELECT ROUND(AVG(utilization),2) AS seatUtilization
                FROM (
                SELECT
                	CASE
                		WHEN rm.total_seats = 0 THEN 0
                		ELSE SUM(CASE WHEN r.id IS NOT NULL THEN 1 ELSE 0 END)
                            / rm.total_seats * 100
                	END AS utilization
                FROM showtime s
                JOIN room rm ON rm.id = s.room_id
                LEFT JOIN seat_instance si ON si.showtime_id = s.id
                LEFT JOIN reservation r ON r.id = si.reservation_id
                    AND r.status = 'CONFIRMED' AND r.is_deleted = false
                WHERE (:theaterId IS NULL OR rm.theater_id = :theaterId)
                    AND (:from IS NULL OR s.show_date >= :from)
                    AND (:to IS NULL OR s.show_date <= :to)
                GROUP BY s.id
                ) AS t
                """;
        return jdbcTemplate.queryForObject(
                sql,
                getParams(from, to, null, theaterId),
                Double.class
        );
    }

    public List<RevenuePointResponse> getRevenueOverTime(LocalDate from, LocalDate to, Long theaterId) {
        String sql = """
                SELECT
                    DATE(p.paid_at) AS date,
                    COALESCE(SUM(p.amount), 0) AS revenue
                FROM
                    payment p
                JOIN reservation r ON
                    r.reservation_code = p.reservation_code
                JOIN showtime s ON
                    s.id = r.showtime_id
                LEFT JOIN room rm ON
                    rm.id = s.room_id
                WHERE
                    p.status = 'PAID'
                    AND
                    p.paid_at IS NOT NULL
                    AND
                    (:from IS NULL OR p.paid_at >= :from)
                    AND
                    (:to IS NULL OR p.paid_at <= :to)
                    AND
                    (:theaterId IS NULL OR rm.theater_id = :theaterId)
                GROUP BY
                    DATE(p.paid_at)
                ORDER BY
                    DATE(p.paid_at)
                """;
        return jdbcTemplate.query(
                sql,
                getParams(from, to, null, theaterId),
                new BeanPropertyRowMapper<>(RevenuePointResponse.class)
        );
    }

    public List<TopMovieResponse> getTopMovies(LocalDate from, LocalDate to) {
        // aggregate-then-join: tickets and revenue aggregated per movie to avoid double counting
        String sql = """
                SELECT
                	m.title AS movieName,
                	COALESCE(tk.ticketSold, 0) AS ticketSold,
                	COALESCE(pv.revenue, 0) AS revenue
                FROM
                	movie m
                LEFT JOIN (
                	SELECT s.movie_id, COUNT(ti.id) AS ticketSold
                	FROM reservation r
                	JOIN showtime s ON s.id = r.showtime_id
                	JOIN ticket ti ON ti.reservation_id = r.id
                	WHERE r.is_deleted = false
                		AND r.status = 'CONFIRMED'
                		AND (:from IS NULL OR r.created_at >= :from)
                        AND (:to IS NULL OR r.created_at <= :to)
                	GROUP BY s.movie_id
                ) tk ON tk.movie_id = m.id
                LEFT JOIN (
                	SELECT s.movie_id, SUM(p.amount) AS revenue
                	FROM payment p
                	JOIN reservation r ON r.reservation_code = p.reservation_code
                	JOIN showtime s ON s.id = r.showtime_id
                	WHERE p.status = 'PAID'
                		AND (:from IS NULL OR p.paid_at >= :from)
                        AND (:to IS NULL OR p.paid_at <= :to)
                	GROUP BY s.movie_id
                ) pv ON pv.movie_id = m.id
                ORDER BY
                	ticketSold DESC
                LIMIT 5
                """;
        return jdbcTemplate.query(
                sql,
                getParams(from, to, null, null),
                new BeanPropertyRowMapper<>(TopMovieResponse.class)
        );
    }

    public List<TopTheaterResponse> getTopTheaters(LocalDate from, LocalDate to) {
        String sql = """
                SELECT
                	t.name AS theaterName,
                	COALESCE(tk.ticketSold, 0) AS ticketsSold,
                	COALESCE(pv.revenue, 0) AS totalRevenue
                FROM
                	theater t
                LEFT JOIN (
                	SELECT rm.theater_id, COUNT(ti.id) AS ticketSold
                	FROM reservation r
                	JOIN showtime s ON s.id = r.showtime_id
                	JOIN room rm ON rm.id = s.room_id
                	JOIN ticket ti ON ti.reservation_id = r.id
                	WHERE r.is_deleted = false
                		AND r.status = 'CONFIRMED'
                		AND (:from IS NULL OR r.created_at >= :from)
                        AND (:to IS NULL OR r.created_at <= :to)
                	GROUP BY rm.theater_id
                ) tk ON tk.theater_id = t.id
                LEFT JOIN (
                	SELECT rm.theater_id, SUM(p.amount) AS revenue
                	FROM payment p
                	JOIN reservation r ON r.reservation_code = p.reservation_code
                	JOIN showtime s ON s.id = r.showtime_id
                	JOIN room rm ON rm.id = s.room_id
                	WHERE p.status = 'PAID'
                		AND (:from IS NULL OR p.paid_at >= :from)
                        AND (:to IS NULL OR p.paid_at <= :to)
                	GROUP BY rm.theater_id
                ) pv ON pv.theater_id = t.id
                ORDER BY
                	ticketsSold DESC
                LIMIT 5
                """;
        return jdbcTemplate.query(
                sql,
                getParams(from, to, null, null),
                new BeanPropertyRowMapper<>(TopTheaterResponse.class)
        );
    }

    public List<PaymentDistribution> getPaymentDistribution(LocalDate from, LocalDate to) {
        String sql = """
                SELECT
                    p.method AS method,
                    COUNT(DISTINCT r.id) AS count,
                    COALESCE(SUM(p.amount), 0) AS amount
                FROM
                    payment p
                JOIN reservation r ON
                    r.reservation_code = p.reservation_code
                WHERE
                    p.status = 'PAID'
                    AND
                    (:from IS NULL OR p.paid_at >= :from)
                    AND
                    (:to IS NULL OR p.paid_at <= :to)
                GROUP BY
                    p.method
                """;

        return jdbcTemplate.query(
                sql,
                getParams(from, to, null, null),
                new BeanPropertyRowMapper<>(PaymentDistribution.class)
        );
    }

    // Theater -> room hierarchy rows for Jasper; one grain per subquery (payment / ticket / reservation).
    public List<TheaterRoomRevenueRow> getTheaterRoomRevenue(LocalDate from, LocalDate to) {
        String sql = """
                SELECT
                    t.name                      AS theaterName,
                    rm.name                     AS roomName,
                    COALESCE(tk.ticketsSold, 0) AS ticketsSold,
                    COALESCE(bk.bookedValue, 0) AS bookedValue,
                    COALESCE(pd.paidValue, 0)   AS paidRevenue
                FROM theater t
                JOIN room rm ON rm.theater_id = t.id AND rm.is_active = TRUE
                LEFT JOIN (
                    -- booked value of confirmed reservations (grain: reservation)
                    SELECT s.room_id, SUM(r.total_amount) AS bookedValue
                    FROM reservation r
                    JOIN showtime s ON s.id = r.showtime_id
                    WHERE r.is_deleted = FALSE
                        AND r.status = 'CONFIRMED'
                        AND (:from IS NULL OR r.created_at >= :from)
                        AND (:to IS NULL OR r.created_at <= :to)
                    GROUP BY s.room_id
                ) bk ON bk.room_id = rm.id
                LEFT JOIN (
                    -- tickets sold (grain: ticket)
                    SELECT s.room_id, COUNT(ti.id) AS ticketsSold
                    FROM ticket ti
                    JOIN reservation r ON r.id = ti.reservation_id
                        AND r.is_deleted = FALSE
                        AND r.status = 'CONFIRMED'
                    JOIN showtime s ON s.id = r.showtime_id
                    WHERE (:from IS NULL OR r.created_at >= :from)
                        AND (:to IS NULL OR r.created_at <= :to)
                    GROUP BY s.room_id
                ) tk ON tk.room_id = rm.id
                LEFT JOIN (
                    -- revenue collected via payment gateway (grain: payment)
                    SELECT s.room_id, SUM(p.amount) AS paidValue
                    FROM payment p
                    JOIN reservation r ON r.reservation_code = p.reservation_code
                    JOIN showtime s ON s.id = r.showtime_id
                    WHERE p.status = 'PAID'
                        AND (:from IS NULL OR p.paid_at >= :from)
                        AND (:to IS NULL OR p.paid_at <= :to)
                    GROUP BY s.room_id
                ) pd ON pd.room_id = rm.id
                ORDER BY t.name, rm.name
                """;
        return jdbcTemplate.query(
                sql,
                getParams(from, to, null, null),
                new BeanPropertyRowMapper<>(TheaterRoomRevenueRow.class)
        );
    }

    private MapSqlParameterSource getParams(LocalDate from, LocalDate to, Long movieId, Long theaterId) {
        return new MapSqlParameterSource()
                .addValue("from", from != null ? from.atStartOfDay() : null)
                .addValue("to", to != null ? to.atTime(LocalTime.MAX) : null)
                .addValue("movieId", movieId)
                .addValue("theaterId", theaterId);
    }
}
