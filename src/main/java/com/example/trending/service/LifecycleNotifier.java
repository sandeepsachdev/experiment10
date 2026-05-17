package com.example.trending.service;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Emails on app lifecycle events: start, stop, and detected pauses.
 *
 * Pause detection: a heartbeat thread wakes every {@link #HEARTBEAT_INTERVAL_MS}ms
 * and measures the wall-clock gap vs. what was expected. A gap larger than
 * {@link #PAUSE_THRESHOLD_MS} means the JVM was suspended (host hibernated,
 * container throttled, etc.) — short GC pauses or scheduling jitter stay
 * well below this threshold.
 */
@Component
public class LifecycleNotifier {

    private static final Logger log = LoggerFactory.getLogger(LifecycleNotifier.class);

    private static final long HEARTBEAT_INTERVAL_MS = 30_000L;
    private static final long PAUSE_THRESHOLD_MS = 60_000L;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z", Locale.ENGLISH).withZone(ZoneId.of("UTC"));

    private final EmailService emailService;
    private final Thread heartbeatThread;
    private volatile boolean running = true;

    public LifecycleNotifier(EmailService emailService) {
        this.emailService = emailService;
        this.heartbeatThread = new Thread(this::heartbeatLoop, "lifecycle-heartbeat");
        this.heartbeatThread.setDaemon(true);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStart() {
        String when = now();
        log.info("Lifecycle: app started at {}", when);
        emailService.sendEmail(
                "Trending News Alerts: app started",
                "<p>The app started at <strong>" + when + "</strong>.</p>"
                        + "<p>The baseline poll is running now; the recurring poll begins 1 minute after baseline completes.</p>"
        );
        heartbeatThread.start();
    }

    @PreDestroy
    public void onStop() {
        running = false;
        heartbeatThread.interrupt();
        String when = now();
        log.info("Lifecycle: app stopping at {}", when);
        emailService.sendEmail(
                "Trending News Alerts: app stopped",
                "<p>The app is shutting down at <strong>" + when + "</strong>.</p>"
        );
    }

    private void heartbeatLoop() {
        long expected = System.currentTimeMillis() + HEARTBEAT_INTERVAL_MS;
        while (running) {
            try {
                Thread.sleep(HEARTBEAT_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            long actual = System.currentTimeMillis();
            long gap = actual - expected;
            if (gap > PAUSE_THRESHOLD_MS) {
                long pauseSeconds = gap / 1000;
                String when = now();
                log.warn("Lifecycle: detected pause of ~{}s ending at {}", pauseSeconds, when);
                emailService.sendEmail(
                        "Trending News Alerts: pause detected (~" + pauseSeconds + "s)",
                        "<p>The app appears to have been paused for approximately <strong>"
                                + pauseSeconds + " seconds</strong>, and resumed at <strong>"
                                + when + "</strong>.</p>"
                                + "<p>This usually means the container was throttled, hibernated, "
                                + "or the host was overloaded. Polls scheduled during the pause "
                                + "will fire late but should resume normally.</p>"
                );
            }
            expected = actual + HEARTBEAT_INTERVAL_MS;
        }
    }

    private static String now() {
        return FMT.format(Instant.now());
    }
}
