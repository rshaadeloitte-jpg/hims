import { useState } from "react";
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

  if (isLoading) return <p>Loading…</p>;

  return (
    <section className="page">
      <header className="page__header"><h1>Lab Orders</h1></header>
      <table className="table">
        <thead><tr><th>#</th><th>Patient</th><th>Status</th><th>Outsourced</th><th>Date</th></tr></thead>
        <tbody>
          {orders.map((o) => (
            <tr key={o.orderId} className={selectedId === o.orderId ? "row-selected" : ""} onClick={() => setSelectedId(o.orderId)}>
              <td>{o.orderId}</td>
              <td>{o.patientId}</td>
              <td>{o.status}</td>
              <td>{o.isOutSourced ? "Yes" : "No"}</td>
              <td>{o.orderDate?.slice(0, 10)}</td>
            </tr>
          ))}
          {orders.length === 0 && <tr><td colSpan={5} className="muted">No orders.</td></tr>}
        </tbody>
      </table>

      {order && (
        <>
          <h2>Order #{order.orderId} — Workflow</h2>
          <div className="row">
            <button className="btn" onClick={() => collect.mutate(order.orderId)}>Collect</button>
            <button className="btn" onClick={() => start.mutate(order.orderId)}>Start</button>
            <button className="btn" onClick={() => cancel.mutate(order.orderId)}>Cancel</button>
          </div>
          <table className="table">
            <thead><tr><th>Test</th><th>Status</th><th>Barcode</th><th>Actions</th><th>Result</th></tr></thead>
            <tbody>
              {order.details?.map((d) => (
                <tr key={d.detailId}>
                  <td>{d.testName}</td>
                  <td>{d.status}{d.verified ? " (verified)" : ""}{d.critical ? " ⚠ critical" : ""}</td>
                  <td>{d.barcode ?? "—"}</td>
                  <td>
                    <button className="btn btn--small" onClick={() => barcode.mutate({ id: order.orderId, detailId: d.detailId })}>Barcode</button>
                    <button className="btn btn--small" onClick={() => accept.mutate({ id: order.orderId, detailId: d.detailId })}>Accept</button>
                    <button className="btn btn--small" onClick={() => reject.mutate({ id: order.orderId, detailId: d.detailId })}>Reject</button>
                    <button className="btn btn--small" onClick={() => finalize.mutate({ id: order.orderId, detailId: d.detailId })}>Finalize</button>
                    <button className="btn btn--small" onClick={() => verify.mutate({ id: order.orderId, detailId: d.detailId })}>Verify</button>
                    <button className="btn btn--small" onClick={() => notifyCritical.mutate({ id: order.orderId, detailId: d.detailId })}>Notify Critical</button>
                  </td>
                  <td>
                    <div className="row">
                      <input
                        type="text"
                        placeholder="Result value"
                        value={resultValues[d.detailId] ?? ""}
                        onChange={(e) => setResultValues((prev) => ({ ...prev, [d.detailId]: e.target.value }))}
                      />
                      <button className="btn btn--small" onClick={() => submitResults(d.detailId)}>Enter</button>
                    </div>
                  </td>
                </tr>
              ))}
              {!order.details || order.details.length === 0 ? (
                <tr><td colSpan={5} className="muted">No test details.</td></tr>
              ) : null}
            </tbody>
          </table>
        </>
      )}
    </section>
  );
}