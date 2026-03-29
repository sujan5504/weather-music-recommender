package com.example.weather_music_recommender.service;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WeatherMoodMapper {

    public MoodProfile mapToMood(String weatherMain) {
        String normalized = weatherMain == null ? "" : weatherMain.trim().toLowerCase();

        return switch (normalized) {
            case "clear" -> new MoodProfile("Sunny Vibes", List.of("pop", "dance", "edm"));
            case "clouds" -> new MoodProfile("Chill Cloudy", List.of("indie", "acoustic", "folk"));
            case "rain", "drizzle", "thunderstorm" -> new MoodProfile("Rainy Mood", List.of("jazz", "blues", "soul"));
            case "snow" -> new MoodProfile("Cozy Snow", List.of("classical", "acoustic", "ambient"));
            case "mist", "fog", "haze", "smoke" -> new MoodProfile("Dreamy Atmosphere", List.of("electronic", "ambient", "indie"));
            default -> new MoodProfile("Anytime Mix", List.of("pop", "rock", "indie"));
        };
    }
}
