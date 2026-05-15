package com.sydneyevents.service.scraper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sydneyevents.model.City;
import com.sydneyevents.model.Event;
import com.sydneyevents.service.EventScraper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * City of Sydney's official "What's On" feed via the Opendatasoft Explore API.
 * Sydney-only.
 */
@Component
public class CitySydneyOpenDataScraper implements EventScraper {
    private static final Logger log = LoggerFactory.getLogger(CitySydneyOpenDataScraper.class);

    private static final String[] DATASET_IDS = {
            "whats-on", "whats-on-public-events", "events"
    };

    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public CitySydneyOpenDataScraper(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public String sourceName() {
        return "City of Sydney Open Data";
    }

    @Override
    public boolean supports(City city) {
        return "sydney".equals(city.slug());
    }

    @Override
    public List<Event> scrape(City city) {
        for (String dataset : DATASET_IDS) {
            try {
                List<Event> found = fetch(dataset);
                if (!found.isEmpty()) {
                    log.info("CitySydneyOpenData: {} events from dataset '{}'",
                            found.size(), dataset);
                    return found;
                }
            } catch (Exception ex) {
                log.debug("Dataset '{}' attempt failed: {}", dataset, ex.getMessage());
            }
        }
        log.info("CitySydneyOpenData: no events found");
        return List.of();
    }

    private List<Event> fetch(String datasetId) throws Exception {
        LocalDate today = LocalDate.now(ZoneId.of("Australia/Sydney"));
        String where = URLEncoder.encode(
                "end_date >= date'" + today + "'", StandardCharsets.UTF_8);
        String url = "https://data.cityofsydney.nsw.gov.au/api/explore/v2.1/catalog/datasets/"
                + datasetId
                + "/records?limit=80&order_by=start_date%20asc&where=" + where;

        log.info("CitySydneyOpenData request: GET {}", url);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0 SydneyEvents")
                .GET()
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        log.info("CitySydneyOpenData response: HTTP {} ({} bytes)",
                resp.statusCode(), resp.body() == null ? 0 : resp.body().length());
        if (resp.statusCode() != 200) return List.of();

        JsonNode root = mapper.readTree(resp.body());
        JsonNode results = root.path("results");
        List<Event> out = new ArrayList<>();
        for (JsonNode r : results) {
            Event e = toEvent(r);
            if (e != null) out.add(e);
        }
        return out;
    }

    private Event toEvent(JsonNode r) {
        String title = firstText(r, "title", "name", "event_name", "event_title");
        if (title == null) return null;

        Event e = new Event();
        e.setTitle(title);
        e.setDescription(firstText(r, "description", "short_description", "event_description"));
        e.setVenue(firstText(r, "venue", "location", "address"));
        e.setUrl(firstText(r, "url", "more_information", "event_url", "link"));
        e.setImageUrl(firstImage(r));
        e.setStartDate(parseDate(firstText(r, "start_date", "startdate", "start", "event_start")));
        e.setEndDate(parseDate(firstText(r, "end_date", "enddate", "end", "event_end")));
        e.setCategory(firstText(r, "category", "type", "event_type"));
        e.setSource(sourceName());
        return e;
    }

    private String firstText(JsonNode node, String... fields) {
        for (String f : fields) {
            JsonNode v = node.path(f);
            if (v.isTextual() && !v.asText().isBlank()) return v.asText();
        }
        return null;
    }

    private String firstImage(JsonNode node) {
        for (String f : new String[]{"image", "image_url", "thumbnail", "photo", "media"}) {
            JsonNode v = node.path(f);
            if (v.isTextual() && !v.asText().isBlank()) return v.asText();
            if (v.isObject()) {
                JsonNode url = v.path("url");
                if (url.isTextual()) return url.asText();
                JsonNode thumb = v.path("thumbnail");
                if (thumb.isTextual()) return thumb.asText();
            }
        }
        return null;
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDate.parse(s.substring(0, Math.min(10, s.length())));
        } catch (Exception ex) {
            try { return OffsetDateTime.parse(s).toLocalDate(); }
            catch (Exception e2) { return null; }
        }
    }
}
