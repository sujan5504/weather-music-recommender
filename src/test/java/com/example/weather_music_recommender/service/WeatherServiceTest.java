package com.example.weather_music_recommender.service;

import com.example.weather_music_recommender.model.WeatherData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("WeatherService Tests")
public class WeatherServiceTest {

    @Autowired
    private WeatherService weatherService;

    @Test
    @DisplayName("Test getWeatherData returns valid WeatherData object")
    void testGetWeatherData() {
        WeatherData weather = weatherService.getWeatherData("London");
        
        assertNotNull(weather);
        assertNotNull(weather.getCondition());
        assertNotNull(weather.getCity());
        assertTrue(weather.getTemperature() >= 5 && weather.getTemperature() <= 35);
        assertTrue(weather.getHumidity() >= 30 && weather.getHumidity() <= 100);
    }

    @Test
    @DisplayName("Test getWeatherData with different cities")
    void testGetWeatherDataMultipleCities() {
        String[] cities = {"New York", "Tokyo", "Paris"};
        
        for (String city : cities) {
            WeatherData weather = weatherService.getWeatherData(city);
            assertEquals(city, weather.getCity());
            assertNotNull(weather.getCondition());
        }
    }

    @Test
    @DisplayName("Test getRandomWeatherData returns random city")
    void testGetRandomWeatherData() {
        WeatherData weather = weatherService.getRandomWeatherData();
        
        assertNotNull(weather);
        assertNotNull(weather.getCity());
        assertNotNull(weather.getCondition());
    }
}