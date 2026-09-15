import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../api/client";
const BASE = import.meta.env.VITE_LINEN_API_BASE ?? "/api";
async function j<T>(u: string, o?: RequestInit): Promise<T> {
  const r = await fetch(u, { headers: { "Content-Type": "application/json", "X-Request-ID": crypto.randomUUID() }, ...o });
  if (!r.ok) throw new ApiError(r.status, "ERR", "Failed"); return r.json();
}
export interface LinenRequest { id: number; fromStore: string; toStore: string; status: string; requestedAt: string; }

export function LinenFrontPage() {
  const { data: reqs = [] } = useQuery({ queryKey: ["linen-reqs"], queryFn: () => j<LinenRequest[]>(`${BASE}/v1/linen-requests`) });
  const qc = useQueryClient();
  const updateStatus = useMutation({ mutationFn: ({ id, status }: { id: number; status: string }) => j(`${BASE}/v1/linen-requests/${id}/status`, { method: "PATCH", body: JSON.stringify({ status }) }), onSuccess: () => qc.invalidateQueries({ queryKey: ["linen-reqs"] }) });

  return (
    <section className="page">
      <header className="page-header"><h1>Linen & Laundry</h1></header>
      <div className="card">
        <table className="table">
          <thead><tr><th>From</th><th>To</th><th>Status</th><th>Requested</th><th>Actions</th></tr></thead>
          <tbody>
            {reqs.map((r) => (
              <tr key={r.id}><td>{r.fromStore}</td><td>{r.toStore}</td><td>{r.status}</td><td>{r.requestedAt}</td>
                <td><select value={r.status} onChange={(e) => updateStatus.mutate({ id: r.id, status: e.target.value })}>
                  {["PENDING", "DISPATCHED", "RECEIVED", "WASHED"].map((s) => <option key={s} value={s}>{s}</option>)}
                </select></td>
              </tr>
            ))}
            {reqs.length === 0 && <tr><td colSpan={5} className="muted">No requests.</td></tr>}
          </tbody>
        </table>
      </div>
    </section>
  );
}
