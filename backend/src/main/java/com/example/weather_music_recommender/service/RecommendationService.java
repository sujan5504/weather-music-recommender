package com.example.weather_music_recommender.service;

import com.example.weather_music_recommender.dto.RecommendationResponse;
import com.example.weather_music_recommender.dto.TrackRecommendation;
import com.example.weather_music_recommender.dto.WeatherSnapshot;
import com.example.weather_music_recommender.model.RecommendationHistory;
import com.example.weather_music_recommender.model.UserProfile;
import com.example.weather_music_recommender.repository.RecommendationHistoryRepository;
import com.example.weather_music_recommender.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RecommendationService {

    private final WeatherClient weatherClient;
    private final WeatherMoodMapper moodMapper;
    private final SpotifyClient spotifyClient;
    private final UserProfileRepository userProfileRepository;
    private final RecommendationHistoryRepository recommendationHistoryRepository;

    public RecommendationService(
            WeatherClient weatherClient,
            WeatherMoodMapper moodMapper,
            SpotifyClient spotifyClient,
            UserProfileRepository userProfileRepository,
            RecommendationHistoryRepository recommendationHistoryRepository
    ) {
        this.weatherClient = weatherClient;
        this.moodMapper = moodMapper;
        this.spotifyClient = spotifyClient;
        this.userProfileRepository = userProfileRepository;
        this.recommendationHistoryRepository = recommendationHistoryRepository;
    }

    @Transactional
    public RecommendationResponse recommend(double latitude, double longitude, String accessToken) {
        WeatherSnapshot weather = weatherClient.getCurrentWeather(latitude, longitude);
        MoodProfile moodProfile = moodMapper.mapToMood(weather.condition());
        List<TrackRecommendation> tracks = spotifyClient.getRecommendations(accessToken, moodProfile.genres(), 12);

        SpotifyClient.SpotifyUser spotifyUser = spotifyClient.getCurrentUser(accessToken);
        UserProfile profile = userProfileRepository.findBySpotifyUserId(spotifyUser.spotifyUserId())
                .orElseGet(UserProfile::new);

        profile.setSpotifyUserId(spotifyUser.spotifyUserId());
        profile.setDisplayName(spotifyUser.displayName());
        profile.setEmail(spotifyUser.email());
        profile.setLastLatitude(latitude);
        profile.setLastLongitude(longitude);
        UserProfile savedProfile = userProfileRepository.save(profile);

        RecommendationHistory history = new RecommendationHistory();
        history.setUserProfile(savedProfile);
        history.setCity(weather.city());
        history.setCountry(weather.country());
        history.setWeatherCondition(weather.condition());
        history.setMood(moodProfile.mood());
        history.setGenresCsv(String.join(",", moodProfile.genres()));
        recommendationHistoryRepository.save(history);

        return new RecommendationResponse(weather, moodProfile.mood(), moodProfile.genres(), tracks);
    }
}
