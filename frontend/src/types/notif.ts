export interface EmailRequest {
  to: string
  subject?: string
  body: string
  metadata?: Record<string, unknown>
}

export interface SmsRequest {
  to: string
  message: string
  metadata?: Record<string, unknown>
}

export interface PushRequest {
  deviceToken: string
  title?: string
  body: string
  metadata?: Record<string, unknown>
}

export interface Notification {
  notificationId: string
  channel: "EMAIL" | "SMS" | "PUSH"
  recipient: string
  subject?: string
  status: "PENDING" | "SENT" | "FAILED"
  provider: string
  providerMessageId?: string
  errorMessage?: string
  metadata?: Record<string, unknown>
  createdAt: string
  sentAt?: string
}
