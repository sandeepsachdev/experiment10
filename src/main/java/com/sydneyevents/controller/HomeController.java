package com.sydneyevents.controller;

import com.sydneyevents.model.City;
import com.sydneyevents.model.DayView;
import com.sydneyevents.model.Event;
import com.sydneyevents.model.WeatherDay;
import com.sydneyevents.service.CityRegistry;
import com.sydneyevents.service.CuratedDataLoader;
import com.sydneyevents.service.EventService;
import com.sydneyevents.service.WeatherService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    private final EventService eventService;
    private final WeatherService weatherService;
    private final CityRegistry cityRegistry;

    public HomeController(EventService eventService,
                          WeatherService weatherService,
                          CityRegistry cityRegistry) {
        this.eventService = eventService;
        this.weatherService = weatherService;
        this.cityRegistry = cityRegistry;
    }

    @GetMapping("/")
    public String home(@RequestParam(value = "city", required = false) String citySlug,
                       Model model) {
        City city = cityRegistry.get(citySlug);
        LocalDate today = CuratedDataLoader.today(city);

        List<WeatherDay> forecast = weatherService.getNext7DaysForecast(city);
        Map<LocalDate, WeatherDay> weatherByDate = forecast.stream()
                .collect(Collectors.toMap(WeatherDay::getDate, w -> w, (a, b) -> a));

        List<Event> allEvents = eventService.getUpcomingEvents(city);

        List<DayView> days = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = today.plusDays(i);
            List<Event> matching = allEvents.stream()
                    .filter(e -> e.occursOn(date))
                    .toList();

            Comparator<Event> spanThenTitle = Comparator
                    .<Event, Long>comparing(HomeController::dayCount)
                    .thenComparing(Event::getTitle, Comparator.nullsLast(Comparator.naturalOrder()));

            List<Event> dayEvents = matching.stream()
                    .sorted(spanThenTitle)
                    .limit(12)
                    .toList();

            days.add(new DayView(date, weatherByDate.get(date), dayEvents));
        }

        model.addAttribute("city", city);
        model.addAttribute("cities", cityRegistry.all());
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

    private static long dayCount(Event e) {
        if (e.getStartDate() == null) return Long.MAX_VALUE;
        LocalDate end = e.getEndDate() != null ? e.getEndDate() : e.getStartDate();
        return end.toEpochDay() - e.getStartDate().toEpochDay() + 1;
    }
}
