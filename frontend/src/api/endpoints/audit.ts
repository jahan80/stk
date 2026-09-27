import { apiClient } from "../client"
import type { ApiResponse, PaginatedResponse } from "@/types/api"
import type { AuditEvent } from "@/types/audit"

export interface AuditSearchParams {
  eventType?: string
  source?: string
  from?: string
  to?: string
  page?: number
  size?: number
}

export const auditApi = {
  search: (params: AuditSearchParams) =>
    apiClient
      .get<ApiResponse<PaginatedResponse<AuditEvent>>>("/audit/events", {
        params,
      })
      .then((r) => r.data),

  getByTraceId: (traceId: string) =>
    apiClient
      .get<ApiResponse<AuditEvent[]>>(`/audit/events/trace/${traceId}`)
      .then((r) => r.data),
}
