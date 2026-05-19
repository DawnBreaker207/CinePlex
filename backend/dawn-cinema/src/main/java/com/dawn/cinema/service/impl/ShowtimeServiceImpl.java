package com.dawn.cinema.service.impl;

import com.dawn.cinema.dto.request.ShowtimeFilterRequest;
import com.dawn.cinema.dto.request.ShowtimeRequest;
import com.dawn.cinema.dto.response.MovieDTO;
import com.dawn.cinema.dto.response.ShowtimeResponse;
import com.dawn.cinema.helper.ShowtimeMappingHelper;
import com.dawn.cinema.model.Seat;
import com.dawn.cinema.model.Showtime;
import com.dawn.cinema.model.Theater;
import com.dawn.cinema.repository.SeatRepository;
import com.dawn.cinema.repository.ShowtimeRepository;
import com.dawn.cinema.repository.TheaterRepository;
import com.dawn.cinema.service.MovieClientCinemaService;
import com.dawn.cinema.service.ShowtimeService;
import com.dawn.common.core.constant.Message;
import com.dawn.common.core.constant.SeatStatus;
import com.dawn.common.core.dto.response.ResponsePage;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShowtimeServiceImpl implements ShowtimeService {
    public static final String SHOWTIME_CACHE = "showtime";

    private final ShowtimeRepository showtimeRepository;

    private final MovieClientCinemaService movieService;

    private final TheaterRepository theaterRepository;

    private final SeatRepository seatRepository;

    @Override
    public List<ShowtimeResponse> getByDate(LocalDate date) {
        log.info("Fetching showtime for date: {}", date);
        return showtimeRepository
                .findByShowDate(date)
                .stream()
                .map(showtime -> {
                    MovieDTO movie = movieService.findOne(showtime.getMovieId());
                    return ShowtimeMappingHelper.map(showtime, movie);
                })
                .toList();
    }

    @Override
    public ResponsePage<ShowtimeResponse> getByMovie(Long movieId, Pageable pageable) {
        log.info("Fetching showtime for movie id: {}", movieId);
        return ResponsePage.of(showtimeRepository
                .findByMovieId(movieId, pageable)
                .map((showtime) -> {
                    MovieDTO movie = movieService.findOne(movieId);
                    return ShowtimeMappingHelper.map(showtime, movie);
                }));
    }

    @Override
    public ResponsePage<ShowtimeResponse> getByTheater(ShowtimeFilterRequest req, Pageable pageable) {
        LocalDate start = req.getStartDate() != null ? req.getStartDate() : LocalDate.now();
        LocalDate end = req.getEndDate() != null ? req.getEndDate() : start.plusDays(30);
        log.info("Fetching showtime for theater id: {}", req.getTheaterId());
        Theater theater = theaterRepository
                .findById(req.getTheaterId())
                .orElseThrow(() -> new ResourceNotFoundException(Message.Exception.THEATER_NOT_FOUND));
        return ResponsePage.of(showtimeRepository
                .findByTheater(theater, start, end, pageable)
                .map((showtime) -> {
                    MovieDTO movie = movieService.findOne(showtime.getMovieId());
                    return ShowtimeMappingHelper.map(showtime, movie);
                }));
    }

    @Override
    public List<ShowtimeResponse> getAvailableShowtime(LocalDate date) {
        log.info("Fetching available showtime from date: {}", date);
        return showtimeRepository
                .findAvailableShowtimeFromDate(date)
                .stream()
                .map(showtime -> {
                    MovieDTO movie = movieService.findOne(showtime.getMovieId());
                    return ShowtimeMappingHelper.map(showtime, movie);
                })
                .toList();
    }

    @Override
    public List<ShowtimeResponse> getAvailableShowtimeForMovie(Long movieId, LocalDate date) {
        log.info("Fetching available showtime for movie id: {} form date {}", movieId, date);
        return showtimeRepository
                .findByShowDateAndMovieId(date, movieId)
                .stream()
                .map(showtime -> {
                    MovieDTO movie = movieService.findOne(showtime.getMovieId());
                    return ShowtimeMappingHelper.map(showtime, movie);
                })
                .toList();
    }

    @Override
    public ShowtimeResponse getById(Long id) {
        log.info("Fetching showtime with id {}", id);
        return showtimeRepository
                .findById(id)
                .map(showtime -> {
                    MovieDTO movie = movieService.findOne(showtime.getMovieId());
                    return ShowtimeMappingHelper.map(showtime, movie);
                })
                .orElseThrow(() -> new ResourceNotFoundException(Message.Exception.SHOWTIME_NOT_FOUND));
    }

    @Override
    @Transactional
    public ShowtimeResponse add(ShowtimeRequest showtimeRequest) {
        log.info("Adding new showtime for movie id: {} at theater id: {}", showtimeRequest.getMovieId(), showtimeRequest.getTheaterId());

        MovieDTO movie = movieService
                .findOne(showtimeRequest.getMovieId());

        Theater theater = theaterRepository
                .findById(showtimeRequest.getTheaterId())
                .orElseThrow(() -> new ResourceNotFoundException(Message.Exception.THEATER_NOT_FOUND));

        if (showtimeRequest.getTotalSeats() > theater.getCapacity()) {
            throw new IllegalArgumentException("Total seats cannot be greater than capacity of " + theater.getCapacity());
        }

        //        All seat available at first
        Showtime showtime = Showtime
                .builder()
                .movieId(movie.getId())
                .theater(theater)
                .showDate(showtimeRequest.getShowDate())
                .showTime(showtimeRequest.getShowTime())
                .totalSeats(showtimeRequest.getTotalSeats())
                .availableSeats(showtimeRequest.getTotalSeats())
                .price(showtimeRequest.getPrice())
                .build();


        Showtime savedShowtime = showtimeRepository.save(showtime);
        log.info("Saved showtime with ID: {}", savedShowtime.getPrice());

//        Create and saved seats
        List<Seat> seats = createSeats(savedShowtime);
        seatRepository.saveAll(seats);


        log.info("Created {} seats for showtime ID: {}", seats.size(), savedShowtime.getId());
        return ShowtimeMappingHelper.map(savedShowtime, movie);
    }

    @Override
    @Transactional
    @Cacheable(value = SHOWTIME_CACHE, key = "'id:' + #id")
    public ShowtimeResponse update(Long id, ShowtimeRequest showtimeDetails) {
        log.info("Updating showtime with id: {}", id);
        Showtime showtime = showtimeRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(Message.Exception.THEATER_NOT_FOUND));

        MovieDTO movie = movieService
                .findOne(showtimeDetails.getMovieId());
        if (showtimeDetails.getMovieId() != null) {
            showtime.setMovieId(movie.getId());
        }

        if (showtimeDetails.getTheaterId() != null) {
            Theater theater = theaterRepository
                    .findById(showtimeDetails.getTheaterId())
                    .orElseThrow(() -> new ResourceNotFoundException(Message.Exception.THEATER_NOT_FOUND));
            showtime.setTheater(theater);
        }

        if (showtimeDetails.getShowDate() != null) {
            showtime.setShowDate(showtimeDetails.getShowDate());
        }

        if (showtimeDetails.getShowTime() != null) {
            showtime.setShowTime(showtimeDetails.getShowTime());
        }

        if (showtimeDetails.getPrice() != null) {
            showtime.setPrice(showtimeDetails.getPrice());
        }

//        DANGER: Don't update total seats or available seats directly - would break reservation!
        Showtime updatedShowtime = showtimeRepository.save(showtime);
        log.info("Updated showtime with ID: {}", updatedShowtime.getId());
        return ShowtimeMappingHelper.map(updatedShowtime, movie);


    }

    @Override
    @Transactional
    @CacheEvict(value = SHOWTIME_CACHE, key = "'id:' + #id")
    public void delete(Long id) {
        log.info("Deleting showtime with id: {}", id);
        Showtime showtime = showtimeRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(Message.Exception.SHOWTIME_NOT_FOUND));

//        Delete the seats first (FK constraint)
        List<Seat> seats = seatRepository.findByShowtime(showtime);

        if (seats != null && !seats.isEmpty()) {
            log.info("Deleting {} seats for showtime ID: {}", seats.size(), id);

            seatRepository.deleteAll(seats);
        }

        showtimeRepository.delete(showtime);
        log.info("Deleted showtime with ID: {}", id);
    }


    private List<Seat> createSeats(Showtime showtime) {
        List<Seat> seats = new ArrayList<>();
        int totalSeats = showtime.getTotalSeats();
        int seatPerRow = 10;

        for (int i = 1; i <= totalSeats; i++) {
            char row = (char) ('A' + (i - 1) / seatPerRow);
            int seatNum = (i - 1) % seatPerRow + 1;

            Seat seat = Seat
                    .builder()
                    .showtime(showtime)
                    .seatNumber(String.format("%c%d", row, seatNum))
                    .status(SeatStatus.AVAILABLE)
                    .build();

            seats.add(seat);
        }

        return seats;
    }
}
