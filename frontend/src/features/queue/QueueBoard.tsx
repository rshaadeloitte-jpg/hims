import { useDashboard } from "./queries";
import { useQueueRealtime } from "./hooks";
import { TOKEN_STATUS } from "./zodSchema";
import type { QueueToken } from "./queries";

function StatusDot({ status }: { status: string }) {
  return <span className={`q-status-dot q-status-dot--${status}`}>{status}</span>;
}

function TokenCard({ token }: { token: QueueToken }) {
  const statusKey = TOKEN_STATUS[token.status as keyof typeof TOKEN_STATUS] ?? "WAITING";
  const cardClass = [
    "q-token-card",
    token.priority === "EMERGENCY" ? "q-token-card--emergency" : "",
    token.status === 2 ? "q-token-card--called" : "",
    token.status === 3 ? "q-token-card--completed" : "",
  ]
    .filter(Boolean)
    .join(" ");

  return (
    <div className={cardClass}>
      <span className="q-token-card__no">#{token.tokenNo}</span>
      <div className="q-token-card__info">
        <div className="q-token-card__patient">Patient {token.patientId}</div>
        <div className="q-token-card__meta">
          {new Date(token.createdAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
        </div>
      </div>
      <span className={`q-priority q-priority--${token.priority.toLowerCase()}`}>{token.priority}</span>
      <span className={`q-status q-status--${statusKey.toLowerCase().replace("_", "-")}`}>{statusKey}</span>
    </div>
  );
}

/**
 * M27 QueueBoard — Linear-style real-time board showing all counters
 * with waiting, called, and completed columns.
 */
export function QueueBoard() {
  const { data: dashboards = [], isLoading, isError } = useDashboard();
  const { status: connStatus } = useQueueRealtime();

  if (isLoading) {
    return (
      <div className="loading-state">
        <div className="spinner" />
        Loading queue board…
      </div>
    );
  }

  if (isError) {
    return <div className="alert alert--error">Failed to load queue board.</div>;
  }

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Queue Board</h1>
          <p className="muted">Real-time token management across all counters</p>
        </div>
        <StatusDot status={connStatus} />
      </header>

      {dashboards.length === 0 && <p className="q-empty">No active counters.</p>}

      <div className="q-board">
        {dashboards.map((db) => (
          <div key={db.counterId} className="q-board__column">
            <div className="q-board__column-header">
              <span className="q-board__column-title">{db.counterName}</span>
              <span className="q-board__column-count">{db.waiting.length}</span>
            </div>
            <div className="q-board__column-body">
              {db.nowServing && (
                <TokenCard token={db.nowServing} />
              )}
              {db.waiting.map((t) => (
                <TokenCard key={t.tokenId} token={t} />
              ))}
              {db.waiting.length === 0 && !db.nowServing && (
                <p className="q-empty">No tokens waiting</p>
              )}
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}