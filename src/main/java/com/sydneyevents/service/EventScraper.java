package com.sydneyevents.service;

import com.sydneyevents.model.Event;

import java.util.List;

public interface EventScraper {
    String sourceName();
    List<Event> scrape();
}
