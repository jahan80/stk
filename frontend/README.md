# StarterKit — Microservices Platform

Production-oriented starter kit built on **Spring Boot 3.5 + Java 21**, **React 19 + Vite**, **PostgreSQL 16**, **RabbitMQ 3.13**.

---

## Services

| Service | Port | Role |
|---|---|---|
| `api-gateway` | 8080 | Spring Cloud Gateway (WebFlux), rate limit, trace-id, JWT verify |
| `auth-service` | 8081 | Users, roles, permissions, JWT, email/mobile verification, password reset |
| `audit-service` | 8082 | Consumes all business events → `audit.audit_events` |
| `notif-service` | 8083 | Email / SMS / Push providers, delivery tracking, idempotent, DLQ |
| `ticket-service` | 8084 | Tickets, comments, groups, SLA, in-app notifications |
| `outbox-commons` | — | Shared Transactional Outbox library |

---

## Transactional Outbox Pattern

Business events are **never published directly** to RabbitMQ:

1. Service writes entity + `outbox_events` row in the same DB transaction.
2. Scheduled `OutboxPublisher` polls pending rows.
3. Each event published in its own `REQUIRES_NEW` TX; failures retried with backoff (5s → 30s → 2m → 10m → 1h cap).
4. Published rows marked `PUBLISHED` and later deleted by cleanup job.

**Guarantees:** at-least-once delivery, no event loss during broker outage.

---

## Event Envelope

```json
{
  "eventId":     "uuid",
  "eventType":   "TICKET_CREATED",
  "eventVersion":"1.0",
  "source":      "ticket-service",
  "traceId":     "uuid",
  "occurredAt":  "2026-01-01T12:00:00Z",
  "data":        { }
}