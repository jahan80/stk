import { useQuery } from "@tanstack/react-query"
import { apiClient } from "@/api/client"
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from "@/components/ui/select"
import { UserCheck } from "lucide-react"

interface Props {
  currentAssignee?: number
  onAssign: (userId: number) => void
  isPending: boolean
}

export function AssigneeDropdown({ currentAssignee, onAssign, isPending }: Props) {
  const { data: usersData, isLoading } = useQuery({
    queryKey: ["auth", "users"],
    queryFn: async () => {
      const { data } = await apiClient.get("/auth/users")
      return data
    },
  })

  const users = usersData?.data ?? []

  return (
    <Select
      value={currentAssignee ? String(currentAssignee) : "none"}
      onValueChange={(v) => {
        if (v !== "none") onAssign(Number(v))
      }}
      disabled={isPending || isLoading}
    >
      <SelectTrigger>
        <SelectValue placeholder="Assign to..." />
      </SelectTrigger>
      <SelectContent>
        <SelectItem value="none">— Unassigned —</SelectItem>
        {users.map((u: any) => (
          <SelectItem key={u.id} value={String(u.id)}>
            #{u.id} — {u.username} ({u.role})
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  )
}
