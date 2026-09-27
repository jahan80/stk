export interface AuditEvent {
  id: number
  eventId: string
  eventType: string
  eventVersion: string
  source: string
  traceId?: string
  payload: Record<string, unknown>
  occurredAt: string
  receivedAt: string
}
