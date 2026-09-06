import { useCallback, useMemo, useRef, useState } from "react";
import { ToastContext } from "./toastContextValue";

let nextId = 1;
const DISPLAY_DURATION_MS = 2600;
// Must stay in sync with the .toast--leaving exit animation duration in index.css
// (var(--motion-fast), plus a small buffer) — the toast stays mounted this long so
// the fade-out actually gets to play instead of the DOM node just vanishing.
const EXIT_DURATION_MS = 160;

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const timers = useRef(new Map());

  const hardRemove = useCallback((id) => {
    setToasts((current) => current.filter((toast) => toast.id !== id));
  }, []);

  const startLeaving = useCallback(
    (id) => {
      setToasts((current) => current.map((toast) => (toast.id === id ? { ...toast, leaving: true } : toast)));
      clearTimeout(timers.current.get(id));
      timers.current.set(id, setTimeout(() => hardRemove(id), EXIT_DURATION_MS));
    },
    [hardRemove]
  );

  const showToast = useCallback(
    (message, tone = "info") => {
      setToasts((current) => {
        // Repeated identical clicks (a user mashing "장바구니 담기") shouldn't pile up
        // a stack of the same message — refresh the existing one's timer instead.
        const duplicate = current.find((toast) => !toast.leaving && toast.message === message && toast.tone === tone);
        if (duplicate) {
          clearTimeout(timers.current.get(duplicate.id));
          timers.current.set(duplicate.id, setTimeout(() => startLeaving(duplicate.id), DISPLAY_DURATION_MS));
          return current;
        }

        const id = nextId++;
        timers.current.set(id, setTimeout(() => startLeaving(id), DISPLAY_DURATION_MS));
        return [...current, { id, message, tone, leaving: false }];
      });
    },
    [startLeaving]
  );

  const value = useMemo(() => ({ showToast }), [showToast]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="toast-stack" role="status" aria-live="polite">
        {toasts.map((toast) => (
          <div key={toast.id} className={`toast toast--${toast.tone}${toast.leaving ? " toast--leaving" : ""}`}>
            {toast.message}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}
