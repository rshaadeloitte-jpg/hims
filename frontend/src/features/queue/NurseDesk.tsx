import { useState } from "react";
import { useTokens, useCounters, useCallNext, useTokenAction } from "./queries";

/**
 * M27 NurseDesk — triage and queue management interface for nursing staff.
 * Two-panel layout: left shows waiting queue, right shows called/completed.
 */
export function NurseDesk() {
  const { data: tokens = [], isLoading } = useTokens();
  const { data: counters = [] } = useCounters();
  const [selectedCounter, setSelectedCounter] = useState<number>(0);
  const callNext = useCallNext(selectedCounter);
  const noShow = useTokenAction("no-show");
  const complete = useTokenAction("complete");

  const filtered = selectedCounter > 0 ? tokens.filter((t) => t.counterId === selectedCounter) : tokens;
  const waiting = filtered.filter((t) => t.status === 1).sort((a, b) => {
    if (a.priority === "EMERGENCY" && b.priority !== "EMERGENCY") return -1;
    if (a.priority !== "EMERGENCY" && b.priority === "EMERGENCY") return 1;
    return new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime();
  });
  const called = filtered.filter((t) => t.status === 2);
  const completed = filtered.filter((t) => t.status === 3);
  const noShows = filtered.filter((t) => t.status === 4);

  const counterName = (id: number) => counters.find((c) => c.counterId === id)?.counterName ?? `Counter ${id}`;

  if (isLoading) {
    return (
      <div className="loading-state">
        <div className="spinner" />
        Loading nurse desk…
      </div>
    );
  }

  return (
    <section className="page">
      <header className="page-header">
        <h1>Nurse Desk</h1>
        <div className="q-toolbar">
          <select value={selectedCounter} onChange={(e) => setSelectedCounter(Number(e.target.value))}>
            <option value={0}>All Counters</option>
            {counters.map((c) => (
              <option key={c.counterId} value={c.counterId}>{c.counterName}</option>
            ))}
          </select>
          {selectedCounter > 0 && (
            <button
              className="q-call-btn q-call-btn--primary"
              onClick={() => callNext.mutate()}
              disabled={callNext.isPending || waiting.length === 0}
            >
              {callNext.isPending ? "Calling…" : "Call Next"}
            </button>
          )}
        </div>
      </header>

      <div className="q-nurse-grid">
        {/* Left: Waiting queue */}
        <div className="q-nurse-panel">
          <h3>Waiting ({waiting.length})</h3>
          {waiting.length === 0 && <p className="q-empty">No patients waiting</p>}
          {waiting.map((t) => (
            <div key={t.tokenId} className={`q-token-card ${t.priority === "EMERGENCY" ? "q-token-card--emergency" : ""}`}>
              <span className="q-token-card__no">#{t.tokenNo}</span>
              <div className="q-token-card__info">
                <div className="q-token-card__patient">Patient {t.patientId}</div>
                <div className="q-token-card__meta">{counterName(t.counterId)}</div>
              </div>
              <span className={`q-priority q-priority--${t.priority.toLowerCase()}`}>{t.priority}</span>
            </div>
          ))}
        </div>

        {/* Right: Called + Completed + No-show */}
        <div className="q-nurse-panel">
          <h3>Called ({called.length})</h3>
          {called.map((t) => (
            <div key={t.tokenId} className="q-token-card q-token-card--called">
              <span className="q-token-card__no">#{t.tokenNo}</span>
              <div className="q-token-card__info">
                <div className="q-token-card__patient">Patient {t.patientId}</div>
                <div className="q-token-card__meta">{counterName(t.counterId)}</div>
              </div>
              <div className="q-manager-actions">
                <button className="btn btn-sm btn-secondary" onClick={() => complete.mutate(t.tokenId)}>Done</button>
                <button className="btn btn-sm btn-secondary" onClick={() => noShow.mutate(t.tokenId)}>No-show</button>
              </div>
            </div>
          ))}
          {called.length === 0 && <p className="q-empty" style={{ padding: "var(--space-3)" }}>None called</p>}

          <h3 style={{ marginTop: "var(--space-4)" }}>Completed ({completed.length})</h3>
          {completed.slice(0, 5).map((t) => (
            <div key={t.tokenId} className="q-token-card q-token-card--completed">
              <span className="q-token-card__no">#{t.tokenNo}</span>
              <div className="q-token-card__info">
                <div className="q-token-card__patient">Patient {t.patientId}</div>
              </div>
            </div>
          ))}

          {noShows.length > 0 && (
            <>
              <h3 style={{ marginTop: "var(--space-4)" }}>No-shows ({noShows.length})</h3>
              {noShows.map((t) => (
                <div key={t.tokenId} className="q-token-card q-token-card--completed">
                  <span className="q-token-card__no">#{t.tokenNo}</span>
                  <div className="q-token-card__info">
                    <div className="q-token-card__patient">Patient {t.patientId}</div>
                  </div>
                  <span className="q-status q-status--no-show">No-show</span>
                </div>
              ))}
            </>
          )}
        </div>
      </div>
    </section>
  );
}