import { apiClient } from "../client"
import type { ApiResponse, PaginatedResponse } from "@/types/api"
import type {
  Ticket,
  TicketSummary,
  TicketCategory,
  TicketComment,
  TicketGroup,
  GroupMember,
  CreateTicketRequest,
  AddCommentRequest,
  ChangeStatusRequest,
  AssignTicketRequest,
  GroupRequest,
  GroupMemberRequest,
  TicketStatus,
  TicketPriority,
} from "@/types/tickets"

export interface TicketSearchParams {
  status?: TicketStatus
  priority?: TicketPriority
  groupId?: number
  unassignedOnly?: boolean
  page?: number
  size?: number
}

export const ticketsApi = {
  list: (params: TicketSearchParams = {}) =>
    apiClient
      .get<ApiResponse<PaginatedResponse<TicketSummary>>>("/tickets", { params })
      .then((r) => r.data),

  get: (id: number) =>
    apiClient.get<ApiResponse<Ticket>>(`/tickets/${id}`).then((r) => r.data),

  create: (data: CreateTicketRequest) =>
    apiClient.post<ApiResponse<Ticket>>("/tickets", data).then((r) => r.data),

  close: (id: number) =>
    apiClient.post<ApiResponse<Ticket>>(`/tickets/${id}/close`).then((r) => r.data),

  changeStatus: (id: number, data: ChangeStatusRequest) =>
    apiClient.post<ApiResponse<Ticket>>(`/tickets/${id}/status`, data).then((r) => r.data),

  assign: (id: number, data: AssignTicketRequest) =>
    apiClient.post<ApiResponse<Ticket>>(`/tickets/${id}/assign`, data).then((r) => r.data),

  assignToGroup: (id: number, groupId: number | null) =>
    apiClient.post<ApiResponse<Ticket>>(`/tickets/${id}/assign-group`, null, {
      params: { groupId },
    }).then((r) => r.data),

  listComments: (id: number) =>
    apiClient.get<ApiResponse<TicketComment[]>>(`/tickets/${id}/comments`).then((r) => r.data),

  addComment: (id: number, data: AddCommentRequest) =>
    apiClient.post<ApiResponse<TicketComment>>(`/tickets/${id}/comments`, data).then((r) => r.data),

  listCategories: () =>
    apiClient.get<ApiResponse<TicketCategory[]>>("/tickets/categories").then((r) => r.data),

  // ===== Groups =====
  listGroups: () =>
    apiClient.get<ApiResponse<TicketGroup[]>>("/tickets/groups").then((r) => r.data),

  listMyGroups: () =>
    apiClient.get<ApiResponse<TicketGroup[]>>("/tickets/groups/my").then((r) => r.data),

  getGroup: (id: number) =>
    apiClient.get<ApiResponse<TicketGroup>>(`/tickets/groups/${id}`).then((r) => r.data),

  createGroup: (data: GroupRequest) =>
    apiClient.post<ApiResponse<TicketGroup>>("/tickets/groups", data).then((r) => r.data),

  updateGroup: (id: number, data: GroupRequest) =>
    apiClient.put<ApiResponse<TicketGroup>>(`/tickets/groups/${id}`, data).then((r) => r.data),

  deleteGroup: (id: number) =>
    apiClient.delete<ApiResponse<void>>(`/tickets/groups/${id}`).then((r) => r.data),

  listGroupMembers: (id: number) =>
    apiClient.get<ApiResponse<GroupMember[]>>(`/tickets/groups/${id}/members`).then((r) => r.data),

  addGroupMember: (id: number, data: GroupMemberRequest) =>
    apiClient.post<ApiResponse<GroupMember>>(`/tickets/groups/${id}/members`, data).then((r) => r.data),

  removeGroupMember: (id: number, userId: number) =>
    apiClient.delete<ApiResponse<void>>(`/tickets/groups/${id}/members/${userId}`).then((r) => r.data),
}

// ===== Configurations =====
import type { TicketConfiguration } from "@/types/tickets"

export const ticketConfigsApi = {
  list: () =>
    apiClient.get<ApiResponse<TicketConfiguration[]>>("/tickets/configurations").then((r) => r.data),

  update: (key: string, value: string) =>
    apiClient
      .put<ApiResponse<TicketConfiguration>>(`/tickets/configurations/${key}`, { configValue: value })
      .then((r) => r.data),

  reset: (key: string) =>
    apiClient
      .post<ApiResponse<TicketConfiguration>>(`/tickets/configurations/${key}/reset-default`)
      .then((r) => r.data),
}
