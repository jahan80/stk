import { useEffect } from "react"
import { usePreferencesStore } from "@/store/preferences"

export function useApplyPreferences() {
  const { theme, fontSize, accentColor, reducedMotion, compactMode } =
    usePreferencesStore()

  // Theme
  useEffect(() => {
    const root = document.documentElement

    const applyTheme = () => {
      root.classList.remove("light", "dark")

      if (theme === "system") {
        const isDark = window.matchMedia("(prefers-color-scheme: dark)").matches
        root.classList.add(isDark ? "dark" : "light")
      } else {
        root.classList.add(theme)
      }
    }

    applyTheme()

    if (theme === "system") {
      const media = window.matchMedia("(prefers-color-scheme: dark)")
      const listener = () => applyTheme()
      media.addEventListener("change", listener)
      return () => media.removeEventListener("change", listener)
    }
  }, [theme])

  // Font size
  useEffect(() => {
    const sizeMap = {
      sm: "14px",
      md: "16px",
      lg: "18px",
      xl: "20px",
    }
    document.documentElement.style.fontSize = sizeMap[fontSize]
  }, [fontSize])

  // Accent color
  useEffect(() => {
    const root = document.documentElement
    const accents = {
      blue: { primary: "221.2 83.2% 53.3%", ring: "221.2 83.2% 53.3%" },
      violet: { primary: "262.1 83.3% 57.8%", ring: "262.1 83.3% 57.8%" },
      green: { primary: "142.1 76.2% 36.3%", ring: "142.1 76.2% 36.3%" },
      rose: { primary: "346.8 77.2% 49.8%", ring: "346.8 77.2% 49.8%" },
      orange: { primary: "24.6 95% 53.1%", ring: "24.6 95% 53.1%" },
      slate: { primary: "215.4 16.3% 46.9%", ring: "215.4 16.3% 46.9%" },
    }
    const { primary, ring } = accents[accentColor]
    root.style.setProperty("--primary", primary)
    root.style.setProperty("--ring", ring)
  }, [accentColor])

  // Reduced motion
  useEffect(() => {
    document.documentElement.classList.toggle("reduce-motion", reducedMotion)
  }, [reducedMotion])

  // Compact mode
  useEffect(() => {
    document.documentElement.classList.toggle("compact", compactMode)
  }, [compactMode])
}
