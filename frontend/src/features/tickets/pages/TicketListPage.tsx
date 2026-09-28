import { useState } from "react"
import { Link } from "react-router-dom"
import { useTickets } from "../hooks"
import { TicketStatusBadge, TicketPriorityBadge } from "../components/TicketBadges"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { Badge } from "@/components/ui/badge"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Plus, FileText, ChevronLeft, ChevronRight, Users, User, Inbox, Settings } from "lucide-react"
import { formatRelativeTime, cn } from "@/lib/utils"
import { useAuthStore } from "@/store/auth"
import type { TicketStatus, TicketPriority } from "@/types/tickets"

type AdminTab = "all" | "assigned" | "unassigned"

export function TicketListPage() {
  const user = useAuthStore((s) => s.user)
  const isAdmin = user?.role === "ADMIN"

  const [page, setPage] = useState(0)
  const [status, setStatus] = useState<TicketStatus | "">("")
  const [priority, setPriority] = useState<TicketPriority | "">("")
  const [tab, setTab] = useState<AdminTab>("all")

  const params = (() => {
    const base: any = {
      status: status || undefined,
      priority: priority || undefined,
      page,
      size: 20,
    }
    if (isAdmin && tab === "assigned") {
      return { ...base, assignedOnly: true }
    }
    if (isAdmin && tab === "unassigned") {
      return { ...base, unassignedOnly: true }
    }
    return base
  })()

  const { data, isLoading } = useTickets(params)

  const tickets = data?.data?.content ?? []
  const totalPages = data?.data?.totalPages ?? 0
  const totalElements = data?.data?.totalElements ?? 0

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">{isAdmin ? "Tickets" : "My Tickets"}</h1>
          <p className="text-muted-foreground">{totalElements} ticket(s)</p>
        </div>
        <div className="flex gap-2">
          {isAdmin && (
            <Button variant="outline" asChild>
              <Link to="/tickets/settings">
                <Settings className="mr-2 h-4 w-4" />
                Settings
              </Link>
            </Button>
          )}
          <Button asChild>
            <Link to="/tickets/new">
              <Plus className="mr-2 h-4 w-4" />
              New Ticket
            </Link>
          </Button>
        </div>
      </div>

      {isAdmin && (
        <div className="flex flex-wrap gap-2">
          <TabBtn active={tab === "all"} onClick={() => { setTab("all"); setPage(0) }}>
            <Users className="mr-1 h-3 w-3" />
            All
          </TabBtn>
          <TabBtn active={tab === "assigned"} onClick={() => { setTab("assigned"); setPage(0) }}>
            Assigned
          </TabBtn>
          <TabBtn active={tab === "unassigned"} onClick={() => { setTab("unassigned"); setPage(0) }}>
            <Inbox className="mr-1 h-3 w-3" />
            Unassigned
          </TabBtn>
        </div>
      )}

      <Card>
        <CardContent className="flex flex-wrap gap-3 pt-6">
          <select
            value={status}
            onChange={(e) => { setStatus(e.target.value as TicketStatus | ""); setPage(0) }}
            className="h-10 rounded-md border border-input bg-background px-3 text-sm"
          >
            <option value="">All statuses</option>
            <option value="OPEN">Open</option>
            <option value="IN_PROGRESS">In Progress</option>
            <option value="WAITING">Waiting</option>
            <option value="RESOLVED">Resolved</option>
            <option value="CLOSED">Closed</option>
          </select>
          <select
            value={priority}
            onChange={(e) => { setPriority(e.target.value as TicketPriority | ""); setPage(0) }}
            className="h-10 rounded-md border border-input bg-background px-3 text-sm"
          >
            <option value="">All priorities</option>
            <option value="LOW">Low</option>
            <option value="MEDIUM">Medium</option>
            <option value="HIGH">High</option>
            <option value="URGENT">Urgent</option>
          </select>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Tickets</CardTitle>
          <CardDescription>Page {page + 1} of {Math.max(totalPages, 1)}</CardDescription>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="space-y-2">
              {[1, 2, 3].map((i) => <Skeleton key={i} className="h-12 w-full" />)}
            </div>
          ) : tickets.length === 0 ? (
            <div className="py-12 text-center text-muted-foreground">
              <FileText className="mx-auto mb-2 h-8 w-8" />
              <p>No tickets found</p>
              <Button asChild variant="link">
                <Link to="/tickets/new">Create your first ticket</Link>
              </Button>
            </div>
          ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Number</TableHead>
                    <TableHead>Title</TableHead>
                    <TableHead>Category</TableHead>
                    <TableHead>Priority</TableHead>
                    <TableHead>Status</TableHead>
                    {isAdmin && <TableHead>Assigned</TableHead>}
                    <TableHead>Created</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {tickets.map((t) => (
                    <TableRow key={t.id}>
                      <TableCell>
                        <Link to={`/tickets/${t.id}`} className="font-mono text-xs text-primary hover:underline">
                          {t.ticketNumber}
                        </Link>
                      </TableCell>
                      <TableCell className="font-medium">{t.title}</TableCell>
                      <TableCell className="text-sm">
                        <Badge variant="outline">{t.categoryCode ?? "—"}</Badge>
                      </TableCell>
                      <TableCell><TicketPriorityBadge priority={t.priority} /></TableCell>
                      <TableCell><TicketStatusBadge status={t.status} /></TableCell>
                      {isAdmin && (
                        <TableCell>
                          {t.groupName ? (
                            <Badge variant="outline">👥 {t.groupName}</Badge>
                          ) : t.assignedTo ? (
                            <Badge variant="outline">User #{t.assignedTo}</Badge>
                          ) : (
                            <Badge variant="secondary">Unassigned</Badge>
                          )}
                        </TableCell>
                      )}
                      <TableCell className="text-xs text-muted-foreground">
                        {formatRelativeTime(t.createdAt)}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>

              {totalPages > 1 && (
                <div className="mt-4 flex items-center justify-between">
                  <Button variant="outline" size="sm" onClick={() => setPage((p) => p - 1)} disabled={page === 0}>
                    <ChevronLeft className="mr-1 h-4 w-4" />
                    Previous
                  </Button>
                  <span className="text-sm text-muted-foreground">Page {page + 1} of {totalPages}</span>
                  <Button variant="outline" size="sm" onClick={() => setPage((p) => p + 1)} disabled={page + 1 >= totalPages}>
                    Next
                    <ChevronRight className="ml-1 h-4 w-4" />
                  </Button>
                </div>
              )}
            </>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

function TabBtn({ active, onClick, children }: any) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        "inline-flex items-center rounded-md border px-3 py-1.5 text-sm font-medium",
        active ? "border-primary bg-primary text-primary-foreground" : "border-border hover:bg-accent"
      )}
    >
      {children}
    </button>
  )
}
