import { useState, useMemo } from "react";
import {
  useGrievances,
  useTicketCounts,
  useSeverities,
  type GrievanceTicket,
} from "./queries";
import { ISSUE_STATUS, CLOSE_STATUS, type IssueStatusKey } from "./zodSchema";

/** Map severity_id to a display label + color class. */
function severityStyle(severityId: number): { label: string; cls: string } {
  // ids 1..4 from master; fall back generically
  const map: Record<number, { label: string; cls: string }> = {
    1: { label: "LOW", cls: "sev--low" },
    2: { label: "MEDIUM", cls: "sev--medium" },
    3: { label: "HIGH", cls: "sev--high" },
    4: { label: "CRITICAL", cls: "sev--critical" },
  };
  return map[severityId] ?? { label: `S${severityId}`, cls: "sev--low" };
}

/** Status column descriptor — Linear kanban lane. */
interface Lane {
  key: IssueStatusKey;
  label: string;
  cls: string;
}

const LANES: Lane[] = [
  { key: 1, label: "New", cls: "lane--new" },
  { key: 2, label: "Assigned", cls: "lane--assigned" },
  { key: 3, label: "Resolved", cls: "lane--resolved" },
  { key: 4, label: "Assign to Creator", cls: "lane--reassigned" },
];

interface GrievanceBoardProps {
  /** Filter scope — omit for full hospital view. */
  unitId?: number;
  mobileNo?: number;
  onSelectTicket?: (ticket: GrievanceTicket) => void;
}

function StatusBadge({ status }: { status: number }) {
  const label = ISSUE_STATUS[status as IssueStatusKey] ?? `S${status}`;
  return <span className={`grv-status grv-status--${label.toLowerCase().replace("_", "-")}`}>{label}</span>;
}

function CloseBadge({ status }: { status?: number }) {
  if (!status) return null;
  const label = CLOSE_STATUS[status as keyof typeof CLOSE_STATUS] ?? "";
  if (!label) return null;
  return <span className={`grv-close grv-close--${label.toLowerCase()}`}>{label}</span>;
}

function TicketCard({ ticket, onSelect }: { ticket: GrievanceTicket; onSelect?: () => void }) {
  const sev = severityStyle(ticket.severityId);
  return (
    <article
      className={`grv-card grv-card--sev-${sev.cls.replace("sev--", "")}`}
      onClick={onSelect}
      role={onSelect ? "button" : undefined}
      tabIndex={onSelect ? 0 : undefined}
      onKeyDown={onSelect ? (e) => e.key === "Enter" && onSelect() : undefined}
    >
      <div className="grv-card__header">
        <span className="grv-card__ticket-no">{ticket.ticketNo}</span>
        <span className={`grv-severity ${sev.cls}`}>{sev.label}</span>
      </div>
      <p className="grv-card__issue" title={ticket.issue}>
        {ticket.issue.length > 80 ? `${ticket.issue.slice(0, 80)}…` : ticket.issue}
      </p>
      <div className="grv-card__footer">
        <span className="grv-card__citizen">{ticket.name}</span>
        <span className="grv-card__date">
          {new Date(ticket.issueDate).toLocaleDateString([], { month: "short", day: "numeric" })}
        </span>
      </div>
      <div className="grv-card__badges">
        <StatusBadge status={ticket.issueStatus} />
        <CloseBadge status={ticket.ticketCloseStatus} />
      </div>
    </article>
  );
}

function CountPill({ label, value, cls }: { label: string; value: number; cls?: string }) {
  return (
    <div className={`grv-count-pill ${cls ?? ""}`}>
      <span className="grv-count-pill__value">{value}</span>
      <span className="grv-count-pill__label">{label}</span>
    </div>
  );
}

export function GrievanceBoard({ unitId, mobileNo, onSelectTicket }: GrievanceBoardProps) {
  const [severityFilter, setSeverityFilter] = useState<number | undefined>(undefined);

  const { data: severities = [] } = useSeverities();
  const { data: counts } = useTicketCounts();
  const { data: tickets = [], isLoading, isError, refetch } = useGrievances(unitId || mobileNo ? { unitId, mobileNo } : undefined);

  const filtered = useMemo(
    () => (severityFilter !== undefined ? tickets.filter((t) => t.severityId === severityFilter) : tickets),
    [tickets, severityFilter],
  );

  const lanes = useMemo(
    () =>
      LANES.map((lane) => ({
        ...lane,
        tickets: filtered.filter((t) => t.issueStatus === lane.key),
      })),
    [filtered],
  );

  if (isLoading) {
    return (
      <div className="loading-state">
        <div className="spinner" />
        Loading grievance board…
      </div>
    );
  }

  if (isError) {
    return (
      <div className="alert alert--error">
        Failed to load grievance board.{" "}
        <button className="btn btn-secondary btn-sm" onClick={() => refetch()}>
          Retry
        </button>
      </div>
    );
  }

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Grievance Board</h1>
          <p className="muted">Linear-style kanban · status + severity dashboard</p>
        </div>
        <div className="toolbar-row">
          <select
            value={severityFilter ?? ""}
            onChange={(e) => setSeverityFilter(e.target.value === "" ? undefined : Number(e.target.value))}
          >
            <option value="">All severities</option>
            {severities.map((s) => (
              <option key={s.id} value={s.id}>
                {s.code} — {s.description}
              </option>
            ))}
          </select>
          <button className="btn btn-secondary" onClick={() => refetch()}>
            Refresh
          </button>
        </div>
      </header>

      {/* Dashboard counts */}
      {counts && (
        <div className="grv-dashboard">
          <CountPill label="Total" value={counts.TOTAL} />
          <CountPill label="New" value={counts.NEW} cls="grv-count-pill--new" />
          <CountPill label="Assigned" value={counts.ASSIGNED} cls="grv-count-pill--assigned" />
          <CountPill label="Resolved" value={counts.RESOLVED} cls="grv-count-pill--resolved" />
          <CountPill label="Today" value={counts.TODAYS_TICKET} cls="grv-count-pill--today" />
          <CountPill label="Citizens" value={counts.RAISED_BY_CITIZEN} />
        </div>
      )}

      {/* Kanban lanes */}
      <div className="grv-board">
        {lanes.map((lane) => (
          <div key={lane.key} className={`grv-lane ${lane.cls}`}>
            <div className="grv-lane__header">
              <span className="grv-lane__title">{lane.label}</span>
              <span className="grv-lane__count">{lane.tickets.length}</span>
            </div>
            <div className="grv-lane__body">
              {lane.tickets.map((t) => (
                <TicketCard
                  key={t.id}
                  ticket={t}
                  onSelect={onSelectTicket ? () => onSelectTicket(t) : undefined}
                />
              ))}
              {lane.tickets.length === 0 && <p className="grv-empty">No tickets</p>}
            </div>
          </div>
        ))}
      </div>

      {filtered.length === 0 && tickets.length > 0 && (
        <p className="grv-empty">No tickets match the severity filter.</p>
      )}
    </section>
  );
}
