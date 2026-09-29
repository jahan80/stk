import { Badge } from "@/components/ui/badge"
import type { TicketStatus, TicketPriority } from "@/types/tickets"

export function TicketStatusBadge({ status }: { status: TicketStatus }) {
  const config = {
    OPEN: { variant: "default" as const, label: "Open" },
    IN_PROGRESS: { variant: "warning" as const, label: "In Progress" },
    WAITING: { variant: "secondary" as const, label: "Waiting" },
    RESOLVED: { variant: "success" as const, label: "Resolved" },
    CLOSED: { variant: "outline" as const, label: "Closed" },
  }[status]

  return <Badge variant={config.variant}>{config.label}</Badge>
}

export function TicketPriorityBadge({ priority }: { priority: TicketPriority }) {
  const config = {
    LOW: { variant: "outline" as const, label: "Low" },
    MEDIUM: { variant: "secondary" as const, label: "Medium" },
    HIGH: { variant: "warning" as const, label: "High" },
    URGENT: { variant: "destructive" as const, label: "Urgent" },
  }[priority]

  return <Badge variant={config.variant}>{config.label}</Badge>
}
