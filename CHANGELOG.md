# Changelog

All notable changes to StarterKit are documented here.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [v15.0.0] — 2026-10-07

Reliability and observability release. Fixes a silent provider-retry bug
in notif-service, closes the notification→audit feedback loop, and
makes gateway event publishing bounded and observable.

### Fixed — Notification retry (P0)

**Problem.** `NotifEventListener` skipped any event whose `event_id`
already existed. Because the PENDING row was persisted BEFORE the
provider call, a provider timeout left `status=FAILED` — and every
subsequent RabbitMQ redelivery was silently ACKed as a duplicate.
Provider-level retries never happened.

**Fix.**
- `V3__add_retry_columns.sql` — adds `attempts`, `last_error`,
  `next_attempt_at`, `locked_until`, `claimed_by` + partial retry index.
- `V4__add_created_at_default.sql` — `created_at DEFAULT NOW()` so
  direct SQL inserts (tests, backfills) don't fail NOT NULL.
- `NotifEventListener` — status-aware idempotency:
  - `SENT` → ACK skip (already delivered)
  - `PENDING` → ACK skip (in-flight)
  - `FAILED` → ACK skip (retry job owns it)
  - absent → first delivery, process now
- `NotifRetryJob` — scheduled worker, claims `FAILED`/`PENDING` rows via
  `FOR UPDATE SKIP LOCKED` with a lease; reclaims expired claims.
- `NotificationService.retryDelivery` — re-uses the same row; no
  duplication.
- Backoff schedule: 30s → 60s → 5m → 30m → 2h, then terminal (exhausted).
- `NotifServiceApplication` — added `@EnableScheduling` (was missing, so
  the retry job never ran).

### Added — Audit for notification delivery (P1)

Before this change, `notif.#` was not subscribed by `audit-service`, so
operators could not see whether a notification was actually delivered.

- `NotifDeliveryEvent` — record carrying `SENT`/`FAILED`/`EXHAUSTED`
  outcomes with recipient masking (email local-part, phone last-4).
- `NotifDeliveryEventPublisher` — best-effort publish to
  `starterkit.events` with routing key `notif.<channel>.<outcome>`.
  Failures are logged but never propagate (audit must not break delivery).
- `NotificationService` — publishes on every terminal outcome.
- `audit-service`:
  - new queue `audit.notif.events` bound to `notif.#`.
  - new `NotifEventListener`.
- `NotifEventListener` (notif svc) — self-loop protection: silently ACK
  `NOTIFICATION_*` events. Without this, our own result events would
  re-enter via `notif.#` and fill the DLQ.
- `NotificationService.reload` — now uses `persister.loadFresh()`
  (`REQUIRES_NEW`) so the HTTP response reflects post-`markSent` status
  (previously returned `PENDING` due to Hibernate 1st-level cache).

### Added — Bounded async gateway event publishing (P1)

**Problem.** `RabbitMqGatewayEventPublisher` published on the request
thread and swallowed all exceptions. Broker outage could stall request
latency, and event loss was invisible.

**Design.** Gateway events (`REQUEST_RECEIVED`, `RESPONSE_SENT`) are
**observability data, not business state**. A full transactional outbox
is overkill: high volume, no business impact on loss, no recovery goal.

- `BoundedAsyncGatewayEventPublisher`
  - `ArrayBlockingQueue(capacity=10_000)`
  - single daemon worker thread
  - non-blocking `offer()` on the request path
  - retry 3× with small backoff on the worker
  - drop on queue-full or after max retries (WARN + counter)
- `GatewayEventPublisherStats` — in-memory counters:
  `enqueued`, `published`, `droppedQueueFull`, `publishFailures`,
  `workerErrors`, `lastErrorAt`.
- `GET /gateway/events/stats` (ADMIN) — operator snapshot.
- `SyncGatewayEventPublisher` — opt-in fallback via
  `gateway.events.publisher=sync` (dev/debug only).
- `ARCHITECTURE.md` — new section explicitly documenting that gateway
  events are observability, not business state.

### Changed — Docker build strategy

Multi-stage Dockerfiles that ran `mvn dependency:go-offline` inside the
build container failed when the environment could not reach
`repo.maven.apache.org` (corporate proxy, offline CI). Switched all
services to **runtime-only Dockerfiles**: the jar is built on the host
(Maven, local `~/.m2`) and Docker only packages it.

Build time dropped from ~6–10 min to ~20 sec, and the Docker build no
longer depends on external Maven repositories.

### Added — repository hygiene

- `.gitignore` — `.backups/`, `*.bak`, `*.bak-*`, `backend-dump.txt`.

### Migration notes

- Flyway V3 + V4 run automatically on `notif-service` startup.
- No data migration required; existing `FAILED` rows become
  retry-eligible after `next_attempt_at = NOW()` backfill (handled in V3).
- No breaking API change for clients.

### Commits

- `191c322` fix(notif): status-aware idempotency + row-level retry with backoff
- `3d73fc2` feat(audit): record notification delivery outcomes
- `2783cd5` build: host-build runtime-only Dockerfiles
- `f354777` feat(gateway): bounded async event publisher with operator stats

## [v14.0.0] — previous

See git history for details prior to v15.0.0. Highlights:

- P1: token-bucket fractional refill, X-User-Id sanitization,
  refresh-token reuse detection, audit authorization,
  verification-email via outbox.
- P2: `.env.example` 12+ char password, README port fix, mention
  notifications, auto-close scheduler, internal token startup guard,
  rate-limit circuit breaker.
- P3: `starterkit-commons` module, Categories Admin API.
