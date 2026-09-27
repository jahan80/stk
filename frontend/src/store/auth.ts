import { create } from "zustand"
import { persist } from "zustand/middleware"
import type { LoginResponse } from "@/types/auth"
import { STORAGE_KEYS } from "@/lib/constants"

interface AuthState {
  accessToken: string | null
  refreshToken: string | null
  user: LoginResponse | null
  isAuthenticated: boolean

  setSession: (data: LoginResponse) => void
  setTokens: (accessToken: string, refreshToken: string) => void
  setUser: (user: LoginResponse) => void
  logout: () => void
  hasRole: (role: string) => boolean
  hasPermission: (permission: string) => boolean
  isAdmin: () => boolean
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      isAuthenticated: false,

      setSession: (data) =>
        set({
          accessToken: data.accessToken,
          refreshToken: data.refreshToken,
          user: data,
          isAuthenticated: true,
        }),

      setTokens: (accessToken, refreshToken) =>
        set({ accessToken, refreshToken }),

      setUser: (user) => set({ user }),

      logout: () =>
        set({
          accessToken: null,
          refreshToken: null,
          user: null,
          isAuthenticated: false,
        }),

      hasRole: (role) => get().user?.role === role,

      hasPermission: (permission) =>
        get().user?.permissions?.includes(permission) ?? false,

      isAdmin: () => get().user?.role === "ADMIN",
    }),
    {
      name: STORAGE_KEYS.AUTH,
      partialize: (state) => ({
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        user: state.user,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
)
