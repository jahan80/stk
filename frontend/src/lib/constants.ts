export const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080"

export const ROLES = {
  ADMIN: "ADMIN",
  USER: "USER",
} as const

export type RoleName = keyof typeof ROLES

export const STORAGE_KEYS = {
  AUTH: "starterkit-auth",
  THEME: "starterkit-theme",
} as const

export const QUERY_STALE_TIME = {
  SHORT: 30 * 1000,        // 30s
  MEDIUM: 5 * 60 * 1000,   // 5m
  LONG: 30 * 60 * 1000,    // 30m
} as const
