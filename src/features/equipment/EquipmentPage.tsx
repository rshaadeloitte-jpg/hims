import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../api/client";
const BASE = import.meta.env.VITE_EQUIPMENT_API_BASE ?? "/api";
async function j<T>(u: string, o?: RequestInit): Promise<T> {
  const r = await fetch(u, { headers: { "Content-Type": "application/json", "X-Request-ID": crypto.randomUUID() }, ...o });
  if (!r.ok) throw new ApiError(r.status, "ERR", "Failed"); return r.json();
}
export interface Equipment { id: number; name: string; assetTag: string; status: string; lastPmDate: string; }

export function EquipmentPage() {
  const { data: items = [] } = useQuery({ queryKey: ["equipment"], queryFn: () => j<Equipment[]>(`${BASE}/v1/equipment`) });
  const qc = useQueryClient();
  const updateStatus = useMutation({ mutationFn: ({ id, status }: { id: number; status: string }) => j(`${BASE}/v1/equipment/${id}/status`, { method: "PATCH", body: JSON.stringify({ status }) }), onSuccess: () => qc.invalidateQueries({ queryKey: ["equipment"] }) });

  return (
    <section className="page">
      <header className="page-header"><h1>Equipment</h1></header>
      <div className="card">
        <table className="table">
          <thead><tr><th>Asset Tag</th><th>Name</th><th>Status</th><th>Last PM</th><th>Actions</th></tr></thead>
          <tbody>
            {items.map((e) => (
              <tr key={e.id}>
                <td>{e.assetTag}</td><td>{e.name}</td><td>{e.status}</td><td>{e.lastPmDate ?? "—"}</td>
                <td><select value={e.status} onChange={(ev) => updateStatus.mutate({ id: e.id, status: ev.target.value })}>
                  {["ACTIVE", "UNDER_MAINTENANCE", "DECOMMISSIONED"].map((s) => <option key={s} value={s}>{s}</option>)}
                </select></td>
              </tr>
            ))}
            {items.length === 0 && <tr><td colSpan={5} className="muted">No equipment.</td></tr>}
          </tbody>
        </table>
      </div>
    </section>
  );
}
