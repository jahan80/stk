export interface Permission {
  id: number
  code: string
  description?: string
}

export interface Role {
  id: number
  name: string
  description?: string
  systemRole: boolean
  createdAt: string
  permissions: Permission[]
}

export interface RoleRequest {
  name: string
  description?: string
}
