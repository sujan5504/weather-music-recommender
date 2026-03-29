package com.example.weather_music_recommender.repository;

import com.example.weather_music_recommender.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findBySpotifyUserId(String spotifyUserId);
}
