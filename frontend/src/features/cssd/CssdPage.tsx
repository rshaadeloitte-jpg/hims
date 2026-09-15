import { useKits, useUpdateKitStatus } from "./queries";

const STATUSES = ["PREPARING", "STERILIZING", "READY", "EXPIRED"];

export function CssdPage() {
  const { data: kits = [], isLoading } = useKits();
  const updateStatus = useUpdateKitStatus();

  if (isLoading) return <p className="muted">Loading kits…</p>;

  return (
    <section className="page">
      <header className="page-header"><h1>CSSD</h1></header>
      <div className="card">
        <table className="table">
          <thead><tr><th>Kit Name</th><th>Status</th><th>Sterilized</th><th>Expires</th><th>Actions</th></tr></thead>
          <tbody>
            {kits.map((k) => (
              <tr key={k.id}>
                <td>{k.kitName}</td>
                <td>{k.status}</td>
                <td>{k.sterilizedAt ?? "—"}</td>
                <td>{k.expiresAt ?? "—"}</td>
                <td>
                  <select value={k.status} onChange={(e) => updateStatus.mutate({ id: k.id, status: e.target.value })}>
                    {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
                  </select>
                </td>
              </tr>
            ))}
            {kits.length === 0 && <tr><td colSpan={5} className="muted">No kits.</td></tr>}
          </tbody>
        </table>
      </div>
    </section>
  );
}
