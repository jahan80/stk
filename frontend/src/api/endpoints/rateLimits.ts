import { apiClient } from "../client"
import type { ApiResponse } from "@/types/api"
import type { RateLimit, RateLimitRequest } from "@/types/rateLimits"

export const rateLimitsApi = {
  list: () =>
    apiClient
      .get<ApiResponse<RateLimit[]>>("/gateway/rate-limits")
      .then((r) => r.data),

  get: (id: number) =>
    apiClient
      .get<ApiResponse<RateLimit>>(`/gateway/rate-limits/${id}`)
      .then((r) => r.data),

  create: (data: RateLimitRequest) =>
    apiClient
      .post<ApiResponse<RateLimit>>("/gateway/rate-limits", data)
      .then((r) => r.data),

  update: (id: number, data: RateLimitRequest) =>
    apiClient
      .put<ApiResponse<RateLimit>>(`/gateway/rate-limits/${id}`, data)
      .then((r) => r.data),

  delete: (id: number) =>
    apiClient
      .delete<ApiResponse<void>>(`/gateway/rate-limits/${id}`)
      .then((r) => r.data),

  resetToDefault: (id: number) =>
    apiClient
      .post<ApiResponse<RateLimit>>(`/gateway/rate-limits/${id}/reset-default`)
      .then((r) => r.data),

  resetAllToDefault: () =>
    apiClient
      .post<ApiResponse<RateLimit[]>>("/gateway/rate-limits/reset-defaults")
      .then((r) => r.data),

  enable: (id: number) =>
    apiClient
      .post<ApiResponse<RateLimit>>(`/gateway/rate-limits/${id}/enable`)
      .then((r) => r.data),

  disable: (id: number) =>
    apiClient
      .post<ApiResponse<RateLimit>>(`/gateway/rate-limits/${id}/disable`)
      .then((r) => r.data),
}
