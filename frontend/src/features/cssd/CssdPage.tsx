import { Table, Text } from "@chakra-ui/react";
import { HimsSelect } from "../../components/ui";
import { useKits, useUpdateKitStatus } from "./queries";

const STATUSES = ["PREPARING", "STERILIZING", "READY", "EXPIRED"];

export function CssdPage() {
  const { data: kits = [], isLoading } = useKits();
  const updateStatus = useUpdateKitStatus();

  if (isLoading) return <p className="muted">Loading kits…</p>;

  return (
    <section className="page">
      <header className="page-header"><h1>CSSD</h1></header>
      <div className="card">
        <Table.Root size="sm" variant="line">
          <Table.Header><Table.Row><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Kit Name</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Status</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Sterilized</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Expires</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Actions</Table.ColumnHeader></Table.Row></Table.Header>
          <Table.Body>
            {kits.map((k) => (
              <Table.Row key={k.id}>
                <Table.Cell>{k.kitName}</Table.Cell>
                <Table.Cell>{k.status}</Table.Cell>
                <Table.Cell>{k.sterilizedAt ?? "—"}</Table.Cell>
                <Table.Cell>{k.expiresAt ?? "—"}</Table.Cell>
                <Table.Cell>
                  <HimsSelect value={k.status} onChange={(e) => updateStatus.mutate({ id: k.id, status: e.target.value })}>
                    {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
                  </HimsSelect>
                </Table.Cell>
              </Table.Row>
            ))}
            {kits.length === 0 && <Table.Row><Table.Cell colSpan={5}><Text color="gray.500">No kits.</Text></Table.Cell></Table.Row>}
          </Table.Body>
        </Table.Root>
      </div>
    </section>
  );
}
