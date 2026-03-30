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

    private static final String ANONYMOUS_USER_ID = "anonymous";

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
    public RecommendationResponse recommend(double latitude, double longitude) {
        WeatherSnapshot weather = weatherClient.getCurrentWeather(latitude, longitude);
        MoodProfile moodProfile = moodMapper.mapToMood(weather.condition());
        String accessToken = spotifyClient.getClientCredentialsToken();
        List<TrackRecommendation> tracks = spotifyClient.getRecommendations(accessToken, moodProfile.genres(), 12);

        UserProfile profile = userProfileRepository.findBySpotifyUserId(ANONYMOUS_USER_ID)
                .orElseGet(UserProfile::new);

        profile.setSpotifyUserId(ANONYMOUS_USER_ID);
        profile.setDisplayName("Guest");
        profile.setEmail(null);
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
