package com.dawn.booking.repository;

import com.dawn.booking.dto.request.ReservationFilterRequest;
import com.dawn.booking.model.Reservation;
import com.dawn.common.core.constant.ReservationStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByReservationCode(String reservationCode);
    @Query(value = """
              SELECT DISTINCT r FROM Reservation AS r
                   WHERE
                         (:#{#reservation.getQuery()} IS NULL OR r.reservationCode LIKE CONCAT('%', :#{#reservation.getQuery()}, '%') )
                         AND (:#{#reservation.getReservationStatus()} IS NULL OR r.reservationStatus = :#{#reservation.getReservationStatus()})
                         AND (
                            :#{#reservation.getStartDate()} IS NULL
                            OR :#{#reservation.getEndDate()} IS NULL
                            OR ( r.createdAt  BETWEEN :#{#startDate} AND :#{#endDate}) )
                         AND (:#{#reservation.getTotalAmount()} IS NULL OR r.totalAmount = :#{#reservation.getTotalAmount()})
                         AND (:#{#reservation.getIsPaid()} IS NULL OR r.isPaid = :#{#reservation.getIsPaid()})
                    ORDER BY
                        CASE WHEN :#{#reservation.getSortBy()} = 'oldest' THEN r.createdAt END ASC,
                        CASE WHEN :#{#reservation.getSortBy()} = 'newest' THEN r.createdAt END DESC
            """)
    Page<Reservation> findAllWithFilter(ReservationFilterRequest reservation, Instant startDate, Instant endDate, Pageable pageable);

    Page<Reservation> findAllByUserIdAndReservationStatusOrderByCreatedAtDesc(Long userId, ReservationStatus status, Pageable pageable);

    Optional<Reservation> findByIdempotencyKey(String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
            @QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")
    })
    @Query("SELECT r FROM Reservation r WHERE r.reservationStatus = :status AND r.expiredAt < :now ORDER BY r.expiredAt ASC")
    List<Reservation> findExpiredLockedSkipped(@Param("status") ReservationStatus status,
                                               @Param("now") Instant now);
}

