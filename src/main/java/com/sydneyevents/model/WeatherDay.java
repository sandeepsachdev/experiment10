package com.sydneyevents.model;

import java.time.LocalDate;

public class WeatherDay {
    private LocalDate date;
    private double maxTempC;
    private double minTempC;
    private int weatherCode;
    private double precipitationMm;
    private int precipitationProbability;
    private double windKph;
    private double uvIndex;
    private String sunrise;
    private String sunset;

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public double getMaxTempC() { return maxTempC; }
    public void setMaxTempC(double maxTempC) { this.maxTempC = maxTempC; }

    public double getMinTempC() { return minTempC; }
    public void setMinTempC(double minTempC) { this.minTempC = minTempC; }

    public int getWeatherCode() { return weatherCode; }
    public void setWeatherCode(int weatherCode) { this.weatherCode = weatherCode; }

    public double getPrecipitationMm() { return precipitationMm; }
    public void setPrecipitationMm(double precipitationMm) { this.precipitationMm = precipitationMm; }

    public int getPrecipitationProbability() { return precipitationProbability; }
    public void setPrecipitationProbability(int precipitationProbability) {
        this.precipitationProbability = precipitationProbability;
    }

    public double getWindKph() { return windKph; }
    public void setWindKph(double windKph) { this.windKph = windKph; }

    public double getUvIndex() { return uvIndex; }
    public void setUvIndex(double uvIndex) { this.uvIndex = uvIndex; }

    public String getSunrise() { return sunrise; }
    public void setSunrise(String sunrise) { this.sunrise = sunrise; }

    public String getSunset() { return sunset; }
    public void setSunset(String sunset) { this.sunset = sunset; }

    public String getDescription() {
        return WeatherCodes.describe(weatherCode);
    }

    public String getIcon() {
        return WeatherCodes.icon(weatherCode);
    }

    public long getMaxTempRounded() { return Math.round(maxTempC); }
    public long getMinTempRounded() { return Math.round(minTempC); }
}
