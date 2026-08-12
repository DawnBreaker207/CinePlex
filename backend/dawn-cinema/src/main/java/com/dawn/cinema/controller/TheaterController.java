package com.dawn.cinema.controller;

import com.dawn.cinema.dto.request.TheaterRequest;
import com.dawn.cinema.dto.response.TheaterResponse;
import com.dawn.cinema.service.TheaterService;
import com.dawn.common.core.dto.response.ResponseObject;
import com.dawn.common.core.dto.response.ResponsePage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/theater")
@Tag(name = "Theater", description = "Operations related to theater")
@RequiredArgsConstructor
@Slf4j
public class TheaterController {

    private final TheaterService theaterService;

    @GetMapping("")
    @Operation(summary = "Get all theaters", description = "Return a list of all theaters")
    public ResponseObject<ResponsePage<TheaterResponse>> findAll(Pageable pageable) {
        return ResponseObject.success(theaterService.findAll(pageable));
    }

    @GetMapping("/search")
    @Operation(summary = "Search theaters by location", description = "Return a theaters matching the location search term")
    public ResponseObject<ResponsePage<TheaterResponse>> searchTheaterByLocation(@RequestParam(required = false) String location, Pageable pageable) {
        log.info("Search theater by location {}", location);
        return ResponseObject.success(theaterService.findByLocation(location, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get theater by ID", description = "Return theater by its ID")
    public ResponseObject<TheaterResponse> findOne(@PathVariable Long id) {
        return ResponseObject.success(theaterService.findOne(id));
    }

    @PostMapping("")
    @Operation(summary = "Add a new theater", description = "Create a new theater (Admin only)")
    public ResponseObject<TheaterResponse> create(@Valid @RequestBody TheaterRequest theater) {
        return ResponseObject.created(theaterService.create(theater));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a theater", description = "Updates an existing theater (Admin only)")
    public ResponseObject<TheaterResponse> update(@PathVariable Long id, @Valid @RequestBody TheaterRequest theater) {
        return ResponseObject.success(theaterService.update(id, theater));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a theater", description = "Delete an existing theater (Admin only)")
    public ResponseObject<Void> remove(@PathVariable Long id) {
        theaterService.remove(id);
        return ResponseObject.deleted();
    }

}
