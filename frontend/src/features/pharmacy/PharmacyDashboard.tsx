import { Link } from "react-router-dom";
import { useItemSales, useSaleReturns, useInterventions } from "./queries";
import { Table, Text } from "@chakra-ui/react";

export function PharmacyDashboard() {
  const { data: sales = [], isLoading: salesLoading } = useItemSales();
  const { data: returns = [] } = useSaleReturns();
  const { data: interventions = [] } = useInterventions();

  if (salesLoading) return <p>Loading…</p>;

  const totalSales = sales.length;
  const dispensed = sales.filter((s) => s.isDispensed).length;
  const pending = sales.filter((s) => !s.isDispensed && !s.isReturned).length;
  const activeReturns = returns.filter((r) => r.status === 1).length;

  return (
    <section className="page">
      <header className="page__header">
        <h1>Pharmacy</h1>
        <Link to="/pharmacy/new" className="btn btn--primary">+ New Sale</Link>
      </header>
      <div className="card-grid">
        <div className="card"><Text fontSize="sm" fontWeight="medium">Total Sales</Text><strong>{totalSales}</strong></div>
        <div className="card"><Text fontSize="sm" fontWeight="medium">Dispensed</Text><strong>{dispensed}</strong></div>
        <div className="card"><Text fontSize="sm" fontWeight="medium">Pending</Text><strong>{pending}</strong></div>
        <div className="card"><Text fontSize="sm" fontWeight="medium">Active Returns</Text><strong>{activeReturns}</strong></div>
        <div className="card"><Text fontSize="sm" fontWeight="medium">Interventions</Text><strong>{interventions.length}</strong></div>
      </div>
      <h2>Recent Sales</h2>
      <Table.Root size="sm" variant="line">
        <Table.Header><Table.Row><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">#</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Patient</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Amount</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Status</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Date</Table.ColumnHeader></Table.Row></Table.Header>
        <Table.Body>
          {sales.slice(0, 20).map((s) => (
            <Table.Row key={s.saleId}>
              <Table.Cell>{s.saleId}</Table.Cell>
              <Table.Cell>{s.patientId}</Table.Cell>
              <Table.Cell>{s.totalAmount?.toFixed(2)}</Table.Cell>
              <Table.Cell>{s.isDispensed ? "Dispensed" : s.isReturned ? "Returned" : "Pending"}</Table.Cell>
              <Table.Cell>{s.createdAt?.slice(0, 10)}</Table.Cell>
            </Table.Row>
          ))}
          {sales.length === 0 && <Table.Row><Table.Cell colSpan={5} className="muted">No sales.</Table.Cell></Table.Row>}
        </Table.Body>
      </Table.Root>
    </section>
  );
}
