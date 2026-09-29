import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { toast } from "sonner"
import { ticketsApi, type TicketSearchParams } from "@/api/endpoints/tickets"
import { queryKeys } from "@/api/queryKeys"
import type {
  CreateTicketRequest,
  AddCommentRequest,
  ChangeStatusRequest,
  AssignTicketRequest,
  GroupRequest,
  GroupMemberRequest,
} from "@/types/tickets"

// ===== Tickets =====
export function useTickets(params: TicketSearchParams = {}) {
  return useQuery({
    queryKey: queryKeys.tickets.list(params),
    queryFn: () => ticketsApi.list(params),
    staleTime: 30_000,
  })
}

export function useTicket(id: number) {
  return useQuery({
    queryKey: queryKeys.tickets.detail(id),
    queryFn: () => ticketsApi.get(id),
    enabled: !!id,
  })
}

export function useCreateTicket() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateTicketRequest) => ticketsApi.create(data),
    onSuccess: () => {
      toast.success("Ticket created")
      qc.invalidateQueries({ queryKey: queryKeys.tickets.all })
    },
    onError: (e: any) => toast.error(e?.response?.data?.message ?? "Failed"),
  })
}

export function useCloseTicket(id: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: () => ticketsApi.close(id),
    onSuccess: () => {
      toast.success("Ticket closed")
      qc.invalidateQueries({ queryKey: queryKeys.tickets.all })
    },
  })
}

export function useChangeStatus(id: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: ChangeStatusRequest) => ticketsApi.changeStatus(id, data),
    onSuccess: (r) => {
      toast.success(`Status: ${r.data.status}`)
      qc.invalidateQueries({ queryKey: queryKeys.tickets.all })
    },
    onError: (e: any) => toast.error(e?.response?.data?.message ?? "Failed"),
  })
}

export function useAssignTicket(id: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: AssignTicketRequest) => ticketsApi.assign(id, data),
    onSuccess: () => {
      toast.success("Assigned")
      qc.invalidateQueries({ queryKey: queryKeys.tickets.all })
    },
  })
}

export function useAssignToGroup(id: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (groupId: number | null) => ticketsApi.assignToGroup(id, groupId),
    onSuccess: () => {
      toast.success("Group updated")
      qc.invalidateQueries({ queryKey: queryKeys.tickets.all })
    },
  })
}

export function useComments(id: number) {
  return useQuery({
    queryKey: queryKeys.tickets.comments(id),
    queryFn: () => ticketsApi.listComments(id),
    enabled: !!id,
  })
}

export function useAddComment(id: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: AddCommentRequest) => ticketsApi.addComment(id, data),
    onSuccess: () => {
      toast.success("Comment added")
      qc.invalidateQueries({ queryKey: queryKeys.tickets.comments(id) })
      qc.invalidateQueries({ queryKey: queryKeys.tickets.detail(id) })
    },
  })
}

// ===== Categories =====
export function useCategories() {
  return useQuery({
    queryKey: queryKeys.tickets.categories,
    queryFn: () => ticketsApi.listCategories(),
    staleTime: 5 * 60_000,
  })
}

// ===== Groups =====
export function useGroups() {
  return useQuery({
    queryKey: queryKeys.tickets.groups,
    queryFn: () => ticketsApi.listGroups(),
    staleTime: 60_000,
  })
}

export function useMyGroups() {
  return useQuery({
    queryKey: queryKeys.tickets.myGroups,
    queryFn: () => ticketsApi.listMyGroups(),
    staleTime: 60_000,
  })
}

export function useCreateGroup() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: GroupRequest) => ticketsApi.createGroup(data),
    onSuccess: () => {
      toast.success("Group created")
      qc.invalidateQueries({ queryKey: queryKeys.tickets.groups })
    },
    onError: (e: any) => toast.error(e?.response?.data?.message ?? "Failed"),
  })
}

export function useUpdateGroup() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: GroupRequest }) =>
      ticketsApi.updateGroup(id, data),
    onSuccess: () => {
      toast.success("Group updated")
      qc.invalidateQueries({ queryKey: queryKeys.tickets.groups })
    },
  })
}

export function useDeleteGroup() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => ticketsApi.deleteGroup(id),
    onSuccess: () => {
      toast.success("Group disabled")
      qc.invalidateQueries({ queryKey: queryKeys.tickets.groups })
    },
  })
}

export function useGroupMembers(id: number) {
  return useQuery({
    queryKey: queryKeys.tickets.groupMembers(id),
    queryFn: () => ticketsApi.listGroupMembers(id),
    enabled: !!id,
  })
}

export function useAddGroupMember() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: GroupMemberRequest }) =>
      ticketsApi.addGroupMember(id, data),
    onSuccess: (_, vars) => {
      toast.success("Member added")
      qc.invalidateQueries({ queryKey: queryKeys.tickets.groupMembers(vars.id) })
      qc.invalidateQueries({ queryKey: queryKeys.tickets.groups })
    },
    onError: (e: any) => toast.error(e?.response?.data?.message ?? "Failed"),
  })
}

export function useRemoveGroupMember() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, userId }: { id: number; userId: number }) =>
      ticketsApi.removeGroupMember(id, userId),
    onSuccess: (_, vars) => {
      toast.success("Member removed")
      qc.invalidateQueries({ queryKey: queryKeys.tickets.groupMembers(vars.id) })
      qc.invalidateQueries({ queryKey: queryKeys.tickets.groups })
    },
  })
}

// ===== Configurations =====
import { configsApi } from "@/api/endpoints/tickets"

export function useTicketConfigs() {
  return useQuery({
    queryKey: queryKeys.tickets.configs,
    queryFn: () => configsApi.list(),
    staleTime: 30_000,
  })
}

export function useUpdateTicketConfig() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ key, value }: { key: string; value: string }) => configsApi.update(key, value),
    onSuccess: () => {
      toast.success("Configuration updated")
      qc.invalidateQueries({ queryKey: queryKeys.tickets.configs })
    },
    onError: (e: any) => toast.error(e?.response?.data?.message ?? "Failed"),
  })
}

export function useResetTicketConfig() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (key: string) => configsApi.reset(key),
    onSuccess: () => {
      toast.success("Reset to default")
      qc.invalidateQueries({ queryKey: queryKeys.tickets.configs })
    },
  })
}
