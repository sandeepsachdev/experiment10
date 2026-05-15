package com.sydneyevents.service.scraper;

import com.sydneyevents.model.City;
import com.sydneyevents.model.Event;
import com.sydneyevents.service.CuratedDataLoader;
import com.sydneyevents.service.EventScraper;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class CuratedEventsProvider implements EventScraper {

    private final CuratedDataLoader loader;

    public CuratedEventsProvider(CuratedDataLoader loader) {
        this.loader = loader;
    }

    @Override
    public String sourceName() {
        return "Curated Highlights";
    }

    @Override
    public List<Event> scrape(City city) {
        LocalDate today = CuratedDataLoader.today(city);
        return loader.eventsFor(city, today, today.plusDays(7));
    }
}
