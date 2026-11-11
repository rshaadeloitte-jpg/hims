import { useState } from "react";
import {
  useClinicalOrders,
  useCreateClinicalOrder,
  useCancelClinicalOrder,
  ORDER_ITEM_TYPES,
  getOrderStatusLabel,
  type OrderItemType,
} from "./queries";
import { clinicalOrderSchema } from "./zodSchema";

interface ItemRow {
  itemType: OrderItemType;
  itemId: string;
  serviceName: string;
  rate: string;
  qty: string;
  doctorSharePercent: string;
}

const createDefaultItem = (): ItemRow => ({
  itemType: "LAB",
  itemId: "1",
  serviceName: "",
  rate: "0",
  qty: "1",
  doctorSharePercent: "",
});

export function OrdersPage() {
  const { data: orders = [], isLoading } = useClinicalOrders();
  const createOrder = useCreateClinicalOrder();
  const cancelOrder = useCancelClinicalOrder();

  const [showForm, setShowForm] = useState(false);
  const [patientId, setPatientId] = useState("");
  const [prescriberId, setPrescriberId] = useState("");
  const [contextType, setContextType] = useState<"encounter" | "admission">("encounter");
  const [contextId, setContextId] = useState("");
  const [wardId, setWardId] = useState("");
  const [concessionPercent, setConcessionPercent] = useState("");
  const [pharmacyPrescriptionId, setPharmacyPrescriptionId] = useState("");
  const [items, setItems] = useState<ItemRow[]>([createDefaultItem()]);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [serverError, setServerError] = useState<string | null>(null);

  const resetForm = () => {
    setPatientId("");
    setPrescriberId("");
    setContextType("encounter");
    setContextId("");
    setWardId("");
    setConcessionPercent("");
    setPharmacyPrescriptionId("");
    setItems([createDefaultItem()]);
    setErrors({});
    setServerError(null);
  };

  const updateItem = (index: number, field: keyof ItemRow, value: string) => {
    setItems((prev) => {
      const next = [...prev];
      next[index] = { ...next[index], [field]: value };
      return next;
    });
  };

  const addItem = () => {
    setItems((prev) => [...prev, createDefaultItem()]);
  };

  const removeItem = (index: number) => {
    setItems((prev) => (prev.length > 1 ? prev.filter((_, i) => i !== index) : prev));
  };

  const handleSubmit = () => {
    setServerError(null);
    setErrors({});

    const payload = {
      patientId: patientId !== "" ? Number(patientId) : undefined,
      prescriberId: prescriberId !== "" ? Number(prescriberId) : undefined,
      encounterId: contextType === "encounter" && contextId !== "" ? Number(contextId) : undefined,
      admissionId: contextType === "admission" && contextId !== "" ? Number(contextId) : undefined,
      wardId: wardId !== "" ? Number(wardId) : undefined,
      concessionPercent: concessionPercent !== "" ? Number(concessionPercent) : undefined,
      pharmacyPrescriptionId:
        pharmacyPrescriptionId !== "" ? Number(pharmacyPrescriptionId) : undefined,
      items: items.map((item, idx) => ({
        itemType: item.itemType,
        itemId: item.itemId !== "" ? Number(item.itemId) : idx + 1,
        serviceName: item.serviceName,
        rate: item.rate !== "" ? Number(item.rate) : 0,
        qty: item.qty !== "" ? Number(item.qty) : 1,
        doctorSharePercent:
          item.doctorSharePercent !== "" ? Number(item.doctorSharePercent) : undefined,
      })),
    };

    const result = clinicalOrderSchema.safeParse(payload);
    if (!result.success) {
      const fieldErrors: Record<string, string> = {};
      result.error.errors.forEach((e) => {
        const key = e.path.join(".");
        fieldErrors[key] = e.message;
        if (typeof e.path[0] === "string" && !fieldErrors[e.path[0]]) {
          fieldErrors[e.path[0]] = e.message;
        }
      });
      setErrors(fieldErrors);
      return;
    }

    createOrder.mutate(result.data, {
      onSuccess: () => {
        setShowForm(false);
        resetForm();
      },
      onError: (err: any) => {
        setServerError(err?.message || "Failed to place order");
      },
    });
  };

  if (isLoading) return <p className="muted">Loading orders…</p>;

  return (
    <section className="page">
      <header className="page-header">
        <h1>Clinical Orders</h1>
        <button
          className="btn btn-primary"
          onClick={() => {
            setShowForm(!showForm);
            if (showForm) resetForm();
          }}
        >
          {showForm ? "Cancel" : "New Order"}
        </button>
      </header>

      {showForm && (
        <div className="card">
          <h2>New Clinical Order</h2>

          {serverError && (
            <div
              className="error alert alert-danger"
              role="alert"
              style={{
                marginBottom: "1rem",
                padding: "0.75rem",
                color: "#c53030",
                backgroundColor: "#fff5f5",
                border: "1px solid #feb2b2",
                borderRadius: "4px",
              }}
            >
              {serverError}
            </div>
          )}

          <div className="form-grid">
            <div>
              <label>
                Patient ID{" "}
                {errors.patientId && <span className="error">({errors.patientId})</span>}
              </label>
              <input
                type="number"
                placeholder="e.g. 501"
                value={patientId}
                onChange={(e) => setPatientId(e.target.value)}
              />
            </div>

            <div>
              <label>
                Prescriber Doctor ID{" "}
                {errors.prescriberId && <span className="error">({errors.prescriberId})</span>}
              </label>
              <input
                type="number"
                placeholder="e.g. 11"
                value={prescriberId}
                onChange={(e) => setPrescriberId(e.target.value)}
              />
            </div>

            <div>
              <label>Context Type</label>
              <div style={{ display: "flex", gap: "0.5rem", marginTop: "4px" }}>
                <button
                  type="button"
                  className={`btn ${contextType === "encounter" ? "btn-primary" : "btn-secondary"}`}
                  onClick={() => setContextType("encounter")}
                >
                  Encounter
                </button>
                <button
                  type="button"
                  className={`btn ${contextType === "admission" ? "btn-primary" : "btn-secondary"}`}
                  onClick={() => setContextType("admission")}
                >
                  Admission
                </button>
              </div>
            </div>

            <div>
              <label>
                {contextType === "encounter" ? "Encounter ID" : "Admission ID"}{" "}
                {(errors.encounterId || errors.admissionId) && (
                  <span className="error">
                    ({errors.encounterId || errors.admissionId})
                  </span>
                )}
              </label>
              <input
                type="number"
                placeholder={contextType === "encounter" ? "e.g. 7001" : "e.g. 9001"}
                value={contextId}
                onChange={(e) => setContextId(e.target.value)}
              />
            </div>

            <div>
              <label>Ward ID (Optional)</label>
              <input
                type="number"
                placeholder="e.g. 2"
                value={wardId}
                onChange={(e) => setWardId(e.target.value)}
              />
            </div>

            <div>
              <label>
                Concession % (Optional){" "}
                {errors.concessionPercent && (
                  <span className="error">({errors.concessionPercent})</span>
                )}
              </label>
              <input
                type="number"
                min="0"
                max="100"
                step="0.01"
                placeholder="0 - 100"
                value={concessionPercent}
                onChange={(e) => setConcessionPercent(e.target.value)}
              />
            </div>
          </div>

          <div className="order-items-section" style={{ marginTop: "1.5rem" }}>
            <h3>
              Order Items{" "}
              {errors.items && <span className="error">({errors.items})</span>}
            </h3>
            {items.map((item, index) => (
              <div
                key={index}
                className="item-row"
                style={{
                  display: "grid",
                  gridTemplateColumns: "140px 100px 1fr 100px 80px auto",
                  gap: "0.5rem",
                  alignItems: "end",
                  marginBottom: "0.75rem",
                }}
              >
                <div>
                  <label>Type</label>
                  <select
                    value={item.itemType}
                    onChange={(e) =>
                      updateItem(index, "itemType", e.target.value as OrderItemType)
                    }
                  >
                    {ORDER_ITEM_TYPES.map((t) => (
                      <option key={t} value={t}>
                        {t}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label>Item ID</label>
                  <input
                    type="number"
                    value={item.itemId}
                    onChange={(e) => updateItem(index, "itemId", e.target.value)}
                    placeholder="1"
                  />
                </div>

                <div>
                  <label htmlFor={`item-service-${index}`}>
                    Service Name{" "}
                    {errors[`items.${index}.serviceName`] && (
                      <span className="error">
                        ({errors[`items.${index}.serviceName`]})
                      </span>
                    )}
                  </label>
                  <input
                    id={`item-service-${index}`}
                    type="text"
                    value={item.serviceName}
                    onChange={(e) => updateItem(index, "serviceName", e.target.value)}
                    placeholder="e.g. Complete Blood Count"
                  />
                </div>

                <div>
                  <label>Rate (₹)</label>
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    value={item.rate}
                    onChange={(e) => updateItem(index, "rate", e.target.value)}
                  />
                </div>

                <div>
                  <label>Qty</label>
                  <input
                    type="number"
                    min="1"
                    value={item.qty}
                    onChange={(e) => updateItem(index, "qty", e.target.value)}
                  />
                </div>

                <div>
                  {items.length > 1 && (
                    <button
                      type="button"
                      className="btn btn-secondary"
                      onClick={() => removeItem(index)}
                    >
                      Remove
                    </button>
                  )}
                </div>
              </div>
            ))}

            <button
              type="button"
              className="btn btn-secondary"
              onClick={addItem}
              style={{ marginTop: "0.25rem", marginBottom: "1rem" }}
            >
              + Add Item
            </button>
          </div>

          <div style={{ marginTop: "1rem" }}>
            <button
              className="btn btn-primary"
              onClick={handleSubmit}
              disabled={createOrder.isPending}
            >
              {createOrder.isPending ? "Placing Order…" : "Place Order"}
            </button>
          </div>
        </div>
      )}

      <div className="card">
        <table className="table">
          <thead>
            <tr>
              <th>#</th>
              <th>Patient</th>
              <th>Prescriber</th>
              <th>Context</th>
              <th>Items</th>
              <th>Net Total</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {orders.map((o) => {
              const orderId = o.orderId ?? o.id;
              const statusLabel = getOrderStatusLabel(o.statusCode, o.status);
              const context = o.encounterId
                ? `Encounter #${o.encounterId}`
                : o.admissionId
                ? `Admission #${o.admissionId}`
                : "—";
              const itemsSummary =
                o.items && o.items.length > 0
                  ? o.items.map((i) => `${i.serviceName} (${i.itemType})`).join(", ")
                  : (o.orderType ?? "—");
              const netTotalDisplay =
                o.netTotal != null ? `₹${Number(o.netTotal).toFixed(2)}` : "—";
              const patientDisplay = o.patientId
                ? `Patient #${o.patientId}`
                : (o.patientName ?? "—");
              const prescriberDisplay = o.prescriberId
                ? `Doctor #${o.prescriberId}`
                : (o.orderedBy ?? "—");

              return (
                <tr key={orderId}>
                  <td>{orderId}</td>
                  <td>{patientDisplay}</td>
                  <td>{prescriberDisplay}</td>
                  <td>{context}</td>
                  <td>{itemsSummary}</td>
                  <td>{netTotalDisplay}</td>
                  <td>
                    <span className={`status-badge status-${statusLabel.toLowerCase()}`}>
                      {statusLabel}
                    </span>
                  </td>
                  <td>
                    {o.statusCode === 1 && (
                      <button
                        className="btn btn-secondary btn-sm"
                        onClick={() => cancelOrder.mutate(orderId!)}
                        disabled={cancelOrder.isPending}
                      >
                        Cancel
                      </button>
                    )}
                  </td>
                </tr>
              );
            })}
            {orders.length === 0 && (
              <tr>
                <td colSpan={8} className="muted">
                  No orders.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
}
