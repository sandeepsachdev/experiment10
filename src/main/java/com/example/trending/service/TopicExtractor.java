package com.example.trending.service;

import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts proper-noun phrases (capitalised word sequences) from a headline.
 * Crude but effective for surfacing entities like "Donald Trump", "Gaza", "Federal Reserve".
 */
@Service
public class TopicExtractor {

    private static final Pattern PROPER_NOUN_PHRASE =
            Pattern.compile("\\b([A-Z][a-z'’]+(?:\\s+(?:of\\s+|the\\s+|and\\s+|for\\s+)?[A-Z][a-z'’]+)*)\\b");

    private static final Set<String> STOPWORDS = Set.of(
            "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
            "january", "february", "march", "april", "may", "june", "july",
            "august", "september", "october", "november", "december",
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
            "of", "with", "by", "from", "up", "about", "into", "over", "after",
            "new", "old", "first", "last", "this", "that", "these", "those",
            "what", "who", "where", "when", "why", "how", "is", "are", "was", "were",
            "i", "you", "he", "she", "it", "we", "they", "my", "your", "his", "her",
            "us", "them", "live", "video", "watch", "news", "update", "updates",
            "report", "reports", "exclusive", "opinion", "analysis", "breaking",
            "today", "yesterday", "tomorrow", "year", "years", "day", "days",
            "mr", "mrs", "ms", "dr"
    );

    public Set<String> extract(String text) {
        Set<String> topics = new HashSet<>();
        if (text == null || text.isBlank()) return topics;

        Matcher m = PROPER_NOUN_PHRASE.matcher(text);
        while (m.find()) {
            String phrase = normalise(m.group(1));
            if (phrase == null) continue;
            if (phrase.length() < 3) continue;

            // Single-word phrase must not be a stopword
            if (!phrase.contains(" ") && STOPWORDS.contains(phrase)) continue;

            topics.add(phrase);
        }
        return topics;
    }

    private String normalise(String s) {
        if (s == null) return null;
        String trimmed = s.trim().toLowerCase();
        // strip trailing connective words that the regex may have included
        while (trimmed.endsWith(" of") || trimmed.endsWith(" the")
                || trimmed.endsWith(" and") || trimmed.endsWith(" for")) {
            trimmed = trimmed.substring(0, trimmed.lastIndexOf(' '));
        }
        return trimmed.isBlank() ? null : trimmed;
    }
}
