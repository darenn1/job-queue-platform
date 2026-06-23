# Job Queue Platform

An asynchronous job processing system built in Java and Spring Boot — durable
ingestion via Kafka, a Redis-backed worker queue, Postgres persistence, JWT
auth, and full observability with Prometheus/Grafana. Built incrementally
following a 14-week, 70-day curriculum spanning JVM internals through
load-test-driven GC tuning.

---

### Data Flows

| Flow | Transport | Description |
|---|---|---|
| Client → Spring Boot | REST (HTTP), JWT | `POST /jobs` submits a job; auth via Bearer JWT, verified without a DB call |
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
| Maven | latest (wrapper included) | Build tool — `pom.xml` |
| Docker | 24.0 | Container runtime |
| Docker Compose | 2.20 | Orchestrates Postgres, Redis, Kafka (KRaft mode), Prometheus, Grafana |
| k6 | latest | Load testing (local dev only) |

### Quick Start

```bash
# 1. Copy env template to project root
cp .env.example .env

# 2. Start infra: Postgres, Redis, Kafka, Prometheus, Grafana
docker compose up -d

# 3. Run Flyway migrations (creates users + jobs tables, indexes)
./mvnw flyway:migrate

# 4. Build and run the app
./mvnw spring-boot:run

# 5. Submit a job
curl -X POST localhost:8080/jobs -H "Authorization: Bearer <jwt>" \
  -d '{"type":"EMAIL","payload":"{...}"}'

# 6. Watch it flow through Grafana
open http://localhost:3000

# 7. Load test
k6 run scripts/load_test.js

# 8. Stop cleanly
docker compose down
```

---

## Postgres Schema

| Table | Written by | Contents |
|---|---|---|
| `users` | App (registration) | `id UUID`, `email`, `password_hash`, `role`, `created_at` |
| `jobs` | App (submission + workers) | `id UUID`, `type`, `payload`, `status`, `priority`, `retry_count`, `submitted_by` (FK → users), `idempotency_key`, `result`, `created_at`, `updated_at` |

**Indexes:** `idx_jobs_status`, `idx_jobs_submitted_by`, composite
`idx_jobs_created_at_id` (`created_at DESC, id DESC` — supports keyset
pagination), and a partial unique index on `idempotency_key WHERE
idempotency_key IS NOT NULL`.

Migrations live in `db/migration/` (Flyway, `V1`–`V5`+). Reference queries —
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

---

## Learning Roadmap

This project was built incrementally following a 14-week, 70-day curriculum
— see [`Job_Queue_Roadmap_Claude.html`](./Job_Queue_Roadmap_Claude.html) —
progressing from Java/JVM fundamentals through a hand-rolled thread pool,
Spring Boot + Postgres, Redis/Kafka/JWT, and finally Docker, observability,
and load-test-driven JVM tuning.