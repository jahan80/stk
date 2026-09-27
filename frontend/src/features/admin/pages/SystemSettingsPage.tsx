import { useState } from "react"
import { useConfigs, useUpdateConfig, useResetConfig } from "../hooks"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Badge } from "@/components/ui/badge"
import { Switch } from "@/components/ui/switch"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle,
} from "@/components/ui/dialog"
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from "@/components/ui/select"
import { Settings, RotateCcw, Save, Search, Loader2 } from "lucide-react"
import type { Configuration } from "@/types/configs"

export function SystemSettingsPage() {
  const { data, isLoading } = useConfigs()
  const updateConfig = useUpdateConfig()
  const resetConfig = useResetConfig()

  const [search, setSearch] = useState("")
  const [editing, setEditing] = useState<Configuration | null>(null)
  const [editValue, setEditValue] = useState("")

  const configs = data?.data ?? []

  const filtered = configs.filter((c) =>
    c.configKey.toLowerCase().includes(search.toLowerCase()) ||
    (c.description ?? "").toLowerCase().includes(search.toLowerCase())
  )

  const grouped = filtered.reduce<Record<string, Configuration[]>>((acc, c) => {
    const group = c.configKey.split(".").slice(0, 2).join(".")
    if (!acc[group]) acc[group] = []
    acc[group].push(c)
    return acc
  }, {})

  const openEdit = (cfg: Configuration) => {
    setEditing(cfg)
    setEditValue(cfg.configValue)
  }

  const handleSave = () => {
    if (!editing) return
    updateConfig.mutate(
      {
        key: editing.configKey,
        data: {
          configKey: editing.configKey,
          configValue: editValue,
          defaultValue: editing.defaultValue,
          valueType: editing.valueType,
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
        <h1 className="text-3xl font-bold">System Settings</h1>
        <p className="text-muted-foreground">
          Configure application behavior ({configs.length} configurations)
        </p>
      </div>

      <div className="relative">
        <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          placeholder="Search configurations..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="pl-9"
        />
      </div>

      {isLoading ? (
        <div className="space-y-2">
          {[1, 2, 3, 4].map((i) => <Skeleton key={i} className="h-20 w-full" />)}
        </div>
      ) : (
        <div className="space-y-4">
          {Object.entries(grouped).map(([group, items]) => (
            <Card key={group}>
              <CardHeader className="pb-3">
                <CardTitle className="text-base font-mono">{group}</CardTitle>
                <CardDescription>{items.length} setting(s)</CardDescription>
              </CardHeader>
              <CardContent className="space-y-2">
                {items.map((c) => (
                  <div
                    key={c.configKey}
                    className="flex items-center gap-4 rounded-md border p-3"
                  >
                    <Settings className="h-4 w-4 text-muted-foreground" />
                    <div className="flex-1 overflow-hidden">
                      <div className="flex items-center gap-2">
                        <p className="font-mono text-sm font-medium truncate">
                          {c.configKey}
                        </p>
                        <Badge variant="outline" className="text-xs">
                          {c.valueType}
                        </Badge>
                        {!c.enabled && (
                          <Badge variant="destructive" className="text-xs">
                            DISABLED
                          </Badge>
                        )}
                      </div>
                      <p className="truncate text-xs text-muted-foreground">
                        {c.description || "—"}
                      </p>
                    </div>
                    <div className="flex items-center gap-2">
                      <code className="rounded bg-muted px-2 py-1 text-xs font-mono">
                        {c.configValue.length > 30
                          ? c.configValue.slice(0, 30) + "…"
                          : c.configValue}
                      </code>
                      <Button variant="outline" size="sm" onClick={() => openEdit(c)}>
                        Edit
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => {
                          if (confirm(`Reset "${c.configKey}" to default?`)) {
                            resetConfig.mutate(c.configKey)
                          }
                        }}
                        title="Reset to default"
                      >
                        <RotateCcw className="h-4 w-4" />
                      </Button>
                    </div>
                  </div>
                ))}
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Edit Dialog */}
      <Dialog open={!!editing} onOpenChange={(o) => !o && setEditing(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Edit Configuration</DialogTitle>
            <DialogDescription className="font-mono text-xs">
              {editing?.configKey}
            </DialogDescription>
          </DialogHeader>
          {editing && (
            <div className="space-y-4">
              <div className="space-y-2">
                <Label>Value</Label>
                {editing.valueType === "BOOLEAN" ? (
                  <Select value={editValue} onValueChange={setEditValue}>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="true">true</SelectItem>
                      <SelectItem value="false">false</SelectItem>
                    </SelectContent>
                  </Select>
                ) : (
                  <Input
                    value={editValue}
                    onChange={(e) => setEditValue(e.target.value)}
                    type={
                      editing.valueType === "INTEGER" || editing.valueType === "LONG"
                        ? "number"
                        : "text"
                    }
                  />
                )}
              </div>

              <div className="rounded-md bg-muted p-3 text-xs">
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Default:</span>
                  <span className="font-mono">{editing.defaultValue}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Type:</span>
                  <span className="font-mono">{editing.valueType}</span>
                </div>
              </div>

              <div className="flex items-center justify-between">
                <Label htmlFor="enabled">Enabled</Label>
                <Switch
                  id="enabled"
                  checked={editing.enabled}
                  onCheckedChange={(v) => setEditing({ ...editing, enabled: v })}
                />
              </div>
            </div>
          )}
          <DialogFooter>
            <Button variant="outline" onClick={() => setEditing(null)}>Cancel</Button>
            <Button onClick={handleSave} disabled={updateConfig.isPending}>
              {updateConfig.isPending ? (
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              ) : (
                <Save className="mr-2 h-4 w-4" />
              )}
              Save
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
