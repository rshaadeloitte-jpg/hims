import { useQuery } from "@tanstack/react-query";
const BASE = import.meta.env.VITE_MORTUARY_API_BASE ?? "/api";

export function MortuaryPage() {
  const { data: chambers = [] } = useQuery({ queryKey: ["mortuary-chambers"], queryFn: () => fetch(`${BASE}/v1/chambers`).then(r => r.json()) });
  return (
    <section className="page">
      <header className="page-header"><h1>Mortuary</h1></header>
      <div className="card">
        <table className="table">
          <thead><tr><th>Chamber</th><th>Body</th><th>Admitted</th><th>Status</th><th>Death Cert</th></tr></thead>
          <tbody>
            {chambers.map((c: any) => <tr key={c.id}><td>{c.chamberNo}</td><td>{c.bodyName ?? "—"}</td><td>{c.admittedAt}</td><td>{c.status}</td><td>{c.deathCertNo ?? "—"}</td></tr>)}
            {chambers.length === 0 && <tr><td colSpan={5} className="muted">No chambers allocated.</td></tr>}
          </tbody>
        </table>
      </div>
    </section>
  );
}
