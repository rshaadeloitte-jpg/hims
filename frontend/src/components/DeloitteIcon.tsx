import React from "react";

export function DeloitteLogo({ size = 24, className = "" }: { size?: number; className?: string }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 32 32"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={className}
      aria-label="Deloitte Brand Icon"
    >
      <rect width="32" height="32" rx="6" fill="#000000" />
      <text
        x="6"
        y="23"
        fontFamily="-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif"
        fontWeight="900"
        fontSize="20"
        fill="#FFFFFF"
      >
        D
      </text>
      <circle cx="23" cy="22" r="3.2" fill="#86BC25" />
    </svg>
  );
}

export function DeloitteIconBadge({ label }: { label: string }) {
  return (
    <span
      style={{
        display: "inline-flex",
        alignItems: "center",
        gap: "4px",
        background: "#000000",
        color: "#FFFFFF",
        padding: "2px 8px",
        borderRadius: "4px",
        fontSize: "11px",
        fontWeight: 600,
        letterSpacing: "0.02em",
      }}
    >
      <span
        style={{
          width: "6px",
          height: "6px",
          borderRadius: "50%",
          backgroundColor: "#86BC25",
          display: "inline-block",
        }}
      />
      {label}
    </span>
  );
}
