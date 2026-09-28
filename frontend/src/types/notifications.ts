export type NotificationType =
  | "TICKET_CREATED"
  | "TICKET_ASSIGNED"
  | "TICKET_COMMENTED"
  | "TICKET_STATUS_CHANGED"
  | "TICKET_MENTIONED"

export interface TicketNotification {
  id: number
  type: NotificationType
  title: string
  message?: string
  ticketId?: number
  actorId?: number
  read: boolean
  readAt?: string
  link?: string
  createdAt: string
}
