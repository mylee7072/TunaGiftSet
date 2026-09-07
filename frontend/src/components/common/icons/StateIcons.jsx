// Simple line-art illustrations for EmptyState/ErrorState — inline SVG, no icon
// library. currentColor throughout so they inherit .state-block's (themed) color.
const sharedProps = {
  width: 56,
  height: 56,
  viewBox: "0 0 24 24",
  fill: "none",
  stroke: "currentColor",
  strokeWidth: 1.5,
  strokeLinecap: "round",
  strokeLinejoin: "round",
  "aria-hidden": "true",
};

export function EmptyBoxIcon(props) {
  return (
    <svg {...sharedProps} {...props}>
      <path d="M3 8.5 12 4l9 4.5-9 4.5-9-4.5Z" />
      <path d="M3 8.5V16l9 4.5 9-4.5V8.5" />
      <path d="M12 13v7.5" />
    </svg>
  );
}

export function EmptyCartIcon(props) {
  return (
    <svg {...sharedProps} {...props}>
      <circle cx="9.5" cy="20" r="1.25" fill="currentColor" stroke="none" />
      <circle cx="18" cy="20" r="1.25" fill="currentColor" stroke="none" />
      <path d="M2.5 3h2.4l1.2 3M6.1 6l1.9 8.4a2 2 0 0 0 2 1.6h6.7a2 2 0 0 0 1.95-1.57L20 6H6.1Z" />
    </svg>
  );
}

export function EmptySearchIcon(props) {
  return (
    <svg {...sharedProps} {...props}>
      <circle cx="10.5" cy="10.5" r="6.5" />
      <path d="m20 20-4.3-4.3" />
      <path d="M8 10.5h5" />
    </svg>
  );
}

export function EmptyWishlistIcon(props) {
  return (
    <svg {...sharedProps} {...props}>
      <path d="M12 20.2s-7.5-4.6-9.6-9.3C.8 7.2 2.9 4 6.3 4c1.9 0 3.5 1 4.7 2.6l1 1.3 1-1.3C14.2 5 15.8 4 17.7 4c3.4 0 5.5 3.2 3.9 6.9-2.1 4.7-9.6 9.3-9.6 9.3Z" />
    </svg>
  );
}

export function ErrorIcon(props) {
  return (
    <svg {...sharedProps} {...props}>
      <path d="M12 3 2 20h20L12 3Z" />
      <path d="M12 10v4" />
      <circle cx="12" cy="17" r="0.9" fill="currentColor" stroke="none" />
    </svg>
  );
}
