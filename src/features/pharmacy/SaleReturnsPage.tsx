import { useSaleReturns, useCompleteSaleReturn, useRejectSaleReturn } from "./queries";

export function SaleReturnsPage() {
  const { data: returns = [], isLoading } = useSaleReturns();
  const complete = useCompleteSaleReturn();
  const reject = useRejectSaleReturn();

  const statusLabel = (s: number) =>
    s === 1 ? "Initiated" : s === 2 ? "Approved" : s === 3 ? "Completed" : "Rejected";

  if (isLoading) return <p>Loading…</p>;
  return (
    <section className="page">
      <header className="page__header"><h1>Sale Returns</h1></header>
      <table className="table">
        <thead><tr><th>#</th><th>Original Sale</th><th>Patient</th><th>Reason</th><th>Status</th><th>Actions</th></tr></thead>
        <tbody>
          {returns.map((r) => (
            <tr key={r.returnId}>
              <td>{r.returnId}</td>
              <td>{r.originalSaleId}</td>
              <td>{r.patientId}</td>
              <td>{r.returnReason}</td>
              <td>{statusLabel(r.status)}</td>
              <td>
                {r.status === 1 && (
                  <>
                    <button className="btn" onClick={() => complete.mutate(r.returnId)}>Complete</button>
                    <button className="btn" onClick={() => reject.mutate(r.returnId)}>Reject</button>
                  </>
                )}
              </td>
            </tr>
          ))}
          {returns.length === 0 && <tr><td colSpan={6} className="muted">No returns.</td></tr>}
        </tbody>
      </table>
    </section>
  );
}