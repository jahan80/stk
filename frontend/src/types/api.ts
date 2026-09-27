export interface ApiResponse<T> {
  success: boolean
  code: string
  message: string
  data: T
  timestamp: string
  traceId: string
}

export interface ValidationErrorResponse {
  fields: Record<string, string>
}

export interface PaginatedResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
}
