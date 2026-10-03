import { useState } from "react"
import {
  useConfigs,
  useUpdateConfig,
  useResetConfig,
  useCreateConfig,
  useDeleteConfig,
  useResetAllConfigs,
} from "../hooks"
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
  AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent,
  AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from "@/components/ui/select"
import {
  Settings, RotateCcw, Save, Search, Loader2, Plus, Trash2,
} from "lucide-react"
import type { Configuration, ConfigurationRequest } from "@/types/configs"

const EMPTY_CONFIG: ConfigurationRequest = {
  configKey: "",
  configValue: "",
  defaultValue: "",
  valueType: "STRING",
  description: "",
  enabled: true,
}

const VALUE_TYPES = ["STRING", "INTEGER", "LONG", "BOOLEAN", "DOUBLE"]

export function SystemSettingsPage() {
  const { data, isLoading } = useConfigs()
  const updateConfig = useUpdateConfig()
  const resetConfig = useResetConfig()
  const createConfig = useCreateConfig()
  const deleteConfig = useDeleteConfig()
  const resetAllConfigs = useResetAllConfigs()

  const [search, setSearch] = useState("")

  // Edit
  const [editing, setEditing] = useState<Configuration | null>(null)
  const [editValue, setEditValue] = useState("")

  // Create
  const [creating, setCreating] = useState(false)
  const [createForm, setCreateForm] = useState<ConfigurationRequest>(EMPTY_CONFIG)

  // Delete
  const [deleting, setDeleting] = useState<Configuration | null>(null)

  // Reset single
  const [resetting, setResetting] = useState<Configuration | null>(null)

  // Reset all
  const [resettingAll, setResettingAll] = useState(false)

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

  const handleCreate = () => {
    if (!createForm.configKey.trim()) return
    createConfig.mutate(
      {
        ...createForm,
        description: createForm.description || undefined,
      },
      {
        onSuccess: () => {
          setCreating(false)
          setCreateForm(EMPTY_CONFIG)
        },
      }
    )
  }

  const handleDelete = () => {
    if (!deleting) return
    deleteConfig.mutate(deleting.configKey, {
      onSuccess: () => setDeleting(null),
    })
  }

  const handleReset = () => {
    if (!resetting) return
    resetConfig.mutate(resetting.configKey, {
      onSuccess: () => setResetting(null),
    })
  }

  const handleResetAll = () => {
    resetAllConfigs.mutate(undefined, {
      onSuccess: () => setResettingAll(false),
    })
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">System Settings</h1>
          <p className="text-muted-foreground">
            Configure application behavior ({configs.length} configurations)
          </p>
        </div>
        <div className="flex gap-2">
          <Button
            variant="outline"
            onClick={() => setResettingAll(true)}
            disabled={resetAllConfigs.isPending || configs.length === 0}
          >
            {resetAllConfigs.isPending ? (
              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
            ) : (
              <RotateCcw className="mr-2 h-4 w-4" />
            )}
            Reset All
          </Button>
          <Button onClick={() => { setCreateForm(EMPTY_CONFIG); setCreating(true) }}>
            <Plus className="mr-2 h-4 w-4" />
            New Config
          </Button>
        </div>
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
      ) : filtered.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center text-muted-foreground">
            <Settings className="mx-auto mb-2 h-8 w-8" />
            {search ? "No configurations match your search" : "No configurations"}
          </CardContent>
        </Card>
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
                    <Settings className="h-4 w-4 text-muted-foreground shrink-0" />
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
                    <div className="flex items-center gap-1">
                      <code className="rounded bg-muted px-2 py-1 text-xs font-mono max-w-[200px] truncate">
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
                        onClick={() => setResetting(c)}
                        title="Reset to default"
                      >
                        <RotateCcw className="h-4 w-4" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => setDeleting(c)}
                        title="Delete"
                      >
                        <Trash2 className="h-4 w-4 text-destructive" />
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

      {/* Create Dialog */}
      <Dialog open={creating} onOpenChange={setCreating}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>New Configuration</DialogTitle>
            <DialogDescription>
              Create a new configuration key.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-2">
              <Label>Key *</Label>
              <Input
                placeholder="app.feature.enabled"
                value={createForm.configKey}
                onChange={(e) => setCreateForm({ ...createForm, configKey: e.target.value })}
              />
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-2">
                <Label>Type *</Label>
                <Select
                  value={createForm.valueType}
                  onValueChange={(v) => setCreateForm({ ...createForm, valueType: v })}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {VALUE_TYPES.map((t) => (
                      <SelectItem key={t} value={t}>{t}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label>Default value *</Label>
                <Input
                  value={createForm.defaultValue}
                  onChange={(e) => setCreateForm({ ...createForm, defaultValue: e.target.value })}
                />
              </div>
            </div>

            <div className="space-y-2">
              <Label>Value</Label>
              <Input
                value={createForm.configValue}
                onChange={(e) => setCreateForm({ ...createForm, configValue: e.target.value })}
              />
            </div>

            <div className="space-y-2">
              <Label>Description</Label>
              <Input
                value={createForm.description ?? ""}
                onChange={(e) => setCreateForm({ ...createForm, description: e.target.value })}
              />
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
              disabled={createConfig.isPending || !createForm.configKey.trim()}
            >
              {createConfig.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              Create
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation */}
      <AlertDialog open={!!deleting} onOpenChange={(o) => !o && setDeleting(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete configuration?</AlertDialogTitle>
            <AlertDialogDescription>
              This will permanently delete{" "}
              <span className="font-mono font-medium">{deleting?.configKey}</span>.
              This action cannot be undone.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={deleteConfig.isPending}>Cancel</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDelete}
              disabled={deleteConfig.isPending}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              {deleteConfig.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
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
              Reset{" "}
              <span className="font-mono font-medium">{resetting?.configKey}</span>{" "}
              to its default value{" "}
              <span className="font-mono">{resetting?.defaultValue}</span>?
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={resetConfig.isPending}>Cancel</AlertDialogCancel>
            <AlertDialogAction onClick={handleReset} disabled={resetConfig.isPending}>
              {resetConfig.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              Reset
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* Reset All Confirmation */}
      <AlertDialog open={resettingAll} onOpenChange={setResettingAll}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Reset ALL configurations?</AlertDialogTitle>
            <AlertDialogDescription>
              This will reset <span className="font-medium">all {configs.length}</span>{" "}
              configurations to their default values. This action cannot be undone.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={resetAllConfigs.isPending}>Cancel</AlertDialogCancel>
            <AlertDialogAction onClick={handleResetAll} disabled={resetAllConfigs.isPending}>
              {resetAllConfigs.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              Reset All
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}
