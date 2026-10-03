import { useState } from "react"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { Link, useSearchParams } from "react-router-dom"
import { ArrowLeft, Loader2, Smartphone, RefreshCw } from "lucide-react"
import { AuthLayout } from "../components/AuthLayout"
import { verifyMobileSchema, type VerifyMobileFormData } from "../schemas"
import { useVerifyMobile, useResendMobileVerification } from "../hooks"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"

export function VerifyMobilePage() {
  const [searchParams] = useSearchParams()
  const mobileNumber = searchParams.get("mobile") ?? ""
  const verifyMutation = useVerifyMobile()
  const resendMutation = useResendMobileVerification()
  const [resendCooldown, setResendCooldown] = useState(0)

  const {
    register,
    handleSubmit,
    getValues,
    formState: { errors, isSubmitting },
  } = useForm<VerifyMobileFormData>({
    resolver: zodResolver(verifyMobileSchema),
    defaultValues: { mobileNumber, code: "" },
  })

  const onSubmit = (data: VerifyMobileFormData) => verifyMutation.mutate(data)

  const handleResend = () => {
    const mobile = getValues("mobileNumber")
    if (!mobile) return
    resendMutation.mutate(mobile, {
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
      title="Verify your mobile"
      description="Enter the code we sent to your mobile number"
      footer={
        <Link to="/login" className="inline-flex items-center gap-1 font-medium text-primary hover:underline">
          <ArrowLeft className="h-3 w-3" />
          Back to login
        </Link>
      }
    >
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <div className="space-y-2">
          <Label htmlFor="mobileNumber">Mobile number</Label>
          <Input
            id="mobileNumber"
            type="tel"
            placeholder="+989123456789"
            {...register("mobileNumber")}
            disabled={isSubmitting}
          />
          {errors.mobileNumber && (
            <p className="text-sm text-destructive">{errors.mobileNumber.message}</p>
          )}
        </div>

        <div className="space-y-2">
          <Label htmlFor="code">Verification code</Label>
          <Input
            id="code"
            type="text"
            placeholder="123456"
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
              <Smartphone className="mr-2 h-4 w-4" />
              Verify mobile
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
