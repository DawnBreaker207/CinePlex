package com.dawn.cinema.controller;

import com.dawn.cinema.dto.request.ShowtimeFilterRequest;
import com.dawn.cinema.dto.request.ShowtimeRequest;
import com.dawn.cinema.dto.response.ShowtimeResponse;
import com.dawn.cinema.service.ShowtimeService;
import com.dawn.common.core.dto.response.ResponseObject;
import com.dawn.common.core.dto.response.ResponsePage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/showtime")
@Tag(name = "Showtime", description = "Operations related to showtime")
@RequiredArgsConstructor
@Slf4j
public class ShowtimeController {

    private final ShowtimeService showtimeService;

    @GetMapping()
    @Operation(summary = "Get showtime by date", description = "Returns showtime for a specific date")
    public ResponseObject<List<ShowtimeResponse>> getShowtimeByDate(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDate date) {
        log.info("Fetching showtime for date: {}", date);
        return ResponseObject.success(showtimeService.getByDate(date));
    }

    @PostMapping("/batch")
    public ResponseObject<List<ShowtimeResponse>> findAllByIds(@RequestBody List<Long> ids) {
        return ResponseObject.success(showtimeService.findAllByIds(ids));
    }

    @GetMapping("/movies/{movieId}")
    @Operation(summary = "Get showtime by movie", description = "Returns showtime for a specific movie")
    public ResponseObject<ResponsePage<ShowtimeResponse>> getShowtimeByMovie(@PathVariable Long movieId, Pageable pageable) {
        log.info("Fetching showtime for movie id: {}", movieId);
        return ResponseObject.success(showtimeService.getByMovie(movieId, pageable));
    }

    @GetMapping("/theaters/{theaterId}")
    @Operation(summary = "Get showtime by theater", description = "Returns available for a specific theater")
    public ResponseObject<ResponsePage<ShowtimeResponse>> getShowtimeByTheater(@ModelAttribute ShowtimeFilterRequest req, Pageable pageable) {
        log.info("Fetching showtime for theater id: {}", req.getTheaterId());
        return ResponseObject.success(showtimeService.getByTheater(req, pageable));
    }

    @GetMapping("/available")
    @Operation(summary = "Get available showtime", description = "Returns available showtime from a specific date")
    public ResponseObject<List<ShowtimeResponse>> getAvailableShowtime(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate searchDate = date != null ? date : LocalDate.now();
        log.info("Fetching available showtime from date: {}", searchDate);
        return ResponseObject.success(showtimeService.getAvailableShowtime(searchDate));
    }

    @GetMapping("/available/movies/{movieId}")
    @Operation(summary = "Get available showtime for a movie", description = "Returns available showtime for a specific movie from a date")
    public ResponseObject<List<ShowtimeResponse>> getAvailableShowtimeForMovie(@PathVariable Long movieId, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate searchDate = date != null ? date : LocalDate.now();
        log.info("Fetching available showtime from movie id: {} from date {}", movieId, searchDate);
        return ResponseObject.success(showtimeService.getAvailableShowtimeForMovie(movieId, searchDate));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get showtime by ID", description = "Returns a showtime by its ID")
    public ResponseObject<ShowtimeResponse> getShowtimeById(@PathVariable Long id) {
        log.info("Fetching showtime with id: {}", id);
        return ResponseObject.success(showtimeService.getById(id));
    }

    @PostMapping()
    @Operation(summary = "Add a new showtime", description = "Create a new showtime (Admin only)")
    public ResponseObject<ShowtimeResponse> add(@Valid @RequestBody ShowtimeRequest showtimeRequest) {
        log.info("Create showtime: {}", showtimeRequest);
        return ResponseObject.created(showtimeService.add(showtimeRequest));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a showtime", description = "Updates an existing showtime (Admin only)")
    public ResponseObject<ShowtimeResponse> update(@PathVariable Long id, @Valid @RequestBody ShowtimeRequest showtimeDetails) {
        log.info("Updating showtime with id: {}", id);
        return ResponseObject.success(showtimeService.update(id, showtimeDetails));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a showtime", description = "Delete an existing theater (Admin only)")
    public ResponseObject<Void> delete(@PathVariable Long id) {
        log.info("Deleting showtime with id: {}", id);
        showtimeService.delete(id);
        return ResponseObject.deleted();
    }
}
