package com.dawn.cinema.repository;

import com.dawn.cinema.model.Theater;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TheaterRepository extends JpaRepository<Theater, Long> {
    Page<Theater> findAll(Pageable pageable);

    Page<Theater> findByLocationContainingIgnoreCase(String location, Pageable pageable);

    Theater findByName(String theaterName);
}
