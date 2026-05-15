package com.sydneyevents.model;

/**
 * WMO weather interpretation codes used by Open-Meteo.
 * https://open-meteo.com/en/docs
 */
public final class WeatherCodes {
    private WeatherCodes() {}

    public static String describe(int code) {
        return switch (code) {
            case 0 -> "Clear sky";
            case 1 -> "Mainly clear";
            case 2 -> "Partly cloudy";
            case 3 -> "Overcast";
            case 45, 48 -> "Fog";
            case 51 -> "Light drizzle";
            case 53 -> "Drizzle";
            case 55 -> "Heavy drizzle";
            case 56, 57 -> "Freezing drizzle";
            case 61 -> "Light rain";
            case 63 -> "Rain";
            case 65 -> "Heavy rain";
            case 66, 67 -> "Freezing rain";
            case 71 -> "Light snow";
            case 73 -> "Snow";
            case 75 -> "Heavy snow";
            case 77 -> "Snow grains";
            case 80 -> "Rain showers";
            case 81 -> "Heavy showers";
            case 82 -> "Violent showers";
            case 85, 86 -> "Snow showers";
            case 95 -> "Thunderstorm";
            case 96, 99 -> "Thunderstorm with hail";
            default -> "Unknown";
        };
    }

    public static String icon(int code) {
        return switch (code) {
            case 0 -> "☀️";
            case 1 -> "🌤️";
            case 2 -> "⛅";
            case 3 -> "☁️";
            case 45, 48 -> "🌫️";
            case 51, 53, 55, 56, 57 -> "🌦️";
            case 61, 63, 65, 66, 67 -> "🌧️";
            case 71, 73, 75, 77, 85, 86 -> "🌨️";
            case 80, 81, 82 -> "🌧️";
            case 95, 96, 99 -> "⛈️";
            default -> "🌡️";
        };
    }
}
