import { useRadOrders, useUpdateRadOrderStatus } from "./queries";

const STATUSES = ["ORDERED", "SCHEDULED", "IN_PROGRESS", "COMPLETED", "CANCELLED"];

export function RadiologyPage() {
  const { data: orders = [], isLoading } = useRadOrders();
  const updateStatus = useUpdateRadOrderStatus();

  if (isLoading) return <p className="muted">Loading radiology orders…</p>;

  return (
    <section className="page">
      <header className="page-header"><h1>Radiology</h1></header>
      <div className="card">
        <table className="table">
          <thead><tr><th>Patient</th><th>Modality</th><th>Body Part</th><th>Priority</th><th>Status</th><th>Scheduled</th><th>Actions</th></tr></thead>
          <tbody>
            {orders.map((o) => (
              <tr key={o.id ?? o.radOrderId}>
                <td>{o.patientName ?? o.patientId}</td>
                <td>{o.modality}</td>
                <td>{o.bodyPart ?? "—"}</td>
                <td>{o.priority}</td>
                <td>{o.status}</td>
                <td>{o.scheduledAt ?? "—"}</td>
                <td>
                  <select value={o.status} onChange={(e) => updateStatus.mutate({ id: (o.id ?? o.radOrderId), status: e.target.value })}>
                    {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
                  </select>
                </td>
              </tr>
            ))}
            {orders.length === 0 && <tr><td colSpan={7} className="muted">No radiology orders.</td></tr>}
          </tbody>
        </table>
      </div>
    </section>
  );
}
