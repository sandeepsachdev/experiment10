package com.sydneyevents.service;

import com.sydneyevents.model.City;
import com.sydneyevents.model.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EventService {
    private static final Logger log = LoggerFactory.getLogger(EventService.class);

    private final List<EventScraper> scrapers;

    public EventService(List<EventScraper> scrapers) {
        this.scrapers = scrapers;
    }

    private final Map<String, ZonedDateTime> lastRefreshed = new ConcurrentHashMap<>();

    // Keyed by date as well as city so the 7-day window rolls over at midnight.
    @Cacheable(value = "events",
            key = "#city.slug() + ':' + T(com.sydneyevents.service.CuratedDataLoader).today(#city)")
    public List<Event> getUpcomingEvents(City city) {
        LocalDate today = CuratedDataLoader.today(city);
        LocalDate end = today.plusDays(7);

        // Dedupe by lower-case title; when both a live and a curated event share
        // a title, keep the live one (curated is the fallback).
        java.util.LinkedHashMap<String, Event> byTitle = new java.util.LinkedHashMap<>();

        for (EventScraper scraper : scrapers) {
            if (!scraper.supports(city)) continue;
            try {
                List<Event> found = scraper.scrape(city);
                for (Event e : found) {
                    if (e.getTitle() == null) continue;
                    if (e.getUrl() != null && !e.getUrl().matches("(?i)^https?://.+")) e.setUrl(null);
                    String key = e.getTitle().trim().toLowerCase();
                    Event existing = byTitle.get(key);
                    if (existing == null) {
                        byTitle.put(key, e);
                    } else if (existing.isCurated() && !e.isCurated()) {
                        // Replace a curated placeholder with the live event.
                        byTitle.put(key, e);
                    }
                }
            } catch (Exception ex) {
                log.warn("Scraper {} failed for {}: {}",
                        scraper.sourceName(), city.slug(), ex.getMessage());
            }
        }

        List<Event> all = new ArrayList<>(byTitle.values());

        List<Event> filtered = new ArrayList<>();
        for (Event e : all) {
            if (e.getStartDate() == null) {
                e.setStartDate(today);
                e.setEndDate(end);
                filtered.add(e);
                continue;
            }
            LocalDate s = e.getStartDate();
            LocalDate f = e.getEndDate() != null ? e.getEndDate() : s;
            if (!f.isBefore(today) && !s.isAfter(end)) {
                filtered.add(e);
            }
        }

        filtered.sort(Comparator
                .comparing(Event::getStartDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Event::getTitle, Comparator.nullsLast(Comparator.naturalOrder())));

        lastRefreshed.put(city.slug(), ZonedDateTime.now(ZoneId.of(city.timezone())));
        log.info("Aggregated {} unique events for {} (next 7 days) from {} sources",
                filtered.size(), city.name(), scrapers.size());
        return filtered;
    }

    /** When events for this city were last fetched from the sources (null if never). */
    public ZonedDateTime lastRefreshed(City city) {
        return lastRefreshed.get(city.slug());
    }
}
