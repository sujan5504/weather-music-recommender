package com.example.weather_music_recommender.service;

import com.example.weather_music_recommender.dto.TrackRecommendation;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SpotifyClient {

    private final RestClient restClient;

    public SpotifyClient(RestClient.Builder restClientBuilder, @Value("${app.spotify.base-url}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    public SpotifyUser getCurrentUser(String accessToken) {
        SpotifyMeResponse response = restClient.get()
                .uri("/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                            "Spotify user profile request failed with status " + clientResponse.getStatusCode().value());
                })
                .body(SpotifyMeResponse.class);

        if (response == null || response.id() == null || response.id().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Spotify profile response is invalid");
        }
        return new SpotifyUser(response.id(), response.displayName(), response.email());
    }

    public List<TrackRecommendation> getRecommendations(String accessToken, List<String> seedGenres, int limit) {
        String genres = seedGenres.stream().filter(Objects::nonNull).map(String::trim).collect(Collectors.joining(","));
        SpotifyRecommendationResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/recommendations")
                        .queryParam("seed_genres", genres)
                        .queryParam("limit", limit)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                            "Spotify recommendations request failed with status " + clientResponse.getStatusCode().value());
                })
                .body(SpotifyRecommendationResponse.class);

        if (response == null || response.tracks() == null) {
            return List.of();
        }

        return response.tracks().stream().map(track -> {
            String artist = track.artists() != null && !track.artists().isEmpty() ? track.artists().get(0).name() : "Unknown Artist";
            String image = null;
            if (track.album() != null && track.album().images() != null && !track.album().images().isEmpty()) {
                image = track.album().images().get(0).url();
            }
            return new TrackRecommendation(
                    track.id(),
                    track.name(),
                    artist,
                    track.album() != null ? track.album().name() : "",
                    track.previewUrl(),
                    track.externalUrls() != null ? track.externalUrls().spotify() : null,
                    image
            );
        }).toList();
    }

    private record SpotifyRecommendationResponse(List<SpotifyTrack> tracks) {
    }

    private record SpotifyTrack(
            String id,
            String name,
            @JsonProperty("preview_url") String previewUrl,
            @JsonProperty("external_urls") ExternalUrls externalUrls,
            Album album,
            List<Artist> artists
    ) {
    }

    private record Artist(String name) {
    }

    private record ExternalUrls(String spotify) {
    }

    private record Album(String name, List<Image> images) {
    }

    private record Image(String url) {
    }

    private record SpotifyMeResponse(
            String id,
            @JsonProperty("display_name") String displayName,
            String email
    ) {
    }

    public record SpotifyUser(String spotifyUserId, String displayName, String email) {
    }
}
