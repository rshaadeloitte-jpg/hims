import { useQuery } from "@tanstack/react-query";
const BASE = import.meta.env.VITE_CHEMO_API_BASE ?? "/api";

export function ChemotherapyPage() {
  const { data: regimens = [] } = useQuery({ queryKey: ["chemo-regimens"], queryFn: () => fetch(`${BASE}/v1/regimens`).then(r => r.json()) });
  return (
    <section className="page">
      <header className="page-header"><h1>Chemotherapy</h1></header>
      <div className="card">
        <table className="table">
          <thead><tr><th>Patient</th><th>Regimen</th><th>Cycle</th><th>Status</th><th>Next Due</th></tr></thead>
          <tbody>
            {regimens.map((r: any) => <tr key={r.id}><td>{r.patientName}</td><td>{r.regimenName}</td><td>{r.currentCycle}</td><td>{r.status}</td><td>{r.nextDueDate ?? "—"}</td></tr>)}
            {regimens.length === 0 && <tr><td colSpan={5} className="muted">No regimens.</td></tr>}
          </tbody>
        </table>
      </div>
    </section>
  );
}
