import { useQuery } from "@tanstack/react-query";
import { ApiError } from "../../api/client";

const BASE = import.meta.env.VITE_TARIFF_API_BASE ?? "/api";
async function j<T>(u: string, o?: RequestInit): Promise<T> {
  const r = await fetch(u, { headers: { "Content-Type": "application/json", "X-Request-ID": crypto.randomUUID() }, ...o });
  if (!r.ok) throw new ApiError(r.status, "ERR", "Failed");
  return r.json();
}

export function TariffsPage() {
  const { data: tariffs = [] } = useQuery({ queryKey: ["tariffs"], queryFn: () => j<any[]>(`${BASE}/v1/tariffs`) });
  return (
    <section className="page">
      <header className="page-header"><h1>Tariffs & Concessions</h1></header>
      <div className="card">
        <table className="table">
          <thead><tr><th>Service</th><th>Base Rate</th><th>Copay %</th><th>Package Class</th><th>Status</th></tr></thead>
          <tbody>
            {tariffs.map((t: any) => <tr key={t.id}><td>{t.serviceName}</td><td>{t.baseRate}</td><td>{t.copayPercent}</td><td>{t.packageClass}</td><td>{t.status}</td></tr>)}
            {tariffs.length === 0 && <tr><td colSpan={5} className="muted">No tariffs configured.</td></tr>}
          </tbody>
        </table>
      </div>
    </section>
  );
}
