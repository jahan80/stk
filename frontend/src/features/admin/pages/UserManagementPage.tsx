import { useState } from "react"
import {
  useUsers,
  useUser,
  useToggleUser,
  useAssignRole,
  useRoles,
  useCreateUser,
} from "../hooks"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Badge } from "@/components/ui/badge"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import {
  Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle,
} from "@/components/ui/dialog"
import {
  AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent,
  AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import { useAuthStore } from "@/store/auth"
import {
  UserPlus, Power, Loader2, Search, Eye, CheckCircle2, XCircle,
} from "lucide-react"
import type { UserSummaryResponse } from "@/types/auth"

export function UserManagementPage() {
  const currentUser = useAuthStore((s) => s.user)
  const { data, isLoading } = useUsers()
  const { data: rolesData } = useRoles()
  const toggle = useToggleUser()
  const assignRole = useAssignRole()
  const createUser = useCreateUser()

  // Create
  const [creating, setCreating] = useState(false)
  const [form, setForm] = useState({ username: "", password: "", email: "", roleId: "" })

  // Detail view
  const [viewingId, setViewingId] = useState<number | null>(null)
  const { data: userDetail, isLoading: isLoadingDetail } = useUser(viewingId)

  // Toggle confirmation
  const [toggling, setToggling] = useState<UserSummaryResponse | null>(null)

  // Search
  const [search, setSearch] = useState("")

  const users = data?.data ?? []
  const roles = rolesData?.data ?? []

  const filtered = users.filter((u) =>
    u.username.toLowerCase().includes(search.toLowerCase()) ||
    (u.email ?? "").toLowerCase().includes(search.toLowerCase()) ||
    u.role.toLowerCase().includes(search.toLowerCase())
  )

  const handleCreate = () => {
    if (!form.username || !form.password || !form.roleId) return
    createUser.mutate(
      {
        username: form.username,
        password: form.password,
        email: form.email || undefined,
        roleId: Number(form.roleId),
      },
      {
        onSuccess: () => {
          setCreating(false)
          setForm({ username: "", password: "", email: "", roleId: "" })
        },
      }
    )
  }

  const handleToggle = () => {
    if (!toggling) return
    toggle.mutate(
      { id: toggling.id, enable: !toggling.enabled },
      { onSuccess: () => setToggling(null) }
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">User Management</h1>
          <p className="text-muted-foreground">Manage users, roles, and access</p>
        </div>
        <Button onClick={() => { setForm({ username: "", password: "", email: "", roleId: "" }); setCreating(true) }}>
          <UserPlus className="mr-2 h-4 w-4" />
          Create User
        </Button>
      </div>

      <div className="relative">
        <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          placeholder="Search by username, email, or role..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="pl-9"
        />
      </div>

      <Card>
        <CardHeader>
          <CardTitle>
            All Users ({filtered.length}
            {search && ` of ${users.length}`})
          </CardTitle>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="space-y-2">
              {[1, 2, 3].map((i) => <Skeleton key={i} className="h-12 w-full" />)}
            </div>
          ) : filtered.length === 0 ? (
            <div className="py-12 text-center text-muted-foreground">
              {search ? "No users match your search" : "No users"}
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>ID</TableHead>
                  <TableHead>Username</TableHead>
                  <TableHead>Email</TableHead>
                  <TableHead>Role</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filtered.map((u) => {
                  const isSelf = u.id === currentUser?.userId
                  return (
                    <TableRow key={u.id}>
                      <TableCell className="font-mono text-xs">{u.id}</TableCell>
                      <TableCell className="font-medium">
                        {u.username}
                        {isSelf && (
                          <Badge variant="outline" className="ml-2 text-xs">You</Badge>
                        )}
                      </TableCell>
                      <TableCell className="text-muted-foreground">{u.email || "—"}</TableCell>
                      <TableCell>
                        <Select
                          value={roles.find((r) => r.name === u.role)?.id?.toString() ?? ""}
                          onValueChange={(v) => assignRole.mutate({ id: u.id, data: { roleId: Number(v) } })}
                          disabled={isSelf}
                        >
                          <SelectTrigger className="w-32">
                            <SelectValue />
                          </SelectTrigger>
                          <SelectContent>
                            {roles.map((r) => (
                              <SelectItem key={r.id} value={String(r.id)}>{r.name}</SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      </TableCell>
                      <TableCell>
                        <Badge variant={u.enabled ? "success" : "destructive"}>
                          {u.enabled ? "Active" : "Disabled"}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-right">
                        <div className="flex justify-end gap-1">
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => setViewingId(u.id)}
                            title="View details"
                          >
                            <Eye className="h-4 w-4" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => setToggling(u)}
                            disabled={isSelf}
                            title={isSelf ? "You cannot disable yourself" : (u.enabled ? "Disable" : "Enable")}
                          >
                            <Power className="h-4 w-4" />
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  )
                })}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      {/* Create Dialog */}
      <Dialog open={creating} onOpenChange={setCreating}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Create New User</DialogTitle>
            <DialogDescription>Add a new user to the system</DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-2">
              <Label>Username *</Label>
              <Input
                value={form.username}
                onChange={(e) => setForm({ ...form, username: e.target.value })}
              />
            </div>
            <div className="space-y-2">
              <Label>Password *</Label>
              <Input
                type="password"
                value={form.password}
                onChange={(e) => setForm({ ...form, password: e.target.value })}
              />
            </div>
            <div className="space-y-2">
              <Label>Email</Label>
              <Input
                type="email"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
              />
            </div>
            <div className="space-y-2">
              <Label>Role *</Label>
              <Select value={form.roleId} onValueChange={(v) => setForm({ ...form, roleId: v })}>
                <SelectTrigger>
                  <SelectValue placeholder="Select role" />
                </SelectTrigger>
                <SelectContent>
                  {roles.map((r) => (
                    <SelectItem key={r.id} value={String(r.id)}>{r.name}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setCreating(false)}>Cancel</Button>
            <Button
              onClick={handleCreate}
              disabled={createUser.isPending || !form.username || !form.password || !form.roleId}
            >
              {createUser.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              Create
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Detail Dialog */}
      <Dialog open={viewingId != null} onOpenChange={(o) => !o && setViewingId(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>User Details</DialogTitle>
            <DialogDescription>
              {userDetail?.data?.username ?? "Loading..."}
            </DialogDescription>
          </DialogHeader>
          {isLoadingDetail ? (
            <div className="space-y-2">
              {[1, 2, 3, 4].map((i) => <Skeleton key={i} className="h-8 w-full" />)}
            </div>
          ) : userDetail?.data ? (
            <div className="space-y-3 text-sm">
              <DetailRow label="ID" value={String(userDetail.data.id)} />
              <DetailRow label="Username" value={userDetail.data.username} />
              <DetailRow label="Email" value={userDetail.data.email || "—"} />
              <DetailRow label="First Name" value={userDetail.data.firstName || "—"} />
              <DetailRow label="Last Name" value={userDetail.data.lastName || "—"} />
              <DetailRow label="Mobile" value={userDetail.data.mobileNumber || "—"} />
              <DetailRow label="Role" value={userDetail.data.role} />
              <DetailRow
                label="Status"
                value={
                  <Badge variant={userDetail.data.enabled ? "success" : "destructive"}>
                    {userDetail.data.enabled ? "Active" : "Disabled"}
                  </Badge>
                }
              />
              <DetailRow
                label="Email Verified"
                value={
                  userDetail.data.emailVerified
                    ? <CheckCircle2 className="h-4 w-4 text-green-500" />
                    : <XCircle className="h-4 w-4 text-muted-foreground" />
                }
              />
              <DetailRow
                label="Mobile Verified"
                value={
                  userDetail.data.mobileVerified
                    ? <CheckCircle2 className="h-4 w-4 text-green-500" />
                    : <XCircle className="h-4 w-4 text-muted-foreground" />
                }
              />
              <DetailRow label="Created" value={new Date(userDetail.data.createdAt).toLocaleString()} />
              <DetailRow label="Updated" value={new Date(userDetail.data.updatedAt).toLocaleString()} />
            </div>
          ) : (
            <div className="py-6 text-center text-muted-foreground">
              Failed to load user details
            </div>
          )}
          <DialogFooter>
            <Button variant="outline" onClick={() => setViewingId(null)}>Close</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Toggle Confirmation */}
      <AlertDialog open={!!toggling} onOpenChange={(o) => !o && setToggling(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>
              {toggling?.enabled ? "Disable user?" : "Enable user?"}
            </AlertDialogTitle>
            <AlertDialogDescription>
              {toggling?.enabled
                ? `This will prevent "${toggling?.username}" from logging in.`
                : `This will allow "${toggling?.username}" to log in again.`}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={toggle.isPending}>Cancel</AlertDialogCancel>
            <AlertDialogAction onClick={handleToggle} disabled={toggle.isPending}>
              {toggle.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              {toggling?.enabled ? "Disable" : "Enable"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}

function DetailRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="flex items-center justify-between border-b pb-2 last:border-0">
      <span className="text-muted-foreground">{label}</span>
      <span className="font-medium">{value}</span>
    </div>
  )
}
