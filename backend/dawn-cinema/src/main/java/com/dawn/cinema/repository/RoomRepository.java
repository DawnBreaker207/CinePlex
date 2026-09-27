package com.dawn.cinema.repository;

import com.dawn.cinema.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByTheaterId(Long theaterId);

    interface TheaterCapacity {
        Long getTheaterId();
        Long getCapacity();
    }

    @Query("select r.theaterId as theaterId, coalesce(sum(r.totalSeats), 0) as capacity " +
            "from Room r where r.theaterId in :theaterIds and r.isActive = true group by r.theaterId")
    List<TheaterCapacity> sumTotalSeatsByTheaterIds(@Param("theaterIds") List<Long> theaterIds);
}
