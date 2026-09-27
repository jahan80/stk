export const queryKeys = {
  auth: {
    me: ["auth", "me"] as const,
  },
  users: {
    all: ["users"] as const,
    detail: (id: number) => ["users", id] as const,
  },
  roles: {
    all: ["roles"] as const,
    detail: (id: number) => ["roles", id] as const,
    permissions: ["permissions"] as const,
  },
  configs: {
    all: ["configs"] as const,
    detail: (key: string) => ["configs", key] as const,
  },
  audit: {
    all: ["audit"] as const,
    byTrace: (traceId: string) => ["audit", "trace", traceId] as const,
  },
  notif: {
    all: ["notif"] as const,
    detail: (id: string) => ["notif", id] as const,
  },
  rateLimits: {
    all: ["rate-limits"] as const,
    detail: (id: number) => ["rate-limits", id] as const,
  },
}
