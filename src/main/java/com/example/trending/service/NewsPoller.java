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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class NewsPoller {

    private static final Logger log = LoggerFactory.getLogger(NewsPoller.class);

    private final TrendingTracker tracker;
    private final TaskScheduler taskScheduler;
    private final String feedsConfig;
    private final long intervalMinutes;
    private final long initialDelaySeconds;

    private final Map<String, String> feeds = new LinkedHashMap<>();
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
            int pipe = trimmed.indexOf('|');
            if (pipe <= 0) continue;
            feeds.put(trimmed.substring(0, pipe).trim(), trimmed.substring(pipe + 1).trim());
        }
        log.info("Configured {} news feeds: {}", feeds.size(), feeds.keySet());

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
        for (Map.Entry<String, String> feed : feeds.entrySet()) {
            try {
                all.addAll(fetchFeed(feed.getKey(), feed.getValue()));
            } catch (Exception e) {
                log.warn("Failed to poll {} ({}): {}", feed.getKey(), feed.getValue(), e.getMessage());
            }
        }
        log.info("Polled {} items across {} sources", all.size(), feeds.size());
        return all;
    }

    private List<NewsItem> fetchFeed(String source, String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "trending-news-alerts/1.0 (+https://github.com)")
                .GET()
                .build();
        HttpResponse<byte[]> resp = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (resp.statusCode() >= 400) {
            throw new RuntimeException("HTTP " + resp.statusCode());
        }
        try (XmlReader reader = new XmlReader(new java.io.ByteArrayInputStream(resp.body()))) {
            SyndFeed feed = new SyndFeedInput().build(reader);
            List<NewsItem> items = new ArrayList<>();
            for (SyndEntry entry : feed.getEntries()) {
                String title = entry.getTitle();
                String description = entry.getDescription() == null ? "" : entry.getDescription().getValue();
                String link = entry.getLink();
                if (title != null) items.add(new NewsItem(source, title, description, link));
            }
            return items;
        }
    }
}
