import { Link } from "react-router-dom"
import { useAuthStore } from "@/store/auth"
import { useTickets } from "@/features/tickets/hooks"
import { useNotifications, useUnreadCount } from "@/features/notifications/hooks"
import { useActiveRateLimits } from "@/features/admin/hooks"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Separator } from "@/components/ui/separator"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import {
  User, Mail, Shield, CheckCircle2, XCircle,
  Ticket, Bell, Gauge, Activity, ArrowRight, Clock,
} from "lucide-react"
import { getInitials, formatRelativeTime, cn } from "@/lib/utils"
import { TicketStatusBadge, TicketPriorityBadge } from "@/features/tickets/components/TicketBadges"

export function DashboardPage() {
  const user = useAuthStore((s) => s.user)
  const isAdmin = user?.role === "ADMIN"

  // Recent tickets (5 latest)
  const { data: ticketsData, isLoading: ticketsLoading } = useTickets({ page: 0, size: 5 })

  // Unread notifications count
  const { data: unreadData } = useUnreadCount()
  const unreadCount = unreadData?.data?.count ?? 0

  // Recent notifications (5 latest)
  const { data: notifData, isLoading: notifLoading } = useNotifications({ size: 5 })
  const notifications = notifData?.data?.content ?? []

  // Active rate limits (admin only)
  const { data: activeLimitsData, isLoading: limitsLoading } = useActiveRateLimits()
  const activeLimits = isAdmin ? (activeLimitsData?.data ?? []) : []

  const tickets = ticketsData?.data?.content ?? []
  const totalTickets = ticketsData?.data?.totalElements ?? 0

  if (!user) {
    return <div>Loading...</div>
  }

  const accountStats = [
    {
      label: "Role",
      value: user.role,
      icon: Shield,
      color: "text-blue-500",
    },
    {
      label: "Email",
      value: user.emailVerified ? "Verified" : "Not verified",
      icon: user.emailVerified ? CheckCircle2 : XCircle,
      color: user.emailVerified ? "text-green-500" : "text-yellow-500",
    },
    {
      label: "Mobile",
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
              Here&apos;s an overview of your workspace
            </p>
          </div>
          <Badge variant={isAdmin ? "default" : "secondary"}>
            {user.role}
          </Badge>
        </CardContent>
      </Card>

      {/* Quick Stats */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <StatCard
          label="My Tickets"
          value={ticketsLoading ? "…" : totalTickets}
          icon={Ticket}
          color="text-blue-500"
          href="/tickets"
        />
        <StatCard
          label="Unread Notifications"
          value={unreadCount}
          icon={Bell}
          color="text-yellow-500"
          href="/notifications"
        />
        <StatCard
          label="Permissions"
          value={user.permissions.length}
          icon={Shield}
          color="text-purple-500"
        />
        <StatCard
          label={isAdmin ? "Active Rate Limits" : "Email Status"}
          value={
            isAdmin
              ? (limitsLoading ? "…" : activeLimits.length)
              : (user.emailVerified ? "Verified" : "Unverified")
          }
          icon={isAdmin ? Gauge : Mail}
          color={isAdmin ? "text-orange-500" : (user.emailVerified ? "text-green-500" : "text-red-500")}
          href={isAdmin ? "/rate-limits" : undefined}
        />
      </div>

      {/* Two-column layout */}
      <div className="grid gap-6 lg:grid-cols-2">
        {/* Recent Tickets */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-3">
            <div>
              <CardTitle className="flex items-center gap-2">
                <Ticket className="h-5 w-5" />
                Recent Tickets
              </CardTitle>
              <CardDescription>Latest 5 tickets</CardDescription>
            </div>
            <Button variant="ghost" size="sm" asChild>
              <Link to="/tickets">
                View all
                <ArrowRight className="ml-1 h-3 w-3" />
              </Link>
            </Button>
          </CardHeader>
          <CardContent>
            {ticketsLoading ? (
              <div className="space-y-2">
                {[1, 2, 3].map((i) => <Skeleton key={i} className="h-12 w-full" />)}
              </div>
            ) : tickets.length === 0 ? (
              <div className="py-8 text-center text-sm text-muted-foreground">
                <Ticket className="mx-auto mb-2 h-8 w-8 opacity-50" />
                No tickets yet
                <div className="mt-2">
                  <Button variant="link" size="sm" asChild>
                    <Link to="/tickets/new">Create your first ticket</Link>
                  </Button>
                </div>
              </div>
            ) : (
              <div className="space-y-2">
                {tickets.map((t) => (
                  <Link
                    key={t.id}
                    to={`/tickets/${t.id}`}
                    className="flex items-center gap-3 rounded-md border p-3 transition-colors hover:bg-accent"
                  >
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center gap-2">
                        <span className="font-mono text-xs text-muted-foreground">
                          {t.ticketNumber}
                        </span>
                        <TicketStatusBadge status={t.status} />
                        <TicketPriorityBadge priority={t.priority} />
                      </div>
                      <p className="mt-1 truncate text-sm font-medium">{t.title}</p>
                    </div>
                    <span className="shrink-0 text-xs text-muted-foreground">
                      {formatRelativeTime(t.createdAt)}
                    </span>
                  </Link>
                ))}
              </div>
            )}
          </CardContent>
        </Card>

        {/* Recent Notifications */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-3">
            <div>
              <CardTitle className="flex items-center gap-2">
                <Bell className="h-5 w-5" />
                Recent Notifications
                {unreadCount > 0 && (
                  <Badge variant="destructive" className="text-xs">
                    {unreadCount} new
                  </Badge>
                )}
              </CardTitle>
              <CardDescription>Latest updates</CardDescription>
            </div>
            <Button variant="ghost" size="sm" asChild>
              <Link to="/notifications">
                View all
                <ArrowRight className="ml-1 h-3 w-3" />
              </Link>
            </Button>
          </CardHeader>
          <CardContent>
            {notifLoading ? (
              <div className="space-y-2">
                {[1, 2, 3].map((i) => <Skeleton key={i} className="h-12 w-full" />)}
              </div>
            ) : notifications.length === 0 ? (
              <div className="py-8 text-center text-sm text-muted-foreground">
                <Bell className="mx-auto mb-2 h-8 w-8 opacity-50" />
                No notifications
              </div>
            ) : (
              <div className="space-y-2">
                {notifications.map((n) => (
                  <div
                    key={n.id}
                    className={cn(
                      "flex items-start gap-3 rounded-md border p-3",
                      !n.read && "bg-primary/5"
                    )}
                  >
                    <span
                      className={cn(
                        "mt-1.5 block h-2 w-2 shrink-0 rounded-full",
                        !n.read ? "bg-primary" : "bg-transparent"
                      )}
                    />
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-medium">{n.title}</p>
                      {n.message && (
                        <p className="line-clamp-2 text-xs text-muted-foreground">
                          {n.message}
                        </p>
                      )}
                      <p className="mt-1 text-[10px] text-muted-foreground">
                        {formatRelativeTime(n.createdAt)}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Admin: Active Rate Limits */}
      {isAdmin && activeLimits.length > 0 && (
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-3">
            <div>
              <CardTitle className="flex items-center gap-2">
                <Activity className="h-5 w-5" />
                Active Rate Limits
              </CardTitle>
              <CardDescription>
                Currently enforced at gateway ({activeLimits.length})
              </CardDescription>
            </div>
            <Button variant="ghost" size="sm" asChild>
              <Link to="/rate-limits">
                Manage
                <ArrowRight className="ml-1 h-3 w-3" />
              </Link>
            </Button>
          </CardHeader>
          <CardContent>
            {limitsLoading ? (
              <Skeleton className="h-20 w-full" />
            ) : (
              <div className="flex flex-wrap gap-2">
                {activeLimits.slice(0, 10).map((l, i) => (
                  <div
                    key={i}
                    className="flex items-center gap-2 rounded-md border bg-muted/30 px-3 py-1.5 text-xs"
                  >
                    {l.method && <Badge variant="outline" className="text-[10px]">{l.method}</Badge>}
                    <span className="font-mono">{l.pathPattern}</span>
                    <span className="text-muted-foreground">
                      {l.requestsPerWindow}/{l.windowSeconds}s
                    </span>
                  </div>
                ))}
                {activeLimits.length > 10 && (
                  <Badge variant="secondary">+{activeLimits.length - 10} more</Badge>
                )}
              </div>
            )}
          </CardContent>
        </Card>
      )}

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
              <Shield className="h-4 w-4 text-muted-foreground" />
              <div>
                <p className="text-sm text-muted-foreground">Role</p>
                <p className="font-medium">{user.role}</p>
              </div>
            </div>
            <div className="flex items-center gap-3">
              <Clock className="h-4 w-4 text-muted-foreground" />
              <div>
                <p className="text-sm text-muted-foreground">Session</p>
                <p className="font-medium">Active</p>
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

      {/* Small stats footer (account) */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        {accountStats.map((stat) => (
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
    </div>
  )
}

// =====================================================
// StatCard Component
// =====================================================
function StatCard({
  label,
  value,
  icon: Icon,
  color,
  href,
}: {
  label: string
  value: string | number
  icon: React.ComponentType<{ className?: string }>
  color: string
  href?: string
}) {
  const content = (
    <Card className={cn(href && "cursor-pointer transition-colors hover:bg-accent/50")}>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
        <CardTitle className="text-sm font-medium text-muted-foreground">
          {label}
        </CardTitle>
        <Icon className={cn("h-4 w-4", color)} />
      </CardHeader>
      <CardContent>
        <div className="text-2xl font-bold">{value}</div>
      </CardContent>
    </Card>
  )

  if (href) {
    return <Link to={href}>{content}</Link>
  }
  return content
}
