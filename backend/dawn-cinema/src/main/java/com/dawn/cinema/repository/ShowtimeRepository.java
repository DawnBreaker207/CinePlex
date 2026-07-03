package com.dawn.cinema.repository;

import com.dawn.cinema.model.Showtime;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {
    List<Showtime> findByShowDate(LocalDate date);

    List<Showtime> findByIdIn(List<Long> ids);

    Page<Showtime> findByMovieId(Long movieId, Pageable pageable);

    @Query("SELECT s.id FROM Showtime s JOIN Room r ON s.roomId = r.id WHERE r.theaterId = :theaterId")
    List<Long> findShowtimeByTheaterId(@Param("theaterId") Long theaterId);

    @Query(value = """
               SELECT s FROM Showtime s
               JOIN Room r ON s.roomId = r.id
               WHERE
                  (:#{#theaterId} IS NULL OR r.theaterId = :#{#theaterId})
               AND (
                   :#{#from} IS NULL
                   OR :#{#to} IS NULL
                   OR (s.showDate BETWEEN :#{#from} AND :#{#to})
               )
               ORDER BY s.showDate DESC
            """)
    Page<Showtime> findByTheater(Long theaterId, LocalDate from, LocalDate to, Pageable pageable);

    List<Showtime> findByShowDateAndMovieId(LocalDate date, Long movieId);

    @Query("SELECT s FROM Showtime s WHERE s.showDate >= :date")
    List<Showtime> findAvailableShowtimeFromDate(LocalDate date);
}
