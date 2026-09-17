package com.dawn.ai.controller;

import com.dawn.ai.service.RecommendationService;
import com.dawn.catalog.dto.response.MovieResponse;
import com.dawn.common.core.dto.response.ResponseObject;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/recommendations")
    public ResponseObject<List<MovieResponse>> recommend(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer limit) {
        return ResponseObject.success(recommendationService.recommend(userId, limit));
    }
}
