import { useQuery } from "@tanstack/react-query";
import { ApiError } from "../../api/client";
const BASE = import.meta.env.VITE_DOCTOR_SHARE_API_BASE ?? "/api";
async function j<T>(u: string): Promise<T> { const r = await fetch(u, { headers: { "X-Request-ID": crypto.randomUUID() } }); if (!r.ok) throw new ApiError(r.status, "ERR", "Failed"); return r.json(); }

export function DoctorSharePage() {
  const { data: shares = [] } = useQuery({ queryKey: ["doctor-shares"], queryFn: () => j<any[]>(`${BASE}/v1/doctor-shares`) });
  return (
    <section className="page">
      <header className="page-header"><h1>Doctor Share</h1></header>
      <div className="card">
        <table className="table">
          <thead><tr><th>Doctor</th><th>Service</th><th>Share %</th><th>Amount</th><th>Period</th></tr></thead>
          <tbody>
            {shares.map((s: any) => <tr key={s.id}><td>{s.doctorName}</td><td>{s.service}</td><td>{s.sharePercent}</td><td>{s.amount}</td><td>{s.period}</td></tr>)}
            {shares.length === 0 && <tr><td colSpan={5} className="muted">No doctor shares.</td></tr>}
          </tbody>
        </table>
      </div>
    </section>
  );
}
