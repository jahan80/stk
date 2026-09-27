import { apiClient } from "../client"
import type { ApiResponse } from "@/types/api"
import type {
  UserSummaryResponse,
  UserDetailResponse,
  CreateUserRequest,
  AssignRoleRequest,
} from "@/types/auth"

export const usersApi = {
  list: () =>
    apiClient
      .get<ApiResponse<UserSummaryResponse[]>>("/auth/users")
      .then((r) => r.data),

  get: (id: number) =>
    apiClient
      .get<ApiResponse<UserDetailResponse>>(`/auth/users/${id}`)
      .then((r) => r.data),

  create: (data: CreateUserRequest) =>
    apiClient
      .post<ApiResponse<UserDetailResponse>>("/auth/users", data)
      .then((r) => r.data),

  assignRole: (id: number, data: AssignRoleRequest) =>
    apiClient
      .put<ApiResponse<UserDetailResponse>>(`/auth/users/${id}/role`, data)
      .then((r) => r.data),

  enable: (id: number) =>
    apiClient
      .post<ApiResponse<UserDetailResponse>>(`/auth/users/${id}/enable`)
      .then((r) => r.data),

  disable: (id: number) =>
    apiClient
      .post<ApiResponse<UserDetailResponse>>(`/auth/users/${id}/disable`)
      .then((r) => r.data),
}
