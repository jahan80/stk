export interface LoginRequest {
  identifier: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  userId: number
  username: string
  email: string
  role: string
  emailVerified: boolean
  mobileVerified: boolean
  permissions: string[]
}

export interface RegisterRequest {
  username: string
  email?: string
  password: string
  mobileNumber?: string
  firstName?: string
  lastName?: string
}

export interface RegisterResponse {
  id: number
  username: string
  email?: string
  mobileNumber?: string
  firstName?: string
  lastName?: string
  role: string
  emailVerified: boolean
  mobileVerified: boolean
}

export interface UserResponse {
  id: number
  username: string
  email?: string
  mobileNumber?: string
  firstName?: string
  lastName?: string
  role: string
  enabled: boolean
  emailVerified: boolean
  mobileVerified: boolean
}

export interface UserDetailResponse extends UserResponse {
  createdAt: string
  updatedAt: string
}

export interface UserSummaryResponse {
  id: number
  username: string
  email?: string
  role: string
  enabled: boolean
}

export interface ForgotPasswordRequest {
  email: string
}

export interface ResetPasswordRequest {
  email: string
  code: string
  newPassword: string
}

export interface VerifyEmailRequest {
  email: string
  code: string
}

export interface VerifyMobileRequest {
  mobileNumber: string
  code: string
}

export interface RefreshTokenRequest {
  refreshToken: string
}

export interface CreateUserRequest {
  username: string
  password: string
  email?: string
  mobileNumber?: string
  firstName?: string
  lastName?: string
  roleId: number
}

export interface AssignRoleRequest {
  roleId: number
}
