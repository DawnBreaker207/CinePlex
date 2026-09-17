package com.dawn.catalog.controller;

import com.dawn.catalog.dto.request.MovieRequest;
import com.dawn.catalog.dto.response.MovieResponse;
import com.dawn.catalog.internal.MovieService;
import com.dawn.common.core.dto.response.ResponseObject;
import com.dawn.common.core.dto.response.ResponsePage;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/movie")
@Tag(name = "Movie", description = "Operations related to movie")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    @GetMapping("")
    public ResponseObject<ResponsePage<MovieResponse>> findAll(@ModelAttribute MovieRequest m, Pageable pageable) {
        return ResponseObject.success(movieService.findAll(m, pageable));
    }

    @GetMapping("/{id}")
    public ResponseObject<MovieResponse> findById(@PathVariable Long id) {
        return ResponseObject.success(movieService.findById(id));
    }

    @GetMapping("/filmId/{id}")
    public ResponseObject<MovieResponse> findByMovieId(@PathVariable String id) {
        return ResponseObject.success(movieService.findByMovieId(id));
    }

    @PostMapping("/batch")
    public ResponseObject<List<MovieResponse>> findAllByIds(@RequestBody List<Long> ids) {
        return ResponseObject.success(movieService.findAllByIds(ids));
    }

    @PostMapping("")
    @RateLimiter(name = "limit")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    public ResponseObject<MovieResponse> create(@Valid @RequestBody MovieRequest m) {
        return ResponseObject.created(movieService.create(m));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    public ResponseObject<MovieResponse> update(@PathVariable Long id, @Valid @RequestBody MovieRequest m) {
        return ResponseObject.success(movieService.update(id, m));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    public ResponseObject<Void> delete(@PathVariable Long id) {
        movieService.delete(id);
        return ResponseObject.deleted();
    }

}
