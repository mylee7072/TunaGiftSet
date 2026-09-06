import { useScrollReveal } from "../../hooks/useScrollReveal";

// Thin wrapper so call sites don't each wire up the ref/className boilerplate.
// `as` lets it render as whatever element fits the layout (section, div, ...).
export function Reveal({ as: Component = "div", className = "", children, ...rest }) {
  const { ref, visible } = useScrollReveal();
  return (
    <Component ref={ref} className={`reveal${visible ? " reveal--visible" : ""}${className ? ` ${className}` : ""}`} {...rest}>
      {children}
    </Component>
  );
}
