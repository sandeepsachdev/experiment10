package com.sydneyevents.service.scraper;

import com.sydneyevents.model.Event;
import com.sydneyevents.service.EventScraper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Scrapes City of Sydney's "What's On" listing.
 * Site: https://whatson.cityofsydney.nsw.gov.au/
 */
@Component
public class WhatsOnSydneyScraper implements EventScraper {
    private static final Logger log = LoggerFactory.getLogger(WhatsOnSydneyScraper.class);
    private static final String URL = "https://whatson.cityofsydney.nsw.gov.au/?past=false";

    @Value("${app.scrape.user-agent}")
    private String userAgent;

    @Value("${app.scrape.timeout-ms}")
    private int timeoutMs;

    @Override
    public String sourceName() {
        return "City of Sydney - What's On";
    }

    @Override
    public List<Event> scrape() {
        List<Event> events = new ArrayList<>();
        try {
            Document doc = Jsoup.connect(URL)
                    .userAgent(userAgent)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "en-AU,en;q=0.9")
                    .timeout(timeoutMs)
                    .get();

            Elements cards = doc.select("article, .event-card, [data-event], li.event, a[href*='/event']");
            for (Element card : cards) {
                Event e = parseCard(card);
                if (e != null && e.getTitle() != null && !e.getTitle().isBlank()) {
                    events.add(e);
                }
                if (events.size() >= 40) break;
            }
            log.info("WhatsOnSydneyScraper found {} events", events.size());
        } catch (Exception e) {
            log.warn("WhatsOnSydneyScraper failed: {}", e.getMessage());
        }
        return events;
    }

    private Event parseCard(Element card) {
        try {
            String title = textOf(card, "h2, h3, .event-title, [class*=title]");
            String desc = textOf(card, "p, .event-description, [class*=description], [class*=summary]");
            String href = absUrl(card, "a[href]");
            String img = absUrl(card, "img");
            String dateStr = textOf(card, "time, .event-date, [class*=date]");
            String venue = textOf(card, ".event-venue, [class*=venue], [class*=location]");

            if (title == null) return null;

            Event ev = new Event();
            ev.setTitle(title.trim());
            ev.setDescription(desc != null ? desc.trim() : "");
            ev.setUrl(href);
            ev.setImageUrl(img);
            ev.setVenue(venue);
            ev.setStartDate(parseDate(dateStr));
            ev.setCategory("What's On");
            ev.setSource(sourceName());
            return ev;
        } catch (Exception ex) {
            return null;
        }
    }

    private String textOf(Element parent, String selector) {
        Element el = parent.selectFirst(selector);
        return el == null ? null : el.text();
    }

    private String absUrl(Element parent, String selector) {
        Element el = parent.selectFirst(selector);
        if (el == null) return null;
        String key = "img".equals(el.tagName()) ? "src" : "href";
        String v = el.absUrl(key);
        return v.isBlank() ? null : v;
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        String t = s.trim();
        for (DateTimeFormatter f : List.of(
                DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH))) {
            try {
                return LocalDate.parse(t, f);
            } catch (Exception ignored) { }
        }
        return null;
    }
}
