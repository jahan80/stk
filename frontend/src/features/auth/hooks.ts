import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { useNavigate } from "react-router-dom"
import { toast } from "sonner"
import { authApi } from "@/api/endpoints/auth"
import { queryKeys } from "@/api/queryKeys"
import { useAuthStore } from "@/store/auth"
import type {
  LoginRequest,
  RegisterRequest,
  ForgotPasswordRequest,
  ResetPasswordRequest,
  VerifyEmailRequest,
  VerifyMobileRequest,
} from "@/types/auth"

export function useLogin() {
  const setSession = useAuthStore((s) => s.setSession)
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (data: LoginRequest) => authApi.login(data),
    onSuccess: (response) => {
      setSession(response.data)
      queryClient.invalidateQueries({ queryKey: queryKeys.auth.me })
      toast.success(`Welcome back, ${response.data.username}!`)
      navigate("/dashboard")
    },
    onError: (error: any) => {
      const msg = error?.response?.data?.message ?? "Login failed. Please try again."
      toast.error(msg)
    },
  })
}

export function useRegister() {
  const navigate = useNavigate()

  return useMutation({
    mutationFn: (data: RegisterRequest) => authApi.register(data),
    onSuccess: (_, variables) => {
      toast.success("Account created! Check your email for verification.")
      navigate(`/verify-email?email=${encodeURIComponent(variables.email ?? "")}`)
    },
    onError: (error: any) => {
      const msg = error?.response?.data?.message ?? "Registration failed. Try again."
      toast.error(msg)
    },
  })
}

export function useLogout() {
  const logout = useAuthStore((s) => s.logout)
  const refreshToken = useAuthStore((s) => s.refreshToken)
  const navigate = useNavigate()

  return useMutation({
    mutationFn: async () => {
      if (refreshToken) {
        try {
          await authApi.logout({ refreshToken })
        } catch {
          // ignore
        }
      }
    },
    onSettled: () => {
      logout()
      navigate("/login")
      toast.success("Logged out successfully")
    },
  })
}

export function useCurrentUser() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)

  return useQuery({
    queryKey: queryKeys.auth.me,
    queryFn: () => authApi.me(),
    enabled: isAuthenticated,
    staleTime: 60_000,
  })
}

export function useForgotPassword() {
  const navigate = useNavigate()

  return useMutation({
    mutationFn: (data: ForgotPasswordRequest) => authApi.forgotPassword(data),
    onSuccess: (_, variables) => {
      toast.success("Reset code sent! Check your email.")
      navigate(`/reset-password?email=${encodeURIComponent(variables.email)}`)
    },
    onError: (error: any) => {
      const msg = error?.response?.data?.message ?? "Failed to send reset code"
      toast.error(msg)
    },
  })
}

export function useResetPassword() {
  const navigate = useNavigate()

  return useMutation({
    mutationFn: (data: ResetPasswordRequest) => authApi.resetPassword(data),
    onSuccess: () => {
      toast.success("Password reset successfully! Please login.")
      navigate("/login")
    },
    onError: (error: any) => {
      const msg = error?.response?.data?.message ?? "Reset failed"
      toast.error(msg)
    },
  })
}

export function useVerifyEmail() {
  const navigate = useNavigate()

  return useMutation({
    mutationFn: (data: VerifyEmailRequest) => authApi.verifyEmail(data),
    onSuccess: () => {
      toast.success("Email verified! You can now login.")
      navigate("/login")
    },
    onError: (error: any) => {
      const msg = error?.response?.data?.message ?? "Verification failed"
      toast.error(msg)
    },
  })
}

export function useResendVerification() {
  return useMutation({
    mutationFn: (email: string) => authApi.resendEmailVerification(email),
    onSuccess: () => toast.success("Verification code resent! Check your email."),
    onError: (error: any) => {
      const msg = error?.response?.data?.message ?? "Failed to resend"
      toast.error(msg)
    },
  })
}

export function useVerifyMobile() {
  const navigate = useNavigate()

  return useMutation({
    mutationFn: (data: VerifyMobileRequest) => authApi.verifyMobile(data),
    onSuccess: () => {
      toast.success("Mobile verified successfully!")
      navigate("/profile")
    },
    onError: (error: any) => {
      const msg = error?.response?.data?.message ?? "Verification failed"
      toast.error(msg)
    },
  })
}

export function useResendMobileVerification() {
  return useMutation({
    mutationFn: (mobileNumber: string) => authApi.resendMobileVerification(mobileNumber),
    onSuccess: () => toast.success("Verification code resent! Check your SMS."),
    onError: (error: any) => {
      const msg = error?.response?.data?.message ?? "Failed to resend"
      toast.error(msg)
    },
  })
}
