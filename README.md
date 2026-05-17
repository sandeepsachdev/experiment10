# Trending News Alerts

Spring Boot app that polls a handful of major news RSS feeds, detects when a
new topic starts trending across multiple sources, emails you about it, and
shows the running list in a small web dashboard.

## How "trending" is defined

A **topic** is a proper-noun phrase (e.g. "donald trump", "gaza", "federal
reserve") extracted from article titles and descriptions. A topic is
**trending** when it appears in articles from at least `trending.minSources`
distinct feeds (default 2) during a poll cycle.

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
| `trending.minSources`                             | —                 | `2`                                            |
| `trending.feeds`                                  | —                 | BBC, Guardian, NPR, Al Jazeera, CBS, Sky, NYT, CNBC |
| `resend.apiKey`                                   | `RESEND_API_KEY`  | *(required for emails)*                        |
| `resend.from`                                     | `RESEND_FROM`     | `Trending Alerts <onboarding@resend.dev>`      |
| `alert.recipient`                                 | `ALERT_RECIPIENT` | *(required for emails)*                        |

The recurring poll is scheduled programmatically after the baseline sweep
completes, so the cycle is "baseline finishes → wait 1 min → first poll →
every 15 min thereafter" rather than aligned to wall-clock minutes.
