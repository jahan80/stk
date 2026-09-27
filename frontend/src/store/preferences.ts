import { create } from "zustand"
import { persist } from "zustand/middleware"

export type Theme = "light" | "dark" | "system"
export type FontSize = "sm" | "md" | "lg" | "xl"
export type AccentColor = "blue" | "violet" | "green" | "rose" | "orange" | "slate"

interface PreferencesState {
  theme: Theme
  fontSize: FontSize
  accentColor: AccentColor
  reducedMotion: boolean
  compactMode: boolean

  setTheme: (theme: Theme) => void
  setFontSize: (size: FontSize) => void
  setAccentColor: (color: AccentColor) => void
  setReducedMotion: (value: boolean) => void
  setCompactMode: (value: boolean) => void
  reset: () => void
}

const DEFAULTS = {
  theme: "system" as Theme,
  fontSize: "md" as FontSize,
  accentColor: "blue" as AccentColor,
  reducedMotion: false,
  compactMode: false,
}

export const usePreferencesStore = create<PreferencesState>()(
  persist(
    (set) => ({
      ...DEFAULTS,

      setTheme: (theme) => set({ theme }),
      setFontSize: (fontSize) => set({ fontSize }),
      setAccentColor: (accentColor) => set({ accentColor }),
      setReducedMotion: (reducedMotion) => set({ reducedMotion }),
      setCompactMode: (compactMode) => set({ compactMode }),

      reset: () => set(DEFAULTS),
    }),
    {
      name: "starterkit-preferences",
    }
  )
)
