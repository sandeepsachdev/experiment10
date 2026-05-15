package com.sydneyevents.controller;

import com.sydneyevents.model.DayView;
import com.sydneyevents.model.Event;
import com.sydneyevents.model.WeatherDay;
import com.sydneyevents.service.EventService;
import com.sydneyevents.service.WeatherService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class HomeController {
    private static final ZoneId SYDNEY = ZoneId.of("Australia/Sydney");

    private final EventService eventService;
    private final WeatherService weatherService;

    public HomeController(EventService eventService, WeatherService weatherService) {
        this.eventService = eventService;
        this.weatherService = weatherService;
    }

    @GetMapping("/")
    public String home(Model model) {
        LocalDate today = LocalDate.now(SYDNEY);

        List<WeatherDay> forecast = weatherService.getNext7DaysForecast();
        Map<LocalDate, WeatherDay> weatherByDate = forecast.stream()
                .collect(Collectors.toMap(WeatherDay::getDate, w -> w, (a, b) -> a));

        List<Event> allEvents = eventService.getUpcomingEvents();

        List<DayView> days = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = today.plusDays(i);
            List<Event> dayEvents = allEvents.stream()
                    .filter(e -> e.occursOn(date))
                    .limit(8)
                    .toList();
            days.add(new DayView(date, weatherByDate.get(date), dayEvents));
        }

        model.addAttribute("days", days);
        model.addAttribute("totalEvents", allEvents.size());
        model.addAttribute("today", today);
        return "index";
    }

    @GetMapping("/health")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
