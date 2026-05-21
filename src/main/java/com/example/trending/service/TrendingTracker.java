package com.example.trending.service;

import com.example.trending.model.NewsItem;
import com.example.trending.model.TrendingTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
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

    /** Every raw topic phrase that has been part of a trending cluster (baseline or alerted). */
    private final Set<String> seenMembers = new HashSet<>();
    /** Word sets of the seen phrases, used to detect subset/superset relationships across polls. */
    private final List<Set<String>> seenWordSets = new ArrayList<>();
    /** Topics shown on the dashboard, keyed by the canonical name chosen at detection time. */
    private final Map<String, TrendingTopic> displayed = new ConcurrentHashMap<>();
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
        int count = 0;
        for (Map.Entry<String, TopicAggregate> e : agg.entrySet()) {
            if (!isTrending(e.getValue())) continue;
            e.getValue().members.forEach(this::markSeen);
            displayed.put(e.getKey(), toTrendingTopic(e.getKey(), e.getValue(), startedAt, true));
            count++;
        }
        baselineReady.set(true);
        log.info("Baseline established: {} trending topics shown on dashboard but not emailed. Examples: {}",
                count, displayed.keySet().stream().limit(10).toList());
    }

    public synchronized void ingest(List<NewsItem> items) {
        Map<String, TopicAggregate> agg = aggregate(items);
        List<TrendingTopic> brandNew = new ArrayList<>();

        for (Map.Entry<String, TopicAggregate> e : agg.entrySet()) {
            TopicAggregate a = e.getValue();
            if (!isTrending(a)) continue;
            // Skip if this cluster is a subset/superset of (or identical to) anything
            // already seen — that makes it "the same topic", so no fresh alert.
            if (relatedToSeen(a)) continue;

            a.members.forEach(this::markSeen);
            TrendingTopic tt = toTrendingTopic(e.getKey(), a, Instant.now(), false);
            displayed.put(e.getKey(), tt);
            brandNew.add(tt);
        }

        if (!brandNew.isEmpty()) {
            log.info("Detected {} newly trending topics: {}",
                    brandNew.size(), brandNew.stream().map(TrendingTopic::topic).toList());
            emailService.sendAlert(brandNew);
        }
    }

    public List<TrendingTopic> getNewlyTrending() {
        return displayed.values().stream()
                .sorted(Comparator.comparing(TrendingTopic::firstDetected).reversed())
                .toList();
    }

    /** A topic trends only when it surfaces in feeds from multiple countries. */
    private boolean isTrending(TopicAggregate a) {
        return a.countries.size() >= minCountries;
    }

    private void markSeen(String phrase) {
        if (seenMembers.add(phrase)) {
            seenWordSets.add(wordsOf(phrase));
        }
    }

    /** True if any member phrase is a subset/superset of (or equal to) a previously-seen phrase. */
    private boolean relatedToSeen(TopicAggregate cluster) {
        for (String member : cluster.members) {
            if (seenMembers.contains(member)) return true;
            Set<String> mw = wordsOf(member);
            for (Set<String> sw : seenWordSets) {
                if (isSubsetEither(mw, sw)) return true;
            }
        }
        return false;
    }

    private TrendingTopic toTrendingTopic(String topic, TopicAggregate a, Instant when, boolean fromBaseline) {
        return new TrendingTopic(
                topic,
                when,
                a.countries.size(),
                new ArrayList<>(new TreeSet<>(a.countries)),
                a.sources.size(),
                new ArrayList<>(new TreeSet<>(a.sources)),
                a.articles.stream().distinct().limit(3).toList(),
                fromBaseline
        );
    }

    private Map<String, TopicAggregate> aggregate(List<NewsItem> items) {
        Map<String, TopicAggregate> raw = new HashMap<>();
        for (NewsItem item : items) {
            Set<String> topics = extractor.extract(item.title() + " " + item.description());
            for (String topic : topics) {
                TopicAggregate a = raw.computeIfAbsent(topic, k -> new TopicAggregate());
                a.members.add(topic);
                a.sources.add(item.source());
                a.countries.add(item.country());
                a.articles.add(item);
            }
        }
        return mergeSubsetTopics(raw);
    }

    /**
     * Merge topics whose words are a subset of another topic's words — e.g.
     * "donald trump" and "donald trump tariffs" are treated as one topic.
     * Each merge cluster keeps the broadest-reaching member as its canonical name.
     */
    private Map<String, TopicAggregate> mergeSubsetTopics(Map<String, TopicAggregate> raw) {
        List<String> topics = new ArrayList<>(raw.keySet());
        int n = topics.size();
        if (n < 2) return raw;

        List<Set<String>> words = new ArrayList<>(n);
        for (String t : topics) words.add(wordsOf(t));

        int[] parent = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (isSubsetEither(words.get(i), words.get(j))) {
                    union(parent, i, j);
                }
            }
        }

        Map<Integer, List<Integer>> clusters = new HashMap<>();
        for (int i = 0; i < n; i++) {
            clusters.computeIfAbsent(find(parent, i), k -> new ArrayList<>()).add(i);
        }

        // Canonical = broadest reach, ties broken by longer (more specific) then alphabetical.
        Comparator<String> byReach = Comparator
                .<String>comparingInt(t -> raw.get(t).countries.size())
                .thenComparingInt(t -> raw.get(t).sources.size())
                .thenComparingInt(t -> raw.get(t).articles.size())
                .thenComparingInt(String::length)
                .thenComparing(Comparator.<String>reverseOrder());

        Map<String, TopicAggregate> merged = new HashMap<>();
        for (List<Integer> cluster : clusters.values()) {
            TopicAggregate combined = new TopicAggregate();
            String canonical = null;
            for (int idx : cluster) {
                String t = topics.get(idx);
                TopicAggregate a = raw.get(t);
                combined.members.addAll(a.members);
                combined.sources.addAll(a.sources);
                combined.countries.addAll(a.countries);
                combined.articles.addAll(a.articles);
                if (canonical == null || byReach.compare(t, canonical) > 0) canonical = t;
            }
            merged.put(canonical, combined);
        }
        return merged;
    }

    private static Set<String> wordsOf(String phrase) {
        return new HashSet<>(Arrays.asList(phrase.split(" ")));
    }

    /** True if {@code a} is a subset of {@code b}, or {@code b} a subset of {@code a}. */
    private static boolean isSubsetEither(Set<String> a, Set<String> b) {
        return a.size() <= b.size() ? b.containsAll(a) : a.containsAll(b);
    }

    private static int find(int[] parent, int i) {
        while (parent[i] != i) {
            parent[i] = parent[parent[i]];
            i = parent[i];
        }
        return i;
    }

    private static void union(int[] parent, int a, int b) {
        int ra = find(parent, a);
        int rb = find(parent, b);
        if (ra != rb) parent[ra] = rb;
    }

    private static class TopicAggregate {
        final Set<String> members = new HashSet<>();
        final Set<String> sources = new HashSet<>();
        final Set<String> countries = new HashSet<>();
        final List<NewsItem> articles = new ArrayList<>();
    }
}
