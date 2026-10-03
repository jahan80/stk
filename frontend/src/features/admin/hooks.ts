import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { toast } from "sonner"
import { usersApi } from "@/api/endpoints/users"
import { rolesApi } from "@/api/endpoints/roles"
import { configsApi } from "@/api/endpoints/configs"
import { auditApi, type AuditSearchParams } from "@/api/endpoints/audit"
import { rateLimitsApi } from "@/api/endpoints/rateLimits"
import { queryKeys } from "@/api/queryKeys"
import type { CreateUserRequest, AssignRoleRequest } from "@/types/auth"
import type { RoleRequest } from "@/types/roles"
import type { ConfigurationRequest } from "@/types/configs"
import type { RateLimitRequest } from "@/types/rateLimits"

// ===== USERS =====
export function useUsers() {
  return useQuery({
    queryKey: queryKeys.users.all,
    queryFn: () => usersApi.list(),
  })
}

export function useCreateUser() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateUserRequest) => usersApi.create(data),
    onSuccess: () => {
      toast.success("User created successfully")
      qc.invalidateQueries({ queryKey: queryKeys.users.all })
    },
    onError: (error: any) => {
      toast.error(error?.response?.data?.message ?? "Failed to create user")
    },
  })
}

export function useAssignRole() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: AssignRoleRequest }) =>
      usersApi.assignRole(id, data),
    onSuccess: () => {
      toast.success("Role assigned")
      qc.invalidateQueries({ queryKey: queryKeys.users.all })
    },
    onError: (error: any) => {
      toast.error(error?.response?.data?.message ?? "Failed to assign role")
    },
  })
}

export function useToggleUser() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, enable }: { id: number; enable: boolean }) =>
      enable ? usersApi.enable(id) : usersApi.disable(id),
    onSuccess: (_, vars) => {
      toast.success(vars.enable ? "User enabled" : "User disabled")
      qc.invalidateQueries({ queryKey: queryKeys.users.all })
    },
    onError: (error: any) => {
      toast.error(error?.response?.data?.message ?? "Failed to update user")
    },
  })
}

// ===== ROLES =====
export function useRoles() {
  return useQuery({
    queryKey: queryKeys.roles.all,
    queryFn: () => rolesApi.list(),
  })
}

export function usePermissions() {
  return useQuery({
    queryKey: queryKeys.roles.permissions,
    queryFn: () => rolesApi.listPermissions(),
  })
}

export function useCreateRole() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: RoleRequest) => rolesApi.create(data),
    onSuccess: () => {
      toast.success("Role created")
      qc.invalidateQueries({ queryKey: queryKeys.roles.all })
    },
    onError: (error: any) => {
      toast.error(error?.response?.data?.message ?? "Failed to create role")
    },
  })
}

export function useDeleteRole() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => rolesApi.delete(id),
    onSuccess: () => {
      toast.success("Role deleted")
      qc.invalidateQueries({ queryKey: queryKeys.roles.all })
    },
    onError: (error: any) => {
      toast.error(error?.response?.data?.message ?? "Failed to delete role")
    },
  })
}

export function useAssignPermissions() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, permissions }: { id: number; permissions: number[] }) =>
      rolesApi.assignPermissions(id, permissions),
    onSuccess: () => {
      toast.success("Permissions updated")
      qc.invalidateQueries({ queryKey: queryKeys.roles.all })
    },
    onError: (error: any) => {
      toast.error(error?.response?.data?.message ?? "Failed to update permissions")
    },
  })
}

// ===== CONFIGURATIONS =====
export function useConfigs() {
  return useQuery({
    queryKey: queryKeys.configs.all,
    queryFn: () => configsApi.list(),
  })
}

export function useUpdateConfig() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ key, data }: { key: string; data: ConfigurationRequest }) =>
      configsApi.update(key, data),
    onSuccess: () => {
      toast.success("Configuration updated")
      qc.invalidateQueries({ queryKey: queryKeys.configs.all })
    },
    onError: (error: any) => {
      toast.error(error?.response?.data?.message ?? "Failed to update")
    },
  })
}

export function useResetConfig() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (key: string) => configsApi.resetToDefault(key),
    onSuccess: () => {
      toast.success("Configuration reset to default")
      qc.invalidateQueries({ queryKey: queryKeys.configs.all })
    },
    onError: (error: any) => {
      toast.error(error?.response?.data?.message ?? "Failed to reset")
    },
  })
}

// ===== AUDIT =====
export function useAuditEvents(params: AuditSearchParams) {
  return useQuery({
    queryKey: [...queryKeys.audit.all, params],
    queryFn: () => auditApi.search(params),
    staleTime: 30_000,
  })
}

export function useAuditByTrace(traceId: string) {
  return useQuery({
    queryKey: queryKeys.audit.byTrace(traceId),
    queryFn: () => auditApi.getByTraceId(traceId),
    enabled: !!traceId,
  })
}

// ===== RATE LIMITS =====
export function useRateLimits() {
  return useQuery({
    queryKey: queryKeys.rateLimits.all,
    queryFn: () => rateLimitsApi.list(),
  })
}

export function useCreateRateLimit() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: RateLimitRequest) => rateLimitsApi.create(data),
    onSuccess: () => {
      toast.success("Rate limit created")
      qc.invalidateQueries({ queryKey: queryKeys.rateLimits.all })
    },
    onError: (error: any) => {
      toast.error(error?.response?.data?.message ?? "Failed to create rate limit")
    },
  })
}

export function useUpdateRateLimit() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: RateLimitRequest }) =>
      rateLimitsApi.update(id, data),
    onSuccess: () => {
      toast.success("Rate limit updated")
      qc.invalidateQueries({ queryKey: queryKeys.rateLimits.all })
    },
    onError: (error: any) => {
      toast.error(error?.response?.data?.message ?? "Failed to update")
    },
  })
}

export function useToggleRateLimit() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, enable }: { id: number; enable: boolean }) =>
      enable ? rateLimitsApi.enable(id) : rateLimitsApi.disable(id),
    onSuccess: (_, vars) => {
      toast.success(vars.enable ? "Rate limit enabled" : "Rate limit disabled")
      qc.invalidateQueries({ queryKey: queryKeys.rateLimits.all })
    },
  })
}

export function useDeleteRateLimit() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => rateLimitsApi.delete(id),
    onSuccess: () => {
      toast.success("Rate limit deleted")
      qc.invalidateQueries({ queryKey: queryKeys.rateLimits.all })
    },
    onError: (error: any) => {
      toast.error(error?.response?.data?.message ?? "Failed to delete rate limit")
    },
  })
}

export function useResetRateLimit() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => rateLimitsApi.resetToDefault(id),
    onSuccess: () => {
      toast.success("Reset to default")
      qc.invalidateQueries({ queryKey: queryKeys.rateLimits.all })
    },
    onError: (error: any) => {
      toast.error(error?.response?.data?.message ?? "Failed to reset")
    },
  })
}

export function useResetAllRateLimits() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: () => rateLimitsApi.resetAllToDefault(),
    onSuccess: () => {
      toast.success("All rate limits reset to defaults")
      qc.invalidateQueries({ queryKey: queryKeys.rateLimits.all })
    },
    onError: (error: any) => {
      toast.error(error?.response?.data?.message ?? "Failed to reset all")
    },
  })
}

export function useActiveRateLimits() {
  return useQuery({
    queryKey: [...queryKeys.rateLimits.all, "active"],
    queryFn: () => rateLimitsApi.active(),
  })
}

export function useCreateConfig() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: ConfigurationRequest) => configsApi.create(data),
    onSuccess: () => {
      toast.success("Configuration created")
      qc.invalidateQueries({ queryKey: queryKeys.configs.all })
    },
    onError: (error: any) => {
      toast.error(error?.response?.data?.message ?? "Failed to create configuration")
    },
  })
}

export function useDeleteConfig() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (key: string) => configsApi.delete(key),
    onSuccess: () => {
      toast.success("Configuration deleted")
      qc.invalidateQueries({ queryKey: queryKeys.configs.all })
    },
    onError: (error: any) => {
      toast.error(error?.response?.data?.message ?? "Failed to delete configuration")
    },
  })
}

export function useResetAllConfigs() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: () => configsApi.resetAllToDefault(),
    onSuccess: () => {
      toast.success("All configurations reset to defaults")
      qc.invalidateQueries({ queryKey: queryKeys.configs.all })
    },
    onError: (error: any) => {
      toast.error(error?.response?.data?.message ?? "Failed to reset all")
    },
  })
}

export function useUpdateRole() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: RoleRequest }) =>
      rolesApi.update(id, data),
    onSuccess: () => {
      toast.success("Role updated")
      qc.invalidateQueries({ queryKey: queryKeys.roles.all })
    },
    onError: (error: any) => {
      toast.error(error?.response?.data?.message ?? "Failed to update role")
    },
  })
}
