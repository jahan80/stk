import { useState } from "react"
import {
  useRateLimits,
  useToggleRateLimit,
  useUpdateRateLimit,
  useCreateRateLimit,
  useDeleteRateLimit,
  useResetRateLimit,
  useResetAllRateLimits,
} from "@/features/admin/hooks"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Badge } from "@/components/ui/badge"
import { Switch } from "@/components/ui/switch"
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from "@/components/ui/table"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle,
} from "@/components/ui/dialog"
import {
  AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent,
  AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import { Gauge, Power, Edit, Loader2, Plus, Trash2, RotateCcw } from "lucide-react"
import type { RateLimit, RateLimitRequest } from "@/types/rateLimits"

const EMPTY_CREATE: RateLimitRequest = {
  pathPattern: "",
  method: "",
  keyType: "IP_PATH",
  requestsPerWindow: 100,
  windowSeconds: 60,
  burstCapacity: 100,
  priority: 0,
  description: "",
  enabled: true,
}

export function RateLimitsPage() {
  const { data, isLoading } = useRateLimits()
  const toggle = useToggleRateLimit()
  const update = useUpdateRateLimit()
  const create = useCreateRateLimit()
  const deleteLimit = useDeleteRateLimit()
  const resetLimit = useResetRateLimit()
  const resetAll = useResetAllRateLimits()

  const [creating, setCreating] = useState(false)
  const [createForm, setCreateForm] = useState<RateLimitRequest>(EMPTY_CREATE)

  const [editing, setEditing] = useState<RateLimit | null>(null)
  const [editForm, setEditForm] = useState({
    requestsPerWindow: 0,
    windowSeconds: 0,
    burstCapacity: 0,
  })

  const [deleting, setDeleting] = useState<RateLimit | null>(null)
  const [resetting, setResetting] = useState<RateLimit | null>(null)
  const [resettingAll, setResettingAll] = useState(false)

  const limits = data?.data ?? []

  const openEdit = (r: RateLimit) => {
    setEditing(r)
    setEditForm({
      requestsPerWindow: r.requestsPerWindow,
      windowSeconds: r.windowSeconds,
      burstCapacity: r.burstCapacity ?? r.requestsPerWindow,
    })
  }

  const handleCreate = () => {
    if (!createForm.pathPattern.trim()) return
    create.mutate(
      {
        ...createForm,
        method: createForm.method || undefined,
        description: createForm.description || undefined,
      },
      {
        onSuccess: () => {
          setCreating(false)
          setCreateForm(EMPTY_CREATE)
        },
      }
    )
  }

  const handleSave = () => {
    if (!editing) return
    update.mutate(
      {
        id: editing.id,
        data: {
          pathPattern: editing.pathPattern,
          method: editing.method,
          keyType: editing.keyType,
          requestsPerWindow: editForm.requestsPerWindow,
          windowSeconds: editForm.windowSeconds,
          burstCapacity: editForm.burstCapacity,
          priority: editing.priority,
          description: editing.description,
          enabled: editing.enabled,
        },
      },
      { onSuccess: () => setEditing(null) }
    )
  }

  const handleDelete = () => {
    if (!deleting) return
    deleteLimit.mutate(deleting.id, {
      onSuccess: () => setDeleting(null),
    })
  }

  const handleReset = () => {
    if (!resetting) return
    resetLimit.mutate(resetting.id, {
      onSuccess: () => setResetting(null),
    })
  }

  const handleResetAll = () => {
    resetAll.mutate(undefined, {
      onSuccess: () => setResettingAll(false),
    })
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">Rate Limits</h1>
          <p className="text-muted-foreground">
            {limits.length} rate limit config(s) in gateway
          </p>
        </div>
        <div className="flex gap-2">
          <Button
            variant="outline"
            onClick={() => setResettingAll(true)}
            disabled={resetAll.isPending || limits.length === 0}
          >
            {resetAll.isPending ? (
              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
            ) : (
              <RotateCcw className="mr-2 h-4 w-4" />
            )}
            Reset All
          </Button>
          <Button onClick={() => { setCreateForm(EMPTY_CREATE); setCreating(true) }}>
            <Plus className="mr-2 h-4 w-4" />
            New Rate Limit
          </Button>
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Rate Limits</CardTitle>
          <CardDescription>
            Configure request limits per path/method
          </CardDescription>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="space-y-2">
              {[1, 2, 3, 4].map((i) => <Skeleton key={i} className="h-12 w-full" />)}
            </div>
          ) : limits.length === 0 ? (
            <div className="py-12 text-center text-muted-foreground">
              <Gauge className="mx-auto mb-2 h-8 w-8" />
              No rate limits configured
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Path</TableHead>
                  <TableHead>Method</TableHead>
                  <TableHead>Key</TableHead>
                  <TableHead>Limit</TableHead>
                  <TableHead>Window</TableHead>
                  <TableHead>Priority</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {limits.map((r) => (
                  <TableRow key={r.id}>
                    <TableCell className="font-mono text-xs">{r.pathPattern}</TableCell>
                    <TableCell>
                      {r.method ? (
                        <Badge variant="outline">{r.method}</Badge>
                      ) : (
                        <span className="text-muted-foreground">*</span>
                      )}
                    </TableCell>
                    <TableCell className="text-xs">{r.keyType}</TableCell>
                    <TableCell className="font-medium">{r.requestsPerWindow}</TableCell>
                    <TableCell className="text-sm">{r.windowSeconds}s</TableCell>
                    <TableCell className="text-sm">{r.priority}</TableCell>
                    <TableCell>
                      <Badge variant={r.enabled ? "success" : "secondary"}>
                        {r.enabled ? "Enabled" : "Disabled"}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex justify-end gap-1">
                        <Button variant="ghost" size="sm" onClick={() => openEdit(r)} title="Edit">
                          <Edit className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => toggle.mutate({ id: r.id, enable: !r.enabled })}
                          title={r.enabled ? "Disable" : "Enable"}
                        >
                          <Power className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => setResetting(r)}
                          title="Reset to default"
                        >
                          <RotateCcw className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => setDeleting(r)}
                          title="Delete"
                        >
                          <Trash2 className="h-4 w-4 text-destructive" />
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

      {/* Create Dialog */}
      <Dialog open={creating} onOpenChange={setCreating}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>New Rate Limit</DialogTitle>
            <DialogDescription>
              Define a path pattern and the request limit for it.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-2">
                <Label>Path pattern *</Label>
                <Input
                  placeholder="/auth/login or /auth/**"
                  value={createForm.pathPattern}
                  onChange={(e) => setCreateForm({ ...createForm, pathPattern: e.target.value })}
                />
              </div>
              <div className="space-y-2">
                <Label>Method</Label>
                <select
                  value={createForm.method ?? ""}
                  onChange={(e) => setCreateForm({ ...createForm, method: e.target.value })}
                  className="h-10 w-full rounded-md border border-input bg-background px-3 text-sm"
                >
                  <option value="">Any</option>
                  <option value="GET">GET</option>
                  <option value="POST">POST</option>
                  <option value="PUT">PUT</option>
                  <option value="PATCH">PATCH</option>
                  <option value="DELETE">DELETE</option>
                </select>
              </div>
            </div>

            <div className="space-y-2">
              <Label>Key type *</Label>
              <select
                value={createForm.keyType}
                onChange={(e) => setCreateForm({ ...createForm, keyType: e.target.value as RateLimitRequest["keyType"] })}
                className="h-10 w-full rounded-md border border-input bg-background px-3 text-sm"
              >
                <option value="IP">IP</option>
                <option value="IP_PATH">IP + Path</option>
                <option value="USER">User</option>
                <option value="USER_PATH">User + Path</option>
              </select>
            </div>

            <div className="grid grid-cols-3 gap-3">
              <div className="space-y-2">
                <Label>Requests / Window *</Label>
                <Input
                  type="number"
                  value={createForm.requestsPerWindow}
                  onChange={(e) => setCreateForm({ ...createForm, requestsPerWindow: Number(e.target.value) })}
                />
              </div>
              <div className="space-y-2">
                <Label>Window (seconds) *</Label>
                <Input
                  type="number"
                  value={createForm.windowSeconds}
                  onChange={(e) => setCreateForm({ ...createForm, windowSeconds: Number(e.target.value) })}
                />
              </div>
              <div className="space-y-2">
                <Label>Burst capacity</Label>
                <Input
                  type="number"
                  value={createForm.burstCapacity ?? 0}
                  onChange={(e) => setCreateForm({ ...createForm, burstCapacity: Number(e.target.value) })}
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-2">
                <Label>Priority</Label>
                <Input
                  type="number"
                  value={createForm.priority}
                  onChange={(e) => setCreateForm({ ...createForm, priority: Number(e.target.value) })}
                />
              </div>
              <div className="space-y-2">
                <Label>Description</Label>
                <Input
                  value={createForm.description ?? ""}
                  onChange={(e) => setCreateForm({ ...createForm, description: e.target.value })}
                />
              </div>
            </div>

            <div className="flex items-center justify-between">
              <Label>Enabled</Label>
              <Switch
                checked={createForm.enabled}
                onCheckedChange={(v) => setCreateForm({ ...createForm, enabled: v })}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setCreating(false)}>Cancel</Button>
            <Button
              onClick={handleCreate}
              disabled={create.isPending || !createForm.pathPattern.trim()}
            >
              {create.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              Create
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Edit Dialog */}
      <Dialog open={!!editing} onOpenChange={(o) => !o && setEditing(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Edit Rate Limit</DialogTitle>
            <DialogDescription className="font-mono text-xs">
              {editing?.pathPattern} {editing?.method ? `[${editing.method}]` : ""}
            </DialogDescription>
          </DialogHeader>
          {editing && (
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-2">
                  <Label>Requests / Window</Label>
                  <Input
                    type="number"
                    value={editForm.requestsPerWindow}
                    onChange={(e) => setEditForm({ ...editForm, requestsPerWindow: Number(e.target.value) })}
                  />
                </div>
                <div className="space-y-2">
                  <Label>Window (seconds)</Label>
                  <Input
                    type="number"
                    value={editForm.windowSeconds}
                    onChange={(e) => setEditForm({ ...editForm, windowSeconds: Number(e.target.value) })}
                  />
                </div>
              </div>
              <div className="space-y-2">
                <Label>Burst capacity</Label>
                <Input
                  type="number"
                  value={editForm.burstCapacity}
                  onChange={(e) => setEditForm({ ...editForm, burstCapacity: Number(e.target.value) })}
                />
              </div>
              <div className="flex items-center justify-between">
                <Label>Enabled</Label>
                <Switch
                  checked={editing.enabled}
                  onCheckedChange={(v) => setEditing({ ...editing, enabled: v })}
                />
              </div>
            </div>
          )}
          <DialogFooter>
            <Button variant="outline" onClick={() => setEditing(null)}>Cancel</Button>
            <Button onClick={handleSave} disabled={update.isPending}>
              {update.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              Save
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation */}
      <AlertDialog open={!!deleting} onOpenChange={(o) => !o && setDeleting(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete rate limit?</AlertDialogTitle>
            <AlertDialogDescription>
              This will permanently delete the rate limit for{" "}
              <span className="font-mono font-medium">{deleting?.pathPattern}</span>
              {deleting?.method && ` [${deleting.method}]`}.
              This action cannot be undone.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={deleteLimit.isPending}>Cancel</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDelete}
              disabled={deleteLimit.isPending}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              {deleteLimit.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              Delete
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* Reset Single Confirmation */}
      <AlertDialog open={!!resetting} onOpenChange={(o) => !o && setResetting(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Reset to default?</AlertDialogTitle>
            <AlertDialogDescription>
              Reset the rate limit for{" "}
              <span className="font-mono font-medium">{resetting?.pathPattern}</span>
              {resetting?.method && ` [${resetting.method}]`}{" "}
              to its default values?
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={resetLimit.isPending}>Cancel</AlertDialogCancel>
            <AlertDialogAction onClick={handleReset} disabled={resetLimit.isPending}>
              {resetLimit.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              Reset
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* Reset All Confirmation */}
      <AlertDialog open={resettingAll} onOpenChange={setResettingAll}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Reset ALL rate limits?</AlertDialogTitle>
            <AlertDialogDescription>
              This will reset <span className="font-medium">all {limits.length}</span> rate
              limit configurations to their default values. This action cannot be undone.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={resetAll.isPending}>Cancel</AlertDialogCancel>
            <AlertDialogAction onClick={handleResetAll} disabled={resetAll.isPending}>
              {resetAll.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              Reset All
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}
