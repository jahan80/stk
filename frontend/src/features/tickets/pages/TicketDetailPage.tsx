import { useParams, Link } from "react-router-dom"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import {
  useTicket, useComments, useAddComment, useCloseTicket,
  useChangeStatus, useAssignTicket,
} from "../hooks"
import { TicketStatusBadge, TicketPriorityBadge } from "../components/TicketBadges"
import { AssigneeDropdown } from "../components/AssigneeDropdown"
import { MentionInput } from "../components/MentionInput"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Separator } from "@/components/ui/separator"
import { Skeleton } from "@/components/ui/skeleton"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Badge } from "@/components/ui/badge"
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from "@/components/ui/select"
import { AlertCircle, ArrowLeft, Clock, MessageSquare, XCircle, Loader2, Send } from "lucide-react"
import { formatDate, formatRelativeTime } from "@/lib/utils"
import { useQuery } from "@tanstack/react-query"
import { apiClient } from "@/api/client"
import { useAuthStore } from "@/store/auth"
import type { TicketStatus } from "@/types/tickets"

const schema = z.object({ body: z.string().min(1, "Comment required").max(10000) })
type FormData = z.infer<typeof schema>
const STATUSES: TicketStatus[] = ["OPEN", "IN_PROGRESS", "WAITING", "RESOLVED", "CLOSED"]

export function TicketDetailPage() {
  const { id } = useParams<{ id: string }>()
  const ticketId = Number(id)
  const user = useAuthStore((s) => s.user)
  // A user is an agent if they have access to this ticket via group membership
  // (backend detects AGENT role in comments)

  const { data: ticketData, isLoading } = useTicket(ticketId)
  const { data: commentsData, isLoading: commentsLoading } = useComments(ticketId)

  // Users list (admin only) for display names
  const { data: usersData } = useQuery({
    queryKey: ["auth", "users"],
    queryFn: async () => {
      const { data } = await apiClient.get("/auth/users")
      return data
    },
    enabled: user?.role === "ADMIN",
  })
  const userMap = ((usersData?.data as any[]) ?? []).reduce(
    (acc: Record<number, string>, u: any) => ({ ...acc, [u.id]: u.username }),
    {}
  )

  const addComment = useAddComment(ticketId)
  const closeTicket = useCloseTicket(ticketId)
  const changeStatus = useChangeStatus(ticketId)
  const assign = useAssignTicket(ticketId)

  const ticket = ticketData?.data
  const comments = commentsData?.data ?? []

  // viewerRole از backend میاد
  const viewerRole = ticket?.viewerRole
  const isAdmin = viewerRole === "ADMIN" || user?.role === "ADMIN"
  const isAgent = viewerRole === "AGENT"
  const canMention = isAdmin || isAgent

  const { register, handleSubmit, reset, watch, setValue, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { body: "" },
  })

  const onSubmit = () => {
    const body = watch("body")
    if (!body?.trim()) return
    addComment.mutate({ body }, { onSuccess: () => reset() })
  }

  if (isLoading) return <Skeleton className="h-96 w-full" />

  if (!ticket) {
    return (
      <div className="py-12 text-center">
        <AlertCircle className="mx-auto mb-2 h-12 w-12 text-muted-foreground" />
        <p>Ticket not found</p>
        <Button asChild variant="link"><Link to="/tickets">Back</Link></Button>
      </div>
    )
  }

  const isOwner = ticket.createdBy === user?.userId
  const canClose = (isOwner || isAdmin) && ticket.status !== "CLOSED" && ticket.status !== "RESOLVED"
  const slaBreached = ticket.slaDeadline && new Date(ticket.slaDeadline) < new Date()
    && ticket.status !== "CLOSED" && ticket.status !== "RESOLVED"

  return (
    <div className="space-y-6">
      <Button variant="ghost" size="sm" asChild>
        <Link to="/tickets"><ArrowLeft className="mr-2 h-4 w-4" /> Back</Link>
      </Button>

      <Card>
        <CardHeader>
          <div className="flex items-start justify-between gap-4">
            <div className="flex-1">
              <div className="mb-2 flex flex-wrap items-center gap-2">
                <span className="font-mono text-xs text-muted-foreground">{ticket.ticketNumber}</span>
                <TicketStatusBadge status={ticket.status} />
                <TicketPriorityBadge priority={ticket.priority} />
                {ticket.categoryCode && <Badge variant="outline">{ticket.categoryCode}</Badge>}
                {slaBreached && (
                  <Badge variant="destructive">
                    <AlertCircle className="mr-1 h-3 w-3" /> SLA Breached
                  </Badge>
                )}
              </div>
              <CardTitle className="text-2xl">{ticket.title}</CardTitle>
              <CardDescription>
                Created by {displayUser(ticket.createdBy, user?.userId, isAdmin, userMap)} · {formatRelativeTime(ticket.createdAt)}
              </CardDescription>
            </div>
            {canClose && (
              <Button
                variant="outline"
                onClick={() => { if (confirm("Close this ticket?")) closeTicket.mutate() }}
                disabled={closeTicket.isPending}
              >
                <XCircle className="mr-2 h-4 w-4" /> Close
              </Button>
            )}
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          {/* Admin actions */}
          {isAdmin && (
            <>
              <div className="grid gap-4 md:grid-cols-2">
                <div className="space-y-2">
                  <label className="text-sm font-medium">Status</label>
                  <Select
                    value={ticket.status}
                    onValueChange={(v) => changeStatus.mutate({ status: v as TicketStatus })}
                    disabled={changeStatus.isPending}
                  >
                    <SelectTrigger><SelectValue /></SelectTrigger>
                    <SelectContent>
                      {STATUSES.map((s) => (
                        <SelectItem key={s} value={s}>{s.replace(/_/g, " ")}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-2">
                  <label className="text-sm font-medium">Assignee</label>
                  <AssigneeDropdown
                    currentAssignee={ticket.assignedTo}
                    onAssign={(userId) => assign.mutate({ assigneeId: userId })}
                    isPending={assign.isPending}
                  />
                </div>
              </div>
              <Separator />
            </>
          )}

          {/* Info */}
          <div className="grid gap-3 text-sm md:grid-cols-3">
            <div>
              <span className="text-muted-foreground">Created by: </span>
              <span className="font-medium">{displayUser(ticket.createdBy, user?.userId, isAdmin, userMap)}</span>
            </div>
            <div>
              <span className="text-muted-foreground">Assigned to: </span>
              <span className="font-medium">{displayUser(ticket.assignedTo, user?.userId, isAdmin, userMap)}</span>
            </div>
            <div>
              <span className="text-muted-foreground">Group: </span>
              <span className="font-medium">{ticket.groupName ?? "—"}</span>
            </div>
          </div>

          {ticket.slaDeadline && (
            <div className={`flex items-center gap-2 rounded-md border p-3 text-xs ${
              slaBreached ? "border-destructive/50 bg-destructive/10 text-destructive" : "bg-muted/30 text-muted-foreground"
            }`}>
              <Clock className="h-4 w-4" />
              <span>SLA: {formatDate(ticket.slaDeadline)}</span>
            </div>
          )}

          <Separator />

          <div>
            <p className="mb-2 text-sm font-medium text-muted-foreground">Description</p>
            <div className="whitespace-pre-wrap rounded-md bg-muted/30 p-3 text-sm">
              {ticket.description}
            </div>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <MessageSquare className="h-5 w-5" />
            Comments ({comments.length})
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {commentsLoading ? (
            <Skeleton className="h-20 w-full" />
          ) : comments.length === 0 ? (
            <p className="py-6 text-center text-sm text-muted-foreground">No comments yet</p>
          ) : (
            <div className="space-y-4">
              {comments.map((c) => (
                <div key={c.id} className="flex gap-3">
                  <Avatar className="h-8 w-8">
                    <AvatarFallback className={c.authorRole === "ADMIN" ? "bg-primary text-primary-foreground text-xs" : "text-xs"}>
                      {c.authorRole === "ADMIN" ? "A" : c.authorRole === "AGENT" ? "S" : "U"}
                    </AvatarFallback>
                  </Avatar>
                  <div className="flex-1 space-y-1">
                    <div className="flex items-center gap-2 text-xs">
                      <span className="font-medium">
                        {c.authorRole === "ADMIN" ? "Admin" : c.authorRole === "AGENT" ? "Support" : `User #${c.authorId}`}
                      </span>
                      <span className="text-muted-foreground">{formatRelativeTime(c.createdAt)}</span>
                    </div>
                    <div className="whitespace-pre-wrap rounded-md bg-muted p-3 text-sm">
                      {renderWithMentions(c.body)}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}

          <Separator />

          <div className="space-y-3">
            {canMention ? (
              <MentionInput
                value={watch("body") || ""}
                onChange={(v) => setValue("body", v)}
                onSubmit={handleSubmit(onSubmit)}
                isPending={addComment.isPending}
              />
            ) : (
              <div className="space-y-3">
                <textarea
                  rows={3}
                  placeholder="Write a reply..."
                  className="flex w-full rounded-md border border-input bg-background px-3 py-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  {...register("body")}
                  disabled={addComment.isPending}
                />
                <div className="flex justify-end">
                  <Button type="submit" onClick={handleSubmit(onSubmit)} disabled={addComment.isPending}>
                    {addComment.isPending ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <Send className="mr-2 h-4 w-4" />}
                    Send
                  </Button>
                </div>
              </div>
            )}
            {errors.body && <p className="text-sm text-destructive">{errors.body.message}</p>}
          </div>
        </CardContent>
      </Card>
    </div>
  )
}

function displayUser(
  id: number | undefined,
  currentUserId: number | undefined,
  isAdmin: boolean,
  userMap: Record<number, string>
): string {
  if (!id) return "—"
  if (id === currentUserId) return "You"
  if (isAdmin && userMap[id]) return userMap[id]
  return `#${id}`
}

function renderWithMentions(text: string) {
  const parts = text.split(/(@[a-zA-Z0-9_.-]+)/g)
  return parts.map((part, i) =>
    part.startsWith("@") ? (
      <span key={i} className="font-medium text-primary">{part}</span>
    ) : (
      <span key={i}>{part}</span>
    )
  )
}
