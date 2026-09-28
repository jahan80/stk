import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { toast } from "sonner"
import { notificationsApi } from "@/api/endpoints/notifications"
import { queryKeys } from "@/api/queryKeys"

export function useNotifications(params: { unreadOnly?: boolean; page?: number; size?: number } = {}) {
  return useQuery({
    queryKey: queryKeys.notifications.list(params),
    queryFn: () => notificationsApi.list(params),
    staleTime: 15_000,
  })
}

export function useUnreadCount() {
  return useQuery({
    queryKey: queryKeys.notifications.unreadCount,
    queryFn: () => notificationsApi.unreadCount(),
    refetchInterval: 30_000,  // poll every 30s
    staleTime: 10_000,
  })
}

export function useMarkAsRead() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => notificationsApi.markAsRead(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.notifications.all })
    },
  })
}

export function useMarkAllAsRead() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: () => notificationsApi.markAllAsRead(),
    onSuccess: (data) => {
      toast.success(`${data.data.marked} notification(s) marked as read`)
      qc.invalidateQueries({ queryKey: queryKeys.notifications.all })
    },
  })
}
