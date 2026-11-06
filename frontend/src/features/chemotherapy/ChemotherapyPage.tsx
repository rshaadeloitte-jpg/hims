import { useQuery } from "@tanstack/react-query";
import { Table } from "@chakra-ui/react";
import { ApiError } from "../../api/client";
const BASE = import.meta.env.VITE_CHEMO_API_BASE ?? "/api";

export function ChemotherapyPage() {
  const { data } = useQuery({
    queryKey: ["chemo-regimens"],
    queryFn: async () => {
      const r = await fetch(`${BASE}/v1/regimens`);
      if (!r.ok) throw new ApiError(r.status, "ERR", "Request failed");
      return r.json();
    },
  });
  const regimens = Array.isArray(data) ? data : [];
  return (
    <section className="page">
      <header className="page-header"><h1>Chemotherapy</h1></header>
      <div className="card">
        <Table.Root size="sm" variant="line">
          <Table.Header><Table.Row><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Patient</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Regimen</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Cycle</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Status</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Next Due</Table.ColumnHeader></Table.Row></Table.Header>
          <Table.Body>
            {regimens.map((r: any) => <Table.Row key={r.id}><Table.Cell>{r.patientName}</Table.Cell><Table.Cell>{r.regimenName}</Table.Cell><Table.Cell>{r.currentCycle}</Table.Cell><Table.Cell>{r.status}</Table.Cell><Table.Cell>{r.nextDueDate ?? "—"}</Table.Cell></Table.Row>)}
            {regimens.length === 0 && <Table.Row><Table.Cell colSpan={5}>No regimens.</Table.Cell></Table.Row>}
          </Table.Body>
        </Table.Root>
      </div>
    </section>
  );
}
