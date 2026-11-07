import { Button, Table } from "@chakra-ui/react";

export function CrossMatch() {
  return (
    <section className="page">
      <header className="page-header">
        <h1>Cross Match</h1>
        <Button variant="secondary" disabled>New Cross Match</Button>
      </header>

      <p className="muted">Cross-match is not available yet.</p>

      <div className="card">
        <h2>Cross Match Requests</h2>
        <Table.Root size="sm" variant="line">
          <Table.Header>
            <Table.Row><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Bag</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Patient</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Method</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Compatibility</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Requested At</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Actions</Table.ColumnHeader></Table.Row>
          </Table.Header>
          <Table.Body>
            <Table.Row><Table.Cell colSpan={6}>No cross matches.</Table.Cell></Table.Row>
          </Table.Body>
        </Table.Root>
      </div>
    </section>
  );
}
