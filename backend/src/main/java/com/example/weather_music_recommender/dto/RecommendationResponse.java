package com.example.weather_music_recommender.dto;

import java.util.List;

public record RecommendationResponse(
        WeatherSnapshot weather,
        String mood,
        List<String> genres,
        List<TrackRecommendation> tracks
) {
}
