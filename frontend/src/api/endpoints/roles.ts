import { apiClient } from "../client"
import type { ApiResponse } from "@/types/api"
import type { Role, RoleRequest, Permission } from "@/types/roles"

export const rolesApi = {
  list: () =>
    apiClient.get<ApiResponse<Role[]>>("/auth/roles").then((r) => r.data),

  get: (id: number) =>
    apiClient.get<ApiResponse<Role>>(`/auth/roles/${id}`).then((r) => r.data),

  create: (data: RoleRequest) =>
    apiClient.post<ApiResponse<Role>>("/auth/roles", data).then((r) => r.data),

  update: (id: number, data: RoleRequest) =>
    apiClient
      .put<ApiResponse<Role>>(`/auth/roles/${id}`, data)
      .then((r) => r.data),

  delete: (id: number) =>
    apiClient.delete<ApiResponse<void>>(`/auth/roles/${id}`).then((r) => r.data),

  assignPermissions: (id: number, permissionIds: number[]) =>
    apiClient
      .put<ApiResponse<Role>>(`/auth/roles/${id}/permissions`, permissionIds)
      .then((r) => r.data),

  listPermissions: () =>
    apiClient
      .get<ApiResponse<Permission[]>>("/auth/permissions")
      .then((r) => r.data),
}
