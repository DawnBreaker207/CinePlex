package com.dawn.cinema.repository;

import com.dawn.cinema.model.SeatInstance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatInstanceRepository extends JpaRepository<SeatInstance, Long> {

    List<SeatInstance> findAllByShowtimeId(Long showtimeId);

    List<SeatInstance> findAllByReservationId(String reservationId);

    List<SeatInstance> findAllByReservationIdIn(List<String> ids);

    List<SeatInstance> findByShowtimeIdAndStatus(Long showtimeId, String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SeatInstance s WHERE s.id IN :seatIds")
    List<SeatInstance> findByIdWithLock(@Param("seatIds") List<Long> seatIds);
}
