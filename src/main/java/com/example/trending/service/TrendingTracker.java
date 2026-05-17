package com.example.trending.service;

import com.example.trending.model.NewsItem;
import com.example.trending.model.TrendingTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class TrendingTracker {

    private static final Logger log = LoggerFactory.getLogger(TrendingTracker.class);

    private final TopicExtractor extractor;
    private final EmailService emailService;
    private final int minSources;

    private final Set<String> baselineTopics = ConcurrentHashMap.newKeySet();
    private final Map<String, TrendingTopic> newlyTrending = new ConcurrentHashMap<>();
    private final AtomicBoolean baselineReady = new AtomicBoolean(false);
    private volatile Instant startedAt = Instant.now();

    public TrendingTracker(TopicExtractor extractor,
                           EmailService emailService,
                           @Value("${trending.minSources}") int minSources) {
        this.extractor = extractor;
        this.emailService = emailService;
        this.minSources = minSources;
    }

    public boolean isBaselineReady() {
        return baselineReady.get();
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public synchronized void recordBaseline(List<NewsItem> items) {
        Map<String, TopicAggregate> agg = aggregate(items);
        for (Map.Entry<String, TopicAggregate> e : agg.entrySet()) {
            if (e.getValue().sources.size() >= minSources) {
                baselineTopics.add(e.getKey());
            }
        }
        startedAt = Instant.now();
        baselineReady.set(true);
        log.info("Baseline established: {} trending topics ignored (will not alert). Examples: {}",
                baselineTopics.size(),
                baselineTopics.stream().limit(10).toList());
    }

    public synchronized void ingest(List<NewsItem> items) {
        Map<String, TopicAggregate> agg = aggregate(items);
        List<TrendingTopic> brandNew = new ArrayList<>();

        for (Map.Entry<String, TopicAggregate> e : agg.entrySet()) {
            String topic = e.getKey();
            TopicAggregate a = e.getValue();
            if (a.sources.size() < minSources) continue;
            if (baselineTopics.contains(topic)) continue;
            if (newlyTrending.containsKey(topic)) continue;

            TrendingTopic tt = new TrendingTopic(
                    topic,
                    Instant.now(),
                    a.sources.size(),
                    new ArrayList<>(a.sources),
                    a.articles.stream().limit(3).toList()
            );
            newlyTrending.put(topic, tt);
            brandNew.add(tt);
        }

        if (!brandNew.isEmpty()) {
            log.info("Detected {} newly trending topics: {}",
                    brandNew.size(), brandNew.stream().map(TrendingTopic::topic).toList());
            emailService.sendAlert(brandNew);
        }
    }

    public List<TrendingTopic> getNewlyTrending() {
        return newlyTrending.values().stream()
                .sorted(Comparator.comparing(TrendingTopic::firstDetected).reversed())
                .toList();
    }

    private Map<String, TopicAggregate> aggregate(List<NewsItem> items) {
        Map<String, TopicAggregate> agg = new HashMap<>();
        for (NewsItem item : items) {
            Set<String> topics = extractor.extract(item.title() + " " + item.description());
            for (String topic : topics) {
                TopicAggregate a = agg.computeIfAbsent(topic, k -> new TopicAggregate());
                a.sources.add(item.source());
                a.articles.add(item);
            }
        }
        return agg;
    }

    private static class TopicAggregate {
        final Set<String> sources = new HashSet<>();
        final List<NewsItem> articles = new ArrayList<>();
    }
}
