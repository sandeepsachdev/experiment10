package com.example.trending.model;

import java.time.Instant;
import java.util.List;

public record TrendingTopic(
        String topic,
        Instant firstDetected,
        int sourceCount,
        List<String> sources,
        List<NewsItem> sampleArticles
) {
}
