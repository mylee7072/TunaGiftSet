import { Suspense } from "react";
import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../../context/useAuth";
import { useScrollRestoration } from "../../hooks/useScrollRestoration";
import { Loading } from "../common/Loading";

const NAV_ITEMS = [
  { to: "/admin", label: "대시보드", end: true },
  { to: "/admin/products", label: "상품 관리" },
  { to: "/admin/categories", label: "카테고리 관리" },
  { to: "/admin/brands", label: "브랜드 관리" },
  { to: "/admin/orders", label: "주문 관리" },
  { to: "/admin/coupons", label: "쿠폰 관리" },
];

export function AdminLayout() {
  const { member } = useAuth();
  useScrollRestoration();

  return (
    <div className="admin-shell">
      <aside className="admin-sidebar">
        <div className="admin-sidebar__brand">SeyoungGiftSet Admin</div>
        <nav className="admin-sidebar__nav">
          {NAV_ITEMS.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) => `admin-sidebar__link${isActive ? " is-active" : ""}`}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
        <NavLink to="/" className="admin-sidebar__exit">
          쇼핑몰로 이동
        </NavLink>
      </aside>
      <div className="admin-main">
        <header className="admin-topbar">
          <span>{member?.name} 관리자님</span>
        </header>
        <main className="admin-content">
          <Suspense fallback={<Loading />}>
            <Outlet />
          </Suspense>
        </main>
      </div>
    </div>
  );
}
