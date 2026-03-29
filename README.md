# WeatherTune

WeatherTune is a Spring Boot web app that logs users in with Spotify, reads browser geolocation, fetches current weather from OpenWeatherMap, maps weather to a mood, and returns matching Spotify track recommendations.

## Stack

- Java 17
- Spring Boot 3.x (Web MVC, Security OAuth2 Client, Thymeleaf, JPA)
- H2 (default dev profile) / MySQL (optional profile)
- Maven

## Project layout

- `backend/src/main/java/com/example/weather_music_recommender/config` - security config
- `backend/src/main/java/com/example/weather_music_recommender/controller` - MVC + API endpoints
- `backend/src/main/java/com/example/weather_music_recommender/service` - weather/spotify clients and recommendation orchestration
- `backend/src/main/java/com/example/weather_music_recommender/model` - JPA entities
- `backend/src/main/java/com/example/weather_music_recommender/repository` - Spring Data repositories
- `backend/src/main/resources/templates` - Thymeleaf pages
- `backend/src/main/resources/static` - CSS/JS assets

## Required API credentials

Set these environment variables before running:

- `SPOTIFY_CLIENT_ID`
- `SPOTIFY_CLIENT_SECRET`
- `OPENWEATHER_API_KEY`

Spotify redirect URI must match:

- `http://localhost:8081/login/oauth2/code/spotify`

## Run locally (H2 profile)

```bash
cd /path/to/weather-music-recommender
export SPOTIFY_CLIENT_ID="your-client-id"
export SPOTIFY_CLIENT_SECRET="your-client-secret"
export OPENWEATHER_API_KEY="your-openweather-key"
mvn clean test
mvn spring-boot:run
```

Open:

- App: `http://localhost:8081`
- H2 console: `http://localhost:8081/h2-console`

## Run with MySQL profile

```bash
cd /path/to/weather-music-recommender
export SPOTIFY_CLIENT_ID="your-client-id"
export SPOTIFY_CLIENT_SECRET="your-client-secret"
export OPENWEATHER_API_KEY="your-openweather-key"
export MYSQL_URL="jdbc:mysql://localhost:3306/weathertune"
export MYSQL_USER="root"
export MYSQL_PASSWORD="root"
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

## API endpoint used by dashboard

- `POST /api/recommendations`
  - Request body:
	- `latitude` (double)
	- `longitude` (double)
  - Response includes weather snapshot, mapped mood, seed genres, and recommended tracks.
