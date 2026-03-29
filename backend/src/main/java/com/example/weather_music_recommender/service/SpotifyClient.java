package com.example.weather_music_recommender.service;

import com.example.weather_music_recommender.dto.TrackRecommendation;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SpotifyClient {

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;
    private String cachedToken;
    private long tokenExpiryTime;

    public SpotifyClient(RestClient.Builder restClientBuilder, 
                        @Value("${app.spotify.base-url}") String baseUrl,
                        @Value("${spring.security.oauth2.client.registration.spotify.client-id}") String clientId,
                        @Value("${spring.security.oauth2.client.registration.spotify.client-secret}") String clientSecret) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public String getClientCredentialsToken() {
        // Return cached token if still valid
        if (cachedToken != null && System.currentTimeMillis() < tokenExpiryTime) {
            return cachedToken;
        }

        try {
            String auth = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
            
            SpotifyTokenResponse response = RestClient.builder()
                    .baseUrl("https://accounts.spotify.com")
                    .build()
                    .post()
                    .uri("/api/token")
                    .header("Authorization", "Basic " + auth)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body("grant_type=client_credentials")
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                                "Spotify token request failed");
                    })
                    .body(SpotifyTokenResponse.class);

            if (response != null && response.accessToken() != null) {
                cachedToken = response.accessToken();
                tokenExpiryTime = System.currentTimeMillis() + (response.expiresIn() * 900); // Cache for 90% of expiry time
                return cachedToken;
            }
        } catch (Exception e) {
            // Fall back gracefully
            return null;
        }
        return null;
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
        if (accessToken == null || accessToken.isBlank()) {
            return List.of();
        }

        // Filter and validate genres
        List<String> validGenres = seedGenres.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(g -> !g.isBlank())
                .toList();

        // Fallback to popular genre if no valid genres
        if (validGenres.isEmpty()) {
            validGenres = List.of("pop");
        }

        // Limit to first 5 genres (Spotify max for seed_genres)
        validGenres = validGenres.stream().limit(5).toList();

        String genres = String.join(",", validGenres);

        try {
            SpotifyRecommendationResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/recommendations")
                            .queryParam("seed_genres", genres)
                            .queryParam("limit", Math.min(limit, 50))
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                                "Spotify recommendations request failed with status " + clientResponse.getStatusCode().value());
                    })
                    .body(SpotifyRecommendationResponse.class);

            if (response == null || response.tracks() == null) {
                return searchByGenres(accessToken, validGenres, limit);
            }

            List<TrackRecommendation> tracks = response.tracks().stream().map(track -> {
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

            if (!tracks.isEmpty()) {
                return tracks;
            }

            return searchByGenres(accessToken, validGenres, limit);
        } catch (Exception e) {
            // Fallback keeps API stable when Spotify /recommendations is unavailable for a token/app.
            System.err.println("Recommendations API error: " + e.getMessage());
            return searchByGenres(accessToken, validGenres, limit);
        }
    }

    private List<TrackRecommendation> searchByGenres(String accessToken, List<String> genres, int limit) {
        try {
            String query = String.join(" OR genre:", genres);
            SpotifySearchResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("q", "genre:" + query)
                            .queryParam("type", "track")
                            .queryParam("limit", Math.min(limit, 50))
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                        // Silent fail - throw exception which will be caught
                        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Search failed");
                    })
                    .body(SpotifySearchResponse.class);

            if (response == null || response.tracks() == null || response.tracks().items() == null) {
                return searchByKeywords(accessToken, genres, limit);
            }

            List<TrackRecommendation> tracks = response.tracks().items().stream().map(track -> {
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

            if (!tracks.isEmpty()) {
                return tracks;
            }

            return searchByKeywords(accessToken, genres, limit);
        } catch (Exception e) {
            System.err.println("Search fallback also failed: " + e.getMessage());
            return searchByKeywords(accessToken, genres, limit);
        }
    }

    private List<TrackRecommendation> searchByKeywords(String accessToken, List<String> genres, int limit) {
        if (accessToken == null || accessToken.isBlank()) {
            return List.of();
        }

        try {
            String query = String.join(" ", genres);
            if (query.isBlank()) {
                query = "chill acoustic";
            }
            final String finalQuery = query;

            SpotifySearchResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("q", finalQuery)
                            .queryParam("type", "track")
                            .queryParam("limit", Math.min(limit, 50))
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Keyword search failed");
                    })
                    .body(SpotifySearchResponse.class);

            if (response == null || response.tracks() == null || response.tracks().items() == null) {
                return List.of();
            }

            return response.tracks().items().stream().map(track -> {
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
        } catch (Exception e) {
            System.err.println("Keyword fallback failed: " + e.getMessage());
            return List.of();
        }
    }

    private record SpotifyRecommendationResponse(List<SpotifyTrack> tracks) {
    }

    private record SpotifySearchResponse(SpotifyTrackPage tracks) {
    }

    private record SpotifyTrackPage(List<SpotifyTrack> items) {
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

    private record SpotifyTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") int expiresIn
    ) {
    }

    public record SpotifyUser(String spotifyUserId, String displayName, String email) {
    }
}

