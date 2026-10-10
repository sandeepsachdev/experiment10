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

    private static final int MAX_LIVE_PER_DAY = 18;

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

            // Order: live events before curated, then short-run before long-run, then title.
            Comparator<Event> spanThenTitle = Comparator
                    .<Event, Boolean>comparing(Event::isCurated)
                    .thenComparing(HomeController::dayCount)
                    .thenComparing(Event::getTitle, Comparator.nullsLast(Comparator.naturalOrder()));

            // Cap live listings only; curated highlights are appended after them
            // (hidden on days with live events) so they never take a live slot.
            List<Event> dayEvents = new ArrayList<>(matching.stream()
                    .filter(e -> !e.isCurated())
                    .sorted(spanThenTitle)
                    .limit(MAX_LIVE_PER_DAY)
                    .toList());
            matching.stream()
                    .filter(Event::isCurated)
                    .sorted(spanThenTitle)
                    .forEach(dayEvents::add);

            days.add(new DayView(date, weatherByDate.get(date), dayEvents));
        }

        boolean anyCuratedHidden = days.stream()
                .anyMatch(d -> d.isHasLiveEvents()
                        && d.getEvents().stream().anyMatch(Event::isCurated));

        model.addAttribute("city", city);
        model.addAttribute("cities", cityRegistry.all());
        model.addAttribute("days", days);
        model.addAttribute("totalEvents", allEvents.stream().filter(e -> !e.isCurated()).count());
        model.addAttribute("lastRefreshed", eventService.lastRefreshed(city));
        model.addAttribute("today", today);
        model.addAttribute("anyCuratedHidden", anyCuratedHidden);
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
