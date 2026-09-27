import { useAuthStore } from "@/store/auth"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Separator } from "@/components/ui/separator"
import { User, Mail, Smartphone, Shield, CheckCircle2, XCircle, Calendar } from "lucide-react"
import { getInitials, formatDate } from "@/lib/utils"

export function DashboardPage() {
  const user = useAuthStore((s) => s.user)

  if (!user) {
    return <div>Loading...</div>
  }

  const stats = [
    {
      label: "Role",
      value: user.role,
      icon: Shield,
      color: "text-blue-500",
    },
    {
      label: "Email verified",
      value: user.emailVerified ? "Verified" : "Not verified",
      icon: user.emailVerified ? CheckCircle2 : XCircle,
      color: user.emailVerified ? "text-green-500" : "text-yellow-500",
    },
    {
      label: "Mobile verified",
      value: user.mobileVerified ? "Verified" : "Not verified",
      icon: user.mobileVerified ? CheckCircle2 : XCircle,
      color: user.mobileVerified ? "text-green-500" : "text-yellow-500",
    },
    {
      label: "Permissions",
      value: user.permissions.length,
      icon: Shield,
      color: "text-purple-500",
    },
  ]

  return (
    <div className="space-y-6">
      {/* Welcome Card */}
      <Card>
        <CardContent className="flex items-center gap-4 pt-6">
          <Avatar className="h-16 w-16">
            <AvatarFallback className="bg-primary text-primary-foreground text-xl">
              {getInitials(user.username)}
            </AvatarFallback>
          </Avatar>
          <div className="flex-1">
            <h1 className="text-2xl font-bold">Welcome back, {user.username}!</h1>
            <p className="text-muted-foreground">
              Here&apos;s an overview of your account
            </p>
          </div>
          <Badge variant={user.role === "ADMIN" ? "default" : "secondary"}>
            {user.role}
          </Badge>
        </CardContent>
      </Card>

      {/* Stats Grid */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        {stats.map((stat) => (
          <Card key={stat.label}>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">
                {stat.label}
              </CardTitle>
              <stat.icon className={`h-4 w-4 ${stat.color}`} />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{stat.value}</div>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Account Info */}
      <Card>
        <CardHeader>
          <CardTitle>Account Information</CardTitle>
          <CardDescription>Your personal details</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid gap-4 md:grid-cols-2">
            <div className="flex items-center gap-3">
              <User className="h-4 w-4 text-muted-foreground" />
              <div>
                <p className="text-sm text-muted-foreground">Username</p>
                <p className="font-medium">{user.username}</p>
              </div>
            </div>
            <div className="flex items-center gap-3">
              <Mail className="h-4 w-4 text-muted-foreground" />
              <div>
                <p className="text-sm text-muted-foreground">Email</p>
                <p className="font-medium">{user.email || "—"}</p>
              </div>
            </div>
            <div className="flex items-center gap-3">
              <Smartphone className="h-4 w-4 text-muted-foreground" />
              <div>
                <p className="text-sm text-muted-foreground">Mobile</p>
                <p className="font-medium">{user.email ? "—" : "—"}</p>
              </div>
            </div>
            <div className="flex items-center gap-3">
              <Shield className="h-4 w-4 text-muted-foreground" />
              <div>
                <p className="text-sm text-muted-foreground">Role</p>
                <p className="font-medium">{user.role}</p>
              </div>
            </div>
          </div>

          {user.permissions.length > 0 && (
            <>
              <Separator />
              <div>
                <p className="mb-2 text-sm text-muted-foreground">Permissions</p>
                <div className="flex flex-wrap gap-2">
                  {user.permissions.map((p) => (
                    <Badge key={p} variant="outline">
                      {p}
                    </Badge>
                  ))}
                </div>
              </div>
            </>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
