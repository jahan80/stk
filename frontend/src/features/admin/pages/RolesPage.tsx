import { useState } from "react"
import { useRoles, usePermissions, useCreateRole, useDeleteRole, useAssignPermissions } from "../hooks"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Badge } from "@/components/ui/badge"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import {
  Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger,
} from "@/components/ui/dialog"
import { Skeleton } from "@/components/ui/skeleton"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Plus, Trash2, Shield, Lock, Loader2 } from "lucide-react"
import { cn } from "@/lib/utils"

export function RolesPage() {
  const { data, isLoading } = useRoles()
  const { data: permData } = usePermissions()
  const createRole = useCreateRole()
  const deleteRole = useDeleteRole()
  const assignPerms = useAssignPermissions()

  const [open, setOpen] = useState(false)
  const [form, setForm] = useState({ name: "", description: "" })

  const [permDialogOpen, setPermDialogOpen] = useState(false)
  const [selectedRole, setSelectedRole] = useState<number | null>(null)
  const [selectedPermIds, setSelectedPermIds] = useState<number[]>([])

  const roles = data?.data ?? []
  const permissions = permData?.data ?? []

  const handleCreate = () => {
    if (!form.name) return
    createRole.mutate(
      { name: form.name, description: form.description },
      {
        onSuccess: () => {
          setOpen(false)
          setForm({ name: "", description: "" })
        },
      }
    )
  }

  const openPermissionsDialog = (roleId: number, currentPerms: { id: number }[]) => {
    setSelectedRole(roleId)
    setSelectedPermIds(currentPerms.map((p) => p.id))
    setPermDialogOpen(true)
  }

  const togglePerm = (id: number) => {
    setSelectedPermIds((prev) =>
      prev.includes(id) ? prev.filter((p) => p !== id) : [...prev, id]
    )
  }

  const handleSavePermissions = () => {
    if (selectedRole == null) return
    assignPerms.mutate(
      { id: selectedRole, permissions: selectedPermIds },
      { onSuccess: () => setPermDialogOpen(false) }
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">Roles &amp; Permissions</h1>
          <p className="text-muted-foreground">Manage roles and their permissions</p>
        </div>
        <Dialog open={open} onOpenChange={setOpen}>
          <DialogTrigger asChild>
            <Button>
              <Plus className="mr-2 h-4 w-4" />
              Create Role
            </Button>
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Create New Role</DialogTitle>
              <DialogDescription>Role names must be UPPERCASE</DialogDescription>
            </DialogHeader>
            <div className="space-y-4">
              <div className="space-y-2">
                <Label>Name *</Label>
                <Input
                  value={form.name}
                  onChange={(e) => setForm({ ...form, name: e.target.value.toUpperCase() })}
                  placeholder="MANAGER"
                />
              </div>
              <div className="space-y-2">
                <Label>Description</Label>
                <Input
                  value={form.description}
                  onChange={(e) => setForm({ ...form, description: e.target.value })}
                  placeholder="Manager role"
                />
              </div>
            </div>
            <DialogFooter>
              <Button variant="outline" onClick={() => setOpen(false)}>Cancel</Button>
              <Button onClick={handleCreate} disabled={createRole.isPending}>
                {createRole.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                Create
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </div>

      <Tabs defaultValue="roles">
        <TabsList>
          <TabsTrigger value="roles">Roles ({roles.length})</TabsTrigger>
          <TabsTrigger value="permissions">All Permissions ({permissions.length})</TabsTrigger>
        </TabsList>

        <TabsContent value="roles">
          <Card>
            <CardHeader>
              <CardTitle>Roles</CardTitle>
              <CardDescription>System roles cannot be modified or deleted</CardDescription>
            </CardHeader>
            <CardContent>
              {isLoading ? (
                <div className="space-y-2">
                  {[1, 2, 3].map((i) => <Skeleton key={i} className="h-12 w-full" />)}
                </div>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>ID</TableHead>
                      <TableHead>Name</TableHead>
                      <TableHead>Description</TableHead>
                      <TableHead>Type</TableHead>
                      <TableHead>Permissions</TableHead>
                      <TableHead className="text-right">Actions</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {roles.map((r) => (
                      <TableRow key={r.id}>
                        <TableCell className="font-mono text-xs">{r.id}</TableCell>
                        <TableCell className="font-medium">
                          <div className="flex items-center gap-2">
                            {r.name}
                            {r.systemRole && <Lock className="h-3 w-3 text-muted-foreground" />}
                          </div>
                        </TableCell>
                        <TableCell className="text-muted-foreground">
                          {r.description || "—"}
                        </TableCell>
                        <TableCell>
                          <Badge variant={r.systemRole ? "default" : "secondary"}>
                            {r.systemRole ? "System" : "Custom"}
                          </Badge>
                        </TableCell>
                        <TableCell>
                          <span className="text-sm text-muted-foreground">
                            {r.permissions.length} permission(s)
                          </span>
                        </TableCell>
                        <TableCell className="text-right">
                          <div className="flex justify-end gap-2">
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => openPermissionsDialog(r.id, r.permissions)}
                            >
                              <Shield className="mr-1 h-3 w-3" />
                              Permissions
                            </Button>
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => {
                                if (confirm(`Delete role "${r.name}"?`)) {
                                  deleteRole.mutate(r.id)
                                }
                              }}
                              disabled={r.systemRole}
                            >
                              <Trash2 className="h-4 w-4" />
                            </Button>
                          </div>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="permissions">
          <Card>
            <CardHeader>
              <CardTitle>All Permissions</CardTitle>
              <CardDescription>Available permissions in the system</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="grid gap-2 md:grid-cols-2 lg:grid-cols-3">
                {permissions.map((p) => (
                  <div key={p.id} className="flex items-center gap-2 rounded-md border p-3">
                    <Shield className="h-4 w-4 text-muted-foreground" />
                    <div>
                      <p className="font-mono text-sm">{p.code}</p>
                      <p className="text-xs text-muted-foreground">{p.description || "—"}</p>
                    </div>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      {/* Permissions Dialog */}
      <Dialog open={permDialogOpen} onOpenChange={setPermDialogOpen}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>Assign Permissions</DialogTitle>
            <DialogDescription>
              Select permissions for this role
            </DialogDescription>
          </DialogHeader>
          <div className="grid max-h-[400px] gap-2 overflow-y-auto md:grid-cols-2">
            {permissions.map((p) => {
              const checked = selectedPermIds.includes(p.id)
              return (
                <button
                  key={p.id}
                  type="button"
                  onClick={() => togglePerm(p.id)}
                  className={cn(
                    "flex items-center gap-2 rounded-md border p-3 text-left transition-colors",
                    checked ? "border-primary bg-primary/5" : "hover:bg-accent"
                  )}
                >
                  <div className={cn(
                    "flex h-4 w-4 items-center justify-center rounded border",
                    checked && "border-primary bg-primary"
                  )}>
                    {checked && <span className="text-xs text-primary-foreground">✓</span>}
                  </div>
                  <div>
                    <p className="font-mono text-sm">{p.code}</p>
                    <p className="text-xs text-muted-foreground">{p.description || "—"}</p>
                  </div>
                </button>
              )
            })}
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setPermDialogOpen(false)}>Cancel</Button>
            <Button onClick={handleSavePermissions} disabled={assignPerms.isPending}>
              {assignPerms.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              Save
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
