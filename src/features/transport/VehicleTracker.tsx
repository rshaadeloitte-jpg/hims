import { useState } from "react";
import {
  useVehicles,
  useVehicleIn,
  useVehicleOut,
  useAllocateVehicle,
} from "./queries";
import {
  vehicleInSchema,
  vehicleOutSchema,
  vehicleAllocationSchema,
} from "./zodSchema";
import type { VehicleInForm, VehicleOutForm, VehicleAllocationForm } from "./zodSchema";
import { StatusIndicator } from "./StatusIndicator";

type Tab = "in" | "out" | "allocate";

const emptyInForm: VehicleInForm = { vehicleId: 0, location: "", odometerReading: 0, notes: "" };
const emptyOutForm: VehicleOutForm = { vehicleId: 0, destination: "", odometerReading: 0, purpose: "" };
const emptyAllocForm: VehicleAllocationForm = { vehicleId: 0, allocatedTo: "", purpose: "", expectedReturnAt: "" };

export function VehicleTracker() {
  const { data: vehicles = [], isLoading, isError } = useVehicles();
  const vehicleIn = useVehicleIn();
  const vehicleOut = useVehicleOut();
  const allocateVehicle = useAllocateVehicle();

  const [tab, setTab] = useState<Tab>("in");
  const [inForm, setInForm] = useState<VehicleInForm>(emptyInForm);
  const [outForm, setOutForm] = useState<VehicleOutForm>(emptyOutForm);
  const [allocForm, setAllocForm] = useState<VehicleAllocationForm>(emptyAllocForm);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const handleIn = () => {
    const result = vehicleInSchema.safeParse(inForm);
    if (!result.success) {
      const fieldErrors: Record<string, string> = {};
      result.error.errors.forEach((e) => { fieldErrors[e.path[0] as string] = e.message; });
      setErrors(fieldErrors);
      return;
    }
    vehicleIn.mutate(result.data, {
      onSuccess: () => { setInForm(emptyInForm); setErrors({}); },
    });
  };

  const handleOut = () => {
    const result = vehicleOutSchema.safeParse(outForm);
    if (!result.success) {
      const fieldErrors: Record<string, string> = {};
      result.error.errors.forEach((e) => { fieldErrors[e.path[0] as string] = e.message; });
      setErrors(fieldErrors);
      return;
    }
    vehicleOut.mutate(result.data, {
      onSuccess: () => { setOutForm(emptyOutForm); setErrors({}); },
    });
  };

  const handleAllocate = () => {
    const result = vehicleAllocationSchema.safeParse(allocForm);
    if (!result.success) {
      const fieldErrors: Record<string, string> = {};
      result.error.errors.forEach((e) => { fieldErrors[e.path[0] as string] = e.message; });
      setErrors(fieldErrors);
      return;
    }
    allocateVehicle.mutate(result.data, {
      onSuccess: () => { setAllocForm(emptyAllocForm); setErrors({}); },
    });
  };

  if (isLoading) return <div className="loading-state"><div className="spinner" />Loading vehicles…</div>;
  if (isError) return <div className="alert alert--error">Failed to load vehicles.</div>;

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Vehicle Tracker</h1>
          <p className="muted">Vehicle in/out &amp; allocation management</p>
        </div>
      </header>

      <div className="tn-tabs">
        <button className={`tn-tab ${tab === "in" ? "tn-tab--active" : ""}`} onClick={() => setTab("in")}>Vehicle In</button>
        <button className={`tn-tab ${tab === "out" ? "tn-tab--active" : ""}`} onClick={() => setTab("out")}>Vehicle Out</button>
        <button className={`tn-tab ${tab === "allocate" ? "tn-tab--active" : ""}`} onClick={() => setTab("allocate")}>Allocation</button>
      </div>

      <div className="card">
        {tab === "in" && (
          <div className="form-grid">
            <div className="field">
              <label>Vehicle <span className="req">*</span></label>
              <select
                value={inForm.vehicleId || ""}
                onChange={(e) => setInForm({ ...inForm, vehicleId: Number(e.target.value) })}
              >
                <option value="">Select vehicle…</option>
                {vehicles.map((v) => (
                  <option key={v.id} value={v.id}>{v.vehicleNumber} ({v.vehicleType})</option>
                ))}
              </select>
              {errors.vehicleId && <span className="field-error">{errors.vehicleId}</span>}
            </div>
            <div className="field">
              <label>Location <span className="req">*</span></label>
              <input
                value={inForm.location}
                onChange={(e) => setInForm({ ...inForm, location: e.target.value })}
                placeholder="e.g. Main Gate"
              />
              {errors.location && <span className="field-error">{errors.location}</span>}
            </div>
            <div className="field">
              <label>Odometer Reading <span className="req">*</span></label>
              <input
                type="number"
                value={inForm.odometerReading}
                onChange={(e) => setInForm({ ...inForm, odometerReading: Number(e.target.value) })}
              />
              {errors.odometerReading && <span className="field-error">{errors.odometerReading}</span>}
            </div>
            <div className="field">
              <label>Notes</label>
              <input
                value={inForm.notes || ""}
                onChange={(e) => setInForm({ ...inForm, notes: e.target.value })}
                placeholder="Optional notes"
              />
            </div>
            <div className="form-actions">
              <button className="btn btn-primary" onClick={handleIn} disabled={vehicleIn.isPending}>
                {vehicleIn.isPending ? "Recording…" : "Record Vehicle In"}
              </button>
            </div>
          </div>
        )}

        {tab === "out" && (
          <div className="form-grid">
            <div className="field">
              <label>Vehicle <span className="req">*</span></label>
              <select
                value={outForm.vehicleId || ""}
                onChange={(e) => setOutForm({ ...outForm, vehicleId: Number(e.target.value) })}
              >
                <option value="">Select vehicle…</option>
                {vehicles.map((v) => (
                  <option key={v.id} value={v.id}>{v.vehicleNumber} ({v.vehicleType})</option>
                ))}
              </select>
              {errors.vehicleId && <span className="field-error">{errors.vehicleId}</span>}
            </div>
            <div className="field">
              <label>Destination <span className="req">*</span></label>
              <input
                value={outForm.destination}
                onChange={(e) => setOutForm({ ...outForm, destination: e.target.value })}
                placeholder="e.g. Airport"
              />
              {errors.destination && <span className="field-error">{errors.destination}</span>}
            </div>
            <div className="field">
              <label>Odometer Reading <span className="req">*</span></label>
              <input
                type="number"
                value={outForm.odometerReading}
                onChange={(e) => setOutForm({ ...outForm, odometerReading: Number(e.target.value) })}
              />
              {errors.odometerReading && <span className="field-error">{errors.odometerReading}</span>}
            </div>
            <div className="field">
              <label>Purpose <span className="req">*</span></label>
              <input
                value={outForm.purpose}
                onChange={(e) => setOutForm({ ...outForm, purpose: e.target.value })}
                placeholder="e.g. Patient transfer"
              />
              {errors.purpose && <span className="field-error">{errors.purpose}</span>}
            </div>
            <div className="form-actions">
              <button className="btn btn-primary" onClick={handleOut} disabled={vehicleOut.isPending}>
                {vehicleOut.isPending ? "Recording…" : "Record Vehicle Out"}
              </button>
            </div>
          </div>
        )}

        {tab === "allocate" && (
          <div className="form-grid">
            <div className="field">
              <label>Vehicle <span className="req">*</span></label>
              <select
                value={allocForm.vehicleId || ""}
                onChange={(e) => setAllocForm({ ...allocForm, vehicleId: Number(e.target.value) })}
              >
                <option value="">Select vehicle…</option>
                {vehicles.map((v) => (
                  <option key={v.id} value={v.id}>{v.vehicleNumber} ({v.vehicleType})</option>
                ))}
              </select>
              {errors.vehicleId && <span className="field-error">{errors.vehicleId}</span>}
            </div>
            <div className="field">
              <label>Allocated To <span className="req">*</span></label>
              <input
                value={allocForm.allocatedTo}
                onChange={(e) => setAllocForm({ ...allocForm, allocatedTo: e.target.value })}
                placeholder="e.g. Dr. Smith / Emergency Dept"
              />
              {errors.allocatedTo && <span className="field-error">{errors.allocatedTo}</span>}
            </div>
            <div className="field">
              <label>Purpose <span className="req">*</span></label>
              <input
                value={allocForm.purpose}
                onChange={(e) => setAllocForm({ ...allocForm, purpose: e.target.value })}
                placeholder="e.g. Emergency duty"
              />
              {errors.purpose && <span className="field-error">{errors.purpose}</span>}
            </div>
            <div className="field">
              <label>Expected Return</label>
              <input
                type="datetime-local"
                value={allocForm.expectedReturnAt || ""}
                onChange={(e) => setAllocForm({ ...allocForm, expectedReturnAt: e.target.value })}
              />
            </div>
            <div className="form-actions">
              <button className="btn btn-primary" onClick={handleAllocate} disabled={allocateVehicle.isPending}>
                {allocateVehicle.isPending ? "Allocating…" : "Allocate Vehicle"}
              </button>
            </div>
          </div>
        )}
      </div>

      <div className="card">
        <h2>Vehicle Status</h2>
        <table className="data-table">
          <thead>
            <tr>
              <th>Number</th>
              <th>Type</th>
              <th>Driver</th>
              <th>Status</th>
              <th>Location</th>
              <th>Allocated To</th>
            </tr>
          </thead>
          <tbody>
            {vehicles.map((v) => (
              <tr key={v.id}>
                <td>{v.vehicleNumber}</td>
                <td>{v.vehicleType}</td>
                <td>{v.driverName}</td>
                <td><StatusIndicator status={v.status} /></td>
                <td>{v.currentLocation ?? "—"}</td>
                <td>{v.allocatedTo ?? "—"}</td>
              </tr>
            ))}
            {vehicles.length === 0 && (
              <tr><td colSpan={6} className="muted">No vehicles found.</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
}
