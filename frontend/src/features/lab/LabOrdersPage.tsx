import { useState } from "react";
import { Button, Table } from "@chakra-ui/react";
import { TextInput } from "../../components/ui";
import {
  useLabOrders,
  useLabOrder,
  useLabOrderAction,
  useBarcode,
  useAcceptReject,
  useEnterResults,
  useFinalizeResult,
  useVerifyResult,
  useNotifyCritical,
} from "./queries";

export function LabOrdersPage() {
  const { data: orders = [], isLoading } = useLabOrders();
  const [selectedId, setSelectedId] = useState(0);
  const { data: order } = useLabOrder(selectedId);

  const collect = useLabOrderAction("collect");
  const start = useLabOrderAction("start");
  const cancel = useLabOrderAction("cancel");
  const barcode = useBarcode();
  const accept = useAcceptReject("accept");
  const reject = useAcceptReject("reject");
  const results = useEnterResults();
  const finalize = useFinalizeResult();
  const verify = useVerifyResult();
  const notifyCritical = useNotifyCritical();

  const [resultValues, setResultValues] = useState<Record<number, string>>({});

  const submitResults = async (detailId: number) => {
    const value = resultValues[detailId];
    if (!value) return;
    await results.mutateAsync({ id: selectedId, detailId, results: [{ value }] });
    setResultValues((prev) => ({ ...prev, [detailId]: "" }));
  };

  if (isLoading) return <div className="loading-state" role="status"><span className="spinner" aria-hidden="true" /> Loading…</div>;

  return (
    <section className="page">
      <header className="page__header"><h1>Lab Orders</h1></header>
      <Table.Root size="sm" variant="line">
        <Table.Header><Table.Row><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">#</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Patient</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Status</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Outsourced</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Date</Table.ColumnHeader></Table.Row></Table.Header>
        <Table.Body>
          {orders.map((o) => (
            <Table.Row key={o.orderId} onClick={() => setSelectedId(o.orderId)}>
              <Table.Cell>{o.orderId}</Table.Cell>
              <Table.Cell>{o.patientId}</Table.Cell>
              <Table.Cell>{o.status}</Table.Cell>
              <Table.Cell>{o.isOutSourced ? "Yes" : "No"}</Table.Cell>
              <Table.Cell>{o.orderDate?.slice(0, 10)}</Table.Cell>
            </Table.Row>
          ))}
          {orders.length === 0 && <Table.Row><Table.Cell colSpan={5}>No orders.</Table.Cell></Table.Row>}
        </Table.Body>
      </Table.Root>

      {order && (
        <>
          <h2>Order #{order.orderId} — Workflow</h2>
          <div className="row">
            <Button variant="secondary" onClick={() => collect.mutate(order.orderId)}>Collect</Button>
            <Button variant="secondary" onClick={() => start.mutate(order.orderId)}>Start</Button>
            <Button variant="secondary" onClick={() => cancel.mutate(order.orderId)}>Cancel</Button>
          </div>
          <Table.Root size="sm" variant="line">
            <Table.Header><Table.Row><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Test</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Status</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Barcode</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Actions</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Result</Table.ColumnHeader></Table.Row></Table.Header>
            <Table.Body>
              {order.details?.map((d) => (
                <Table.Row key={d.detailId}>
                  <Table.Cell>{d.testName}</Table.Cell>
                  <Table.Cell>{d.status}{d.verified ? " (verified)" : ""}{d.critical ? " ⚠ critical" : ""}</Table.Cell>
                  <Table.Cell>{d.barcode ?? "—"}</Table.Cell>
                  <Table.Cell>
                    <Button variant="secondary" size="sm" onClick={() => barcode.mutate({ id: order.orderId, detailId: d.detailId })}>Barcode</Button>
                    <Button variant="secondary" size="sm" onClick={() => accept.mutate({ id: order.orderId, detailId: d.detailId })}>Accept</Button>
                    <Button variant="secondary" size="sm" onClick={() => reject.mutate({ id: order.orderId, detailId: d.detailId })}>Reject</Button>
                    <Button variant="secondary" size="sm" onClick={() => finalize.mutate({ id: order.orderId, detailId: d.detailId })}>Finalize</Button>
                    <Button variant="secondary" size="sm" onClick={() => verify.mutate({ id: order.orderId, detailId: d.detailId })}>Verify</Button>
                    <Button variant="secondary" size="sm" onClick={() => notifyCritical.mutate({ id: order.orderId, detailId: d.detailId })}>Notify Critical</Button>
                  </Table.Cell>
                  <Table.Cell>
                    <div className="row">
                      <TextInput
                        type="text"
                        placeholder="Result value"
                        value={resultValues[d.detailId] ?? ""}
                        onChange={(e) => setResultValues((prev) => ({ ...prev, [d.detailId]: e.target.value }))}
                      />
                      <Button variant="secondary" size="sm" onClick={() => submitResults(d.detailId)}>Enter</Button>
                    </div>
                  </Table.Cell>
                </Table.Row>
              ))}
              {!order.details || order.details.length === 0 ? (
                <Table.Row><Table.Cell colSpan={5}>No test details.</Table.Cell></Table.Row>
              ) : null}
            </Table.Body>
          </Table.Root>
        </>
      )}
    </section>
  );
}