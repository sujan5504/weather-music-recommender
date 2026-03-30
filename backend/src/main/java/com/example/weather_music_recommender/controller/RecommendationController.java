package com.example.weather_music_recommender.controller;

import com.example.weather_music_recommender.dto.LocationRequest;
import com.example.weather_music_recommender.dto.RecommendationResponse;
import com.example.weather_music_recommender.service.RecommendationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @PostMapping("/recommendations")
    public RecommendationResponse recommend(@Valid @RequestBody LocationRequest request) {
        return recommendationService.recommend(request.latitude(), request.longitude());
    }
}
