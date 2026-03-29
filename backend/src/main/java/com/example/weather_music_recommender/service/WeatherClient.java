package com.example.weather_music_recommender.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.example.weather_music_recommender.dto.WeatherSnapshot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class WeatherClient {

    private final RestClient restClient;
    private final String weatherApiKey;
    private final String units;

    public WeatherClient(
            RestClient.Builder restClientBuilder,
            @Value("${app.weather.base-url}") String baseUrl,
            @Value("${app.weather.api-key:}") String weatherApiKey,
            @Value("${app.weather.units:metric}") String units
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.weatherApiKey = weatherApiKey;
        this.units = units;
    }

    public WeatherSnapshot getCurrentWeather(double lat, double lon) {
        if (weatherApiKey == null || weatherApiKey.isBlank()) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Missing OPENWEATHER_API_KEY configuration");
        }

        OpenWeatherResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/weather")
                        .queryParam("lat", lat)
                        .queryParam("lon", lon)
                        .queryParam("appid", weatherApiKey)
                        .queryParam("units", units)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                    throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_GATEWAY,
                            "OpenWeather request failed with status " + clientResponse.getStatusCode().value());
                })
                .body(OpenWeatherResponse.class);

        if (response == null || response.weather() == null || response.weather().isEmpty() || response.main() == null) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_GATEWAY,
                    "OpenWeather returned an invalid payload");
        }

        WeatherCondition condition = response.weather().get(0);
        return new WeatherSnapshot(
                response.name(),
                response.sys() != null ? response.sys().country() : "",
                condition.main(),
                condition.description(),
                response.main().temp(),
                response.main().feelsLike(),
                response.main().humidity(),
                response.wind() != null ? response.wind().speed() : 0.0
        );
    }

    private record OpenWeatherResponse(
            String name,
            Main main,
            List<WeatherCondition> weather,
            Wind wind,
            Sys sys
    ) {
    }

    private record Main(
            double temp,
            @JsonProperty("feels_like") double feelsLike,
            int humidity
    ) {
    }

    private record WeatherCondition(String main, String description) {
    }

    private record Wind(double speed) {
    }

    private record Sys(String country) {
    }
}
