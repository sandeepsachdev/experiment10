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

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Generic scraper that reads JSON-LD schema.org/Event blocks from
 * city-specific tourism / venue pages.
 */
@Component
public class JsonLdEventScraper implements EventScraper {
    private static final Logger log = LoggerFactory.getLogger(JsonLdEventScraper.class);

    private static final Map<String, List<String>> CITY_URLS = Map.of(
            "sydney", List.of(
                    "https://www.sydney.com/events",
                    "https://www.sydneyoperahouse.com/whats-on.html"
            ),
            "melbourne", List.of(
                    "https://www.artscentremelbourne.com.au/whats-on",
                    "https://www.melbournemuseum.com.au/whats-on/"
            )
    );

    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${app.scrape.user-agent}")
    private String userAgent;

    @Value("${app.scrape.timeout-ms}")
    private int timeoutMs;

    @Override
    public String sourceName() {
        return "Tourism JSON-LD";
    }

    @Override
    public boolean supports(City city) {
        return CITY_URLS.containsKey(city.slug());
    }

    @Override
    public List<Event> scrape(City city) {
        List<String> urls = CITY_URLS.getOrDefault(city.slug(), List.of());
        List<Event> all = new ArrayList<>();
        for (String url : urls) {
            try {
                log.info("JsonLd request: GET {}", url);
                Document doc = Jsoup.connect(url)
                        .userAgent(userAgent)
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .header("Accept-Language", "en-AU,en;q=0.9")
                        .timeout(timeoutMs)
                        .ignoreContentType(true)
                        .get();
                for (Element script : doc.select("script[type=application/ld+json]")) {
                    parseJsonLd(script.data(), url, all);
                }
                log.info("JsonLd response: parsed {} (total {} events so far)", url, all.size());
            } catch (Exception e) {
                log.warn("JsonLd scrape failed for {}: {}", url, e.getMessage());
            }
        }
        return all;
    }

    private void parseJsonLd(String json, String pageUrl, List<Event> sink) {
        try {
            JsonNode root = mapper.readTree(json);
            collect(root, pageUrl, sink);
        } catch (Exception ignored) { }
    }

    private void collect(JsonNode node, String pageUrl, List<Event> sink) {
        if (node == null) return;
        if (node.isArray()) {
            node.forEach(n -> collect(n, pageUrl, sink));
            return;
        }
        if (!node.isObject()) return;

        if (node.has("@graph")) {
            collect(node.get("@graph"), pageUrl, sink);
        }

        String type = node.path("@type").asText("");
        if (type.equalsIgnoreCase("Event") || type.toLowerCase().contains("event")) {
            Event e = toEvent(node, pageUrl);
            if (e != null) sink.add(e);
        }
    }

    private Event toEvent(JsonNode n, String pageUrl) {
        String title = textOrNull(n, "name");
        if (title == null) return null;

        Event e = new Event();
        e.setTitle(title);
        e.setDescription(textOrNull(n, "description"));
        e.setUrl(firstNonBlank(textOrNull(n, "url"), pageUrl));

        JsonNode img = n.path("image");
        if (img.isTextual()) {
            e.setImageUrl(img.asText());
        } else if (img.isArray() && img.size() > 0) {
            JsonNode first = img.get(0);
            e.setImageUrl(first.isTextual() ? first.asText() : first.path("url").asText(null));
        } else if (img.isObject()) {
            e.setImageUrl(img.path("url").asText(null));
        }

        e.setStartDate(parseDate(textOrNull(n, "startDate")));
        e.setEndDate(parseDate(textOrNull(n, "endDate")));

        JsonNode location = n.path("location");
        if (location.isObject()) {
            e.setVenue(location.path("name").asText(null));
        } else if (location.isTextual()) {
            e.setVenue(location.asText());
        }

        String source = pageUrl.contains("operahouse") ? "Sydney Opera House"
                : pageUrl.contains("artscentremelbourne") ? "Arts Centre Melbourne"
                : pageUrl.contains("melbournemuseum") ? "Melbourne Museum"
                : "Destination NSW";
        e.setSource(source);
        e.setCategory("Featured");
        return e;
    }

    private String textOrNull(JsonNode n, String field) {
        JsonNode v = n.path(field);
        return v.isMissingNode() || v.isNull() || !v.isTextual() ? null : v.asText();
    }

    private String firstNonBlank(String a, String b) {
        return a != null && !a.isBlank() ? a : b;
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDate.parse(s.substring(0, Math.min(10, s.length())));
        } catch (Exception e) {
            try {
                return OffsetDateTime.parse(s).toLocalDate();
            } catch (Exception e2) {
                return null;
            }
        }
    }
}
