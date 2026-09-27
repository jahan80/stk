import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Label } from "@/components/ui/label"
import { Switch } from "@/components/ui/switch"
import { Button } from "@/components/ui/button"
import { Separator } from "@/components/ui/separator"
import { usePreferencesStore, type Theme, type FontSize, type AccentColor } from "@/store/preferences"
import { Moon, Sun, Monitor, Type, Palette, Sparkles, RotateCcw, Zap } from "lucide-react"
import { cn } from "@/lib/utils"

const THEMES: { value: Theme; label: string; icon: React.ElementType }[] = [
  { value: "light", label: "Light", icon: Sun },
  { value: "dark", label: "Dark", icon: Moon },
  { value: "system", label: "System", icon: Monitor },
]

const FONT_SIZES: { value: FontSize; label: string; sample: string }[] = [
  { value: "sm", label: "Small", sample: "Aa" },
  { value: "md", label: "Medium", sample: "Aa" },
  { value: "lg", label: "Large", sample: "Aa" },
  { value: "xl", label: "XL", sample: "Aa" },
]

const ACCENT_COLORS: { value: AccentColor; label: string; hex: string }[] = [
  { value: "blue", label: "Blue", hex: "#3b82f6" },
  { value: "violet", label: "Violet", hex: "#8b5cf6" },
  { value: "green", label: "Green", hex: "#22c55e" },
  { value: "rose", label: "Rose", hex: "#f43f5e" },
  { value: "orange", label: "Orange", hex: "#f97316" },
  { value: "slate", label: "Slate", hex: "#64748b" },
]

export function PreferencesCard() {
  const {
    theme, fontSize, accentColor, reducedMotion, compactMode,
    setTheme, setFontSize, setAccentColor, setReducedMotion, setCompactMode, reset,
  } = usePreferencesStore()

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Sparkles className="h-5 w-5" />
          Appearance
        </CardTitle>
        <CardDescription>
          Customize how the app looks. Settings are saved automatically.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">

        {/* Theme */}
        <div className="space-y-3">
          <Label className="flex items-center gap-2">
            <Sun className="h-4 w-4" />
            Theme
          </Label>
          <div className="grid grid-cols-3 gap-2">
            {THEMES.map((t) => {
              const Icon = t.icon
              const active = theme === t.value
              return (
                <button
                  key={t.value}
                  type="button"
                  onClick={() => setTheme(t.value)}
                  className={cn(
                    "flex flex-col items-center gap-2 rounded-lg border-2 p-3 transition-all hover:bg-accent",
                    active ? "border-primary bg-primary/5" : "border-border"
                  )}
                >
                  <Icon className={cn("h-5 w-5", active && "text-primary")} />
                  <span className="text-xs font-medium">{t.label}</span>
                </button>
              )
            })}
          </div>
        </div>

        <Separator />

        {/* Font size */}
        <div className="space-y-3">
          <Label className="flex items-center gap-2">
            <Type className="h-4 w-4" />
            Font size
          </Label>
          <div className="grid grid-cols-4 gap-2">
            {FONT_SIZES.map((f) => {
              const active = fontSize === f.value
              return (
                <button
                  key={f.value}
                  type="button"
                  onClick={() => setFontSize(f.value)}
                  className={cn(
                    "flex flex-col items-center gap-1 rounded-lg border-2 p-2 transition-all hover:bg-accent",
                    active ? "border-primary bg-primary/5" : "border-border"
                  )}
                >
                  <span
                    className={cn(
                      "font-semibold",
                      f.value === "sm" && "text-xs",
                      f.value === "md" && "text-sm",
                      f.value === "lg" && "text-base",
                      f.value === "xl" && "text-lg"
                    )}
                  >
                    {f.sample}
                  </span>
                  <span className="text-[10px] text-muted-foreground">{f.label}</span>
                </button>
              )
            })}
          </div>
        </div>

        <Separator />

        {/* Accent color */}
        <div className="space-y-3">
          <Label className="flex items-center gap-2">
            <Palette className="h-4 w-4" />
            Accent color
          </Label>
          <div className="flex flex-wrap gap-2">
            {ACCENT_COLORS.map((c) => {
              const active = accentColor === c.value
              return (
                <button
                  key={c.value}
                  type="button"
                  onClick={() => setAccentColor(c.value)}
                  title={c.label}
                  className={cn(
                    "h-9 w-9 rounded-full border-2 transition-all hover:scale-110",
                    active
                      ? "border-foreground ring-2 ring-offset-2 ring-offset-background"
                      : "border-transparent"
                  )}
                  style={{ backgroundColor: c.hex }}
                />
              )
            })}
          </div>
        </div>

        <Separator />

        {/* Toggles */}
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <Label htmlFor="reduced-motion" className="flex items-center gap-2">
                <Zap className="h-4 w-4" />
                Reduce motion
              </Label>
              <p className="text-xs text-muted-foreground">
                Minimize animations and transitions
              </p>
            </div>
            <Switch
              id="reduced-motion"
              checked={reducedMotion}
              onCheckedChange={setReducedMotion}
            />
          </div>

          <div className="flex items-center justify-between">
            <div>
              <Label htmlFor="compact-mode" className="flex items-center gap-2">
                <Sparkles className="h-4 w-4" />
                Compact mode
              </Label>
              <p className="text-xs text-muted-foreground">
                Reduce spacing and padding
              </p>
            </div>
            <Switch
              id="compact-mode"
              checked={compactMode}
              onCheckedChange={setCompactMode}
            />
          </div>
        </div>

        <Separator />

        {/* Reset */}
        <div className="flex justify-end">
          <Button variant="outline" size="sm" onClick={reset}>
            <RotateCcw className="mr-2 h-4 w-4" />
            Reset to defaults
          </Button>
        </div>
      </CardContent>
    </Card>
  )
}
