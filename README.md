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
