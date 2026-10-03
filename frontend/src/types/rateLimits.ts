export interface RateLimit {
  id: number
  pathPattern: string
  method?: string
  keyType: "IP" | "IP_PATH" | "USER" | "USER_PATH"
  requestsPerWindow: number
  windowSeconds: number
  burstCapacity?: number
  defaultRequestsPerWindow: number
  defaultWindowSeconds: number
  defaultBurstCapacity?: number
  defaultEnabled: boolean
  enabled: boolean
  priority: number
  description?: string
  createdAt: string
  updatedAt: string
}

export interface RateLimitRequest {
  pathPattern: string
  method?: string
  keyType: "IP" | "IP_PATH" | "USER" | "USER_PATH"
  requestsPerWindow: number
  windowSeconds: number
  burstCapacity?: number
  priority: number
  description?: string
  enabled: boolean
}

export interface ActiveRateLimit {
  pathPattern: string
  method?: string
  keyType: "IP" | "IP_PATH" | "USER" | "USER_PATH"
  requestsPerWindow: number
  windowSeconds: number
  burstCapacity?: number
  priority: number
}
