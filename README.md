# Trending News Alerts

Spring Boot app that polls a handful of major news RSS feeds, detects when a
new topic starts trending across multiple sources, emails you about it, and
shows the running list in a small web dashboard.

## How "trending" is defined

A **topic** is a multi-word proper-noun phrase (e.g. "donald trump",
"federal reserve", "white house") extracted from article titles and
descriptions. A topic is **trending** when it appears in feeds based in at
least `trending.minCountries` **different countries** (default 2) during a
poll cycle. Two outlets from the same country (e.g. BBC and the Guardian,
both UK) do not on their own make a topic trend — the same story must
surface across borders.

Each feed in `trending.feeds` is tagged with a country, so the bundled
sources span the UK, US, Qatar, Australia, Germany and France.

### Subset topics

When one trending topic appears as a **contiguous run of whole words**
inside another — e.g. "donald trump" within "donald trump tariffs" — they
are treated as a single topic. Their feed coverage is combined and the
broadest-reaching phrase is used as the label, so you get one alert and one
dashboard entry instead of several near-duplicates. This also holds across
polls: a topic that is a contiguous sub-phrase (or super-phrase) of one
already seen does not produce a fresh alert.

Matching is contiguous only — a topic whose words merely overlap another
without forming an adjacent phrase (e.g. "donald trump" vs. "donald john
trump") is kept as a separate topic.

## Lifecycle emails

In addition to trending-topic alerts, the app emails on three lifecycle
events (same recipient, same Resend setup):

- **App started** — sent once Spring finishes startup.
- **App stopped** — sent via `@PreDestroy` on SIGTERM. (If the container is
  SIGKILL'd, the email can't be sent.)
- **App pause detected** — a heartbeat thread wakes every 30s and watches
  for wall-clock gaps. Anything larger than 60s past the expected wake time
  is treated as a pause (host hibernated, container throttled) and the
  detected pause duration is emailed.

## Baseline behaviour

On startup the app runs **one full sweep** across every feed and records
every currently-trending topic as the *baseline*. Baseline topics are
**shown on the dashboard** (tagged `baseline`) so you can see what's
trending right now, but they **do not** trigger emails. Only topics that
cross the threshold on a **later** poll are considered "new" — those are
tagged `new`, listed on the dashboard, and emailed exactly once.

## Running locally

```bash
export RESEND_API_KEY=re_...
export ALERT_RECIPIENT=you@example.com
mvn spring-boot:run
```

Then visit http://localhost:8080. The dashboard auto-refreshes every 60s.

## Deploying to Railway

1. Push this repo to GitHub and create a new Railway project from it.
   Nixpacks detects Maven + Java 17 automatically (see `nixpacks.toml`).
2. Set these environment variables in the Railway service:
   - `RESEND_API_KEY` — from https://resend.com (free tier: 3k emails/month,
     100/day)
   - `ALERT_RECIPIENT` — the email address that should receive alerts
   - `RESEND_FROM` *(optional)* — defaults to
     `Trending Alerts <onboarding@resend.dev>`, which works without a verified
     domain. For production, verify your own domain in Resend and set this to
     something like `Alerts <alerts@yourdomain.com>`.
3. Deploy. Railway will expose the dashboard on the public URL it assigns.

### Why Resend (and not SMTP)?

Railway's container network frequently blocks outbound SMTP — port 25 is
blocked outright, and 465/587 are unreliable depending on the region your
service lands in. **Sending email via an HTTPS API is the reliable path on
Railway**, and Resend has the cleanest setup of the HTTP providers:

- One env var (`RESEND_API_KEY`) and one HTTPS POST — no SMTP config, no
  TLS handshake quirks.
- Test sending domain (`onboarding@resend.dev`) works immediately, so you
  can verify end-to-end before owning a domain.
- Free tier covers this use case comfortably (a couple of alerts per day).

Alternatives that also work well on Railway: **SendGrid** (similar HTTP API,
100 emails/day free) and **Postmark** (paid, but excellent deliverability).
Avoid Gmail SMTP for anything but personal experiments — app-password auth
is being phased out and deliverability degrades fast.

## Configuration

All settings in `src/main/resources/application.properties` are overridable
via env vars:

| Property                                          | Env var           | Default                                        |
|---------------------------------------------------|-------------------|------------------------------------------------|
| `trending.poll.intervalMinutes`                   | —                 | `15`                                           |
| `trending.poll.initialDelayAfterBaselineSeconds`  | —                 | `60` (first poll fires 1 min after baseline)   |
| `trending.minCountries`                           | —                 | `2` (distinct countries required to trend)     |
| `trending.feeds`                                  | —                 | 11 feeds (`name\|country\|url`) across 6 countries |
| `resend.apiKey`                                   | `RESEND_API_KEY`  | *(required for emails)*                        |
| `resend.from`                                     | `RESEND_FROM`     | `Trending Alerts <onboarding@resend.dev>`      |
| `alert.recipient`                                 | `ALERT_RECIPIENT` | *(required for emails)*                        |

The recurring poll is scheduled programmatically after the baseline sweep
completes, so the cycle is "baseline finishes → wait 1 min → first poll →
every 15 min thereafter" rather than aligned to wall-clock minutes.

## Development prompts

This app was built iteratively from the following prompts, verbatim and in
order:

1. Create a spring boot app that can email me when a new topic is trending
   in the news. Let me know the best way to send email on railway. It should
   also provide a user interface showing the new trending topicss since it
   starting running and a time when they started trending. Don't show or
   email about topics that were the trending when the app started but only
   topics which started trending after all news sources where polled once.
   You decide what is a trending topic. It just appear on multiple news
   feeds.
2. Are you running ?
3. Hrllo
4. Don't send an email but do show current trending topics on startup.
5. Start the first of the the 15 minute refresh cycle happening 1 min after
   the baseline
6. Send an email every time the app is started or stopped or it is detected
   the app was paused
7. Can you change trending topics to consist of two words or more.
8. Show times on Sydney time
9. Make the web interface display better on mobile
10. When se being the shutdown email include in the email how long the app
    was running for before being shutdown
11. Strip punctuation from trending topics so topics with and without
    function like apostrophes are treated the same
12. Trending topics must come from news sources in different countries to
    qualify as tending
13. Add all prompts to the the readme
14. Consider trending topics which are a subset of other trending topics
    the same topic
15. Do contiguous-phrase matching only
