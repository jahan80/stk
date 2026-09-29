import { useState } from "react"
import { Outlet, useLocation } from "react-router-dom"
import { Sidebar } from "./Sidebar"
import { Header } from "./Header"
import { cn } from "@/lib/utils"

const TITLES: Record<string, string> = {
  "/dashboard": "Dashboard",
  "/profile": "Profile",
  "/admin/users": "User Management",
  "/admin/roles": "Roles & Permissions",
  "/admin/settings": "System Settings",
  "/admin/audit": "Audit Logs",
  "/admin/rate-limits": "Rate Limits",
  "/notifications": "Notifications",
  "/tickets": "Tickets",
  "/tickets/new": "New Ticket",
  "/admin/ticket-groups": "Ticket Groups",
}

export function AppLayout() {
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const location = useLocation()
  const title = TITLES[location.pathname] ?? "Dashboard"

  return (
    <div className="flex h-screen overflow-hidden bg-background">
      {/* Desktop sidebar */}
      <div className="hidden md:block">
        <Sidebar />
      </div>

      {/* Mobile sidebar */}
      <div
        className={cn(
          "fixed inset-0 z-40 md:hidden",
          sidebarOpen ? "block" : "hidden"
        )}
      >
        <div
          className="absolute inset-0 bg-black/50"
          onClick={() => setSidebarOpen(false)}
        />
        <div className="relative h-full w-64">
          <Sidebar onNavigate={() => setSidebarOpen(false)} />
        </div>
      </div>

      {/* Main content */}
      <div className="flex flex-1 flex-col overflow-hidden">
        <Header onMenuClick={() => setSidebarOpen(true)} title={title} />
        <main className="flex-1 overflow-y-auto p-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
