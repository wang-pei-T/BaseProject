import { createBrowserRouter, Navigate } from "react-router-dom";
import App from "../App";
import LoginPage from "../pages/login";
import PlatformLoginPage from "../pages/login/PlatformLoginPage";
import SessionPage from "../pages/session";
import UserPage from "../pages/user";
import OrgPage from "../pages/org";
import RolePage from "../pages/role";
import MenuPage from "../pages/menu";
import DictPage from "../pages/dict";
import ConfigPage from "../pages/config";
import MessagePage from "../pages/message";
import PlatformAnnouncementPage from "../pages/platform-announcement";
import PlatformAssistPage from "../pages/platform-assist";
import PlatformAuditPage from "../pages/platform-audit";
import PlatformConfigPage from "../pages/platform-config";
import PlatformLogPage from "../pages/platform-log";
import TenantListPage from "../pages/tenant";
import PlatformAccountPage from "../pages/platform-account";
import PlatformRolePage from "../pages/platform-role";
import TenantAuditPage from "../pages/tenant-audit";
import TenantLogPage from "../pages/tenant-log";
import RuntimeLogPage from "../pages/runtime-log";
import ProfilePage from "../pages/profile";
import HomePage from "../pages/home";

const router = createBrowserRouter([
  {
    path: "/",
    element: <App />,
    children: [
      {
        index: true,
        element: <HomePage />,
      },
      {
        path: "users",
        element: <UserPage />,
      },
      {
        path: "orgs",
        element: <OrgPage />,
      },
      {
        path: "org-members",
        element: <Navigate to="/orgs" replace />,
      },
      {
        path: "roles",
        element: <RolePage />,
      },
      {
        path: "menus",
        element: <MenuPage />,
      },
      {
        path: "dicts",
        element: <DictPage />,
      },
      {
        path: "configs",
        element: <ConfigPage />,
      },
      {
        path: "sessions",
        element: <SessionPage />,
      },
      {
        path: "messages",
        element: <MessagePage />,
      },
      {
        path: "platform/announcements",
        element: <PlatformAnnouncementPage />,
      },
      {
        path: "platform/configs",
        element: <PlatformConfigPage />,
      },
      {
        path: "platform/assist",
        element: <PlatformAssistPage />,
      },
      {
        path: "platform/logs",
        element: <PlatformLogPage />,
      },
      {
        path: "platform/tenants",
        element: <TenantListPage />,
      },
      {
        path: "platform/accounts",
        element: <PlatformAccountPage />,
      },
      {
        path: "platform/roles",
        element: <PlatformRolePage />,
      },
      {
        path: "platform/audits",
        element: <PlatformAuditPage />,
      },
      {
        path: "audits",
        element: <TenantAuditPage />,
      },
      {
        path: "logs",
        element: <TenantLogPage />,
      },
      {
        path: "runtime-logs",
        element: <RuntimeLogPage />,
      },
      {
        path: "profile",
        element: <ProfilePage />,
      },
    ],
  },
  {
    path: "/login",
    element: <LoginPage />,
  },
  {
    path: "/platform/login",
    element: <PlatformLoginPage />,
  },
]);

export default router;

