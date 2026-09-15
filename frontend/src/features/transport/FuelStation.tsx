import { useState } from "react";
import { useVehicles, useFuelLogs, useAddFuelLog } from "./queries";
import { fuelLogCreateSchema } from "./zodSchema";
import type { FuelLogCreateForm } from "./zodSchema";

const emptyForm: FuelLogCreateForm = {
  vehicleId: 0,
  fuelType: "PETROL",
  quantity: 0,
  cost: 0,
  odometerReading: 0,
  filledBy: "",
};

const FUEL_TYPES = ["PETROL", "DIESEL", "CNG", "ELECTRIC"] as const;

export function FuelStation() {
  const { data: vehicles = [], isLoading: vehiclesLoading } = useVehicles();
  const { data: fuelLogs = [], isLoading: logsLoading, isError } = useFuelLogs();
  const addFuelLog = useAddFuelLog();
  const [form, setForm] = useState<FuelLogCreateForm>(emptyForm);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const handleSubmit = () => {
    const result = fuelLogCreateSchema.safeParse(form);
    if (!result.success) {
      const fieldErrors: Record<string, string> = {};
      result.error.errors.forEach((e) => { fieldErrors[e.path[0] as string] = e.message; });
      setErrors(fieldErrors);
      return;
    }
    addFuelLog.mutate(result.data, {
      onSuccess: () => { setForm(emptyForm); setErrors({}); },
    });
  };

  if (vehiclesLoading) return <div className="loading-state"><div className="spinner" />Loading vehicles…</div>;

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Fuel Station</h1>
          <p className="muted">Fuel log &amp; refueling records</p>
        </div>
      </header>

      <div className="card">
        <h2>Add Fuel Entry</h2>
        <div className="form-grid">
          <div className="field">
            <label>Vehicle <span className="req">*</span></label>
            <select
              value={form.vehicleId || ""}
              onChange={(e) => setForm({ ...form, vehicleId: Number(e.target.value) })}
            >
              <option value="">Select vehicle…</option>
              {vehicles.map((v) => (
                <option key={v.id} value={v.id}>{v.vehicleNumber} ({v.vehicleType})</option>
              ))}
            </select>
            {errors.vehicleId && <span className="field-error">{errors.vehicleId}</span>}
          </div>
          <div className="field">
            <label>Fuel Type <span className="req">*</span></label>
            <select
              value={form.fuelType}
              onChange={(e) => setForm({ ...form, fuelType: e.target.value as FuelLogCreateForm["fuelType"] })}
            >
              {FUEL_TYPES.map((t) => (
                <option key={t} value={t}>{t}</option>
              ))}
            </select>
          </div>
          <div className="field">
            <label>Quantity (L/kWh) <span className="req">*</span></label>
            <input
              type="number"
              step="0.01"
              value={form.quantity || ""}
              onChange={(e) => setForm({ ...form, quantity: Number(e.target.value) })}
            />
            {errors.quantity && <span className="field-error">{errors.quantity}</span>}
          </div>
          <div className="field">
            <label>Cost (₹) <span className="req">*</span></label>
            <input
              type="number"
              step="0.01"
              value={form.cost || ""}
              onChange={(e) => setForm({ ...form, cost: Number(e.target.value) })}
            />
            {errors.cost && <span className="field-error">{errors.cost}</span>}
          </div>
          <div className="field">
            <label>Odometer Reading <span className="req">*</span></label>
            <input
              type="number"
              value={form.odometerReading || ""}
              onChange={(e) => setForm({ ...form, odometerReading: Number(e.target.value) })}
            />
            {errors.odometerReading && <span className="field-error">{errors.odometerReading}</span>}
          </div>
          <div className="field">
            <label>Filled By <span className="req">*</span></label>
            <input
              value={form.filledBy}
              onChange={(e) => setForm({ ...form, filledBy: e.target.value })}
              placeholder="e.g. Driver name"
            />
            {errors.filledBy && <span className="field-error">{errors.filledBy}</span>}
          </div>
          <div className="form-actions">
            <button className="btn btn-primary" onClick={handleSubmit} disabled={addFuelLog.isPending}>
              {addFuelLog.isPending ? "Saving…" : "Add Fuel Entry"}
            </button>
          </div>
        </div>
      </div>

      <div className="card">
        <h2>Fuel Log History</h2>
        {isError && <div className="alert alert--error">Failed to load fuel logs.</div>}
        {logsLoading ? (
          <div className="loading-state"><div className="spinner" />Loading fuel logs…</div>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>Vehicle</th>
                <th>Type</th>
                <th>Quantity</th>
                <th>Cost (₹)</th>
                <th>Odometer</th>
                <th>Filled By</th>
                <th>Date</th>
              </tr>
            </thead>
            <tbody>
              {fuelLogs.map((log) => (
                <tr key={log.id}>
                  <td>{log.vehicleNumber ?? `Vehicle #${log.vehicleId}`}</td>
                  <td>{log.fuelType}</td>
                  <td>{log.quantity}</td>
                  <td>{log.cost.toFixed(2)}</td>
                  <td>{log.odometerReading}</td>
                  <td>{log.filledBy}</td>
                  <td>{new Date(log.filledAt).toLocaleString()}</td>
                </tr>
              ))}
              {fuelLogs.length === 0 && (
                <tr><td colSpan={7} className="muted">No fuel records found.</td></tr>
              )}
            </tbody>
          </table>
        )}
      </div>
    </section>
  );
}
