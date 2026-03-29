const fetchBtn = document.getElementById("fetchBtn");
const statusEl = document.getElementById("status");
const weatherCardEl = document.getElementById("weatherCard");
const tracksEl = document.getElementById("tracks");
const moodLabelEl = document.getElementById("moodLabel");

function setStatus(message) {
    statusEl.textContent = message;
}

function renderWeather(weather) {
    weatherCardEl.classList.remove("hidden");
    weatherCardEl.innerHTML = `
        <h3>${weather.city || "Unknown city"}${weather.country ? ", " + weather.country : ""}</h3>
        <p><strong>${weather.condition}</strong> - ${weather.description}</p>
        <p>${weather.temperature.toFixed(1)} C (feels like ${weather.feelsLike.toFixed(1)} C)</p>
        <p>Humidity: ${weather.humidity}% | Wind: ${weather.windSpeed} m/s</p>
    `;
}

function renderTracks(tracks) {
    tracksEl.innerHTML = "";
    if (!tracks || tracks.length === 0) {
        tracksEl.innerHTML = "<p>No tracks found. Try again in a moment.</p>";
        return;
    }

    tracks.forEach((track) => {
        const card = document.createElement("article");
        card.className = "track-card";
        card.innerHTML = `
            ${track.imageUrl ? `<img src="${track.imageUrl}" alt="${track.albumName} cover">` : ""}
            <h3>${track.trackName}</h3>
            <p>${track.artistName}</p>
            <p class="album">${track.albumName}</p>
            ${track.externalUrl ? `<a href="${track.externalUrl}" target="_blank" rel="noopener">Open on Spotify</a>` : ""}
        `;
        tracksEl.appendChild(card);
    });
}

async function fetchRecommendations(latitude, longitude) {
    const response = await fetch("/api/recommendations", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({ latitude, longitude })
    });

    if (!response.ok) {
        const message = await response.text();
        throw new Error(message || "Failed to fetch recommendations");
    }

    return response.json();
}

fetchBtn.addEventListener("click", () => {
    if (!navigator.geolocation) {
        setStatus("Geolocation is not supported in this browser.");
        return;
    }

    setStatus("Getting your location...");
    navigator.geolocation.getCurrentPosition(async (position) => {
        try {
            setStatus("Fetching weather and Spotify recommendations...");
            const { latitude, longitude } = position.coords;
            const result = await fetchRecommendations(latitude, longitude);
            renderWeather(result.weather);
            renderTracks(result.tracks);
            moodLabelEl.textContent = `Mood: ${result.mood} | Genres: ${result.genres.join(", ")}`;
            setStatus("Done. Enjoy your weather-tuned tracks.");
        } catch (error) {
            setStatus(`Could not fetch recommendations: ${error.message}`);
        }
    }, (error) => {
        setStatus(`Location error: ${error.message}`);
    });
});
