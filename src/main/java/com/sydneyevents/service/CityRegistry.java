package com.sydneyevents.service;

import com.sydneyevents.model.City;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class CityRegistry {

    public static final String DEFAULT_SLUG = "sydney";

    private final Map<String, City> cities = new LinkedHashMap<>();

    public CityRegistry() {
        // Capital cities
        add(new City("sydney", "Sydney", "NSW",
                -33.8688, 151.2093, "Australia/Sydney",
                "https://images.unsplash.com/photo-1506973035872-a4ec16b8e8d9?auto=format&fit=crop&w=1800&q=80"));
        add(new City("melbourne", "Melbourne", "VIC",
                -37.8136, 144.9631, "Australia/Melbourne",
                "https://images.unsplash.com/photo-1545044846-351ba102b6d5?auto=format&fit=crop&w=1800&q=80"));
        add(new City("brisbane", "Brisbane", "QLD",
                -27.4698, 153.0251, "Australia/Brisbane",
                "https://images.unsplash.com/photo-1566734904496-9309bb1798ae?auto=format&fit=crop&w=1800&q=80"));
        add(new City("perth", "Perth", "WA",
                -31.9523, 115.8613, "Australia/Perth",
                "https://images.unsplash.com/photo-1573935448851-fa5b8b03cd44?auto=format&fit=crop&w=1800&q=80"));
        add(new City("adelaide", "Adelaide", "SA",
                -34.9285, 138.6007, "Australia/Adelaide",
                "https://images.unsplash.com/photo-1566408669374-5a6d5dca1ef5?auto=format&fit=crop&w=1800&q=80"));
        add(new City("hobart", "Hobart", "TAS",
                -42.8821, 147.3272, "Australia/Hobart",
                "https://images.unsplash.com/photo-1589330273594-fade1ee91647?auto=format&fit=crop&w=1800&q=80"));
        add(new City("darwin", "Darwin", "NT",
                -12.4634, 130.8456, "Australia/Darwin",
                "https://images.unsplash.com/photo-1591538834179-c3ed4a0a8b1b?auto=format&fit=crop&w=1800&q=80"));
        add(new City("canberra", "Canberra", "ACT",
                -35.2809, 149.1300, "Australia/Sydney",
                "https://images.unsplash.com/photo-1602166242292-91d0b6f24ce6?auto=format&fit=crop&w=1800&q=80"));

        // Regional centres
        add(new City("gold-coast", "Gold Coast", "QLD",
                -28.0167, 153.4000, "Australia/Brisbane",
                "https://images.unsplash.com/photo-1493606278519-11aa9f86e40a?auto=format&fit=crop&w=1800&q=80"));
        add(new City("newcastle", "Newcastle", "NSW",
                -32.9283, 151.7817, "Australia/Sydney",
                "https://images.unsplash.com/photo-1599831004099-fa7f5a3a3a87?auto=format&fit=crop&w=1800&q=80"));
        add(new City("wollongong", "Wollongong", "NSW",
                -34.4278, 150.8931, "Australia/Sydney",
                "https://images.unsplash.com/photo-1597566954186-3f3d51e09a16?auto=format&fit=crop&w=1800&q=80"));
        add(new City("geelong", "Geelong", "VIC",
                -38.1499, 144.3617, "Australia/Melbourne",
                "https://images.unsplash.com/photo-1601981258639-08d8a8c46e7b?auto=format&fit=crop&w=1800&q=80"));
        add(new City("sunshine-coast", "Sunshine Coast", "QLD",
                -26.6500, 153.0667, "Australia/Brisbane",
                "https://images.unsplash.com/photo-1572206912757-5a78ac39bf9c?auto=format&fit=crop&w=1800&q=80"));
        add(new City("cairns", "Cairns", "QLD",
                -16.9186, 145.7781, "Australia/Brisbane",
                "https://images.unsplash.com/photo-1542362567-b07e54358753?auto=format&fit=crop&w=1800&q=80"));
        add(new City("townsville", "Townsville", "QLD",
                -19.2589, 146.8169, "Australia/Brisbane",
                "https://images.unsplash.com/photo-1605125927676-c33b88aaf2c1?auto=format&fit=crop&w=1800&q=80"));
        add(new City("ballarat", "Ballarat", "VIC",
                -37.5622, 143.8503, "Australia/Melbourne",
                "https://images.unsplash.com/photo-1611605645802-c21be743c321?auto=format&fit=crop&w=1800&q=80"));
        add(new City("bendigo", "Bendigo", "VIC",
                -36.7570, 144.2794, "Australia/Melbourne",
                "https://images.unsplash.com/photo-1568797629192-89d7da82c1f4?auto=format&fit=crop&w=1800&q=80"));
        add(new City("launceston", "Launceston", "TAS",
                -41.4332, 147.1441, "Australia/Hobart",
                "https://images.unsplash.com/photo-1571406761758-9a3eed5338ef?auto=format&fit=crop&w=1800&q=80"));
    }

    private void add(City c) {
        cities.put(c.slug(), c);
    }

    public City defaultCity() {
        return cities.get(DEFAULT_SLUG);
    }

    public City get(String slug) {
        if (slug == null) return defaultCity();
        City c = cities.get(slug.toLowerCase());
        return c != null ? c : defaultCity();
    }

    public List<City> all() {
        return List.copyOf(cities.values());
    }

    public Map<String, City> asMap() {
        return Collections.unmodifiableMap(cities);
    }
}
