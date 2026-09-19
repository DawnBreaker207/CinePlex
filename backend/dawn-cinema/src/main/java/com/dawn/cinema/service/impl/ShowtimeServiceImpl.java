package com.dawn.cinema.service.impl;

import com.dawn.catalog.api.CatalogModuleApi;
import com.dawn.catalog.dto.response.MovieResponse;
import com.dawn.cinema.dto.request.ShowtimeFilterRequest;
import com.dawn.cinema.dto.request.ShowtimeRequest;
import com.dawn.cinema.dto.response.ShowtimeResponse;
import com.dawn.cinema.helper.ShowtimeMappingHelper;
import com.dawn.cinema.model.*;
import com.dawn.cinema.repository.*;
import com.dawn.cinema.service.ShowtimeService;
import com.dawn.common.core.constant.Constants;
import com.dawn.common.core.constant.ErrorCode;
import com.dawn.common.core.constant.RabbitMQConstants;
import com.dawn.common.core.constant.SeatStatus;
import com.dawn.common.core.dto.event.ShowtimeCanceledEvent;
import com.dawn.common.core.dto.response.ResponsePage;
import com.dawn.common.core.exception.wrapper.InvalidRequestException;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShowtimeServiceImpl implements ShowtimeService {
    public static final String SHOWTIME_CACHE = "showtime";

    private final ShowtimeRepository showtimeRepository;

    private final CatalogModuleApi catalogModuleApi;

    private final TheaterRepository theaterRepository;

    private final RoomRepository roomRepository;

    private final SeatTemplateRepository seatTemplateRepository;

    private final SeatInstanceRepository seatInstanceRepository;

    private final RabbitTemplate rabbitTemplate;

    private static final String STATUS_CANCELED = "CANCELED";

    private void enrichSeatCounts(List<ShowtimeResponse> responses) {
        if (responses.isEmpty()) return;
        List<Long> ids = responses.stream().map(ShowtimeResponse::getId).distinct().toList();
        Map<Long, Long> totalByShowtime = seatInstanceRepository.countTotalByShowtimeIds(ids)
                .stream().collect(Collectors.toMap(
                        SeatInstanceRepository.SeatCount::getShowtimeId,
                        SeatInstanceRepository.SeatCount::getTotal));
        Map<Long, Long> availableByShowtime = seatInstanceRepository.countAvailableByShowtimeIds(ids)
                .stream().collect(Collectors.toMap(
                        SeatInstanceRepository.SeatCount::getShowtimeId,
                        SeatInstanceRepository.SeatCount::getTotal));
        responses.forEach(r -> {
            r.setTotalSeats(totalByShowtime.getOrDefault(r.getId(), 0L).intValue());
            r.setAvailableSeats(availableByShowtime.getOrDefault(r.getId(), 0L).intValue());
        });
    }

    @Override
    public List<ShowtimeResponse> getByDate(LocalDate date) {
        log.info("Fetching showtime for date: {}", date);
        List<ShowtimeResponse> responses = showtimeRepository
                .findByShowDate(date)
                .stream()
                .map(showtime -> {
                    MovieResponse movie = catalogModuleApi.findMovieById(showtime.getMovieId());
                    Theater theater = resolveTheater(showtime.getRoomId());
                    return ShowtimeMappingHelper.map(showtime, movie, theater);
                })
                .toList();
        enrichSeatCounts(responses);
        return responses;
    }

    @Override
    public ResponsePage<ShowtimeResponse> getByMovie(Long movieId, Pageable pageable) {
        log.info("Fetching showtime for movie id: {}", movieId);
        Page<ShowtimeResponse> page = showtimeRepository
                .findByMovieId(movieId, pageable)
                .map((showtime) -> {
                    MovieResponse movie = catalogModuleApi.findMovieById(movieId);
                    Theater theater = resolveTheater(showtime.getRoomId());
                    return ShowtimeMappingHelper.map(showtime, movie, theater);
                });
        enrichSeatCounts(page.getContent());
        return ResponsePage.of(page);
    }

    @Override
    public ResponsePage<ShowtimeResponse> getByTheater(ShowtimeFilterRequest req, Pageable pageable) {
        LocalDate start = req.getStartDate() != null ? req.getStartDate() : LocalDate.now();
        LocalDate end = req.getEndDate() != null ? req.getEndDate() : start.plusDays(30);
        log.info("Fetching showtime for theater id: {}", req.getTheaterId());
        Theater theater = theaterRepository
                .findById(req.getTheaterId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.THEATER_NOT_FOUND.format()));
        Page<Showtime> showtimePage = showtimeRepository
                .findByTheater(theater.getId(), start, end, pageable);
        List<Long> movieIds = showtimePage.getContent()
                .stream()
                .map(Showtime::getMovieId)
                .distinct()
                .toList();
        Map<Long, MovieResponse> movieCache = catalogModuleApi
                .findMoviesByIds(movieIds)
                .stream()
                .collect(Collectors.toMap(MovieResponse::getId, Function.identity()));
        Page<ShowtimeResponse> page = showtimePage.map(
                showtime -> ShowtimeMappingHelper.map(
                        showtime,
                        movieCache.get(showtime.getMovieId()),
                        theater));
        enrichSeatCounts(page.getContent());
        return ResponsePage.of(page);
    }

    @Override
    public List<ShowtimeResponse> getAvailableShowtime(LocalDate date) {
        log.info("Fetching available showtime from date: {}", date);
        List<ShowtimeResponse> responses = showtimeRepository
                .findAvailableShowtimeFromDate(date)
                .stream()
                .map(showtime -> {
                    MovieResponse movie = catalogModuleApi.findMovieById(showtime.getMovieId());
                    Theater theater = resolveTheater(showtime.getRoomId());
                    return ShowtimeMappingHelper.map(showtime, movie, theater);
                })
                .toList();
        enrichSeatCounts(responses);
        return responses;
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

        Map<Long, MovieResponse> movieMap = catalogModuleApi.findMoviesByIds(movieIds)
                .stream()
                .collect(Collectors.toMap(MovieResponse::getId, m -> m));
        List<ShowtimeResponse> responses = showtimes.stream()
                .map(showtime -> {
                    Theater theater = resolveTheater(showtime.getRoomId());
                    return ShowtimeMappingHelper.map(
                            showtime,
                            movieMap.get(showtime.getMovieId()),
                            theater);
                })
                .toList();
        enrichSeatCounts(responses);
        return responses;
    }

    @Override
    public List<ShowtimeResponse> getAvailableShowtimeForMovie(Long movieId, LocalDate date) {
        log.info("Fetching available showtime for movie id: {} form date {}", movieId, date);
        List<ShowtimeResponse> responses = showtimeRepository
                .findByShowDateAndMovieId(date, movieId)
                .stream()
                .map(showtime -> {
                    MovieResponse movie = catalogModuleApi.findMovieById(showtime.getMovieId());
                    Theater theater = resolveTheater(showtime.getRoomId());
                    return ShowtimeMappingHelper.map(showtime, movie, theater);
                })
                .toList();
        enrichSeatCounts(responses);
        return responses;
    }

    @Override
    public ShowtimeResponse getById(Long id) {
        log.info("Fetching showtime with id {}", id);
        return showtimeRepository
                .findById(id)
                .map(showtime -> {
                    MovieResponse movie = catalogModuleApi.findMovieById(showtime.getMovieId());
                    Theater theater = resolveTheater(showtime.getRoomId());
                    ShowtimeResponse response = ShowtimeMappingHelper.map(showtime, movie, theater);
                    response.setAvailableSeats(
                            (int) seatInstanceRepository.findByShowtimeIdAndStatus(id, SeatStatus.AVAILABLE.name()).size());
                    return response;
                })
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.SHOWTIME_NOT_FOUND.format()));
    }

    @Override
    @Transactional
    public ShowtimeResponse add(ShowtimeRequest showtimeRequest) {
        log.info("Adding new showtime for movie id: {} at theater id: {}", showtimeRequest.getMovieId(), showtimeRequest.getTheaterId());

        MovieResponse movie = catalogModuleApi
                .findMovieById(showtimeRequest.getMovieId());

        Theater theater = theaterRepository
                .findById(showtimeRequest.getTheaterId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.THEATER_NOT_FOUND.format()));

        List<Room> rooms = roomRepository.findByTheaterId(theater.getId());
        if (rooms.isEmpty()) {
            throw new IllegalArgumentException("No rooms found for theater: " + theater.getId());
        }
        Room room = rooms.getFirst();

        checkNoOverlap(room.getId(), showtimeRequest.getShowDate(), showtimeRequest.getShowTime(),
                movie.getDuration(), null);

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
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.THEATER_NOT_FOUND.format()));

        MovieResponse movie = catalogModuleApi
                .findMovieById(showtimeDetails.getMovieId());
        if (showtimeDetails.getMovieId() != null) {
            showtime.setMovieId(movie.getId());
        }

        if (showtimeDetails.getTheaterId() != null) {
            List<Room> rooms = roomRepository.findByTheaterId(showtimeDetails.getTheaterId());
            if (!rooms.isEmpty()) {
                showtime.setRoomId(rooms.getFirst().getId());
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

        MovieResponse durationSource = showtimeDetails.getMovieId() != null
                ? movie
                : catalogModuleApi.findMovieById(showtime.getMovieId());
        checkNoOverlap(showtime.getRoomId(), showtime.getShowDate(), showtime.getShowTime(),
                durationSource != null ? durationSource.getDuration() : null, id);

        Showtime updatedShowtime = showtimeRepository.save(showtime);
        log.info("Updated showtime with ID: {}", updatedShowtime.getId());
        Theater theater = resolveTheater(updatedShowtime.getRoomId());
        return ShowtimeMappingHelper.map(updatedShowtime, movie, theater);
    }

    @Override
    @Transactional
    @CacheEvict(value = SHOWTIME_CACHE, key = "'id:' + #id")
    public ShowtimeResponse cancelShowtime(Long id) {
        Showtime showtime = showtimeRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.SHOWTIME_NOT_FOUND.format()));
        if (!STATUS_CANCELED.equals(showtime.getStatus())) {
            showtime.setStatus(STATUS_CANCELED);
            showtimeRepository.save(showtime);
            try {
                rabbitTemplate.convertAndSend(
                        RabbitMQConstants.EXCHANGE_CINEMA,
                        RabbitMQConstants.RK_CINEMA_SHOWTIME_CANCELED,
                        ShowtimeCanceledEvent.builder().showtimeId(id).build());
            } catch (RuntimeException e) {
                log.warn("Showtime canceled event not queued for {}", id, e);
            }
        }
        Theater theater = resolveTheater(showtime.getRoomId());
        MovieResponse movie = catalogModuleApi.findMovieById(showtime.getMovieId());
        return ShowtimeMappingHelper.map(showtime, movie, theater);
    }

    @Override
    @Transactional
    @CacheEvict(value = SHOWTIME_CACHE, key = "'id:' + #id")
    public void delete(Long id) {
        log.info("Deleting showtime with id: {}", id);
        Showtime showtime = showtimeRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.SHOWTIME_NOT_FOUND.format()));

        List<SeatInstance> seats = seatInstanceRepository.findAllByShowtimeId(id);

        if (seats != null && !seats.isEmpty()) {
            log.info("Deleting {} seats for showtime ID: {}", seats.size(), id);
            seatInstanceRepository.deleteAll(seats);
        }

        showtimeRepository.delete(showtime);
        log.info("Deleted showtime with ID: {}", id);
    }

    private static final int FALLBACK_DURATION_MINUTES = 120;

    private void checkNoOverlap(Long roomId, LocalDate date, LocalTime start,
            Integer durationMinutes, Long excludeId) {
        if (roomId == null || date == null || start == null) {
            return;
        }
        LocalDateTime newStart = LocalDateTime.of(date, start);
        LocalDateTime newEnd = newStart.plusMinutes(durationMinutes != null ? durationMinutes : FALLBACK_DURATION_MINUTES);
        List<Showtime> existing = showtimeRepository.findByRoomIdAndShowDate(roomId, date);
        Map<Long, Integer> durations = catalogModuleApi
                .findMoviesByIds(existing.stream()
                        .map(Showtime::getMovieId).filter(Objects::nonNull).distinct().toList())
                .stream()
                .collect(Collectors.toMap(MovieResponse::getId, MovieResponse::getDuration, (a, b) -> a));
        for (Showtime s : existing) {
            if ((excludeId != null && excludeId.equals(s.getId()))
                    || s.getShowTime() == null
                    || STATUS_CANCELED.equals(s.getStatus())) {
                continue;
            }
            LocalDateTime sStart = LocalDateTime.of(s.getShowDate(), s.getShowTime());
            Integer d = durations.get(s.getMovieId());
            LocalDateTime sEnd = sStart.plusMinutes(d != null ? d : FALLBACK_DURATION_MINUTES);
            if (newStart.isBefore(sEnd) && sStart.isBefore(newEnd)) {
                throw new InvalidRequestException(ErrorCode.SHOWTIME_OVERLAP);
            }
        }
    }

    private void createSeatInstances(Showtime showtime) {        List<SeatTemplate> templates = seatTemplateRepository.findByRoomId(showtime.getRoomId());
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
