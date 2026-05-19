package com.dawn.catalog.service.impl;

import com.dawn.catalog.dto.request.MovieRequest;
import com.dawn.catalog.dto.response.MovieResponse;
import com.dawn.catalog.helper.MovieMappingHelper;
import com.dawn.catalog.model.Genre;
import com.dawn.catalog.model.Movie;
import com.dawn.catalog.repository.GenreRepository;
import com.dawn.catalog.repository.MovieRepository;
import com.dawn.catalog.service.MovieService;
import com.dawn.common.core.constant.Message;
import com.dawn.common.core.dto.response.ResponsePage;
import com.dawn.common.core.exception.wrapper.ResourceAlreadyExistedException;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MovieServiceImpl implements MovieService {
    public static final String CACHE_INFO = "movie_info";
    public static final String CACHE_LIST = "movie_list";
    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;


    @Override
    @Cacheable(value = CACHE_LIST, key = "T(java.util.Objects).hash(#m) + '-'+ #pageable.pageNumber + '-'+ #pageable.pageSize")
    public ResponsePage<MovieResponse> findAll(MovieRequest m, Pageable pageable) {
        return ResponsePage.of(movieRepository
                .findAllWithFilter(m, pageable)
                .map(MovieMappingHelper::map));
    }

    @Override
    @Cacheable(value = CACHE_INFO, key = "#id")
    public MovieResponse findById(Long id) {
        return movieRepository
                .findById(id)
                .map(MovieMappingHelper::map)
                .orElseThrow(() -> new ResourceNotFoundException(Message.Exception.MOVIE_NOT_FOUND));
    }

    @Override
    @Cacheable(value = CACHE_INFO, key = "'filmId:' + #id")
    public MovieResponse findByMovieId(String id) {
        return movieRepository
                .findByFilmId(id)
                .map(MovieMappingHelper::map)
                .orElseThrow(() -> new ResourceNotFoundException(Message.Exception.MOVIE_NOT_FOUND));
    }

    @Override
    @Transactional
    @Caching(
            put = {@CachePut(value = CACHE_INFO, key = "#result.id")},
            evict = {@CacheEvict(value = CACHE_LIST, allEntries = true)}
    )
    public MovieResponse create(MovieRequest m) {
        movieRepository
                .findByFilmId(String.valueOf(m.getFilmId()))
                .ifPresent((movie) -> {
                    throw new ResourceAlreadyExistedException(Message.Exception.MOVIE_EXISTED);
                });
        Set<Genre> genres = checkExistedGenre(m.getGenres());
        Movie movie = MovieMappingHelper.map(m);
        movie.setGenres(genres);
        return MovieMappingHelper.map(movieRepository.save(movie));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_INFO, key = "#id"),
            @CacheEvict(value = CACHE_LIST, allEntries = true),
    })
    public MovieResponse update(Long id, MovieRequest movieDetails) {

        Movie movie = movieRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(Message.Exception.MOVIE_NOT_FOUND));

        Set<Genre> genres = checkExistedGenre(movieDetails.getGenres());

        movie.setTitle(movieDetails.getTitle());
        movie.setOriginalTitle(movieDetails.getOriginalTitle());
        movie.setPoster(movieDetails.getPoster());
        movie.setBackdrop(movieDetails.getBackdrop());
        movie.setOverview(movieDetails.getOverview());
        movie.setDuration(movieDetails.getDuration());
        movie.setGenres(genres);
        movie.setReleaseDate(movieDetails.getReleaseDate());
        movie.setLanguage(movieDetails.getLanguage());
        movie.setCountry(movieDetails.getCountry());
        movie.setFilmId(movieDetails.getFilmId());
        movie.setImdbId(movieDetails.getImdbId());
        return MovieMappingHelper.map(movieRepository.save(movie));

    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = CACHE_INFO, key = "#id"),
            @CacheEvict(value = CACHE_LIST, allEntries = true)
    })
    public void delete(Long id) {
        movieRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(Message.Exception.MOVIE_NOT_FOUND));

        movieRepository.deleteById(id);
    }

    private Set<Genre> checkExistedGenre(Set<String> genreNames) {
        List<Genre> existedGenres = genreRepository.findAll();

        Map<String, Genre> existingNames = existedGenres
                .stream()
                .collect(Collectors
                        .toMap(Genre::getName, g -> g));

        Set<Genre> result = new HashSet<>();
        for (String name : genreNames) {
            Genre genre = existingNames.get(name);
            if (genre == null) {
                genre = genreRepository.save(Genre.builder().name(name).build());
            }
            result.add(genre);
        }
        return result;
    }
}
