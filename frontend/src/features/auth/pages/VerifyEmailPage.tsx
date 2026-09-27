import { useState } from "react"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { Link, useSearchParams } from "react-router-dom"
import { ArrowLeft, Loader2, MailCheck, RefreshCw } from "lucide-react"
import { AuthLayout } from "../components/AuthLayout"
import { verifyEmailSchema, type VerifyEmailFormData } from "../schemas"
import { useVerifyEmail, useResendVerification } from "../hooks"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"

export function VerifyEmailPage() {
  const [searchParams] = useSearchParams()
  const email = searchParams.get("email") ?? ""
  const verifyMutation = useVerifyEmail()
  const resendMutation = useResendVerification()
  const [resendCooldown, setResendCooldown] = useState(0)

  const {
    register,
    handleSubmit,
    getValues,
    formState: { errors, isSubmitting },
  } = useForm<VerifyEmailFormData>({
    resolver: zodResolver(verifyEmailSchema),
    defaultValues: { email, code: "" },
  })

  const onSubmit = (data: VerifyEmailFormData) => verifyMutation.mutate(data)

  const handleResend = () => {
    const email = getValues("email")
    if (!email) return
    resendMutation.mutate(email, {
      onSuccess: () => {
        setResendCooldown(60)
        const interval = setInterval(() => {
          setResendCooldown((c) => {
            if (c <= 1) { clearInterval(interval); return 0 }
            return c - 1
          })
        }, 1000)
      },
    })
  }

  return (
    <AuthLayout
      title="Verify your email"
      description="Enter the code we sent to your email address"
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
          <Label htmlFor="code">Verification code</Label>
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

        <Button type="submit" className="w-full" disabled={isSubmitting || verifyMutation.isPending}>
          {verifyMutation.isPending ? (
            <>
              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              Verifying...
            </>
          ) : (
            <>
              <MailCheck className="mr-2 h-4 w-4" />
              Verify email
            </>
          )}
        </Button>

        <Button
          type="button"
          variant="outline"
          className="w-full"
          onClick={handleResend}
          disabled={resendCooldown > 0 || resendMutation.isPending}
        >
          {resendMutation.isPending ? (
            <>
              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              Resending...
            </>
          ) : resendCooldown > 0 ? (
            `Resend in ${resendCooldown}s`
          ) : (
            <>
              <RefreshCw className="mr-2 h-4 w-4" />
              Resend code
            </>
          )}
        </Button>
      </form>
    </AuthLayout>
  )
}
