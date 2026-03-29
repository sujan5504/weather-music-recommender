package com.example.weather_music_recommender.controller;

import com.example.weather_music_recommender.dto.LocationRequest;
import com.example.weather_music_recommender.dto.RecommendationResponse;
import com.example.weather_music_recommender.service.RecommendationService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @PostMapping("/recommendations")
    public RecommendationResponse recommend(
            @Valid @RequestBody LocationRequest request,
            @AuthenticationPrincipal OAuth2User principal,
            @RegisteredOAuth2AuthorizedClient("spotify") OAuth2AuthorizedClient authorizedClient
    ) {
        if (principal == null || authorizedClient == null || authorizedClient.getAccessToken() == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "Spotify authorization is required");
        }

        return recommendationService.recommend(
                request.latitude(),
                request.longitude(),
                authorizedClient.getAccessToken().getTokenValue()
        );
    }
}
