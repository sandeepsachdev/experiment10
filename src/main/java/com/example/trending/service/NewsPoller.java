package com.example.trending.service;

import com.example.trending.model.NewsItem;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class NewsPoller {

    private static final Logger log = LoggerFactory.getLogger(NewsPoller.class);

    /** A configured feed: display name, country of origin, RSS URL. */
    private record Feed(String name, String country, String url) {
    }

    private final TrendingTracker tracker;
    private final TaskScheduler taskScheduler;
    private final String feedsConfig;
    private final long intervalMinutes;
    private final long initialDelaySeconds;

    private final List<Feed> feeds = new ArrayList<>();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public NewsPoller(TrendingTracker tracker,
                      TaskScheduler taskScheduler,
                      @Value("${trending.feeds}") String feedsConfig,
                      @Value("${trending.poll.intervalMinutes}") long intervalMinutes,
                      @Value("${trending.poll.initialDelayAfterBaselineSeconds}") long initialDelaySeconds) {
        this.tracker = tracker;
        this.taskScheduler = taskScheduler;
        this.feedsConfig = feedsConfig;
        this.intervalMinutes = intervalMinutes;
        this.initialDelaySeconds = initialDelaySeconds;
    }

    @PostConstruct
    void init() {
        for (String entry : feedsConfig.split(",")) {
            String trimmed = entry.trim();
            // format: name|country|url
            String[] parts = trimmed.split("\\|", 3);
            if (parts.length < 3) continue;
            feeds.add(new Feed(parts[0].trim(), parts[1].trim(), parts[2].trim()));
        }
        log.info("Configured {} news feeds across {} countries: {}",
                feeds.size(),
                feeds.stream().map(Feed::country).distinct().count(),
                feeds.stream().map(f -> f.name() + " (" + f.country() + ")").toList());

        // Run baseline poll on startup, in a background thread so we don't block app start.
        // Once baseline completes, the recurring poll is scheduled relative to that moment.
        Thread baseline = new Thread(this::runBaselinePoll, "baseline-poll");
        baseline.setDaemon(true);
        baseline.start();
    }

    private void runBaselinePoll() {
        log.info("Running baseline poll across all sources — topics found here will NOT trigger alerts");
        List<NewsItem> items = pollAll();
        tracker.recordBaseline(items);

        Instant firstRun = Instant.now().plusSeconds(initialDelaySeconds);
        Duration interval = Duration.ofMinutes(intervalMinutes);
        taskScheduler.scheduleAtFixedRate(this::scheduledPoll, firstRun, interval);
        log.info("First scheduled poll at {} ({}s after baseline), then every {} min",
                firstRun, initialDelaySeconds, intervalMinutes);
    }

    private void scheduledPoll() {
        log.info("Running scheduled poll");
        List<NewsItem> items = pollAll();
        tracker.ingest(items);
    }

    private List<NewsItem> pollAll() {
        List<NewsItem> all = new ArrayList<>();
        for (Feed feed : feeds) {
            try {
                all.addAll(fetchFeed(feed));
            } catch (Exception e) {
                log.warn("Failed to poll {} ({}): {}", feed.name(), feed.url(), e.getMessage());
            }
        }
        log.info("Polled {} items across {} sources", all.size(), feeds.size());
        return all;
    }

    private List<NewsItem> fetchFeed(Feed feed) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(feed.url()))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "trending-news-alerts/1.0 (+https://github.com)")
                .GET()
                .build();
        HttpResponse<byte[]> resp = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (resp.statusCode() >= 400) {
            throw new RuntimeException("HTTP " + resp.statusCode());
        }
        try (XmlReader reader = new XmlReader(new java.io.ByteArrayInputStream(resp.body()))) {
            SyndFeed syndFeed = new SyndFeedInput().build(reader);
            List<NewsItem> items = new ArrayList<>();
            for (SyndEntry entry : syndFeed.getEntries()) {
                String title = entry.getTitle();
                String description = entry.getDescription() == null ? "" : entry.getDescription().getValue();
                String link = entry.getLink();
                if (title != null) {
                    items.add(new NewsItem(feed.name(), feed.country(), title, description, link));
                }
            }
            return items;
        }
    }
}
