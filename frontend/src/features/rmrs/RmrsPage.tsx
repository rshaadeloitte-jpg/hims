import { useQuery } from "@tanstack/react-query";
import { ApiError } from "../../api/client";
const BASE = import.meta.env.VITE_RMRS_API_BASE ?? "/api";

export function RmrsPage() {
  const { data } = useQuery({
    queryKey: ["rmrs-journals"],
    queryFn: async () => {
      const r = await fetch(`${BASE}/v1/journals`);
      if (!r.ok) throw new ApiError(r.status, "ERR", "Request failed");
      return r.json();
    },
  });
  const journals = Array.isArray(data) ? data : [];
  return (
    <section className="page">
      <header className="page-header"><h1>RMRS Treasury</h1></header>
      <div className="card">
        <table className="table">
          <thead><tr><th>Voucher No</th><th>Type</th><th>Amount</th><th>Date</th><th>Status</th></tr></thead>
          <tbody>
            {journals.map((j: any) => <tr key={j.id}><td>{j.voucherNo}</td><td>{j.type}</td><td>{j.amount}</td><td>{j.date}</td><td>{j.status}</td></tr>)}
            {journals.length === 0 && <tr><td colSpan={5} className="muted">No journal entries.</td></tr>}
          </tbody>
        </table>
      </div>
    </section>
  );
}
