import { Suspense } from "react";
import { AnimatePresence, LazyMotion, domAnimation, m, useReducedMotion } from "motion/react";
import { Outlet, useLocation } from "react-router-dom";
import { Header } from "./Header";
import { Footer } from "./Footer";
import { Loading } from "../common/Loading";
import { useScrollRestoration } from "../../hooks/useScrollRestoration";
import { EASE_ENTER, EASE_EXIT, MOTION_SLOW } from "../../motion/tokens";

export function RootLayout() {
  const location = useLocation();
  const shouldReduceMotion = useReducedMotion();
  useScrollRestoration();

  return (
    <div className="app-shell">
      <a href="#main-content" className="skip-link">
        본문 바로가기
      </a>
      <Header />
      <main id="main-content" className="app-main">
        {/* LazyMotion + the lowercase `m` component (instead of `motion.*`) only
         * bundles the features named below — fades/exit/tap/inView — rather than
         * motion's full drag+layout-projection engine. `strict` throws if a page
         * ever reaches for `motion.*` directly, so that heavier bundle can't creep
         * back in unnoticed. Pages that need drag (ProductDetailPage) load domMax
         * locally, scoped to their own already-lazy route chunk. */}
        <LazyMotion features={domAnimation} strict>
          {/* mode="wait" holds the next page out until the current one has finished
           * leaving — no overlap to manage against the fixed header/footer around it.
           * Keyed by pathname only (not search) so an in-place filter/sort/page
           * change on the same route (e.g. ProductListPage) updates smoothly instead
           * of re-triggering the transition — see useScrollRestoration for the same
           * reasoning. */}
          <AnimatePresence mode="wait" initial={false}>
            <m.div
              key={location.pathname}
              initial={shouldReduceMotion ? false : { opacity: 0, y: 6 }}
              animate={{
                opacity: 1,
                y: 0,
                transition: { duration: shouldReduceMotion ? 0 : MOTION_SLOW, ease: EASE_ENTER },
              }}
              exit={
                shouldReduceMotion
                  ? undefined
                  : { opacity: 0, y: -6, transition: { duration: MOTION_SLOW, ease: EASE_EXIT } }
              }
            >
              <Suspense fallback={<Loading />}>
                <Outlet />
              </Suspense>
            </m.div>
          </AnimatePresence>
        </LazyMotion>
      </main>
      <Footer />
    </div>
  );
}
