# Changelog

All notable changes to StarterKit are documented here.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [v16.0.0] — 2026-10-10

Release focused on architecture cleanup and security hardening.

### Added — notification delivery is fully event-driven

- **Notification retry (P0)** — status-aware idempotency; the
  listener now only ACKs on terminal success, and a dedicated
  retry job owns FAILED rows with row-level lease (FOR UPDATE
  SKIP LOCKED). Backoff: 30s -> 60s -> 5m -> 30m -> 2h.
- **Delivery outcomes in audit** — notif-service publishes
  NOTIFICATION_SENT / FAILED / EXHAUSTED to audit-service.
- **Bounded async gateway event publisher** — token bucket
  queue with queue-full drop + operator stats endpoint
  /gateway/events/stats.
- **Independent IN_APP channel** in notif-service with its own
  JWT verification, /notify/me API, and per-recipient
  deterministic event_id derivation.
- **Event-driven IN_APP delivery (D2)** — NOTIFICATION_REQUESTED
  is the single command event for all in-app notifications.
  ticket.notifications table dropped (V17).
- **Per-account verification resend cooldown** enforced in
  auth-service (email, mobile, password reset).

### Changed — security hardening

- Host ports for notif-service (8083) and ticket-service (8084)
  removed; services are now internal-only.
- Pagination `size` parameter capped at 100 on all list
  endpoints (audit, notif, ticket).
- APP_DEFAULT_ADMIN_ENABLED now defaults to false in
  docker-compose.yml; dev must opt in explicitly.

### Changed — architecture

- **TicketService split** into TicketCommandService,
  TicketQueryService, TicketAssignmentService,
  TicketCommentService.
- **Dockerfile strategy** switched to host-build runtime-only:
  jars are built on the host with Maven and Docker only
  packages the runtime. Build time 6-10 min -> 20 sec.
- **notif-service** now verifies JWTs independently
  (SecurityConfig, JwtAuthenticationFilter, public key).

### Removed

- ticket.notifications (V17)
- ticket.NotificationService, ExternalNotificationDispatcher,
  TicketNotificationController, TicketNotification entity
- NotifSendEmailEvent / NotifSendSmsEvent (replaced by
  unified NotifRequestedEvent)

### Documentation

- ARCHITECTURE.md: rate limiting in distributed deployment.
- CHANGELOG.md: this file.

### Migration notes

- Flyway V5 (notif), V17 (ticket), V27 (auth) run automatically
  on service startup.
- **Breaking for frontend**: /tickets/notifications is gone.
  Use /notify/me (GET), /notify/me/unread-count,
  /notify/me/{id}/read, /notify/me/read-all.

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
