# WeatherTune Documentation (ggod)

This folder contains architecture-focused documentation for the WeatherTune project.

## Architecture Image

![WeatherTune Architecture](architecture.svg)

## System Overview

WeatherTune is a Spring Boot web app that provides weather-based music recommendations without requiring end-user login.

- Browser opens `GET /dashboard`
- Frontend (`dashboard.js`) requests browser geolocation
- Frontend calls `POST /api/recommendations`
- Backend fetches current weather from OpenWeatherMap
- Backend maps weather to mood/genres
- Backend fetches tracks from Spotify APIs using app credentials
- Backend returns weather snapshot, mood, genres, and tracks

## Main Components

### Web Layer

- `backend/src/main/java/com/example/weather_music_recommender/controller/ViewController.java`
- `backend/src/main/java/com/example/weather_music_recommender/controller/RecommendationController.java`

Responsibilities:
- Serve dashboard view
- Accept anonymous recommendation requests

### Service Layer

- `backend/src/main/java/com/example/weather_music_recommender/service/RecommendationService.java`
- `backend/src/main/java/com/example/weather_music_recommender/service/WeatherClient.java`
- `backend/src/main/java/com/example/weather_music_recommender/service/SpotifyClient.java`
- `backend/src/main/java/com/example/weather_music_recommender/service/WeatherMoodMapper.java`

Responsibilities:
- Orchestrate recommendation flow
- Call external APIs (OpenWeatherMap + Spotify)
- Map weather condition to music mood and genres

### Persistence Layer

- `backend/src/main/java/com/example/weather_music_recommender/model/UserProfile.java`
- `backend/src/main/java/com/example/weather_music_recommender/model/RecommendationHistory.java`
- `backend/src/main/java/com/example/weather_music_recommender/repository/UserProfileRepository.java`
- `backend/src/main/java/com/example/weather_music_recommender/repository/RecommendationHistoryRepository.java`

Responsibilities:
- Store user metadata (anonymous profile mode)
- Store recommendation history

### Frontend

- `backend/src/main/resources/templates/dashboard.html`
- `backend/src/main/resources/static/js/dashboard.js`
- `backend/src/main/resources/static/css/styles.css`

Responsibilities:
- Trigger location-based recommendation fetch
- Render weather card and track cards

## Runtime Profiles

- H2 (default): `backend/src/main/resources/application-h2.properties`
- MySQL (optional): `backend/src/main/resources/application-mysql.properties`

## Environment Variables

Set these before running:

```bash
export SPOTIFY_CLIENT_ID="your-client-id"
export SPOTIFY_CLIENT_SECRET="your-client-secret"
export OPENWEATHER_API_KEY="your-openweather-api-key"
```

## Quick Run

```bash
cd /Users/dhirajnath/Downloads/weather-music-recommender
mvn spring-boot:run
```

Then open:

- `http://localhost:8081/dashboard`

## Notes

- Security config allows anonymous dashboard and recommendation API access.
- Spotify calls use client-credentials flow at backend level.
- If Spotify recommendations endpoint is unavailable for a token/app, the backend can use search fallback.

