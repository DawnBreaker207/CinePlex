package com.dawn.cinema.repository;

import com.dawn.cinema.model.SeatTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatTemplateRepository extends JpaRepository<SeatTemplate, Long> {
    List<SeatTemplate> findByRoomId(Long roomId);
}
