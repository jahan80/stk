import { apiClient } from "../client"
import type { ApiResponse, PaginatedResponse } from "@/types/api"
import type { TicketNotification } from "@/types/notifications"

export const notificationsApi = {
  list: (params: { unreadOnly?: boolean; page?: number; size?: number } = {}) =>
    apiClient
      .get<ApiResponse<PaginatedResponse<TicketNotification>>>("/tickets/notifications", { params })
      .then((r) => r.data),

  unreadCount: () =>
    apiClient
      .get<ApiResponse<{ count: number }>>("/tickets/notifications/unread-count")
      .then((r) => r.data),

  markAsRead: (id: number) =>
    apiClient
      .post<ApiResponse<void>>(`/tickets/notifications/${id}/read`)
      .then((r) => r.data),

  markAllAsRead: () =>
    apiClient
      .post<ApiResponse<{ marked: number }>>("/tickets/notifications/read-all")
      .then((r) => r.data),
}
