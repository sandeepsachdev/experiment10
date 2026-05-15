# What's On in Sydney

A Spring Boot web app that shows what's on in Sydney each day for the upcoming 7 days, with photos, descriptions, and a daily weather forecast.

## Features

- 7-day rolling view of Sydney events with images and rich descriptions
- Daily weather forecast (high/low, conditions, rain probability, UV, wind, sunrise/sunset)
- Aggregates from multiple sources with deduplication
- Hourly caching to keep the site fast and reduce load on upstream sources
- Curated fallback ensures content is always available

## Sources

- **Weather**: [Open-Meteo](https://open-meteo.com/) — free, no API key required
- **Events**:
  - City of Sydney's *What's On* listing
  - Schema.org / JSON-LD event blocks from `sydney.com` and `sydneyoperahouse.com`
  - A curated list of iconic Sydney attractions and recurring activities

## Run locally

```bash
mvn spring-boot:run
```

Open http://localhost:8080

## Build & run with Docker

```bash
docker build -t sydney-events .
docker run -p 8080:8080 sydney-events
```

## Deploy to Render

This repo includes a `render.yaml` blueprint so you can deploy with one click:

1. Push the repo to GitHub.
2. In Render, choose **New → Blueprint** and point it at this repo.
3. Render detects `render.yaml`, builds the Docker image, and starts the web service.

Or set it up manually:

1. Create a **New → Web Service** in Render and connect your repo.
2. Set **Runtime** to **Docker** (Render will use the `Dockerfile` at the repo root).
3. Set **Health Check Path** to `/health`.
4. Pick the **Free** plan (or higher). No environment variables are required.

Render injects `PORT` automatically; the container reads it via `server.port`.

## Configuration

`src/main/resources/application.yml`:

- `app.weather.latitude` / `app.weather.longitude` / `app.weather.timezone` — defaults to Sydney
- `app.scrape.user-agent` — sent on all scraping requests
- `app.scrape.timeout-ms` — per-request timeout
- `spring.cache.caffeine.spec` — cache TTL (default 1h)

## Stack

- Java 21, Spring Boot 3.3, Thymeleaf
- Jsoup for HTML scraping
- Caffeine for in-memory caching
- Docker, deployable to Render

## Prompts used to build this app

This project was built with Claude Code. The full prompt history is kept here
so the codebase remains reproducible from intent. Append new prompts to the
bottom of the list as the project evolves.

### 1. Initial generation

> Create a spring boot app that can deploy easily to render with a docker file.
> The apps should scan sources to find what's in in Sydney each day in the
> upcoming 7 days. There should be lots of pictures and description of the
> events and also weather each day

Produced the initial scaffold: Spring Boot 3.3 / Java 21 app, multi-stage
`Dockerfile`, `render.yaml` blueprint, Open-Meteo weather integration, three
event scrapers (City of Sydney *What's On*, JSON-LD on `sydney.com` and
`sydneyoperahouse.com`), a curated fallback list, Thymeleaf UI with hero
image, day navigation, weather cards and an event-card grid.

### 2. "Only the curated highlights are showing"

> Only the curated highlights are showing

The live scrapers were being bot-blocked and the 12 always-open anchor
attractions were filling every day's slot budget, so each day looked
identical. Fixes:

- Added `CitySydneyOpenDataScraper` (Opendatasoft JSON API — no HTML parsing)
- Added `EventbriteScraper` (parses schema.org JSON-LD from Eventbrite Sydney)
- Switched all scrapers to a realistic Chrome `User-Agent` and
  `Accept-Language` header
- Expanded `CuratedEventsProvider` with 17 day-of-week-specific recurring
  events (Sat: Rocks Markets, Bondi Farmers, Darling Harbour fireworks;
  Sun: Glebe / Bondi Markets, Opera Bar Sunday Sessions; Mon: comedy +
  free yoga; Tue: swing + cheap cinema; Wed: twilight zoo + symphony
  rehearsal; Thu: Art After Hours + late-night shopping; Fri: Carriageworks
  Twilight Market + MCA Friday Live)
- `HomeController` now sorts each day's events by run-length so single-day
  recurring events surface before always-open anchors; per-day cap lifted
  to 12

### 3. Document the prompt history

> Add prompt to generate this app to the readme. Add future prompts as well

Added this section.
