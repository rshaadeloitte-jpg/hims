import { useState } from "react";
import { useDepartmentDashboard, useDepartmentTokens } from "./queries";
import { TOKEN_STATUS } from "./zodSchema";

interface DepartmentQueueProps {
  departmentId: number;
  departmentName?: string;
}

/**
 * M27 DepartmentQueue — department-specific queue view (e.g. RAD, PATHO).
 * Shows department stats, counters, and token list.
 */
export function DepartmentQueue({ departmentId, departmentName }: DepartmentQueueProps) {
  const { data: dashboard, isLoading: dashLoading } = useDepartmentDashboard(departmentId);
  const { data: tokens = [], isLoading: tokensLoading } = useDepartmentTokens(departmentId);
  const [statusFilter, setStatusFilter] = useState<string>("ALL");
  const [priorityFilter, setPriorityFilter] = useState<string>("ALL");

  const filtered = tokens.filter((t) => {
    if (statusFilter !== "ALL" && TOKEN_STATUS[t.status as keyof typeof TOKEN_STATUS] !== statusFilter) return false;
    if (priorityFilter !== "ALL" && t.priority !== priorityFilter) return false;
    return true;
  });

  if (dashLoading || tokensLoading) {
    return (
      <div className="loading-state">
        <div className="spinner" />
        Loading department queue…
      </div>
    );
  }

  return (
    <section className="page">
      <header className="q-dept-header">
        <div>
          <h1>{departmentName ?? `Department ${departmentId}`} Queue</h1>
        </div>
        {dashboard && (
          <div className="q-dept-stats">
            <div className="q-dept-stat">
              <div className="q-dept-stat__value">{dashboard.waiting}</div>
              <div className="q-dept-stat__label">Waiting</div>
            </div>
            <div className="q-dept-stat">
              <div className="q-dept-stat__value">{dashboard.called}</div>
              <div className="q-dept-stat__label">Called</div>
            </div>
            <div className="q-dept-stat">
              <div className="q-dept-stat__value">{dashboard.nowServing}</div>
              <div className="q-dept-stat__label">Now Serving</div>
            </div>
            <div className="q-dept-stat">
              <div className="q-dept-stat__value">{dashboard.avgWaitMinutes}m</div>
              <div className="q-dept-stat__label">Avg Wait</div>
            </div>
          </div>
        )}
      </header>

      {/* Counters strip */}
      {dashboard && dashboard.counters.length > 0 && (
        <div className="q-board" style={{ marginBottom: "var(--space-4)" }}>
          {dashboard.counters.map((c) => (
            <div key={c.counterId} className="q-board__column">
              <div className="q-board__column-header">
                <span className="q-board__column-title">{c.counterName}</span>
                <span className="q-board__column-count">{c.waitingCount}</span>
              </div>
              <div className="q-board__column-body" style={{ minHeight: 60, padding: "var(--space-3)" }}>
                <div style={{ fontSize: "var(--font-size-sm)", color: "var(--color-text-muted)" }}>
                  Now serving: <strong style={{ color: "var(--color-primary)" }}>#{c.nowServing || "—"}</strong>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Filters */}
      <div className="q-toolbar">
        <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
          <option value="ALL">All Statuses</option>
          <option value="WAITING">Waiting</option>
          <option value="CALLED">Called</option>
          <option value="COMPLETED">Completed</option>
          <option value="NO_SHOW">No-show</option>
        </select>
        <select value={priorityFilter} onChange={(e) => setPriorityFilter(e.target.value)}>
          <option value="ALL">All Priorities</option>
          <option value="NORMAL">Normal</option>
          <option value="EMERGENCY">Emergency</option>
        </select>
      </div>

      {/* Token list */}
      <div className="card">
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Token</th>
                <th>Patient</th>
                <th>Priority</th>
                <th>Status</th>
                <th>Issued At</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((t) => {
                const statusLabel = TOKEN_STATUS[t.status as keyof typeof TOKEN_STATUS] ?? "Unknown";
                return (
                  <tr key={t.tokenId}>
                    <td className="cell--num">#{t.tokenNo}</td>
                    <td>{t.patientId}</td>
                    <td>
                      <span className={`q-priority q-priority--${t.priority.toLowerCase()}`}>{t.priority}</span>
                    </td>
                    <td>
                      <span className={`q-status q-status--${statusLabel.toLowerCase().replace("_", "-")}`}>{statusLabel}</span>
                    </td>
                    <td>{new Date(t.createdAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}</td>
                  </tr>
                );
              })}
              {filtered.length === 0 && (
                <tr><td colSpan={5} className="q-empty">No tokens match filters.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </section>
  );
}