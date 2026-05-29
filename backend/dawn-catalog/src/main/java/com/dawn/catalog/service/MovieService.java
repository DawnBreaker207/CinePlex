package com.dawn.catalog.service;

import com.dawn.catalog.dto.request.MovieRequest;
import com.dawn.catalog.dto.response.MovieResponse;
import com.dawn.common.core.dto.response.ResponsePage;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MovieService {
    ResponsePage<MovieResponse> findAll(MovieRequest m, Pageable pageable);

    List<MovieResponse> findAllByIds(List<Long> ids);

    MovieResponse findByMovieId(String id);

    MovieResponse findById(Long id);

    MovieResponse create(MovieRequest id);

    MovieResponse update(Long id, MovieRequest m);

    void delete(Long id);
}
