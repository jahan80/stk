import { useState } from "react"
import { Link } from "react-router-dom"
import {
  useGroups, useCreateGroup, useUpdateGroup, useDeleteGroup,
  useGroupMembers, useAddGroupMember, useRemoveGroupMember,
  useTicketConfigs, useUpdateTicketConfig, useResetTicketConfig,
} from "../hooks"
import { useQuery } from "@tanstack/react-query"
import { apiClient } from "@/api/client"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import {
  Plus, Edit, Power, Users, Trash2, Loader2, ArrowLeft, Tags, Settings, UserPlus, Shield, RotateCcw,
} from "lucide-react"
import type { TicketGroup, TicketCategory, TicketConfiguration } from "@/types/tickets"

export function TicketSettingsPage() {
  return (
    <div className="space-y-6">
      <div>
        <Button variant="ghost" size="sm" asChild>
          <Link to="/tickets"><ArrowLeft className="mr-2 h-4 w-4" /> Back to tickets</Link>
        </Button>
      </div>

      <div>
        <h1 className="text-3xl font-bold">Ticket Settings</h1>
        <p className="text-muted-foreground">Manage groups, categories, and configurations</p>
      </div>

      <Tabs defaultValue="groups">
        <TabsList>
          <TabsTrigger value="groups">
            <Users className="mr-2 h-4 w-4" /> Groups
          </TabsTrigger>
          <TabsTrigger value="categories">
            <Tags className="mr-2 h-4 w-4" /> Categories
          </TabsTrigger>
          <TabsTrigger value="configs">
            <Settings className="mr-2 h-4 w-4" /> Configurations
          </TabsTrigger>
        </TabsList>

        <TabsContent value="groups" className="space-y-4">
          <GroupsTab />
        </TabsContent>

        <TabsContent value="categories" className="space-y-4">
          <CategoriesTab />
        </TabsContent>

        <TabsContent value="configs" className="space-y-4">
          <ConfigsTab />
        </TabsContent>
      </Tabs>
    </div>
  )
}

// =====================================================
// GROUPS TAB
// =====================================================
function GroupsTab() {
  const { data, isLoading } = useGroups()
  const createGroup = useCreateGroup()
  const updateGroup = useUpdateGroup()
  const deleteGroup = useDeleteGroup()

  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<TicketGroup | null>(null)
  const [form, setForm] = useState({ name: "", description: "" })

  const [membersOpen, setMembersOpen] = useState(false)
  const [selectedGroup, setSelectedGroup] = useState<TicketGroup | null>(null)

  const groups = data?.data ?? []

  const openCreate = () => { setEditing(null); setForm({ name: "", description: "" }); setOpen(true) }
  const openEdit = (g: TicketGroup) => {
    setEditing(g); setForm({ name: g.name, description: g.description ?? "" }); setOpen(true)
  }

  const handleSave = () => {
    if (editing) {
      updateGroup.mutate({ id: editing.id, data: form }, { onSuccess: () => setOpen(false) })
    } else {
      createGroup.mutate(form, { onSuccess: () => setOpen(false) })
    }
  }

  return (
    <>
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <div>
            <CardTitle>Groups ({groups.length})</CardTitle>
            <CardDescription>Support teams and their members</CardDescription>
          </div>
          <Button onClick={openCreate}>
            <Plus className="mr-2 h-4 w-4" /> New Group
          </Button>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <Skeleton className="h-64 w-full" />
          ) : groups.length === 0 ? (
            <div className="py-12 text-center text-muted-foreground">
              <Users className="mx-auto mb-2 h-8 w-8" />
              <p>No groups yet</p>
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
                    <TableCell><Badge variant="outline">{g.memberCount} member(s)</Badge></TableCell>
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
                            onClick={() => { if (confirm(`Disable "${g.name}"?`)) deleteGroup.mutate(g.id) }}
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
            <DialogDescription>Add or remove members. Roles: LEADER (manager) or AGENT.</DialogDescription>
          </DialogHeader>
          {selectedGroup && <MembersList groupId={selectedGroup.id} />}
        </DialogContent>
      </Dialog>
    </>
  )
}

// =====================================================
// MEMBERS LIST (with user dropdown)
// =====================================================
function MembersList({ groupId }: { groupId: number }) {
  const { data, isLoading } = useGroupMembers(groupId)
  const addMember = useAddGroupMember()
  const removeMember = useRemoveGroupMember()

  // Fetch users from auth-service
  const { data: usersData } = useQuery({
    queryKey: ["auth", "users"],
    queryFn: async () => {
      const { data } = await apiClient.get("/auth/users")
      return data
    },
  })

  const [userId, setUserId] = useState("")
  const [role, setRole] = useState<"LEADER" | "AGENT">("AGENT")

  const members = data?.data ?? []
  const users = usersData?.data ?? []
  const existingUserIds = members.map((m) => m.userId)
  const availableUsers = users.filter((u: any) => !existingUserIds.includes(u.id))

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
        <select
          value={userId}
          onChange={(e) => setUserId(e.target.value)}
          className="flex-1 h-10 rounded-md border border-input bg-background px-3 text-sm"
        >
          <option value="">Select user...</option>
          {availableUsers.length === 0 ? (
            <option value="" disabled>No available users</option>
          ) : (
            availableUsers.map((u: any) => (
              <option key={u.id} value={String(u.id)}>
                #{u.id} — {u.username} ({u.role})
              </option>
            ))
          )}
        </select>

        <select
          value={role}
          onChange={(e) => setRole(e.target.value as any)}
          className="w-32 h-10 rounded-md border border-input bg-background px-3 text-sm"
        >
          <option value="AGENT">Agent</option>
          <option value="LEADER">Leader</option>
        </select>

        <Button onClick={handleAdd} disabled={!userId || addMember.isPending}>
          {addMember.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <UserPlus className="h-4 w-4" />}
          Add
        </Button>
      </div>

      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>User</TableHead>
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
            members.map((m) => {
              const u = users.find((x: any) => x.id === m.userId)
              return (
                <TableRow key={m.id}>
                  <TableCell>
                    <span className="font-medium">#{m.userId}</span>
                    {u && <span className="text-muted-foreground"> — {u.username}</span>}
                  </TableCell>
                  <TableCell>
                    <Badge variant={m.role === "LEADER" ? "default" : "secondary"}>
                      {m.role === "LEADER" && <Shield className="mr-1 h-3 w-3" />}
                      {m.role}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-right">
                    <Button variant="ghost" size="sm" onClick={() => removeMember.mutate({ id: groupId, userId: m.userId })}>
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </TableCell>
                </TableRow>
              )
            })
          )}
        </TableBody>
      </Table>
    </div>
  )
}

// =====================================================
// CATEGORIES TAB (read-only for now)
// =====================================================
function CategoriesTab() {
  const { data, isLoading } = useQuery({
    queryKey: ["ticket-categories"],
    queryFn: async () => {
      const { data } = await apiClient.get("/tickets/categories")
      return data
    },
  })

  const categories: TicketCategory[] = data?.data ?? []

  return (
    <Card>
      <CardHeader>
        <CardTitle>Categories ({categories.length})</CardTitle>
        <CardDescription>ITIL-based categories: IR, CR, SR</CardDescription>
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <Skeleton className="h-32 w-full" />
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Code</TableHead>
                <TableHead>Name</TableHead>
                <TableHead>Description</TableHead>
                <TableHead>Status</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {categories.map((c) => (
                <TableRow key={c.id}>
                  <TableCell><Badge variant="outline">{c.code}</Badge></TableCell>
                  <TableCell className="font-medium">{c.name}</TableCell>
                  <TableCell className="text-muted-foreground">{c.description ?? "—"}</TableCell>
                  <TableCell>
                    <Badge variant={c.enabled ? "success" : "secondary"}>
                      {c.enabled ? "Active" : "Disabled"}
                    </Badge>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </CardContent>
    </Card>
  )
}

// =====================================================
// CONFIGS TAB (placeholder — phase 2)
// =====================================================
function ConfigsTab() {
  const { data, isLoading } = useTicketConfigs()
  const updateConfig = useUpdateTicketConfig()
  const resetConfig = useResetTicketConfig()

  const [search, setSearch] = useState("")
  const [editing, setEditing] = useState<TicketConfiguration | null>(null)
  const [editValue, setEditValue] = useState("")

  const configs = data?.data ?? []
  const filtered = configs.filter(
    (c) =>
      c.configKey.toLowerCase().includes(search.toLowerCase()) ||
      (c.description ?? "").toLowerCase().includes(search.toLowerCase())
  )

  // Group by prefix (e.g. "TICKET.NUMBER", "TICKET.SLA")
  const grouped = filtered.reduce<Record<string, TicketConfiguration[]>>((acc, c) => {
    const parts = c.configKey.split(".")
    const group = parts.slice(0, 2).join(".")
    if (!acc[group]) acc[group] = []
    acc[group].push(c)
    return acc
  }, {})

  const handleSave = () => {
    if (!editing) return
    updateConfig.mutate(
      { key: editing.configKey, value: editValue },
      { onSuccess: () => setEditing(null) }
    )
  }

  return (
    <div className="space-y-4">
      <Input
        placeholder="Search configurations..."
        value={search}
        onChange={(e) => setSearch(e.target.value)}
      />

      {isLoading ? (
        <Skeleton className="h-64 w-full" />
      ) : (
        <div className="space-y-4">
          {Object.entries(grouped).map(([group, items]) => (
            <Card key={group}>
              <CardHeader className="pb-3">
                <CardTitle className="font-mono text-base">{group}</CardTitle>
                <CardDescription>{items.length} setting(s)</CardDescription>
              </CardHeader>
              <CardContent className="space-y-2">
                {items.map((c) => (
                  <div key={c.configKey} className="flex items-center gap-3 rounded-md border p-3">
                    <div className="flex-1 overflow-hidden">
                      <div className="flex items-center gap-2">
                        <p className="truncate font-mono text-sm font-medium">{c.configKey}</p>
                        <Badge variant="outline" className="text-xs">{c.valueType}</Badge>
                      </div>
                      <p className="truncate text-xs text-muted-foreground">{c.description || "—"}</p>
                    </div>
                    <code className="rounded bg-muted px-2 py-1 text-xs font-mono">
                      {c.configValue.length > 25 ? c.configValue.slice(0, 25) + "…" : c.configValue}
                    </code>
                    <Button variant="outline" size="sm" onClick={() => { setEditing(c); setEditValue(c.configValue) }}>
                      Edit
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => {
                        if (confirm(`Reset "${c.configKey}" to default?`)) resetConfig.mutate(c.configKey)
                      }}
                    >
                      <RotateCcw className="h-4 w-4" />
                    </Button>
                  </div>
                ))}
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      <Dialog open={!!editing} onOpenChange={(o) => !o && setEditing(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Edit Configuration</DialogTitle>
            <DialogDescription className="font-mono text-xs">{editing?.configKey}</DialogDescription>
          </DialogHeader>
          {editing && (
            <div className="space-y-4">
              <div className="space-y-2">
                <Label>Value</Label>
                {editing.valueType === "BOOLEAN" ? (
                  <Select value={editValue} onValueChange={setEditValue}>
                    <SelectTrigger><SelectValue /></SelectTrigger>
                    <SelectContent>
                      <SelectItem value="true">true</SelectItem>
                      <SelectItem value="false">false</SelectItem>
                    </SelectContent>
                  </Select>
                ) : (
                  <Input
                    value={editValue}
                    onChange={(e) => setEditValue(e.target.value)}
                    type={editing.valueType === "INTEGER" ? "number" : "text"}
                  />
                )}
              </div>
              <div className="rounded-md bg-muted p-3 text-xs">
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Default:</span>
                  <span className="font-mono">{editing.defaultValue}</span>
                </div>
              </div>
            </div>
          )}
          <DialogFooter>
            <Button variant="outline" onClick={() => setEditing(null)}>Cancel</Button>
            <Button onClick={handleSave} disabled={updateConfig.isPending}>
              {updateConfig.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              Save
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
