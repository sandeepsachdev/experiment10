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
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class TrendingTracker {

    private static final Logger log = LoggerFactory.getLogger(TrendingTracker.class);

    private final TopicExtractor extractor;
    private final EmailService emailService;
    private final int minCountries;

    private final Set<String> baselineTopics = ConcurrentHashMap.newKeySet();
    private final Map<String, TrendingTopic> newlyTrending = new ConcurrentHashMap<>();
    private final AtomicBoolean baselineReady = new AtomicBoolean(false);
    private volatile Instant startedAt = Instant.now();

    public TrendingTracker(TopicExtractor extractor,
                           EmailService emailService,
                           @Value("${trending.minCountries}") int minCountries) {
        this.extractor = extractor;
        this.emailService = emailService;
        this.minCountries = minCountries;
    }

    public boolean isBaselineReady() {
        return baselineReady.get();
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public synchronized void recordBaseline(List<NewsItem> items) {
        Map<String, TopicAggregate> agg = aggregate(items);
        startedAt = Instant.now();
        for (Map.Entry<String, TopicAggregate> e : agg.entrySet()) {
            if (!isTrending(e.getValue())) continue;
            String topic = e.getKey();
            baselineTopics.add(topic);
            newlyTrending.put(topic, toTrendingTopic(topic, e.getValue(), startedAt, true));
        }
        baselineReady.set(true);
        log.info("Baseline established: {} trending topics shown on dashboard but not emailed. Examples: {}",
                baselineTopics.size(),
                baselineTopics.stream().limit(10).toList());
    }

    public synchronized void ingest(List<NewsItem> items) {
        Map<String, TopicAggregate> agg = aggregate(items);
        List<TrendingTopic> brandNew = new ArrayList<>();

        for (Map.Entry<String, TopicAggregate> e : agg.entrySet()) {
            String topic = e.getKey();
            if (!isTrending(e.getValue())) continue;
            if (baselineTopics.contains(topic)) continue;
            if (newlyTrending.containsKey(topic)) continue;

            TrendingTopic tt = toTrendingTopic(topic, e.getValue(), Instant.now(), false);
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

    /** A topic trends only when it surfaces in feeds from multiple countries. */
    private boolean isTrending(TopicAggregate a) {
        return a.countries.size() >= minCountries;
    }

    private TrendingTopic toTrendingTopic(String topic, TopicAggregate a, Instant when, boolean fromBaseline) {
        return new TrendingTopic(
                topic,
                when,
                a.countries.size(),
                new ArrayList<>(new TreeSet<>(a.countries)),
                a.sources.size(),
                new ArrayList<>(new TreeSet<>(a.sources)),
                a.articles.stream().limit(3).toList(),
                fromBaseline
        );
    }

    private Map<String, TopicAggregate> aggregate(List<NewsItem> items) {
        Map<String, TopicAggregate> agg = new HashMap<>();
        for (NewsItem item : items) {
            Set<String> topics = extractor.extract(item.title() + " " + item.description());
            for (String topic : topics) {
                TopicAggregate a = agg.computeIfAbsent(topic, k -> new TopicAggregate());
                a.sources.add(item.source());
                a.countries.add(item.country());
                a.articles.add(item);
            }
        }
        return agg;
    }

    private static class TopicAggregate {
        final Set<String> sources = new HashSet<>();
        final Set<String> countries = new HashSet<>();
        final List<NewsItem> articles = new ArrayList<>();
    }
}
