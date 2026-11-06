import { useSyncStatuses } from "./queries";

export function InteropNewPage() {
  const { data: statuses = [], isLoading } = useSyncStatuses();

  if (isLoading) return <p className="muted">Loading sync statuses…</p>;

  return (
    <section className="page">
      <header className="page-header"><h1>Interoperability</h1></header>
      <div className="card">
        <table className="table">
          <thead><tr><th>Drug Code</th><th>Drug Name</th><th>Sync Status</th></tr></thead>
          <tbody>
            {statuses.map((s) => <tr key={s.id}><td>{s.drugCode}</td><td>{s.drugName}</td><td>{s.syncStatus}</td></tr>)}
            {statuses.length === 0 && <tr><td colSpan={3} className="muted">No sync records.</td></tr>}
          </tbody>
        </table>
      </div>
    </section>
  );
}
