package com.sydneyevents.service.scraper;

import com.sydneyevents.model.City;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Scrapes the City of Melbourne "What's On" site.
 * The site is a Rails/Turbo app — event cards are server-rendered as
 * div.page-preview elements with a.main-link, h3.title, time[datetime], img.
 */
@Component
public class WhatsonMelbourneScraper implements EventScraper {
    private static final Logger log = LoggerFactory.getLogger(WhatsonMelbourneScraper.class);
    private static final String BASE = "https://whatson.melbourne.vic.gov.au";

    private static final List<String> PAGES = List.of(
            BASE + "/article/whats-free-in-melbourne-this-month",
            BASE + "/things-to-do/major-events",
            BASE + "/"
    );

    @Value("${app.scrape.user-agent}")
    private String userAgent;

    @Value("${app.scrape.timeout-ms}")
    private int timeoutMs;

    @Override
    public String sourceName() {
        return "City of Melbourne - What's On";
    }

    @Override
    public boolean supports(City city) {
        return "melbourne".equals(city.slug());
    }

    @Override
    public List<Event> scrape(City city) {
        Map<String, Event> byTitle = new LinkedHashMap<>();
        for (String pageUrl : PAGES) {
            try {
                log.info("WhatsonMelbourne request: GET {}", pageUrl);
                Document doc = Jsoup.connect(pageUrl)
                        .userAgent(userAgent)
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .header("Accept-Language", "en-AU,en;q=0.9")
                        .timeout(timeoutMs)
                        .get();

                // Listing pages use div.page-preview; article pages use div.listing-container
                Elements cards = doc.select("div.page-preview, div.listing-container");
                int before = byTitle.size();
                for (Element card : cards) {
                    Event e = parseCard(card);
                    if (e != null && e.getTitle() != null) {
                        byTitle.putIfAbsent(e.getTitle().toLowerCase(), e);
                    }
                }
                log.info("WhatsonMelbourne response: {} new events from {}", byTitle.size() - before, pageUrl);
            } catch (Exception ex) {
                log.warn("WhatsonMelbourneScraper failed for {}: {}", pageUrl, ex.getMessage());
            }
        }

        List<Event> events = new ArrayList<>(byTitle.values());
        log.info("WhatsonMelbourne total: {} events", events.size());
        return events;
    }

    private Event parseCard(Element card) {
        // Listing pages: a.main-link; article pages: a.link
        Element link = card.selectFirst("a.main-link, a.link");
        if (link == null) return null;

        String title = link.selectFirst("h3.title") != null
                ? link.selectFirst("h3.title").text().trim()
                : null;
        if (title == null || title.isBlank()) return null;

        String href = link.attr("href");
        String url = href.startsWith("http") ? href : BASE + href;

        // Dates: search the whole card — on some pages they live outside the <a>
        Elements times = card.select("time[datetime]");
        LocalDate startDate = null;
        LocalDate endDate = null;
        if (!times.isEmpty()) {
            startDate = parseDate(times.first().attr("datetime"));
            if (times.size() > 1) endDate = parseDate(times.last().attr("datetime"));
        }

        // Image — prefer the link's img, fall back to any img in card
        Element img = card.selectFirst("img");
        String imageUrl = img != null ? img.absUrl("src") : null;
        if (imageUrl == null || imageUrl.isBlank()) {
            imageUrl = img != null ? img.attr("src") : null;
            if (imageUrl != null && !imageUrl.startsWith("http")) {
                imageUrl = BASE + imageUrl;
            }
        }

        // Description: p.summary (listing pages) or div.rich-text > p (article pages)
        Element summary = card.selectFirst("p.summary");
        if (summary == null) summary = card.selectFirst("div.rich-text p");
        String description = summary != null ? summary.text().trim() : null;

        Event e = new Event();
        e.setTitle(title);
        e.setDescription(description == null || description.isBlank() ? null : description);
        e.setUrl(url);
        e.setImageUrl(imageUrl);
        e.setStartDate(startDate);
        e.setEndDate(endDate);
        e.setCategory("Melbourne");
        e.setSource(sourceName());
        return e;
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s.substring(0, Math.min(10, s.length()))); }
        catch (Exception ignored) { return null; }
    }
}
