import { useEffect, useLayoutEffect } from "react";
import { useLocation, useNavigationType } from "react-router-dom";

// Module-level (not component state) on purpose: it needs to survive the very
// unmount/remount this hook is reacting to, keyed by React Router's own
// per-history-entry location.key.
const scrollPositions = new Map();

// Plain <BrowserRouter>/<Routes> (what this app uses) has no built-in scroll
// restoration — that's a data-router-only feature. This is the manual
// equivalent: forward navigation always starts at the top; back/forward
// navigation (POP) restores where the user left off, best-effort.
export function useScrollRestoration() {
  const location = useLocation();
  const navigationType = useNavigationType();

  // Continuously record the CURRENT entry's scroll position so it's available
  // if/when the user comes back to it via the browser's back button.
  useEffect(() => {
    function handleScroll() {
      scrollPositions.set(location.key, window.scrollY);
    }
    window.addEventListener("scroll", handleScroll, { passive: true });
    return () => window.removeEventListener("scroll", handleScroll);
  }, [location.key]);

  useLayoutEffect(() => {
    if (navigationType === "POP" && scrollPositions.has(location.key)) {
      const y = scrollPositions.get(location.key);
      // Two rAFs: give the freshly-mounted page (including any skeleton/loading
      // state) one paint to settle into roughly its final height before jumping —
      // scrolling immediately would clamp to a still-short, not-yet-loaded page.
      requestAnimationFrame(() => requestAnimationFrame(() => window.scrollTo(0, y)));
    } else {
      // Covers both a genuinely new page and an in-place filter/sort/page change
      // on the same route (e.g. ProductListPage) — landing at the top for either
      // is the expected result of a forward navigation, not just a "new page" rule.
      window.scrollTo(0, 0);
    }
  }, [location.key, navigationType]);
}
