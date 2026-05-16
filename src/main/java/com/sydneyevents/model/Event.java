package com.sydneyevents.model;

import java.time.LocalDate;
import java.util.Objects;

public class Event {
    private String title;
    private String description;
    private String imageUrl;
    private String url;
    private LocalDate startDate;
    private LocalDate endDate;
    private String venue;
    private String category;
    private String source;
    private boolean curated;

    public Event() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public boolean isCurated() { return curated; }
    public void setCurated(boolean curated) { this.curated = curated; }

    public boolean occursOn(LocalDate date) {
        if (startDate == null) return false;
        LocalDate end = endDate != null ? endDate : startDate;
        return !date.isBefore(startDate) && !date.isAfter(end);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Event event)) return false;
        return Objects.equals(title, event.title) && Objects.equals(url, event.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, url);
    }
}
