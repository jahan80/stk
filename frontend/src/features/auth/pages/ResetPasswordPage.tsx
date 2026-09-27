import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { Link, useSearchParams } from "react-router-dom"
import { ArrowLeft, KeyRound, Loader2 } from "lucide-react"
import { AuthLayout } from "../components/AuthLayout"
import { resetPasswordSchema, type ResetPasswordFormData } from "../schemas"
import { useResetPassword } from "../hooks"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"

export function ResetPasswordPage() {
  const [searchParams] = useSearchParams()
  const email = searchParams.get("email") ?? ""
  const mutation = useResetPassword()

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ResetPasswordFormData>({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues: { email, code: "", newPassword: "" },
  })

  const onSubmit = (data: ResetPasswordFormData) => mutation.mutate(data)

  return (
    <AuthLayout
      title="Reset password"
      description="Enter the code we sent to your email and your new password"
      footer={
        <Link to="/login" className="inline-flex items-center gap-1 font-medium text-primary hover:underline">
          <ArrowLeft className="h-3 w-3" />
          Back to login
        </Link>
      }
    >
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <div className="space-y-2">
          <Label htmlFor="email">Email</Label>
          <Input id="email" type="email" {...register("email")} disabled={isSubmitting} />
          {errors.email && (
            <p className="text-sm text-destructive">{errors.email.message}</p>
          )}
        </div>

        <div className="space-y-2">
          <Label htmlFor="code">Reset code</Label>
          <Input
            id="code"
            type="text"
            placeholder="12345678"
            maxLength={10}
            {...register("code")}
            disabled={isSubmitting}
          />
          {errors.code && (
            <p className="text-sm text-destructive">{errors.code.message}</p>
          )}
        </div>

        <div className="space-y-2">
          <Label htmlFor="newPassword">New password</Label>
          <Input
            id="newPassword"
            type="password"
            placeholder="••••••••"
            autoComplete="new-password"
            {...register("newPassword")}
            disabled={isSubmitting}
          />
          {errors.newPassword && (
            <p className="text-sm text-destructive">{errors.newPassword.message}</p>
          )}
        </div>

        <Button type="submit" className="w-full" disabled={isSubmitting || mutation.isPending}>
          {mutation.isPending ? (
            <>
              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              Resetting...
            </>
          ) : (
            <>
              <KeyRound className="mr-2 h-4 w-4" />
              Reset password
            </>
          )}
        </Button>
      </form>
    </AuthLayout>
  )
}
