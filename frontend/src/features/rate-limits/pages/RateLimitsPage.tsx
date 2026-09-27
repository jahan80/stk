import { useState } from "react"
import { useRateLimits, useToggleRateLimit, useUpdateRateLimit } from "@/features/admin/hooks"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Badge } from "@/components/ui/badge"
import { Switch } from "@/components/ui/switch"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle,
} from "@/components/ui/dialog"
import { Gauge, Power, Edit, Loader2 } from "lucide-react"
import type { RateLimit } from "@/types/rateLimits"

export function RateLimitsPage() {
  const { data, isLoading } = useRateLimits()
  const toggle = useToggleRateLimit()
  const update = useUpdateRateLimit()

  const [editing, setEditing] = useState<RateLimit | null>(null)
  const [form, setForm] = useState({
    requestsPerWindow: 0,
    windowSeconds: 0,
    burstCapacity: 0,
  })

  const limits = data?.data ?? []

  const openEdit = (r: RateLimit) => {
    setEditing(r)
    setForm({
      requestsPerWindow: r.requestsPerWindow,
      windowSeconds: r.windowSeconds,
      burstCapacity: r.burstCapacity ?? r.requestsPerWindow,
    })
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
          requestsPerWindow: form.requestsPerWindow,
          windowSeconds: form.windowSeconds,
          burstCapacity: form.burstCapacity,
          priority: editing.priority,
          description: editing.description,
          enabled: editing.enabled,
        },
      },
      { onSuccess: () => setEditing(null) }
    )
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold">Rate Limits</h1>
        <p className="text-muted-foreground">
          {limits.length} rate limit config(s) in gateway
        </p>
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
                      {r.method ? <Badge variant="outline">{r.method}</Badge> : <span className="text-muted-foreground">*</span>}
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
                      <div className="flex justify-end gap-2">
                        <Button variant="ghost" size="sm" onClick={() => openEdit(r)}>
                          <Edit className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => toggle.mutate({ id: r.id, enable: !r.enabled })}
                        >
                          <Power className="h-4 w-4" />
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
                    value={form.requestsPerWindow}
                    onChange={(e) => setForm({ ...form, requestsPerWindow: Number(e.target.value) })}
                  />
                </div>
                <div className="space-y-2">
                  <Label>Window (seconds)</Label>
                  <Input
                    type="number"
                    value={form.windowSeconds}
                    onChange={(e) => setForm({ ...form, windowSeconds: Number(e.target.value) })}
                  />
                </div>
              </div>
              <div className="space-y-2">
                <Label>Burst capacity</Label>
                <Input
                  type="number"
                  value={form.burstCapacity}
                  onChange={(e) => setForm({ ...form, burstCapacity: Number(e.target.value) })}
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
    </div>
  )
}
