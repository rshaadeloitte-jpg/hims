import { useState } from "react";
import { useClinicalOrders, useCreateClinicalOrder, useUpdateOrderStatus } from "./queries";
import { clinicalOrderSchema } from "./zodSchema";

const ORDER_TYPES = ["LAB", "RADIOLOGY", "PHARMACY", "PROCEDURE", "CONSULTATION"];
const STATUSES = ["PENDING", "IN_PROGRESS", "COMPLETED", "CANCELLED"];
const PRIORITIES = ["ROUTINE", "URGENT", "STAT"];

export function OrdersPage() {
  const { data: orders = [], isLoading } = useClinicalOrders();
  const createOrder = useCreateClinicalOrder();
  const updateStatus = useUpdateOrderStatus();
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ patientId: "", orderType: "", description: "", priority: "ROUTINE" });
  const [errors, setErrors] = useState<Record<string, string>>({});

  const handleSubmit = () => {
    const result = clinicalOrderSchema.safeParse({ ...form, patientId: Number(form.patientId) });
    if (!result.success) {
      const fieldErrors: Record<string, string> = {};
      result.error.errors.forEach((e) => { fieldErrors[e.path[0]] = e.message; });
      setErrors(fieldErrors);
      return;
    }
    createOrder.mutate(result.data, { onSuccess: () => { setShowForm(false); setForm({ patientId: "", orderType: "", description: "", priority: "ROUTINE" }); setErrors({}); } });
  };

  if (isLoading) return <p className="muted">Loading orders…</p>;

  return (
    <section className="page">
      <header className="page-header">
        <h1>Clinical Orders</h1>
        <button className="btn btn-primary" onClick={() => setShowForm(!showForm)}>
          {showForm ? "Cancel" : "New Order"}
        </button>
      </header>

      {showForm && (
        <div className="card">
          <h2>New Clinical Order</h2>
          <div className="form-grid">
            <label>Patient ID {errors.patientId && <span className="error">{errors.patientId}</span>}</label>
            <input value={form.patientId} onChange={(e) => setForm({ ...form, patientId: e.target.value })} />
            <label>Order Type {errors.orderType && <span className="error">{errors.orderType}</span>}</label>
            <select value={form.orderType} onChange={(e) => setForm({ ...form, orderType: e.target.value })}>
              <option value="">Select…</option>
              {ORDER_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
            </select>
            <label>Priority</label>
            <select value={form.priority} onChange={(e) => setForm({ ...form, priority: e.target.value })}>
              {PRIORITIES.map((p) => <option key={p} value={p}>{p}</option>)}
            </select>
            <label>Description {errors.description && <span className="error">{errors.description}</span>}</label>
            <textarea value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
          </div>
          <button className="btn btn-primary" onClick={handleSubmit} disabled={createOrder.isPending}>
            {createOrder.isPending ? "Saving…" : "Place Order"}
          </button>
        </div>
      )}

      <div className="card">
        <table className="table">
          <thead>
            <tr><th>Patient</th><th>Type</th><th>Description</th><th>Priority</th><th>Status</th><th>Ordered By</th><th>Actions</th></tr>
          </thead>
          <tbody>
            {orders.map((o) => (
              <tr key={o.id}>
                <td>{o.patientName}</td>
                <td>{o.orderType}</td>
                <td>{o.description}</td>
                <td>{o.priority}</td>
                <td>{o.status}</td>
                <td>{o.orderedBy}</td>
                <td>
                  <select value={o.status} onChange={(e) => updateStatus.mutate({ id: o.id, status: e.target.value })}>
                    {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
                  </select>
                </td>
              </tr>
            ))}
            {orders.length === 0 && <tr><td colSpan={7} className="muted">No orders.</td></tr>}
          </tbody>
        </table>
      </div>
    </section>
  );
}
