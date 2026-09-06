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
export function useDismissibleOverlay(open, onClose) {
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

  return { shouldRender, closing };
}
