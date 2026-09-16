import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Table } from "@chakra-ui/react";
import { HimsSelect } from "../../components/ui";
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
        <Table.Root size="sm" variant="line">
          <Table.Header><Table.Row><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Asset Tag</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Name</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Status</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Last PM</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Actions</Table.ColumnHeader></Table.Row></Table.Header>
          <Table.Body>
            {items.map((e) => (
              <Table.Row key={e.id}>
                <Table.Cell>{e.assetTag}</Table.Cell><Table.Cell>{e.name}</Table.Cell><Table.Cell>{e.status}</Table.Cell><Table.Cell>{e.lastPmDate ?? "—"}</Table.Cell>
                <Table.Cell><HimsSelect aria-label={`Status for ${e.assetTag}`} value={e.status} onChange={(ev) => updateStatus.mutate({ id: e.id, status: ev.target.value })}>
                  {["ACTIVE", "UNDER_MAINTENANCE", "DECOMMISSIONED"].map((s) => <option key={s} value={s}>{s}</option>)}
                </HimsSelect></Table.Cell>
              </Table.Row>
            ))}
            {items.length === 0 && <Table.Row><Table.Cell colSpan={5}>No equipment.</Table.Cell></Table.Row>}
          </Table.Body>
        </Table.Root>
      </div>
    </section>
  );
}
