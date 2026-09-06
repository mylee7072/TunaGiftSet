import { Outlet, useLocation } from "react-router-dom";
import { Header } from "./Header";
import { Footer } from "./Footer";
import { useScrollRestoration } from "../../hooks/useScrollRestoration";

export function RootLayout() {
  const location = useLocation();
  useScrollRestoration();

  return (
    <div className="app-shell">
      <Header />
      <main className="app-main">
        {/* Keyed by pathname only (not search) so an in-place filter/sort/page
         * change on the same route (e.g. ProductListPage) updates smoothly instead
         * of re-triggering the fade — see useScrollRestoration for the same reasoning. */}
        <div key={location.pathname} className="route-transition">
          <Outlet />
        </div>
      </main>
      <Footer />
    </div>
  );
}
