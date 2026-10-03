import { useState } from "react"
import { useQuery } from "@tanstack/react-query"
import { notifApi, type NotifSearchParams } from "@/api/endpoints/notif"
import { queryKeys } from "@/api/queryKeys"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle,
} from "@/components/ui/dialog"
import {
  Bell, Mail, MessageSquare, Smartphone, ChevronLeft, ChevronRight, ExternalLink, AlertCircle,
} from "lucide-react"
import { formatRelativeTime, formatDate } from "@/lib/utils"
import type { Notification } from "@/types/notif"

export function NotificationsPage() {
  const [page, setPage] = useState(0)
  const [channel, setChannel] = useState("")
  const [status, setStatus] = useState("")
  const [selected, setSelected] = useState<Notification | null>(null)

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
        <h1 className="text-3xl font-bold">Delivery Notifications</h1>
        <p className="text-muted-foreground">
          {totalElements} notification(s) processed by notif-service
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
          <CardDescription>Page {page + 1} of {Math.max(totalPages, 1)} — click a row for details</CardDescription>
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
                  <TableHead>Event ID</TableHead>
                  <TableHead>Recipient</TableHead>
                  <TableHead>Subject</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Created</TableHead>
                  <TableHead className="text-right"></TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {notifications.map((n) => (
                  <TableRow
                    key={n.notificationId}
                    className="cursor-pointer"
                    onClick={() => setSelected(n)}
                  >
                    <TableCell>
                      <div className="flex items-center gap-2">
                        {channelIcon(n.channel)}
                        <span className="text-sm">{n.channel}</span>
                      </div>
                    </TableCell>
                    <TableCell>
                      <code className="rounded bg-muted px-1.5 py-0.5 text-[10px] font-mono">
                        {n.eventId ? n.eventId.slice(0, 8) + "…" : "—"}
                      </code>
                    </TableCell>
                    <TableCell className="text-sm font-mono">
                      {n.recipient.length > 25
                        ? n.recipient.slice(0, 25) + "…"
                        : n.recipient}
                    </TableCell>
                    <TableCell className="text-sm text-muted-foreground">
                      {n.subject || "—"}
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
                    <TableCell className="text-right">
                      <Button variant="ghost" size="sm">
                        <ExternalLink className="h-4 w-4" />
                      </Button>
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

      {/* Details Dialog */}
      <Dialog open={!!selected} onOpenChange={(o) => !o && setSelected(null)}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              {selected && channelIcon(selected.channel)}
              Notification Details
            </DialogTitle>
            <DialogDescription className="font-mono text-xs">
              {selected?.notificationId}
            </DialogDescription>
          </DialogHeader>
          {selected && (
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <DetailRow label="Channel" value={selected.channel} />
                <DetailRow label="Status" value={selected.status} />
                <DetailRow label="Provider" value={selected.provider} />
                <DetailRow label="Event ID" value={selected.eventId || "—"} mono />
                <DetailRow label="Recipient" value={selected.recipient} mono />
                <DetailRow label="Provider Msg ID" value={selected.providerMessageId || "—"} mono />
                <DetailRow label="Created" value={formatDate(selected.createdAt)} />
                <DetailRow
                  label="Sent"
                  value={selected.sentAt ? formatDate(selected.sentAt) : "—"}
                />
              </div>

              {selected.subject && (
                <div>
                  <p className="mb-1 text-sm font-medium">Subject</p>
                  <p className="rounded-md bg-muted p-3 text-sm">{selected.subject}</p>
                </div>
              )}

              {selected.errorMessage && (
                <div>
                  <p className="mb-1 flex items-center gap-1 text-sm font-medium text-destructive">
                    <AlertCircle className="h-4 w-4" />
                    Error
                  </p>
                  <pre className="overflow-x-auto rounded-md bg-destructive/10 p-3 text-xs text-destructive">
                    {selected.errorMessage}
                  </pre>
                </div>
              )}

              {selected.metadata && Object.keys(selected.metadata).length > 0 && (
                <div>
                  <p className="mb-1 text-sm font-medium">Metadata</p>
                  <pre className="overflow-x-auto rounded-md bg-muted p-3 text-xs">
                    {JSON.stringify(selected.metadata, null, 2)}
                  </pre>
                </div>
              )}
            </div>
          )}
        </DialogContent>
      </Dialog>
    </div>
  )
}

function DetailRow({
  label,
  value,
  mono = false,
}: {
  label: string
  value: string
  mono?: boolean
}) {
  return (
    <div>
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className={`text-sm ${mono ? "font-mono break-all" : ""}`}>{value}</p>
    </div>
  )
}
