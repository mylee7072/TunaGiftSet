// JS mirror of the --motion-*/--ease-* custom properties in index.css (durations
// there are ms, motion's `transition.duration` wants seconds — converted below).
// Keep these two files in sync by hand; there's no build step that shares them.
export const MOTION_FAST = 0.13;
export const MOTION_NORMAL = 0.22;
export const MOTION_SLOW = 0.32;

export const EASE_STANDARD = [0.4, 0, 0.2, 1];
export const EASE_ENTER = [0, 0, 0.2, 1];
export const EASE_EXIT = [0.4, 0, 1, 1];

// Page-grid stagger: cards fade+lift in on entry, ~50ms apart. Container has no
// visual state of its own — it only orchestrates staggerChildren for the items.
export const staggerContainer = {
  hidden: {},
  show: { transition: { staggerChildren: 0.05 } },
};

export const staggerItem = {
  hidden: { opacity: 0, y: 12 },
  show: { opacity: 1, y: 0, transition: { duration: MOTION_NORMAL, ease: EASE_ENTER } },
};
