package com.sydneyevents.service.scraper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sydneyevents.model.Event;
import com.sydneyevents.service.EventScraper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Scrapes the public Eventbrite Sydney listings page.
 * Eventbrite ships full schema.org/Event JSON-LD on every listing card,
 * so we parse that rather than fragile HTML.
 */
@Component
public class EventbriteScraper implements EventScraper {
    private static final Logger log = LoggerFactory.getLogger(EventbriteScraper.class);

    private static final String URL = "https://www.eventbrite.com.au/d/australia--sydney/events--this-week/";

    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${app.scrape.user-agent}")
    private String userAgent;

    @Value("${app.scrape.timeout-ms}")
    private int timeoutMs;

    @Override
    public String sourceName() {
        return "Eventbrite Sydney";
    }

    @Override
    public List<Event> scrape() {
        List<Event> events = new ArrayList<>();
        try {
            Document doc = Jsoup.connect(URL)
                    .userAgent(userAgent)
                    .header("Accept-Language", "en-AU,en;q=0.9")
                    .timeout(timeoutMs)
                    .get();

            for (Element script : doc.select("script[type=application/ld+json]")) {
                collect(mapper.readTree(script.data()), events);
            }
            log.info("EventbriteScraper found {} events", events.size());
        } catch (Exception e) {
            log.warn("EventbriteScraper failed: {}", e.getMessage());
        }
        return events;
    }

    private void collect(JsonNode node, List<Event> sink) {
        if (node == null) return;
        if (node.isArray()) {
            node.forEach(n -> collect(n, sink));
            return;
        }
        if (!node.isObject()) return;
        if (node.has("@graph")) collect(node.get("@graph"), sink);

        String type = node.path("@type").asText("");
        if (type.toLowerCase().contains("event")) {
            Event e = toEvent(node);
            if (e != null) sink.add(e);
        }
    }

    private Event toEvent(JsonNode n) {
        String title = textOrNull(n, "name");
        if (title == null) return null;

        Event e = new Event();
        e.setTitle(title);
        e.setDescription(textOrNull(n, "description"));
        e.setUrl(textOrNull(n, "url"));

        JsonNode img = n.path("image");
        if (img.isTextual()) e.setImageUrl(img.asText());
        else if (img.isArray() && img.size() > 0) {
            JsonNode f = img.get(0);
            e.setImageUrl(f.isTextual() ? f.asText() : f.path("url").asText(null));
        }

        e.setStartDate(parseDate(textOrNull(n, "startDate")));
        e.setEndDate(parseDate(textOrNull(n, "endDate")));

        JsonNode loc = n.path("location");
        if (loc.isObject()) e.setVenue(loc.path("name").asText(null));

        e.setCategory("Eventbrite");
        e.setSource(sourceName());
        return e;
    }

    private String textOrNull(JsonNode n, String f) {
        JsonNode v = n.path(f);
        return v.isTextual() && !v.asText().isBlank() ? v.asText() : null;
    }

    private LocalDate parseDate(String s) {
        if (s == null) return null;
        try { return LocalDate.parse(s.substring(0, Math.min(10, s.length()))); }
        catch (Exception e1) {
            try { return OffsetDateTime.parse(s).toLocalDate(); }
            catch (Exception e2) { return null; }
        }
    }
}
