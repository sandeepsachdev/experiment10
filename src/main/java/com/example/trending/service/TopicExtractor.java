package com.example.trending.service;

import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts multi-word proper-noun phrases (two or more capitalised words)
 * from a headline — e.g. "donald trump", "federal reserve", "white house".
 * Single-word entities are intentionally ignored.
 */
@Service
public class TopicExtractor {

    private static final Pattern PROPER_NOUN_PHRASE =
            Pattern.compile("\\b([A-Z][a-z'’]+(?:\\s+(?:of\\s+|the\\s+|and\\s+|for\\s+)?[A-Z][a-z'’]+)+)\\b");

    public Set<String> extract(String text) {
        Set<String> topics = new HashSet<>();
        if (text == null || text.isBlank()) return topics;

        Matcher m = PROPER_NOUN_PHRASE.matcher(text);
        while (m.find()) {
            String phrase = normalise(m.group(1));
            if (phrase == null) continue;
            if (!phrase.contains(" ")) continue; // require >= 2 words after normalisation
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
