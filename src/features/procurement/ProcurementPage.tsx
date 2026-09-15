import { useState } from "react";
import { useApprovePurchaseOrder, useCreatePurchaseOrder, usePurchaseOrders } from "./queries";
import { purchaseOrderSchema } from "./zodSchema";

const STATUSES = ["DRAFT", "PENDING_APPROVAL", "APPROVED", "REJECTED", "RECEIVED"];

export function ProcurementPage() {
  const { data: orders = [], isLoading } = usePurchaseOrders();
  const createOrder = useCreatePurchaseOrder();
  const approveOrder = useApprovePurchaseOrder();
  const [showForm, setShowForm] = useState(false);
  const [supplierId, setSupplierId] = useState("");
  const [items, setItems] = useState([{ itemId: "", quantity: "", unitPrice: "" }]);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const handleSubmit = () => {
    const parsedItems = items.map((i) => ({ itemId: Number(i.itemId), quantity: Number(i.quantity), unitPrice: Number(i.unitPrice) }));
    const result = purchaseOrderSchema.safeParse({ supplierId: Number(supplierId), items: parsedItems });
    if (!result.success) {
      const fieldErrors: Record<string, string> = {};
      result.error.errors.forEach((e) => { fieldErrors[e.path[0]] = e.message; });
      setErrors(fieldErrors);
      return;
    }
    createOrder.mutate(result.data, { onSuccess: () => { setShowForm(false); setSupplierId(""); setItems([{ itemId: "", quantity: "", unitPrice: "" }]); setErrors({}); } });
  };

  if (isLoading) return <p className="muted">Loading purchase orders…</p>;

  return (
    <section className="page">
      <header className="page-header">
        <h1>Procurement</h1>
        <button className="btn btn-primary" onClick={() => setShowForm(!showForm)}>
          {showForm ? "Cancel" : "New PO"}
        </button>
      </header>

      {showForm && (
        <div className="card">
          <h2>New Purchase Order</h2>
          <div className="form-grid">
            <label>Supplier ID {errors.supplierId && <span className="error">{errors.supplierId}</span>}</label>
            <input value={supplierId} onChange={(e) => setSupplierId(e.target.value)} />
            <h3>Items</h3>
            {items.map((item, idx) => (
              <div key={idx} className="item-row">
                <input placeholder="Item ID" value={item.itemId} onChange={(e) => { const newItems = [...items]; newItems[idx].itemId = e.target.value; setItems(newItems); }} />
                <input placeholder="Qty" type="number" value={item.quantity} onChange={(e) => { const newItems = [...items]; newItems[idx].quantity = e.target.value; setItems(newItems); }} />
                <input placeholder="Unit Price" type="number" value={item.unitPrice} onChange={(e) => { const newItems = [...items]; newItems[idx].unitPrice = e.target.value; setItems(newItems); }} />
              </div>
            ))}
            <button className="btn btn-secondary" onClick={() => setItems([...items, { itemId: "", quantity: "", unitPrice: "" }])}>Add Item</button>
          </div>
          <button className="btn btn-primary" onClick={handleSubmit} disabled={createOrder.isPending}>
            {createOrder.isPending ? "Saving…" : "Create PO"}
          </button>
        </div>
      )}

      <div className="card">
        <table className="table">
          <thead>
            <tr><th>PO No</th><th>Supplier</th><th>Amount</th><th>Status</th><th>Created</th><th>Actions</th></tr>
          </thead>
          <tbody>
            {orders.map((o) => (
              <tr key={o.id}>
                <td>{o.poNo}</td>
                <td>{o.supplierName}</td>
                <td>{o.totalAmount}</td>
                <td>{o.status}</td>
                <td>{o.createdAt}</td>
                <td>
                  {o.status === "PENDING_APPROVAL" && (
                    <button className="btn btn-sm btn-primary" onClick={() => approveOrder.mutate(o.id)} disabled={approveOrder.isPending}>
                      Approve
                    </button>
                  )}
                </td>
              </tr>
            ))}
            {orders.length === 0 && <tr><td colSpan={6} className="muted">No purchase orders.</td></tr>}
          </tbody>
        </table>
      </div>
    </section>
  );
}
