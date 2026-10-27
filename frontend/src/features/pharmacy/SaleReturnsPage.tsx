import { useSaleReturns, useCompleteSaleReturn, useRejectSaleReturn } from "./queries";
import { Button, Table } from "@chakra-ui/react";

export function SaleReturnsPage() {
  const { data: returns = [], isLoading } = useSaleReturns();
  const complete = useCompleteSaleReturn();
  const reject = useRejectSaleReturn();

  const statusLabel = (s: number) =>
    s === 1 ? "Initiated" : s === 2 ? "Approved" : s === 3 ? "Completed" : "Rejected";

  if (isLoading) return <div className="loading-state" role="status"><span className="spinner" aria-hidden="true" /> Loading…</div>;
  return (
    <section className="page">
      <header className="page__header"><h1>Sale Returns</h1></header>
      <Table.Root size="sm" variant="line">
        <Table.Header><Table.Row><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">#</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Original Sale</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Patient</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Reason</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Status</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Actions</Table.ColumnHeader></Table.Row></Table.Header>
        <Table.Body>
          {returns.map((r) => (
            <Table.Row key={r.returnId}>
              <Table.Cell>{r.returnId}</Table.Cell>
              <Table.Cell>{r.originalSaleId}</Table.Cell>
              <Table.Cell>{r.patientId}</Table.Cell>
              <Table.Cell>{r.returnReason}</Table.Cell>
              <Table.Cell>{statusLabel(r.status)}</Table.Cell>
              <Table.Cell>
                {r.status === 1 && (
                  <>
                    <Button variant="primary" size="sm" onClick={() => complete.mutate(r.returnId)}>Complete</Button>
                    <Button variant="secondary" size="sm" onClick={() => reject.mutate(r.returnId)}>Reject</Button>
                  </>
                )}
              </Table.Cell>
            </Table.Row>
          ))}
          {returns.length === 0 && <Table.Row><Table.Cell colSpan={6} className="muted">No returns.</Table.Cell></Table.Row>}
        </Table.Body>
      </Table.Root>
    </section>
  );
}
