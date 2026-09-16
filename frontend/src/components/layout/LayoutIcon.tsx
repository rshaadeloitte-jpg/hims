interface IconProps {
  name: string;
  size?: number;
}

/**
 * Minimal stroke-icon set (24x24, currentColor) so the sidebar/header
 * look professional without pulling in an icon dependency.
 */
export function LayoutIcon({ name, size = 18 }: IconProps) {
  const common = {
    width: size,
    height: size,
    viewBox: "0 0 24 24",
    fill: "none",
    stroke: "currentColor",
    strokeWidth: 1.8,
    strokeLinecap: "round",
    strokeLinejoin: "round",
  } as const;

  switch (name) {
    case "grid":
      return (
        <svg {...common} aria-hidden="true">
          <rect x="3" y="3" width="7" height="7" rx="1.5" />
          <rect x="14" y="3" width="7" height="7" rx="1.5" />
          <rect x="3" y="14" width="7" height="7" rx="1.5" />
          <rect x="14" y="14" width="7" height="7" rx="1.5" />
        </svg>
      );
    case "calendar":
      return (
        <svg {...common} aria-hidden="true">
          <rect x="3" y="4.5" width="18" height="16" rx="2" />
          <path d="M8 2.5v4M16 2.5v4M3 9.5h18" />
        </svg>
      );
    case "activity":
      return (
        <svg {...common} aria-hidden="true">
          <path d="M3 12h4l2.5-6 4 12 2.5-6H21" />
        </svg>
      );
    case "receipt":
      return (
        <svg {...common} aria-hidden="true">
          <path d="M6 2.5h12V22l-3-2-3 2-3-2-3 2V2.5Z" />
          <path d="M9 7.5h6M9 11.5h6" />
        </svg>
      );
    case "pill":
      return (
        <svg {...common} aria-hidden="true">
          <rect x="3.5" y="8.5" width="13" height="7" rx="3.5" transform="rotate(-45 10 12)" />
          <path d="M10.5 9.5l4 4" />
        </svg>
      );
    case "bed":
      return (
        <svg {...common} aria-hidden="true">
          <path d="M3 18v-8M3 14h18v4M3 11h6v3M21 14v-3a3 3 0 0 0-3-3h-7v6" />
          <circle cx="7.5" cy="8" r="1.6" />
        </svg>
      );
    case "monitor":
      return (
        <svg {...common} aria-hidden="true">
          <rect x="3" y="4" width="18" height="12" rx="2" />
          <path d="M9 20h6M12 16v4" />
        </svg>
      );
    case "settings":
      return (
        <svg {...common} aria-hidden="true">
          <circle cx="12" cy="12" r="3" />
          <path d="M19 12a7 7 0 0 0-.14-1.4l2-1.55-2-3.46-2.36.95A7 7 0 0 0 14 5.1L13.7 2.6h-3.4L10 5.1a7 7 0 0 0-2.5 1.44l-2.36-.95-2 3.46 2 1.55a7 7 0 0 0 0 2.8l-2 1.55 2 3.46 2.36-.95a7 7 0 0 0 2.5 1.44l.3 2.5h3.4l.3-2.5a7 7 0 0 0 2.5-1.44l2.36.95 2-3.46-2-1.55c.1-.46.14-.92.14-1.4Z" />
        </svg>
      );
    case "search":
      return (
        <svg {...common} aria-hidden="true">
          <circle cx="11" cy="11" r="7" />
          <path d="m20 20-3.8-3.8" />
        </svg>
      );
    case "bell":
      return (
        <svg {...common} aria-hidden="true">
          <path d="M6 9a6 6 0 1 1 12 0c0 5 2 6 2 6H4s2-1 2-6" />
          <path d="M10 20a2 2 0 0 0 4 0" />
        </svg>
      );
    case "menu":
      return (
        <svg {...common} aria-hidden="true">
          <path d="M4 6h16M4 12h16M4 18h16" />
        </svg>
      );
    case "chevron":
      return (
        <svg {...common} aria-hidden="true">
          <path d="m6 9 6 6 6-6" />
        </svg>
      );
    case "x":
      return (
        <svg {...common} aria-hidden="true">
          <path d="M6 6l12 12M18 6 6 18" />
        </svg>
      );
    case "logout":
      return (
        <svg {...common} aria-hidden="true">
          <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9" />
        </svg>
      );
    case "user":
      return (
        <svg {...common} aria-hidden="true">
          <circle cx="12" cy="8" r="4" />
          <path d="M4 21c0-4 3.6-6.5 8-6.5s8 2.5 8 6.5" />
        </svg>
      );
    default:
      return (
        <svg {...common} aria-hidden="true">
          <circle cx="12" cy="12" r="8" />
        </svg>
      );
  }
}
