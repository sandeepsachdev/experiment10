package com.sydneyevents.service.scraper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sydneyevents.model.City;
import com.sydneyevents.model.Event;
import com.sydneyevents.service.CuratedDataLoader;
import com.sydneyevents.service.EventScraper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Component
public class WhatsOnSydneyScraper implements EventScraper {
    private static final Logger log = LoggerFactory.getLogger(WhatsOnSydneyScraper.class);
    private static final String BASE = "https://whatson.cityofsydney.nsw.gov.au";
    private static final String URL = BASE + "/?past=false";
    private static final String AUTO_SECTION_ID = "d4dd3ec0-62fd-11ec-83fa-d576181408f2";

    @Value("${app.scrape.user-agent}")
    private String userAgent;

    @Value("${app.scrape.timeout-ms}")
    private int timeoutMs;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String sourceName() {
        return "City of Sydney - What's On";
    }

    @Override
    public boolean supports(City city) {
        return "sydney".equals(city.slug());
    }

    @Override
    public List<Event> scrape(City city) {
        try {
            log.info("WhatsOnSydney request: GET {}", URL);
            Document doc = Jsoup.connect(URL)
                    .userAgent(userAgent)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "en-AU,en;q=0.9")
                    .timeout(timeoutMs)
                    .get();

            Element script = doc.selectFirst("script#__NEXT_DATA__");
            if (script == null) {
                log.warn("WhatsOnSydney: __NEXT_DATA__ not found");
                return List.of();
            }

            JsonNode root = mapper.readTree(script.data());
            JsonNode additionalData = root.path("props").path("pageProps").path("sectionAdditionalData");

            LocalDate today = CuratedDataLoader.today(city);
            LocalDate end = today.plusDays(7);
            Map<String, Event> bySlug = new LinkedHashMap<>();

            // Automated (Algolia-style) section
            JsonNode autoSection = additionalData.path(AUTO_SECTION_ID);
            if (autoSection.isObject()) {
                for (JsonNode hit : autoSection.path("events").path("hits")) {
                    String slug = hit.path("slug").asText();
                    Event e = toEvent(hit, today, end);
                    if (e != null && !slug.isBlank()) bySlug.putIfAbsent(slug, e);
                }
            }

            // Curated sections (arrays)
            for (JsonNode section : additionalData) {
                if (!section.isArray()) continue;
                for (JsonNode item : section) {
                    String key = item.path("slug").asText(item.path("objectID").asText());
                    if (!key.isBlank()) bySlug.putIfAbsent(key, toEvent(item, today, end));
                }
            }

            List<Event> events = new ArrayList<>(bySlug.values());
            events.removeIf(e -> e == null || e.getTitle() == null || e.getTitle().isBlank());
            log.info("WhatsOnSydney response: {} events", events.size());
            return events;
        } catch (Exception e) {
            log.warn("WhatsOnSydneyScraper failed: {}", e.getMessage());
            return List.of();
        }
    }

    private Event toEvent(JsonNode item, LocalDate today, LocalDate end) {
        String name = item.path("name").asText("").trim();
        if (name.isBlank()) return null;

        String slug = item.path("slug").asText("");
        String imageUrl = extractImageUrl(item.path("tileImageCloudinary"));

        // "dates" lists sessions (unordered, capped at 100) and upcomingDate is the
        // next one; keep whichever fall inside our window.
        TreeSet<LocalDate> occurrences = new TreeSet<>();
        for (JsonNode d : item.path("dates")) {
            addIfInWindow(occurrences, parseDate(d.asText("")), today, end);
        }
        addIfInWindow(occurrences, parseDate(item.path("upcomingDate").asText("")), today, end);
        if (occurrences.isEmpty()) return null;

        String category = "";
        JsonNode cats = item.path("categories");
        if (cats.isArray() && !cats.isEmpty()) category = cats.get(0).asText("");

        String venue = item.path("venueName").asText("").trim();

        Event e = new Event();
        e.setTitle(name);
        e.setDescription(item.path("strapline").asText("").trim());
        e.setUrl(slug.isBlank() ? BASE : BASE + "/events/" + slug);
        e.setImageUrl(imageUrl);
        e.setVenue(venue.isBlank() ? null : venue);
        e.setOccurrences(occurrences);
        e.setStartDate(occurrences.first());
        e.setEndDate(occurrences.last());
        e.setCategory(category);
        e.setSource(sourceName());
        return e;
    }

    private static void addIfInWindow(Set<LocalDate> sink, LocalDate date, LocalDate from, LocalDate to) {
        if (date != null && !date.isBefore(from) && !date.isAfter(to)) sink.add(date);
    }

    private String extractImageUrl(JsonNode imageField) {
        try {
            JsonNode arr = imageField.isTextual()
                    ? mapper.readTree(imageField.asText())
                    : imageField;
            if (arr.isArray() && !arr.isEmpty()) {
                String url = arr.get(0).path("secure_url").asText("");
                if (url.isBlank()) url = arr.get(0).path("url").asText("");
                return url.isBlank() ? null : url;
            }
        } catch (Exception ignored) { }
        return null;
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDate.parse(s.substring(0, Math.min(10, s.length())));
        } catch (Exception ignored) {
            return null;
        }
    }
}
