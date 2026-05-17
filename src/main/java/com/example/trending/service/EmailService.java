package com.example.trending.service;

import com.example.trending.model.NewsItem;
import com.example.trending.model.TrendingTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * Sends alerts via Resend's HTTP API.
 * Why HTTP over SMTP: Railway containers commonly block outbound SMTP (port 25/465),
 * and even 587 with TLS is fiddly. A simple HTTPS POST is the reliable path.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String RESEND_ENDPOINT = "https://api.resend.com/emails";

    private final String apiKey;
    private final String fromAddress;
    private final String recipient;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public EmailService(@Value("${resend.apiKey}") String apiKey,
                        @Value("${resend.from}") String fromAddress,
                        @Value("${alert.recipient}") String recipient) {
        this.apiKey = apiKey;
        this.fromAddress = fromAddress;
        this.recipient = recipient;
    }

    public void sendAlert(List<TrendingTopic> topics) {
        String subject = topics.size() == 1
                ? "Trending: " + topics.get(0).topic()
                : topics.size() + " new trending topics";
        sendEmail(subject, renderHtml(topics));
    }

    public boolean sendEmail(String subject, String html) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("RESEND_API_KEY not configured — skipping email '{}'", subject);
            return false;
        }
        if (recipient == null || recipient.isBlank()) {
            log.warn("ALERT_RECIPIENT not configured — skipping email '{}'", subject);
            return false;
        }

        String body = """
                {
                  "from": %s,
                  "to": [%s],
                  "subject": %s,
                  "html": %s
                }
                """.formatted(jsonString(fromAddress), jsonString(recipient), jsonString(subject), jsonString(html));

        HttpRequest req = HttpRequest.newBuilder(URI.create(RESEND_ENDPOINT))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                log.info("Sent email '{}'", subject);
                return true;
            }
            log.error("Resend API returned {} for '{}': {}", resp.statusCode(), subject, resp.body());
        } catch (Exception e) {
            log.error("Failed to send email '{}'", subject, e);
        }
        return false;
    }

    private String renderHtml(List<TrendingTopic> topics) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h2>New trending topics</h2><ul>");
        for (TrendingTopic t : topics) {
            sb.append("<li><strong>").append(escape(t.topic())).append("</strong> — across ")
                    .append(t.sourceCount()).append(" sources (")
                    .append(escape(String.join(", ", t.sources()))).append(")");
            if (!t.sampleArticles().isEmpty()) {
                sb.append("<ul>");
                for (NewsItem a : t.sampleArticles()) {
                    sb.append("<li><a href=\"").append(escape(a.link() == null ? "#" : a.link()))
                            .append("\">").append(escape(a.title())).append("</a> <em>(")
                            .append(escape(a.source())).append(")</em></li>");
                }
                sb.append("</ul>");
            }
            sb.append("</li>");
        }
        sb.append("</ul>");
        return sb.toString();
    }

    private static String jsonString(String s) {
        if (s == null) return "\"\"";
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.append("\"").toString();
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
