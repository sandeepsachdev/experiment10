package com.sydneyevents.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class DayView {
    private final LocalDate date;
    private final WeatherDay weather;
    private final List<Event> events;

    public DayView(LocalDate date, WeatherDay weather, List<Event> events) {
        this.date = date;
        this.weather = weather;
        this.events = events;
    }

    public LocalDate getDate() { return date; }
    public WeatherDay getWeather() { return weather; }
    public List<Event> getEvents() { return events; }

    public String getDayName() {
        return date.format(DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH));
    }

    public String getDateLabel() {
        return date.format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH));
    }

    public boolean isToday() {
        return date.equals(LocalDate.now(java.time.ZoneId.of("Australia/Sydney")));
    }
}
