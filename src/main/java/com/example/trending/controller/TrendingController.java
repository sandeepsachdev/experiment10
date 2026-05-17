package com.example.trending.controller;

import com.example.trending.service.TrendingTracker;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Controller
public class TrendingController {

    private final TrendingTracker tracker;
    private final DateTimeFormatter fmt =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z", Locale.ENGLISH)
                    .withZone(ZoneId.of("Australia/Sydney"));

    public TrendingController(TrendingTracker tracker) {
        this.tracker = tracker;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("topics", tracker.getNewlyTrending());
        model.addAttribute("baselineReady", tracker.isBaselineReady());
        model.addAttribute("startedAt", fmt.format(tracker.getStartedAt()));
        model.addAttribute("fmt", fmt);
        return "index";
    }
}
