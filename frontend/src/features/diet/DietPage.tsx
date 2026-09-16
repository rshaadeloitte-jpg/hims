import { Table, Text } from "@chakra-ui/react";
import { HimsSelect } from "../../components/ui";
import { useDietOrders, useUpdateDietOrderStatus } from "./queries";

const STATUS_LABELS: Record<number, string> = {
  1: "Unreleased",
  2: "Active",
  3: "Cancelled",
};

export function DietPage() {
  const { data: orders = [], isLoading } = useDietOrders();
  const updateStatus = useUpdateDietOrderStatus();

  if (isLoading) return <p className="muted">Loading diet orders…</p>;

  return (
    <section className="page">
      <header className="page-header"><h1>Diet & Kitchen</h1></header>
      <div className="card">
        <Table.Root size="sm" variant="line">
          <Table.Header><Table.Row><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Patient</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Meal</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Diet Type</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Status</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Ordered At</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Actions</Table.ColumnHeader></Table.Row></Table.Header>
          <Table.Body>
            {orders.map((o) => (
              <Table.Row key={o.id}>
                <Table.Cell>{o.patientName ?? `Patient #${o.patientId}`}</Table.Cell>
                <Table.Cell>{o.mealType ?? "Standard"}</Table.Cell>
                <Table.Cell>{o.dietType ?? `Service #${o.serviceId}`}</Table.Cell>
                <Table.Cell>{STATUS_LABELS[o.orderStatus] ?? (o.status ? "Active" : "Inactive")}</Table.Cell>
                <Table.Cell>{o.orderedAt ?? o.fromDate ?? "—"}</Table.Cell>
                <Table.Cell>
                  <HimsSelect
                    value={String(o.orderStatus ?? (o.status ? 2 : 1))}
                    onChange={(e) => updateStatus.mutate({ id: o.id, status: Number(e.target.value) })}
                  >
                    <option value="1">Unreleased</option>
                    <option value="2">Active</option>
                    <option value="3">Cancelled</option>
                  </HimsSelect>
                </Table.Cell>
              </Table.Row>
            ))}
            {orders.length === 0 && <Table.Row><Table.Cell colSpan={6}><Text color="gray.500">No diet orders.</Text></Table.Cell></Table.Row>}
          </Table.Body>
        </Table.Root>
      </div>
    </section>
  );
}
