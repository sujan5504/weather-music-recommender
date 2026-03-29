package com.example.weather_music_recommender.repository;

import com.example.weather_music_recommender.model.RecommendationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationHistoryRepository extends JpaRepository<RecommendationHistory, Long> {
}
