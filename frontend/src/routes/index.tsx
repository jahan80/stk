import { createBrowserRouter, Navigate } from "react-router-dom"
import { AppLayout } from "@/components/layout/AppLayout"
import { ProtectedRoute } from "./ProtectedRoute"
import { AdminRoute } from "./AdminRoute"

// Auth pages
import { LoginPage } from "@/features/auth/pages/LoginPage"
import { RegisterPage } from "@/features/auth/pages/RegisterPage"
import { ForgotPasswordPage } from "@/features/auth/pages/ForgotPasswordPage"
import { ResetPasswordPage } from "@/features/auth/pages/ResetPasswordPage"
import { VerifyEmailPage } from "@/features/auth/pages/VerifyEmailPage"
import { VerifyMobilePage } from "@/features/auth/pages/VerifyMobilePage"

// App pages
import { DashboardPage } from "@/features/dashboard/pages/DashboardPage"
import { ProfilePage } from "@/features/profile/pages/ProfilePage"
import { PublicProfilePage } from "@/features/profile/pages/PublicProfilePage"

// Admin pages
import { UserManagementPage } from "@/features/admin/pages/UserManagementPage"
import { RolesPage } from "@/features/admin/pages/RolesPage"
import { SystemSettingsPage } from "@/features/admin/pages/SystemSettingsPage"
import { AuditLogPage } from "@/features/audit/pages/AuditLogPage"
import { RateLimitsPage } from "@/features/rate-limits/pages/RateLimitsPage"
import { NotificationsPage } from "@/features/notif/pages/NotificationsPage"
import { TicketListPage } from "@/features/tickets/pages/TicketListPage"
import { TicketDetailPage } from "@/features/tickets/pages/TicketDetailPage"
import { CreateTicketPage } from "@/features/tickets/pages/CreateTicketPage"
import { TicketSettingsPage } from "@/features/tickets/pages/TicketSettingsPage"

export const router = createBrowserRouter([
  // Public auth routes
  { path: "/login", element: <LoginPage /> },
  { path: "/register", element: <RegisterPage /> },
  { path: "/forgot-password", element: <ForgotPasswordPage /> },
  { path: "/reset-password", element: <ResetPasswordPage /> },
  { path: "/verify-email", element: <VerifyEmailPage /> },
  { path: "/verify-mobile", element: <VerifyMobilePage /> },

  // Public profile (no auth)
  { path: "/u/:username", element: <PublicProfilePage /> },

  // Protected app routes
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <AppLayout />,
        children: [
          { path: "/", element: <Navigate to="/dashboard" replace /> },
          { path: "/dashboard", element: <DashboardPage /> },
          { path: "/profile", element: <ProfilePage /> },
          { path: "/notifications", element: <NotificationsPage /> },

          // Ticket routes
          { path: "/tickets", element: <TicketListPage /> },
          { path: "/tickets/new", element: <CreateTicketPage /> },
          { path: "/tickets/:id", element: <TicketDetailPage /> },
          { path: "/tickets/settings", element: <TicketSettingsPage /> },

          // Admin routes
          {
            element: <AdminRoute />,
            children: [
              { path: "/admin/users", element: <UserManagementPage /> },
              { path: "/admin/roles", element: <RolesPage /> },
              { path: "/admin/settings", element: <SystemSettingsPage /> },
              { path: "/admin/audit", element: <AuditLogPage /> },
              { path: "/admin/rate-limits", element: <RateLimitsPage /> },
            ],
          },
        ],
      },
    ],
  },

  // Fallback
  { path: "*", element: <Navigate to="/dashboard" replace /> },
])
