package com.sydneyevents.service;

import com.sydneyevents.model.City;
import com.sydneyevents.model.Event;

import java.util.List;

public interface EventScraper {
    String sourceName();

    /** Whether this scraper produces results for the given city. */
    default boolean supports(City city) {
        return true;
    }

    List<Event> scrape(City city);
}
