import { Link, Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../context/useAuth";
import { Loading } from "../components/common/Loading";

// Same "remember where they came from" pattern as ProtectedRoute, plus a role check on top —
// an authenticated non-admin never sees admin data, just a plain 403-style message (not a
// redirect loop back to login, since re-authenticating as the same account won't help).
export function AdminRoute() {
  const { isAuthenticated, isLoading, member } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return <Loading label="로그인 상태를 확인하고 있습니다..." />;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  if (member?.role !== "ADMIN") {
    return (
      <div className="container section">
        <h1 className="page-title">접근 권한이 없습니다</h1>
        <p className="muted">관리자만 접근할 수 있는 페이지입니다.</p>
        <Link to="/" className="btn btn--primary">
          홈으로 이동
        </Link>
      </div>
    );
  }

  return <Outlet />;
}
