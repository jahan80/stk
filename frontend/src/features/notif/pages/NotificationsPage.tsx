import { useState } from "react"
import { useQuery } from "@tanstack/react-query"
import { notifApi, type NotifSearchParams } from "@/api/endpoints/notif"
import { queryKeys } from "@/api/queryKeys"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Skeleton } from "@/components/ui/skeleton"
import { Bell, Mail, MessageSquare, Smartphone, ChevronLeft, ChevronRight } from "lucide-react"
import { formatRelativeTime } from "@/lib/utils"

export function NotificationsPage() {
  const [page, setPage] = useState(0)
  const [channel, setChannel] = useState("")
  const [status, setStatus] = useState("")

  const params: NotifSearchParams = {
    channel: channel || undefined,
    status: status || undefined,
    page,
    size: 20,
  }

  const { data, isLoading } = useQuery({
    queryKey: [...queryKeys.notif.all, params],
    queryFn: () => notifApi.search(params),
    staleTime: 15_000,
  })

  const notifications = data?.data?.content ?? []
  const totalPages = data?.data?.totalPages ?? 0
  const totalElements = data?.data?.totalElements ?? 0

  const channelIcon = (ch: string) => {
    switch (ch) {
      case "EMAIL": return <Mail className="h-4 w-4" />
      case "SMS": return <MessageSquare className="h-4 w-4" />
      case "PUSH": return <Smartphone className="h-4 w-4" />
      default: return <Bell className="h-4 w-4" />
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold">Notifications</h1>
        <p className="text-muted-foreground">
          {totalElements} notification(s) sent
        </p>
      </div>

      {/* Filters */}
      <div className="flex gap-3">
        <select
          value={channel}
          onChange={(e) => { setChannel(e.target.value); setPage(0) }}
          className="h-10 rounded-md border border-input bg-background px-3 text-sm"
        >
          <option value="">All channels</option>
          <option value="EMAIL">Email</option>
          <option value="SMS">SMS</option>
          <option value="PUSH">Push</option>
        </select>
        <select
          value={status}
          onChange={(e) => { setStatus(e.target.value); setPage(0) }}
          className="h-10 rounded-md border border-input bg-background px-3 text-sm"
        >
          <option value="">All statuses</option>
          <option value="PENDING">Pending</option>
          <option value="SENT">Sent</option>
          <option value="FAILED">Failed</option>
        </select>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Notification History</CardTitle>
          <CardDescription>Page {page + 1} of {Math.max(totalPages, 1)}</CardDescription>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="space-y-2">
              {[1, 2, 3, 4].map((i) => <Skeleton key={i} className="h-12 w-full" />)}
            </div>
          ) : notifications.length === 0 ? (
            <div className="py-12 text-center text-muted-foreground">
              <Bell className="mx-auto mb-2 h-8 w-8" />
              No notifications found
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Channel</TableHead>
                  <TableHead>Recipient</TableHead>
                  <TableHead>Subject</TableHead>
                  <TableHead>Provider</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Created</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {notifications.map((n) => (
                  <TableRow key={n.notificationId}>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        {channelIcon(n.channel)}
                        <span className="text-sm">{n.channel}</span>
                      </div>
                    </TableCell>
                    <TableCell className="text-sm font-mono">
                      {n.recipient.length > 30
                        ? n.recipient.slice(0, 30) + "…"
                        : n.recipient}
                    </TableCell>
                    <TableCell className="text-sm text-muted-foreground">
                      {n.subject || "—"}
                    </TableCell>
                    <TableCell>
                      <Badge variant="outline">{n.provider}</Badge>
                    </TableCell>
                    <TableCell>
                      <Badge
                        variant={
                          n.status === "SENT"
                            ? "success"
                            : n.status === "FAILED"
                            ? "destructive"
                            : "secondary"
                        }
                      >
                        {n.status}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-xs text-muted-foreground">
                      {formatRelativeTime(n.createdAt)}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}

          {totalPages > 1 && (
            <div className="mt-4 flex items-center justify-between">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
              >
                <ChevronLeft className="mr-1 h-4 w-4" />
                Previous
              </Button>
              <span className="text-sm text-muted-foreground">
                Page {page + 1} of {totalPages}
              </span>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => p + 1)}
                disabled={page + 1 >= totalPages}
              >
                Next
                <ChevronRight className="ml-1 h-4 w-4" />
              </Button>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
