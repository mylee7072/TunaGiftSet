import { useEffect, useRef, useState } from "react";

// Reveals an element once, the first time it enters the viewport — not every time
// it's scrolled past, which would just be distracting on the way back up. Falls
// back to "already visible" immediately when IntersectionObserver isn't available
// or the element is already on-screen at mount, so content is never stuck hidden.
export function useScrollReveal(options) {
  const ref = useRef(null);
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    const node = ref.current;
    if (!node) return undefined;

    if (typeof IntersectionObserver === "undefined") {
      setVisible(true);
      return undefined;
    }

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setVisible(true);
          observer.disconnect();
        }
      },
      { threshold: 0.15, rootMargin: "0px 0px -40px 0px", ...options }
    );
    observer.observe(node);
    return () => observer.disconnect();
  }, [options]);

  return { ref, visible };
}
