package com.sydneyevents.service.scraper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sydneyevents.model.City;
import com.sydneyevents.model.Event;
import com.sydneyevents.service.EventScraper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Scrapes Eventbrite's public city listings page.
 * Eventbrite ships an ItemList of schema.org/Event JSON-LD on each results page.
 */
@Component
public class EventbriteScraper implements EventScraper {
    private static final Logger log = LoggerFactory.getLogger(EventbriteScraper.class);

    private static final int PAGES = 3;

    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${app.scrape.user-agent}")
    private String userAgent;

    @Value("${app.scrape.timeout-ms}")
    private int timeoutMs;

    @Override
    public String sourceName() {
        return "Eventbrite";
    }

    @Override
    public List<Event> scrape(City city) {
        List<Event> events = new ArrayList<>();
        for (int page = 1; page <= PAGES; page++) {
            String url = "https://www.eventbrite.com.au/d/australia--"
                    + city.slug() + "/events--this-week/?page=" + page;
            int before = events.size();
            try {
                log.info("Eventbrite request: GET {}", url);
                Document doc = Jsoup.connect(url)
                        .userAgent(userAgent)
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .header("Accept-Language", "en-AU,en;q=0.9")
                        .timeout(timeoutMs)
                        .get();

                for (Element script : doc.select("script[type=application/ld+json]")) {
                    collect(mapper.readTree(script.data()), events);
                }
                log.info("Eventbrite response: {} events from page {} for {}",
                        events.size() - before, page, city.slug());
            } catch (Exception e) {
                log.warn("EventbriteScraper failed for {} page {}: {}", city.slug(), page, e.getMessage());
                break;
            }
            if (events.size() == before) break;
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

        String type = node.path("@type").asText("");
        if (type.toLowerCase().contains("event")) {
            Event e = toEvent(node);
            if (e != null) sink.add(e);
            return;
        }
        // Events are usually nested, e.g. ItemList > itemListElement > item
        node.forEach(child -> collect(child, sink));
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

        e.setImageFallbackUrl(unwrapResizedImage(e.getImageUrl()));

        e.setStartDate(parseDate(textOrNull(n, "startDate")));
        e.setEndDate(parseDate(textOrNull(n, "endDate")));

        JsonNode loc = n.path("location");
        if (loc.isObject()) e.setVenue(loc.path("name").asText(null));

        e.setSource(sourceName());
        return e;
    }

    /**
     * img.evbuc.com/&lt;url-encoded original&gt;?w=512... is Eventbrite's resizing CDN;
     * return the original it wraps so the page can fall back to it.
     */
    private String unwrapResizedImage(String url) {
        String prefix = "https://img.evbuc.com/";
        if (url == null || !url.startsWith(prefix)) return null;
        String inner = url.substring(prefix.length());
        int q = inner.indexOf('?');
        if (q >= 0) inner = inner.substring(0, q);
        String decoded = URLDecoder.decode(inner, StandardCharsets.UTF_8);
        return decoded.startsWith("https://") ? decoded : null;
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
