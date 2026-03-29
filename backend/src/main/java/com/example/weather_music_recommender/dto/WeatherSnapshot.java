package com.example.weather_music_recommender.dto;

public record WeatherSnapshot(
        String city,
        String country,
        String condition,
        String description,
        double temperature,
        double feelsLike,
        int humidity,
        double windSpeed
) {
}
