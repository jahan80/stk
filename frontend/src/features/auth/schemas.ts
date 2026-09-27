import { z } from "zod"

export const loginSchema = z.object({
  identifier: z.string().min(1, "Username, email or mobile is required"),
  password: z.string().min(8, "Password must be at least 8 characters"),
})

export type LoginFormData = z.infer<typeof loginSchema>

export const registerSchema = z.object({
  username: z.string().min(3, "Username must be at least 3 characters").max(100),
  email: z.string().email("Invalid email address").optional().or(z.literal("")),
  password: z.string().min(8, "Password must be at least 8 characters").max(255),
  mobileNumber: z.string().regex(/^\+?[0-9]{8,20}$/, "Invalid mobile number").optional().or(z.literal("")),
  firstName: z.string().max(100).optional().or(z.literal("")),
  lastName: z.string().max(100).optional().or(z.literal("")),
})

export type RegisterFormData = z.infer<typeof registerSchema>

export const forgotPasswordSchema = z.object({
  email: z.string().email("Invalid email address"),
})

export type ForgotPasswordFormData = z.infer<typeof forgotPasswordSchema>

export const resetPasswordSchema = z.object({
  email: z.string().email("Invalid email address"),
  code: z.string().min(4, "Code is required").max(10),
  newPassword: z.string().min(8, "Password must be at least 8 characters"),
})

export type ResetPasswordFormData = z.infer<typeof resetPasswordSchema>

export const verifyEmailSchema = z.object({
  email: z.string().email("Invalid email address"),
  code: z.string().min(4, "Code is required").max(10),
})

export type VerifyEmailFormData = z.infer<typeof verifyEmailSchema>
