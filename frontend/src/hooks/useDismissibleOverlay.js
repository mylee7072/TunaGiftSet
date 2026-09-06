import { useEffect, useRef, useState } from "react";

// Keep in sync with the --motion-fast-based exit animations in index.css
// (.modal--closing / .modal-overlay--closing / .nav-drawer--closing) — a small
// buffer over the CSS duration so we never unmount mid-frame.
const EXIT_DURATION_MS = 160;

// Shared open/close lifecycle for modals, sheets, and the mobile nav drawer:
//  - keeps the element mounted long enough to play its CSS exit animation instead
//    of vanishing instantly when the `open` prop flips to false
//  - closes on Escape
//  - locks background scroll while visible (a coupon list or a long drawer
//    shouldn't let the page behind it scroll along with it)
//  - returns focus to whatever had it before the overlay opened, once the exit
//    animation finishes — but only if focus is still "unclaimed" (on <body> or
//    gone), so it never yanks focus away from something the user has since
//    clicked into
const FOCUSABLE_SELECTOR =
  'a[href], button:not([disabled]), textarea:not([disabled]), input:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])';

// `containerRef` is optional — pass it to also trap Tab/Shift+Tab focus inside
// the overlay while it's open (WCAG 2.4.3). Without it, this still handles ESC,
// scroll-lock, and focus-return exactly as before.
export function useDismissibleOverlay(open, onClose, containerRef) {
  const [shouldRender, setShouldRender] = useState(open);
  const [closing, setClosing] = useState(false);
  const triggerRef = useRef(null);

  useEffect(() => {
    if (open) {
      triggerRef.current = document.activeElement;
      setShouldRender(true);
      setClosing(false);
      return undefined;
    }

    setShouldRender((wasRendered) => {
      if (!wasRendered) return false;
      setClosing(true);
      return wasRendered;
    });

    const timer = setTimeout(() => {
      setShouldRender(false);
      setClosing(false);
      const trigger = triggerRef.current;
      if (trigger && document.body.contains(trigger)) {
        const active = document.activeElement;
        if (!active || active === document.body) {
          trigger.focus();
        }
      }
    }, EXIT_DURATION_MS);

    return () => clearTimeout(timer);
  }, [open]);

  useEffect(() => {
    if (!shouldRender || !onClose) return undefined;
    function handleKeyDown(event) {
      if (event.key === "Escape") onClose();
    }
    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [shouldRender, onClose]);

  useEffect(() => {
    if (!shouldRender) return undefined;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, [shouldRender]);

  useEffect(() => {
    if (!shouldRender || !containerRef) return undefined;

    function handleTab(event) {
      if (event.key !== "Tab") return;
      const container = containerRef.current;
      if (!container) return;
      const focusable = Array.from(container.querySelectorAll(FOCUSABLE_SELECTOR));
      if (focusable.length === 0) return;

      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    }

    document.addEventListener("keydown", handleTab);
    return () => document.removeEventListener("keydown", handleTab);
  }, [shouldRender, containerRef]);

  return { shouldRender, closing };
}
