import { useCabinBoard } from "./queries";
import { useQueueRealtime } from "./hooks";

interface CabinDisplayProps {
  counterId: number;
}

/**
 * M27 CabinDisplay — large-screen display for a single cabin/counter.
 * Shows the currently serving token number prominently with upcoming queue.
 */
export function CabinDisplay({ counterId }: CabinDisplayProps) {
  const { data: board, isLoading, isError } = useCabinBoard(counterId);
  const { status: connStatus } = useQueueRealtime();

  if (isLoading) {
    return (
      <div className="loading-state">
        <div className="spinner" />
        Loading cabin display…
      </div>
    );
  }

  if (isError) {
    return <div className="alert alert--error">Failed to load cabin display.</div>;
  }

  if (!board) {
    return <p className="q-empty">No data for this cabin.</p>;
  }

  const nextTokens = board.waiting.slice(0, 5);

  return (
    <section className="q-cabin">
      <div style={{ position: "absolute", top: 16, right: 16 }}>
        <span className={`q-status-dot q-status-dot--${connStatus}`}>{connStatus}</span>
      </div>

      <p className="q-cabin__now-serving">Now Serving</p>

      {board.nowServing ? (
        <>
          <div className="q-cabin__token-no">#{board.nowServing.tokenNo}</div>
          <p className="q-cabin__patient">Patient {board.nowServing.patientId}</p>
        </>
      ) : (
        <>
          <div className="q-cabin__token-no" style={{ color: "var(--color-text-muted)" }}>—</div>
          <p className="q-cabin__patient" style={{ color: "var(--color-text-muted)" }}>No patient being served</p>
        </>
      )}

      {nextTokens.length > 0 && (
        <>
          <p className="q-cabin__next">
            Next: <strong>#{nextTokens[0].tokenNo}</strong> · {board.waiting.length} waiting
          </p>
          <div className="q-cabin__waiting-strip">
            {nextTokens.map((t) => (
              <span key={t.tokenId} className="q-cabin__waiting-chip">
                #{t.tokenNo}
              </span>
            ))}
          </div>
        </>
      )}
    </section>
  );
}