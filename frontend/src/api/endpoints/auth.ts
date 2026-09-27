import { apiClient } from "../client"
import type { ApiResponse } from "@/types/api"
import type {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  RegisterResponse,
  UserResponse,
  ForgotPasswordRequest,
  ResetPasswordRequest,
  VerifyEmailRequest,
  VerifyMobileRequest,
  RefreshTokenRequest,
} from "@/types/auth"

export const authApi = {
  login: (data: LoginRequest) =>
    apiClient
      .post<ApiResponse<LoginResponse>>("/auth/login", data)
      .then((r) => r.data),

  register: (data: RegisterRequest) =>
    apiClient
      .post<ApiResponse<RegisterResponse>>("/auth/register", data)
      .then((r) => r.data),

  me: () =>
    apiClient.get<ApiResponse<UserResponse>>("/auth/me").then((r) => r.data),

  refresh: (data: RefreshTokenRequest) =>
    apiClient
      .post<ApiResponse<LoginResponse>>("/auth/refresh", data)
      .then((r) => r.data),

  logout: (data: RefreshTokenRequest) =>
    apiClient.post<ApiResponse<void>>("/auth/logout", data).then((r) => r.data),

  forgotPassword: (data: ForgotPasswordRequest) =>
    apiClient
      .post<ApiResponse<void>>("/auth/password/forgot", data)
      .then((r) => r.data),

  resetPassword: (data: ResetPasswordRequest) =>
    apiClient
      .post<ApiResponse<void>>("/auth/password/reset", data)
      .then((r) => r.data),

  verifyEmail: (data: VerifyEmailRequest) =>
    apiClient
      .post<ApiResponse<void>>("/auth/email/verify", data)
      .then((r) => r.data),

  resendEmailVerification: (email: string) =>
    apiClient
      .post<ApiResponse<void>>("/auth/email/resend-verification", { email })
      .then((r) => r.data),

  verifyMobile: (data: VerifyMobileRequest) =>
    apiClient
      .post<ApiResponse<void>>("/auth/mobile/verify", data)
      .then((r) => r.data),

  resendMobileVerification: (mobileNumber: string) =>
    apiClient
      .post<ApiResponse<void>>("/auth/mobile/resend-verification", {
        mobileNumber,
      })
      .then((r) => r.data),
}
