import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { Link } from "react-router-dom"
import { Loader2, UserPlus } from "lucide-react"
import { AuthLayout } from "../components/AuthLayout"
import { registerSchema, type RegisterFormData } from "../schemas"
import { useRegister } from "../hooks"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"

export function RegisterPage() {
  const registerMutation = useRegister()

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      username: "",
      email: "",
      password: "",
      mobileNumber: "",
      firstName: "",
      lastName: "",
    },
  })

  const onSubmit = (data: RegisterFormData) => {
    // Remove empty strings
    const cleaned = Object.fromEntries(
      Object.entries(data).filter(([_, v]) => v !== "")
    )
    registerMutation.mutate(cleaned)
  }

  return (
    <AuthLayout
      title="Create an account"
      description="Enter your details to get started"
      footer={
        <>
          Already have an account?{" "}
          <Link to="/login" className="font-medium text-primary hover:underline">
            Sign in
          </Link>
        </>
      }
    >
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <div className="space-y-2">
          <Label htmlFor="username">Username *</Label>
          <Input
            id="username"
            type="text"
            placeholder="johndoe"
            autoComplete="username"
            {...register("username")}
            disabled={isSubmitting}
          />
          {errors.username && (
            <p className="text-sm text-destructive">{errors.username.message}</p>
          )}
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div className="space-y-2">
            <Label htmlFor="firstName">First name</Label>
            <Input
              id="firstName"
              type="text"
              placeholder="John"
              {...register("firstName")}
              disabled={isSubmitting}
            />
            {errors.firstName && (
              <p className="text-sm text-destructive">{errors.firstName.message}</p>
            )}
          </div>
          <div className="space-y-2">
            <Label htmlFor="lastName">Last name</Label>
            <Input
              id="lastName"
              type="text"
              placeholder="Doe"
              {...register("lastName")}
              disabled={isSubmitting}
            />
            {errors.lastName && (
              <p className="text-sm text-destructive">{errors.lastName.message}</p>
            )}
          </div>
        </div>

        <div className="space-y-2">
          <Label htmlFor="email">Email</Label>
          <Input
            id="email"
            type="email"
            placeholder="you@example.com"
            autoComplete="email"
            {...register("email")}
            disabled={isSubmitting}
          />
          {errors.email && (
            <p className="text-sm text-destructive">{errors.email.message}</p>
          )}
        </div>

        <div className="space-y-2">
          <Label htmlFor="mobileNumber">Mobile number</Label>
          <Input
            id="mobileNumber"
            type="tel"
            placeholder="+989123456789"
            autoComplete="tel"
            {...register("mobileNumber")}
            disabled={isSubmitting}
          />
          {errors.mobileNumber && (
            <p className="text-sm text-destructive">{errors.mobileNumber.message}</p>
          )}
        </div>

        <div className="space-y-2">
          <Label htmlFor="password">Password *</Label>
          <Input
            id="password"
            type="password"
            placeholder="••••••••"
            autoComplete="new-password"
            {...register("password")}
            disabled={isSubmitting}
          />
          {errors.password && (
            <p className="text-sm text-destructive">{errors.password.message}</p>
          )}
          <p className="text-xs text-muted-foreground">At least 8 characters</p>
        </div>

        <Button
          type="submit"
          className="w-full"
          disabled={isSubmitting || registerMutation.isPending}
        >
          {registerMutation.isPending ? (
            <>
              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              Creating account...
            </>
          ) : (
            <>
              <UserPlus className="mr-2 h-4 w-4" />
              Create account
            </>
          )}
        </Button>
      </form>
    </AuthLayout>
  )
}
