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
  notifications: {
    all: ["notifications"] as const,
    list: (params: any) => ["notifications", "list", params] as const,
    unreadCount: ["notifications", "unread-count"] as const,
  },
  tickets: {
    all: ["tickets"] as const,
    list: (params: any) => ["tickets", "list", params] as const,
    detail: (id: number) => ["tickets", id] as const,
    comments: (id: number) => ["tickets", id, "comments"] as const,
    categories: ["tickets", "categories"] as const,
    groups: ["tickets", "groups"] as const,
    myGroups: ["tickets", "groups", "my"] as const,
    groupDetail: (id: number) => ["tickets", "groups", id] as const,
    groupMembers: (id: number) => ["tickets", "groups", id, "members"] as const,
  },
  rateLimits: {
    all: ["rate-limits"] as const,
    detail: (id: number) => ["rate-limits", id] as const,
  },
}
