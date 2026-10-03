import { useState } from "react"
import { useAuditEvents, useAuditByTrace } from "@/features/admin/hooks"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Skeleton } from "@/components/ui/skeleton"
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Search, FileText, ChevronLeft, ChevronRight, X, ExternalLink, Calendar } from "lucide-react"
import { formatDate, formatRelativeTime } from "@/lib/utils"

export function AuditLogPage() {
  const [page, setPage] = useState(0)
  const [eventType, setEventType] = useState("")
  const [source, setSource] = useState("")
  const [selectedTrace, setSelectedTrace] = useState<string | null>(null)
  const [fromDate, setFromDate] = useState("")
  const [toDate, setToDate] = useState("")

  const { data, isLoading } = useAuditEvents({
    eventType: eventType || undefined,
    source: source || undefined,
    from: fromDate ? new Date(fromDate).toISOString() : undefined,
    to: toDate ? new Date(toDate + "T23:59:59").toISOString() : undefined,
    page,
    size: 20,
  })

  const { data: traceData, isLoading: traceLoading } = useAuditByTrace(selectedTrace ?? "")

  const events = data?.data?.content ?? []
  const totalPages = data?.data?.totalPages ?? 0
  const totalElements = data?.data?.totalElements ?? 0
  const traceEvents = traceData?.data ?? []

  const clearFilters = () => {
    setEventType("")
    setSource("")
    setFromDate("")
    setToDate("")
    setPage(0)
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold">Audit Logs</h1>
        <p className="text-muted-foreground">
          {totalElements} event(s) recorded
        </p>
      </div>

      {/* Filters */}
      <Card>
        <CardContent className="flex flex-wrap items-end gap-3 pt-6">
          <div className="flex-1 min-w-[200px] space-y-2">
            <Label>Event Type</Label>
            <select
              value={eventType}
              onChange={(e) => { setEventType(e.target.value); setPage(0) }}
              className="h-10 w-full rounded-md border border-input bg-background px-3 text-sm"
            >
              <option value="">All event types</option>
              <optgroup label="Auth">
                <option value="USER_REGISTERED">USER_REGISTERED</option>
                <option value="USER_LOGGED_IN">USER_LOGGED_IN</option>
                <option value="USER_LOGGED_OUT">USER_LOGGED_OUT</option>
              </optgroup>
              <optgroup label="Ticket">
                <option value="TICKET_CREATED">TICKET_CREATED</option>
                <option value="TICKET_ASSIGNED">TICKET_ASSIGNED</option>
                <option value="TICKET_COMMENTED">TICKET_COMMENTED</option>
                <option value="TICKET_STATUS_CHANGED">TICKET_STATUS_CHANGED</option>
              </optgroup>
              <optgroup label="Gateway">
                <option value="REQUEST_RECEIVED">REQUEST_RECEIVED</option>
                <option value="RESPONSE_SENT">RESPONSE_SENT</option>
              </optgroup>
            </select>
          </div>
          <div className="flex-1 min-w-[200px] space-y-2">
            <Label>Source</Label>
            <select
              value={source}
              onChange={(e) => { setSource(e.target.value); setPage(0) }}
              className="h-10 w-full rounded-md border border-input bg-background px-3 text-sm"
            >
              <option value="">All sources</option>
              <option value="auth-service">auth-service</option>
              <option value="ticket-service">ticket-service</option>
              <option value="api-gateway">api-gateway</option>
              <option value="notif-service">notif-service</option>
            </select>
          </div>
          <div className="space-y-2">
            <Label>From</Label>
            <Input
              type="date"
              value={fromDate}
              onChange={(e) => { setFromDate(e.target.value); setPage(0) }}
              className="w-[160px]"
            />
          </div>
          <div className="space-y-2">
            <Label>To</Label>
            <Input
              type="date"
              value={toDate}
              onChange={(e) => { setToDate(e.target.value); setPage(0) }}
              className="w-[160px]"
            />
          </div>
          <Button variant="outline" onClick={clearFilters}>
            <X className="mr-2 h-4 w-4" />
            Clear
          </Button>
        </CardContent>
      </Card>

      {/* Table */}
      <Card>
        <CardHeader>
          <CardTitle>Events</CardTitle>
          <CardDescription>
            Page {page + 1} of {Math.max(totalPages, 1)}
          </CardDescription>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="space-y-2">
              {[1, 2, 3, 4, 5].map((i) => <Skeleton key={i} className="h-12 w-full" />)}
            </div>
          ) : events.length === 0 ? (
            <div className="py-12 text-center text-muted-foreground">
              <FileText className="mx-auto mb-2 h-8 w-8" />
              No events found
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>ID</TableHead>
                  <TableHead>Event Type</TableHead>
                  <TableHead>Source</TableHead>
                  <TableHead>Trace ID</TableHead>
                  <TableHead>Occurred</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {events.map((e) => (
                  <TableRow key={e.id}>
                    <TableCell className="font-mono text-xs">{e.id}</TableCell>
                    <TableCell>
                      <Badge variant="outline">{e.eventType}</Badge>
                    </TableCell>
                    <TableCell className="text-sm">{e.source}</TableCell>
                    <TableCell>
                      {e.traceId ? (
                        <button
                          className="font-mono text-xs text-primary hover:underline"
                          onClick={() => setSelectedTrace(e.traceId!)}
                        >
                          {e.traceId.slice(0, 8)}…
                        </button>
                      ) : (
                        <span className="text-muted-foreground">—</span>
                      )}
                    </TableCell>
                    <TableCell className="text-xs text-muted-foreground">
                      {formatRelativeTime(e.occurredAt)}
                    </TableCell>
                    <TableCell className="text-right">
                      {e.traceId && (
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => setSelectedTrace(e.traceId!)}
                        >
                          <ExternalLink className="h-4 w-4" />
                        </Button>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}

          {/* Pagination */}
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

      {/* Trace Dialog */}
      <Dialog open={!!selectedTrace} onOpenChange={(o) => !o && setSelectedTrace(null)}>
        <DialogContent className="max-w-3xl max-h-[80vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Trace Details</DialogTitle>
            <DialogDescription className="font-mono text-xs">
              {selectedTrace}
            </DialogDescription>
          </DialogHeader>
          {traceLoading ? (
            <Skeleton className="h-40 w-full" />
          ) : traceEvents.length === 0 ? (
            <p className="text-muted-foreground">No events for this trace</p>
          ) : (
            <div className="space-y-3">
              {traceEvents.map((e) => (
                <div key={e.id} className="rounded-md border p-3">
                  <div className="mb-2 flex items-center justify-between">
                    <Badge variant="outline">{e.eventType}</Badge>
                    <span className="text-xs text-muted-foreground">
                      {formatDate(e.occurredAt)}
                    </span>
                  </div>
                  <pre className="overflow-x-auto rounded bg-muted p-2 text-xs">
                    {JSON.stringify(e.payload, null, 2)}
                  </pre>
                </div>
              ))}
            </div>
          )}
        </DialogContent>
      </Dialog>
    </div>
  )
}
