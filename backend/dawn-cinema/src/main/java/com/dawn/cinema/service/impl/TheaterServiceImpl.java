package com.dawn.cinema.service.impl;

import com.dawn.cinema.dto.request.TheaterRequest;
import com.dawn.cinema.dto.response.TheaterResponse;
import com.dawn.cinema.helper.TheaterMappingHelper;
import com.dawn.cinema.model.Theater;
import com.dawn.cinema.repository.ShowtimeRepository;
import com.dawn.cinema.repository.TheaterRepository;
import com.dawn.cinema.service.TheaterService;
import com.dawn.common.core.constant.Message;
import com.dawn.common.core.dto.response.ResponsePage;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TheaterServiceImpl implements TheaterService {
    public static final String THEATER_CACHE = "theater";

    private final TheaterRepository theaterRepository;
    private final ShowtimeRepository showtimeRepository;

    private Map<Long, List<Long>> showtimeIdsByTheater(List<Long> theaterIds) {
        return showtimeRepository.findShowtimeByTheaterIds(theaterIds).stream()
                .collect(Collectors.groupingBy(
                        row -> (Long) row[0],
                        Collectors.mapping(row -> (Long) row[1], Collectors.toList())));
    }

    private ResponsePage<TheaterResponse> mapPage(Page<Theater> page) {
        List<Long> theaterIds = page.getContent().stream().map(Theater::getId).toList();
        Map<Long, List<Long>> showtimeIdsByTheater = theaterIds.isEmpty()
                ? Map.of()
                : showtimeIdsByTheater(theaterIds);
        return ResponsePage.of(page.map(theater ->
                TheaterMappingHelper.map(theater, showtimeIdsByTheater.getOrDefault(theater.getId(), List.of()))));
    }

    @Override
    @Cacheable(value = THEATER_CACHE)
    public ResponsePage<TheaterResponse> findAll(Pageable pageable) {
        return mapPage(theaterRepository.findAll(pageable));
    }

    @Override
    @Cacheable(value = THEATER_CACHE, key = "'location:' + #location")
    public ResponsePage<TheaterResponse> findByLocation(String location, Pageable pageable) {
        log.info("Search theater by location {}", location);
        return mapPage(theaterRepository.findByLocationContainingIgnoreCase(location, pageable));
    }

    @Override
    @Cacheable(value = THEATER_CACHE, key = "'id:' + #id")
    public TheaterResponse findOne(Long id) {
        return theaterRepository
                .findById(id)
                .map(theater -> TheaterMappingHelper.map(
                        theater,
                        showtimeRepository.findShowtimeByTheaterId(theater.getId())))
                .orElseThrow(() -> new ResourceNotFoundException(Message.Exception.THEATER_NOT_FOUND));
    }

    @Override
    @Transactional
    @CachePut(value = THEATER_CACHE, key = "'id:' + #result.id")
    public TheaterResponse create(TheaterRequest request) {
        log.info("Add new theater: {}", request);
        Theater theater = TheaterMappingHelper.map(request);
        return TheaterMappingHelper.map(theaterRepository.save(theater));
    }

    @Override
    @Transactional
    @CachePut(value = THEATER_CACHE, key = "'id:' + #id")
    public TheaterResponse update(Long id, TheaterRequest theaterDetails) {
        Theater theater = theaterRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(Message.Exception.THEATER_NOT_FOUND));
        theater.setName(theaterDetails.getName());
        theater.setLocation(theaterDetails.getLocation());
        return TheaterMappingHelper.map(theaterRepository.save(theater));
    }

    @Override
    @Transactional
    @CacheEvict(value = THEATER_CACHE, key = "'id:' + #id")
    public void remove(Long id) {
        theaterRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(Message.Exception.THEATER_NOT_FOUND));
        theaterRepository.deleteById(id);
    }


}
