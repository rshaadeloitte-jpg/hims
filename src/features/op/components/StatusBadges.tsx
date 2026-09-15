import type { AppointmentStatus, AbhaStatus } from "../api/types";
import type { QueuePriority, QueueToken } from "../api/types";

/** Appointment status badge — 09 §2.1 (BR-APT-001 forward-only machine). */
const APPT_LABELS: Record<AppointmentStatus, string> = {
  1: "Booked",
  2: "Checked-In",
  3: "Completed",
  4: "Cancelled",
  5: "Rescheduled",
  6: "No-Show",
};

const APPT_TONES: Record<AppointmentStatus, string> = {
  1: "badge--info",
  2: "badge--info",
  3: "badge--success",
  4: "badge--muted",
  5: "badge--muted",
  6: "badge--danger",
};

export function AppointmentStatusBadge({ status }: { status: AppointmentStatus }) {
  return (
    <span className={`badge ${APPT_TONES[status]}`} title={`BR-APT-001 status ${status}`}>
      {APPT_LABELS[status]}
    </span>
  );
}

/** ABHA status badge. */
const ABHA_LABELS: Record<AbhaStatus, string> = {
  PENDING_OTP: "Pending OTP",
  OTP_VERIFIED: "OTP Verified",
  LINKED: "ABHA Linked",
  FAILED: "Failed",
};
const ABHA_TONES: Record<AbhaStatus, string> = {
  PENDING_OTP: "badge--warning",
  OTP_VERIFIED: "badge--info",
  LINKED: "badge--success",
  FAILED: "badge--danger",
};

export function AbhaStatusBadge({ status }: { status: AbhaStatus }) {
  return <span className={`badge ${ABHA_TONES[status]}`}>{ABHA_LABELS[status]}</span>;
}

/** Queue token priority + status badge. */
const QUEUE_PRIORITY_TONES: Record<QueuePriority, string> = {
  NORMAL: "badge--muted",
  EMERGENCY: "badge--danger",
  VIP: "badge--warning",
};

const QUEUE_STATUS_TONES: Record<QueueToken["status"], string> = {
  WAITING: "badge--muted",
  IN_PROGRESS: "badge--info",
  COMPLETED: "badge--success",
  CANCELLED: "badge--muted",
  NO_SHOW: "badge--warning",
  TRANSFERRED: "badge--muted",
};

export function QueuePriorityBadge({ priority }: { priority: QueuePriority }) {
  return <span className={`badge ${QUEUE_PRIORITY_TONES[priority]}`}>{priority}</span>;
}

export function QueueStatusBadge({ status }: { status: QueueToken["status"] }) {
  return <span className={`badge ${QUEUE_STATUS_TONES[status]}`}>{status.replace("_", " ")}</span>;
}

/** Generic "yes/no" pill used in the appointment category strip. */
export function YesNoBadge({ value, yes = "Yes", no = "No" }: { value: boolean; yes?: string; no?: string }) {
  return <span className={`badge ${value ? "badge--success" : "badge--muted"}`}>{value ? yes : no}</span>;
}
