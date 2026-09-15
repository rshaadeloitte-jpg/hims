import { VEHICLE_STATUS, MAINTENANCE_STATUS } from "./zodSchema";

/**
 * M21 Transport — Vercel-style status indicator (pill with colored dot).
 * Maps entity status strings to a dot + label, using design tokens.
 */

type StatusVariant = "info" | "success" | "warning" | "error" | "muted";

function statusVariant(status: string): StatusVariant {
  switch (status) {
    // vehicle statuses
    case VEHICLE_STATUS.IN:
      return "success";
    case VEHICLE_STATUS.OUT:
      return "info";
    case VEHICLE_STATUS.ALLOCATED:
      return "warning";
    case VEHICLE_STATUS.MAINTENANCE:
      return "error";
    // maintenance statuses
    case MAINTENANCE_STATUS.APPROVED:
      return "success";
    case MAINTENANCE_STATUS.IN_PROGRESS:
      return "info";
    case MAINTENANCE_STATUS.COMPLETED:
      return "muted";
    case MAINTENANCE_STATUS.REJECTED:
      return "error";
    case MAINTENANCE_STATUS.PENDING:
    default:
      return "warning";
  }
}

const variantClass: Record<StatusVariant, string> = {
  info: "tn-status--info",
  success: "tn-status--success",
  warning: "tn-status--warning",
  error: "tn-status--error",
  muted: "tn-status--muted",
};

interface StatusIndicatorProps {
  status: string;
  className?: string;
}

export function StatusIndicator({ status, className }: StatusIndicatorProps) {
  const variant = statusVariant(status);
  return (
    <span className={`tn-status ${variantClass[variant]}${className ? ` ${className}` : ""}`}>
      <span className="tn-status__dot" aria-hidden="true" />
      {status}
    </span>
  );
}
