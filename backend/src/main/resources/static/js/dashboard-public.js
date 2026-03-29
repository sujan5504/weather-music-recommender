async function fetchRecommendations() {
    const status = document.getElementById('status');
    const fetchBtn = document.getElementById('fetchBtn');

    status.textContent = 'Getting your location...';
    fetchBtn.disabled = true;

    try {
        // Get user's geolocation
        const position = await new Promise((resolve, reject) => {
            navigator.geolocation.getCurrentPosition(resolve, reject);
        });

        const latitude = position.coords.latitude;
        const longitude = position.coords.longitude;

        status.textContent = 'Fetching weather and recommendations...';

        // Call public recommendations API
        const response = await fetch('/api/recommendations/public', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                latitude: latitude,
                longitude: longitude
            })
        });

        if (!response.ok) {
            throw new Error(`API error: ${response.status}`);
        }

        const data = await response.json();

        // Display weather card
        displayWeatherCard(data);

        // Display mood
        displayMood(data.mood);

        // Display tracks
        displayTracks(data.tracks);

        status.textContent = 'Recommendations loaded!';

    } catch (error) {
        status.textContent = `Error: ${error.message}. Please allow location access and try again.`;
        console.error('Error:', error);
    } finally {
        fetchBtn.disabled = false;
    }
}

function displayWeatherCard(data) {
    const weatherCard = document.getElementById('weatherCard');
    const weather = data.weather;

    weatherCard.innerHTML = `
        <div class="weather-info">
            <h3>${weather.city}, ${weather.country}</h3>
            <p><strong>Condition:</strong> ${weather.condition}</p>
            <p><strong>Temperature:</strong> ${weather.temperature}°C</p>
            <p><strong>Humidity:</strong> ${weather.humidity}%</p>
        </div>
    `;
    weatherCard.classList.remove('hidden');
}

function displayMood(mood) {
    const moodLabel = document.getElementById('moodLabel');
    moodLabel.textContent = `🎵 Mood: ${mood}`;
    moodLabel.className = `mood mood-${mood.toLowerCase()}`;
}

function displayTracks(tracks) {
    const tracksContainer = document.getElementById('tracks');
    tracksContainer.innerHTML = '';

    if (!tracks || tracks.length === 0) {
        tracksContainer.innerHTML = '<p>No tracks found</p>';
        return;
    }

    tracks.forEach((track, index) => {
        const trackElement = document.createElement('div');
        trackElement.className = 'track-card';
        
        let playButtonHTML = '';
        if (track.previewUrl) {
            playButtonHTML = `<audio id="preview-${index}" src="${track.previewUrl}"></audio>
                             <button class="btn small" onclick="togglePreview(${index})">▶ Preview</button>`;
        }

        trackElement.innerHTML = `
            <div class="track-image">
                <img src="${track.image || '/css/placeholder.png'}" alt="${track.name}">
            </div>
            <div class="track-info">
                <h4>${track.name}</h4>
                <p class="artist">${track.artist}</p>
                <p class="album">${track.album}</p>
                <div class="track-actions">
                    ${playButtonHTML}
                    <a href="${track.spotifyUrl}" target="_blank" class="btn small spotify">
                        Open in Spotify
                    </a>
                </div>
            </div>
        `;
        
        tracksContainer.appendChild(trackElement);
    });
}

function togglePreview(index) {
    const audio = document.getElementById(`preview-${index}`);
    if (audio.paused) {
        // Pause all other audio
        document.querySelectorAll('audio').forEach(a => a.pause());
        audio.play();
    } else {
        audio.pause();
        audio.currentTime = 0;
    }
}

// Event listeners
document.addEventListener('DOMContentLoaded', () => {
    const fetchBtn = document.getElementById('fetchBtn');
    fetchBtn.addEventListener('click', fetchRecommendations);
});

