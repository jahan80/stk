import { Menu, Moon, Sun, Monitor } from "lucide-react"
import { Button } from "@/components/ui/button"
import { usePreferencesStore } from "@/store/preferences"

interface HeaderProps {
  onMenuClick: () => void
  title?: string
}

export function Header({ onMenuClick, title }: HeaderProps) {
  const { theme, setTheme } = usePreferencesStore()

  const nextTheme = () => {
    const order = ["light", "dark", "system"] as const
    const idx = order.indexOf(theme)
    setTheme(order[(idx + 1) % order.length])
  }

  const ThemeIcon = theme === "light" ? Sun : theme === "dark" ? Moon : Monitor

  return (
    <header className="flex h-16 items-center gap-4 border-b bg-card px-6">
      <Button
        variant="ghost"
        size="icon"
        className="md:hidden"
        onClick={onMenuClick}
      >
        <Menu className="h-5 w-5" />
      </Button>

      <h2 className="text-lg font-semibold">{title ?? "Dashboard"}</h2>

      <div className="ml-auto">
        <Button
          variant="ghost"
          size="icon"
          onClick={nextTheme}
          title={`Theme: ${theme}`}
        >
          <ThemeIcon className="h-5 w-5" />
        </Button>
      </div>
    </header>
  )
}
