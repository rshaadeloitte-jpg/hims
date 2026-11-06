import { useQuery } from "@tanstack/react-query";
import { Table, Text } from "@chakra-ui/react";
import { ApiError } from "../../api/client";
const BASE = import.meta.env.VITE_MORTUARY_API_BASE ?? "/api";

export function MortuaryPage() {
  const { data } = useQuery({
    queryKey: ["mortuary-chambers"],
    queryFn: async () => {
      const r = await fetch(`${BASE}/v1/chambers`);
      if (!r.ok) throw new ApiError(r.status, "ERR", "Request failed");
      return r.json();
    },
  });
  const chambers = Array.isArray(data) ? data : [];
  return (
    <section className="page">
      <header className="page-header"><h1>Mortuary</h1></header>
      <div className="card">
        <Table.Root size="sm" variant="line">
          <Table.Header><Table.Row><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Chamber</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Body</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Admitted</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Status</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Death Cert</Table.ColumnHeader></Table.Row></Table.Header>
          <Table.Body>
            {chambers.map((c: any) => <Table.Row key={c.id}><Table.Cell>{c.chamberNo}</Table.Cell><Table.Cell>{c.bodyName ?? "—"}</Table.Cell><Table.Cell>{c.admittedAt}</Table.Cell><Table.Cell>{c.status}</Table.Cell><Table.Cell>{c.deathCertNo ?? "—"}</Table.Cell></Table.Row>)}
            {chambers.length === 0 && <Table.Row><Table.Cell colSpan={5}><Text color="gray.500">No chambers allocated.</Text></Table.Cell></Table.Row>}
          </Table.Body>
        </Table.Root>
      </div>
    </section>
  );
}
