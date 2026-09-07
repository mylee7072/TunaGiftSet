import { m, useReducedMotion } from "motion/react";
import { staggerContainer, staggerItem } from "../../motion/tokens";

// Cards fade+lift in one after another, once, the first time the grid scrolls
// into view (whileInView + viewport once, not a mount-time animate) — a plain
// mount trigger would finish invisibly for grids that start below the fold
// (e.g. HomePage's Reveal-wrapped sections), since the cards would already be
// fully faded in by the time the section itself becomes visible. Deliberately
// separate from useScrollReveal/<Reveal>, which still owns the section-level
// fade+lift around this grid — this only staggers the items inside it.
export function StaggerGrid({ items, renderItem, keyFor, className = "product-grid" }) {
  const shouldReduceMotion = useReducedMotion();

  // Plain markup, no motion props at all — rather than lean on initial={false}
  // here, which is well-defined paired with `animate` but ambiguous paired only
  // with `whileInView` (whether it still gates on viewport entry or renders the
  // "show" variant unconditionally isn't something to gamble reduced-motion
  // correctness on).
  if (shouldReduceMotion) {
    return (
      <div className={className}>
        {items.map((item) => (
          <div key={keyFor(item)}>{renderItem(item)}</div>
        ))}
      </div>
    );
  }

  return (
    <m.div
      className={className}
      variants={staggerContainer}
      initial="hidden"
      whileInView="show"
      viewport={{ once: true, amount: 0.2 }}
    >
      {items.map((item) => (
        <m.div key={keyFor(item)} variants={staggerItem}>
          {renderItem(item)}
        </m.div>
      ))}
    </m.div>
  );
}
