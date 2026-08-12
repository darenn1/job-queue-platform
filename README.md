# Job Queue Platform

An asynchronous job processing system built in Java and Spring Boot — durable
ingestion via Kafka, a Redis-backed worker queue, Postgres persistence, JWT
auth, and full observability with Prometheus/Grafana. Built by spanning JVM internals through load-test-driven GC tuning.

---

### Data Flows

| Flow | Transport | Description |
|---|---|---|
| Client → Spring Boot | REST (HTTP), JWT | `POST /jobs` submits a job; auth via short-lived Bearer access token, verified without a DB call |
| Client → Spring Boot | REST (HTTP), refresh token | `POST /auth/refresh` exchanges a long-lived refresh token for a new access token; each exchange rotates the refresh token — the old one is revoked and cannot be reused |
| Spring Boot → Kafka | Kafka producer, JSON | Job published to `jobs` topic (3 partitions), message key = job ID |
| Kafka → Spring Boot | `@KafkaListener`, manual ack | Consumer group `workers`; offset committed only after the job is safely enqueued in Redis |
| Spring Boot → Postgres | Spring Data JPA | Job lookup/persistence, status updates, idempotency-key checks |
| Spring Boot → Redis (queue) | `LPUSH` / `BRPOP` | Durable Kafka message handed off to a fast, ephemeral worker queue |
| Spring Boot → Redis (cache) | `@Cacheable` / `@CacheEvict` | `GET /jobs/{id}` cached for 30s, invalidated on every status transition |
| Spring Boot → Redis (rate limit) | Sorted Set, sliding window | Per-API-key throttling; over-limit requests get `429` + `Retry-After` |
| Spring Boot → Prometheus | Micrometer, `/actuator/prometheus` | Custom metrics: submitted/completed/failed counts, processing duration, queue depth |
| Prometheus → Grafana | Scrape, 15s interval | Dashboards: throughput, p99 latency, queue depth over time |

---

## Getting Started

### Prerequisites

| Tool | Min version | Purpose |
|---|---|---|
| JDK | 21 | Application runtime |
| Maven | 3.9+ (or Maven Wrapper, if included) | Build tool — `pom.xml` |
| Docker | 24.0+ | Container runtime |
| Docker Compose | 2.20+ | Orchestrates Postgres, Redis, Kafka (KRaft mode), Prometheus, Grafana |
| k6 | latest | Load testing (local dev only) |

### Quick Start

```bash
# 1. Move into project folder
cd job-queue-platform-refined

# 2. Copy env template to project root
cp .env.example .env

# 3. Start infra: Postgres, Redis, Kafka, Prometheus, Grafana
docker compose up -d

# 4. Run Flyway migrations (creates users, jobs, refresh_tokens tables, indexes)
./mvnw flyway:migrate

# 5. Build and run the app
./mvnw spring-boot:run

# 6. Register and log in — response includes BOTH an access token and a refresh token
curl -X POST localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","email":"alice@example.com","password":"password123"}'
# -> { "accessToken": "...", "refreshToken": "...", "username": "alice", "role": "USER" }

# 7. Submit a job with the access token
curl -X POST localhost:8080/jobs -H "Authorization: Bearer <accessToken>" \
  -d '{"type":"EMAIL","payload":"{...}"}'

# 8. When the access token expires (1h), exchange the refresh token for a new pair —
#    the OLD refresh token is revoked the instant this succeeds; save the new one
curl -X POST localhost:8080/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refreshToken>"}'
# -> a fresh { "accessToken": "...", "refreshToken": "..." } pair

# 9. Log out — revokes the refresh token server-side, independent of access-token expiry
curl -X POST localhost:8080/auth/logout \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refreshToken>"}'

# 10. Watch it flow through Grafana
open http://localhost:3000

# 11. Load test
k6 run scripts/load_test.js

# 12. Stop cleanly
docker compose down
```

---

## Postgres Schema

| Table | Written by | Contents |
|---|---|---|
| `users` | App (registration) | `id UUID`, `email`, `password_hash`, `role`, `created_at` |
| `jobs` | App (submission + workers) | `id UUID`, `type`, `payload`, `status`, `priority`, `retry_count`, `submitted_by` (FK → users), `idempotency_key`, `result`, `created_at`, `updated_at` |
| `refresh_tokens` | App (login/register/refresh) | `id UUID` (FK → users), `token_hash`, `expires_at`, `revoked`, `created_at` |

**Indexes:** `idx_jobs_status`, `idx_jobs_submitted_by`, composite
`idx_jobs_created_at_id` (`created_at DESC, id DESC` — supports keyset
pagination), and a partial unique index on `idempotency_key WHERE idempotency_key IS NOT NULL` and `idx_refresh_tokens_user_id`.

**Note on secrets at rest**: neither `password_hash`, `api_key_hash`, nor `token_hash` ever stores a raw, usable credential. Passwords use BCrypt (salted, deliberately slow — resists brute-forcing a low-entropy, human-chosen secret). API keys and refresh tokens use SHA-256 instead — both are already-random, high-entropy values generated server-side, so a fast deterministic hash is the correct tool: it supports an indexed lookup `(WHERE token_hash = ?)` that a salted hash like BCrypt cannot, without the unnecessary per-request latency BCrypt's slowness would add for a value that was never guessable in the first place.

A scheduled job (`RefreshTokenCleanupService`, daily) deletes `refresh_tokens` rows once they're genuinely expired, or have been revoked for more than 7 days — the retention window keeps recently revoked tokens around briefly for reuse-detection purposes before they're purged.

Migrations live in `db/migration/` (Flyway, `V1`–`V7`+). Reference queries —
e.g. the admin job-summary aggregation — live in `db/queries/`.

---

## Key Design Decisions

- **Kafka for ingestion, Redis for execution** — Kafka gives a durable,
  replayable log so no submitted job is ever lost; Redis (`LPUSH`/`BRPOP`)
  gives workers a fast, ephemeral queue. The consumer only commits a Kafka
  offset after the job is safely enqueued in Redis, so a crash between
  receipt and queuing can't drop a job.

- **Keyset over offset pagination** — the composite index
  `(created_at DESC, id DESC)` keeps query cost roughly constant at any page
  depth; offset pagination degrades linearly as the page number grows.
  Compared with `EXPLAIN ANALYZE` output in `docs/performance.md`.

- **Idempotency enforced in Redis, backed by Postgres** — a 24h-TTL key
  check in Redis short-circuits duplicate submissions before they reach
  Postgres; the partial unique index on `idempotency_key` is the backstop,
  not the primary guard.

- **GC strategy chosen by measurement, not default** — G1GC and ZGC were
  both benchmarked under k6 load with JFR profiling attached. The choice,
  along with the JFR-identified bottleneck that drove tuning (e.g. an
  undersized HikariCP pool), is documented with before/after throughput
  numbers in `docs/performance.md` rather than assumed.

- **Two-layer rate limiting** — a Redis sorted-set sliding window (per API
  key) avoids the boundary-burst problem of fixed-window counters, and runs
  as a `HandlerInterceptor` so a rejected request never reaches controller
  logic.

- **Dual-token auth with rotation, not a single long-lived JWT** — a single-token design    forces a choice between short expiry (frequent re-login) and long expiry (a leaked token stays dangerous for a long time). Splitting the concerns avoids the trade-off: the access token stays short-lived and stateless (verified without a DB call, unrevocable before expiry by design), while the refresh token is a database row — long-lived, but revocable — used only to mint new access tokens. *Rotation*: every `/auth/refresh` call immediately revokes the presented refresh token and issues a new one — a refresh token is single-use by design, never reused across renewals. *Reuse detection*: because tokens are single-use, a refresh token being presented a second time (found in the database but already revoked) is treated as evidence of compromise, not a benign retry — the response is to revoke *every* refresh token belonging to that user, not just the reused one. This is deliberate: it means a stolen-and-reused token forces the legitimate user to re-authenticate everywhere, closing the whole session family rather than leaving the attacker's access intact alongside the victim's. Same pattern used by Auth0 and AWS Cognito for the same reason.
