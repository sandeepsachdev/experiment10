package com.sydneyevents.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sydneyevents.model.City;
import com.sydneyevents.model.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads per-city curated event content from src/main/resources/cities/&lt;slug&gt;.json
 * and expands it into dated Event instances covering the next 7 days.
 *
 * Each JSON file has:
 *   - anchors:    iconic always-open attractions (shown every day)
 *   - recurring:  map of weekday name to events that only show on that day
 */
@Component
public class CuratedDataLoader {
    private static final Logger log = LoggerFactory.getLogger(CuratedDataLoader.class);

    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, JsonNode> cache = new ConcurrentHashMap<>();

    public List<Event> eventsFor(City city, LocalDate from, LocalDate to) {
        JsonNode root = load(city.slug());
        if (root == null) return List.of();

        List<Event> out = new ArrayList<>();

        for (JsonNode a : root.path("anchors")) {
            Event e = toEvent(a, city);
            if (e == null) continue;
            e.setStartDate(from);
            e.setEndDate(to);
            out.add(e);
        }

        JsonNode recurring = root.path("recurring");
        if (recurring.isObject()) {
            Map<DayOfWeek, List<JsonNode>> byDow = new HashMap<>();
            recurring.fields().forEachRemaining(entry -> {
                try {
                    DayOfWeek dow = DayOfWeek.valueOf(entry.getKey().toUpperCase(Locale.ROOT));
                    List<JsonNode> list = new ArrayList<>();
                    entry.getValue().forEach(list::add);
                    byDow.put(dow, list);
                } catch (IllegalArgumentException ex) {
                    log.warn("Unknown day of week '{}' in {}.json", entry.getKey(), city.slug());
                }
            });

            LocalDate d = from;
            while (!d.isAfter(to)) {
                List<JsonNode> dayEvents = byDow.get(d.getDayOfWeek());
                if (dayEvents != null) {
                    for (JsonNode n : dayEvents) {
                        Event e = toEvent(n, city);
                        if (e == null) continue;
                        e.setStartDate(d);
                        e.setEndDate(d);
                        out.add(e);
                    }
                }
                d = d.plusDays(1);
            }
        }
        return out;
    }

    private JsonNode load(String slug) {
        return cache.computeIfAbsent(slug, this::readFromClasspath);
    }

    private JsonNode readFromClasspath(String slug) {
        ClassPathResource res = new ClassPathResource("cities/" + slug + ".json");
        if (!res.exists()) {
            log.warn("No curated data file for city '{}'", slug);
            return null;
        }
        try (var is = res.getInputStream()) {
            return mapper.readTree(is);
        } catch (Exception ex) {
            log.error("Failed to read curated data for '{}'", slug, ex);
            return null;
        }
    }

    private Event toEvent(JsonNode n, City city) {
        String title = text(n, "title");
        if (title == null) return null;
        Event e = new Event();
        e.setTitle(title);
        e.setDescription(text(n, "description"));
        e.setImageUrl(text(n, "image"));
        e.setUrl(text(n, "url"));
        e.setVenue(text(n, "venue"));
        e.setCategory(text(n, "category"));
        e.setSource("Curated " + city.name() + " Highlights");
        return e;
    }

    private String text(JsonNode n, String f) {
        JsonNode v = n.path(f);
        return v.isTextual() && !v.asText().isBlank() ? v.asText() : null;
    }

    public static LocalDate today(City city) {
        return LocalDate.now(ZoneId.of(city.timezone()));
    }
}
