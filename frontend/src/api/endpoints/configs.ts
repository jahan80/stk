import { apiClient } from "../client"
import type { ApiResponse } from "@/types/api"
import type { Configuration, ConfigurationRequest } from "@/types/configs"

export const configsApi = {
  list: () =>
    apiClient
      .get<ApiResponse<Configuration[]>>("/auth/configurations")
      .then((r) => r.data),

  get: (key: string) =>
    apiClient
      .get<ApiResponse<Configuration>>(`/auth/configurations/${key}`)
      .then((r) => r.data),

  create: (data: ConfigurationRequest) =>
    apiClient
      .post<ApiResponse<Configuration>>("/auth/configurations", data)
      .then((r) => r.data),

  update: (key: string, data: ConfigurationRequest) =>
    apiClient
      .put<ApiResponse<Configuration>>(`/auth/configurations/${key}`, data)
      .then((r) => r.data),

  delete: (key: string) =>
    apiClient
      .delete<ApiResponse<void>>(`/auth/configurations/${key}`)
      .then((r) => r.data),

  resetToDefault: (key: string) =>
    apiClient
      .post<ApiResponse<Configuration>>(
        `/auth/configurations/${key}/reset-default`
      )
      .then((r) => r.data),

  resetAllToDefault: () =>
    apiClient
      .post<ApiResponse<Configuration[]>>("/auth/configurations/reset-defaults")
      .then((r) => r.data),

  setCurrentAsDefault: (key: string) =>
    apiClient
      .post<ApiResponse<Configuration>>(
        `/auth/configurations/${key}/set-default`
      )
      .then((r) => r.data),
}
