import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../context/useAuth";
import { Loading } from "../components/common/Loading";

// Redirects unauthenticated users to /login, remembering where they came from via
// router state (not a query string) — a bare route object, not a URL, so it can't
// be abused for an open redirect to an external site.
export function ProtectedRoute() {
  const { isAuthenticated, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return <Loading label="로그인 상태를 확인하고 있습니다..." />;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  return <Outlet />;
}
