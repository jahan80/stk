import { apiClient } from "../client"
import type { ApiResponse, PaginatedResponse } from "@/types/api"
import type {
  EmailRequest,
  SmsRequest,
  PushRequest,
  Notification,
} from "@/types/notif"

export interface NotifSearchParams {
  channel?: string
  status?: string
  recipient?: string
  page?: number
  size?: number
}

export const notifApi = {
  sendEmail: (data: EmailRequest) =>
    apiClient
      .post<ApiResponse<Notification>>("/notify/email", data)
      .then((r) => r.data),

  sendSms: (data: SmsRequest) =>
    apiClient
      .post<ApiResponse<Notification>>("/notify/sms", data)
      .then((r) => r.data),

  sendPush: (data: PushRequest) =>
    apiClient
      .post<ApiResponse<Notification>>("/notify/push", data)
      .then((r) => r.data),

  get: (id: string) =>
    apiClient
      .get<ApiResponse<Notification>>(`/notify/${id}`)
      .then((r) => r.data),

  search: (params: NotifSearchParams) =>
    apiClient
      .get<ApiResponse<PaginatedResponse<Notification>>>("/notify", { params })
      .then((r) => r.data),
}
