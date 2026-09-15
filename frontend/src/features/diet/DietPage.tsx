import { useState } from "react";
import { useDietOrders, useUpdateDietOrderStatus } from "./queries";

const MEAL_TYPES = ["BREAKFAST", "LUNCH", "DINNER", "SNACKS"];
const STATUSES = ["ORDERED", "PREPARED", "DELIVERED", "CANCELLED"];

export function DietPage() {
  const { data: orders = [], isLoading } = useDietOrders();
  const updateStatus = useUpdateDietOrderStatus();

  if (isLoading) return <p className="muted">Loading diet orders…</p>;

  return (
    <section className="page">
      <header className="page-header"><h1>Diet & Kitchen</h1></header>
      <div className="card">
        <table className="table">
          <thead><tr><th>Patient</th><th>Meal</th><th>Diet Type</th><th>Status</th><th>Ordered At</th><th>Actions</th></tr></thead>
          <tbody>
            {orders.map((o) => (
              <tr key={o.id}>
                <td>{o.patientName}</td>
                <td>{o.mealType}</td>
                <td>{o.dietType}</td>
                <td>{o.status}</td>
                <td>{o.orderedAt}</td>
                <td>
                  <select value={o.status} onChange={(e) => updateStatus.mutate({ id: o.id, status: e.target.value })}>
                    {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
                  </select>
                </td>
              </tr>
            ))}
            {orders.length === 0 && <tr><td colSpan={6} className="muted">No diet orders.</td></tr>}
          </tbody>
        </table>
      </div>
    </section>
  );
}
