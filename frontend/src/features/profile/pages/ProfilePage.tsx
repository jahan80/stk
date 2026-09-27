import { useCurrentUser } from "@/features/auth/hooks"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Separator } from "@/components/ui/separator"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Skeleton } from "@/components/ui/skeleton"
import { Button } from "@/components/ui/button"
import { PreferencesCard } from "../components/PreferencesCard"
import { CheckCircle2, XCircle, Mail, Smartphone, User as UserIcon, Shield, ExternalLink } from "lucide-react"
import { getInitials } from "@/lib/utils"
import { Link } from "react-router-dom"

export function ProfilePage() {
  const { data, isLoading } = useCurrentUser()
  const user = data?.data

  if (isLoading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-48 w-full" />
        <Skeleton className="h-64 w-full" />
      </div>
    )
  }

  if (!user) return <div>No user data</div>

  return (
    <div className="space-y-6">
      {/* Profile Header */}
      <Card>
        <CardContent className="flex items-center gap-6 pt-6">
          <Avatar className="h-24 w-24">
            <AvatarFallback className="bg-primary text-primary-foreground text-3xl">
              {getInitials(user.username)}
            </AvatarFallback>
          </Avatar>
          <div className="flex-1">
            <div className="flex items-center gap-3">
              <h1 className="text-3xl font-bold">{user.username}</h1>
              <Badge variant={user.role === "ADMIN" ? "default" : "secondary"}>
                {user.role}
              </Badge>
            </div>
            <p className="mt-1 text-muted-foreground">
              {user.firstName && user.lastName
                ? `${user.firstName} ${user.lastName}`
                : "No name set"}
            </p>
            <div className="mt-3 flex gap-2">
              <Button variant="outline" size="sm" asChild>
                <Link to={`/u/${user.username}`}>
                  <ExternalLink className="mr-2 h-4 w-4" />
                  View public profile
                </Link>
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Tabs */}
      <Tabs defaultValue="overview">
        <TabsList>
          <TabsTrigger value="overview">Overview</TabsTrigger>
          <TabsTrigger value="security">Security</TabsTrigger>
          <TabsTrigger value="preferences">Preferences</TabsTrigger>
        </TabsList>

        <TabsContent value="overview" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>Personal Information</CardTitle>
              <CardDescription>Your account details</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <InfoRow icon={UserIcon} label="Username" value={user.username} />
              <Separator />
              <InfoRow icon={Mail} label="Email" value={user.email || "—"} />
              <Separator />
              <InfoRow icon={Smartphone} label="Mobile" value={user.mobileNumber || "—"} />
              <Separator />
              <InfoRow icon={Shield} label="Role" value={user.role} />
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="security" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>Verification Status</CardTitle>
              <CardDescription>Your account verification</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <VerificationRow
                icon={Mail}
                label="Email"
                value={user.email || "—"}
                verified={user.emailVerified}
              />
              <Separator />
              <VerificationRow
                icon={Smartphone}
                label="Mobile"
                value={user.mobileNumber || "—"}
                verified={user.mobileVerified}
              />
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Account Status</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="flex items-center gap-3">
                {user.enabled ? (
                  <>
                    <CheckCircle2 className="h-5 w-5 text-green-500" />
                    <span className="font-medium">Account is active</span>
                  </>
                ) : (
                  <>
                    <XCircle className="h-5 w-5 text-red-500" />
                    <span className="font-medium">Account is disabled</span>
                  </>
                )}
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="preferences" className="space-y-4">
          <PreferencesCard />
        </TabsContent>
      </Tabs>
    </div>
  )
}

function InfoRow({ icon: Icon, label, value }: { icon: React.ElementType; label: string; value: string }) {
  return (
    <div className="flex items-center gap-3">
      <Icon className="h-4 w-4 text-muted-foreground" />
      <div className="flex-1">
        <p className="text-sm text-muted-foreground">{label}</p>
        <p className="font-medium">{value}</p>
      </div>
    </div>
  )
}

function VerificationRow({
  icon: Icon, label, value, verified,
}: {
  icon: React.ElementType
  label: string
  value: string
  verified: boolean
}) {
  return (
    <div className="flex items-center gap-3">
      <Icon className="h-4 w-4 text-muted-foreground" />
      <div className="flex-1">
        <p className="text-sm text-muted-foreground">{label}</p>
        <p className="font-medium">{value}</p>
      </div>
      {verified ? (
        <Badge variant="success">
          <CheckCircle2 className="mr-1 h-3 w-3" />
          Verified
        </Badge>
      ) : (
        <Badge variant="warning">
          <XCircle className="mr-1 h-3 w-3" />
          Not verified
        </Badge>
      )}
    </div>
  )
}
