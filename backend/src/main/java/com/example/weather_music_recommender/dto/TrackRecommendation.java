package com.example.weather_music_recommender.dto;

public record TrackRecommendation(
        String spotifyTrackId,
        String trackName,
        String artistName,
        String albumName,
        String previewUrl,
        String externalUrl,
        String imageUrl
) {
}
