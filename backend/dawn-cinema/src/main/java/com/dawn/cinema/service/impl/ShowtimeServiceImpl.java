package com.dawn.cinema.service.impl;

import com.dawn.cinema.client.MovieClientCinemaService;
import com.dawn.cinema.dto.request.ShowtimeFilterRequest;
import com.dawn.cinema.dto.request.ShowtimeRequest;
import com.dawn.cinema.dto.response.MovieDTO;
import com.dawn.cinema.dto.response.ShowtimeResponse;
import com.dawn.cinema.helper.ShowtimeMappingHelper;
import com.dawn.cinema.model.*;
import com.dawn.cinema.repository.*;
import com.dawn.cinema.service.ShowtimeService;
import com.dawn.common.core.constant.Constants;
import com.dawn.common.core.constant.Message;
import com.dawn.common.core.constant.SeatStatus;
import com.dawn.common.core.dto.response.ResponsePage;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShowtimeServiceImpl implements ShowtimeService {
    public static final String SHOWTIME_CACHE = "showtime";

    private final ShowtimeRepository showtimeRepository;

    private final MovieClientCinemaService movieService;

    private final TheaterRepository theaterRepository;

    private final RoomRepository roomRepository;

    private final SeatTemplateRepository seatTemplateRepository;

    private final SeatInstanceRepository seatInstanceRepository;

    @Override
    public List<ShowtimeResponse> getByDate(LocalDate date) {
        log.info("Fetching showtime for date: {}", date);
        return showtimeRepository
                .findByShowDate(date)
                .stream()
                .map(showtime -> {
                    MovieDTO movie = movieService.findOne(showtime.getMovieId());
                    Theater theater = resolveTheater(showtime.getRoomId());
                    return ShowtimeMappingHelper.map(showtime, movie, theater);
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
                    Theater theater = resolveTheater(showtime.getRoomId());
                    return ShowtimeMappingHelper.map(showtime, movie, theater);
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
        Page<Showtime> showtimePage = showtimeRepository
                .findByTheater(theater.getId(), start, end, pageable);
        List<Long> movieIds = showtimePage.getContent()
                .stream()
                .map(Showtime::getMovieId)
                .distinct()
                .toList();
        Map<Long, MovieDTO> movieCache = movieService
                .findAllByIds(movieIds)
                .stream()
                .collect(Collectors.toMap(MovieDTO::getId, Function.identity()));
        return ResponsePage.of(
                showtimePage.map(
                        showtime -> ShowtimeMappingHelper.map(
                                showtime,
                                movieCache.get(showtime.getMovieId()),
                                theater)));
    }

    @Override
    public List<ShowtimeResponse> getAvailableShowtime(LocalDate date) {
        log.info("Fetching available showtime from date: {}", date);
        return showtimeRepository
                .findAvailableShowtimeFromDate(date)
                .stream()
                .map(showtime -> {
                    MovieDTO movie = movieService.findOne(showtime.getMovieId());
                    Theater theater = resolveTheater(showtime.getRoomId());
                    return ShowtimeMappingHelper.map(showtime, movie, theater);
                })
                .toList();
    }

    @Override
    public List<ShowtimeResponse> findAllByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();

        log.info("Batch fetching {} showtimes", ids.size());

        List<Showtime> showtimes = showtimeRepository.findByIdIn(ids);

        List<Long> movieIds = showtimes.stream()
                .map(Showtime::getMovieId)
                .distinct()
                .toList();

        Map<Long, MovieDTO> movieMap = movieService.findAllByIds(movieIds)
                .stream()
                .collect(Collectors.toMap(MovieDTO::getId, m -> m));
        return showtimes.stream()
                .map(showtime -> {
                    Theater theater = resolveTheater(showtime.getRoomId());
                    return ShowtimeMappingHelper.map(
                            showtime,
                            movieMap.get(showtime.getMovieId()),
                            theater);
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
                    Theater theater = resolveTheater(showtime.getRoomId());
                    return ShowtimeMappingHelper.map(showtime, movie, theater);
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
                    Theater theater = resolveTheater(showtime.getRoomId());
                    return ShowtimeMappingHelper.map(showtime, movie, theater);
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

        List<Room> rooms = roomRepository.findByTheaterId(theater.getId());
        if (rooms.isEmpty()) {
            throw new IllegalArgumentException("No rooms found for theater: " + theater.getId());
        }
        Room room = rooms.get(0);

        Showtime showtime = Showtime
                .builder()
                .movieId(movie.getId())
                .roomId(room.getId())
                .showDate(showtimeRequest.getShowDate())
                .showTime(showtimeRequest.getShowTime())
                .price(showtimeRequest.getPrice())
                .build();

        Showtime savedShowtime = showtimeRepository.save(showtime);
        log.info("Saved showtime with ID: {}", savedShowtime.getPrice());

        createSeatInstances(savedShowtime);

        log.info("Created seat instances for showtime ID: {}", savedShowtime.getId());
        return ShowtimeMappingHelper.map(savedShowtime, movie, theater);
    }

    @Override
    @Transactional
    @CachePut(value = SHOWTIME_CACHE, key = "'id:' + #id")
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
            List<Room> rooms = roomRepository.findByTheaterId(showtimeDetails.getTheaterId());
            if (!rooms.isEmpty()) {
                showtime.setRoomId(rooms.get(0).getId());
            }
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

        Showtime updatedShowtime = showtimeRepository.save(showtime);
        log.info("Updated showtime with ID: {}", updatedShowtime.getId());
        Theater theater = resolveTheater(updatedShowtime.getRoomId());
        return ShowtimeMappingHelper.map(updatedShowtime, movie, theater);
    }

    @Override
    @Transactional
    @CacheEvict(value = SHOWTIME_CACHE, key = "'id:' + #id")
    public void delete(Long id) {
        log.info("Deleting showtime with id: {}", id);
        Showtime showtime = showtimeRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(Message.Exception.SHOWTIME_NOT_FOUND));

        List<SeatInstance> seats = seatInstanceRepository.findAllByShowtimeId(id);

        if (seats != null && !seats.isEmpty()) {
            log.info("Deleting {} seats for showtime ID: {}", seats.size(), id);
            seatInstanceRepository.deleteAll(seats);
        }

        showtimeRepository.delete(showtime);
        log.info("Deleted showtime with ID: {}", id);
    }

    private void createSeatInstances(Showtime showtime) {
        List<SeatTemplate> templates = seatTemplateRepository.findByRoomId(showtime.getRoomId());
        List<SeatInstance> seats = new ArrayList<>();

        for (SeatTemplate template : templates) {
            SeatInstance seat = SeatInstance
                    .builder()
                    .showtimeId(showtime.getId())
                    .seatTemplateId(template.getId())
                    .status(SeatStatus.AVAILABLE.name())
                    .price("VIP".equals(template.getSeatType()) && showtime.getVipPrice() != null
                            ? showtime.getVipPrice()
                            : showtime.getPrice())
                    .build();
            seats.add(seat);
        }

        seatInstanceRepository.saveAll(seats);
    }

    private Theater resolveTheater(Long roomId) {
        if (roomId == null) return null;
        return roomRepository.findById(roomId)
                .map(room -> theaterRepository.findById(room.getTheaterId()).orElse(null))
                .orElse(null);
    }
}
