export type TicketStatus = "OPEN" | "IN_PROGRESS" | "WAITING" | "RESOLVED" | "CLOSED"
export type TicketPriority = "LOW" | "MEDIUM" | "HIGH" | "URGENT"
export type AuthorRole = "USER" | "ADMIN" | "AGENT" | "SYSTEM"
export type GroupMemberRole = "LEADER" | "AGENT"

export interface TicketCategory {
  id: number
  code: string
  name: string
  description?: string
  enabled: boolean
  displayOrder: number
}

export interface TicketSummary {
  id: number
  ticketNumber: string
  title: string
  status: TicketStatus
  priority: TicketPriority
  categoryId: number
  categoryName?: string
  categoryCode?: string
  groupId?: number
  groupName?: string
  createdBy: number
  assignedTo?: number
  createdAt: string
  updatedAt: string
}

export interface TicketComment {
  id: number
  ticketId: number
  authorId: number
  authorRole: AuthorRole
  body: string
  createdAt: string
}

export interface Ticket {
  id: number
  ticketNumber: string
  title: string
  description: string
  status: TicketStatus
  priority: TicketPriority
  categoryId: number
  categoryName?: string
  categoryCode?: string
  groupId?: number
  groupName?: string
  createdBy: number
  assignedTo?: number
  createdAt: string
  updatedAt: string
  resolvedAt?: string
  closedAt?: string
  viewerRole?: "ADMIN" | "AGENT" | "USER"
  commentCount: number
  comments?: TicketComment[]
}

export interface TicketGroup {
  id: number
  name: string
  description?: string
  enabled: boolean
  memberCount: number
  members?: GroupMember[]
  createdAt: string
  updatedAt: string
}

export interface GroupMember {
  id: number
  userId: number
  role: GroupMemberRole
  createdAt: string
}

// ===== Requests =====
export interface CreateTicketRequest {
  title: string
  description: string
  categoryId: number
  priority?: TicketPriority
}

export interface AddCommentRequest {
  body: string
}

export interface ChangeStatusRequest {
  status: TicketStatus
}

export interface AssignTicketRequest {
  assigneeId: number
}

export interface GroupRequest {
  name: string
  description?: string
  enabled?: boolean
}

export interface GroupMemberRequest {
  userId: number
  role?: GroupMemberRole
}

// ===== Configuration =====
export interface TicketConfiguration {
  id: number
  configKey: string
  configValue: string
  defaultValue: string
  valueType: string
  description?: string
  enabled: boolean
  createdAt: string
  updatedAt: string
}
