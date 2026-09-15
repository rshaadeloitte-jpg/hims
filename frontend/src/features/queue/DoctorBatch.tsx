import { useDoctorBatch, useCallPatient, useTokenAction } from "./queries";
import { TOKEN_STATUS } from "./zodSchema";
import type { DoctorBatchItem } from "./queries";

interface DoctorBatchProps {
  doctorId: number;
}

function BatchItem({ item, index, onCall, onComplete, onNoShow }: {
  item: DoctorBatchItem;
  index: number;
  onCall: (id: number) => void;
  onComplete: (id: number) => void;
  onNoShow: (id: number) => void;
}) {
  const statusLabel = TOKEN_STATUS[item.status as keyof typeof TOKEN_STATUS] ?? "Unknown";

  return (
    <div className="q-batch-item">
      <span className="q-batch-item__seq">{index + 1}</span>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontWeight: 500, fontSize: "var(--font-size-sm)" }}>
          {item.patientName ?? `Patient ${item.patientId}`}
        </div>
        <div style={{ fontSize: "var(--font-size-xs)", color: "var(--color-text-muted)" }}>
          Token #{item.tokenNo} · {new Date(item.createdAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
        </div>
      </div>
      <span className={`q-priority q-priority--${item.priority.toLowerCase()}`}>{item.priority}</span>
      <span className={`q-status q-status--${statusLabel.toLowerCase().replace("_", "-")}`}>{statusLabel}</span>
      <div className="q-manager-actions">
        {item.status === 1 && (
          <button className="btn btn-sm btn-primary" onClick={() => onCall(item.tokenId)}>Call</button>
        )}
        {item.status === 2 && (
          <>
            <button className="btn btn-sm btn-secondary" onClick={() => onComplete(item.tokenId)}>Complete</button>
            <button className="btn btn-sm btn-secondary" onClick={() => onNoShow(item.tokenId)}>No-show</button>
          </>
        )}
      </div>
    </div>
  );
}

/**
 * M27 DoctorBatch — view and manage a doctor's patient queue for the day.
 */
export function DoctorBatch({ doctorId }: DoctorBatchProps) {
  const { data: batch = [], isLoading, isError } = useDoctorBatch(doctorId);
  const callPatient = useCallPatient();
  const complete = useTokenAction("complete");
  const noShow = useTokenAction("no-show");

  if (isLoading) {
    return (
      <div className="loading-state">
        <div className="spinner" />
        Loading doctor batch…
      </div>
    );
  }

  if (isError) {
    return <div className="alert alert--error">Failed to load doctor batch.</div>;
  }

  const waiting = batch.filter((b) => b.status === 1).length;
  const inConsult = batch.filter((b) => b.status === 2).length;
  const done = batch.filter((b) => b.status === 3).length;

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Doctor Batch</h1>
          <p className="muted">Doctor #{doctorId} — {batch.length} patients today</p>
        </div>
        <div className="q-dept-stats">
          <div className="q-dept-stat">
            <div className="q-dept-stat__value">{waiting}</div>
            <div className="q-dept-stat__label">Waiting</div>
          </div>
          <div className="q-dept-stat">
            <div className="q-dept-stat__value">{inConsult}</div>
            <div className="q-dept-stat__label">In Consult</div>
          </div>
          <div className="q-dept-stat">
            <div className="q-dept-stat__value">{done}</div>
            <div className="q-dept-stat__label">Done</div>
          </div>
        </div>
      </header>

      {batch.length === 0 && <p className="q-empty">No patients in today's batch.</p>}

      <div className="q-batch-list">
        {batch.map((item, i) => (
          <BatchItem
            key={item.tokenId}
            item={item}
            index={i}
            onCall={(id) => callPatient.mutate(id)}
            onComplete={(id) => complete.mutate(id)}
            onNoShow={(id) => noShow.mutate(id)}
          />
        ))}
      </div>
    </section>
  );
}