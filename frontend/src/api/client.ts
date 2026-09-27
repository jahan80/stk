import axios, { AxiosError, type InternalAxiosRequestConfig } from "axios"
import { useAuthStore } from "@/store/auth"
import { API_BASE_URL } from "@/lib/constants"
import type { ApiResponse } from "@/types/api"
import { toast } from "sonner"

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: { "Content-Type": "application/json" },
  timeout: 15000,
})

// =====================================================
// Request interceptor: attach access token
// =====================================================
apiClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = useAuthStore.getState().accessToken
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// =====================================================
// Response interceptor: 401 → refresh token
// =====================================================
let isRefreshing = false
let pendingRequests: Array<(token: string) => void> = []

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiResponse<unknown>>) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & {
      _retry?: boolean
    }

    // Skip auth endpoints (don't try to refresh on login failure)
    const isAuthEndpoint =
      originalRequest.url?.includes("/auth/login") ||
      originalRequest.url?.includes("/auth/register") ||
      originalRequest.url?.includes("/auth/refresh")

    // 401 → try refresh
    if (
      error.response?.status === 401 &&
      !originalRequest._retry &&
      !isAuthEndpoint
    ) {
      if (isRefreshing) {
        return new Promise((resolve) => {
          pendingRequests.push((token: string) => {
            if (originalRequest.headers) {
              originalRequest.headers.Authorization = `Bearer ${token}`
            }
            resolve(apiClient(originalRequest))
          })
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        const refreshToken = useAuthStore.getState().refreshToken
        if (!refreshToken) throw new Error("No refresh token")

        const { data } = await axios.post<
          ApiResponse<{
            accessToken: string
            refreshToken: string
            expiresIn: number
          }>
        >(`${API_BASE_URL}/auth/refresh`, { refreshToken })

        const newAccessToken = data.data.accessToken
        const newRefreshToken = data.data.refreshToken

        useAuthStore.getState().setTokens(newAccessToken, newRefreshToken)

        pendingRequests.forEach((cb) => cb(newAccessToken))
        pendingRequests = []

        if (originalRequest.headers) {
          originalRequest.headers.Authorization = `Bearer ${newAccessToken}`
        }
        return apiClient(originalRequest)
      } catch {
        useAuthStore.getState().logout()
        if (window.location.pathname !== "/login") {
          window.location.href = "/login"
        }
        return Promise.reject(error)
      } finally {
        isRefreshing = false
      }
    }

    // Show error toast (skip for auth/me on 401)
    const apiError = error.response?.data
    if (
      apiError &&
      typeof apiError === "object" &&
      "success" in apiError &&
      !apiError.success &&
      error.response?.status !== 401
    ) {
      toast.error(apiError.message || "Something went wrong")
    }

    return Promise.reject(error)
  }
)
