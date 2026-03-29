package com.example.weather_music_recommender.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class WeatherMoodMapperTest {

    private final WeatherMoodMapper mapper = new WeatherMoodMapper();

    @Test
    void mapsClearToSunnyVibes() {
        MoodProfile profile = mapper.mapToMood("Clear");
        assertEquals("Sunny Vibes", profile.mood());
        assertFalse(profile.genres().isEmpty());
    }

    @Test
    void mapsUnknownToFallback() {
        MoodProfile profile = mapper.mapToMood("VolcanicAsh");
        assertEquals("Anytime Mix", profile.mood());
    }
}
