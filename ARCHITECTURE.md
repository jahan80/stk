# Architecture Notes

## Layers (per service)

    com.starterkit.<svc>/
    ├── <domain>/
    │   ├── api/               # REST controllers + DTOs
    │   ├── application/       # services, event publishers, listeners
    │   ├── domain/            # entities, repositories
    │   └── infrastructure/    # config, JPA, providers, RabbitMQ
    └── shared/
        ├── api/               # ApiResponse, ApiCode, exception handlers
        ├── domain/            # BaseEntity
        └── infrastructure/    # TraceIdFilter, JWT, event helpers

**Rule:** cross-service calls via REST or events only, never via DB joins.

## Databases

Each service owns its own schema:

| Service | Schema |
|---|---|
| auth-service | `auth` |
| ticket-service | `ticket` |
| audit-service | `audit` |
| notif-service | `notif` |
| api-gateway | `gateway` |

Flyway manages all migrations. Never modify entities without a matching migration.

## Event Flow (end-to-end)

Example: user creates a ticket.

1. `POST /tickets` → `TicketService.create()` in `@Transactional`.
2. `ticketRepo.save(ticket)` → `ticket.tickets`.
3. `eventPublisher.publish(...)` → `OutboxTicketEventPublisher` → `outboxService.save(...)` → `ticket.outbox_events` same TX.
4. Transaction commits.
5. `OutboxPublisher` scheduler fetches pending rows.
6. `RabbitTemplate.convertAndSend("starterkit.events", "ticket.created", envelope)`.
7. Success → `PUBLISHED`, `published_at=now()`. Failure → `attempts++`, `next_retry_at = now + backoff`.
8. RabbitMQ fans out to bound queues:
   - `audit.ticket.events` → `TicketEventListener` → `audit.audit_events`
   - In-app notification → `ticket.notifications` (UI-only, not a business event).

## Retry + DLQ Policy

**Broker level (Spring AMQP):**

    retry.enabled=true
    retry.max-attempts=3
    retry.initial-interval=1000
    retry.multiplier=2.0
    retry.max-interval=10000
    default-requeue-rejected=false

    attempt 1 → fail → retry 1s
    attempt 2 → fail → retry 2s
    attempt 3 → fail → DLX → DLQ

**Outbox level (our code):**

    attempt 1 → 5s
    attempt 2 → 30s
    attempt 3 → 2m
    attempt 4 → 10m
    attempt 5+ → 1h (cap)

Publisher never gives up. DLQ bounded (`ttl=7d`, `max-length=10000`).

## Idempotency

RabbitMQ = at-least-once. Consumers must be idempotent.

- `notif.notifications.event_id` UNIQUE + pre-check `existsByEventId`
- `audit.audit_events.event_id` UNIQUE + same pattern
- Race conditions caught by DB constraint → retried → skip on next attempt.

## Security

- **JWT (RSA)** — private key only in auth-service; public key distributed to gateway + ticket.
- **Refresh tokens** — 512-bit random, SHA-256 hashed, rotated each use.
- **Reuse detection** — revoked token replay → revoke ALL user's tokens.
- **Rate limiting** — token-bucket per IP/path at gateway.
- **BCrypt** for passwords.
- **Default admin** — only when `APP_DEFAULT_ADMIN_ENABLED=true` (dev).

## Known TODOs

- HttpOnly refresh cookie (currently in localStorage)
- Integration tests for outbox → MQ → consumer chain
- Notification template engine
- Public profile backend endpoint

## Gateway event publishing (observability, not business)

`api-gateway` emits two event types:

| Event | Routing key | Purpose |
|---|---|---|
| `REQUEST_RECEIVED`  | `gateway.request.received`  | Per-request audit |
| `RESPONSE_SENT`     | `gateway.response.sent`     | Per-response audit (status, latency, size) |

These are **observability events**, not business state. They are
deliberately NOT routed through the transactional outbox:

- They fire on EVERY request → high volume.
- Losing a few is acceptable; business correctness is unaffected.
- The audit service uses them for tracing/ops, not for state.

### Publisher strategy

Two implementations exist; the active one is chosen by
`gateway.events.publisher`:

| Value | Bean | Behavior |
|---|---|---|
| `async` (default) | `BoundedAsyncGatewayEventPublisher` | Enqueue → single worker thread → retry 3x → drop on exhaustion |
| `sync`            | `SyncGatewayEventPublisher`            | Publish on the request thread; log on failure |

### Backpressure semantics (async)

- Bounded queue: `gateway.events.queue-capacity` (default 10 000).
- When the queue is full → event is **dropped**:
  - logged at WARN with `queue FULL`,
  - counted in `GatewayEventPublisherStats.droppedQueueFull`.
- Worker retries each event 3 times with small backoff.
- After 3 failures → dropped, counted in `publishFailures`.

This bounds memory and prevents the gateway from stalling when
the broker is unavailable.

### Operator visibility

`GET /gateway/events/stats` (ADMIN only) returns:

    {
      "enqueued":           123456,
      "published":          123450,
      "droppedQueueFull":   3,
      "publishFailures":    3,
      "workerErrors":       0,
      "lastErrorAt":        "2026-10-07T15:00:00Z",
      "lastErrorSecondsAgo": 42
    }

Alert if `droppedQueueFull` or `publishFailures` grows.
