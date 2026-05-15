package com.sydneyevents.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sydneyevents.model.WeatherDay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class WeatherService {
    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${app.weather.latitude}")
    private double latitude;

    @Value("${app.weather.longitude}")
    private double longitude;

    @Value("${app.weather.timezone}")
    private String timezone;

    public WeatherService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Cacheable("weather")
    public List<WeatherDay> getNext7DaysForecast() {
        String url = "https://api.open-meteo.com/v1/forecast"
                + "?latitude=" + latitude
                + "&longitude=" + longitude
                + "&daily=weather_code,temperature_2m_max,temperature_2m_min,"
                + "precipitation_sum,precipitation_probability_max,"
                + "wind_speed_10m_max,uv_index_max,sunrise,sunset"
                + "&timezone=" + timezone.replace("/", "%2F")
                + "&forecast_days=7";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Weather API returned {}: {}", response.statusCode(), response.body());
                return List.of();
            }

            return parseForecast(response.body());
        } catch (Exception e) {
            log.error("Failed to fetch weather forecast", e);
            return List.of();
        }
    }

    private List<WeatherDay> parseForecast(String json) throws Exception {
        JsonNode root = mapper.readTree(json);
        JsonNode daily = root.path("daily");

        JsonNode times = daily.path("time");
        JsonNode codes = daily.path("weather_code");
        JsonNode maxT = daily.path("temperature_2m_max");
        JsonNode minT = daily.path("temperature_2m_min");
        JsonNode precip = daily.path("precipitation_sum");
        JsonNode precipProb = daily.path("precipitation_probability_max");
        JsonNode wind = daily.path("wind_speed_10m_max");
        JsonNode uv = daily.path("uv_index_max");
        JsonNode sunrise = daily.path("sunrise");
        JsonNode sunset = daily.path("sunset");

        List<WeatherDay> days = new ArrayList<>();
        int n = times.size();
        for (int i = 0; i < n; i++) {
            WeatherDay d = new WeatherDay();
            d.setDate(LocalDate.parse(times.get(i).asText()));
            d.setWeatherCode(codes.get(i).asInt());
            d.setMaxTempC(maxT.get(i).asDouble());
            d.setMinTempC(minT.get(i).asDouble());
            d.setPrecipitationMm(precip.get(i).asDouble());
            d.setPrecipitationProbability(precipProb.path(i).asInt(0));
            d.setWindKph(wind.get(i).asDouble());
            d.setUvIndex(uv.path(i).asDouble(0));
            d.setSunrise(formatTime(sunrise.path(i).asText("")));
            d.setSunset(formatTime(sunset.path(i).asText("")));
            days.add(d);
        }
        return days;
    }

    private String formatTime(String iso) {
        // Open-Meteo returns local time like "2025-05-15T06:42"
        int t = iso.indexOf('T');
        return t > 0 && iso.length() >= t + 6 ? iso.substring(t + 1, t + 6) : iso;
    }
}
