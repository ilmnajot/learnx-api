# LearnX API

Backend for LearnX, daily 5-minute English practice for IT professionals.

Java 25, Spring Boot 4, MyBatis, PostgreSQL 17, Flyway.

## Run locally

Requires JDK 25 and Docker.

```bash
docker compose up -d --wait
./mvnw spring-boot:run
```

The API runs on <http://localhost:8080>. PostgreSQL runs on port 5433, so it doesn't clash with a server on 5432.

To use the Claude tutor, copy `.env.example` to `.env` and set `ANTHROPIC_API_KEY`. Without a key, the rule-based tutor runs instead and only catches a fixed list of common mistakes.

## Test

```bash
./mvnw test     # unit tests, no Docker needed
./mvnw verify   # also runs integration tests against real PostgreSQL (needs Docker)
```

## Structure

Code is organised by feature. Each feature owns its controller, service and MyBatis mapper.

| Package | Responsibility |
|---|---|
| `auth`, `user` | Registration, login, rotating refresh tokens, profile |
| `placement` | Placement test and level |
| `learning` | Topics, lessons, phrases, completion |
| `review` | Spaced repetition schedule and due phrases |
| `roleplay` | Speaking roleplay sessions |
| `ai` | `AiTutor` port with Claude, rule-based and fallback implementations, plus daily quota |
| `writing` | "Fix my message"; text is never stored |
| `game` | Game phrase pools and capped results |
| `video` | Short YouTube videos with practice phrases |
| `squad` | Squads, weekly incidents, team streak |
| `activity` | Streaks; listens to `PracticeRecorded` events |
| `today` | Read models for the home and progress screens |

Decisions worth keeping:

- **Events decouple features.** Lessons, reviews and games publish `PracticeRecorded`. Streaks and squads listen, in the same transaction, so a failure rolls back the whole action.
- **AI calls run outside transactions**, so a slow provider never holds a database connection.
- **Stateless and multi-instance safe.** Limits are enforced by atomic SQL or row locks, not process memory.
- **Refresh tokens rotate.** Only hashes are stored, and reusing an old token revokes all of that user's sessions. A token rotated in the last 10 seconds still works, because a page reload can cancel a refresh before the browser saves the new cookie.
- **Videos are only embedded.** They play through YouTube's official player and are never downloaded or re-hosted. Practice phrases are original, not copied from transcripts.

## Adding a video

Add a Flyway migration that inserts into `videos` and `phrases`, following `V3__videos.sql`. Before adding one:

1. Watch all of it and check it's suitable for a workplace audience.
2. Confirm that embedding is allowed. A `200` from this URL means it is:

   ```bash
   curl -s -o /dev/null -w '%{http_code}' 'https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=VIDEO_ID&format=json'
   ```

## API

All routes are under `/api/v1` and need a bearer token, except the `auth` routes. Errors use RFC 9457 problem details with a stable `code`.

| Method and path | Purpose |
|---|---|
| `POST /auth/register`, `/login`, `/refresh`, `/logout` | Session handling; the refresh token is an HttpOnly cookie |
| `POST /auth/verify-email`, `/forgot-password`, `/reset-password` | Email confirmation and password reset |
| `GET`, `PATCH /me`, `POST /me/resend-verification` | Profile, onboarding and confirmation email |
| `GET /placement/questions`, `POST /placement/submit` | Placement test |
| `GET /today`, `GET /progress` | Home and progress data |
| `GET /topics`, `GET /lessons/{id}`, `POST /lessons/{id}/complete` | Course |
| `POST /lessons/{id}/roleplay`, `POST /roleplay/{id}/turns` | Roleplay |
| `GET /reviews/due`, `POST /reviews/{phraseId}` | Spaced review |
| `GET /games/pool`, `POST /games/results` | Games |
| `POST /writing/improve` | Fix a message |
| `GET /videos`, `GET /videos/{id}`, `POST /videos/{id}/complete` | Videos |
| `GET`, `DELETE /squads/me`, `POST /squads`, `POST /squads/join` | Squads |

## Production

Set `SPRING_PROFILES_ACTIVE=prod` and the variables listed in `.env.example`. Startup fails if any are missing.

Serve the API over HTTPS. Health probes are at `/actuator/health/liveness` and `/actuator/health/readiness`.

**Email:** works with any SMTP provider, such as Resend or Brevo. Verify your sending domain with the provider, or confirmation emails will land in spam or be rejected. Without a mail server in development, emails and their links are written to the log.

**Abuse protection:**

- Per-IP limits on sign-up, login, password reset and AI requests live in `learnx.rate-limit`. Each can be overridden with a `LEARNX_LIMIT_*` variable.
- AI requires a confirmed email. A per-learner limit (`LEARNX_AI_DAILY_LIMIT`) and a limit across everyone (`LEARNX_AI_GLOBAL_DAILY_LIMIT`) also apply.
- Set a spending limit in the Anthropic console too, as the final safeguard.

**Reverse proxy:** client IPs come from `X-Forwarded-For`, trusted only when the request arrives from a private-network proxy, and read from the right so visitors can't fake them. If your proxy has a public address, set `server.tomcat.remoteip.internal-proxies`. Behind Cloudflare, also make sure the proxy passes on the real visitor address, or every visitor counts as one IP.

## Not built yet

- Telegram reminders and Telegram login, which need a bot token
- Payments
- Server-side speech recognition (the frontend uses the browser's)
