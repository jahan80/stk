import { useParams, Link } from "react-router-dom"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Button } from "@/components/ui/button"
import { Separator } from "@/components/ui/separator"
import { CheckCircle2, Mail, Smartphone, User as UserIcon, Shield, ArrowLeft, Lock } from "lucide-react"
import { getInitials } from "@/lib/utils"

export function PublicProfilePage() {
  const { username } = useParams<{ username: string }>()

  // TODO: Call public API endpoint (not implemented yet in backend)
  // For now, show a placeholder with the username

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-background to-muted p-4">
      <Card className="w-full max-w-2xl">
        <CardHeader>
          <Button variant="ghost" size="sm" asChild className="w-fit">
            <Link to="/login">
              <ArrowLeft className="mr-2 h-4 w-4" />
              Back to login
            </Link>
          </Button>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="flex items-center gap-4">
            <Avatar className="h-20 w-20">
              <AvatarFallback className="bg-primary text-primary-foreground text-2xl">
                {getInitials(username)}
              </AvatarFallback>
            </Avatar>
            <div>
              <h1 className="text-2xl font-bold">{username}</h1>
              <Badge variant="secondary">Public Profile</Badge>
            </div>
          </div>

          <Separator />

          <div className="rounded-lg border border-dashed p-6 text-center text-muted-foreground">
            <Lock className="mx-auto mb-2 h-8 w-8" />
            <p className="font-medium">Public profile not available</p>
            <p className="mt-1 text-sm">
              This feature requires a backend endpoint that doesn't exist yet.
              <br />
              Coming in a future update.
            </p>
          </div>

          <div className="text-center text-xs text-muted-foreground">
            Username: <strong>{username}</strong>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
