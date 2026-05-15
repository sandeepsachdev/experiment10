package com.sydneyevents.service.scraper;

import com.sydneyevents.model.Event;
import com.sydneyevents.service.EventScraper;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Curated, always-available list of things on in Sydney.
 *
 * Combines two flavours:
 *  - Daily anchors: iconic, always-open attractions (Opera House, Bondi walk, etc.)
 *  - Day-of-week specific recurring events (markets, fireworks, free music, etc.)
 *
 * Acts as a guaranteed baseline so each day shows distinct, realistic content
 * even if every external scraper is blocked or rate-limited.
 *
 * Image URLs link to Unsplash (free to hotlink, attribution-friendly).
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
        LocalDate weekEnd = today.plusDays(7);
        List<Event> events = new ArrayList<>();

        addDailyAnchors(events, today, weekEnd);
        addRecurringByDayOfWeek(events, today, weekEnd);

        return events;
    }

    // ---------------------------------------------------------------------
    // Daily anchors — open every day across the next 7 days.
    // ---------------------------------------------------------------------
    private void addDailyAnchors(List<Event> events, LocalDate today, LocalDate weekEnd) {
        events.add(make(
                "Sydney Opera House — Guided Tour",
                "Step inside one of the world's most iconic buildings. Guided tours run hourly and reveal the architectural genius of Jørn Utzon, the engineering breakthroughs of its sail-shaped shells, and the stories behind world-premiere performances on its stages.",
                "https://images.unsplash.com/photo-1506973035872-a4ec16b8e8d9?auto=format&fit=crop&w=1200&q=80",
                "https://www.sydneyoperahouse.com/visit-us/tours",
                today, weekEnd, "Sydney Opera House, Bennelong Point", "Landmark"));

        events.add(make(
                "Bondi to Coogee Coastal Walk",
                "A six-kilometre cliff-top path linking Sydney's most famous beaches. Pass sandstone headlands, ocean pools at Bronte and Clovelly, and the rock-cut waters of Mahon Pool. Allow two hours one-way; free, open daily.",
                "https://images.unsplash.com/photo-1523428096881-5bd79d043006?auto=format&fit=crop&w=1200&q=80",
                "https://www.bondicoastalwalk.com/",
                today, weekEnd, "Bondi Beach to Coogee Beach", "Outdoors"));

        events.add(make(
                "BridgeClimb — Sydney Harbour Bridge",
                "Scale the arches of the Sydney Harbour Bridge at 134 metres above the water. Dawn, day, twilight and night climbs run every day with panoramic views over the Opera House, harbour and city skyline.",
                "https://images.unsplash.com/photo-1571983823232-07c00f4b3c80?auto=format&fit=crop&w=1200&q=80",
                "https://www.bridgeclimb.com/",
                today, weekEnd, "3 Cumberland Street, The Rocks", "Adventure"));

        events.add(make(
                "Taronga Zoo Sydney",
                "Home to more than 4,000 animals in harbour-front habitats. Don't miss the Seal Show, Free Flight Bird Show and Australian Walkabout. The Sky Safari cable car carries you in from the ferry wharf with one of the best views in Sydney.",
                "https://images.unsplash.com/photo-1549366021-9f761d450615?auto=format&fit=crop&w=1200&q=80",
                "https://taronga.org.au/sydney-zoo",
                today, weekEnd, "Bradleys Head Road, Mosman", "Family"));

        events.add(make(
                "Royal Botanic Garden Sydney",
                "30 hectares of harbour-side gardens beside the Opera House. Free to enter; daily Aboriginal Heritage Tours, the Calyx exhibition glasshouse, and Mrs Macquarie's Chair for the postcard view of the bridge framing the Opera House.",
                "https://images.unsplash.com/photo-1597007030739-6d2e7172ee6a?auto=format&fit=crop&w=1200&q=80",
                "https://www.botanicgardens.org.au/",
                today, weekEnd, "Mrs Macquaries Road, Sydney", "Outdoors"));

        events.add(make(
                "Art Gallery of New South Wales",
                "Free entry to one of Australia's leading public galleries, now spanning two architectural landmarks connected by the harbour-side art garden. See major Aboriginal and Torres Strait Islander art, European masters and rotating international exhibitions.",
                "https://images.unsplash.com/photo-1577720580479-7d839d829c73?auto=format&fit=crop&w=1200&q=80",
                "https://www.artgallery.nsw.gov.au/",
                today, weekEnd, "Art Gallery Road, The Domain", "Culture"));

        events.add(make(
                "Manly Ferry & Beach Day",
                "Catch the F1 ferry from Circular Quay through the heads of Sydney Harbour to Manly — a half-hour ride that locals call 'the world's best commute'. Surf lessons, ocean pools and The Corso are all a short stroll from the wharf.",
                "https://images.unsplash.com/photo-1551406483-3731d1997540?auto=format&fit=crop&w=1200&q=80",
                "https://transportnsw.info/",
                today, weekEnd, "Circular Quay to Manly Wharf", "Outdoors"));

        events.add(make(
                "Cockatoo Island UNESCO Site",
                "The largest island in Sydney Harbour, a UNESCO World Heritage convict site and former shipyard. Self-guided audio tour, art installations, and waterfront camping you can book by the night. Ferry from Circular Quay.",
                "https://images.unsplash.com/photo-1580418827493-f2b22c0a76cb?auto=format&fit=crop&w=1200&q=80",
                "https://www.cockatooisland.gov.au/",
                today, weekEnd, "Cockatoo Island, Sydney Harbour", "Culture"));

        events.add(make(
                "Sea Life Sydney Aquarium",
                "Walk through transparent underwater tunnels surrounded by sharks, rays and dugongs. The largest collection of Australian aquatic life in the world. Located on Darling Harbour next to WILD LIFE Sydney Zoo.",
                "https://images.unsplash.com/photo-1583212292454-1fe6229603b7?auto=format&fit=crop&w=1200&q=80",
                "https://www.sydneyaquarium.com.au/",
                today, weekEnd, "1-5 Wheat Road, Darling Harbour", "Family"));

        events.add(make(
                "Powerhouse Museum",
                "Sydney's science and design museum in a former tram power station. Big-name design exhibitions, the Boulton & Watt steam engine and ZOEEE, the Strasburg model railway. Open daily 10am-5pm.",
                "https://images.unsplash.com/photo-1518998053901-5348d3961a04?auto=format&fit=crop&w=1200&q=80",
                "https://powerhouse.com.au/",
                today, weekEnd, "500 Harris Street, Ultimo", "Culture"));

        events.add(make(
                "Watsons Bay & The Gap at Sunset",
                "Sandstone cliffs at South Head and a famous fish-and-chips beer garden at Doyles. Catch the late-afternoon ferry from Circular Quay for golden-hour photos at The Gap looking south down the coast.",
                "https://images.unsplash.com/photo-1602002418082-a4443e081dd1?auto=format&fit=crop&w=1200&q=80",
                "https://www.harbourlife.com.au/",
                today, weekEnd, "Watsons Bay", "Outdoors"));

        events.add(make(
                "Queen Victoria Building (QVB) High Tea",
                "Heritage Romanesque shopping arcade in the heart of the CBD. Take afternoon tea at the Tea Room beneath stained-glass domes, then browse three levels of boutiques and the Royal Clock's hourly tableau.",
                "https://images.unsplash.com/photo-1601762603339-fd61e28b698a?auto=format&fit=crop&w=1200&q=80",
                "https://www.qvb.com.au/",
                today, weekEnd, "455 George Street, Sydney", "Food & Drink"));
    }

    // ---------------------------------------------------------------------
    // Day-of-week recurring events — only show on the relevant weekday(s).
    // ---------------------------------------------------------------------
    private void addRecurringByDayOfWeek(List<Event> events, LocalDate today, LocalDate weekEnd) {

        // Monday
        addOn(events, today, weekEnd, DayOfWeek.MONDAY, make(
                "Comedy Lounge at the Roxbury, Glebe",
                "Sydney's longest-running Monday-night stand-up. New material night with established headliners testing routines — eight comics, a $10 cover and a cold schooner in hand.",
                "https://images.unsplash.com/photo-1527224538127-2104bb71c51b?auto=format&fit=crop&w=1200&q=80",
                "https://www.theroxbury.com.au/",
                null, null, "182 St Johns Road, Glebe", "Comedy"));

        addOn(events, today, weekEnd, DayOfWeek.MONDAY, make(
                "Free Yoga at Bondi Beach",
                "Donation-based 60-minute vinyasa flow on the south end of Bondi Beach as the sun sets. BYO mat or towel; all levels welcome. Run by Bondi Yoga every Monday evening, weather permitting.",
                "https://images.unsplash.com/photo-1545205597-3d9d02c29597?auto=format&fit=crop&w=1200&q=80",
                "https://www.bondiyoga.com/",
                null, null, "Bondi Beach (south end)", "Wellness"));

        // Tuesday
        addOn(events, today, weekEnd, DayOfWeek.TUESDAY, make(
                "Tuesday Night Swing at Hyde Park Inn",
                "Free swing dance lessons at 7pm followed by a social dance to a live four-piece. Beginners welcome, no partner required. Sydney's longest-running weekly swing night.",
                "https://images.unsplash.com/photo-1535525153412-5a42439a210d?auto=format&fit=crop&w=1200&q=80",
                "https://www.swingpit.com.au/",
                null, null, "271 Elizabeth Street, Sydney", "Music"));

        addOn(events, today, weekEnd, DayOfWeek.TUESDAY, make(
                "Cheap Tuesday at Dendy Newtown",
                "Indie cinema night — all sessions $13 every Tuesday at one of Sydney's best-loved arthouse cinemas. Pre-show drinks at the in-house bar.",
                "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=1200&q=80",
                "https://www.dendy.com.au/",
                null, null, "261-263 King Street, Newtown", "Film"));

        // Wednesday
        addOn(events, today, weekEnd, DayOfWeek.WEDNESDAY, make(
                "Twilight at Taronga (Wednesday Late Opening)",
                "Taronga Zoo stays open until 7:30pm on Wednesdays in the warmer months — see nocturnal animals waking up, with sunset views of the city skyline from the Sky Safari.",
                "https://images.unsplash.com/photo-1564349683136-77e08dba1ef7?auto=format&fit=crop&w=1200&q=80",
                "https://taronga.org.au/sydney-zoo/whats-on",
                null, null, "Taronga Zoo, Mosman", "Family"));

        addOn(events, today, weekEnd, DayOfWeek.WEDNESDAY, make(
                "Sydney Symphony Open Rehearsal",
                "Watch the Sydney Symphony Orchestra rehearse for an upcoming concert in the Opera House Concert Hall. Tickets are a fraction of the regular price; check the season schedule.",
                "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?auto=format&fit=crop&w=1200&q=80",
                "https://www.sydneysymphony.com/",
                null, null, "Sydney Opera House Concert Hall", "Music"));

        // Thursday
        addOn(events, today, weekEnd, DayOfWeek.THURSDAY, make(
                "Art Gallery of NSW — Art After Hours",
                "Free Thursday late-night opening until 10pm, with celebrity guest talks, live music, films and pop-up bars across the gallery's two buildings.",
                "https://images.unsplash.com/photo-1518998053901-5348d3961a04?auto=format&fit=crop&w=1200&q=80",
                "https://www.artgallery.nsw.gov.au/art-after-hours/",
                null, null, "Art Gallery Road, The Domain", "Culture"));

        addOn(events, today, weekEnd, DayOfWeek.THURSDAY, make(
                "Late-Night Shopping in the CBD",
                "Sydney's department stores and CBD boutiques stay open until 9pm every Thursday. Pitt Street Mall, the Strand Arcade and the QVB host pop-up performers and food stalls.",
                "https://images.unsplash.com/photo-1519567241046-7f570eee3ce6?auto=format&fit=crop&w=1200&q=80",
                "https://www.sydneycbd.com.au/",
                null, null, "Pitt Street Mall, Sydney", "Shopping"));

        // Friday
        addOn(events, today, weekEnd, DayOfWeek.FRIDAY, make(
                "Carriageworks Twilight Food Market",
                "Friday-night farmers' and street-food market in the cathedral-sized industrial halls of Carriageworks, Redfern. 70+ producers, woodfired pizza, natural wine bars and live DJs from 4pm.",
                "https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=1200&q=80",
                "https://carriageworks.com.au/",
                null, null, "245 Wilson Street, Eveleigh", "Markets"));

        addOn(events, today, weekEnd, DayOfWeek.FRIDAY, make(
                "Friday Night Live at the MCA",
                "Free guided exhibition tours and live music at the Museum of Contemporary Art Australia, with sunset cocktails on the rooftop sculpture terrace overlooking the Opera House.",
                "https://images.unsplash.com/photo-1531058020387-3be344556be6?auto=format&fit=crop&w=1200&q=80",
                "https://www.mca.com.au/",
                null, null, "140 George Street, The Rocks", "Culture"));

        // Saturday
        addOn(events, today, weekEnd, DayOfWeek.SATURDAY, make(
                "The Rocks Markets",
                "Every Saturday and Sunday under the Harbour Bridge, more than 150 stalls of local artisans, designers and street food. Live music and pop-up bars from late morning. Free entry, kid-friendly.",
                "https://images.unsplash.com/photo-1555529669-e69e7aa0ba9a?auto=format&fit=crop&w=1200&q=80",
                "https://www.therocks.com/things-to-do/the-rocks-markets",
                null, null, "Playfair Street & George Street, The Rocks", "Markets"));

        addOn(events, today, weekEnd, DayOfWeek.SATURDAY, make(
                "Darling Harbour Fireworks",
                "Free 8:30pm fireworks every Saturday night over Cockle Bay. Grab a spot on the boardwalk by Tumbalong Park or watch from the Pyrmont Bridge for an uninterrupted view.",
                "https://images.unsplash.com/photo-1530541930197-ff16ac917b0e?auto=format&fit=crop&w=1200&q=80",
                "https://www.darlingharbour.com/",
                null, null, "Darling Harbour", "Free"));

        addOn(events, today, weekEnd, DayOfWeek.SATURDAY, make(
                "Eveleigh Farmers' Market",
                "75 stallholders selling food grown, raised and made within a few hours of Sydney. 8am-1pm every Saturday under the heritage steel of Carriageworks. Coffee carts, woodfired bagels and live folk.",
                "https://images.unsplash.com/photo-1488459716781-31db52582fe9?auto=format&fit=crop&w=1200&q=80",
                "https://carriageworks.com.au/events/eveleigh-farmers-market/",
                null, null, "243 Wilson Street, Eveleigh", "Markets"));

        addOn(events, today, weekEnd, DayOfWeek.SATURDAY, make(
                "Bondi Farmers Market",
                "Every Saturday morning at Bondi Public School, 9am-1pm. Top NSW producers, breakfast paddocks and a kids' play corner. A short walk from the beach for a post-market swim.",
                "https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&w=1200&q=80",
                "https://bondifarmersmarket.com.au/",
                null, null, "Bondi Public School, Campbell Parade", "Markets"));

        // Sunday
        addOn(events, today, weekEnd, DayOfWeek.SUNDAY, make(
                "Glebe Markets",
                "Sydney's beloved Sunday market under the plane trees of Glebe Public School. Vintage clothing, vinyl, plants and 30+ international food stalls. 10am-4pm every Sunday.",
                "https://images.unsplash.com/photo-1523413651479-597eb2da0ad6?auto=format&fit=crop&w=1200&q=80",
                "https://www.glebemarkets.com.au/",
                null, null, "Glebe Public School, 183A Glebe Point Road", "Markets"));

        addOn(events, today, weekEnd, DayOfWeek.SUNDAY, make(
                "Bondi Markets",
                "Every Sunday on the lawns of Bondi Beach Public School, 9am-4pm. Curated emerging Australian designers, vintage fashion, plus a separate farmers' produce section. Beach is two minutes' walk away.",
                "https://images.unsplash.com/photo-1528698827591-e19ccd7bc23d?auto=format&fit=crop&w=1200&q=80",
                "https://bondimarkets.com.au/",
                null, null, "Bondi Beach Public School, Campbell Parade", "Markets"));

        addOn(events, today, weekEnd, DayOfWeek.SUNDAY, make(
                "Sunday Sessions at the Opera Bar",
                "Live local DJs and bands every Sunday afternoon on the Opera Bar's harbourside terrace, framed by the Sydney Harbour Bridge. Free entry, food and drink from 11am.",
                "https://images.unsplash.com/photo-1559925393-8be0ec4767c8?auto=format&fit=crop&w=1200&q=80",
                "https://operabar.com.au/",
                null, null, "Sydney Opera House lower concourse", "Music"));
    }

    private void addOn(List<Event> sink, LocalDate from, LocalDate to,
                       DayOfWeek dow, Event template) {
        LocalDate d = from;
        while (!d.isAfter(to)) {
            if (d.getDayOfWeek() == dow) {
                Event e = copy(template);
                e.setStartDate(d);
                e.setEndDate(d);
                sink.add(e);
            }
            d = d.plusDays(1);
        }
    }

    private Event copy(Event src) {
        Event e = new Event();
        e.setTitle(src.getTitle());
        e.setDescription(src.getDescription());
        e.setImageUrl(src.getImageUrl());
        e.setUrl(src.getUrl());
        e.setVenue(src.getVenue());
        e.setCategory(src.getCategory());
        e.setSource(src.getSource());
        return e;
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
