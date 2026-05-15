package com.sydneyevents.service.scraper;

import com.sydneyevents.model.Event;
import com.sydneyevents.service.EventScraper;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Curated, always-available list of iconic things on in Sydney.
 * Acts as a guaranteed fallback when scrapers are blocked.
 *
 * Images are from Unsplash (free to use, attribution-friendly hotlinks).
 */
@Component
public class CuratedEventsProvider implements EventScraper {

    private static final ZoneId SYDNEY = ZoneId.of("Australia/Sydney");

    @Override
    public String sourceName() {
        return "Curated Sydney Highlights";
    }

    @Override
    public List<Event> scrape() {
        LocalDate today = LocalDate.now(SYDNEY);
        List<Event> events = new ArrayList<>();

        // Daily highlights spread across the next 7 days
        events.add(make(
                "Sydney Opera House Tour",
                "Step inside one of the world's most iconic buildings. Guided tours run hourly and reveal the architectural genius of Jørn Utzon, the engineering breakthroughs of its sail-shaped shells, and the stories behind world-premiere performances on its stages.",
                "https://images.unsplash.com/photo-1506973035872-a4ec16b8e8d9?auto=format&fit=crop&w=1200&q=80",
                "https://www.sydneyoperahouse.com/visit-us/tours",
                today, today.plusDays(7),
                "Sydney Opera House, Bennelong Point",
                "Landmark"));

        events.add(make(
                "Bondi to Coogee Coastal Walk",
                "A six-kilometre cliff-top path linking Sydney's most famous beaches. Pass sandstone headlands, ocean pools at Bronte and Clovelly, and the rock-cut waters of Mahon Pool. Allow two hours one-way; free, open daily.",
                "https://images.unsplash.com/photo-1523428096881-5bd79d043006?auto=format&fit=crop&w=1200&q=80",
                "https://www.bondicoastalwalk.com/",
                today, today.plusDays(7),
                "Bondi Beach to Coogee Beach",
                "Outdoors"));

        events.add(make(
                "BridgeClimb Sydney Harbour Bridge",
                "Scale the arches of the Sydney Harbour Bridge at 134 metres above the water. Dawn, day, twilight and night climbs run every day with panoramic views over the Opera House, harbour and city skyline.",
                "https://images.unsplash.com/photo-1571983823232-07c00f4b3c80?auto=format&fit=crop&w=1200&q=80",
                "https://www.bridgeclimb.com/",
                today, today.plusDays(7),
                "3 Cumberland Street, The Rocks",
                "Adventure"));

        events.add(make(
                "Taronga Zoo Sydney",
                "Home to more than 4,000 animals in harbour-front habitats. Don't miss the Seal Show, Free Flight Bird Show and Australian Walkabout. The Sky Safari cable car carries you in from the ferry wharf with one of the best views in Sydney.",
                "https://images.unsplash.com/photo-1549366021-9f761d450615?auto=format&fit=crop&w=1200&q=80",
                "https://taronga.org.au/sydney-zoo",
                today, today.plusDays(7),
                "Bradleys Head Road, Mosman",
                "Family"));

        events.add(make(
                "Royal Botanic Garden Sydney",
                "30 hectares of harbour-side gardens beside the Opera House. Free to enter; daily Aboriginal Heritage Tours, the Calyx exhibition glasshouse, and Mrs Macquarie's Chair for the postcard view of the bridge framing the Opera House.",
                "https://images.unsplash.com/photo-1597007030739-6d2e7172ee6a?auto=format&fit=crop&w=1200&q=80",
                "https://www.botanicgardens.org.au/",
                today, today.plusDays(7),
                "Mrs Macquaries Road, Sydney",
                "Outdoors"));

        events.add(make(
                "Art Gallery of New South Wales",
                "Free entry to one of Australia's leading public galleries, now spanning two architectural landmarks connected by the harbour-side art garden. See major Aboriginal and Torres Strait Islander art, European masters and rotating international exhibitions.",
                "https://images.unsplash.com/photo-1577720580479-7d839d829c73?auto=format&fit=crop&w=1200&q=80",
                "https://www.artgallery.nsw.gov.au/",
                today, today.plusDays(7),
                "Art Gallery Road, The Domain",
                "Culture"));

        events.add(make(
                "Manly Ferry & Beach Day",
                "Catch the F1 ferry from Circular Quay through the heads of Sydney Harbour to Manly — a half-hour ride that locals call 'the world's best commute'. Surf lessons, ocean pools and The Corso are all a short stroll from the wharf.",
                "https://images.unsplash.com/photo-1551406483-3731d1997540?auto=format&fit=crop&w=1200&q=80",
                "https://transportnsw.info/",
                today, today.plusDays(7),
                "Circular Quay to Manly Wharf",
                "Outdoors"));

        events.add(make(
                "Queen Victoria Building (QVB) High Tea",
                "Heritage Romanesque shopping arcade in the heart of the CBD. Take afternoon tea at the Tea Room beneath stained-glass domes, then browse three levels of boutiques and the Royal Clock's hourly tableau.",
                "https://images.unsplash.com/photo-1601762603339-fd61e28b698a?auto=format&fit=crop&w=1200&q=80",
                "https://www.qvb.com.au/",
                today, today.plusDays(7),
                "455 George Street, Sydney",
                "Food & Drink"));

        events.add(make(
                "Sydney Fish Market — Sunrise Tour",
                "The Southern Hemisphere's largest working fish market. Behind-the-scenes tours start at 6:40am and take you onto the auction floor where the day's catch is sold by Dutch clock. Stay for sashimi breakfast on the wharf.",
                "https://images.unsplash.com/photo-1559339352-11d035aa65de?auto=format&fit=crop&w=1200&q=80",
                "https://www.sydneyfishmarket.com.au/",
                today, today.plusDays(7),
                "Bank Street, Pyrmont",
                "Food & Drink"));

        events.add(make(
                "Cockatoo Island UNESCO Site",
                "The largest island in Sydney Harbour, a UNESCO World Heritage convict site and former shipyard. Self-guided audio tour, art installations, and waterfront camping you can book by the night. Ferry from Circular Quay.",
                "https://images.unsplash.com/photo-1580418827493-f2b22c0a76cb?auto=format&fit=crop&w=1200&q=80",
                "https://www.cockatooisland.gov.au/",
                today, today.plusDays(7),
                "Cockatoo Island, Sydney Harbour",
                "Culture"));

        events.add(make(
                "The Rocks Markets",
                "Every Saturday and Sunday under the Harbour Bridge, more than 150 stalls of local artisans, designers and street food. Live music and pop-up bars from late morning. Free entry, kid-friendly.",
                "https://images.unsplash.com/photo-1555529669-e69e7aa0ba9a?auto=format&fit=crop&w=1200&q=80",
                "https://www.therocks.com/things-to-do/the-rocks-markets",
                nextWeekday(today, java.time.DayOfWeek.SATURDAY),
                nextWeekday(today, java.time.DayOfWeek.SUNDAY),
                "Playfair Street & George Street, The Rocks",
                "Markets"));

        events.add(make(
                "Darling Harbour Fireworks",
                "Free 8:30pm fireworks every Saturday night over Cockle Bay. Grab a spot on the boardwalk by Tumbalong Park or watch from the Pyrmont Bridge for an uninterrupted view.",
                "https://images.unsplash.com/photo-1530541930197-ff16ac917b0e?auto=format&fit=crop&w=1200&q=80",
                "https://www.darlingharbour.com/",
                nextWeekday(today, java.time.DayOfWeek.SATURDAY),
                nextWeekday(today, java.time.DayOfWeek.SATURDAY),
                "Darling Harbour",
                "Free"));

        events.add(make(
                "Sea Life Sydney Aquarium",
                "Walk through transparent underwater tunnels surrounded by sharks, rays and dugongs. The largest collection of Australian aquatic life in the world. Located on Darling Harbour next to WILD LIFE Sydney Zoo.",
                "https://images.unsplash.com/photo-1583212292454-1fe6229603b7?auto=format&fit=crop&w=1200&q=80",
                "https://www.sydneyaquarium.com.au/",
                today, today.plusDays(7),
                "1-5 Wheat Road, Darling Harbour",
                "Family"));

        events.add(make(
                "Powerhouse Museum",
                "Sydney's science and design museum in a former tram power station. Big-name design exhibitions, the Boulton & Watt steam engine and ZOEEE, the Strasburg model railway. Open daily 10am-5pm.",
                "https://images.unsplash.com/photo-1518998053901-5348d3961a04?auto=format&fit=crop&w=1200&q=80",
                "https://powerhouse.com.au/",
                today, today.plusDays(7),
                "500 Harris Street, Ultimo",
                "Culture"));

        events.add(make(
                "Sunset at Watsons Bay",
                "Sandstone cliffs at South Head and a famous fish-and-chips beer garden at Doyles. Catch the late-afternoon ferry from Circular Quay for golden-hour photos at The Gap looking south down the coast.",
                "https://images.unsplash.com/photo-1602002418082-a4443e081dd1?auto=format&fit=crop&w=1200&q=80",
                "https://www.harbourlife.com.au/",
                today, today.plusDays(7),
                "Watsons Bay",
                "Outdoors"));

        return events;
    }

    private LocalDate nextWeekday(LocalDate from, java.time.DayOfWeek dow) {
        LocalDate d = from;
        while (d.getDayOfWeek() != dow) d = d.plusDays(1);
        if (d.isAfter(from.plusDays(7))) return from;
        return d;
    }

    private Event make(String title, String desc, String img, String url,
                       LocalDate start, LocalDate end, String venue, String category) {
        Event e = new Event();
        e.setTitle(title);
        e.setDescription(desc);
        e.setImageUrl(img);
        e.setUrl(url);
        e.setStartDate(start);
        e.setEndDate(end);
        e.setVenue(venue);
        e.setCategory(category);
        e.setSource(sourceName());
        return e;
    }
}
