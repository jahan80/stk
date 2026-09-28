import { useState } from "react"
import {
  useGroups, useCreateGroup, useUpdateGroup, useDeleteGroup,
  useGroupMembers, useAddGroupMember, useRemoveGroupMember,
} from "../hooks"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Plus, Edit, Power, Users, Trash2, Loader2 } from "lucide-react"
import type { TicketGroup } from "@/types/tickets"

export function AdminGroupsPage() {
  const { data, isLoading } = useGroups()
  const createGroup = useCreateGroup()
  const updateGroup = useUpdateGroup()
  const deleteGroup = useDeleteGroup()

  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<TicketGroup | null>(null)
  const [form, setForm] = useState({ name: "", description: "", enabled: true })

  const [membersOpen, setMembersOpen] = useState(false)
  const [selectedGroup, setSelectedGroup] = useState<TicketGroup | null>(null)

  const groups = data?.data ?? []

  const openCreate = () => {
    setEditing(null)
    setForm({ name: "", description: "", enabled: true })
    setOpen(true)
  }

  const openEdit = (g: TicketGroup) => {
    setEditing(g)
    setForm({ name: g.name, description: g.description ?? "", enabled: g.enabled })
    setOpen(true)
  }

  const handleSave = () => {
    if (editing) {
      updateGroup.mutate({ id: editing.id, data: form }, { onSuccess: () => setOpen(false) })
    } else {
      createGroup.mutate(form, { onSuccess: () => setOpen(false) })
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">Ticket Groups</h1>
          <p className="text-muted-foreground">Support teams and their members</p>
        </div>
        <Button onClick={openCreate}>
          <Plus className="mr-2 h-4 w-4" /> New Group
        </Button>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Groups ({groups.length})</CardTitle>
          <CardDescription>Manage groups and members</CardDescription>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <Skeleton className="h-64 w-full" />
          ) : groups.length === 0 ? (
            <div className="py-12 text-center text-muted-foreground">
              <Users className="mx-auto mb-2 h-8 w-8" />
              <p>No groups yet</p>
              <Button variant="link" onClick={openCreate}>Create the first group</Button>
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Name</TableHead>
                  <TableHead>Description</TableHead>
                  <TableHead>Members</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {groups.map((g) => (
                  <TableRow key={g.id}>
                    <TableCell className="font-medium">{g.name}</TableCell>
                    <TableCell className="text-muted-foreground">{g.description ?? "—"}</TableCell>
                    <TableCell>
                      <Badge variant="outline">{g.memberCount} member(s)</Badge>
                    </TableCell>
                    <TableCell>
                      <Badge variant={g.enabled ? "success" : "secondary"}>
                        {g.enabled ? "Active" : "Disabled"}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex justify-end gap-2">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => { setSelectedGroup(g); setMembersOpen(true) }}
                        >
                          <Users className="mr-1 h-3 w-3" /> Members
                        </Button>
                        <Button variant="ghost" size="sm" onClick={() => openEdit(g)}>
                          <Edit className="h-4 w-4" />
                        </Button>
                        {g.enabled && (
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => {
                              if (confirm(`Disable "${g.name}"?`)) deleteGroup.mutate(g.id)
                            }}
                          >
                            <Power className="h-4 w-4" />
                          </Button>
                        )}
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      {/* Create/Edit Dialog */}
      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{editing ? "Edit Group" : "New Group"}</DialogTitle>
            <DialogDescription>Support team details</DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-2">
              <Label>Name *</Label>
              <Input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
            </div>
            <div className="space-y-2">
              <Label>Description</Label>
              <Input value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setOpen(false)}>Cancel</Button>
            <Button onClick={handleSave} disabled={!form.name || createGroup.isPending || updateGroup.isPending}>
              {(createGroup.isPending || updateGroup.isPending) && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              Save
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Members Dialog */}
      <Dialog open={membersOpen} onOpenChange={(o) => !o && setMembersOpen(false)}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>Members of {selectedGroup?.name}</DialogTitle>
            <DialogDescription>Add or remove members</DialogDescription>
          </DialogHeader>
          {selectedGroup && <MembersList groupId={selectedGroup.id} />}
        </DialogContent>
      </Dialog>
    </div>
  )
}

function MembersList({ groupId }: { groupId: number }) {
  const { data, isLoading } = useGroupMembers(groupId)
  const addMember = useAddGroupMember()
  const removeMember = useRemoveGroupMember()

  const [userId, setUserId] = useState("")
  const [role, setRole] = useState<"LEADER" | "AGENT">("AGENT")

  const members = data?.data ?? []

  const handleAdd = () => {
    if (!userId) return
    addMember.mutate(
      { id: groupId, data: { userId: Number(userId), role } },
      { onSuccess: () => setUserId("") }
    )
  }

  return (
    <div className="space-y-4">
      <div className="flex gap-2">
        <Input
          placeholder="User ID"
          type="number"
          value={userId}
          onChange={(e) => setUserId(e.target.value)}
        />
        <Select value={role} onValueChange={(v) => setRole(v as any)}>
          <SelectTrigger className="w-32"><SelectValue /></SelectTrigger>
          <SelectContent>
            <SelectItem value="AGENT">Agent</SelectItem>
            <SelectItem value="LEADER">Leader</SelectItem>
          </SelectContent>
        </Select>
        <Button onClick={handleAdd} disabled={!userId || addMember.isPending}>
          {addMember.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <Plus className="h-4 w-4" />}
          Add
        </Button>
      </div>

      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>User ID</TableHead>
            <TableHead>Role</TableHead>
            <TableHead className="text-right">Actions</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {isLoading ? (
            <TableRow><TableCell colSpan={3}><Skeleton className="h-8 w-full" /></TableCell></TableRow>
          ) : members.length === 0 ? (
            <TableRow><TableCell colSpan={3} className="text-center text-muted-foreground py-6">No members</TableCell></TableRow>
          ) : (
            members.map((m) => (
              <TableRow key={m.id}>
                <TableCell className="font-mono">#{m.userId}</TableCell>
                <TableCell>
                  <Badge variant={m.role === "LEADER" ? "default" : "secondary"}>{m.role}</Badge>
                </TableCell>
                <TableCell className="text-right">
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => removeMember.mutate({ id: groupId, userId: m.userId })}
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </TableCell>
              </TableRow>
            ))
          )}
        </TableBody>
      </Table>
    </div>
  )
}
